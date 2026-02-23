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
// TODO:Object context = environment.getContext();

// build SPARQL
return toSELECT(queryField, selectionSet.getSelections(), environment.getFieldDefinition().getArguments());
}

private StringBuilder toSELECT(Field queryField, List<Selection> selections, List<GraphQLArgument> arguments) {
log.debug("sparql.select.queryField: "+queryField);
String typeIRI = GQL.toIRI(schemaDefType);
log.debug("sparql.select.typeIRI: "+typeIRI);

StringBuilder select = new StringBuilder();
StringBuilder where = new StringBuilder();
String subject = "?"+(idField==null?"this":idField.getName());
log.debug("sparql.select.subject: "+idField+" -> "+subject);
select.append(subject);

where.append(subject).append(" rdf:type <").append(typeIRI).append(">.\n");
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
