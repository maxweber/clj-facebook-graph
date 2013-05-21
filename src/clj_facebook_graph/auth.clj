(ns clj-facebook-graph.auth
  (:use [clj-facebook-graph.helper :only [facebook-base-url facebook-fql-base-url]] 
        [clojure.data.json :only [read-json]])
  (:require [clj-oauth2.client :as oauth2]
            [clojure.string :as str])
  (:import [org.apache.commons.codec.binary Base64]
           [javax.crypto Mac]
           [javax.crypto.spec SecretKeySpec]))

(def facebook-oauth2-endpoint
  {:access-query-param :access_token
   :authorization-uri "https://graph.facebook.com/oauth/authorize"
   :access-token-uri "https://graph.facebook.com/oauth/access_token"
   :grant-type "authorization_code"})

(defn get-access-token
  "Fetches the access token using clj-oauth2/client."
  [facebook-app-info params & [auth-req]]
  (:access-token (oauth2/get-access-token
                  (merge facebook-oauth2-endpoint facebook-app-info)
                  params
                  auth-req)))


(defn make-auth-request [facebook-app-info]
  (oauth2/make-auth-request (merge facebook-oauth2-endpoint facebook-app-info)))

(defonce ^:dynamic *facebook-auth* nil)

(defmacro with-facebook-auth
  "Binds the *facebook-auth* variable to the current thread scope. The *facebook-auth* variable is
   used by the Ring-style middleware for clj-http to append the access_token query parameter
   to the Facebook Graph API requests. For this reason the *facebook-auth* variable should be a
   map which includes the current access token as value under the key :access-token."
  [facebook-auth & body]
  `(binding [*facebook-auth* ~facebook-auth]
     ~@body))

(defn- oauth2-access-token []
  (assoc *facebook-auth*
    :query-param (:access-query-param facebook-oauth2-endpoint)))

(defn wrap-facebook-access-token [client]
  "Ring-style middleware to add the Facebook access token to the request, when it is found in the thread bounded *facebook-auth* variable.
   It doesn't add the access_token query parameter, if the URL doesn't start with the facebook-base-url (https://graph.facebook.com). So
   the clj-http client of clj-facebook-graph can also do other HTTP requests."
  (fn [req]
    (let [url (:url req)]
      (if (and *facebook-auth* (string? url)
               (or (.startsWith url facebook-base-url)
                   (.startsWith url facebook-fql-base-url)))
        (client (do
                  (-> req
                      (assoc :oauth2 (oauth2-access-token))
                      (assoc-in [:query-params :access_token] (:access-token *facebook-auth*)))))
        (client req)))))

(defn with-facebook-access-token [uri]
  (oauth2/with-access-token uri (oauth2-access-token)))

(defn hmac-sha-256
  "Returns a HMAC-SHA256 hash of the provided data."
  [^String key ^String data]
  (let [hmac-key (SecretKeySpec. (.getBytes key) "HmacSHA256")
        hmac (doto (Mac/getInstance "HmacSHA256") (.init hmac-key))]
    (String. (org.apache.commons.codec.binary.Base64/encodeBase64
              (.doFinal hmac (.getBytes data)))
             "UTF-8")))

(defn base64-decode
  "Decodes a base64 string, convenience wrapper around Java library."
  [base64]
  (String. (Base64/decodeBase64 base64)))

(defn strtr
  "My take on PHP's strtr function."
  [value from to]
  ((apply comp (map (fn [a b] #(.replace % a b)) from to))
   value))

(defn decode-signed-request
  "Takes a Facebook signed_request parameter and the applications secret
  key and returns a payload hash or nil if there was a problem."
  [signed-request key]
  (when (and signed-request key
             (re-matches #"^[^\.]+\.[^\.]+$" signed-request))
    (let [[signiture payload] (str/split signed-request #"\.")
          signiture (str (strtr signiture "-_" "+/") "=")]
      (when (= signiture (hmac-sha-256 key payload))
        (read-json (base64-decode payload))))))

(defn extract-facebook-auth [session]
  (:facebook-auth (val session)))

(defn facebook-auth-user
  "Get the basic facebook user data for the given facebook-auth (access_token)."
  [client-get facebook-auth]
  (with-facebook-auth facebook-auth (client-get [:me] {:extract :body})))

(defn facebook-auth-by-name
  "Take all sessions from the session-store and extracts the facebook-auth
   information. Finally a map is created where the user's Facebook name is
   associated with his current facebook-auth (access-token)."
  [client-get session-store]
  (first (map #(let [facebook-auth (extract-facebook-auth %)
                     user-name (:name (facebook-auth-user client-get facebook-auth))]
                 (identity {user-name
                            facebook-auth}))
              session-store)))

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
  [client-get session-store name & body]
  `(let [current-fb-users# (facebook-auth-by-name ~client-get ~session-store)]
    (with-facebook-auth (current-fb-users# ~name) ~@body)))
