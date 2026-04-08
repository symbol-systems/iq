# IQ Platform User Stories - SKOS TTL Files

This directory contains SKOS-based RDF/TTL representations of all 75 user stories for the IQ platform, organized by user role.

## Overview

The user stories are expressed using SKOS (Simple Knowledge Organization System), enabling:
- **Semantic organization** of stories by role, theme, priority, and phase
- **Queryability** via SPARQL to find stories by various criteria
- **Machine readability** for automated processing and reporting
- **Traceability** linking stories to RDF patterns and implementation guidance

## File Structure

### Master Index
- **`ai-user-stories-index.ttl`** - Main index with all roles, themes, priorities, and phases

### Role-Specific Files
- **`ai-architect-stories.ttl`** - 12 stories for Enterprise AI Architect role
- **`ai-domain-expert-stories.ttl`** - 14 stories for Domain Expert / Business Rules Engineer
- **`ai-agent-developer-stories.ttl`** - 16 stories for Agent Developer / Workflow Engineer
- **`ai-integration-engineer-stories.ttl`** - 12 stories for Integration Engineer
- **`ai-platform-ops-stories.ttl`** - 12 stories for Platform Operations
- **`ai-compliance-officer-stories.ttl`** - 9 stories for Data Analyst / Compliance Officer

## Namespace

All stories use the `ai:` namespace:
```
@prefix ai: <https://symbol.systems/v0/ai#>
```

## SKOS Structure

### Top-Level Concepts
Each file defines a role as a SKOS Concept with narrower stories:

```turtle
ai:RoleEnterpriseArchitect a skos:Concept ;
skos:prefLabel "Enterprise AI Architect" ;
skos:definition "..." ;
skos:narrower ai:Story001-MultiTenantRealm , ai:Story002-... .
```

### Individual Stories
Each story includes:
- **skos:prefLabel** - Short story name
- **skos:definition** - User story goal statement
- **skos:scopeNote** - Acceptance criteria and details
- **dct:identifier** - Story sequence number
- **ai:storyType** - "User Story" classification
- **ai:theme** - Business/technical theme (e.g., "multi-tenancy", "compliance")
- **ai:priority** - "critical", "high", "medium", or "low"
- **ai:rdfPattern** - Example RDF triple pattern showing manifestation

### Example Story
```turtle
ai:Story005-ComplianceAutomation a skos:Concept ;
skos:prefLabel "Compliance Rule Automation via SPARQL" ;
skos:definition "Encode compliance rules (SOX, HIPAA, PCI-DSS) as SPARQL rules validating every agent decision and data access" ;
skos:scopeNote "Rules in SPARQL or SHACL shapes; pre-execution validation prevents non-compliant actions; violations trigger alerts/logs; rule audit trail shown; business team updates without engineering" ;
dct:identifier "005" ;
ai:storyType "User Story" ;
ai:theme "compliance" ;
ai:priority "critical" ;
ai:rdfPattern "rule:ComplianceRule rdf:type iq:ComplianceRule ; iq:validates agent:Agent ." .
```

## SPARQL Examples

### Find all critical-priority stories
```sparql
PREFIX ai: <https://symbol.systems/v0/ai#>
PREFIX skos: <http://www.w3.org/2004/02/skos/core#>

SELECT DISTINCT ?story ?title
WHERE {
  ?story a skos:Concept ;
skos:prefLabel ?title ;
ai:priority "critical" .
}
ORDER BY ?story
```

### Find all compliance-themed stories
```sparql
PREFIX ai: <https://symbol.systems/v0/ai#>
PREFIX skos: <http://www.w3.org/2004/02/skos/core#>

SELECT DISTINCT ?story ?title ?role
WHERE {
  ?role a skos:Concept ;
skos:narrower ?story .
  ?story skos:prefLabel ?title ;
ai:theme "compliance" .
}
ORDER BY ?story
```

### Find stories for a specific role
```sparql
PREFIX ai: <https://symbol.systems/v0/ai#>
PREFIX skos: <http://www.w3.org/2004/02/skos/core#>

SELECT DISTINCT ?story ?title ?priority
WHERE {
  ai:RoleEnterpriseArchitect skos:narrower ?story .
  ?story skos:prefLabel ?title ;
ai:priority ?priority .
}
ORDER BY ?story
```

