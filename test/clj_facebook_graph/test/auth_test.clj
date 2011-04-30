(ns clj-facebook-graph.test.auth-test
  (:use [clj-facebook-graph.auth])
  (:use [clojure.test]))

(deftest hmac-sha-256-test
  (let [example "Mvq8XaiwC0gceHL5+4GF0xBeYZJu1uWk7bX5WQ6mvQ8="]
    (is (= example (hmac-sha-256 "test" "example message")) "Hashes correctly")))

(deftest strtr-test
  (is (= "dave/bob+test" (strtr "dave_bob-test" "-_" "+/")) "strtr works as we use it.")
  (is (= "fr3d-jam3s" (strtr "fred james" " e" "-3")) "strtr works for other values")
  (is (= "dave" (strtr "dave" "3" "1")) "strtr doesn't mess with other things"))

(deftest decode-signed-request-test
  (let [example "vlXgu64BQGFSQrY0ZcJBZASMvYvTHu9GQ0YM9rjPSso.eyJhbGdvcml0aG0iOiJITUFDLVNIQTI1NiIsIjAiOiJwYXlsb2FkIn0"]
    (is (= {:algorithm "HMAC-SHA256" :0 "payload"} (decode-signed-request example "secret"))
        "Decodes correctly signed payload")
    (is (= nil (decode-signed-request example "wrong-secret"))
        "Discards payload with wrong signiture")))