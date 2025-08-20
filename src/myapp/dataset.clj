(ns myapp.dataset
  "データセット生成とユーティリティ関数"
  (:require [clojure.core.matrix :as m]
            [clojure.core.matrix.random :as rand]))

(defn normal-random
  "標準正規分布からランダムな値を生成"
  ([]
   (first (rand/sample-normal [1])))
  ([n]
   (repeatedly n normal-random)))

(defn generate-sin-dataset
  "sin関数の周辺にノイズを加えたデータセットを生成"
  [num-points & {:keys [x-range noise-std]
                 :or {x-range [(- Math/PI) Math/PI] noise-std 0.1}}]
  (let [[x-min x-max] x-range
        x-values (repeatedly num-points #(+ x-min (* (rand) (- x-max x-min))))
        y-values (map (fn [x]
                        (let [true-y (Math/sin x)
                              noise (* noise-std (normal-random))]
                          (+ true-y noise)))
                      x-values)]
    (map (fn [x y] [(m/array [x]) (m/array [y])]) x-values y-values)))

(defn generate-uniform-sin-dataset
  "等間隔でsin関数のデータセットを生成（ノイズ付き）"
  [num-points & {:keys [x-range noise-std]
                 :or {x-range [(- Math/PI) Math/PI] noise-std 0.1}}]
  (let [[x-min x-max] x-range
        step (/ (- x-max x-min) (dec num-points))
        x-values (map #(+ x-min (* % step)) (range num-points))
        y-values (map (fn [x]
                        (let [true-y (Math/sin x)
                              noise (* noise-std (normal-random))]
                          (+ true-y noise)))
                      x-values)]
    (map (fn [x y] [(m/array [x]) (m/array [y])]) x-values y-values)))

(defn generate-clean-sin-dataset
  "ノイズなしのsin関数データセットを生成（テスト用）"
  [num-points & {:keys [x-range]
                 :or {x-range [(- Math/PI) Math/PI]}}]
  (let [[x-min x-max] x-range
        step (/ (- x-max x-min) (dec num-points))
        x-values (map #(+ x-min (* % step)) (range num-points))]
    (map (fn [x] [(m/array [x]) (m/array [(Math/sin x)])]) x-values)))

(defn split-dataset
  "データセットを訓練用とテスト用に分割"
  [dataset split-ratio]
  (let [shuffled (shuffle dataset)
        split-point (int (* split-ratio (count shuffled)))
        train-set (take split-point shuffled)
        test-set (drop split-point shuffled)]
    [train-set test-set]))

(defn normalize-dataset
  "データセットを正規化（平均0、標準偏差1）"
  [dataset]
  (let [inputs (map first dataset)
        outputs (map second dataset)

        ;; 入力の統計
        input-values (map #(m/mget % 0) inputs)
        input-mean (/ (reduce + input-values) (count input-values))
        input-var (/ (reduce + (map #(let [diff (- % input-mean)] (* diff diff)) input-values))
                     (count input-values))
        input-std (Math/sqrt input-var)

        ;; 出力の統計
        output-values (map #(m/mget % 0) outputs)
        output-mean (/ (reduce + output-values) (count output-values))
        output-var (/ (reduce + (map #(let [diff (- % output-mean)] (* diff diff)) output-values))
                      (count output-values))
        output-std (Math/sqrt output-var)

        ;; 正規化
        normalized (map (fn [[input output]]
                          [(m/array [(/ (- (m/mget input 0) input-mean) input-std)])
                           (m/array [(/ (- (m/mget output 0) output-mean) output-std)])])
                        dataset)]

    {:dataset normalized
     :input-stats {:mean input-mean :std input-std}
     :output-stats {:mean output-mean :std output-std}}))

(defn denormalize-output
  "正規化された出力を元のスケールに戻す"
  [normalized-output output-stats]
  (let [{:keys [mean std]} output-stats]
    (+ (* normalized-output std) mean)))

(defn dataset-statistics
  "データセットの統計情報を表示"
  [dataset]
  (let [inputs (map #(m/mget (first %) 0) dataset)
        outputs (map #(m/mget (second %) 0) dataset)]
    {:size (count dataset)
     :input {:min (apply min inputs)
             :max (apply max inputs)
             :mean (/ (reduce + inputs) (count inputs))}
     :output {:min (apply min outputs)
              :max (apply max outputs)
              :mean (/ (reduce + outputs) (count outputs))}}))

(defn create-test-grid
  "テスト用の等間隔グリッドを作成"
  [num-points & {:keys [x-range]
                 :or {x-range [(- Math/PI) Math/PI]}}]
  (let [[x-min x-max] x-range
        step (/ (- x-max x-min) (dec num-points))]
    (map #(m/array [(+ x-min (* % step))]) (range num-points))))