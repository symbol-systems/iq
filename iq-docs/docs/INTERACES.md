# INTERACES.md

- **`I_API<T>`** — `iq-platform/src/main/java/systems/symbol/tools/I_API.java`
  - Description: generic HTTP REST API wrapper for typed responses.
  - Key methods: `get()`, `get(Map)`, `post(Map)`, `put(Map)`, `delete(Map)`, `head(Map)`

- **`I_APIResponse<T>`** — `iq-platform/src/main/java/systems/symbol/tools/I_APIResponse.java`
  - Description: REST response wrapper with typed payload + HTTP status.
  - Key methods: `asData()`, `getStatus()`

- **`I_Assist<T>`** — `iq-platform/src/main/java/systems/symbol/llm/I_Assist.java`
  - Description: chat/assist conversation container (messages, helpers).
  - Key methods: `add(I_LLMessage)`, `messages()`, `latest()`, `system/assistant/user(...)`

- **`I_Agent`** — `iq-abstract/src/main/java/systems/symbol/agent/I_Agent.java`
  - Description: agent interface exposing working memory and state machine.
  - Key methods: `getThoughts()`, `getStateMachine()`

- **`I_Agentic<T>`** — `iq-platform/src/main/java/systems/symbol/agent/I_Agentic.java`
  - Description: facade for agent-like things (extends `I_Self`, `I_Facade`).

- **`I_Avatar`** — `iq-agentic/src/main/java/systems/symbol/agent/I_Avatar.java`
  - Description: composite of `I_Agent`, `I_Intent`, `I_Delegate<Resource>`.

- **`I_Bootstrap`** — `iq-abstract/src/main/java/systems/symbol/platform/I_Bootstrap.java`
  - Description: bootstraps IQ environment (boot(self, model)).
  - Key method: `boot(IRI self, Model model)`

- **`I_Budget` / `I_Fund` / `I_Treasury`** — `iq-agentic/src/main/java/systems/symbol/budget/*`
  - Description: domain interfaces for budget/fund/treasury management (project-specific).

- **`I_Corpus<T>`** — `iq-abstract/src/main/java/systems/symbol/finder/I_Corpus.java`
  - Description: corpus-level search helper (by concept → search).
  - Key methods: `byConcept(T concept)`

- **`I_Crawler<T>`** — `iq-platform/src/main/java/systems/symbol/lake/crawl/I_Crawler.java`
  - Description: web / lake crawler contract.

- **`I_Claim` / `I_Claimed`** — `iq-platform/src/main/java/systems/symbol/trust/*`
  - Description: trust/claim-related markers and helper contracts.

- **`I_Contents`** — `iq-abstract/src/main/java/systems/symbol/platform/I_Contents.java`
  - Description: access text-based assets; `getContent(subject, datatype)`.

- **`I_Crawler`** — listed above.

- **`I_Decide` / `I_Delegate<T>` / `I_Guard`** — `iq-abstract/src/main/java/systems/symbol/decide/*`
  - Description: decision/delegate patterns for runtime delegates and guards.

- **`I_Facade`** — `iq-abstract/src/main/java/systems/symbol/agent/I_Facade.java`
  - Description: small scripting facade exposing `getBindings()`.

- **`I_FacadeAPI`** — `iq-platform/src/main/java/systems/symbol/agent/I_FacadeAPI.java`
  - Description: string-friendly wrapper for scripting REST and file downloads.
  - Key methods: `api(String)`, `json(Response)`, `download(String)`

- **`I_Finder` / `I_Search` / `I_Find` / `I_ModelFinder` / `I_Indexer` / `I_Found`** — `systems/symbol/finder/*`
  - Description: embedding and semantic find/search/corpus contracts. Example: `I_Finder` handles embeddings (`embed`, `store`, `find(Embedding, ...)`) and `I_ModelFinder` returns RDF `Model` results.

- **`I_Fleet`** — `iq-abstract/src/main/java/systems/symbol/fleet/I_Fleet.java`
  - Description: fleet management contract (project-specific).

- **`I_Intents` / `I_Intent`** — `iq-abstract/src/main/java/systems/symbol/intent/*`
  - Description: intent and intent lists for agents. `I_Intent` defines intent contract for actors.

- **`I_KeyStore` / `I_Keys` / `I_TrustKeys`** — `iq-platform/src/main/java/systems/symbol/trust/*`
  - Description: key management; `I_Keys` provides `KeyPair keys()`.

- **`I_LLM` / `I_LLMConfig` / `I_LLMessage` / `I_ToolSpec`** — `iq-platform/src/main/java/systems/symbol/llm/*`
  - Description: SDK-quality LLM contracts: models, configs, messages and tool specs. `I_LLM` exposes `tools()` and `complete(chat)`.

- **`I_LoadSave`** — `iq-abstract/src/main/java/systems/symbol/platform/I_LoadSave.java`
  - Description: save/load contract with `save()` and `load()`.

- **`I_Realm` / `I_Realms`** — `iq-platform/src/main/java/systems/symbol/realm/*`
  - Description: realm abstraction (self identity, repository access, secrets, keys).
  - Key methods: `getModel()`, `getRepository()`, `getSecrets()`, `keys()`

- **`I_Response`** — `iq-apis/src/main/java/systems/symbol/controller/responses/I_Response.java`
  - Description: API response contract used across controllers.

- **`I_RiskException` / `I_Sovereign` / `I_Authority` / `I_TrustZone`** — `iq-platform/src/main/java/systems/symbol/trust/*`
  - Description: trust and sovereignty interfaces defining trust zones, authorities and risk handling.

- **`I_Secrets` / `I_SecretsStore`** — `iq-abstract/src/main/java/systems/symbol/secrets/*`
  - Description: secrets retrieval API (`getSecret(key)`), store contract.

- **`I_StartStop` / `I_Self`** — `iq-abstract/src/main/java/systems/symbol/platform/*`
  - Description: lifecycle and identity helpers. `I_StartStop` has `start()`/`stop()`. `I_Self` has `getSelf()` and static helpers like `version()` and `trust(...)`.

- **`I_StateListener`** — `iq-abstract/src/main/java/systems/symbol/fsm/I_StateListener.java`
  - Description: state transition listener contract (`onTransition(from,to)`).

- **`I_StateMachine<T>`** — `iq-abstract/src/main/java/systems/symbol/fsm/I_StateMachine.java` (detailed above)

- **Other smaller or domain-specific I_ interfaces**
  - Many modules declare `I_*` interfaces for domain boundaries (e.g., `I_Fund`, `I_Budget`, `I_Treasury`, `I_Fleet`, `I_Crawler`, `I_APIResponse`, `I_FacadeAPI`). See file list below for the full set.

