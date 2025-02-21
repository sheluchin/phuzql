# PhuzQL

PhuzQL is a Clojure-based proof of concept tool for building and executing
GraphQL queries against the Star Wars API. It provides an interactive
command-line interface to construct queries using the `fzf` fuzzy finder for
selecting query attributes.

See https://fnguy.com/phuzql_poc.html for more details.

## Features

- Interactive query building with `fzf`
- Integration with the Star Wars GraphQL API
- Supports nested query structures

## Prerequisites

- Babashka (`bb`)
- `fzf` installed on your system

## Usage

To execute a query, run the following command in your terminal from the repo root:

```bash
bb -m com.fnguy.phuzql.core/execute-query
```

This will start an interactive session where you can build and execute a query
against the Star Wars API.
