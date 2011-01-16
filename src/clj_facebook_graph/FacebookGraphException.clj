; Copyright (c) Maximilian Weber. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file epl-v10.html at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns clj-facebook-graph.FacebookGraphException
  (:gen-class
   :extends java.lang.Exception
   :implements [clojure.lang.IDeref]
   :state state
   :init init
   :constructors {[clojure.lang.Associative] [String]}
   :methods [[getError [] clojure.lang.Associative]]))

(defn -init
  ([error]
     [[(if-let [message (:message error)] message (str error))] error]))

(defn -deref
  [this]
  (.state this))

(defn -getError
  [this]
  (.state this))
