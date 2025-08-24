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
https://zenn.dev/karaage0703/articles/3bd2957807f311

```bash
npm install -g @anthropic-ai/claude-code

claude mcp add clojure-mcp -- clojure -X:mcp :port 7888

# 設定をプロジェクトローカルに置くには (/path/to/project/.mcp.json にできる)
claude mcp add clojure-mcp -s project -- clojure -X:mcp :port 7888
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

## Codex CLI

インストール
```
npm install -g @openai/codex

codex -m gpt-5 --config model_reasoning_effort=high
```

~/.codex/config.toml
```toml
[mcp_servers.clojure-mcp]
command = "clojure"
args = ["-X:mcp", ":port", "7888"]
```

## Emacsでやる場合の注意

以下を入れないと手動で再読み込みが必要

```elisp
;; ディスク上のファイルが変更されたら、バッファを自動的に再読み込みする
(global-auto-revert-mode 1)
```

## 使ってて感じた注意点

- clojure_eval ツールは、特に指定しない場合、デフォルトで user 名前空間でコードを実行する。作業する名前空間は明示的に指定する必要あり
- ライブラリのインストールを指示すると、deps.ednを書き換えてくれるので、nREPLサーバを再起動してインストールを完了する必要がある

### プロンプト

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

マージソートを実装し、evalで動作確認した上でsrc/myapp/core.clj に書き込んでください
動作確認はどの名前空間で行ないましたか？
=> 

✦ 評価は user 名前空間で行いました。

  clojure_eval ツールは、特に指定しない場合、デフォルトで user 名前空間でコードを実行します。

  今回実装した merge-sort 関数は、他の名前空間の関数に依存しない自己完結したものであったため、user
  名前空間で問題なく動作確認ができました。
  
myapp.core名前空間でevalしてください。
各マージの段階の途中経過をprintしてください。

オプショナルパラメータverboseで途中経過の表示を切り替えられるようにしてください。デフォルトは表示しないようにしてください。

テストは core 名前空間ではなく、myapp.coreでやるようにしてください。
(core/merge-sort [3 1 4 2]) ではなく (myapp.core/merge-sort [1 3 2 5 4 6]) でやるようにしてください。

バブルソートを実装し、evalでmyapp.core名前空間で動作確認した上でsrc/myapp/core.clj に書き込んでください

### WebAPI

myapp.coreで定義された fibonacci-memo を使って、以下のようなWebAPIを定義してください。
コードはsrc/myapp/webapi.cljに書いてください。コードの評価はmyapp.webapiで行なってください。

- URL: GET /fibonacci
- Query Parameters
  - n: positive integer to calc Fib(n)
- Response
  - JSON object like `{"value": <Integer value of Fib(n)>}`

Ring + Reitit ライブラリを使うように修正してください。
Muuntaja で JSON を透過的にハンドリングするようにし、Malli で I/O スキーマを宣言・検証するようにしてください。

Malliのスキーマはどこに定義されている？
IOスキーマを宣言するのはReititの機能で、Malliはそれに従ってバリデーションをする機能を持っているということでしょうか

codex
- 結論: ほぼその理解で正しいです。 「どこにスキーマを書くか」は Reitit（ルートのdata）で、「検証・変換（coercion）を誰がするか」は Malli（reitit.coercion.malli 経由）です。
結論: ほぼその理解で正しいです。 「どこにスキーマを書くか」は Reitit（ルートのdata）で、「検証・変換（coercion）を誰がするか」は Malli（reitit.coercion.malli 経由）です。
-
役割分担:
    - Reitit: ルート定義に I/O スキーマを宣言する場所を提供（例: :parameters {:query [:map [:n pos-int?]]}, :responses {...}）。実際の検証はしません。
    - Malli: スキーマ表現（[:map [:n pos-int?]] など）を解釈し、検証・（必要なら）型変換を行うライブラリ。
    - Reititのcoercion: reitit.coercion.malli が Reitit の Coercion 実装として Malli を使う橋渡し。
    - ミドルウェア: coercion/coerce-request-middleware と coercion/coerce-response-middleware が、ルートに宣言されたスキーマに従って Malli で I/O を検証/変換します。
    - Muuntaja: JSON のパース/エンコードとコンテンツネゴシエーション（スキーマ検証とは役割が別）。
-
配線の要点（本リポの設定）:
    - ルータの :data に :coercion malli/coercion を設定。
    - ルートに :parameters と :responses を宣言（これらは Malli スキーマ記法）。
    - ミドルウェア列に coerce-*-middleware を入れて実行時に Malli 検証を有効化。
    - Muuntajaの format-* ミドルウェアで JSON <-> Clojure データを透過的に扱う。
-
補足:
    - Reititは他のcoercion実装（Spec など）にも差し替え可能で、Malliはその一つです。
    - 400エラー時の詳細（humanizedなど）は Malli が生成した検証エラーを Reitit が整形して返すものです。必要なら一律 {:error "..."}  に整形するハンドラも追加できます。

myapp/webapi内の関数のユニットテストと、HTTPクライアントを使った統合テストを実装し、testディレクトリ以下に置いてください。

- clj -M -m clojure.test
- Or REPL:
    - (require 'myapp.webapi-unit-test 'myapp.webapi-integration-test)
    - (clojure.test/run-tests 'myapp.webapi-unit-test 'myapp.webapi-integration-test)
