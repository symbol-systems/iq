package systems.symbol.gql;

import systems.symbol.rdf4j.sparql.QueryHelper;
import graphql.language.*;
import graphql.schema.*;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.parser.sparql.SPARQLQueries;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class SPARQLDataFetcher implements DataFetcher<Collection<Map<String, Object>>> {
private static final Logger log = LoggerFactory.getLogger(SPARQLDataFetcher.class);
ObjectTypeDefinition schemaDefType;
Map<String, FieldDefinition> fieldDefinitionMap = new HashMap<>();
FieldDefinition idField;
Repository repository;

public SPARQLDataFetcher(ObjectTypeDefinition schemaDefType, Repository repository) {
this.schemaDefType=schemaDefType;
this.repository=repository;
mapFields(schemaDefType.getFieldDefinitions());
log.info("sparql.schemaDefType: "+ idField+" @ "+schemaDefType);
}

private void mapFields(List<FieldDefinition> fields) {
fields.forEach( field -> {
fieldDefinitionMap.put(field.getName(), field);
Type type = field.getType();
if (type instanceof NonNullType ) {
type = ((NonNullType)type).getType();
}
if (type instanceof TypeName && ((TypeName) type).getName().equals("ID")) {
TypeName typeName = (TypeName)type;
idField = field;
log.info("sparql.idField: "+ field+" @ "+ typeName);
} else {
log.info("sparql.field: "+ field+" @ "+type.getClass());
}
});
}

@Override
public Collection<Map<String, Object>> get(DataFetchingEnvironment environment) throws Exception {
StringBuilder sparql = toSELECT(environment);

RepositoryConnection connection = repository.getConnection();

// prepare SPARQL
String prefixClauses = SPARQLQueries.getPrefixClauses(connection.getNamespaces());
log.info("sparql.select.query: " + prefixClauses + sparql);
TupleQuery tupleQuery = connection.prepareTupleQuery(prefixClauses + sparql);
QueryHelper.setBindings(connection.getValueFactory(), tupleQuery, environment.getArguments());

// evaluate SPARQL
TupleQueryResult queryResult = tupleQuery.evaluate();
Collection<Map<String, Object>> models = QueryHelper.models(queryResult);
connection.close();
return models;
}

private StringBuilder toSELECT(DataFetchingEnvironment environment) {
log.info("sparql.select.getGraphQLSchema: "+environment.getGraphQLSchema());
log.info("sparql.select.getGraphQLSchema: "+environment.getGraphQLSchema());
log.info("sparql.select.getArguments: "+environment.getArguments().keySet());
log.info("sparql.select.getFieldType: "+environment.getFieldType());
log.info("sparql.select.getArguments: "+environment.getFieldDefinition().getArguments());
//log.info("sparql.select.getExecutionStepInfo: "+environment.getExecutionStepInfo());
//log.info("sparql.select.args: "+environment.getExecutionStepInfo().getFieldDefinition().getArguments());
Field queryField = environment.getField();
log.info("sparql.select.queryField: "+queryField);
FieldDefinition fieldDefinition = fieldDefinitionMap.get(queryField.getName());
log.info("sparql.select.queryFieldDefinition: "+queryField.getName()+"->"+fieldDefinition);
SelectionSet selectionSet = queryField.getSelectionSet();
log.info("sparql.select.selectionSet: "+selectionSet.getSelections());

// Extract GraphQL context for authorization and tenant info
Object context = environment.getContext();
if (context != null) {
log.info("sparql.select.context: type={}, value={}", context.getClass().getSimpleName(), context);
} else {
log.info("sparql.select.context: null (public query, no tenant/auth context)");
}

// build SPARQL
return toSELECT(queryField, selectionSet.getSelections(), environment.getFieldDefinition().getArguments(), context);
}

private StringBuilder toSELECT(Field queryField, List<Selection> selections, List<GraphQLArgument> arguments) {
return toSELECT(queryField, selections, arguments, null);
}

private StringBuilder toSELECT(Field queryField, List<Selection> selections, List<GraphQLArgument> arguments, Object context) {
log.debug("sparql.select.queryField: "+queryField);
String typeIRI = GQL.toIRI(schemaDefType);
log.debug("sparql.select.typeIRI: "+typeIRI);

StringBuilder select = new StringBuilder();
StringBuilder where = new StringBuilder();
String subject = "?"+(idField==null?"this":idField.getName());
log.debug("sparql.select.subject: "+idField+" -> "+subject);
select.append(subject);

where.append(subject).append(" rdf:type <").append(typeIRI).append(">.\n");

// Extract realm from context and add tenant/authorization filtering
String realm = extractRealmFromContext(context);
if (realm != null) {
// Add realm filtering to WHERE clause to prevent cross-tenant data access
// This enforces multi-tenant isolation at the SPARQL query level
where.append("\n# Realm isolation filter for tenant: ").append(realm).append("\n");
where.append("FILTER( ");
where.append("***REMOVED***(str(").append(subject).append("), \"^").append(realm).append("\") || ");
where.append("***REMOVED***(str(").append(subject).append("), \"#").append(realm).append("$\")");
where.append(" )\n");
log.info("sparql.select.realm_filter: applied - realm={}", realm);
} else {
// No realm specified - this is a public/system query
log.warn("sparql.select.realm_filter: WARNING - no realm context provided (public query or missing context)");
}

selections.forEach( f-> {
if (f instanceof Field) {
Field field = (Field)f;
toSELECT(subject, field, select, where);
}
});
for(GraphQLArgument arg: arguments) {
log.debug("sparql.select.arg: "+arg+" -> "+arg.getDefinition());
String predIRI = GQL.toIRI(arg.getDefinition());
if (predIRI==null) {
// no rdf mapping for this argument; skip
} else if (isRequired(arg.getDefinition().getType())) {
where.append("?").append(idField.getName()).append(" <").append(predIRI).append(">");
where.append(" ?").append(arg.getName()).append(".");
} else {
where.append("\n\tOPTIONAL { ");
where.append("?").append(idField.getName()).append(" <").append(predIRI).append(">");
where.append(" ?").append(arg.getName()).append(".");
where.append("}\n");
}
}
StringBuilder sparql = new StringBuilder();
sparql.append("SELECT DISTINCT ").append(select).append(" WHERE {\n").append(where).append("\n}");
return sparql;
}

/**
 * Extract realm identifier from GraphQL context for multi-tenant isolation.
 * 
 * Tries multiple strategies in order:
 * 1. Explicit "kernel.realm" or "realm" key in context map
 * 2. Derive realm from "actor" principal IRI (expects format like "urn:iq:actor:{realm}:...")
 * 3. Return null if no realm information available (public query)
 * 
 * @param context The GraphQL DataFetchingEnvironment context (typically a Map)
 * @return the realm identifier, or null if not available
 */
String extractRealmFromContext(Object context) {
if (context == null) {
return null;
}

// Case 1: Direct realm key in context map
if (context instanceof Map) {
Map ctx = (Map) context;

// Try explicit realm keys
Object realmObj = ctx.get("kernel.realm");
if (realmObj != null) return realmObj.toString();
realmObj = ctx.get("realm");
if (realmObj != null) return realmObj.toString();

// Case 2: Derive realm from actor principal IRI
// Actor IRIs typically follow patterns like:
// - http://symbol.systems/{realm}/actor/{actorId}
// - urn:iq:actor:{realm}:{actorId}
// - urn:iq:realm:{realm}/actor/{actorId}
Object actorObj = ctx.get("actor");
if (actorObj == null) actorObj = ctx.get("kernel.principal");
if (actorObj == null) actorObj = ctx.get("principal");
if (actorObj == null) actorObj = ctx.get("userPrincipal");

if (actorObj != null) {
String actor = actorObj.toString();
// Parse realm from actor IRI patterns
String extractedRealm = parseRealmFromActorIRI(actor);
if (extractedRealm != null) {
log.debug("sparql.realm_extraction: derived from actor IRI - actor={}, realm={}", actor, extractedRealm);
return extractedRealm;
}
}
}

return null;
}

/**
 * Parse realm identifier from actor IRI using common URI patterns.
 * 
 * Supported patterns:
 * - http://symbol.systems/{realm}/actor/{actorId}
 * - urn:iq:actor:{realm}:{actorId}
 * - urn:iq:realm:{realm}/actor/{actorId}
 * - Fallback: use entire actor IRI as realm (conservative approach)
 * 
 * @param actorIRI the actor's IRI
 * @return the realm identifier, or null if cannot parse
 */
String parseRealmFromActorIRI(String actorIRI) {
if (actorIRI == null || actorIRI.isBlank()) {
return null;
}

// Pattern 1: http://symbol.systems/REALM/actor/...
if (actorIRI.contains("symbol.systems/")) {
String[] parts = actorIRI.split("/");
for (int i = 0; i < parts.length - 1; i++) {
if ("symbol.systems".equals(parts[i]) && i + 1 < parts.length) {
String candidate = parts[i + 1];
if (!candidate.isEmpty() && !"actor".equals(candidate)) {
return candidate;
}
}
}
}

// Pattern 2: urn:iq:actor:REALM:...
if (actorIRI.startsWith("urn:iq:actor:")) {
String suffix = actorIRI.substring("urn:iq:actor:".length());
String[] parts = suffix.split(":");
if (parts.length > 0) {
return parts[0];
}
}

// Pattern 3: urn:iq:realm:REALM/actor/...
if (actorIRI.contains("urn:iq:realm:")) {
String[] parts = actorIRI.split("/");
for (int i = 0; i < parts.length; i++) {
if (parts[i].startsWith("urn:iq:realm:")) {
String realmPart = parts[i].substring("urn:iq:realm:".length());
if (!realmPart.isEmpty()) {
return realmPart;
}
}
}
}

// Fallback: use entire actor IRI as realm (conservative - allows same realm only)
log.debug("sparql.realm_extraction: using entire actor IRI as realm - actor={}", actorIRI);
return actorIRI;
}

private boolean isRequired(Type type) {
log.info("sparql.select.required: "+type+" -> "+type.getClass());
return (type instanceof graphql.schema.GraphQLNonNull || type instanceof graphql.language.NonNullType);
}

private boolean isID(GraphQLInputType type) {
return (type instanceof graphql.schema.GraphQLNonNull);
}
//
//private GraphQLInputType findIDx(GraphQLArgument arg) {
//if (isRequired(arg.getType())) return findID(arg.getType().getChildren());
//return findID(arg.getChildren());
//}

private GraphQLInputType findID(List<GraphQLArgument> children) {
GraphQLInputType type = null;
for(GraphQLSchemaElement child: children) {
log.info("sparql.findID: "+child+" -> "+ child.getChildren());
for(GraphQLSchemaElement ctype: child.getChildren()) {
//graphql.schema.GraphQLNonNull x;
log.info("sparql.findID.ctype: "+ctype+" -> "+ ctype.getClass());
}
}
return type;
}

private void toSELECT(String subject, Field field, StringBuilder select, StringBuilder where) {
String alias = field.getResultKey();
FieldDefinition fieldDefinition = fieldDefinitionMap.get(field.getName());
Type fieldDefinitionType = fieldDefinition.getType();
boolean required = (fieldDefinitionType instanceof NonNullType);
String fieldIRI = GQL.toIRI(fieldDefinition);
log.info("sparql.fieldDefn: "+alias+" -> "+ fieldDefinitionType.getChildren() +" -> "+fieldIRI);
if (fieldIRI!=null) {
select.append(" ?").append(alias);
if (required) {
where.append(subject).append(" <").append(fieldIRI).append("> ?").append(alias).append(".\n");
} else {
where.append("OPTIONAL { ").append(subject).append(" <").append(fieldIRI).append("> ?").append(alias).append(" }.\n");
}
}
}
}
