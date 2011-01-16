; Copyright (c) Maximilian Weber. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file epl-v10.html at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns clj-facebook-graph.helper
  "Some helper functions."
  (:use [clojure.contrib.json :only [read-json]]
        [clojure.java.io :only [reader]]
        [clj-http.client :only [generate-query-string]]
        [clj-http.client :only [unexceptional-status?]])
  (:use ring.middleware.params))

(defn parse-params
  "Transforms the query parameters of an URL into a map of parameter value pairs (both are strings)."
  [params]
  (let [f (ns-resolve 'ring.middleware.params 'parse-params)]
    (f params "UTF-8")))

(defn read-json-body
  "Reads a JSON document from a request body."
  [request]
  (let [body (:body request)]
    (read-json (if (string? body) body (reader body)))))

(defn build-url [request]
  "Builds a URL string which corresponds to the information of the request."
  (let [{:keys [server-port server-name uri query-string scheme]} request]
    (str (name scheme) "://" server-name (when server-port (str ":" server-port)) uri
         (when query-string "?") query-string)))

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
