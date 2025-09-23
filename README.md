# Samples for Clojure-MCP + LLM Agent

動機: 

- LLM Agentは便利だが、トイプロブレムでしかうまくいかない感覚がある
  - プロジェクトが大きくなってくると全体を把握し、整合性を取りながらの開発が難しい(コンテキスト把握の限界)
    - 一応、自己要約、プロジェクトをgrepするなどして、必要な箇所のみ読み込もうとはしている
- LLM AgentはLispのREPL駆動開発と相性がいいのでは、という仮説
  - 細かくコード生成 → 式単位での評価を繰り返すことにより高速でサイクルを回せることにより品質が上がる
  - コンテキスト長もその関数に閉じたものにできる(純粋関数なら特に)
  - clojure.specによる仕様と生成の連携
    - 明確な指示書になる: specがLLMに対する「コードの仕様書」となり、曖昧さのない厳密な指示を与えられる
    - 高速なフィードバックループ: LLMが生成したコードをspecで即座に自動検証できる
    - 品質の自動担保: 生成的テストにより、LLMが見逃しがちなエッジケースも自動で検証できる

## Clojure-MCP

LLM Agentの相手をするMCPサーバ。その先のnREPLサーバを経由して処理系と繋がる

![clojure-mcp-image](/images/clojure-mcp-image.png)

### 最小構成でClojureのプロジェクト作成

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

### 開発するプロジェクトのディレクトリでnREPLサーバを起動する

ここに対してLLM AgentからもnREPLクライアントからも接続できる

```bash
$ clojure -M:nrepl
nREPL server started on port 7888 on host localhost - nrepl://localhost:7888
```

### Clojureコマンドの設定にMCPサーバの設定を入れる

~/.clojure/deps.edn

```clojure
{:aliases
 {:mcp
  {:deps {org.slf4j/slf4j-nop {:mvn/version "2.0.16"} ;; stdio用
          com.bhauman/clojure-mcp {:git/url "https://github.com/bhauman/clojure-mcp.git"
                                   :git/tag "v0.1.10-alpha"}}
   :exec-fn clojure-mcp.main/start-mcp-server
   :exec-args {:port 7888}}}}
```

### MCPサーバ起動テスト

```bash
clojure -X:mcp :port 7888
```

stdioにJSON-RPCのメッセージが出ていればok。

## Claude Code側の設定

MCPサーバ起動テストに使ったコマンドを設定する
https://zenn.dev/karaage0703/articles/3bd2957807f311

```bash
# claude-codeのインストール
npm install -g @anthropic-ai/claude-code

# MCPサーバの設定
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

## プロジェクトのテスト実行

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

## 意外とコストが高い

1回の評価がコンパクトになるのでコストメリットがあると思っていたので予想と反している。

API呼び出しに都度それまでのコンテキストを含めるため？
トークン数に対して課金されるAPIだと重くなりそう。

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

トークン数のイメージ
![token-image](/images/token-image.png)

一度完成した関数の実装の詳細は忘れる必要がありそう。
本来はインターフェースのみ知っていればいいはず。
何をコンテキストに残すかを選択するのはLLM Agentの先端的な課題のよう。

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

codex -m gpt-5-codex --config model_reasoning_effort=high
```

Codex CLIは設定ファイル経由で

~/.codex/config.toml
```toml
[mcp_servers.clojure-mcp]
command = "clojure"
args = ["-X:mcp", ":port", "7888"]
```

# その他

## Emacsでやる場合の注意

LLM Agentによってファイルが更新されるため、以下を入れないと手動で再読み込みが必要

```elisp
;; ディスク上のファイルが変更されたら、バッファを自動的に再読み込みする
(global-auto-revert-mode 1)
```

## 使ってて感じた注意点

- clojure_eval ツールは、特に指定しない場合、デフォルトで user 名前空間でコードを実行する。作業する名前空間は明示的に指定する必要あり
- ライブラリのインストールを指示すると、deps.ednを書き換えてくれるので、nREPLサーバを再起動してインストールを完了する必要がある
