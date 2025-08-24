(ns myapp.webapi-unit-test
  (:require [clojure.test :refer :all]
            [myapp.webapi :as webapi]))

(defn- slurp-body [resp]
  (when-let [b (:body resp)]
    (slurp b)))

(deftest app-fibonacci-ok
  (let [resp (webapi/app {:request-method :get
                          :uri "/fibonacci"
                          :headers {"accept" "application/json"}
                          :query-params {"n" "10"}})]
    (is (= 200 (:status resp)))
    (is (= "{\"value\":55}" (slurp-body resp)))))

(deftest app-fibonacci-invalid
  ;; invalid n -> 400
  (let [resp (webapi/app {:request-method :get
                          :uri "/fibonacci"
                          :headers {"accept" "application/json"}
                          :query-params {"n" "-1"}})]
    (is (= 400 (:status resp))))
  ;; missing n -> 400
  (let [resp (webapi/app {:request-method :get
                          :uri "/fibonacci"
                          :headers {"accept" "application/json"}})]
    (is (= 400 (:status resp)))))

