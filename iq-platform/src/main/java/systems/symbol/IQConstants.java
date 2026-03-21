package systems.symbol;

/**
 * Central repository for IQ platform constants.
 * 
 * Consolidates magic numbers, strings, timeouts, and configuration values
 * to improve maintainability and consistency across modules.
 * 
 * @author Symbol Systems
 * @since 0.92
 */
public final class IQConstants {

// ==================== Timeouts (milliseconds) ====================

/** Default timeout for SPARQL query execution (5 seconds) */
public static final int QUERY_TIMEOUT_MS = 5000;

/** Default timeout for RDF repository operations (10 seconds) */
public static final int REPO_TIMEOUT_MS = 10000;

/** ACL check timeout (3 seconds) */
public static final int ACL_CHECK_TIMEOUT_MS = 3000;

// ==================== CLI Timeouts (seconds) ====================

/** Default timeout for CLI boot sequence */
public static final int BOOT_TIMEOUT_S = 30;

/** Default timeout for waiting for actor readiness state */
public static final int ACTOR_READY_TIMEOUT_S = 300;

// ==================== LLM Endpoints ====================

/** OpenAI API endpoint for chat completions */
public static final String OPENAI_COMPLETIONS = "https://api.openai.com/v1/chat/completions";

/** Groq API endpoint for chat completions */
public static final String GROQ_COMPLETIONS = "https://api.groq.com/openai/v1/chat/completions";

// ==================== RDF Namespaces & Prefixes ====================

/**
 * IQ default namespace.
 * Used for internal RDF vocabulary (actors, realms, permissions, etc.)
 */
public static final String NS_IQ = "urn:iq:";

/** vCard namespace for contact information */
public static final String NS_VCARD = "http://www.w3.org/2006/vcard/ns#";

/** RDF namespace */
public static final String NS_RDF = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";

/** RDFS namespace */
public static final String NS_RDFS = "http://www.w3.org/2000/01/rdf-schema#";

// ==================== Common RDF Classes & Properties ====================

/** Actor class IRI */
public static final String IRI_ACTOR = NS_IQ + "Actor";

/** Realm class IRI */
public static final String IRI_REALM = NS_IQ + "Realm";

/** ACL: hasAccessTo property (allow rule) */
public static final String IRI_HAS_ACCESS = NS_IQ + "hasAccessTo";

/** ACL: isBlockedFrom property (deny rule) */
public static final String IRI_IS_BLOCKED = NS_IQ + "isBlockedFrom";

/** Property: isPublic (boolean) */
public static final String IRI_IS_PUBLIC = NS_IQ + "isPublic";

// ==================== Internationalization (i18n) Keys ====================

// Boot command messages
public static final String MSG_BOOT_FAILED = "iq.cli.boot.failed";
public static final String MSG_BOOT_SUCCESS = "iq.cli.boot.success";
public static final String MSG_BOOT_STARTING = "iq.cli.boot.starting";
public static final String MSG_BOOT_COMPLETE = "iq.cli.boot.complete";
public static final String MSG_BOOT_NO_ACTORS = "iq.cli.boot.no_actors";
public static final String MSG_BOOT_ERROR = "iq.cli.boot.error";
public static final String MSG_BOOT_TIMEOUT = "iq.cli.boot.timeout";

// LLM messages
public static final String MSG_LLM_CONFIG_NULL = "llm.config.null";
public static final String MSG_LLM_NAME_MISSING = "llm.name.missing";
public static final String MSG_LLM_URL_MISSING = "llm.url.missing";
public static final String MSG_LLM_SECRET_KEY_MISSING = "llm.secret.key.missing";
public static final String MSG_LLM_SECRET_KEY_INVALID = "llm.secret.key.invalid";
public static final String MSG_LLM_SECRET_MISSING = "llm.secret.missing_or_empty";
public static final String MSG_LLM_INITIALIZED = "llm.initialized";

// ACL messages
public static final String MSG_ACL_DENIED = "acl.access.denied";
public static final String MSG_ACL_GRANTED = "acl.access.granted";
public static final String MSG_ACL_ANONYMOUS = "acl.access.anonymous";
public static final String MSG_ACL_BLOCKED = "acl.access.blocked";
public static final String MSG_ACL_ADMIN = "acl.access.admin";
public static final String MSG_ACL_PUBLIC = "acl.realm.public";

// ==================== Cache Configuration ====================

/** ACL query result cache TTL (minutes) */
public static final int ACL_CACHE_TTL_MINUTES = 5;

/** Maximum size of ACL query result cache */
public static final int ACL_CACHE_MAX_SIZE = 1000;

// ==================== Performance Tuning ====================

/** Default number of threads for parallel RDF operations */
public static final int RDF_EXECUTOR_THREADS = 4;

/** Default batch size for bulk RDF operations */
public static final int RDF_BATCH_SIZE = 100;

// ==================== Versioning ====================

/** IQ platform version */
public static final String IQ_VERSION = "0.92";

/** IQ codename */
public static final String IQ_CODENAME = "Symbol";

// Prevent instantiation
private IQConstants() {
throw new AssertionError("IQConstants should not be instantiated");
}
}
