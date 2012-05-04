(defproject clj-facebook-graph "0.4.0"
  :description "A Clojure client for the Facebook Graph API."
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [org.clojure/data.json "0.1.2"  :exclude [org.clojure/clojure]]
                 [ring/ring-core "1.0.1"  :exclude [org.clojure/clojure]]

                 [clj-http "0.3.6"]
                 [clj-oauth2 "0.2.0"]]

  :dev-dependencies [[ring/ring-devel "1.0.1"  :exclude [org.clojure/clojure]]
                     [ring/ring-jetty-adapter "1.0.1"  :exclude [org.clojure/clojure]]
                     [compojure "1.0.1"  :exclude [org.clojure/clojure]]]

  :repositories {"jboss" "http://repository.jboss.org/nexus/content/groups/public/"}

  :aot [clj-facebook-graph.FacebookGraphException])
