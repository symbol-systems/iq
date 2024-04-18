# IQ: Agents, Actions and Autonomy

IQ is a platform designed for the development and deployment of intelligent agents with natural language processing (NLP) abilities. 

### Technology Stack

`IQ` follows a Java mono-repository structure managed through Maven.

The `IQ` platform is built using Java, with the Quarkus framework serving as its primary foundation.

Our project incorporates the amazing work of so many, including but not limited to:

- **Java**: widespread, enterprise libraries, portability and integration.

- **RDF4j**: A semantic data management for storing and querying RDF-based knowledge.

- **Apache Camel**: A semantic data management for storing and querying RDF-based knowledge.

- **LangChain4J**: Library to manage embeddings and vector search for multiple vendors.

- **Quarkus**: Efficient and resource optimized runtime.

## Behavior and functionality of agents.

#### `I_Agent` Interface

- **Purpose**: Defines the contract for agents with pluggable skills within the Symbol Systems framework.
- **Methods**:
- `void setModel(Model model)`: Sets the RDF4J model for the agent.
- `Model getModel()`: Retrieves the knowledge associated with the agent.
- `I_StateMachine<Resource> getStateMachine()`: Returns the state machine associated with a given task.
- `void learn(I_StateMachine<Resource> fsm)`: Associates tasks with corresponding state machines for learning.
- `IRI getIdentity()`: Returns the identity (IRI) of the agent.
- `Resource decide(Resource state) throws StateException`: Makes a decision based on the provided state.

#### `I_Instruct` Interface

- **Purpose**: Represents instructions provided to agents, defining the subject, intent, and object of an action.
- **Methods**:
- `Resource getSubject()`: Retrieves the subject of the instruction.
- `I_Intent getIntent()`: Retrieves the intent (action) associated with the instruction.
- `Resource getObject()`: Retrieves the object (target) of the instruction.

#### `I_Intent` Interface

- **Purpose**: Defines the contract for processing intents (actions) based on provided instructions.
- **Methods**:
- `Set<IRI> execute(IRI subject, Resource object) throws StateException`: Executes an action based on the provided subject and object.

#### `I_Decision` Interface

- **Purpose**: Defines the contract for a decision maker.
- **Methods**:
- `Set<IRI> decide(Resource state) throws StateException`: Executes a decision to change state.

## Implementations of I_Intents (actions)

#### `AbstractIntent` Class

- **Purpose**: Abstract class providing common functionality for intent processing.
- **Fields**:
- `Model model`: The knowledge associated with the intent.
- `IRI self`: The identity (IRI) of the intent.
- **Constructor**:
- `AbstractIntent(Model model, IRI self)`: Initializes the intent with knowledge and identity.
- **Methods**:
- `Set<IRI> execute(IRI subject, Resource object)`: Executes an action based on the provided subject and object.

#### `JSR233` Class

- **Purpose**: Represents an intent capable of executing scripts using JSR 233 scripting engines.
- **Constructor**:
- `JSR233(Model model, IRI self)`: Initializes the JSR233 intent with knowledge and identity.
- **Methods**:

## Functional I_Agents (entities that make decisions and execute actions)

#### `AbstractAgent` Class

- **Purpose**: Abstract class providing common functionality for agents.
- **Fields**:
- `Model model`: The knowledge associated with the agent.
- `IRI self`: The identity (IRI) of the agent.
- **Constructor**:
- `AbstractAgent(Model model, IRI self)`: Initializes the agent with knowledge and identity.
- **Methods**:
- `boolean onTransition(Resource from, Resource to)`: Handles transitions between states.
- `Set<IRI> execute(IRI subject, Resource object) throws StateException`: Executes an action based on the provided subject and object.

#### `IntentAgent` Class

- **Purpose**: Represents an agent that executes intents (actions) based on provided instructions.
- **Fields**:
- `I_Intent intent`: The intent to be executed by the agent.
- **Constructor**:
- `IntentAgent(I_Intent intent, Model model, IRI self)`: Initializes the IntentAgent with the provided intent, RDF4J model, and identity.
- **Methods**:
- `Set<IRI> execute(IRI subject, Resource object) throws StateException`: Executes an action based on the provided subject and object.

#### `LazyAgent` Class

- **Purpose**: Represents a simple agent that performs no actions, used for testing or placeholder purposes.
- **Constructor**:
- `LazyAgent(Model model, IRI self)`: Initializes the LazyAgent with knowledge and identity.
- **Methods**:
- `boolean onTransition(Resource from, Resource to)`: Handles transitions between states.
- `Set<IRI> execute(IRI subject, Resource state) throws StateException`: Executes an action based on the provided subject and state.

#### `LLMAgent` Class

- **Purpose**: Represents an agent that interacts with a Language Model (LLM) for natural language processing tasks.
- **Fields**:
- `I_LLM<String> llm`: The Language Model with which the agent interacts.
- `I_Agent agent`: The underlying agent to which LLMAgent delegates decisions.
- **Constructor**:
- `LLMAgent(I_LLM<String> llm, I_Agent agent)`: Initializes the LLMAgent with the provided Language Model and agent.
- **Methods**:
- `boolean isOnline()`: Checks if the LLMAgent is online and ready for interaction.
- `I_Thread<String> say(String message) throws APIException, IOException, StateException`: Sends a message to the LLMAgent for processing.

#### `ScriptAgent` Class

- **Purpose**: Represents an agent capable of executing scripts using JSR 233 scripting engines.
- **Constructor**:
- `ScriptAgent(Model model, IRI self)`: Initializes the ScriptAgent with knowledge and identity.

