package systems.symbol.gql;

import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.language.*;
import graphql.schema.FieldCoordinates;
import graphql.schema.GraphQLCodeRegistry;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import org.eclipse.rdf4j.repository.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static graphql.schema.idl.RuntimeWiring.newRuntimeWiring;

/*
 *  symbol.systems2
 *  Copyright (c) 2009-2015, 2021-2023 Symbol Systems, All Rights Reserved.
 *  Licence: https://symbol.systems/about/license
 */
public class GQL {
	private static final Logger log = LoggerFactory.getLogger(GQL.class);
	static SchemaParser schemaParser = new SchemaParser();
	TypeDefinitionRegistry typeDefinitionRegistry = new TypeDefinitionRegistry();

	public GQL() {
	}

	public TypeDefinitionRegistry parse(String schema) {
		TypeDefinitionRegistry schemaDef = schemaParser.parse(schema);
		typeDefinitionRegistry.merge(schemaDef);
		return schemaDef;
	}

	protected GraphQL newGraphQL(Repository repository) {
		return newGraphQL(typeDefinitionRegistry, wiring(), repository);
	}

	protected GraphQL newGraphQL(TypeDefinitionRegistry schemaDef, RuntimeWiring.Builder wiringBuilder, Repository repository) {
		GraphQLSchema graphQLSchema = newGraphQLSchema(schemaDef, wiringBuilder, repository);
		return GraphQL.newGraphQL(graphQLSchema).build();
	}

	protected GraphQLSchema newGraphQLSchema(RuntimeWiring.Builder wiringBuilder, Repository repository) {
		return newGraphQLSchema(typeDefinitionRegistry, wiringBuilder, repository);
	}

	protected GraphQLSchema newGraphQLSchema(TypeDefinitionRegistry schemaDef, RuntimeWiring.Builder wiringBuilder, Repository repository) {

		// wire-up the data fetchers
		GraphQLCodeRegistry.Builder codeRegistry = wiring(schemaDef, repository);
		wiringBuilder.codeRegistry(codeRegistry);

		// bind schema and wiring
		SchemaGenerator schemaGenerator = new SchemaGenerator();
		RuntimeWiring wiring = wiringBuilder.build();
		return schemaGenerator.makeExecutableSchema(schemaDef, wiring);
	}

	protected GraphQLCodeRegistry.Builder wiring(TypeDefinitionRegistry schemaDef, Repository repository) {
		log.info("GQL.schemaDef.types: "+schemaDef.types());
		GraphQLCodeRegistry.Builder codeRegistry = GraphQLCodeRegistry.newCodeRegistry();
		String rootType = "Query";
		Optional<TypeDefinition> queryDef = schemaDef.getType(rootType);
		log.info("GQL.queryDef: "+queryDef);
		queryDef.get().getChildren().forEach( f -> {
			FieldDefinition field = (FieldDefinition)f;
			ObjectTypeDefinition schemaDefType = (ObjectTypeDefinition)schemaDef.getType(field.getType()).orElse(null);
			if (schemaDefType!=null) {
				String iri = toIRI(schemaDefType);
				SPARQLDataFetcher innerFetcher = new SPARQLDataFetcher(schemaDefType, repository);
// try to load a per-type policy template from the repository
PolicyEngine engine = loadPolicyEngineForType(repository, GQL.toIRI(schemaDefType));
if (engine==null) engine = new AskPolicyEngine(repository, null);
								AuthorizationDataFetcher dataFetcher = new AuthorizationDataFetcher(repository, GQL.toIRI(schemaDefType), engine, innerFetcher);
				log.info("GQL.queryDef.f: "+iri+" -> "+field.getName()+" @ "+schemaDefType);
				codeRegistry.dataFetcher( FieldCoordinates.coordinates(rootType, field.getName()), dataFetcher);
			}
		});
		return codeRegistry;
	}

	RuntimeWiring.Builder wiring() {
		RuntimeWiring.Builder runtimeWiring = newRuntimeWiring();
		runtimeWiring.directive("rdf", new IRIDirective());
		return runtimeWiring;
	}

	public static String toIRI(DirectivesContainer def) {
		Collection<Directive> directives = def.getDirectives("rdf");
		AtomicReference<String> found = new AtomicReference();
		directives.forEach( d-> {
			if (d.getName().equals("rdf")) {
				d.getArguments().forEach( a -> {
					if ( a.getName().equals("iri") )
						found.set( ((StringValue)a.getValue()).getValue());
				});
			}
		});
		return found.get();
	}

/**
 * Convenience helper for tests: parse schema and produce an executable GraphQLSchema wired to the repository.
 */
public GraphQLSchema makeExecutableSchema(String schema, Repository repository) {
TypeDefinitionRegistry registry = parse(schema);
return newGraphQLSchema(registry, wiring(), repository);
}
protected PolicyEngine loadPolicyEngineForType(Repository repository, String typeIRI) {
try (var conn = repository.getConnection()) {
String sparql = "SELECT ?template ?allow WHERE { ?p <http://symbol.systems/v0/onto/trust#forType> <"+typeIRI+"> . OPTIONAL { ?p <http://symbol.systems/v0/onto/trust#askTemplate> ?template } OPTIONAL { ?p <http://symbol.systems/v0/onto/trust#defaultAllow> ?allow } } LIMIT 1";
var q = conn.prepareTupleQuery(org.eclipse.rdf4j.query.QueryLanguage.SPARQL, sparql);
try (var res = q.evaluate()) {
if (res.hasNext()) {
var binding = res.next();
String template = binding.hasBinding("template")? binding.getValue("template").stringValue() : null;
boolean allow = false;
if (binding.hasBinding("allow")) {
String v = binding.getValue("allow").stringValue();
allow = "true".equalsIgnoreCase(v) || "1".equals(v);
}
return new AskPolicyEngine(repository, template, allow);
}
}
} catch (Exception e) {
log.warn("Error loading policy for {}: {}", typeIRI, e.getMessage());
}
return null;
}

public ExecutionResult execute(Repository repository, GraphQL graphQL, String query, Map map) {
log.info("GQL.execute.query: " + query);
log.info("GQL.execute.vars: " + map);

ExecutionInput.Builder builder = ExecutionInput.newExecutionInput();
//builder.context(repository);
builder.query(query);
builder.variables(map);
ExecutionInput executionInput = builder.build();
return graphQL.execute(executionInput);

}
}
