(ns myapp.neural-network
  "多層パーセプトロンによる関数近似"
  (:require [clojure.core.matrix :as m]
            [clojure.core.matrix.random :as rand]))

;; core.matrixの実装をVectorz（高性能）に設定
(m/set-current-implementation :vectorz)

;; 活性化関数
(defn sigmoid
  "シグモイド関数"
  [x]
  (/ 1.0 (+ 1.0 (Math/exp (- x)))))

(defn sigmoid-derivative
  "シグモイド関数の微分"
  [x]
  (let [s (sigmoid x)]
    (* s (- 1.0 s))))

(defn tanh-fn
  "tanh関数"
  [x]
  (Math/tanh x))

(defn tanh-derivative
  "tanh関数の微分"
  [x]
  (let [t (tanh-fn x)]
    (- 1.0 (* t t))))

(defn relu
  "ReLU関数"
  [x]
  (max 0.0 x))

(defn relu-derivative
  "ReLU関数の微分"
  [x]
  (if (> x 0) 1.0 0.0))

(defn linear
  "線形関数（恒等関数）"
  [x]
  x)

(defn linear-derivative
  "線形関数の微分"
  [x]
  1.0)

;; 活性化関数のマップ
(def activation-functions
  {:sigmoid {:fn sigmoid :derivative sigmoid-derivative}
   :tanh {:fn tanh-fn :derivative tanh-derivative}
   :relu {:fn relu :derivative relu-derivative}
   :linear {:fn linear :derivative linear-derivative}})

;; ニューラルネットワーク構造
(defrecord NeuralNetwork [layers weights biases activation-fn])

(defn create-network
  "ニューラルネットワークを作成する"
  [layer-sizes & {:keys [activation] :or {activation :tanh}}]
  (let [num-layers (count layer-sizes)
        weights (vec (for [i (range (dec num-layers))]
                       (let [input-size (nth layer-sizes i)
                             output-size (nth layer-sizes (inc i))]
                         ;; Xavier初期化
                         (m/mul (rand/sample-normal [output-size input-size])
                                (Math/sqrt (/ 2.0 input-size))))))
        biases (vec (for [i (range 1 num-layers)]
                      (m/zero-vector (nth layer-sizes i))))]
    (->NeuralNetwork layer-sizes weights biases activation)))

(defn forward-pass
  "順伝播"
  [network input]
  (let [{:keys [weights biases activation-fn]} network
        activation-fn-impl (get-in activation-functions [activation-fn :fn])]
    (loop [activations [input]
           layer-idx 0]
      (if (< layer-idx (count weights))
        (let [prev-activation (last activations)
              weight (nth weights layer-idx)
              bias (nth biases layer-idx)
              z (m/add (m/mmul weight prev-activation) bias)
              a (if (= layer-idx (dec (count weights)))
                  ;; 出力層は線形活性化
                  z
                  ;; 隠れ層は指定された活性化関数
                  (m/emap activation-fn-impl z))]
          (recur (conj activations a) (inc layer-idx)))
        activations))))

(defn mean-squared-error
  "平均二乗誤差"
  [predicted actual]
  (let [diff (m/sub predicted actual)]
    (/ (m/esum (m/mul diff diff)) (m/ecount actual))))

(defn backward-pass
  "誤差逆伝播"
  [network input target learning-rate]
  (let [{:keys [weights biases activation-fn]} network
        activation-fn-impl (get-in activation-functions [activation-fn :fn])
        derivative-fn (get-in activation-functions [activation-fn :derivative])

        ;; 順伝播で各層の出力を計算
        activations (forward-pass network input)
        num-layers (count weights)

        ;; 出力層の誤差
        output-error (m/sub (last activations) target)

        ;; 逆伝播で各層の誤差と勾配を計算
        [new-weights new-biases]
        (loop [layer-idx (dec num-layers)
               error output-error
               new-weights weights
               new-biases biases]
          (if (>= layer-idx 0)
            (let [current-activation (nth activations layer-idx)
                  next-activation (nth activations (inc layer-idx))
                  weight (nth weights layer-idx)

                  ;; 出力層以外は活性化関数の微分を適用
                  delta (if (= layer-idx (dec num-layers))
                          error ;; 出力層
                          (m/mul error (m/emap derivative-fn next-activation)))

                  ;; 重みとバイアスの勾配
                  weight-gradient (m/outer-product delta current-activation)
                  bias-gradient delta

                  ;; 重みとバイアスを更新
                  updated-weight (m/sub weight (m/mul learning-rate weight-gradient))
                  updated-bias (m/sub (nth biases layer-idx) (m/mul learning-rate bias-gradient))

                  ;; 前の層への誤差
                  prev-error (if (> layer-idx 0)
                               (m/mmul (m/transpose weight) delta)
                               nil)]
              (recur (dec layer-idx)
                     prev-error
                     (assoc new-weights layer-idx updated-weight)
                     (assoc new-biases layer-idx updated-bias)))
            [new-weights new-biases]))]

    (assoc network :weights new-weights :biases new-biases)))

(defn train-network
  "ネットワークを訓練する"
  [network training-data epochs learning-rate & {:keys [verbose] :or {verbose false}}]
  (loop [net network
         epoch 0]
    (if (< epoch epochs)
      (let [total-error (atom 0.0)
            updated-net
            (reduce (fn [current-net [input target]]
                      (let [prediction (last (forward-pass current-net input))
                            error (mean-squared-error prediction target)]
                        (swap! total-error + error)
                        (backward-pass current-net input target learning-rate)))
                    net
                    training-data)
            avg-error (/ @total-error (count training-data))]
        (when (and verbose (= 0 (mod epoch 100)))
          (println (format "Epoch %d: Error = %.6f" epoch avg-error)))
        (recur updated-net (inc epoch)))
      net)))

(defn predict
  "予測を行う"
  [network input]
  (last (forward-pass network input)))