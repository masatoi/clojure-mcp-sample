# Clojure + Clojure-MCP + Claude Code手順

## 最小構成でプロジェクト作成

```bash
mkdir myapp && cd myapp
cat > deps.edn <<'EDN'
{:paths ["src"]
 :aliases {}}
EDN
mkdir -p src/myapp
cat > src/myapp/core.clj <<'CLJ'
(ns myapp.core)
(defn -main [] (println "hello"))
CLJ
```

## 開発するプロジェクト側でnREPLサーバを起動する

```bash
$ clojure -M:nrepl
nREPL server started on port 7888 on host localhost - nrepl://localhost:7888
```

## Clojureコマンドの設定にMCPサーバの設定を入れる

~/.clojure/deps.edn

```clojure
{:aliases
 {:mcp
  {:deps {org.slf4j/slf4j-nop {:mvn/version "2.0.16"} ;; stdio用
          com.bhauman/clojure-mcp {:git/url "https://github.com/bhauman/clojure-mcp.git"
                                   :git/tag "v0.1.8-alpha"
                                   :git/sha "457f197"}}
   :exec-fn clojure-mcp.main/start-mcp-server
   :exec-args {:port 7888}}}}
```

MCPサーバ起動テスト

```bash
clojure -X:mcp :port 7888
```

## Claude Code側の設定

MCPサーバ起動テストに使ったコマンドを設定する

```bash
claude mcp add clojure-mcp -- clojure -X:mcp :port 7888
```

claude で起動して /mcp コマンドで接続確認

```
 Manage MCP servers
│
│ ❯ 1. clojure-mcp  ✔ connected · Enter to view details
│
│ MCP Config locations (by scope):
│  • User config (available in all your projects):
│    • /home/wiz/.claude.json
│  • Project config (shared via .mcp.json):
│    • /home/wiz/clj/myapp/.mcp.json (file does not exist)
│  • Local config (private to you in this project):
│    • /home/wiz/.claude.json [project: /home/wiz/clj/myapp]
```

## テスト実行

特定のテストファイルのみ実行:
```bash
clojure -M -e "(require '[clojure.test :as test]) (require '[myapp.core-test]) (test/run-tests 'myapp.core-test)"
```

全テストファイルを実行:
```bash
clojure -M -e "(require '[clojure.test :as test]) (require '[myapp.core-test]) (test/run-all-tests #\".*-test$\")"
```

## より複雑なプログラム

### プロンプト

多層パーセプトロンを実装し、関数近似するためのプログラムを作ってください。
sin関数の周辺に標準正規分布でデータセットを作り、関数近似して元のsin関数を近似させてください。

### evalのテスト (別経路でnREPLに接続して確認)

MCPサーバ(clojure-mcp)に対して最小の例でevalを実行してみてください。
同様に、ここまでに定義したフィボナッチ関数を呼び出せますか？

## コストが高い

API呼び出しに都度それまでのコンテキストを含めるため？

```
> /cost 
  ⎿  Total cost:            $4.29
     Total duration (API):  8m 21.6s
     Total duration (wall): 20h 44m 35.2s
     Total code changes:    0 lines added, 0 lines removed
     Usage by model:
         claude-3-5-haiku:  2.6k input, 243 output, 0 cache read, 0 cache write
            claude-sonnet:  612 input, 25.0k output, 6.4m cache read, 533.3k cache write
```

一度完成した関数の実装の詳細は忘れる必要がありそう。インターフェースのみ知っていればいい

## Gemini CLIとの接続

https://github.com/google-gemini/gemini-cli/blob/main/docs/tools/mcp-server.md


```
gemini mcp add clojure-mcp clojure -X:mcp :port 7888
```

プロジェクトローカルに設定ができる。

```bash
$ cat .gemini/settings.json 
{
  "mcpServers": {
    "clojure-mcp": {
      "command": "clojure",
      "args": [
        "-X:mcp",
        ":port",
        "7888"
      ]
    }
  }
}
```

### evalのテスト (別経路でnREPLに接続して確認)

MCPサーバ(clojure-mcp)に対して最小の例でevalを実行してみてください。
簡単な関数を定義してみてください。
今評価した関数の定義を見せてください。

ガウス＝ルジャンドルのアルゴリズムで円周率を計算する関数を定義し、evalを使って動作を検証し、関数の定義をsrc/myapp/core.clj に書き込んでください。
モンテカルロ法により円周率を計算する関数を書き、ランダムな点の数で何通りか実験してください。関数の定義をsrc/myapp/core.clj に書き込んでください。


モンテカルロ法により円周率を計算する関数の別のバージョンを書いてください。以下を実装してください。

重点サンプリング (Importance Sampling):
円の中心付近に点を多く打つように、乱数生成を調整する方法です。円の中心付近に点が集中するように調整することで、円周率の近似精度を向上させることができます。

evalで動作を検証し、関数の定義をsrc/myapp/core.clj に書き込んでください。

src/myapp/core.cljの各関数にclojure.specを追加してください。

evalでそれぞれの制約を検証してください。

スペック定義は正しかったでしょうか、もし変更点がある場合はsrc/myapp/core.clj を修正してください。

重点サンプリングによってより少ないサンプル点でより精度の高い円周率の近似ができたでしょうか？
もう一度evalを実行してみてください。平均的な結果として元のモンテカルロ法の方が良い結果になるでしょうか。

現在のコードは一様分布ですか？原点を中心とした二次元正規分布にするとどうなりますか？

正規分布でサンプリングするバージョンも src/myapp/core.clj に書き込んでください。

この関数の呼び出し例を示して、実際に評価してみてください

接続中のnREPLサーバの情報を表示してください。

エラトステネスのふるいによる素数判定をする関数を定義して

