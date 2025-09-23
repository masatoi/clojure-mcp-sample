(ns myapp.core
  (:require [clojure.spec.alpha :as s]))

(import '[java.util Random])

;; --- Specs ---

(s/fdef fibonacci
  :args (s/cat :n nat-int?)
  :ret nat-int?)

(s/fdef fibonacci-memo
  :args (s/cat :n nat-int?)
  :ret nat-int?)

(s/fdef gauss-legendre-pi
  :args (s/cat :iterations pos-int?)
  :ret double?)

(s/fdef monte-carlo-pi
  :args (s/cat :num-points pos-int?)
  :ret double?)

(s/fdef monte-carlo-pi-importance
  :args (s/cat :num-points pos-int?)
  :ret double?)

(s/fdef monte-carlo-pi-normal
  :args (s/cat :num-points pos-int? :sigma (s/and double? pos?))
  :ret double?)

(s/fdef sieve-of-eratosthenes
  :args (s/cat :n (s/and int? #(> % 1)))
  :ret (s/coll-of int?))

(s/fdef quicksort
  :args (s/cat :coll (s/coll-of number?))
  :ret (s/coll-of number?))

(s/fdef merge-sort
  :args (s/cat :coll (s/coll-of number?))
  :ret (s/coll-of number?))

(s/fdef -main
  :args (s/cat)
  :ret nil?)

;; --- Implementations ---

(defn add1 [n]
  (+ n 1))

(defn stalin-sort
  "スターリンソート：順序に従わない要素を削除してソートする"
  [coll]
  (if (empty? coll)
    []
    (reduce (fn [acc x]
              (if (or (empty? acc) (>= x (last acc)))
                (conj acc x)
                acc))
            []
            coll)))

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

(defn gauss-legendre-pi [iterations]
  (loop [n 0
         a 1.0
         b (/ 1.0 (Math/sqrt 2.0))
         t 0.25
         p 1.0]
    (if (< n iterations)
      (let [a-next (/ (+ a b) 2.0)
            b-next (Math/sqrt (* a b))
            t-next (- t (* p (Math/pow (- a a-next) 2.0)))
            p-next (* 2.0 p)]
        (recur (inc n) a-next b-next t-next p-next))
      (/ (Math/pow (+ a b) 2.0) (* 4.0 t)))))

(defn monte-carlo-pi [num-points]
  (let [points-in-circle (->> (repeatedly num-points
                                          #(let [x (- (* 2.0 (rand)) 1.0)
                                                 y (- (* 2.0 (rand)) 1.0)]
                                             (+ (* x x) (* y y))))
                              (filter #(<= % 1.0))
                              (count))]
    (* 4.0 (/ points-in-circle num-points))))

(defn- sample-triangular []
  (let [u (rand)]
    (if (< u 0.5)
      (+ -1.0 (Math/sqrt (* 2.0 u)))
      (- 1.0 (Math/sqrt (* 2.0 (- 1.0 u)))))))

(defn monte-carlo-pi-importance [num-points]
  "Calculates Pi using Monte Carlo with triangular importance sampling."
  (let [samples (repeatedly num-points
                            #(let [x (sample-triangular)
                                   y (sample-triangular)
                                   px (- 1.0 (Math/abs x))
                                   py (- 1.0 (Math/abs y))]
                               (if (<= (+ (* x x) (* y y)) 1.0)
                                 (/ 1.0 (* px py))
                                 0.0)))]
    (/ (reduce + samples) num-points)))

(let [^Random rng (Random.)]
  (defn- sample-normal [sigma]
    (* sigma (.nextGaussian rng))))

(defn- pdf-2d-normal [x y sigma]
  (let [sigma-sq (* sigma sigma)]
    (/ (Math/exp (- (/ (+ (* x x) (* y y)) (* 2.0 sigma-sq))))
       (* 2.0 Math/PI sigma-sq))))

(defn monte-carlo-pi-normal [num-points sigma]
  "Calculates Pi using Monte Carlo with 2D normal importance sampling."
  (let [samples (repeatedly num-points
                            #(let [x (sample-normal sigma)
                                   y (sample-normal sigma)]
                               (if (<= (+ (* x x) (* y y)) 1.0)
                                 (/ 1.0 (pdf-2d-normal x y sigma))
                                 0.0)))]
    (/ (reduce + samples) num-points)))

(defn sieve-of-eratosthenes [n]
  (let [primes (volatile! (vec (repeat (inc n) true)))]
    (vswap! primes assoc 0 false)
    (vswap! primes assoc 1 false)
    (loop [p 2]
      (if (> (* p p) n)
        (for [i (range 2 (inc n)) :when (get @primes i)] i)
        (do
          (if (get @primes p)
            (doseq [i (range (* p p) (inc n) p)]
              (vswap! primes assoc i false)))
          (recur (inc p)))))))

(defn quicksort [coll]
  (if (empty? coll)
    []
    (let [pivot (first coll)
          rest (rest coll)]
      (concat (quicksort (filter #(< % pivot) rest))
              [pivot]
              (quicksort (filter #(>= % pivot) rest))))))

(defn bubble-sort
  "バブルソートを実装する関数。昇順で並び替えます。"
  ([coll] (bubble-sort coll false))
  ([coll verbose]
   (let [n (count coll)]
     (loop [v (vec coll)
            pass 0]
       (if (>= pass (dec n))
         v
         (let [[v' swapped?]
               (loop [v v
                      j 0
                      swapped? false]
                 (if (>= j (- n pass 1))
                   [v swapped?]
                   (let [a (v j)
                         b (v (inc j))]
                     (if (> a b)
                       (let [v2 (-> v (assoc j b) (assoc (inc j) a))]
                         (when verbose (println "swap at" j ":" a "<->" b "=>" v2))
                         (recur v2 (inc j) true))
                       (recur v (inc j) swapped?)))))]
           (if swapped?
             (recur v' (inc pass))
             v')))))))

(defn merge-sort
  "マージソートを実装する関数"
  ([coll] (merge-sort coll false))
  ([coll verbose]
   (if (<= (count coll) 1)
     coll
     (let [[left right] (split-at (quot (count coll) 2) coll)]
       (when verbose (println "Splitting:" coll "->" left right))
       (let [sorted-left (merge-sort left verbose)
             sorted-right (merge-sort right verbose)]
         (when verbose (println "Merging:" sorted-left "and" sorted-right))
         (letfn [(merge-fn [l r]
                   (cond
                     (empty? l) r
                     (empty? r) l
                     (<= (first l) (first r)) (lazy-seq (cons (first l) (merge-fn (rest l) r)))
                     :else (lazy-seq (cons (first r) (merge-fn l (rest r))))))]
           (let [merged (merge-fn sorted-left sorted-right)]
             (when verbose (println "Merged:" (seq merged)))
             merged)))))))

(defn -main [] (println "hello"))
