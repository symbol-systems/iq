## IQ: Intelligent Agent Ecosystem

IQ manages fleets of intelligent agents alongside their goals, actions, discoveries, decisions and creations. 

At its core, IQ looks after the `who, what, when, why, and how` of your AI ecosystem.

Use IQ to build intelligent systems, enabling them to understand, reason, act and adapt.

IQ provides a suite of tools and components for intelligent systems capable of understanding, interpreting, and responding to the world, planning and taking actions accordingly.

At the core of the framework are agents, entities equipped with pluggable skills that enable them to adapt and specialize in various domains and operational contexts.

The utilization of RDF facilitates the encoding of an agent's knowledge, states, and decision-making logic as semantic knowledge - an agent's digital DNA.

This enables agents to engage in self-reflection, anchor decisions in 'facts', engage in logical reasoning, synthesize new information, then adapt their "digital DNA" to become more capable.

Taking actions is a key aspect of agent functionality, allowing agents to translate user intentions into corresponding actions that can be executed.

### Project Aspirations

- **Operational Efficiency:** Streamline workflows, optimize decision-making, and save resources through intelligent agent behaviors and dynamic scripting.
- **Augmented Awareness:** Unlock insights and relationships for more informed decision-making.
- **Federated Collaboration:** Foster collaboration and knowledge sharing through federated knowledge integration, benefiting diverse user communities.
- **Transparency and Trust:** Implement transparent blockchain governance for secure and trustworthy processes.
- **Auditable Decisions:** Track the provenance of data, decisions and insights.
- **User-Friendly:** Improve user interaction with natural language and generative technologies.

### Project Features

Our features were built to support intelligent agent orchestration and knowledge management use cases such as:

| Capability                                    | Value Propositions                                                                                                                      |
|-----------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------|
| Knowledge Discovery              | Extract valuable insights, saving time and resources and curate an up-to-date knowledge base.                                           |
| Research and Curation                         | Actively retrieve information to stay informed about events, trends, and situations.                                                    |
| Federated Knowledge Integration               | Connect and share knowledge with external sources to enhance overall intelligence, fostering collaboration and enriching understanding. |
| Dynamic Agent Behavior                        | Simulate and optimize complex workflows and decision-making processes for more efficient and intelligent agent behaviors.               |
| Dynamic Scripting                             | You and your agents can create and execute scripts based on workflows to build standard operating procedures.                           |
| Semantic Data Storage and Retrieval           | Enable sophisticated knowledge exploration, uncovering meaningful insights and relationships within the knowledge base.                 |
| Natural Language Processing (NLP) Integration | Improve language understanding, enhancing user interaction and making the platform more accessible and user-friendly.                   |
| Blockchain Governance                         | Implement transparent and secure governance, establishing trust in the platform's processes and decision-making.                        |
| Graph-based Relationship Management           | Explore relationships between entities for predictive analysis, supporting better-informed decision-making and strategic planning.      |
| API-first EcoSystem                           | Grow by building APIs, consuming external APIs, integrating with services and leveraging additional functionalities.                    |

### Usage Scenarios

| User Audience       | Example Use Cases                                                                                                               |
|---------------------|---------------------------------------------------------------------------------------------------------------------------------|
| Professionals Users | - **AI Chat:** Engage with the AI fleet, ask questions, conduct research, share goals, insights and create value.               |
|                     | - **API Platform :** APIs to integrate with services, external data, apps and other AI.                                         |
| Business Users      | - **Sources of Truth :** Connect knowledge across teams, files, APIs, databases, AIs and humans.                                |
|                     | - **Blockchain:** Implement transparent governance for trustworthy decision-making and processes.                               |
| Data Scientists     | - **Semantic Linked Data:** Navigate and analyze data to uncover meaningful insights and relationships.                         |
|                     | - **Graph-based Relationship Management:** Harness relationships for comprehensive data analysis and planning.                  |
| Healthcare          | - **Decision Support:** Enhance diagnosis and treatment decisions with intelligent agent insights and knowledge.                |
|                     | - **Patient Data Analysis:** Leverage semantic data retrieval for in-depth analysis of patient records and medical research.    |
| Financial     | - **Market Analytics:** Analyze market data and predict trends by utilizing graph-based relationship management.                |
|                     | - **Regulatory Audits** : Integrate external financial (XBRL) data sources for fact-checked market analysis.                    |
| Supply Chain        | - **Dynamic Workflow Optimization:** Optimize supply chain processes through dynamic agent behavior and scripting capabilities. |
|                     | - **Relationship Visualization:** Use graph-based relationship management to visualize and enhance supply chain relationships.  |
| Education           | - **Curriculum Enhancement:** Stay updated on AI research for curriculum development with research and curation capabilities.   |
|                     | - **Knowledge Sharing:** Facilitate federated knowledge integration for collaborative research and information sharing.         |
| AI Researchers      | - **Knowledge Discovery:** Uncover new insights and trends in AI research, saving time and resources.                           |
|                     | - **Research and Curation:** Stay informed about the latest advancements, events, and breakthroughs in the AI field.            |
| AI Developers       | - **Dynamic Behavior:** Simulate and optimize workflows to create more efficient and intelligent AI agents.                     |
|                     | - **Code Co-Pilot:** Co-develop queries and scripts that enact workflows and standard operating procedures.                     |

### Technology Stack

IQ follows a Java mono-repository structure managed through Maven.

The IQ platform is built using Java, with the Quarkus framework serving as its primary foundation.

Our project incorporates the amazing work of so many, including but not limited to:

- **Java**: widespread, enterprise libraries, portability and integration.

- **RDF4j**: A semantic data management for storing and querying RDF-based knowledge.

- **Apache Camel**: A semantic data management for storing and querying RDF-based knowledge.

- **LangChain4J**: Library to manage embeddings and vector search for multiple vendors.

- **Quarkus**: Efficient and resource optimized runtime.

### Maven Modules 

IQ comprises a set of integrated modules handling data, communication and interaction among humans and AI. 

The Maven modules include:

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

