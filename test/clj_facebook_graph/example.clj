; Copyright (c) Maximilian Weber. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file epl-v10.html at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns clj-facebook-graph.example
  (:use [ring.util.response :only [redirect]]
        [ring.middleware.stacktrace :only [wrap-stacktrace-web]]
        [ring.middleware.params :only [wrap-params]]
        [ring.middleware.session :only [wrap-session]]
        [ring.middleware.session.memory :only [memory-store]]
        [ring.handler.dump :only [handle-dump]]
        [clj-facebook-graph.auth :only [with-facebook-auth with-facebook-access-token make-auth-request]]
        [clj-facebook-graph.helper :only [facebook-base-url]]
        [clj-facebook-graph.ring-middleware :only [wrap-facebook-access-token-required
                                                   wrap-facebook-extract-callback-code
                                                   wrap-facebook-auth]]
        compojure.core
        hiccup.core
        ring.adapter.jetty)
  (:require [compojure.route :as route]
            [clj-facebook-graph.client :as client])
  (:import [java.lang Exception]
           [clj_facebook_graph FacebookGraphException]))

(def name-id-map (atom {}))

(defn create-friend-id-by-name [friends]
  (into {} (map (fn [{:keys [name id]}]
                  [name id]) friends)))

(defn get-friends-name-id-mapping [facebook-auth]
  (let [access-token (:access_token facebook-auth)]
    (if-let [friends-name-id-mapping (@name-id-map access-token)]
      friends-name-id-mapping
      (let [friends-id-by-name (create-friend-id-by-name
                                (client/get [:me :friends] {:extract :data}))]
        (do (swap! name-id-map assoc access-token friends-id-by-name)
            friends-id-by-name)))))

(defn wrap-facebook-id-by-name [client]
  (fn [request]
    (let [url (:url request)]
      (if (and clj-facebook-graph.auth/*facebook-auth* (vector? url))
        (let [[name] url]
          (if-let [name (:name name)]
            (let [friends-name-id-mapping
                  (get-friends-name-id-mapping clj-facebook-graph.auth/*facebook-auth*)
                  id (friends-name-id-mapping name)
                  request (assoc request :url (assoc url 0 id))]
              (client request))
            (client request)))
        (client request)))))

(def request (wrap-facebook-id-by-name #'clj-facebook-graph.client/request))

(defn fb-get
  "Like #'request, but sets the :method and :url as appropriate."
  [url & [req]]
  (request (merge req {:method :get :url url})))

(defn get-album-overview [id]
  (map (fn [{:keys [id name]}]
         {:name name
          :preview-image (with-facebook-access-token
                           (str facebook-base-url "/" id "/picture"))})
       (fb-get [id :albums] {:extract :data})))

(defn render-album-overview [id]
  (html (map (fn [{:keys [name preview-image]}]
               [:div [:h3 name] [:img {:src preview-image}]])
             (get-album-overview id))))

(defonce facebook-app-info {:client-id "your Facebook app id"
                            :client-secret "your Facebook app's secret"
                            :redirect-uri "http://localhost:8080/facebook-callback"
                            :scope  ["user_photos" "friends_photos" "publish_stream"]})

(defroutes app
  (GET "/facebook-login" [] (redirect (:uri (make-auth-request facebook-app-info))))
  (GET "/facebook-callback" request
       (if-let [return-to (:return-to (:session request))]
         (redirect return-to)
         (handle-dump request)))
  (GET "/albums/:id" [id] (if (not clj-facebook-graph.auth/*facebook-auth*)
                            (throw
                             (FacebookGraphException.
                              {:error :facebook-login-required}))
                            (if (.contains id "_")
                              (render-album-overview {:name (.replaceAll id "_" " ")})
                              (render-album-overview id))))
  (GET "/show-session" {session :session} (str session))
  (route/not-found "Page not found"))

(def session-store (atom {}))

(defn wrap-app [app facebook-app-info] 
  (-> app
      (wrap-facebook-auth)
      (wrap-facebook-extract-callback-code facebook-app-info)
      (wrap-facebook-access-token-required facebook-app-info)
      (wrap-session {:store (memory-store session-store)})
      (wrap-params)
      (wrap-stacktrace-web)))

(def the-app (wrap-app app facebook-app-info))

(defn start-server []
  (future (run-jetty (var the-app) {:port 8080 :join? false})))

;(def server (start-server))
;(.stop (.get server))


(def example-wall-post
     {:message "Check out this funny article"
      :link "http://www.example.com/article.html"
      :picture "http://www.example.com/article-thumbnail.jpg'"
      :name "Article Title"
      :caption "Caption for the link"
      :description "Longer description of the link"
      :actions "{\"name\": \"View on Zombo\", \"link\": \"http://www.zombo.com\"}"
      :privacy "{\"value\": \"ALL_FRIENDS\"}"
      :targeting "{\"countries\":\"US\",\"regions\":\"6,53\",\"locales\":\"6\"}"})

(defmacro with-facebook-auth-by-name [name & body]
  (list* 'clj-facebook-graph.auth/with-facebook-auth-by-name
         'clj-facebook-graph.client/get
         @session-store name body))
