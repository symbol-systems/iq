package systems.symbol.connect.jdbc.schema;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Introspects JDBC database schema via DatabaseMetaData API.
 *
 * <p>
 * Single Responsibility: Extract table/column metadata without generating mappings or RDF.
 * This class is tested independently and can be reused by other connectors (not just IQ).
 * </p>
 *
 * <p>
 * Detects:
 * </p>
 * <ul>
 *   <li>Tables and views</li>
 *   <li>Columns with types, nullability, default values</li>
 *   <li>Primary keys</li>
 *   <li>Foreign keys</li>
 *   <li>Indexes</li>
 * </ul>
 */
public class JdbcSchemaIntrospector {

  private static final Logger LOG = Logger.getLogger(JdbcSchemaIntrospector.class.getName());

  private final Connection connection;
  private final String catalogPattern;
  private final String schemaPattern;

  public JdbcSchemaIntrospector(Connection connection) {
    this(connection, null, null);
  }

  public JdbcSchemaIntrospector(Connection connection, String catalog, String schema) {
    this.connection = connection;
    this.catalogPattern = catalog;
    this.schemaPattern = schema;
  }

  /**
   * Introspect all tables in the configured catalog/schema.
   *
   * @return List of detected table metadata
   * @throws SQLException if introspection fails
   */
  public List<TableMetadata> introspectTables() throws SQLException {
    List<TableMetadata> tables = new ArrayList<>();

    DatabaseMetaData meta = connection.getMetaData();

    // Get all tables in this schema
    try (ResultSet tableRs =
        meta.getTables(catalogPattern, schemaPattern, "%", new String[] {"TABLE", "VIEW"})) {
      while (tableRs.next()) {
        String tableCatalog = tableRs.getString("TABLE_CAT");
        String tableSchema = tableRs.getString("TABLE_SCHEM");
        String tableName = tableRs.getString("TABLE_NAME");
        String tableType = tableRs.getString("TABLE_TYPE");
        String remarks = tableRs.getString("REMARKS");

        TableMetadata table =
            introspectTable(
                tableCatalog,
                tableSchema,
                tableName,
                "VIEW".equalsIgnoreCase(tableType),
                remarks);
        tables.add(table);

        LOG.log(Level.FINE, "Introspected table: {0}.{1} ({2} columns)", 
            new Object[] {tableSchema, tableName, table.getColumns().size()});
      }
    }

    LOG.log(Level.INFO, "Introspected {0} tables", tables.size());
    return tables;
  }

  /**
   * Introspect a single table: columns, keys, indexes.
   *
   * @param catalog Catalog name
   * @param schema Schema name
   * @param tableName Table name
   * @param isView Whether this is a view
   * @param remarks Table remarks/comments
   * @return Table metadata
   * @throws SQLException if introspection fails
   */
  private TableMetadata introspectTable(
      String catalog, String schema, String tableName, boolean isView, String remarks)
      throws SQLException {

    DatabaseMetaData meta = connection.getMetaData();

    // Get columns
    List<ColumnMetadata> columns = new ArrayList<>();
    try (ResultSet colRs = meta.getColumns(catalog, schema, tableName, "%")) {
      while (colRs.next()) {
        String colName = colRs.getString("COLUMN_NAME");
        int dataType = colRs.getInt("DATA_TYPE");
        String typeName = colRs.getString("TYPE_NAME");
        int columnSize = colRs.getInt("COLUMN_SIZE");
        int decimalDigits = colRs.getInt("DECIMAL_DIGITS");
        int nullable = colRs.getInt("NULLABLE");
        String columnDefault = colRs.getString("COLUMN_DEF");
        String columnRemarks = colRs.getString("REMARKS");

        ColumnMetadata col =
            new ColumnMetadata(
                colName,
                dataType,
                typeName,
                columnSize,
                decimalDigits,
                nullable == DatabaseMetaData.columnNullable,
                columnDefault,
                columnRemarks);
        columns.add(col);
      }
    }

    // Get primary keys
    Set<String> primaryKeyColumns = new HashSet<>();
    try (ResultSet pkRs = meta.getPrimaryKeys(catalog, schema, tableName)) {
      while (pkRs.next()) {
        String colName = pkRs.getString("COLUMN_NAME");
        primaryKeyColumns.add(colName);
      }
    }

    // Mark columns as PK
    for (ColumnMetadata col : columns) {
      if (primaryKeyColumns.contains(col.getColumnName())) {
        col.setPrimaryKey(true);
      }
    }

    // Get foreign keys
    List<ForeignKeyMetadata> foreignKeys = introspectForeignKeys(catalog, schema, tableName);

    // Get indexes
    List<IndexMetadata> indexes = introspectIndexes(catalog, schema, tableName);

    return new TableMetadata(
        catalog, schema, tableName, isView, remarks, columns, foreignKeys, indexes);
  }

  /**
   * Introspect foreign keys referencing this table.
   */
  private List<ForeignKeyMetadata> introspectForeignKeys(
      String catalog, String schema, String tableName) throws SQLException {

    List<ForeignKeyMetadata> foreignKeys = new ArrayList<>();
    DatabaseMetaData meta = connection.getMetaData();

    try (ResultSet fkRs = meta.getImportedKeys(catalog, schema, tableName)) {
      while (fkRs.next()) {
        String fkName = fkRs.getString("FK_NAME");
        String pkTableName = fkRs.getString("PKTABLE_NAME");
        String pkColumnName = fkRs.getString("PKCOLUMN_NAME");
        String fkColumnName = fkRs.getString("FKCOLUMN_NAME");

        foreignKeys.add(
            new ForeignKeyMetadata(
                fkName, fkColumnName, pkTableName, pkColumnName, fkRs.getShort("KEY_SEQ")));
      }
    }

    return foreignKeys;
  }

