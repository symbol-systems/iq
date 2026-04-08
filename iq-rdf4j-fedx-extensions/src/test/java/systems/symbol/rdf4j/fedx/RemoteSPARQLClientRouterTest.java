package systems.symbol.rdf4j.fedx;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.rdf4j.query.TupleQueryResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Unit tests for RemoteSPARQLClientRouter.
 *
 * Tests strategy dispatch mechanism and routing logic.
 */
@DisplayName("RemoteSPARQLClientRouter")
class RemoteSPARQLClientRouterTest {

  @Mock
  private I_RemoteSPARQLExecutor mockJdbcExecutor;

  @Mock
  private I_RemoteSPARQLExecutor mockOpenApiExecutor;

  @Mock
  private I_RemoteSPARQLExecutor mockFallbackExecutor;

  @Mock
  private TupleQueryResult mockResult;

  private RemoteSPARQLClientRouter router;
  private FedXEndpoint jdbcEndpoint;
  private FedXEndpoint openApiEndpoint;
  private FedXEndpoint unknownEndpoint;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);

    // Set up mock executors with canHandle logic
    when(mockJdbcExecutor.canHandle(any())).thenAnswer(invocation -> {
      FedXEndpoint ep = invocation.getArgument(0);
      return ep.sparqlEndpoint().startsWith("urn:iq:sparql:jdbc:");
    });

    when(mockOpenApiExecutor.canHandle(any())).thenAnswer(invocation -> {
      FedXEndpoint ep = invocation.getArgument(0);
      return ep.sparqlEndpoint().startsWith("urn:iq:sparql:api:");
    });

    when(mockFallbackExecutor.canHandle(any())).thenReturn(true);

    // Set up execute to return mock result
    when(mockJdbcExecutor.execute(any(), any())).thenReturn(mockResult);
    when(mockOpenApiExecutor.execute(any(), any())).thenReturn(mockResult);
    when(mockFallbackExecutor.execute(any(), any())).thenReturn(mockResult);

    // Create router with executors in order: specific first, fallback last
    List<I_RemoteSPARQLExecutor> executors = new ArrayList<>();
    executors.add(mockJdbcExecutor);
    executors.add(mockOpenApiExecutor);
    executors.add(mockFallbackExecutor);

    router = new RemoteSPARQLClientRouter(executors);

    // Create test endpoints
    jdbcEndpoint = new FedXEndpoint(
        "jdbc-endpoint",
        "jdbc:h2:mem:testdb",
        "urn:iq:sparql:jdbc:users",
        true,
        false,
        null);

    openApiEndpoint = new FedXEndpoint(
        "api-endpoint",
        "https://api.example.com",
        "urn:iq:sparql:api:users",
        true,
        false,
        null);

    unknownEndpoint = new FedXEndpoint(
        "unknown-endpoint",
        "unknown://endpoint",
        "urn:iq:sparql:unknown:resource",
        true,
        false,
        null);
  }

  @Test
  @DisplayName("should route JDBC queries to JDBC executor")
  void routeJdbcQueriesToJdbcExecutor() throws Exception {
    String sparqlQuery = "SELECT * FROM ?table";

    TupleQueryResult result = router.executeQuery(jdbcEndpoint, sparqlQuery);

    assertNotNull(result);
    verify(mockJdbcExecutor).canHandle(jdbcEndpoint);
    verify(mockJdbcExecutor).execute(jdbcEndpoint, sparqlQuery);
    verify(mockOpenApiExecutor, never()).execute(any(), any());
    verify(mockFallbackExecutor, never()).execute(any(), any());
  }

  @Test
  @DisplayName("should route OpenAPI queries to OpenAPI executor")
  void routeOpenApiQueriesToOpenApiExecutor() throws Exception {
    String sparqlQuery = "SELECT * FROM ?service";

    TupleQueryResult result = router.executeQuery(openApiEndpoint, sparqlQuery);

    assertNotNull(result);
    verify(mockOpenApiExecutor).canHandle(openApiEndpoint);
    verify(mockOpenApiExecutor).execute(openApiEndpoint, sparqlQuery);
    verify(mockJdbcExecutor, never()).execute(any(), any());
    verify(mockFallbackExecutor, never()).execute(any(), any());
  }

  @Test
  @DisplayName("should use fallback executor when no specific executor matches")
  void useFallbackExecutorWhenNoMatch() throws Exception {
    String sparqlQuery = "SELECT * FROM ?resource";

    TupleQueryResult result = router.executeQuery(unknownEndpoint, sparqlQuery);

    assertNotNull(result);
    verify(mockJdbcExecutor).canHandle(unknownEndpoint);
    verify(mockOpenApiExecutor).canHandle(unknownEndpoint);
    verify(mockFallbackExecutor).canHandle(unknownEndpoint);
    verify(mockFallbackExecutor).execute(unknownEndpoint, sparqlQuery);
  }

  @Test
  @DisplayName("should respect executor order (specific before fallback)")
  void respectExecutorOrder() throws Exception {
    String sparqlQuery = "SELECT * FROM ?table";

    TupleQueryResult result = router.executeQuery(jdbcEndpoint, sparqlQuery);

    // Verify JDBC executor is called before fallback
    InOrder inOrder = inOrder(mockJdbcExecutor, mockFallbackExecutor);
    inOrder.verify(mockJdbcExecutor).canHandle(jdbcEndpoint);
    inOrder.verify(mockJdbcExecutor).execute(jdbcEndpoint, sparqlQuery);
    inOrder.verify(mockFallbackExecutor, never()).execute(any(), any());
  }

  @Test
  @DisplayName("should pass query to correct executor")
  void passQueryToCorrectExecutor() throws Exception {
    String sparqlQuery = "SELECT ?subject WHERE { ?subject ?predicate ?object }";

    router.executeQuery(jdbcEndpoint, sparqlQuery);

    verify(mockJdbcExecutor).execute(jdbcEndpoint, sparqlQuery);
  }

  @Test
  @DisplayName("should handle multiple queries independently")
  void handleMultipleQueriesIndependently() throws Exception {
    String query1 = "SELECT * FROM table1";
    String query2 = "SELECT * FROM table2";

    router.executeQuery(jdbcEndpoint, query1);
    router.executeQuery(openApiEndpoint, query2);

    verify(mockJdbcExecutor).execute(jdbcEndpoint, query1);
    verify(mockOpenApiExecutor).execute(openApiEndpoint, query2);
  }

  @Test
  @DisplayName("should return executor result directly")
  void returnExecutorResultDirectly() throws Exception {
    String sparqlQuery = "SELECT * FROM ?table";

    TupleQueryResult result = router.executeQuery(jdbcEndpoint, sparqlQuery);

    assertEquals(mockResult, result);
  }

  @Test
  @DisplayName("should call canHandle before execute")
  void callCanHandleBeforeExecute() throws Exception {
    String sparqlQuery = "SELECT * FROM ?table";

    router.executeQuery(jdbcEndpoint, sparqlQuery);

    InOrder inOrder = inOrder(mockJdbcExecutor);
    inOrder.verify(mockJdbcExecutor).canHandle(jdbcEndpoint);
    inOrder.verify(mockJdbcExecutor).execute(jdbcEndpoint, sparqlQuery);
  }

  @Test
  @DisplayName("should work with single executor")
  void workWithSingleExecutor() throws Exception {
    List<I_RemoteSPARQLExecutor> executors = new ArrayList<>();
    executors.add(mockFallbackExecutor);

    RemoteSPARQLClientRouter singleRouter = new RemoteSPARQLClientRouter(executors);

    String sparqlQuery = "SELECT * FROM ?table";

    TupleQueryResult result = singleRouter.executeQuery(jdbcEndpoint, sparqlQuery);

    assertNotNull(result);
    verify(mockFallbackExecutor).canHandle(jdbcEndpoint);
    verify(mockFallbackExecutor).execute(jdbcEndpoint, sparqlQuery);
  }

  @Test
  @DisplayName("should iterate executors in order")
  void iterateExecutorsInOrder() throws Exception {
    String sparqlQuery = "SELECT * FROM ?table";

    // Make first executor return false, second return true
    when(mockJdbcExecutor.canHandle(any())).thenReturn(false);
    when(mockOpenApiExecutor.canHandle(any())).thenReturn(false);
    when(mockFallbackExecutor.canHandle(any())).thenReturn(true);

    router.executeQuery(unknownEndpoint, sparqlQuery);

    InOrder inOrder = inOrder(mockJdbcExecutor, mockOpenApiExecutor, mockFallbackExecutor);
    inOrder.verify(mockJdbcExecutor).canHandle(unknownEndpoint);
    inOrder.verify(mockOpenApiExecutor).canHandle(unknownEndpoint);
    inOrder.verify(mockFallbackExecutor).canHandle(unknownEndpoint);
    inOrder.verify(mockFallbackExecutor).execute(unknownEndpoint, sparqlQuery);
  }

  @Test
  @DisplayName("should be stateless between invocations")
  void beStatelessBetweenInvocations() throws Exception {
    String query1 = "SELECT 1";
    String query2 = "SELECT 2";

    router.executeQuery(jdbcEndpoint, query1);
    router.executeQuery(openApiEndpoint, query2);

    // Each query should be handled independently
    verify(mockJdbcExecutor).execute(jdbcEndpoint, query1);
    verify(mockOpenApiExecutor).execute(openApiEndpoint, query2);
  }

  @Test
  @DisplayName("should support dynamic executor registration")
  void supportDynamicExecutorRegistration() throws Exception {
    List<I_RemoteSPARQLExecutor> executors = new ArrayList<>();
    executors.add(mockJdbcExecutor);

    RemoteSPARQLClientRouter dynamicRouter = new RemoteSPARQLClientRouter(executors);

    String sparqlQuery = "SELECT * FROM ?table";

    TupleQueryResult result = dynamicRouter.executeQuery(jdbcEndpoint, sparqlQuery);

    assertNotNull(result);
    verify(mockJdbcExecutor).execute(jdbcEndpoint, sparqlQuery);
  }
}
