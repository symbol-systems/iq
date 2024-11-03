package systems.symbol.gql;

import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.GraphQLError;
import graphql.Scalars;
import graphql.language.Argument;
import graphql.language.FieldDefinition;
import graphql.language.ObjectTypeDefinition;
import graphql.language.TypeDefinition;
import graphql.schema.*;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import org.dataloader.BatchLoaderEnvironment;
import org.dataloader.BatchLoaderWithContext;
import org.dataloader.DataLoader;
import org.dataloader.DataLoaderOptions;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.repository.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;
import static graphql.schema.idl.RuntimeWiring.newRuntimeWiring;

/*
 *  symbol.systems
 *  Copyright (c) 2009-2015, 2021-2023 Symbol Systems, All Rights Reserved.
 *  Licence: https://symbol.systems/about/license
 */
public class RDFS2GQL {
	private static final Logger log = LoggerFactory.getLogger(GQL.class);
	static SchemaParser schemaParser = new SchemaParser();
	TypeDefinitionRegistry typeDefinitionRegistry = new TypeDefinitionRegistry();

	static Map<IRI, GraphQLOutputType> scalarTypes  = new HashMap();

	static {
		scalarTypes.put(XSD.STRING, Scalars.GraphQLString);
		scalarTypes.put(XSD.INT, Scalars.GraphQLInt);
		scalarTypes.put(XSD.INTEGER, Scalars.GraphQLInt);
		scalarTypes.put(XSD.FLOAT, Scalars.GraphQLFloat);
		scalarTypes.put(XSD.DOUBLE, Scalars.GraphQLFloat);
		scalarTypes.put(XSD.DECIMAL, Scalars.GraphQLFloat);
	}

	private List<Argument> arguments;

	public RDFS2GQL() {
//		DirectiveDefinition rdf = new DirectiveDefinition("rdf");
//		typeDefinitionRegistry.add(rdf);
	}

	public TypeDefinitionRegistry parse(String schema) {
		TypeDefinitionRegistry schemaDef = schemaParser.parse(schema);
		typeDefinitionRegistry.merge(schemaDef);
		return schemaDef;
	}

	protected GraphQL newGraphQL(Repository repository) {
		return newGraphQL(typeDefinitionRegistry, wiring(), repository);
	}

	protected GraphQL newGraphQL(TypeDefinitionRegistry schemaDef, RuntimeWiring.Builder runtimeWiring, Repository repository) {
		System.out.println("schemaDef.types: "+schemaDef.types());
		SchemaGenerator schemaGenerator = new SchemaGenerator();
		GraphQLCodeRegistry.Builder codeRegistry = GraphQLCodeRegistry.newCodeRegistry();
		String rootType = "Query";
		Optional<TypeDefinition> queryDef = schemaDef.getType(rootType);
		System.out.println("queryDef: "+queryDef);
		queryDef.get().getChildren().forEach( f -> {
			FieldDefinition field = (FieldDefinition)f;
			ObjectTypeDefinition schemaDefType = (ObjectTypeDefinition)schemaDef.getType(field.getType()).orElse(null);
			if (schemaDefType!=null) {
				String iri = GQL.toIRI(schemaDefType);
				SPARQLDataFetcher dataFetcher = new SPARQLDataFetcher(schemaDefType, repository);
				System.out.println("queryDef.f: "+iri+" -> "+field.getName()+" @ "+schemaDefType);
				codeRegistry.dataFetcher( FieldCoordinates.coordinates(rootType, field.getName()), dataFetcher);
			}
		});

		// wire-up the data fetchers
		runtimeWiring.codeRegistry(codeRegistry);
		RuntimeWiring wiring = runtimeWiring.build();
		GraphQLSchema graphQLSchema = schemaGenerator.makeExecutableSchema(schemaDef, wiring);
		return GraphQL.newGraphQL(graphQLSchema).build();
	}


	RuntimeWiring.Builder wiring() {
		RuntimeWiring.Builder runtimeWiring = newRuntimeWiring();
		runtimeWiring.directive("rdf", new IRIDirective());
		return runtimeWiring;
	}

	public DataFetcher fetch(String typeName, String fieldName) {
		System.out.println("fetch: "+typeName+"->"+fieldName);
		return (DataFetcher<Object>) dataFetchingEnvironment -> {
			System.out.println("dataFetchingEnvironment: "+dataFetchingEnvironment);
			return new Date();
		};
	}