  /**
   * Introspect indexes on this table.
   */
  private List<IndexMetadata> introspectIndexes(String catalog, String schema, String tableName)
      throws SQLException {

    List<IndexMetadata> indexes = new ArrayList<>();
    DatabaseMetaData meta = connection.getMetaData();

    try (ResultSet idxRs =
        meta.getIndexInfo(catalog, schema, tableName, false, false)) {
      Map<String, IndexMetadata> indexMap = new HashMap<>();

      while (idxRs.next()) {
        String indexName = idxRs.getString("INDEX_NAME");
        boolean nonUnique = idxRs.getBoolean("NON_UNIQUE");
        String columnName = idxRs.getString("COLUMN_NAME");

        IndexMetadata idx =
            indexMap.computeIfAbsent(
                indexName,
                k ->
                    new IndexMetadata(
                        indexName, !nonUnique, new ArrayList<>()));

        if (columnName != null) {
          idx.getColumns().add(columnName);
        }
      }

      indexes.addAll(indexMap.values());
    }

    return indexes;
  }

  /** Table metadata (immutable). */
  public static class TableMetadata {
    private final String catalog;
    private final String schema;
    private final String tableName;
    private final boolean isView;
    private final String remarks;
    private final List<ColumnMetadata> columns;
    private final List<ForeignKeyMetadata> foreignKeys;
    private final List<IndexMetadata> indexes;

    public TableMetadata(
        String catalog,
        String schema,
        String tableName,
        boolean isView,
        String remarks,
        List<ColumnMetadata> columns,
        List<ForeignKeyMetadata> foreignKeys,
        List<IndexMetadata> indexes) {
      this.catalog = catalog;
      this.schema = schema;
      this.tableName = tableName;
      this.isView = isView;
      this.remarks = remarks;
      this.columns = List.copyOf(columns);
      this.foreignKeys = List.copyOf(foreignKeys);
      this.indexes = List.copyOf(indexes);
    }

    public String getCatalog() {
      return catalog;
    }

    public String getSchema() {
      return schema;
    }

    public String getTableName() {
      return tableName;
    }

    public boolean isView() {
      return isView;
    }

    public String getRemarks() {
      return remarks;
    }

    public List<ColumnMetadata> getColumns() {
      return columns;
    }

    public List<ForeignKeyMetadata> getForeignKeys() {
      return foreignKeys;
    }

    public List<IndexMetadata> getIndexes() {
      return indexes;
    }

    public long estimateRowCount() {
      // Placeholder: would require running COUNT(*) for accurate estimate
      return -1L;
    }
  }

  /** Column metadata. */
  public static class ColumnMetadata {
    private final String columnName;
    private final int dataType;
    private final String typeName;
    private final int columnSize;
    private final int decimalDigits;
    private final boolean nullable;
    private final String columnDefault;
    private final String remarks;
    private boolean primaryKey = false;

    public ColumnMetadata(
        String columnName,
        int dataType,
        String typeName,
        int columnSize,
        int decimalDigits,
        boolean nullable,
        String columnDefault,
        String remarks) {
      this.columnName = columnName;
      this.dataType = dataType;
      this.typeName = typeName;
      this.columnSize = columnSize;
      this.decimalDigits = decimalDigits;
      this.nullable = nullable;
      this.columnDefault = columnDefault;
      this.remarks = remarks;
    }

    // Getters
    public String getColumnName() {
      return columnName;
    }

    public int getDataType() {
      return dataType;
    }

    public String getTypeName() {
      return typeName;
    }

    public int getColumnSize() {
      return columnSize;
    }

    public int getDecimalDigits() {
      return decimalDigits;
    }

    public boolean isNullable() {
      return nullable;
    }

    public String getColumnDefault() {
      return columnDefault;
    }

    public String getRemarks() {
      return remarks;
    }

    public boolean isPrimaryKey() {
      return primaryKey;
    }

    public void setPrimaryKey(boolean primaryKey) {
      this.primaryKey = primaryKey;
    }
  }

  /** Foreign key metadata. */
  public static class ForeignKeyMetadata {
    private final String fkName;
    private final String fkColumn;
    private final String referencedTable;
    private final String referencedColumn;
    private final short sequence;

    public ForeignKeyMetadata(
        String fkName,
        String fkColumn,
        String referencedTable,
        String referencedColumn,
        short sequence) {
      this.fkName = fkName;
      this.fkColumn = fkColumn;
      this.referencedTable = referencedTable;
      this.referencedColumn = referencedColumn;
      this.sequence = sequence;
    }

    public String getFkName() {
      return fkName;
    }

    public String getFkColumn() {
      return fkColumn;
    }

    public String getReferencedTable() {
      return referencedTable;
    }

    public String getReferencedColumn() {
      return referencedColumn;
    }

    public short getSequence() {
      return sequence;
    }
  }

  /** Index metadata. */
  public static class IndexMetadata {
    private final String indexName;
    private final boolean unique;
    private final List<String> columns;

    public IndexMetadata(String indexName, boolean unique, List<String> columns) {
      this.indexName = indexName;
      this.unique = unique;
      this.columns = List.copyOf(columns);
    }

    public String getIndexName() {
      return indexName;
    }

    public boolean isUnique() {
      return unique;
    }

    public List<String> getColumns() {
      return columns;
    }
  }
}
