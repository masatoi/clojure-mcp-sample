(ns myapp.sin-approximation
  "多層パーセプトロンによるsin関数の近似デモ"
  (:require [myapp.neural-network :as nn]
            [myapp.dataset :as ds]
            [clojure.core.matrix :as m]
            [clojure.pprint :as pp]))

(defn evaluate-model
  "モデルの性能を評価"
  [network test-data & {:keys [denormalize-fn] :or {denormalize-fn identity}}]
  (let [predictions (map (fn [[input target]]
                           (let [pred (nn/predict network input)
                                 actual target]
                             {:predicted (denormalize-fn (m/mget pred 0))
                              :actual (denormalize-fn (m/mget actual 0))
                              :input (m/mget input 0)}))
                         test-data)
        errors (map #(Math/abs (- (:predicted %) (:actual %))) predictions)
        mse (/ (reduce + (map #(let [e (- (:predicted %) (:actual %))] (* e e)) predictions))
               (count predictions))
        mae (/ (reduce + errors) (count predictions))
        max-error (apply max errors)]
    {:predictions predictions
     :mse mse
     :mae mae
     :max-error max-error
     :num-samples (count test-data)}))

(defn print-evaluation
  "評価結果を整形して表示"
  [evaluation]
  (println "\n=== モデル評価結果 ===")
  (printf "サンプル数: %d\n" (:num-samples evaluation))
  (printf "平均二乗誤差 (MSE): %.6f\n" (:mse evaluation))
  (printf "平均絶対誤差 (MAE): %.6f\n" (:mae evaluation))
  (printf "最大誤差: %.6f\n" (:max-error evaluation))

  (println "\n=== 予測例（最初の10サンプル） ===")
  (println "入力値\t\t予測値\t\t実際値\t\t誤差")
  (doseq [pred (take 10 (:predictions evaluation))]
    (printf "%.4f\t\t%.4f\t\t%.4f\t\t%.4f\n"
            (:input pred) (:predicted pred) (:actual pred)
            (Math/abs (- (:predicted pred) (:actual pred))))))

(defn train-sin-approximator
  "sin関数近似器を訓練"
  [& {:keys [num-training-points num-test-points hidden-layers
             epochs learning-rate noise-std activation verbose]
      :or {num-training-points 1000
           num-test-points 200
           hidden-layers [20 15]
           epochs 1000
           learning-rate 0.01
           noise-std 0.1
           activation :tanh
           verbose true}}]

  (println "=== sin関数近似器を訓練中 ===")
  (printf "訓練データ点数: %d\n" num-training-points)
  (printf "テストデータ点数: %d\n" num-test-points)
  (printf "隠れ層構造: %s\n" (vec hidden-layers))
  (printf "エポック数: %d\n" epochs)
  (printf "学習率: %.4f\n" learning-rate)
  (printf "ノイズ標準偏差: %.4f\n" noise-std)
  (printf "活性化関数: %s\n" activation)

  ;; データセット生成
  (println "\nデータセット生成中...")
  (let [training-data (ds/generate-sin-dataset num-training-points
                                               :noise-std noise-std)
        test-data (ds/generate-clean-sin-dataset num-test-points)

        ;; データセット統計
        train-stats (ds/dataset-statistics training-data)
        test-stats (ds/dataset-statistics test-data)]

    (println "\n訓練データ統計:")
    (pp/pprint train-stats)
    (println "\nテストデータ統計:")
    (pp/pprint test-stats)

    ;; ネットワーク作成
    (let [layer-sizes (concat [1] hidden-layers [1])
          network (nn/create-network layer-sizes :activation activation)]

      (println (str "\nネットワーク構造: " layer-sizes))
      (println "訓練開始...")

      ;; 訓練
      (let [trained-network (nn/train-network network training-data epochs learning-rate
                                              :verbose verbose)]

        (println "\n訓練完了!")

        ;; 評価
        (println "\n訓練データでの評価:")
        (let [train-eval (evaluate-model trained-network (take 100 training-data))]
          (print-evaluation train-eval))

        (println "\nテストデータでの評価:")
        (let [test-eval (evaluate-model trained-network test-data)]
          (print-evaluation test-eval))

        ;; 特定の点での予測を表示
        (println "\n=== 特定の点での予測 ===")
        (let [test-points [0.0 (/ Math/PI 4) (/ Math/PI 2) (/ (* 3 Math/PI) 4) Math/PI]]
          (println "x\t\t真値\t\t予測値\t\t誤差")
          (doseq [x test-points]
            (let [true-y (Math/sin x)
                  pred-y (m/mget (nn/predict trained-network (m/array [x])) 0)
                  error (Math/abs (- pred-y true-y))]
              (printf "%.4f\t\t%.4f\t\t%.4f\t\t%.4f\n" x true-y pred-y error))))

        trained-network))))

(defn demo-sin-approximation
  "sin関数近似のデモを実行"
  []
  (println "多層パーセプトロンによるsin関数近似デモ")
  (println "==========================================")

  ;; 基本的なデモ
  (println "\n【デモ1】基本的な近似")
  (let [network1 (train-sin-approximator
                  :num-training-points 500
                  :hidden-layers [10 8]
                  :epochs 500
                  :learning-rate 0.01
                  :noise-std 0.05
                  :verbose false)]

    ;; より複雑なネットワークでのデモ
    (println "\n\n【デモ2】より複雑なネットワーク")
    (let [network2 (train-sin-approximator
                    :num-training-points 1000
                    :hidden-layers [30 20 10]
                    :epochs 1000
                    :learning-rate 0.005
                    :noise-std 0.1
                    :activation :tanh
                    :verbose false)]

      ;; 異なる活性化関数でのデモ
      (println "\n\n【デモ3】ReLU活性化関数を使用")
      (let [network3 (train-sin-approximator
                      :num-training-points 800
                      :hidden-layers [25 15]
                      :epochs 800
                      :learning-rate 0.01
                      :noise-std 0.08
                      :activation :relu
                      :verbose false)]

        (println "\n\n=== 全デモ完了 ===")
        {:basic network1 :complex network2 :relu network3}))))

(defn interactive-prediction
  "対話的な予測機能"
  [network]
  (println "\n=== 対話的予測モード ===")
  (println "x値を入力してください（例: 0.5, 1.57, -1.0）")
  (println "終了するには 'quit' を入力してください")

  (loop []
    (print "\nx = ")
    (flush)
    (let [input (read-line)]
      (if (= input "quit")
        (println "終了します")
        (do
          (try
            (let [x (Double/parseDouble input)
                  true-y (Math/sin x)
                  pred-y (m/mget (nn/predict network (m/array [x])) 0)
                  error (Math/abs (- pred-y true-y))]
              (printf "sin(%.4f) = %.4f (真値)\n" x true-y)
              (printf "予測値 = %.4f\n" pred-y)
              (printf "誤差 = %.4f\n" error))
            (catch Exception e
              (println "無効な入力です。数値を入力してください。")))
          (recur))))))