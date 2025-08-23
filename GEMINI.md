# GEMINI Project Context

## Project Overview

This is a Clojure project designed for mathematical computations. It serves as a testbed for exploring and implementing various algorithms, primarily focused on different methods for approximating Pi (π) and calculating Fibonacci numbers. The project is configured to work with the Gemini CLI and an nREPL server, allowing for interactive development and evaluation.

**Key Technologies:**

*   **Language:** Clojure
*   **Dependency Management:** `deps.edn`
*   **Testing:** `clojure.test`
*   **Code Contracts:** `clojure.spec.alpha`

**Core Functionality:**

*   **Fibonacci:** Includes both a standard recursive implementation and a memoized version for efficiency.
*   **Pi Approximation:**
    *   Gauss-Legendre Algorithm
    *   Standard Monte Carlo method
    *   Monte Carlo with triangular importance sampling
    *   Monte Carlo with 2D normal distribution importance sampling

## Building and Running

### nREPL Server

To enable interactive development, start an nREPL server. The project is pre-configured with an alias for this.

```bash
# Start the nREPL server (defaults to port 7888)
clojure -M:nrepl
```

### Running Tests

The `README.md` provides commands for running tests.

```bash
# Run all tests
clojure -M -e "(require '[clojure.test :as test]) (require '[myapp.core-test]) (test/run-all-tests #\"^myapp.*-test$\")"

# Run tests for a specific namespace
clojure -M -e "(require '[clojure.test :as test]) (require '[myapp.core-test]) (test/run-tests 'myapp.core-test)"
```

### Running the Main Function

The project has a `-main` function in the `myapp.core` namespace, which can be executed as follows:

```bash
# Run the main function
clojure -M -m myapp.core
```

## Development Conventions

*   **Source Code:** All source code is located in the `src/` directory, following standard Clojure conventions.
*   **Tests:** Tests are located in the `test/` directory. Each function in `myapp.core` has corresponding tests in `myapp.core-test`.
*   **Function Specs:** `clojure.spec.alpha` is used to define specifications (specs) for all public functions. These specs are defined at the top of the `myapp.core.clj` file and serve as contracts for function arguments and return values.
*   **Documentation:** Public functions include docstrings explaining their purpose.