### Find stories by phase
```sparql
PREFIX ai: <https://symbol.systems/v0/ai#>
PREFIX skos: <http://www.w3.org/2004/02/skos/core#>

SELECT DISTINCT ?phase ?story ?title
WHERE {
  ?phase a skos:Concept ;
skos:narrower ?story .
  ?story skos:prefLabel ?title .
}
ORDER BY ?phase ?story
```

## Story Themes

- **RDF-Driven Behavior** (28 stories) - Business logic configured in RDF/TTL
- **Compliance & Governance** (19 stories) - Regulatory compliance, audit trails, policy enforcement
- **Multi-Tenancy & Scale** (7 stories) - Multi-tenant isolation, SaaS-grade scaling
- **Integration & Data Unification** (13 stories) - Connectors, semantic mappings, federated queries
- **Workflows & Orchestration** (11 stories) - Agent workflows, coordination, state management
- **Security & Access Control** (9 stories) - Authentication, authorization, threat detection
- **Performance & Scalability** (6 stories) - Caching, indexing, resource optimization
- **Governance & Operations** (12 stories) - Deployment, monitoring, high availability

## Story Priorities

- **Critical** (15 stories) - Enterprise deployment requirement, immediate risk
- **High** (28 stories) - Core platform features
- **Medium** (22 stories) - Important features enhancing capability
- **Low** (10 stories) - Nice-to-have advanced scenarios

## Implementation Phases

- **Phase 1: Core Platform** (5 stories) - Foundation
- **Phase 2: Enterprise Features** (6 stories) - Enterprise-scale
- **Phase 3: Advanced Patterns** (5 stories) - Advanced workflows
- **Phase 4: Compliance & Analytics** (5 stories) - Compliance/reporting

## Usage

### Load into RDF4J Repository
```bash
# Using RDF4J workbench or SPARQL endpoint
curl -X POST http://localhost:8080/rdf4j-server/repositories/iq-stories \
  -F "file=@ai-user-stories-index.ttl" \
  -F "file=@ai-architect-stories.ttl" \
  -F "file=@ai-domain-expert-stories.ttl" \
  # ... etc for all files
```

### Query with SPARQL
All files use standard SKOS and Dublin Core, compatible with any RDF store:
```sparql
PREFIX ai: <https://symbol.systems/v0/ai#>
PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
PREFIX dct: <http://purl.org/dc/terms/>

SELECT DISTINCT ?role ?story ?title ?priority ?theme
WHERE {
  ?role a skos:Concept ;
skos:narrower ?story .
  ?story dct:identifier ?id ;
skos:prefLabel ?title ;
ai:priority ?priority ;
ai:theme ?theme .
}
ORDER BY ?role ?id
```

## Relationships to Other Documentation

- **USER_STORIES.md** - Markdown version with expanded narrative descriptions
- **IQ Platform Architecture** - Technical details of RDF/SPARQL integration
- **IQ Connector Framework** - Integration patterns and examples

## Extending

To add new stories:

1. Identify the role (or create a new role concept)
2. Create story concept with appropriate properties
3. Add story to role's `skos:narrower` list in role file
4. Update index file if adding new themes/phases
5. Ensure theme/priority values are consistent with taxonomy

Example:
```turtle
ai:Story999-NewFeature a skos:Concept ;
skos:prefLabel "New Feature Name" ;
skos:definition "..." ;
skos:scopeNote "..." ;
dct:identifier "999" ;
ai:storyType "User Story" ;
ai:theme "existing-or-new-theme" ;
ai:priority "high" ;
ai:rdfPattern "..." .
```

## Standards Compliance

- **SKOS** (https://www.w3.org/TR/skos-reference/) - Concept schemes and vocabulary management
- **Dublin Core** (https://www.dublincore.org/) - Metadata (creator, date, identifier, etc.)
- **RDF 1.1** (https://www.w3.org/TR/rdf11-concepts/) - Semantic web foundations
- **FOAF** (http://xmlns.com/foaf/0.1/) - Social ontology (when referencing people)

## Maintenance

- **Version:** 1.0
- **Last Updated:** April 7, 2026
- **Maintainer:** IQ Product Management
- **Review Cycle:** Quarterly with product/engineering planning

---

**For more details, see [/iq-docs/docs/USER_STORIES.md](/iq-docs/docs/USER_STORIES.md)**
