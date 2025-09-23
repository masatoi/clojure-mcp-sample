(ns myapp.db
  (:require [datascript.core :as d]))

;; 1. スキーマを定義します
;; :db/unique :db.unique/identity は、この属性がエンティティのユニークな識別子であることを示します。
(def schema
  {:user/id {:db/unique :db.unique/identity}
   :user/name {:db/type :db.type/string}
   :user/age {:db/type :db.type/long}
   :user/address {:db/type :db.type/string}
   :user/created-at {:db/type :db.type/instant}
   :user/updated-at {:db/type :db.type/instant}})

;; 2. スキーマを使ってインメモリDBを作成します
(def conn (d/create-conn schema))

(defn find-all-users [db]
  (d/q '[:find (pull ?e [*])
         :where [?e :user/name]]
       db))

(defn find-user-by-id [db id]
  (d/pull db '[*] [:user/id id]))

(defn create-user! [conn user-data]
  (let [now (java.util.Date.)
        new-user (merge user-data
                        {:user/id (random-uuid)
                         :user/created-at now
                         :user/updated-at now})]
    (d/transact! conn [new-user])))

(defn update-user! [conn id new-data]
  (let [now (java.util.Date.)
        db @conn
        user-id (d/q '[:find ?e .
                       :in $ ?id
                       :where [?e :user/id ?id]]
                     db id)
        updated-data (merge new-data
                            {:db/id user-id
                             :user/updated-at now})]
    (when user-id
      (d/transact! conn [updated-data]))))

(defn delete-user! [conn id]
  (d/transact! conn [[:db.fn/retractEntity [:user/id id]]]))

(defn find-paginated
  "汎用的なページネーションクエリを実行する。"
  [db {:keys [entity-attr sort-attr limit cursor-id direction] :or {limit 10}}]
  (let [sort-val-cursor (when cursor-id
                          (d/q '[:find ?sort-val .
                                 :in $ ?id
                                 :where [?e entity-attr ?id]
                                 [?e sort-attr ?sort-val]]
                               db cursor-id))]
    (d/q {:find '[(pull ?e [*])]
          :in '[$ ?sort-attr-val ?cursor-id]
          :where '[[?e entity-attr ?id]
                   [?e sort-attr ?sort-val]
                   (or (and (= ?sort-val ?sort-attr-val)
                            (if (= direction :forward)
                              (< ?id ?cursor-id)
                              (> ?id ?cursor-id)))
                       (if (= direction :forward)
                         (< ?sort-val ?sort-attr-val)
                         (> ?sort-val ?sort-attr-val)))]
          :order-by '[(if (= direction :forward) desc asc) ?sort-val
                      (if (= direction :forward) desc asc) ?id]
          :limit 'limit}
         db sort-val-cursor cursor-id)))

;; 3. データをトランザクションで追加します
(d/transact! conn
             [{:name "Alice", :age 30}
              {:name "Bob", :age 25}
              {:name "Charlie", :age 35}])

;; 4. Datalogを使ってクエリを実行します
;; "30歳より上のユーザーの名前と年齢を探す"

;; REPLなどで結果を確認するために
(comment
  (println result)
  ;; => #{["Charlie" 35]}
  )
