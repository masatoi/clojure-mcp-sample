# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Clojure neural network project implementing multi-layer perceptron (MLP) for function approximation, specifically designed to approximate the sin function using supervised learning. The project demonstrates machine learning concepts using pure Clojure with the core.matrix library.

## Key Components

### Neural Network Architecture
- **`neural_network.clj`**: Core MLP implementation with configurable layers, activation functions (sigmoid, tanh, ReLU, linear), forward/backward propagation, and training algorithms
- **`dataset.clj`**: Data generation utilities for creating sin function datasets with noise, normalization, and train/test splitting
- **`sin_approximation.clj`**: Complete demonstration pipeline that trains and evaluates neural networks for sin function approximation
- **`core.clj`**: Basic utilities including fibonacci functions (both recursive and memoized)

The neural network uses matrix operations via vectorz-clj backend and implements standard backpropagation with configurable learning rates and epochs.

## Development Commands

### REPL Development
Start nREPL server for interactive development:
```bash
clojure -M:nrepl
```
This starts nREPL on port 7888 with JVM options for attach capability.

### Testing
Run specific test namespace:
```bash
clojure -M -e "(require '[clojure.test :as test]) (require '[myapp.core-test]) (test/run-tests 'myapp.core-test)"
```

Run all tests:
```bash
clojure -M -e "(require '[clojure.test :as test]) (require '[myapp.core-test]) (test/run-all-tests #\".*-test$\")"
```

### Neural Network Demo
To run the sin approximation demo, evaluate in REPL:
```clojure
(require '[myapp.sin-approximation :as sin])
(sin/demo-sin-approximation)
```

## MCP Integration

This project is configured to work with clojure-mcp for Claude Code integration. The nREPL server must be running on port 7888 for MCP tools to function properly.

## Dependencies

- `net.mikera/core.matrix`: Matrix operations library
- `net.mikera/vectorz-clj`: High-performance vector/matrix backend
- `nrepl/nrepl`: REPL server for development

## Architecture Notes

The codebase follows functional programming principles with immutable data structures. The neural network is implemented as a record containing weights, biases, and activation functions. Training data flows through a pipeline of generation → normalization → training → evaluation.

Key design patterns:
- Records for network state management
- Higher-order functions for configurable activation functions
- Matrix operations abstracted through core.matrix
- Separation of concerns between data generation, network logic, and demonstration code