(ns clj-facebook-graph.auth
  (:use [clj-facebook-graph.helper :only [parse-params request-to-url wrap-exceptions
                                          facebook-base-url]]
        [clj-http.client :only [wrap-request]]
        [clojure.contrib.json :only [read-json]])
  (:require [clj-http.client :as client]
            [clojure.string :as str])
  (:import [org.apache.commons.codec.binary Base64]
           [javax.crypto Mac]
           [javax.crypto.spec SecretKeySpec]))

(def facebook-auth-base-url "https://graph.facebook.com/oauth/authorize")

(defn facebook-auth-url
  "Assembles the URL (a map for a clj-http request) to redirect your user to the Facebook login
   and authorisation page. See http://developers.facebook.com/docs/authentication/#web_server_auth for details.
   You need your Facebook app id (client-id) and the URL of your webapp,
   where Facebook should redirect your user after he has successfully logged
   in with his Facebook credentials. Furthermore you can provide a list with
   the additionally permissions that your Facebook app needs. See
   http://developers.facebook.com/docs/authentication/permissions for details."
  ([client-id redirect-uri permissions]
     {:method :get
      :url facebook-auth-base-url
      :query-params {:client_id client-id
                     :redirect_uri redirect-uri
                     :scope (apply str (interpose "," permissions))}})
  ([facebook-app-info]
     (let [{:keys [client-id redirect-uri permissions]} facebook-app-info]
       (request-to-url (facebook-auth-url client-id redirect-uri permissions)))))

(def facebook-auth-url-str
  (comp request-to-url facebook-auth-url))

(def facebook-access-token-base-url "https://graph.facebook.com/oauth/access_token")

(defn facebook-access-token-url
  "Assembles the URL to get an access token after you have
   received a code from Facebook via the redirect_uri. The
redirect_uri has to be the same as in the HTTP request to
https://graph.facebook.com/oauth/authorize. Besides the request_uri
and the received code you have to provide the app id (client-id) and
secret (client-secret) of your Facebook app.
 See http://developers.facebook.com/docs/authentication/#web_server_auth for details."
  ([client-id redirect-uri client-secret code]
     {:method :get
      :url facebook-access-token-base-url
      :query-params {:client_id client-id
                     :redirect_uri redirect-uri
                     :client_secret client-secret
                     :code code}}))

(def ^{:doc "clj-http client to fetch the access token"}
     request (wrap-request (wrap-exceptions #'clj-http.core/request)))

(defn extract-access-token
  "Extracts the access token from the response map."
  [response]
  (let [body (:body response)
       {access-token "access_token"} (parse-params body)]
        access-token))

(defn get-access-token
  "Fetchs the access token with the help of the clj-http client."
  ([client-id redirect-uri client-secret code]
     (let [req (facebook-access-token-url client-id redirect-uri client-secret code)
           access-token (extract-access-token (request req))]
       access-token)))

(defn with-query-params-access-token
  "Adds the Facebook access token to the query params of the request."
  [req access-token]
  (merge-with merge req {:query-params {:access_token access-token}}))

(defonce *facebook-auth* nil)

(defmacro with-facebook-auth
  "Binds the *facebook-auth* variable to the current thread scope. The *facebook-auth* variable is
   used by the Ring-style middleware for clj-http to append the access_token query parameter
   to the Facebook Graph API requests. For this reason the *facebook-auth* variable should be a
   map which includes the current access token as value under the key :access-token."
  [facebook-auth & body]
  `(binding [*facebook-auth* ~facebook-auth]
     ~@body))

(defn wrap-facebook-access-token [client]
  "Ring-style middleware to add the Facebook access token to the request, when it is found in the thread bounded *facebook-auth* variable.
   It doesn't add the access_token query parameter, if the URL doesn't start with the facebook-base-url (https://graph.facebook.com). So
   the clj-http client of clj-facebook-graph can also do other HTTP requests."
  (fn [req]
    (let [url (:url req)]
      (if (and *facebook-auth* (string? url) (.startsWith url facebook-base-url))
        (let [{:keys [access-token]} *facebook-auth*
              req (with-query-params-access-token req access-token)]
          (client req))
        (client req)))))

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
