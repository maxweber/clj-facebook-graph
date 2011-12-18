; Copyright (c) Maximilian Weber. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file epl-v10.html at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns clj-facebook-graph.error-handling
  (:use [clojure.data.json])
  (:import clj_facebook_graph.FacebookGraphException))

(def
 ^{:doc "A map with some interesting Facebook errors that are
 assigned to a keyword to identify the error easier. At the moment the
 map only include two errors which are relevant for authentication and authorisation."}
 facebook-errors {
                  :OAuthException {"Error validating access token."
                                   :invalid-access-token
                                   "An access token is required to request this resource."
                                   :access-token-required
                                   }
                  })

(defn identify-facebook-error
  "Tries to identify the Facebook error in the response with the help
   of the #'facebook-errors map (the json response must already be converted
   into a Clojure data structure at this point). If the error is not included
   in the facebook-errors map the keyword :unknown is set as error type. The
   error is returned in a form like this one:

   {:error [:OAuthException :invalid-access-token] :message \"Error validation access token\"}"
  [response] (let [{:keys [type message]} (get-in response [:body :error])
                   error-type (keyword type)]
               {:error
                [error-type (if-let [error (get-in facebook-errors [error-type message])]
                              error :unknown)]
                :message message}))

(defn wrap-facebook-exceptions
  "The ring-style middleware to detect Facebook errors in the response of
   a clj-http request. At the moment Facebook always return the
   HTTP status code 400 (Bad Request) in case of an error."
  [client]
  (fn [req]
    (let [{:keys [status] :as resp} (client req)]
      (if (or (not (clojure.core/get req :throw-exceptions true))
              (not= status 400))
        resp
        (throw (let [resp (assoc resp :body (read-json (:body resp)))]
                 (FacebookGraphException. (identify-facebook-error resp))))))))

