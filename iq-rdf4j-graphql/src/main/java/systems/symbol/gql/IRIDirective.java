package systems.symbol.gql;

import graphql.schema.GraphQLDirective;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import graphql.schema.idl.SchemaDirectiveWiring;
import graphql.schema.idl.SchemaDirectiveWiringEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class IRIDirective implements SchemaDirectiveWiring {
    private static final Logger log = LoggerFactory.getLogger(IRIDirective.class);

    public GraphQLFieldDefinition onField(SchemaDirectiveWiringEnvironment<GraphQLFieldDefinition> environment) {
        GraphQLFieldDefinition fieldDef = environment.getElement();
        List<GraphQLDirective> directivesByName = fieldDef.getAllDirectivesByName().get("rdf");
        log.info("directivesByName: " + fieldDef.getName() + " -> " + directivesByName);
//		GraphQLArgument iri = fieldDef.getDirective("rdf").getArgument("iri");
//		log.info("onField: "+ iri.getValue()+"-->"+fieldDef);
        return fieldDef;
    }

    public GraphQLObjectType onObject(SchemaDirectiveWiringEnvironment<GraphQLObjectType> environment) {
        GraphQLObjectType objectType = environment.getElement();
//		GraphQLArgument iri = objectType.getDirective("rdf").getArgument("iri");
//		log.info("onObject: "+ iri.getValue()+"-->"+objectType);
        return objectType;
    }

}
