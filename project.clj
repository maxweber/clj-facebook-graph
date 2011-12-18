(defproject clj-facebook-graph "0.4.0"
  :description "A Clojure client for the Facebook Graph API."
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [org.clojure/data.json "0.1.1"]
                 [ring/ring-core "1.0.1"]
                 [clj-http "0.2.6"]
                 [clj-oauth2 "0.1.0"]]
  :dev-dependencies [[ring/ring-devel "1.0.1"]
                     [ring/ring-jetty-adapter "1.0.0"]
                     [compojure "0.6.4"]]
  :aot [clj-facebook-graph.FacebookGraphException])
