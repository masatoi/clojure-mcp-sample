(ns myapp.webapi-integration-test
  (:require [clojure.test :refer :all]
            [myapp.webapi :as webapi]
            [cheshire.core :as json])
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

(defn- http-post [path body]
  (let [req (-> (HttpRequest/newBuilder)
                (.uri (URI/create (str @base-url* path)))
                (.header "Content-Type" "application/json")
                (.POST (HttpRequest$BodyPublishers/ofString (json/encode body)))
                (.build))]
    (.send @client req (HttpResponse$BodyHandlers/ofString))))

(defn- http-put [path body]
  (let [req (-> (HttpRequest/newBuilder)
                (.uri (URI/create (str @base-url* path)))
                (.header "Content-Type" "application/json")
                (.PUT (HttpRequest$BodyPublishers/ofString (json/encode body)))
                (.build))]
    (.send @client req (HttpResponse$BodyHandlers/ofString))))

(defn- http-delete [path]
  (let [req (-> (HttpRequest/newBuilder)
                (.uri (URI/create (str @base-url* path)))
                (.DELETE)
                (.build))]
    (.send @client req (HttpResponse$BodyHandlers/ofString))))

(deftest user-api-crud-test
  (testing "CREATE user"
    (let [create-response (http-post "/api/users" {:user/name "Test User" :user/age 30})
          _ (is (= 201 (.statusCode create-response)))
          users-response (http-get "/api/users")
          _ (is (= 200 (.statusCode users-response)))
          users (-> users-response .body (json/decode true) first)
          user-id (-> users first :user/id)]

      (is (= "Test User" (-> users first :user/name)))

      (testing "READ user"
        (let [user-response (http-get (str "/api/users/" user-id))]
          (is (= 200 (.statusCode user-response)))
          (is (= "Test User" (-> user-response .body (json/decode true) :user/name)))))

      (testing "UPDATE user"
        (let [update-response (http-put (str "/api/users/" user-id) {:user/age 31})]
          (is (= 200 (.statusCode update-response)))
          (let [user-response (http-get (str "/api/users/" user-id))]
            (is (= 31 (-> user-response .body (json/decode true) :user/age))))))

      (testing "DELETE user"
        (let [delete-response (http-delete (str "/api/users/" user-id))]
          (is (= 204 (.statusCode delete-response)))
          (let [users-response (http-get "/api/users")]
            (is (= "[]" (.body users-response)))))))))

