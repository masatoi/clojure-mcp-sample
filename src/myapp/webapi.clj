(ns myapp.webapi
  (:require [myapp.core :as core]
            [myapp.db :as db]
            [myapp.schema :as schema]
            [reitit.ring :as ring]
            [reitit.ring.middleware.parameters :as parameters]
            [reitit.ring.middleware.muuntaja :as muuntaja]
            [reitit.coercion.malli :as malli]
            [reitit.ring.coercion :as coercion]
            [muuntaja.core :as m]
            [ring.adapter.jetty :as jetty]
            [ring.util.codec]))

(defn- build-paging-url [request direction cursor-id]
  (let [base-uri (str (-> request :uri))
        params (-> request :query-params
                   (dissoc "before" "after")
                   (assoc (name direction) (str cursor-id)))]
    (str base-uri "?" (ring.util.codec/form-encode params))))

(def app
  (ring/ring-handler
   (ring/router
    ["/api"
     ["/ping" (fn [_] {:status 200 :body "pong"})]
     ["/users"
      {:get {:summary "Get a paginated list of users"
             :parameters {:query [:map
                                  [:limit {:optional true} int?]
                                  [:before {:optional true} :uuid]
                                  [:after {:optional true} :uuid]]}
             :responses {200 {:body schema/UsersResponse}}
             :handler (fn [req]
                        (let [{:keys [before after limit]} (get-in req [:parameters :query])
                              limit (or limit 10)
                              direction (if before :backward :forward)
                              cursor-id (or before after)
                              results (db/find-paginated (db-val req)
                                                         {:entity-attr :user/id
                                                          :sort-attr :user/created-at
                                                          :limit (inc limit)
                                                          :cursor-id cursor-id
                                                          :direction direction})
                              items (take limit results)
                              has-more? (> (count results) limit)
                              paged-items (if (= direction :backward) (reverse items) items)
                              next-cursor (when (or (and (= direction :forward) has-more?)
                                                    (and (= direction :backward) (some? before)))
                                            (-> paged-items last :user/id))
                              prev-cursor (when (or (and (= direction :backward) has-more?)
                                                    (and (= direction :forward) (some? after)))
                                            (-> paged-items first :user/id))]
                          {:status 200
                           :body {:data (map #(dissoc % :db/id) paged-items)
                                  :paging {:next (when next-cursor (build-paging-url req :after next-cursor))
                                           :prev (when prev-cursor (build-paging-url req :before prev-cursor))}}}))};}
       :post {:summary "Create a new user"
              :parameters {:body schema/CreateUserRequest}
              :responses {201 nil?}
              :handler (fn [req]
                         (db/create-user! (db-conn req) (get-in req [:parameters :body]))
                         {:status 201})}}]
     ["/users/:id"
      {:parameters {:path [:map [:id :uuid]]}
       :get {:summary "Get a user by id"
             :responses {200 {:body schema/User}}
             :handler (fn [req]
                        (let [id (get-in req [:parameters :path :id])]
                          {:status 200
                           :body (db/find-user-by-id (db-val req) id)}))}
       :put {:summary "Update a user"
             :parameters {:body schema/UpdateUserRequest}
             :responses {200 nil?}
             :handler (fn [req]
                        (let [id (get-in req [:parameters :path :id])
                              body (get-in req [:parameters :body])]
                          (db/update-user! (db-conn req) id body)
                          {:status 200}))}
       :delete {:summary "Delete a user"
                :responses {204 nil?}
                :handler (fn [req]
                           (let [id (get-in req [:parameters :path :id])]
                             (db/delete-user! (db-conn req) id)
                             {:status 204}))}}]]
    {:data {:muuntaja m/instance
            :coercion malli/coercion
            :middleware [parameters/parameters-middleware
                         muuntaja/format-middleware
                         coercion/coerce-response-middleware
                         coercion/coerce-request-middleware
                         db-middleware]}})))

(def app
  (ring/ring-handler
   (ring/router
    ["/api"
     ["/ping" (fn [_] {:status 200 :body "pong"})]
     ["/users"
      {:get {:handler (fn [req]
                        {:status 200
                         :body (db/find-all-users (db-val req))})}
       :post {:handler (fn [req]
                         (db/create-user! (db-conn req) (:body-params req))
                         {:status 201})}}]
     ["/users/:id"
      {:get {:handler (fn [req]
                        (let [id (java.util.UUID/fromString (get-in req [:path-params :id]))]
                          {:status 200
                           :body (db/find-user-by-id (db-val req) id)}))}
       :put {:handler (fn [req]
                        (let [id (java.util.UUID/fromString (get-in req [:path-params :id]))
                              body (:body-params req)]
                          (db/update-user! (db-conn req) id body)
                          {:status 200}))}
       :delete {:handler (fn [req]
                           (let [id (java.util.UUID/fromString (get-in req [:path-params :id]))]
                             (db/delete-user! (db-conn req) id)
                             {:status 204}))}}]]
    {:data {:muuntaja m/instance
            :coercion malli/coercion
            :middleware [parameters/parameters-middleware
                         muuntaja/format-middleware
                         coercion/coerce-response-middleware
                         coercion/coerce-request-middleware
                         db-middleware]}})))

(defonce ^:private server* (atom nil))

(defonce server* (atom nil))

(defn- db-middleware [handler]
  (fn [request]
    (handler (assoc request :db-conn db/conn))))

(defn- db-val [request]
  @(get request :db-conn))

(defn- db-conn [request]
  (get request :db-conn))

(defn start-server
  ([] (start-server 8080))
  ([port]
   (let [srv (jetty/run-jetty app {:port port :join? false})]
     (reset! server* srv)
     srv)))

(defn stop-server
  ([]
   (when-let [srv @server*]
     (.stop srv)
     (reset! server* nil)
     :stopped))
  ([srv]
   (if (some? srv)
     (do (.stop srv)
         (when (= @server* srv) (reset! server* nil))
         :stopped)
     (stop-server))))

(comment)
  ;; REPL usage:
  ;; Restart your REPL to pick new deps, then:
  ;; (require 'myapp.webapi :reload)
  ;; ;; Handler-only checks
  ;; (app {:request-method :get :uri "/fibonacci"
  ;;       :headers {"accept" "application/json"}
  ;;       :query-params {"n" "10"}})
  ;; => {:status 200, :body {:value 55}, ...}
  ;; ;; Start/stop Jetty
  ;; (def s (start-server 8080))
  ;; curl 'http://localhost:8080/fibonacci?n=10' -> {"value":55}
  ;; (stop-server s)

