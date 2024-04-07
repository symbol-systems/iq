
For example:
------------

$ npm install && npm start
$ curl -v http://localhost:7001/types

This /types/ API is mapped to the rdfs_types.sparql file in the ./sparql/ folder.

Governance Graph
----------------

The systems.symbol.ttl file contains a ss:Service graph describing the OpenAPI and it's runtime components.


:api-service-sparql
a   ss:Service;
a   k8s:DockerImage;
a   k8s:HelmChart;

dc:title"Micro APIs: Core Insights";
k8s:name"api-service-sparql";

ss:feature <https://api.symbol.systems/v1/gg/sparql/:query>;
ss:feature <https://api.symbol.systems/v1/gg/healthz/sparql>;
ss:feature <https://api.symbol.systems/v1/gg/openapi/sparql>;
.

Docker Demo
-----------


docker run --rm -p 8080:8080 yyz1989/rdf4j



curl -H "Content-Type: application/json" -X POST --data '{"this":"https://api.symbol.systems/v1/gg/openapi/sparql/"}' http://localhost:7001/gg/this
  
  