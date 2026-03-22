# iq-cli — Community Command-Line Interface

`iq-cli` is the community edition CLI for the IQ platform. It gives you terminal access to IQ's knowledge graph, connector ecosystem, and agent workflows — without needing to write a single line of Java.

It is designed to be approachable: start it, explore what's connected, run queries, and inspect your knowledge state — all from one command.

## What you can do with it

- Explore connected data sources and available connectors
- Run SPARQL queries and named scripts against your local knowledge graph
- Import and export realm knowledge to and from the local lake
- Inspect and trace agent state and transitions
- Work with local and remote RDF repositories

## Starting the CLI

```bash
./bin/iq-cli
```

This compiles and launches the community CLI in interactive mode.

## Requirements

- Java 21
- Maven (the wrapper in the repository handles everything else)
- The repository cloned locally

## Configuration

The CLI reads from environment variables and the `.iq/` directory in your working folder. Common settings:

- `IQ_HOME` — override the default state directory
- `AWS_ACCESS_KEY_ID` / `AWS_SECRET_ACCESS_KEY` — for AWS connector access
- `GITHUB_TOKEN` — for GitHub connector access

## Part of the IQ family

`iq-cli` is the open edition. For advanced orchestration, additional connectors, and enterprise workflow support, see `iq-cli-pro`.
