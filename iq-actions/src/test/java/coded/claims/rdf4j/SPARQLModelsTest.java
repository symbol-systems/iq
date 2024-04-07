package systems.symbol.rdf4j;

import systems.symbol.core.IQException;
import systems.symbol.iq.AbstractIQTest;
import systems.symbol.model.NamedMap;
import systems.symbol.rdf4j.iq.KBMS;
import org.eclipse.rdf4j.model.IRI;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SPARQLMapperTest extends AbstractIQTest {
    String testCaseURL = "https://test.symbol.systems/cases#TestCase";


    @Test
    public void testModel() {
        KBMS kbms = new KBMS(ctx, repository.getConnection());
        SPARQLMapper sparql = new SPARQLMapper(kbms);
        Map<String,Object> binding = new HashMap<>();
        binding.put(NS.KEY_AT_ID, iriSelect);
        NamedMap namedMap = sparql.model(binding);
        assert namedMap !=null;
        assert namedMap.size()==1;
        assert namedMap.getIdentity().equals(iriSelect);
    }

    @Test
    public void testGetQuery() throws IQException {
        KBMS kbms = new KBMS(ctx, repository.getConnection());
        SPARQLMapper sparql = new SPARQLMapper(kbms);
        String query = sparql.findQuery(iriSelect);
        assert query!=null;
//        System.out.println("iq.models.query: "+query);
    }

    @Test
    public void testModels() throws IQException {
        KBMS kbms = new KBMS(ctx, repository.getConnection());
        SPARQLMapper sparql = new SPARQLMapper(kbms);
        List<Map<String,Object>> models = sparql.models(iriSelect);
        System.out.println("iq.models.select.models: "+iriSelect+" -< "+ models);
        assert models!=null;
        assert !models.isEmpty();
    }

    @Test
    public void testModelsPivot() {
        KBMS kbms = new KBMS(ctx, repository.getConnection());
        SPARQLMapper sparql = new SPARQLMapper(kbms);
        Map<String,Object> binding = new HashMap<>();
        binding.put(NS.KEY_AT_ID, testCaseURL);
        binding.put("this", testCaseURL+"Ignored");
        Map<String,Object> pivot = sparql.pivotOn(binding, NS.KEY_AT_ID, NS.KEY_AT_THIS);
        System.out.println("iq.models.pivot: " + pivot);
        assert pivot != null;
        assert pivot.get(NS.KEY_AT_THIS) == testCaseURL;
        assert !pivot.containsKey(NS.KEY_AT_ID);
        assert pivot.containsKey("this");
    }

    @Test
    public void testModelsBindingPivot() throws IQException {
        KBMS kbms = new KBMS(ctx, repository.getConnection());
        SPARQLMapper sparql = new SPARQLMapper(kbms);
        HashMap<String,Object> binding = new HashMap<>();
        binding.put(NS.KEY_AT_ID, testCaseURL);
        System.out.println("iq.models.binding: "+binding);

        List<Map<String,Object>> models = sparql.models(iriSelect, binding); // follow pref:inScheme ... to TestSuite
        assert models!=null;
        System.out.println("iq.models.binding.models: "+models.size());
        assert models.size() == 3;
        Map<String,Object> found = models.stream().iterator().next();
        NamedMap namedMap = new NamedMap(found);
        System.out.println("iq.models.binding.found: "+ namedMap);
        assert namedMap.get("id")!=null;
    }

    @Test
    public void testFindQuery() {
        KBMS kbms = new KBMS(ctx, repository.getConnection());
        SPARQLMapper sparql = new SPARQLMapper(kbms);
        IRI queryIRI = kbms.toIRI("queries/actions.sparql");
        System.out.println("iq.models.queryIRI.acl: "+queryIRI);
        String acl_query = sparql.findQuery(queryIRI);
        assert acl_query!=null;
        assert acl_query.contains("SELECT");
    }

    @Test
    public void testGetAllQueries() {
    }


    @Test
    public void testQueryResultsToModels() {
    }

    @Test
    public void testPivotOn() {
        KBMS kbms = new KBMS(ctx, repository.getConnection());
        SPARQLMapper sparql = new SPARQLMapper(kbms);
        Map model = new HashMap();
        model.put(NS.KEY_AT_ID, NS.GG_TEST); // will pivot to KEY_AT_THIS
        Map pivotOn = sparql.pivotOn(model);
        assert pivotOn != null;
        assert pivotOn.get(NS.KEY_AT_THIS) == NS.GG_TEST;
        assert pivotOn.size() == 1;
    }

    @Test
    public void testQuery() {
    }

    @Test
    public void testSetBindings() {
    }

    @Test
    public void testGraph() {
    }

    @Test
    public void testACLQuery() {
        KBMS kbms = new KBMS(ctx, repository.getConnection());
        SPARQLMapper sparql = new SPARQLMapper(kbms);
        Map<String,Object> binding = new HashMap<>();
        IRI queryIRI = kbms.toIRI(NS.GG_TEST+"queries/actions.sparql");
        String query = sparql.findQuery(queryIRI);
        assert query != null;
        System.out.println("iq.models.acl.query: "+ query);
        List<Map<String,Object>> models = sparql.models(queryIRI, binding);
        System.out.println("iq.models.acl.found: "+models);
        assert models!=null;
        assert models.size()>0;
    }

    @Test
    public void testAsk() throws IQException {
        KBMS kbms = new KBMS(ctx, repository.getConnection());
        SPARQLMapper sparql = new SPARQLMapper(kbms);
        IRI queryIRI = kbms.toIRI(NS.GG_TEST+"queries/allow.sparql");
        String query = sparql.findQuery(queryIRI);
        System.out.println("iq.models.testAsk.query: "+ query);
        assert query!=null;
        assert query.contains("ASK");
        Map model = new HashMap();
        model.put(NS.KEY_AT_ID, "https://test.symbol.systems/public/"); // will pivot to KEY_AT_THIS
        model.put("@agent", "http://www.w3.org/ns/auth/acl#Anonymous");
        model.put("@mode", "http://www.w3.org/ns/auth/acl#Read");
        Boolean found = sparql.ask(queryIRI, model);
        System.out.println("iq.models.testAsk.found: "+found);
        assert found!=null;
        assert found == true;
    }

    @Test
    public void testPrefixed() {
        KBMS kbms = new KBMS(ctx, repository.getConnection());
        SPARQLMapper sparql = new SPARQLMapper(kbms);
        String query = sparql.prefixed("ASK");
        assert query!=null;
        assert query.contains("PREFIX");
    }

}