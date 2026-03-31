# Glossary of Terms

## A

**Agent**
An autonomous decision-making entity in IQ that consults knowledge rules, makes decisions, and triggers actions. Agents have state machines that enforce valid transitions.

**API Key / Token**
A credential used to authenticate requests to IQ or external services (like OpenAI, Slack, GitHub). Keep these secret.

## C

**Chat API**
A REST endpoint that accepts natural language questions and returns grounded answers using both the knowledge graph and an LLM.

**Connector**
A plug-in that integrates IQ with external systems (Slack, AWS, GitHub, databases, etc.). Connectors can read data into the knowledge graph or write decisions out to systems.

**CRUD**
Create, Read, Update, Delete — the four basic operations on data.

## D

**Data Lake**
A centralized repository of structured and unstructured data. IQ can ingest from data lakes and also act as one.

## E

**ETL**
Extract, Transform, Load — the process of pulling data from source systems, transforming it, and loading it into a target system. IQ supports ETL workflows.

## F

**Fact**
A single piece of information in the knowledge graph. Example: "Alice is the CEO of Acme Corp". Facts are stored as RDF triples.

**FSM (Finite State Machine)**
A model of computation with a fixed set of states and rules for transitioning between them. Workflows in IQ are defined by FSMs.

## G

**Grounded / Grounding**
When an AI's response is based on verified facts rather than guesses or hallucinations. IQ grounds LLM responses in real data.

**GraphQL**
A query language for APIs. IQ exposes SPARQL results as GraphQL endpoints.

## J

**JWT (JSON Web Token)**
A secure way to authenticate users and manage sessions. IQ uses JWTs for multi-tenant isolation.

## K

**Knowledge Graph**
A database of structured facts (RDF triples) that represent relationships and entities. The "knowledge" that IQ uses to ground decisions.

## L

**LLM (Large Language Model)**
An AI model trained on large text corpora (like GPT-3.5, GPT-4, Llama, Claude). IQ integrates with LLMs to add intelligence to decisions.

## M

**MCP (Model Context Protocol)**
A protocol that lets LLMs call IQ as a tool, giving them access to enterprise data and workflows.

**Multi-tenant**
A system that isolates data and configuration between multiple customers or teams. IQ supports multi-tenancy via realms.

## P

**Policy**
A declarative rule that governs behavior. "Approve if amount < $5k AND requester is manager" is a policy. Policies are written in RDF/SPARQL.

**PQL (Policy Query Language)**
A way to express business rules in a human-readable format that compiles to SPARQL.

## Q

**Query**
A request for information from the knowledge graph, usually in SPARQL format.

## R

**RAG (Retrieval-Augmented Generation)**
A technique where an LLM retrieves relevant facts from a knowledge graph to ground its response. IQ's core capability.

**RDF (Resource Description Framework)**
A standard for representing facts as triples: subject-predicate-object. Example: "Alice | is-CEO-of | Acme Corp"

**Realm**
A tenant's private knowledge graph, secrets store, and rule set. Realms don't see each other's data. Used for multi-tenancy.

## S

**SPARQL**
The query language for RDF graphs. Like SQL for knowledge graphs.

**State Machine**
A model that defines valid states and transitions. IQ workflows are built on state machines.

**Stateful**
Maintaining context across multiple interactions. "Where are we in the approval process?" is a stateful question. IQ is stateful.

## T

**TTL (Turtle)**
A text format for writing RDF triples. Example:
```
alice a Person ; name "Alice" .
```

**Token**
In LLM context: a small unit of text (roughly 4 characters per token). LLM costs are measured in tokens. In security context: see API Key.

## V

**Vault**
A secure storage for secrets (API keys, passwords, tokens). IQ stores connector credentials in a vault.

## W

**Workflow**
A multi-step process with decision points and state transitions. Approval workflows, incident response, order fulfillment, etc.

---

## More resources

- **Concepts:** [WHAT_IS_IQ.md](WHAT_IS_IQ.md)
- **Getting started:** [QUICKSTART.md](QUICKSTART.md)
- **Use cases:** [USECASES.md](USECASES.md)
