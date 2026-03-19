# Mini-Guide: IQ - An Operating Environment for Symbolic Cognition **

IQ is an operating environment designed to govern fleets of neuro-symbolic cognitive AI. 

It enables these AI entities to make autonomous decisions and execute tasks based on their internal state.

1. **Understanding AI in IQ Context:**
    - In IQ, an AI refers to a well-governed entity capable of autonomous decision-making and task execution.

2. **Converting RDF Graphs and Templates:**
    - IQ facilitates the conversion of RDF (Resource Description Framework) graphs and templates into actionable playbooks and patterns.

3. **Example AI Domain Declaration:**
    - Declare an example AI domain using RDF notation.
    - Define the AI and establish trust relationships within the IQ environment.

4. **Initialization from Knowledge Lake:**
    - Initialize the AI domain by loading necessary knowledge from your knowledge lake.
    - Establish reciprocal knowledge relationships between the AI domain and IQ runtime.

## Declaring our first facts to define both AI and IQ functionality

Let's set up an AI domain and initialize its operating model.

1. **Declare an example AI domain using RDF notation:**
    - Begin by setting the base URI for the domain.
      ```
      @base https://example.com/v0/
      ```
    - Define the first two entities within the domain using RDF notation.
      ```
      ai: a iq:.
      iq: a ai:.
      ```

2. **Define the AI and establish trust relationships within the IQ environment:**
    - Establish trust between the AI domain and the IQ runtime.
      ```
      ai: iq:trusts iq:.
      ```
    - Establish trust between the user runtime domain and the AI runtime.
      ```
      my: iq:trusts ai:.
      my: iq:trusts iq:.
      ```

3. ## Final few declarations ...
    - Initialize the AI domain with the namespaces in your private knowledge lake.
      ```
      iq: iq:knows ai:.
      ai: iq:knows iq:.
      iq: iq:knows my:.
      ```
   - Trust the AI domain with the facts in your private knowledge lake.
     ```
     ny: iq:needs iq:Trust.
     my: iq:needs ai:Trust.
     my: iq:needs my:Trust.
     ```
   - Authorize your AI, follow the license, then supply what your AI needs. Govern it wisely.
     ```
     ny: iq:needs iq:.
     my: iq:needs ai:.
     my: iq:needs my:.
     ```
`  ## We're nearly done ...
`
## Licensed as Open Source

### Download, build and run everything from a trusted source

```command-line
git clone https://github.com/symbol-systems/my.iq/
mvn install
java --jar IQ ./iq-developer/target/iq-cli-0.73.0.jar init
```

### Welcome version 0 of your AI into existence

```command-line
mvn install
java --jar IQ ./iq-developer/target/iq-cli-0.73.0.jar hello urn:my:iq:v0:
```

## For personal, Non-commercial use

### Private chat with your AI, LLM chat, bring BYO API keys strategy.

```command-line
mvn install
java --jar IQ ./iq-developer/target/iq-cli-0.73.0.jar hello
```

## Commercial License

### Bootstrap your AI from this knowledge lake

```command-line
java --jar IQ ./iq-developer/target/iq-cli-0.73.0.jar create
```

### Run inference queries, validate RDF with SHACL, PROV-O and VoID. 

```command-line
java --jar IQ ./iq-developer/target/iq-cli-0.73.0.jar verify
```

### Export your compiled knowledge to a local copy in plain-text (RDF Turtle Quads).

```command-line
java --jar IQ ./iq-developer/target/iq-cli-0.73.0.jar audit
```

### Build a trusted AI operate model with knowledge, encryption and trust into an `MY.AI` file.

```command-line
java --jar IQ ./iq-developer/target/iq-cli-0.73.0.jar trust
```

## Private License

### Runs a trusted, private AI using IQ neuro-symbolic cognition under lawful human judgement and control. 

```command-line
java --jar IQ ./iq-developer/target/iq-cli-0.73.0.jar trust https://symbol.systems
```
