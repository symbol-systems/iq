package systems.symbol.platform;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.PROV;
import org.eclipse.rdf4j.model.vocabulary.RDF;

/**
 * The Provenance class provides methods to generate PROV-O (Provenance Ontology) statements
 * within an RDF model to represent provenance information.
 *
 * It is part of the systems.symbol.agent package and relies on the Eclipse RDF4J framework.
 *
 * @author [Your Name]
 * @version 1.1
 */
public class Provenance {
    /**
     * Generates PROV-O statements representing the 'generation' activity in provenance.
     *
     * @param model   The RDF model to which the provenance statements will be added.
     * @param actor    The IRI representing the agent or entity that performed the generation.
     * @param activity The IRI representing the generation activity.
     * @param result   The IRI representing the generated result.
     */
    public static void generated(Model model, Resource actor, Resource activity, Resource result, IRI ctx) {
        SimpleValueFactory vf = SimpleValueFactory.getInstance();

        // Create PROV-O statements
        Resource actorResource = vf.createIRI(actor.stringValue());
        Resource activityResource = activity.isIRI()?vf.createIRI(activity.stringValue()):vf.createBNode(activity.stringValue());
        Resource resultResource = vf.createIRI(result.stringValue());

        model.add(actorResource, RDF.TYPE, PROV.AGENT, ctx);
        model.add(activityResource, RDF.TYPE, PROV.ACTIVITY, ctx);
        model.add(resultResource, RDF.TYPE, PROV.ENTITY, ctx);

        model.add(activityResource, PROV.GENERATED, resultResource, ctx);
        model.add(actorResource, PROV.GENERATED, resultResource, ctx);
        model.add(resultResource, PROV.WAS_GENERATED_BY, actorResource, ctx);
    }

    /**
     * Generates PROV-O statements representing the 'usage' relation in provenance.
     *
     * @param model   The RDF model to which the provenance statements will be added.
     * @param activity The IRI representing the using activity.
     * @param entity   The IRI representing the used entity.
     */
    public static void used(Model model, IRI activity, IRI entity, IRI ctx) {
        SimpleValueFactory vf = SimpleValueFactory.getInstance();

        // Create PROV-O statements
        Resource activityResource = vf.createIRI(activity.stringValue());
        Resource entityResource = vf.createIRI(entity.stringValue());

        // Assertion: activity used entity
        model.add(activityResource, PROV.USED, entityResource, ctx);

        // Assertion: entity wasUsedBy activity
        model.add(entityResource, PROV.USAGE, activityResource, ctx);

        // Optional: Add type information
        model.add(activityResource, RDF.TYPE, PROV.ACTIVITY, ctx);
        model.add(entityResource, RDF.TYPE, PROV.ENTITY, ctx);
    }

    /**
     * Generates PROV-O statements representing the 'derivation' relation in provenance.
     *
     * @param model          The RDF model to which the provenance statements will be added.
     * @param generatedEntity The IRI representing the generated entity.
     * @param usedEntity      The IRI representing the used entity for derivation.
     */
    public static void wasDerivedFrom(Model model, IRI generatedEntity, IRI usedEntity,IRI ctx) {
        SimpleValueFactory vf = SimpleValueFactory.getInstance();

        // Create PROV-O statements
        Resource generatedEntityResource = vf.createIRI(generatedEntity.stringValue());
        Resource usedEntityResource = vf.createIRI(usedEntity.stringValue());

        // Assertion: generatedEntity wasDerivedFrom usedEntity
        model.add(generatedEntityResource, PROV.WAS_DERIVED_FROM, usedEntityResource, ctx);

        // Assertion: usedEntity wasDerivedTo generatedEntity
        model.add(usedEntityResource, PROV.DERIVATION, generatedEntityResource, ctx);

        // Optional: Add type information
        model.add(generatedEntityResource, RDF.TYPE, PROV.ENTITY, ctx);
        model.add(usedEntityResource, RDF.TYPE, PROV.ENTITY, ctx);
    }

    /**
     * Generates PROV-O statements representing the 'attribution' relation in provenance.
     *
     * @param model  The RDF model to which the provenance statements will be added.
     * @param entity The IRI representing the entity being attributed.
     * @param agent  The IRI representing the agent or entity to whom the attribution is made.
     */
    public static void wasAttributedTo(Model model, IRI entity, IRI agent) {
        SimpleValueFactory vf = SimpleValueFactory.getInstance();

        // Create PROV-O statements
        Resource entityResource = vf.createIRI(entity.stringValue());
        Resource agentResource = vf.createIRI(agent.stringValue());

        // Assertion: entity wasAttributedTo agent
        model.add(entityResource, PROV.WAS_ATTRIBUTED_TO, agentResource);

        // Assertion: agent attributed entity
        model.add(agentResource, PROV.ATTRIBUTION, entityResource);

        // Optional: Add type information
        model.add(entityResource, RDF.TYPE, PROV.ENTITY);
        model.add(agentResource, RDF.TYPE, PROV.AGENT);
    }

    // Existing generated method remains unchanged

    /**
     * Generates PROV-O statements representing the 'revision' relation in provenance.
     *
     * @param model       The RDF model to which the provenance statements will be added.
     * @param newerEntity The IRI representing the newer version of the entity.
     * @param olderEntity The IRI representing the older version of the entity.
     */
    public static void wasRevisionOf(Model model, IRI newerEntity, IRI olderEntity) {
        SimpleValueFactory vf = SimpleValueFactory.getInstance();

        // Create PROV-O statements
        Resource newerEntityResource = vf.createIRI(newerEntity.stringValue());
        Resource olderEntityResource = vf.createIRI(olderEntity.stringValue());

        // Assertion: newerEntity wasRevisionOf olderEntity
        model.add(newerEntityResource, PROV.WAS_REVISION_OF, olderEntityResource);

        // Assertion: olderEntity wasRevisionFor newerEntity
        model.add(olderEntityResource, PROV.REVISION, newerEntityResource);

        // Optional: Add type information
        model.add(newerEntityResource, RDF.TYPE, PROV.ENTITY);
        model.add(olderEntityResource, RDF.TYPE, PROV.ENTITY);
    }

    /**
     * Adds a custom attribute to a subject in the RDF model.
     *
     * @param model    The RDF model to which the custom attribute will be added.
     * @param subject  The IRI representing the subject to which the attribute is added.
     * @param property The IRI representing the custom attribute property.
     * @param value    The value of the custom attribute.
     */
    public static void addCustomAttribute(Model model, IRI subject, IRI property, String value) {
        SimpleValueFactory vf = SimpleValueFactory.getInstance();

        // Create PROV-O statements
        IRI subjectResource = vf.createIRI(subject.stringValue());
        IRI propertyResource = vf.createIRI(property.stringValue());

        // Assertion: subject has custom attribute
        model.add(subjectResource, propertyResource, vf.createLiteral(value));

        // Optional: Add type information for property (assuming it's a custom property)
        model.add(propertyResource, RDF.TYPE, RDF.PROPERTY);
    }
}
