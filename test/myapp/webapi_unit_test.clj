(ns myapp.webapi-unit-test
  (:require [clojure.test :refer :all]
            [myapp.webapi :as webapi]
            [myapp.db :as db]
            [datascript.core :as d]
            [cheshire.core :as json]))

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

(def test-conn)

(defn- with-test-db [f]
  (binding [test-conn (d/create-conn db/schema)]
    (f)))

(use-fixtures :each with-test-db)

(deftest user-handlers-test
  (let [app webapi/app]
    (testing "CREATE user"
      (let [create-resp (app {:request-method :post
                              :uri "/api/users"
                              :body-params {:user/name "Unit Test User" :user/age 40}
                              :db-conn test-conn})
            _ (is (= 201 (:status create-resp)))
            users (db/find-all-users @test-conn)
            user (-> users first first)
            user-id (:user/id user)]

        (is (= "Unit Test User" (:user/name user)))

        (testing "READ user"
          (let [read-resp (app {:request-method :get
                                :uri (str "/api/users/" user-id)
                                :path-params {:id (str user-id)}
                                :db-conn test-conn})
                read-user (-> read-resp :body (json/decode true))]
            (is (= 200 (:status read-resp)))
            (is (= "Unit Test User" (:user/name read-user)))))

        (testing "UPDATE user"
          (let [update-resp (app {:request-method :put
                                  :uri (str "/api/users/" user-id)
                                  :path-params {:id (str user-id)}
                                  :body-params {:user/age 41}
                                  :db-conn test-conn})]
            (is (= 200 (:status update-resp)))
            (let [updated-user (db/find-user-by-id @test-conn user-id)]
              (is (= 41 (:user/age updated-user))))))

        (testing "DELETE user"
          (let [delete-resp (app {:request-method :delete
                                  :uri (str "/api/users/" user-id)
                                  :path-params {:id (str user-id)}
                                  :db-conn test-conn})]
            (is (= 204 (:status delete-resp)))
            (is (empty? (db/find-all-users @test-conn)))))))))

(deftest user-pagination-test
  (let [app webapi/app
        conn test-conn
        ;; テストデータを逆順で投入 (created-atが古い順)
        users (for [i (range 25)]
                (:user/id (first (d/transact! conn [{:user/name (str "User " i)
                                                     :user/age (+ 20 i)
                                                     :user/created-at (java.util.Date. (* i 10000))
                                                     :user/id (random-uuid)}]))))]

    (testing "first page"
      (let [resp (app {:request-method :get :uri "/api/users" :query-params {"limit" "10"} :db-conn conn})
            body (-> resp :body (json/decode true))]
        (is (= 200 (:status resp)))
        (is (= 10 (count (:data body))))
        (is (= "User 24" (-> body :data first :user/name))) ; created-at降順なので一番新しいもの
        (is (nil? (:prev (:paging body))))
        (is (some? (:next (:paging body))))))

    (testing "next page (after)"
      (let [first-page-resp (app {:request-method :get :uri "/api/users" :query-params {"limit" "10"} :db-conn conn})
            next-page-url (-> first-page-resp :body (json/decode true) :paging :next)
            resp (app {:request-method :get :uri next-page-url :db-conn conn})
            body (-> resp :body (json/decode true))]
        (is (= 200 (:status resp)))
        (is (= 10 (count (:data body))))
        (is (= "User 14" (-> body :data first :user/name)))
        (is (some? (:prev (:paging body))))
        (is (some? (:next (:paging body))))))

    (testing "previous page (before)"
      (let [first-page-resp (app {:request-method :get :uri "/api/users" :query-params {"limit" "10"} :db-conn conn})
            next-page-url (-> first-page-resp :body (json/decode true) :paging :next)
            second-page-resp (app {:request-method :get :uri next-page-url :db-conn conn})
            prev-page-url (-> second-page-resp :body (json/decode true) :paging :prev)
            resp (app {:request-method :get :uri prev-page-url :db-conn conn})
            body (-> resp :body (json/decode true))]
        (is (= 200 (:status resp)))
        (is (= 10 (count (:data body))))
        (is (= "User 24" (-> body :data first :user/name)))))))

