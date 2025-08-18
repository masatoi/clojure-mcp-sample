(ns myapp.core)
(defn fibonacci
  "フィボナッチ数を計算する関数"
  [n]
  (cond
    (< n 0) (throw (IllegalArgumentException. "引数は非負の整数である必要があります"))
    (= n 0) 0
    (= n 1) 1
    :else (+ (fibonacci (- n 1)) (fibonacci (- n 2)))))

(def fibonacci-memo
  "メモ化を使った効率的なフィボナッチ関数"
  (memoize
   (fn [n]
     (cond
       (< n 0) (throw (IllegalArgumentException. "引数は非負の整数である必要があります"))
       (= n 0) 0
       (= n 1) 1
       :else (+ (fibonacci-memo (- n 1)) (fibonacci-memo (- n 2)))))))

(defn -main [] (println "hello"))
