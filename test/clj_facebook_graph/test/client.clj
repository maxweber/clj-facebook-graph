(ns clj-facebook-graph.test.client
  (:use clojure.test)
  (:require [clj-facebook-graph.client :as client]
            [clj-http.client :as http]
            [ring.util.codec]))

(deftest wrap-facebook-data-extractor-test
  (testing "data from this request should be returned even if no pagination links are in the response"
    (let [data "DATA"
          fb-response (constantly {:body {:data data}})
          client (client/wrap-facebook-data-extractor fb-response)
          request {:paging true :extract :data}
          response (client request)]
      (is (= response data))))

  (testing "paging params should be mapped to :query-params"
    (let [pagination-params {"since" "123"}
          pagination-url (str "http://localhost?" (ring.util.codec/form-encode pagination-params))
          data [1 2 3]
          lazy-req-params (atom nil)
          fb-response (fn [req] (reset! lazy-req-params req) {:body {:data data :paging {:next pagination-url}}})
          client (client/wrap-facebook-data-extractor fb-response)
          request {:paging true :extract :data}
          response (dorun (take 4 (client request)))]
      (is (= (:query-params @lazy-req-params) pagination-params)))))

(deftest middleware-interaction-test
  (testing "query params in :url are overridden by :query-params"
    (let [query-params {"q" "1"}
          query-string "q=12345"
          client (-> identity
                     http/wrap-query-params
                     http/wrap-url)
          response (client {:url (str "http://localhost/?" query-string) :query-params query-params})]
      (is (= (ring.util.codec/form-decode (:query-string response)) query-params)))))
