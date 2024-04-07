# Maven Build Guide

IQ comprises a set of integrated modules handling data, communication and interaction among humans and AI.

### The Maven modules include:

- **iq-cli:** Command-line interface for knowledge base bootstrapping.
- **iq-blockchain:** Design, build, deploy, and govern through Ethereum smart contracts.
- **iq-finder:** Module dedicated to locating and retrieving information.
- **iq-fsm:** Finite State Machine implementation for agent behavior modeling.
- **iq-graphs:** Component for managing and visualizing relationships between entities.
- **iq-moat:** Module ensuring the discovery, curation, and storage of knowledge.
- **iq-onto:** Ontology module defining the structure and semantics of IQ.
- **iq-platform:** Core  functionality and governance mechanisms.
- **iq-rdf4j:** Integration with RDF4j for semantic data storage and retrieval.
- **iq-rdf4j-camel:** RDF4j and Apache Camel for semantic integration.
- **iq-rdf4j-graphql:** GraphQL integration for flexible query capabilities.
- **iq-rdf4j-nlp:** Integration for language understanding.
- **iq-run-apis:** Module for executing external APIs within the IQ ecosystem.
- **iq-strings:** String manipulation utilities for handling textual data.
- **iq-commons:** Shared functionalities and utilities across the IQ ecosystem.

### The build system steps:

1. **Clean and Install:**

`mvn clean install`
- cleans the project, then compiles the source code, runs tests, and packages the project to ensure a fresh and consistent build.

2. **Compile Only:**

`mvn compile`
- compile the source code. Helpful during the development phase for faster feedback on code changes.

3. **Run Tests:**

`mvn test`
- Compile then execute the tests without packaging the project. Useful for running unit tests and ensuring code integrity.

4. **Generate Javadoc:**

`mvn javadoc:javadoc`
- Generate Javadoc documentation for the project. This is helpful for developers looking to extend or improve the code.

5. **Dependency Updates:**

`mvn versions:display-dependency-updates`
- Identify any available updates for project dependencies. Useful for keeping dependencies up-to-date and addressing security or functionality improvements.

6. **Dependency Tree:**

`mvn dependency:tree`
- Display the project's dependency tree, showing the hierarchy of dependencies. Useful for understanding the project's dependencies and resolving any conflicts.

7. **Package as JAR:**

`mvn package`
- Package the compiled code into a JAR file without running tests. Useful for creating distributable artifacts.

8. **API Platform:**

`mvn compile quarkus:dev -pl iq-run-apis -am`
- Run the Quarkus in development mode for live coding and testing. 

