# clj-facebook-graph

clj-facebook-graph is a simple Clojure client for the Facebook Graph
API based on [clj-http](https://github.com/getwoven/clj-http) and
[Ring](https://github.com/mmcgrana/ring). It offers some convenience
when you are working with the Facebook Graph API. Furthermore
clj-facebook-graph provides a simple authentication flow in the form
of some Ring middleware.

A tutorial can be found here:
[http://max-weber.tumblr.com/post/4183433816/clj-facebook-graph](http://max-weber.tumblr.com/post/4183433816/clj-facebook-graph)

The project is at an early stage, but feel free to extend it and use
it as foundation for your Facebook integration. Furthermore only
reading from the Facebook Graph API is supported at the
moment. Nevertheless you can simply use the clj-facebook-graph client
to do HTTP posts to write something to the Facebook Graph API, because
it is just a normal clj-http client. In general clj-facebook-graph
never tries to hide the restful HTTP nature of the Facebook Graph
API. It only provides some convenience for repeating tasks like adding
the access token to every request URL. You can simply use all other
features of the Facebook Graph API, even when they are not documented
in clj-facebook-graph. For further reading please consider the offical
Facebook Graph API documentation:

[http://developers.facebook.com/docs/api/](http://developers.facebook.com/docs/api/)

[http://developers.facebook.com/docs/authentication/](http://developers.facebook.com/docs/authentication/)

[http://developers.facebook.com/docs/](http://developers.facebook.com/docs/)

## Usage

The clj-facebook-graph library offers the following amenities when you
work with the Facebook Graph API:

For each and every request the Facebook Graph API expects that you
append an access token as query parameter to the request URL. The
clj-facebook-graph library takes care of it automatically when you use
the clj-facebook-graph.auth/with-facebook-auth macro:

    (use 'clj-facebook-graph.auth)
    (require '(clj-facebook-graph [client :as client]))

    (def facebook-auth {:access-token "fill in an access token here,
    read on to find out how to receive one with clj-facebook-graph."})

    (with-facebook-auth facebook-auth 
      (client/get "https://graph.facebook.com/me/friends"))

The code lines above will return a list of all your ("/me/")
Facebook friends, if you have received an access token with your
Facebook credentials. The access token is appended to the URL
automatically, if you provide it in a map (like the facebook-auth
above) to the with-facebook-auth
macro. [clj-http](http://github.com/clj-sys/clj-http) uses the
[HttpComponents Client](http://hc.apache.org/) to do the HTTP requests
and add a Ring-style architecture on top of it. For this reason
clj-http can be extended through Ring-style middleware. The
clj-facebook-graph.auth/wrap-facebook-access-token middleware takes
care that the access_token query parameter with the supplied access
token is added to the request URL.

So you can nearly do nothing with the Facebook Graph API when you
don't have an access token to authorize your requests. This is why
clj-facebook-graph have several Ring middleware handlers that realize a
simple Facebook authentication flow. Take a look at
clj-facebook-graph.example in the test source folder. There you can
find a full blown example how to use clj-facebook-graph to do an
authentication via Facebook to receive an access token. Furthermore
some hints are provided how to extend clj-facebook-graph. Pay attention
that on first-time use you have to invoke "lein compile" to compile
the clj_facebook_graph.FacebookGraphException ahead-of-time, otherwise
you will get a ClassNotFoundException for this class. In order to
use it you have to provide some information about your Facebook app in
the clj_facebook_graph.example.clj:

    (defonce facebook-app-info
      {:client-id "your Facebook app id"
       :client-secret "your Facebook app's secret"
       :redirect-uri "http://localhost:8080/facebook-callback"
       :scope  ["user_photos" "friends_photos"]})

Fill in the app id (client-id) and secret (client-secret) of your
[Facebook app](http://www.facebook.com/developers/apps.php).

Pay attention that the settings of your Facebook app contains the site
location "http://localhost/" and the website domain "localhost",
otherwise Facebook will not accept your request, when your web
application runs locally.

To start the Jetty server in the example, evaluate the line (def
server (start-server)) in your REPL under the
clj-facebook-graph.example namespace.

clj-facebook-graph have even more conveniences to offer:

Instead of writing:

    (with-facebook-auth facebook-auth (client/get "https://graph.facebook.com/me/friends"))

You can simply write:

    (with-facebook-auth facebook-auth (client/get [:me :friends]))

It doesn't matter if :me or :friends is a keyword or a string, you can
use both or even mix it. This shortcut works for every part of the
[Facebook Graph API](http://developers.facebook.com/docs/api/).


The Facebook Graph API is a restful web service which almost always
returns a JSON document as body of the response. So clj-facebook-graph
automatically converts the JSON body into a Clojure data structure
(clojure.data.json/read-json), if the Content-Type is
"text/javascript". The Facebook Graph API uses "text/javascript"
instead of the more precise mime type "application/json".

The Facebook Graph API mostly returns a JSON document in the form like
this one:
    {
        \"data\": [...]
    }

So instead of extracting the data part of the body manual each an
every time, you can simply write the following:

    (with-facebook-auth facebook-auth (client/get [:me :friends]
                          {:extract :data}))

This also works for the paging and other parts of the
response. Besides clj-facebook-graph also supports to handle the
pagination of an response automatically:

    (take 5 (with-facebook-auth facebook-auth 
             (client/get [:me :home] {:query-params {:limit 5} :extract :data :paging true})))

The above code only fetchs five items at a time from your Facebook
news feed. It's a lazy seq so if you would take 6 items another
request would be triggered to the next page URL.

As you can see the :query-params key is used to specify extra parameters for the request. Here's
another example that specifies the fields we want Facebook to return. (Note this example requires
extra permissions, you can't query friend's birthdays by default.)

    (with-facebook-auth facebook-auth
      (client/get [:me :friends] {:query-params {:fields "link,birthday,name,picture"} :extract :data}))))

If your app uses the Facebook canvas feature you will often need to make sense of the signed_request
parameter that Facebook hides the user's information inside. No problems, pull it out of the request and
pass it to the clj-facebook-graph.auth/decode-signed-request function. It returns a map of payload
or nil if the signiture was wrong.

    (use 'clj-facebook-graph.auth)
    (decode-signed-request signed_request "Facebook secret key")

### Posting something to the Facebook Graph API

clj-facebook-graph has a basic support to do HTTP post against the
Facebook Graph API. So for example let's post something onto your own
wall/feed. The Facebook Graph API documentation for this use case can
be found
[here](http://developers.facebook.com/docs/reference/api/post/) The
variable example-wall-post under the namespace
clj-facebook-graph.example is a
equivalent Clojure map to the curl example on the documentation
page. To post this wall entry you invoke the following in the
clj-facebook-graph.example namespace:

    (with-facebook-auth facebook-auth 
                        (client/post [:me :feed] 
                          {:form-params example-wall-post}))

Pay attention to the fact that your Facebook application needs the
"publish_stream" permission to do this post request. You have to add
the "publish_stream" permission to the :permissions vector in the your
facebook-app-info. For the clj-facebook-graph.example this is already
the case.

### FQL

clj-facebook-graph can also perform FQL queries. See Facebook's [FQL
documentation](http://developers.facebook.com/docs/reference/fql/)
to learn how to write a FQL query. Let's take the example FQL query
from the documentation link:

     SELECT name FROM user WHERE uid = me() 

To perform this FQL query with clj-facebook-graph invoke:

     (with-facebook-auth facebook-auth
                         (client/get :fql 
                              {:fql "SELECT name FROM user WHERE uid = me()"}))

The clj-facebook-graph middleware instructs the Facebook FQL API to
return the response as JSON, which are converted to the corresponding
Clojure data struture automatically.

## Installation

This project is built with Leiningen and prepared for use with Swank
Clojure. Pay attention that on first-time use you have to invoke 
"lein compile" to compile the clj_facebook_graph.FacebookGraphException
ahead-of-time, otherwise you will get a ClassNotFoundException for
this class.

## License

Copyright (C) 2011 Maximilian Weber

Distributed under the Eclipse Public License, the same as Clojure.
