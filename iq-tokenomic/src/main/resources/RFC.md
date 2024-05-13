# RFC Draft: Fact Claims - Trustworthy Agentic Collaboration across Autonomous Decentralized Ecosystems

## Status of This Document

This document is a draft (`v0.4`) and has not yet been finalized. Feedback and contributions from the community are welcome. Please refer to the latest version of this document for the most up-to-date information.

Feedback and comments on this specification are welcomed at any time. However, readers should note that the comment period for this specific version of the document has concluded, and the Working Group will not be making substantive modifications to this version of the specification at this stage. 

Any issues or concerns can be reported directly on GitHub, for  more information visit [fact.claims](https://fact.claims/about/contact).

The Working Group has received valuable implementation feedback, demonstrating that there are at least two implementations for each normative feature outlined in the specification. 

To delve into further details, please consult the code, test suite and use case reports.

---

## Abstract

In a decentralized, AI-first environment, maintaining trustworthy and immutable data is essential for ensuring transparency, auditability, and reliability. 

This paper proposes a methodology for leveraging the InterPlanetary File System (IPFS), Linked Data (JSON-LD), and Smart Contracts to construct a trusted decentralized network of fact graphs. 

By utilizing the distributed, immutable, and censorship-resistant IPFS, we address the challenges associated with decentralized trust and enhance the integrity and auditability of diverse fact claims. 

We outline the technical architecture, design considerations, and implementation strategies for integrating IPFS into existing systems to establish a reliable and tamper-resistant repository for storing and accessing fact claims.

---

## 1. Introduction

Given rapidly evolving artificial intelligence (AI), the need for trustworthy and immutable data has become paramount. 

Decentralized ecosystems, built upon principles of transparency, auditability, and reliability, require robust mechanisms for managing and verifying information exchange. In this context, the establishment of fact claims - assertions of truth or validity regarding specific entities, relationships, or events - plays a crucial role in facilitating trustful collaboration across autonomous decentralized ecosystems.

The fact claims ecosystem serves as the backbone for managing trusted collaborations and value exchange in diverse domains, including research, innovation, curation, collaboration, and creativity. It encompasses a wide range of activities, from asserting research findings and coordinating project activities to managing legal contracts and tracking supply chain transactions. However, ensuring transparency, accountability, and interoperability within such a trustful ecosystem requires a standardized approach for representing facts - a knowledge graph.

Traditional centralized storage solutions, while effective in some contexts, pose several challenges in decentralized environments. These challenges include single points of failure, data manipulation risks, and vulnerabilities to censorship and tampering. In contrast, decentralized technologies offer innovative solutions to address these challenges and enhance the integrity and auditability of fact claims.

This RFC proposes a methodology for leveraging the InterPlanetary File System (IPFS), Linked Data (JSON-LD), and Smart Contracts to construct a trusted network of fact graphs. By harnessing the distributed, immutable, and censorship-resistant nature of IPFS, we aim to address the challenges associated with decentralized trust and establish a reliable repository for storing and accessing fact claims. Through the use of JSON-LD, we ensure semantic interoperability and compatibility, enabling machine-readable representation and interpretation of fact claims. Additionally, Smart Contracts provide governance mechanisms for managing the lifecycle of fact claims, ensuring transparency, and enforcing trust among participants.

The technical architecture outlined in this RFC provides a blueprint for integrating IPFS into existing systems to establish a tamper-resistant repository for storing and accessing fact claims. By adopting standard ontologies such as PROV-O, SKOS, and VOID, we enhance interoperability and facilitate knowledge organization within the ecosystem. Furthermore, the validation mechanisms outlined in this RFC ensure compliance with predefined constraints and rules, ensuring data integrity and consistency across fact graphs.

In the following sections, we delve deeper into the objectives, technical details, use cases, security considerations, and implementation strategies for constructing trusted fact claims. 

Through this RFC, we aim to lay the groundwork for a future where collaboration thrives securely and reliably, fostering innovation, transparency, and trust among participants.

---

## 2. Objective

This section defines the objectives of the proposal, including the goals, scope, and intended outcomes. It outlines the need for a standardized approach to representing fact claims  and identifies the key technologies and standards utilized.

2.1. **Ensure Trustworthiness**: Ensure that the facts are trustworthy and immutable - fostering trust among stakeholders.

2.2. **Semantic Interoperability**: Incorporating standards such as PROV-O, SKOS, and VOID annotations facilitates semantic interoperability. Enable seamless information exchange and collaboration across diverse domains and platforms.

2.3. **Knowledge Curation**: The fact graph should serve as a structured repository for organizing and discovering relevant facts by adhering to standards, ontologies and vocabularies, such as PROV-O, SKOS, and VOID.

2.4. **Transparent and Reproducible**: By capturing provenance information using PROV-O, the fact graph enables transparent and reproducible research. Auditors can trace the lineage of data and assertions, understand how they were derived or obtained, and verify their authenticity. 

2.5. **Reasoning and Analysis**: The fact graph facilitates automated reasoning and analysis. By representing data and relationships using standardized RDF, tools can infer new knowledge, detect patterns, and derive insights.

2.6. **Privacy and Security**: While promoting openness and transparency maintain privacy and security of sensitive information. Access control is part of the graph, identifying confidential data, authorized agents, privacy regulations and ethical standards.

2.7. **Collaboration and Sharing**: By providing APIs, query interfaces, and visualization tools, the graph enables seamless collaboration and communication, accelerating the pace of discovery and innovation.

---

## 3. Fact Claims Protocol

The `fact graph` describes how fact claims are represented as linked data serialized in JSON-LD format. 

JSON-LD provides a lightweight and flexible means to express semantic information in a structured and easily understandable manner. 

By following these steps, the Fact Claim protocol ensures the integrity, transparency, and traceability of asserted claims.

### 3.1. **Create Factual Claims in RDF Format**

- 3.1.1 Define the factual claims using RDF format.

```turtle
@prefix prov: <http://www.w3.org/ns/prov#> .

<https://fact.claims/> prov:generated <https://example.claims/facts/claim1>.

<https://example.claims/facts/claim1> prov:wasAttributedTo <ethereum://0x123abc...> .
```

- 3.1.2. Make useful and necessary assertions with reference to relevant ontologies.

```json-ld
<https://example.claims/facts/claim1>

    rdf:label 'fact claim #1';
    
    rdf:comment 'Trusted Facts for Autonomous Agents'.
```

### 3.2. **Attribution to Ethereum Smart Contract Address**

- 3.2.1. Embed `prov:wasAttributedTo` within the RDF claims to the Ethereum smart contract address (`ethereum://0x123abc...`) serving as the originating entity.

- 3.2.2. These links and backlinks establish the provenance of the claims and provide a traceable connection to the blockchain.

### 3.3. **Link to Websites with JSON-LD References**

- 3.3.1. Include links within the RDF claims to websites containing JSON-LD (Linked Data) documents.

- 3.3.2. These JSON-LD documents further elaborate on and reference the asserted claims, providing additional context and information.

### 3.4. **Convert RDF Claims to JSON-LD Format**

- 3.4.1. As necessary, convert the RDF claims into JSON-LD format to maximize compatibility with other systems and applications.

- 3.4.2. JSON-LD provides a standardized way to represent linked data in JSON format, facilitating interoperability and integration.

### 3.5. **Publish Claims to IPFS**

- 3.5.1. Publish the RDF as JSON-LD to the IPFS (InterPlanetary File System) network.

- 3.5.2. IPFS assigns a unique cryptographic hash to each piece of content, ensuring its immutability and availability.

Example URL format: `ipfs://QmXqPj9sk...`

- 3.5.3. We can't include references to the `ipfs://` URL within its own document, however one can and should reference URLs of previously published IPFS claims.

### 3.6. **Emit FactClaims Event**

- 3.6.1. Emit a FactClaims event on the Ethereum blockchain, containing the `ipfs://...` URL of the published claims.

```solidity
pragma solidity ^0.8.0;

interface IFactClaims {
    event FactClaims(string ipfsURL);

    function claim(string memory ipfsURL) external {
        emit FactClaims(ipfsURL);
    }
}
```

- 3.6.2. This event asserts that the linked data is trusted by the account owner of the contract.

### 3.7. **Validate FactClaims Events**

- 3.7.1. Implement an Oracle / listener for the FactClaims event on the Ethereum blockchain.

- 3.7.2. Dereference the `ipfs://...` provided in the event to retrieve the published claims from the IPFS network.

- 3.7.3. Validate the claims against predefined criteria, ensuring their integrity and authenticity.

- 3.7.4. Only trust those claims that are transitively traceable via `prov:wasAttributedTo` property to the expected `ethereum://` address.

- 3.7.5. Act upon claims and additional assertions according to the use case.

### 3.8 Linked Data

- 3.8.1. **Serialization**: The fact graph is serialized in JSON-LD format, which extends JSON by adding support for Linked Data. JSON-LD allows for the representation of data in a graph-like structure composed of subject-predicate-object triples, following the RDF graph namedMap.

- 3.8.2. **Enriched XHTML**: To enhance human readability, the fact graph can be embedded within XHTML documents. This embedding allows for the seamless integration of structured data and semantic annotations within web pages, facilitating both human and machine interpretation.

#### For Example

```json-ld
{
  "@context": {
    "prov": "http://www.w3.org/ns/prov#"
  },
  "@graph": [
    {
      "@id": "ipfs://QmXqPj9sk...",
      "prov:wasAttributedTo": {
        "@id": "ethereum://0x123abc..."
      }
    },
    {
      "@id": "ethereum://0x123abc...",
      "prov:generated": {
        "@id": "ipfs://QmXqPj9sk..."
      },
      "prov:used": {
        "@id": "https://example.claims/facts/claim1"
      }
    },
    {
      "@id": "https://example.claims/facts/claim1",
      "prov:hadPrimarySource": {
        "@id": "ipfs://QmXqPj9sk..."
      }
    },
    {
      "@id": "https://example.claims/facts/claim1",
      "prov:wasDerivedFrom": {
        "@id": "ethereum://0x123abc..."
      }
    }
  ]
}
```

- 3.8.3 URL Dereferencing

At runtime, URLs referenced within the JSON-LD, especially those residing outside the IPFS subgraph, may be dereferenced at the agent's discretion. This dereferencing process involves retrieving the content associated with a URL from the web and incorporating it into the fact graph. It's crucial to ensure that data referenced on the public internet is permalinked, consistently and reliably accessible.

- 3.8.4 Mutable Internet-Sourced Facts

Unlike fact claims stored within the IPFS subgraph, Internet-sourced facts need not be immutable. Dynamic observations and updates from external sources may be ingested and inferred by the agent at its discretion. This approach enables the fact graph to incorporate real-time data and adapt to evolving information landscapes while maintaining the integrity of internally stored immutable fact claims.

- 3.8.5 Semantic Interoperability and Human Readability

The utilization of JSON-LD for fact graph representation ensures semantic interoperability with existing standards such as RDF, PROV-O, SKOS, and VOID annotations. Additionally, embedding JSON-LD within XHTML documents enhances human readability and accessibility, fostering both machine-readable interpretation and intuitive understanding by humans.

By leveraging JSON-LD serialization and embedded XHTML documents, the fact graph achieves a balance between machine readability, semantic interoperability, and human accessibility, laying the foundation for a robust and comprehensible knowledge representation framework.

---

## 4. Ontologies for Interoperability

This section introduces standard semantic ontologies used in the proposal, such as PROV-O, SKOS, and VOID. It outlines the role of these ontologies in enhancing interoperability and compatibility.

By adhering to these standard ontologies and vocabulary definitions, the fact graph ensures consistency, compatibility, and seamless integration.

The fact graph relies on standard semantic ontologies to ensure interoperability and compatibility across diverse ecosystems

### PROV-O (Provenance Ontology)

PROV-O furnishes a standardized vocabulary for representing provenance information, facilitating the documentation and tracking of entities, activities, and their relationships. By incorporating PROV-O, the fact graph can effectively capture the lineage and historical context of data, decisions, and actions within the ecosystem.

This vocabulary defines the only mandatory fact (`prov:wasAttributedTo`) needed to define a fact claim .

### SKOS (Simple Knowledge Organization System)

SKOS presents a standard framework for representing controlled vocabularies and organizing knowledge systems. By utilizing SKOS, the fact graph can establish and manage hierarchical and associative relationships among concepts, thereby enhancing semantic enrichment and knowledge organization within the ecosystem.

### VOID (Vocabulary of Interlinked Datasets)

VOID provides metadata specifications for describing RDF graphsets, including details concerning the dataset's structure, accessibility, and usage. Integration of VOID into the fact graph facilitates comprehensive documentation of dataset characteristics and attributes, thereby improving dataset discoverability, accessibility, and interoperability within the ecosystem.

### General Knowledge (OpenGraph, Schema.org, and Others)

Additionally, integration with Schema.org, OpenGraph, and similar standards significantly enhances search engine results (SERPs) and addresses the evolving landscape of "answer engines." Considering the importance of trust and relevance in the post-search era, further exploration of these standards is warranted.

---

## 5. Normative Features

This section outlines the validation process, including SHACL (Shapes Constraint Language) validation, and emphasizes conformance with VOID and PROV-O standards to maintain interoperability and data quality.

| Requirement                | Community Contributions                                                                                                                                                                       |
|----------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| IPFS Integration           | [js-ipfs](https://github.com/ipfs/js-ipfs) for JavaScript or [py-ipfs](https://github.com/ipfs/py-ipfs) for Python or [Java IPFS API](https://github.com/ipfs/java-ipfs-http-client) for Java |
| RDF Representation         | [Apache Jena](https://jena.apache.org/) for Java or [RDFLib](https://github.com/RDFLib/rdflib) for Python                                                                                     |
| Smart Contract Integration | [Web3.js](https://github.com/ethereum/web3.js/) for JavaScript or [Web3.py](https://github.com/ethereum/web3.py) for Python                                                                   |
| SHACL Validation           | [TopQuadrant SHACL](https://github.com/TopQuadrant/shacl) or [Eclipse RDF4J SHACL](https://rdf4j.org/documentation/programming/shacl/)                                                        |


### 5.1 JSON-LD and SHACL Validation

The validation uses SHACL to ensure compliance with the protocol's simplest semantics, your use case likely includes additional steps.

As a minimum, a fact graph should conform to the following SHACL

```turtle
trust:FactClaims
    a sh:NodeShape ;
    sh:targetClass prov:Entity ;
    sh:property [
        sh:path prov:generated ;
        sh:minCount 1 ;
        sh:nodeKind sh:IRI ;
    ] ;
    sh:property [
        sh:path prov:wasAttributedTo ;
        sh:minCount 1 ;
        sh:nodeKind sh:IRI ;
    ] .
```

### 5.2 Immutable Fact Claims

In fact claims ecosystems, establishing the immutability of data is crucial for building trust and reliability. The integration of IPFS (InterPlanetary File System) and RDF not only ensures immutability but also provides cryptographic proof of ownership.

5.2.1. **Unique Identification** IPFS assigns a unique cryptographic hash to each piece of content, including fact claims. This hash, derived from the content itself, serves as the content's address on the IPFS network, securely linking the content's identity to its actual data.

5.2.2. **Immutable Nature** Utilizing cryptographic hashing algorithms like SHA-256, IPFS ensures that any modification to the content results in a completely different hash. Thus, even the slightest alteration to the content produces a distinct address, rendering the original content immutable.

5.2.3. **Decentralized Distribution** IPFS content is distributed across multiple nodes. When a fact claim is added to IPFS, it gets replicated across numerous nodes, ensuring redundancy and resilience. This further strengthens the immutability of the content.

### 5.3 Cryptographic Ownership

5.3.1. **FactClaims Provenance Protocol** Upon publishing a fact claim to IPFS, a corresponding smart contract event (e.g., FactClaim(string ipfsURL)) is emitted. This event, integrated with the Fact Claims provenance protocol, establishes cryptographic proof of ownership by linking the fact claim to the DAO (Decentralized Autonomous Organization), smart contract, and/or private key holder.

5.3.2. **Smart Contract Integration** The smart contract event serves as an immutable record of the fact claim's publication, attributing ownership to the entity that initiated the event. This integration with smart contracts ensures transparency, traceability, and accountability in the generation and evolution of fact claims.

### 5.4 Semantic Representation

5.4.1. **Semantic Interoperability** RDF provides a standardized framework for representing data in a semantic format. Fact claims, along with their associated metadata and relationships, can be represented using RDF vocabularies such as PROV-O and VOID (Vocabulary of Interlinked Datasets). This semantic representation enhances interoperability and machine readability.

5.4.2. **Provenance and Attribution** RDF enables the representation of provenance information, including attribution, sources, and lineage of fact claims. By incorporating PROV-O, fact claims can include metadata describing their origins, transformations, and ownership. This provenance information enhances transparency and trust in the data.

### 5.5 Web of Trust

5.5.1. **Verification Mechanisms** IPFS enables verification mechanisms that allow users to verify the integrity of fact claims by recalculating their content hashes and comparing them with the expected addresses. This process ensures that the content remains unchanged and authentic, enhancing trust and reliability.

5.5.2. **Semantic Integrity** By crawling the fact graph, the trustworthiness and provenance of fact claims can be inspected.

---

## 6. Problem Domains

This section explores various use cases for fact claims, including regulatory compliance, financial auditing, supply chain management, healthcare data management, and more. It discusses the standards and technologies relevant to each use case.

- AI-powered answer engines require a new namedMap for finding facts.
- Trust is paramount in assessing the credibility of information sources.
- Fact claims in RDF format, enriched with metadata, serve as foundational elements.
- Verifiable assertions backed by cryptographic proofs and smart contracts ensure trust.
- Real-time algorithms verify dynamic and evolving fact claims.
- Semantic coherence and trust supersede traditional SEO practices.

| Problem Domain                        | Standards                                                                                                                                                                                                                                                                                                                                                                                                           |
|---------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Regulatory Compliance                 | - [XBRL (eXtensible Business Reporting Language)](https://www.xbrl.org/) - [FIBO (Financial Industry Business Ontology)](https://spec.edmcouncil.org/fibo/) - [FIGREGONT (Financial Industry Regulatory and Governance)](https://finregont.com/)                                                                                                                                                           |
| Financial Auditing                    | - [XBRL (eXtensible Business Reporting Language)](https://www.xbrl.org/) - [RDF](https://www.w3.org/RDF/) - [PROV-O](https://www.w3.org/TR/prov-o/)                                                                                                                                                                                                          |
| Supply Chain Management               | - [GS1 Standards](https://www.gs1.org/standards) - [RDF](https://www.w3.org/RDF/) - [PROV-O](https://www.w3.org/TR/prov-o/)                                                                                                                                                                                                                                  |
| Healthcare Data Management            | - [HL7 (Health Level Seven International)](https://www.hl7.org/) - [FHIR (Fast Healthcare Interoperability Resources)](https://www.hl7.org/fhir/) - [RDF](https://www.w3.org/RDF/)                                                                                                                                                                                                 |
| Intellectual Property       | - [W3C ODRL (Open Digital Rights Language)](https://www.w3.org/TR/odrl/) - [RDF](https://www.w3.org/RDF/)                                                                                                                                                                                                                                                                          |
| Research Collaboration                | - [PROV-O](https://www.w3.org/TR/prov-o/) - [SKOS (Simple Knowledge Organization System)](https://www.w3.org/2004/02/skos/) - [RDF](https://www.w3.org/RDF/)                                                                                                                                                                                                 |
| Innovation Tracking                   | - [W3C PROV-O](https://www.w3.org/TR/prov-o/) - [RDF](https://www.w3.org/RDF/)                                                                                                                                                                                                                                                                               |
| Environmental Sustainability          | - [OGC SOSA/SSN (Spatial Data on the Web Best Practices)](https://www.w3.org/TR/vocab-ssn/) - [RDF](https://www.w3.org/RDF/)                                                                                                                                                                                                                                                       |
| Legal Contracts and Agreements        | - [W3C ODRL (Open Digital Rights Language)](https://www.w3.org/TR/odrl/) - [RDF](https://www.w3.org/RDF/)                                                                                                                                                                                                                                                                          |
| Identity and Access Management        | - [W3C VC (Verifiable Credentials)](https://www.w3.org/TR/vc-data-namedMap/) - [DID (Decentralized Identifiers)](https://www.w3.org/TR/did-core/) - [RDF](https://www.w3.org/RDF/)                                                                                                                                                                                                    |
| Energy Trading and Grid Management    | - [IEC CIM (Common Information Model for Energy Markets)](https://www.iec.ch/cim/) - [RDF](https://www.w3.org/RDF/)                                                                                                                                                                                                                                                                |
| Education and Credential Verification | - [W3C VC (Verifiable Credentials)](https://www.w3.org/TR/vc-data-namedMap/) - [Open Badges (Open Badges Specification)](https://www.imsglobal.org/sites/default/files/Badges/OBv2p0/index.html) - [RDF](https://www.w3.org/RDF/)                                                                                                                                                     |
| Asset Tokenization and Management     | - [ERC-20 (Ethereum Request for Comments 20)](https://eips.ethereum.org/EIPS/eip-20) - [ERC-721 (Ethereum Request for Comments 721)](https://eips.ethereum.org/EIPS/eip-721) - [RDF](https://www.w3.org/RDF/)                                                                                                                                                                      |
| News and Data-driven Narratives       | - [NewsML-G2 (News Markup Language - Generation 2)](https://iptc.org/standards/newsml-g2/) - [NITF (News Industry Text Format)](https://iptc.org/standards/nitf/) - [Linked Data Platform (LDP)](https://www.w3.org/TR/ldp/) - [Semantic Web Standards (RDF, RDFa, JSON-LD)](https://www.w3.org/RDF/) - [Schema.org](https://schema.org/) - [W3C Data Catalog Vocabulary (DCAT)](https://www.w3.org/TR/vocab-dcat/) |

### 6.1 Regulatory Compliance

Adhering to laws, regulations, and industry standards is crucial for transparency and accountability in regulatory compliance. Utilizing standards such as [XBRL](https://www.xbrl.org/), [FIBO](https://spec.edmcouncil.org/fibo/) and [FIGREGONT](https://finregont.com/) ensures the traceability and integrity of financial data and regulatory reporting.

### 6.2 Financial Auditing

Ensuring the accuracy and compliance of financial records requires robust standards like [XBRL](https://www.xbrl.org/), [RDF](https://www.w3.org/RDF/), and [PROV-O](https://www.w3.org/TR/prov-o/). These standards facilitate the representation and provenance tracking of financial data, supporting audit trails and regulatory compliance.

### 6.3 Supply Chain Management

Documenting supply chain processes and ensuring transparency is essential for effective supply chain management. Standards such as [GS1](https://www.gs1.org/standards), [RDF](https://www.w3.org/RDF/), and [PROV-O](https://www.w3.org/TR/prov-o/) enable the tracking of product provenance and support efficient supply chain operations.

### 6.4 Healthcare Data Management

Securely managing and analyzing patient health information relies on interoperable standards like [HL7](https://www.hl7.org/), [FHIR](https://www.hl7.org/fhir/), and [RDF](https://www.w3.org/RDF/). These standards facilitate semantic representation and provenance tracking of healthcare data, enhancing patient care and research.

### 6.5 Intellectual Property Management

Protecting and monetizing intellectual property assets requires standards like [W3C ODRL](https://www.w3.org/TR/odrl/) and [RDF](https://www.w3.org/RDF/). These standards support the representation, licensing, and provenance tracking of patents, trademarks, and copyrights.

### 6.6 Research Collaboration

Collaborating to advance scientific knowledge relies on standards such as [PROV-O](https://www.w3.org/TR/prov-o/), [SKOS](https://www.w3.org/2004/02/skos/), and [RDF](https://www.w3.org/RDF/). These standards enable the documentation, sharing, and attribution of research data and findings among researchers and institutions.

### 6.7 Innovation Tracking

Monitoring trends and advancements in various fields is essential for innovation tracking. Standards like [W3C PROV-O](https://www.w3.org/TR/prov-o/) and [RDF](https://www.w3.org/RDF/) facilitate the capture, analysis, and visualization of innovation-related data and its provenance.

### 6.8 Environmental Sustainability

Promoting conservation efforts and informed decision-making in environmental sustainability relies on standards like [OGC SOSA/SSN](https://www.w3.org/TR/vocab-ssn/) and [RDF](https://www.w3.org/RDF/). These standards support the collection, integration, and analysis of environmental data.

### 6.9 Legal Contracts and Agreements

Enforcing legal terms and conditions in contracts and agreements is facilitated by standards like [W3C ODRL](https://www.w3.org/TR/odrl/) and [RDF](https://www.w3.org/RDF/). These standards enable the representation, interpretation, and enforcement of legal terms and conditions.

### 6.10 Identity and Access Management

Managing digital identities and controlling access to resources requires standards like [W3C VC](https://www.w3.org/TR/vc-data-namedMap/), [DID](https://www.w3.org/TR/did-core/), and [RDF](https://www.w3.org/RDF/). These standards enable verifiable credentials, decentralized identifiers, and semantic representation of identity data.

### 6.11 Energy Trading and Grid Management

Optimizing energy production and distribution relies on standards like [IEC CIM](https://www.iec.ch/cim/) and [RDF](https://www.w3.org/RDF/). These standards support interoperability, data exchange, and modeling of energy systems for efficient grid management.

### 6.12 Education and Credential Verification

Validating academic achievements and qualifications depends on standards like [W3C VC](https://www.w3.org/TR/vc-data-namedMap/), [Open Badges](https://www.imsglobal.org/sites/default/files/Badges/OBv2p0/index.html), and [RDF](https://www.w3.org/RDF/). These standards enable the issuance, verification, and exchange of digital credentials and badges.

### 6.13 Asset Tokenization and Management

Representing real-world assets as digital tokens requires standards like [ERC-20](https://eips.ethereum.org/EIPS/eip-20), [ERC-721](https://eips.ethereum.org/EIPS/eip-721), and [RDF](https://www.w3.org/RDF/). These standards facilitate the creation, transfer, and management of tokenized assets while ensuring transparency and interoperability.

### 6.14 News and Data-driven Narratives

Creating, disseminating, and analyzing news content and data stories is supported by standards like [NewsML-G2](https://iptc.org/standards/newsml-g2/) and [NITF](https://iptc.org/standards/nitf/).

---

## 7. Integrity, Security and Privacy

This section addresses key concerns, focusing on best practices for mitigating risks and safeguarding trust.

### 7.1 Compliance with Semantic Standards

Fact graphs should conform to standard semantic ontologies like VOID and PROV-O. 

Adhering to these standards ensures semantic interoperability, facilitating seamless integration and exchange of fact claims across diverse platforms.

### 7.2 Immutable Fact Claims

Ensuring the immutability of data is crucial for establishing trust. Integration of IPFS and RDF not only ensures immutability but also provides cryptographic proof of ownership provenance protocol.

- **Unique Identification**: IPFS assigns a unique cryptographic hash to each content piece, ensuring its identity and integrity.
- **Immutable Nature**: Modifications to content result in distinct hashes, preserving original content integrity.
- **Decentralized Distribution**: Content is replicated across IPFS nodes, ensuring redundancy and resilience.

### 7.3. Cryptographic Ownership

- **FactClaims Provenance Protocol**: Emitting a smart contract event links fact claims to DAO, smart contract, or private key holder, establishing cryptographic ownership.
- **Smart Contract Integration**: Smart contract events serve as immutable records, ensuring transparency and accountability.

### 7.4. Semantic Interactivity

- **Semantic Interoperability**: RDF enables semantic representation of fact claims, enhancing interoperability.
- **Provenance and Attribution**: Incorporating PROV-O allows metadata representation, enhancing transparency and trust.

### 7.5. Verification and Trust

- **Verification Mechanisms**: IPFS enables content integrity verification, enhancing trust and reliability.
- **Semantic Integrity**: Combining IPFS immutability with RDF semantic representation fosters greater trust in fact claims' accuracy and context.

---

## 8. Acknowledgments and References

We acknowledge contributions from many individuals or organizations who shared expertise, feedback, review, or other assistance during the development of the proposal.

- [XBRL (eXtensible Business Reporting Language)](https://www.xbrl.org/)
- [NewsML-G2 (News Markup Language - Generation 2)](https://iptc.org/standards/newsml-g2/)
- [NITF (News Industry Text Format)](https://iptc.org/standards/nitf/)
- [Linked Data Platform (LDP)](https://www.w3.org/TR/ldp/)
- [PROV-O](https://www.w3.org/TR/prov-o/)
- [SKOS (Simple Knowledge Organization System)](https://www.w3.org/2004/02/skos/)
- [RDF](https://www.w3.org/RDF/)
- [RDFa](https://www.w3.org/TR/xhtml-rdfa/)
- [JSON-LD](https://json-ld.org/)
- [Schema.org](https://schema.org/)
- [W3C Data Catalog Vocabulary (DCAT)](https://www.w3.org/TR/vocab-dcat/)
- [GS1 Standards](https://www.gs1.org/standards)
- [HL7 (Health Level Seven International)](https://www.hl7.org/)
- [FHIR (Fast Healthcare Interoperability Resources)](https://www.hl7.org/fhir/)
- [W3C ODRL (Open Digital Rights Language)](https://www.w3.org/TR/odrl/)
- [OGC SOSA/SSN (Spatial Data on the Web Best Practices)](https://www.w3.org/TR/vocab-ssn/)
- [IEC CIM (Common Information Model for Energy Markets)](https://www.iec.ch/cim/)
- [W3C VC (Verifiable Credentials)](https://www.w3.org/TR/vc-data-namedMap/)
- [DID (Decentralized Identifiers)](https://www.w3.org/TR/did-core/)
- [ERC-20 (Ethereum Request for Comments 20)](https://eips.ethereum.org/EIPS/eip-20)
- [ERC-721 (Ethereum Request for Comments 721)](https://eips.ethereum.org/EIPS/eip-721)


| GitHub Repositories                                                        |
|----------------------------------------------------------------------------|
| [RDF GitHub Repository](https://github.com/w3c/rdf)                        |
| [PROV-O GitHub Repository](https://github.com/w3c/prov-o)                  |
| [IPFS GitHub Repository](https://github.com/ipfs)                          |
| [Smart Contracts GitHub Repository](https://github.com/ethereum/solidity)  |

---

## 9. Changes from Previous Versions

As applicable, this section summarizes the changes made in the current version compared to earlier versions of the proposal.

#### Draft 0.4 (Current Version):
- Introduced interface `IFactClaims` to mandate the `FactClaim` event in the contract, ensuring adherence to a standardized event structure.
- Added specification to ensure the `FactClaim` event is emitted from the `claim` function, enhancing clarity and consistency in contract functionality.

#### Draft 0.3:
- Added JSON-LD and RDF specifications and examples to facilitate interoperability and data exchange, enhancing the proposal's compatibility with linked data standards.
- Draft sections for various components and functionalities, outlining the structure and scope of the proposal.

#### Draft 0.2:
- Created the initial version of the `FactClaims` contract, laying the foundation for the proposed Ethereum smart contract.
- Implemented the `claim` function to emit the `FactClaim` event with the provided `ipfsURL`, enabling the publication of fact claims with corresponding IPFS URLs.

#### Draft 0.1:
- Initial draft outlining the concept and structure of the FactClaims proposal, providing an overview of the intended protocol for publishing fact claims.
- Introduced the proposed protocol for publishing fact claims, setting the groundwork for subsequent iterations and developments.