# clj-facebook-graph

clj-facebook-graph is a simple Clojure client for the Facebook Graph API based on clj-http and Ring. It offers some convenience when you are working with the Facebook Graph API. Furthermore clj-facebook-graph provides a simple authentication flow in the form of some Ring middleware.

The project is at an early stage, but feel free to extend it and use it as foundation for your Facebook integration.

## Usage

See the source code for documentation, a detailed introduction will follow. In the meanwhile take a look at clj-facebook-graph.example in the test source folder. There you can find a full blown example how to use clj-facebook and some hints how to extend it. To use it you have to provide some information about your Facebook app in the clj_facebook_graph.example.clj:

    (defonce facebook-app-info {:client-id "your Facebook app id"
                            :client-secret "your Facebook app's secret"
                            :redirect-uri "http://localhost:8080/facebook-callback"
                            :permissions  ["user_photos" "friends_photos"]})

Fill in the app id (client-id) and secret (client-secret) of your Facebook app (http://www.facebook.com/developers/apps.php).

Pay attention that the settings of your Facebook app contains the site location "http://localhost/" and the website domain "localhost", otherwise Facebook will not accept your request, when your web application runs locally.

To start the Jetty server in the example, evaluate the line (def server (start-server)) in your REPL under the clj-facebook-graph.example namespace.

## Installation

This project is built with Leiningen and prepared for use with Swank Clojure.

## License

Copyright (C) 2011 Maximilian Weber

Distributed under the Eclipse Public License, the same as Clojure.
