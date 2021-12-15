# Ethereum Indexer
This app is used to index the Ethereum blockchain in a postgresql database, exposing a REST API with the necessary data to build a light wallet.

As of now, this powers the Ethereum Wallet used by the [Stakenet DEX](https://stakenet.io/dex/).


## Goal
As of now, there are two main APIs exposed (check the [routes](./src/main/resources/routes) file for more details):
- An endpoint that exposes the current USD price based on the info from [coinmarketcap](https://coinmarketcap.com), which syncs the price in the background making sure to not consume all the credits.
- An endpoint to list all the transactions by a given address (including token transfers).

## Usage
First of all, make sure to run an Ethereum Node, check [install-geth](./docs/install-geth.md) for a way to do so.

Update the [application.conf](./src/main/resources/application.conf) to define the settings for your own environment.

[https://sdkman.io](sdkman) is the suggested way to pick the correct Java version to run the project, then:
- `sbt compile` compiles the app.
- `sbt test` runs the tests.
- `sbt run` runs the app.

