# Repository Guidelines

## Project Structure & Modules
- `src/myapp/`: Application code. Key modules include `core.clj` (algorithms and entry point), `dataset.clj`, `neural_network.clj`, and `sin_approximation.clj`.
- `test/myapp/`: Unit tests (e.g., `core_test.clj`).
- `deps.edn`: Dependencies and aliases (e.g., `:nrepl`).

## Build, Test, and Development Commands
- Run app: `clj -M -m myapp.core` — executes `-main` in `myapp.core`.
- Run tests (all): `clj -M -m clojure.test` — discovers and runs `clojure.test` suites.
- Run tests (single ns): `clj -M -m clojure.test myapp.core-test`.
- Start nREPL: `clj -M:nrepl` — starts nREPL on port 7888 (see `deps.edn`).
- REPL (CLI): `clj -M` — loads `src` and `test` on the classpath.

## Coding Style & Naming Conventions
- Indentation: 2 spaces; prefer thread-first/last macros for clarity.
- Naming: namespaces mirror paths (`myapp.core` -> `src/myapp/core.clj`), vars in `kebab-case`.
- Public APIs: add `s/fdef` specs for args/return (see existing specs in `core.clj`).
- Formatting/Linting: No formatter configured; follow idiomatic Clojure style and keep diffs minimal.

## Testing Guidelines
- Framework: `clojure.test`.
- Location/Names: `test/myapp/*_test.clj` with namespaces like `myapp.core-test`.
- Run locally before PRs: `clj -M -m clojure.test`. Add edge cases and negative tests where relevant.

## Commit & Pull Request Guidelines
- Commit style (from history): short, imperative subjects (e.g., "Add …", "Fix …", "Change …"). Keep to ~50 chars; add a concise body when useful.
- PRs: include a clear description, motivation, and scope; link issues; note any behavior or API changes; include run instructions or screenshots when applicable.
- Checklist: tests pass locally; new code is covered; docs updated (`README.md` or inline docstrings/specs).

## Architecture Notes
- This repo focuses on algorithmic implementations (e.g., Fibonacci, sorts, prime sieve, Monte Carlo π). Prefer small, pure functions with clear inputs/outputs and add new algorithms under `src/myapp/`, exposing user-facing entry points via `myapp.core` when appropriate.