	protected TypeDefinitionRegistry define(RDFSDomain domain) {
		TypeDefinitionRegistry registry = new TypeDefinitionRegistry();
		Map<IRI, GraphQLObjectType> objectMap = new HashMap<>();
		Map<IRI, RDFSResource> missing = new HashMap<>();

		domain.classes.forEach((c_iri, rdfsClass) -> {
			String class_name = c_iri.getLocalName();
			System.out.println("class_name: "+class_name);
			GraphQLObjectType.Builder typeDef = newObject().name(class_name);
			if (rdfsClass.description != null)
				typeDef.description(rdfsClass.description);
			Collection<GraphQLFieldDefinition> fields = new ArrayList<>();
			GraphQLOutputType graphQLID = Scalars.GraphQLID;
//			fields.add(Scalars.GraphQLID.getDefinition());

			defineLiterals(typeDef, rdfsClass);
//			defineObjects(typeDef, objectMap, missing, rdfsClass);

			GraphQLObjectType graphQLObjectType = typeDef.build();
			System.out.println("objectType: "+graphQLObjectType);
			objectMap.put(c_iri, graphQLObjectType);
			registry.add(graphQLObjectType.getDefinition());
		});
		return registry;
	}

	private void defineObjects(GraphQLObjectType.Builder typeDef, Map<IRI, GraphQLObjectType> objectMap, Map<IRI, RDFSResource> missing, RDFSClass rdfsClass) {
		rdfsClass.objects.forEach((o_iri, object) -> {
			GraphQLFieldDefinition.Builder fieldDef = newFieldDefinition();
			fieldDef.name(o_iri.getLocalName());
			GraphQLObjectType graphQLObjectType = objectMap.get(object.getIdentity());
			if (graphQLObjectType != null) {
				fieldDef.type(graphQLObjectType);
				if (object.description != null)
					fieldDef.description(object.description);
				typeDef.field(fieldDef);
			} else {
				missing.put(object.getIdentity(), object);
			}
		});
	}

	private Collection<GraphQLFieldDefinition> defineLiterals(GraphQLObjectType.Builder typeDef, RDFSClass rdfsClass) {
		Collection<GraphQLFieldDefinition> literals = new ArrayList<>();
		rdfsClass.literals.forEach((s_iri, scalar) -> {
			GraphQLFieldDefinition.Builder fieldDef = newFieldDefinition();
			GraphQLObjectType scalarType = null;
//			if (scalar.range!=null) {
//				scalarType = scalarTypes.get(scalar.range);
//				if (scalarType == null)
//					scalarType = newObject().name(scalar.range.getLocalName()).build();
//			}
//			if (scalarType==null) scalarType = Scalars.GraphQLString;
//			fieldDef.type(scalarType);
//			fieldDef.name(s_iri.getLocalName());
//			if (scalar.description != null)
//				fieldDef.description(scalar.description);
//			literals.add(fieldDef);
		});
		return literals;
	}

	void dump(ExecutionResult executionResult) {
		Map<String, Object> model = executionResult.toSpecification();
		System.out.println("dump.model: "+model);
		Object data = executionResult.getData();
		System.out.println("dump.data: "+data);
		List<GraphQLError> errors = executionResult.getErrors();
		System.out.println("dump.errors: "+errors);
	}

	public DataLoader<IRI, Map> load(IRI root) {
		DataLoaderOptions options = DataLoaderOptions.newOptions()
				.setBatchLoaderContextProvider(() -> {
					return null;
				});
		System.out.println("load.root: "+root);

		BatchLoaderWithContext<IRI, Map> batchLoader = new BatchLoaderWithContext<>() {
			@Override
			public CompletionStage<List<Map>> load(List<IRI> predicates, BatchLoaderEnvironment env) {
				System.out.println("load.predicates: "+predicates);

				return CompletableFuture.supplyAsync(() -> {
					List<Map> items = new ArrayList<>();
					System.out.println("load.items: "+items);
					return items;
				});
			}
		};
		DataLoader<IRI, Map> loader = DataLoader.newDataLoader(batchLoader);
		return loader;
	}

}
