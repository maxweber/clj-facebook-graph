; Copyright (c) Maximilian Weber. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file epl-v10.html at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns clj-facebook-graph.client
  "A client for the Facebook Graph API based on clj-http and clj-oauth2."
  (:use [clj-facebook-graph.helper :only [wrap-exceptions facebook-base-url facebook-fql-base-url]]
        [clj-facebook-graph.auth :only [wrap-facebook-access-token]]
        [clj-facebook-graph.error-handling :only [wrap-facebook-exceptions]]
        [clojure.data.json :only [read-json]] 
        [clj-oauth2.client :only [wrap-oauth2]])
  (:require [clj-http.client :as client]
            [ring.util.codec])
  (:refer-clojure :exclude [get]))

(defn wrap-facebook-url-builder [client]
  "Offers some convenience by assemble a Facebook Graph API URL from a vector of keywords or strings.
  Instead of defining the whole Facebook Graph API URL like this (client/get \"https://graph.facebook.com/me/friends\") you can
  simple write (client/get [:me :friends]) (you can also write [\"me\" \"friends\"]). It's flexible thanks to the homogeneity
  of the Facebook Graph API. When you have more than an id (here \"me\") and a connection type
  (here \"friends\"), you can also provide three or more
  keywords (or strings) like in the case of 'https://graph.facebook.com/me/videos/uploaded' for example."
  (fn [req]
    (let [{:keys [url]} req]    
      (if (vector? url)
        (let [url-parts-as-str (map #(if (keyword? %) (name %) (str %)) url)
              url (apply str (interpose "/" (conj url-parts-as-str facebook-base-url)))]
          (client (assoc req :url url)))
        (client req)))))

(defn wrap-json-response-conversion [client]
  "Automatically transforms the body of a response of a Facebook Graph API request from JSON to a Clojure
   data structure through the use of clojure.data.json. It checks if the header Content-Type
   is 'text/javascript' which the Facebook Graph API returns in the case of a JSON response."
  (fn [req]
    (let [{:keys [headers] :as resp} (client req)
          content-type (headers "content-type")]
      (if (and content-type
               (or
                (.startsWith content-type "text/javascript")
                (.startsWith content-type "application/json")))
        (assoc resp :body (read-json (:body resp)))
        resp))))

(defn wrap-facebook-data-extractor [client]
  "The Facebook Graph API mostly returns a JSON document in the form like this one:
   {
      \"data\": [...]
   }
  This Ring-style middleware for clj-http automatically extracts the data part of the
  response, when you provide a map entry in the request like this one ':request :data'
  in the request map. If you like, you can also extract the paging part (':request :paging').
  If you add paging to your Facebook Graph API request through a query parameter like
  limit (see 'http://developers.facebook.com/docs/api/#reading' for details) you can also
  add the map entry ':extract :data :paging true' and you get a lazy-seq as request body, which
  automatically triggers the pagination as you walk through the seq.
  This Ring-style middleware also supports to simply extract the body part of the request
  (':extract :body'). "
  (fn [req]
    (let [{:keys [extract paging]} req
          response (client req)
          body (:body response)]
      (if extract
        (if (= :body extract)
          body
          (let [extraction (extract body)
                the-client (wrap-facebook-data-extractor client)]
            (if paging
              (if-let [url (get-in body [:paging :next])]
                (let [query-params (-> url client/parse-url :query-string ring.util.codec/form-decode)]
                  (lazy-cat extraction
                            (the-client {:method :get :url url :query-params query-params :extract :data :paging true})))
                extraction)
              extraction)))
        response))))

(defn wrap-fql [client]
  (fn [req]
    (let [{:keys [url fql]} req]
      (if (and (= url :fql))
        (client
         (-> req
             (assoc :url facebook-fql-base-url)
             (assoc-in [:query-params :query]
                       fql)
             (assoc-in [:query-params :format]
                       "json")))
        (client req)))))

(defn wrap-request
  "Wraps the clj-http client with the Ring-style middleware for the
   Facebook Graph API."
  ([request wrap-request-fn]
     (-> request
         wrap-facebook-exceptions
         wrap-exceptions
         wrap-request-fn
         wrap-oauth2
         wrap-facebook-access-token
         wrap-json-response-conversion
         wrap-facebook-url-builder
         wrap-facebook-data-extractor
         wrap-fql
         ))
  ([request] (wrap-request request client/wrap-request)))

(def
  request
  (wrap-request #'clj-http.core/request))

(defn get
  "Like #'request, but sets the :method and :url as appropriate."
  [url & [req]]
  (request (merge req {:method :get :url url})))

(defn post
  "Like #'request, but sets the :method and :url as appropriate."
  [url & [req]]
  (request (merge req {:method :post :url url})))
