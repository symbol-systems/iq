## IQ - operating environment for cognitive AI

IQ provides the infrastructure and tools necessary for the development, deployment, and operation of AI systems that integrate neural and symbolic reasoning. 

In essence, a framework for building and harnessing the power of intelligent systems that combine the strengths of both neural and symbolic approaches to AI.

At the heart of `IQ` is the `mind graph`, a declarative AI model based on RDF.

As a cognitive toolkit, `IQ` is fed on knowledge - these are your facts, your data, your processes, policies, goals, tools, actions and more.

## Knowledge Axioms

Knowledge within the IQ framework is meticulously curated across five distinct trust zones: `Code`, `AI`, `Agents`, `Curated`, and `Community`. 

Each zone operates within its own unique namespace, ensuring clear delineation and organization of concepts. 

Within these namespaces, concepts are assigned well-known, immutable IRIs (International Resource Indicators) for uniqueness and disambiguation.

### Core Namespaces:

| Prefix | Namespace                   |
|--------|-----------------------------|
| iq     | <iq:>                   |
| ai     | <urn:ai:>                   |
| my     | <urn:my:{uuid}:>            |


These namespaces serve distinct purposes: 

the codebase utilizes `iq:`, AI axioms leverage `ai:`, and each agent is assigned a dedicated `my:` namespace, fostering modularity and clarity across the IQ ecosystem.

## Curated Knowledge

A curated namespace is published by an organization with a well-known domain, for example `w3.org`. 

Concepts within each namespace must have permanent, well-known, immutable IRIs (international resource indicators).

Every imported ontology/schema/vocabulary must be defined in terms from one (or more) of the following curated namespaces.

| Prefix  | Curated Core Namespace                        |
|---------|-----------------------------------------------|
| owl     | <http://www.w3.org/2002/07/owl#>              |
| prov    | <http://www.w3.org/ns/prov#>                  |
| rdf     | <http://www.w3.org/1999/02/22-rdf-syntax-ns#> |
| rdfs    | <http://www.w3.org/2000/01/rdf-schema#>       |
| skos    | <http://www.w3.org/2004/02/skos/core#>        |
| xsd     | <http://www.w3.org/2001/XMLSchema#>           |
| schema  | <http://schema.org/>                          |

## Cognitive Knowledge

`IQ` empowers your agents with cognitive capabilities including inference and perception.

At its core, `IQ` empowers intelligent systems to comprehend, reason, act, and adapt, offering a comprehensive suite of tools and components for interpreting and responding to the world.

The neuro-symbolic model emulates automatic and autonomic responses (system-1) and more intuitive, deliberate, analytical thinking (system-2).

## Agentic Knowledge

Agents acquire a semantic understanding of their domain, storing knowledge in a graph database that can be queried, analysed and exported.

Equipped with pluggable skills, agents can make decisions based on that knowledge.

They follow your playbook, using both neural and logical decision-making. They can even develop and adapt their own workflows.

Their explicit knowledge confers a sense of themselves , they can query and update their own state of mind.

## Federated Knowledge

Knowledge within the IQ framework facilitates seamless collaboration and interoperability between trusted parties. 

This approach enables federation, cloning, import, and export of knowledge across different entities, promoting a inclusive and cohesive AI ecosystem. 

By blending knowledge from various sources, organizations can leverage diverse expertise and insights to enhance their AI capabilities. 

This process ensures that valuable knowledge can be easily shared, replicated, and integrated into different contexts, fostering innovation and accelerating progress in the field of AI.


## Understanding Knowledge

Knowledge is the cornerstone of intelligence within the IQ framework. 

It serves as the foundation upon which intelligent systems comprehend, reason, and act. 

Let's explore the role of knowledge IQ:

- **RDF:** Organizes information flow for efficient decision-making.
- **SPARQL:** Use SPARQL queries to `construct`, `describe`, `insert` and `delete` your domain knowledge.
- **Executable:** The IQ operate model embodies executables such as workflows, cognitions and scripting. 
- **State-Based:** Utilizes Finite State Machines for structured decision-making.
- **Structured :** Extracts insights from data lakes, databases, facts graphs, linked data or uploaded files.
- **Neural:** Includes natural language and neural algorithms such as LLMs, RNNs.

### Strategic Knowledge

- **Sequential:** Step-by-step processing flow for simple tasks.
- **Trees of Thought:** Organizes information flow for efficient decision-making.
- **Forests of Knowledge:** Enriches understanding through interconnected concepts.
- **Command and Control:** A central registry of your agents, knowledge and their collaborations.
- **Situation Awareness:** Reacts to incoming signals then triggers behaviours to process.

### Episodic Knowledge (Memory)

- **Contextual:** Facilitates dynamic adaptation to varying contexts.
- **Working:** Enables transient storage and manipulation of information.
- **Semantic:** Stores factual knowledge crucial for decision-making.
- **Episodic:** Recalls past events for informed decision-making.
- **Perceptual:** Stores and retrieves sensory information.
- **Persistent:** Stores and retrieves binary and other bulky documents.

### Behavioral Knowledge

- **Stateful:** Managed workflows that capture standard operating procedures.
- **Side-Effects:** Trigger actions that can include built-in tools, APIs and scripts.
- **Scripting:** Enhances adaptability through scripts that can be written by you or your agents.
- **API Calling:** Integrates external APIs for dynamic information retrieval.
- **Curiosity** Actively retrieves and curates information to deepen understanding.

### Formal Knowledge

- **Ontology:** A knowledge graph of domain models and relationships.
- **Taxonomy:** A classification that relates entities into meaningful hierarchies.
- **Retrieval-Augmented (RAG):** Dynamically retrieves information to enhance responsiveness.
- **Fact-Augmented (FAG):** Integrates relevant knowledge for contextually accurate responses.
- **Synthesis:** Links together knowledge from unstructured documents and other sources.

### Runtime Knowledge

- **Self:** Observes and monitors vital statistics of the operating environment, runtimes and online dependencies.
- **Protection:** Monitors it's security posture, maintains a threat model and proposes remediation.
- **Reflection:** Fosters self-awareness and iterative improvements.
- **Adaption:** Empowers autonomous adjustment and optimization.
- **Awareness:** Runtime transparency and reliability.
