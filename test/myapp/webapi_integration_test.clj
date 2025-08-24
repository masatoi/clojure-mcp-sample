(ns myapp.webapi-integration-test
  (:require [clojure.test :refer :all]
            [myapp.webapi :as webapi])
  (:import [java.net URI]
           [java.net.http HttpClient HttpRequest HttpResponse]))

(defonce ^HttpClient client (HttpClient/newHttpClient))
(def ^:private server* (atom nil))
(def ^:private base-url* (atom nil))

(defn- server-port [server]
  (let [connectors (.getConnectors server)
        c (first (seq connectors))]
    (.getLocalPort c)))

(defn with-server [f]
  (let [srv (webapi/start-server 0)
        port (server-port srv)
        url (str "http://localhost:" port)]
    (reset! server* srv)
    (reset! base-url* url)
    (try
      (f)
      (finally
        (webapi/stop-server srv)
        (reset! server* nil)
        (reset! base-url* nil)))))

(use-fixtures :each with-server)

(defn- http-get [path]
  (let [url (str @base-url* path)
        builder (HttpRequest/newBuilder (URI. url))
        req (.build (.GET (.header builder "accept" "application/json")))
        resp (.send client req (java.net.http.HttpResponse$BodyHandlers/ofString))]
    {:status (.statusCode resp)
     :body (.body resp)}))

(deftest http-fibonacci-ok
  (let [{:keys [status body]} (http-get "/fibonacci?n=10")]
    (is (= 200 status))
    (is (= "{\"value\":55}" body))))

(deftest http-fibonacci-invalid
  (let [{:keys [status]} (http-get "/fibonacci?n=-1")] 
    (is (= 400 status)))
  (let [{:keys [status]} (http-get "/fibonacci")] 
    (is (= 400 status))))

