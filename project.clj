(defproject clj-facebook-graph "0.3.0-SNAPSHOT"
  :description "A Clojure client for the Facebook Graph API."
  :dependencies [[org.clojure/clojure "1.2.1"]
                 [org.clojure/clojure-contrib "1.2.0"]
                 [ring/ring-core "0.3.5"]
                 [clj-http "0.1.3"]
                 [clj-oauth2 "0.0.1-SNAPSHOT"]]
  :dev-dependencies [[swank-clojure "1.2.1"]
                     [ring/ring-jetty-adapter "0.3.5"]
                     [ring/ring-devel "0.3.5"]
                     [ring/ring-jetty-adapter "0.3.5"]
                     [compojure "0.5.3"]]
  :aot [clj-facebook-graph.FacebookGraphException])
