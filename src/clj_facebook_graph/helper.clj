; Copyright (c) Maximilian Weber. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file epl-v10.html at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns clj-facebook-graph.helper
  "Some helper functions."
  (:use [clojure.contrib.json :only [read-json read-json-from Read-JSON-From]]
        [clojure.java.io :only [reader]]
        [clj-http.client :only [generate-query-string]]
        [clj-http.client :only [unexceptional-status?]]
        [clojure.string :only [blank?]]
        [clj-facebook-graph.auth :only [with-facebook-auth]]
        ring.middleware.params)
  (:require [clj-facebook-graph.client :as client])
  (:import
   (java.io PushbackReader ByteArrayInputStream InputStreamReader)))

(def facebook-base-url "https://graph.facebook.com")

(def facebook-fql-base-url "https://api.facebook.com/method/fql.query")

(defn parse-params
  "Transforms the query parameters of an URL into a map of parameter value pairs (both are strings)."
  [params]
  (let [f (ns-resolve 'ring.middleware.params 'parse-params)]
    (f params "UTF-8")))

(extend-type (Class/forName "[B")
  Read-JSON-From
   (read-json-from [input keywordize? eof-error? eof-value]
                   (read-json-from (PushbackReader. (InputStreamReader.
                                                      (ByteArrayInputStream. input)))
                    keywordize? eof-error? eof-value)))

(defn build-url [request]
  "Builds a URL string which corresponds to the information of the request."
  (let [{:keys [server-port server-name uri query-string scheme]} request]
    (str (name scheme) "://" server-name (when server-port (str ":" server-port)) uri
         (when (not (blank? query-string)) "?") query-string)))

(defn request-to-url
  "Transforms a clj-http request with its query parameters into a full URL."
  [request]
  (let [{:keys [url query-params]} request]
    (if (seq query-params)
      (str url "?" (generate-query-string query-params))
      url)))

(defn wrap-exceptions [client]
  "An alternative Ring-style middleware to the #'clj-http.core/wrap-exceptions. This
   one also extracts the body from the http response, which is very helpful to see
   the error description Facebook has returned as JSON document."
  (fn [req]
    (let [{:keys [status body] :as resp} (client req)]
      (if (or (not (clojure.core/get req :throw-exceptions true))
              (unexceptional-status? status))
        resp
        (throw (Exception. (str "Status: " status " body: " (slurp body))))))))

(defn wrap-print-request-map [client]
  "Simply prints the request map to *out*."
  (fn [req]
    (println req)
    (client req)))

(defn extract-facebook-auth [session]
  (:facebook-auth (val session)))

(defn facebook-auth-user
  "Get all friends of the current (logged in facebook) user."
  [facebook-auth]
  (with-facebook-auth facebook-auth (client/get [:me] {:extract :body})))

(defn facebook-auth-by-name
  "Take all sessions from the session-store and extracts the facebook-auth
   information. Finally a map is created where the user's Facebook name is
   associated with his current facebook-auth (access-token)."
  [session-store]
  (first (map #(let [facebook-auth (extract-facebook-auth %)
                     user-name (:name (facebook-auth-user facebook-auth))]
                 (identity {user-name
                            facebook-auth}))
              @session-store)))

(defmacro with-facebook-auth-by-name
  "Uses the informations created by #'facebook-auth-by-name to provide a
   comfortable way to query the Facebook Graph API on the REPL by using
   a Facebook name of a current logged in user.
   Imagine you want to play around a little bit with the Facebook
   Graph API on the REPL and your Facebook name is 'Max Mustermann'.
   Then you log in to Facebook through the .../facebook-login URL.
   Afterwards the facebook-auth (access token) information corresponding
   to your Facebook account is associated with the corresponding HTTP
   session. Now you can simply do the following on the REPL:

   (with-facebook-auth-by-name \"Max Mustermann\" (fb-get [:me :friends]))

   to list all your Facebook friends. Thereby an annoying manual lookup of
   the corresponding access-token is avoided."
  [session-store name & body]
  `(let [current-fb-users# (facebook-auth-by-name ~session-store)]
    (with-facebook-auth (current-fb-users# ~name) ~@body)))
