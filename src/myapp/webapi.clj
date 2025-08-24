(ns myapp.webapi
  (:require [myapp.core :as core]
            [reitit.ring :as ring]
            [reitit.ring.middleware.parameters :as parameters]
            [reitit.ring.middleware.muuntaja :as muuntaja]
            [reitit.coercion.malli :as malli]
            [reitit.ring.coercion :as coercion]
            [muuntaja.core :as m]
            [ring.adapter.jetty :as jetty]))

(def app
  (let [router
        (ring/router
         [["/fibonacci"
           {:get {:parameters {:query [:map
                                       [:n pos-int?]]}
                  :responses {200 {:body [:map [:value pos-int?]]}
                              400 {:body [:map [:error string?]]}}
                  :handler (fn [{{{:keys [n]} :query} :parameters}]
                             (let [value (core/fibonacci-memo n)]
                               {:status 200
                                :body {:value value}}))}}]]
         {:data {:coercion malli/coercion
                 :muuntaja m/instance
                 :middleware [parameters/parameters-middleware
                              muuntaja/format-negotiate-middleware
                              muuntaja/format-request-middleware
                              muuntaja/format-response-middleware
                              coercion/coerce-exceptions-middleware
                              coercion/coerce-request-middleware
                              coercion/coerce-response-middleware]}})]
    (ring/ring-handler router (ring/create-default-handler))))

(defonce ^:private server* (atom nil))

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

