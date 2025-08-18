(ns myapp.core-test
  (:require [clojure.test :refer :all]
            [myapp.core :as core]))

(deftest fibonacci-normal-cases-test
  (testing "正常なケースのテスト"
    (is (= 0 (core/fibonacci 0)))
    (is (= 1 (core/fibonacci 1)))
    (is (= 5 (core/fibonacci 5)))
    (is (= 55 (core/fibonacci 10)))))

(deftest fibonacci-memo-normal-cases-test
  (testing "メモ化版での正常なケースのテスト"
    (is (= 0 (core/fibonacci-memo 0)))
    (is (= 1 (core/fibonacci-memo 1)))
    (is (= 5 (core/fibonacci-memo 5)))
    (is (= 55 (core/fibonacci-memo 10)))
    (is (= 377 (core/fibonacci-memo 14)))))

(deftest fibonacci-negative-input-test
  (testing "fibonacci関数での負の数のテスト"
    (is (thrown-with-msg? IllegalArgumentException
                          #"引数は非負の整数である必要があります"
                          (core/fibonacci -1)))
    (is (thrown-with-msg? IllegalArgumentException
                          #"引数は非負の整数である必要があります"
                          (core/fibonacci -5)))))

(deftest fibonacci-memo-negative-input-test
  (testing "fibonacci-memo関数での負の数のテスト"
    (is (thrown-with-msg? IllegalArgumentException
                          #"引数は非負の整数である必要があります"
                          (core/fibonacci-memo -1)))
    (is (thrown-with-msg? IllegalArgumentException
                          #"引数は非負の整数である必要があります"
                          (core/fibonacci-memo -10)))))

(deftest fibonacci-boundary-values-test
  (testing "境界値のテスト"
    (is (= 0 (core/fibonacci 0)))
    (is (= 1 (core/fibonacci 1)))
    (is (= 0 (core/fibonacci-memo 0)))
    (is (= 1 (core/fibonacci-memo 1)))))

(deftest fibonacci-non-integer-test
  (testing "非整数のテスト"
    (is (thrown-with-msg? IllegalArgumentException
                          #"引数は非負の整数である必要があります"
                          (core/fibonacci 2.5)))
    (is (thrown-with-msg? IllegalArgumentException
                          #"引数は非負の整数である必要があります"
                          (core/fibonacci -1.5)))
    (is (thrown-with-msg? IllegalArgumentException
                          #"引数は非負の整数である必要があります"
                          (core/fibonacci-memo 2.5)))
    (is (thrown-with-msg? IllegalArgumentException
                          #"引数は非負の整数である必要があります"
                          (core/fibonacci-memo -1.5)))))

(deftest fibonacci-sequence-correctness-test
  (testing "フィボナッチ数列の正しさを検証"
    (let [expected [0 1 1 2 3 5 8 13 21 34 55]]
      (dotimes [i (count expected)]
        (is (= (nth expected i) (core/fibonacci i))
            (str "fibonacci(" i ") should be " (nth expected i)))
        (is (= (nth expected i) (core/fibonacci-memo i))
            (str "fibonacci-memo(" i ") should be " (nth expected i)))))))