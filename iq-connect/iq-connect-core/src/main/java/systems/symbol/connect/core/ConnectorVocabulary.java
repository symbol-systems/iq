package systems.symbol.connect.core;

/**
 * Common IRIs used by connector implementations.
 */
public final class ConnectorVocabulary {

    public static final String PREFIX = "https://symbol.systems/v0/connect#";

    public static final String HAS_SYNC_STATUS = PREFIX + "syncStatus";
    public static final String LAST_SYNCED_AT = PREFIX + "lastSyncedAt";
    public static final String CHECKPOINT = PREFIX + "checkpoint";
    public static final String HAS_RESOURCE = PREFIX + "hasResource";

    private ConnectorVocabulary() {
        // utility
    }
}
