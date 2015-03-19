package integration.dbunit

import java.sql.Connection
import java.sql.DriverManager
import org.dbunit.database.CachedResultSetTableFactory
import org.dbunit.database.DatabaseConfig
import org.dbunit.database.DatabaseConnection
import org.dbunit.database.DatabaseSequenceFilter
import org.dbunit.database.IDatabaseConnection
import org.dbunit.dataset.FilteredDataSet
import org.dbunit.dataset.IDataSet
import org.dbunit.dataset.filter.IncludeTableFilter
import org.dbunit.ext.mssql.InsertIdentityOperation
import org.dbunit.operation.DatabaseOperation
import org.dbunit.dataset.filter.SequenceTableFilter
import org.apache.commons.io.FileUtils
import java.io.File
import play.api.libs.json.Json

trait AbstractIntegrationTest {

  var jdbcConnection: Connection = null

  def getIDatabaseConnection(): IDatabaseConnection = {
    if (jdbcConnection == null) {
      jdbcConnection = DriverManager.getConnection(getJdbcURL, getUserName, getPassword);
    } else {
      if (jdbcConnection.isClosed()) {
        jdbcConnection = DriverManager.getConnection(getJdbcURL, getUserName, getPassword);
      }
    }
    val dbConnection = new DatabaseConnection(jdbcConnection, getSchemaName);
    val config = dbConnection.getConfig();
    config.setProperty(DatabaseConfig.PROPERTY_RESULTSET_TABLE_FACTORY, new CachedResultSetTableFactory());
    return dbConnection;
  }

  private def getUserName = "myapp"
  private def getPassword = "myapp"
  private def getSchemaName = "public"
  private def getJdbcURL = "jdbc:postgresql://localhost:5432/myapp_test?stringtype=unspecified"

  def afterEachTest() {
    try {
      jdbcConnection.close()
    } catch {
      case _: Throwable => {}
    }
  }

  def getDbConnection(): Connection = {
    val conn = getIDatabaseConnection().getConnection()
    conn.setAutoCommit(true)
    conn.setReadOnly(false)
    conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED)
    conn
  }

  /**
   * This should be called before each unit test. Performs the truncation and reset of sequences of the database.
   *
   * @param data DbUnit dataset.
   * @param noAudit True if you want no auditing; false otherwise.
   * @throws Exception
   */
  def setUpBeforeClass(datasetPath: String) {
    setUpBeforeClass(new JSONDataSet(datasetPath), true)
  }
  
  /**
   * This should be called before each unit test. Performs the truncation and reset of sequences of the database.
   *
   * @param data DbUnit dataset.
   * @param noAudit True if you want no auditing; false otherwise.
   * @throws Exception
   */
  def setUpBeforeClass(data: IDataSet) {
    setUpBeforeClass(data, true)
  }

  /**
   * This should be called before each unit test. Performs the truncation and reset of sequences of the database.
   *
   * @param data DbUnit dataset.
   * @param noAudit True if you want no auditing; false otherwise.
   * @param rebuildDb True if you want the database rebuilt.
   * @throws Exception
   */
  def setUpBeforeClass(data: IDataSet, rebuildDb: Boolean) {
    setUpBeforeClass(data, rebuildDb, getJdbcURL)
  }

  def setUpBeforeClass(data: IDataSet, rebuildDb: Boolean, url: String) {
    Class.forName("org.postgresql.Driver")
    if (rebuildDb) {
      val conn = getIDatabaseConnection()
      truncateDatabase(conn)

      val filter = new DatabaseSequenceFilter(conn)
      val dataset = new FilteredDataSet(filter, data)
      try {
        InsertIdentityOperation.CLEAN_INSERT.execute(conn, dataset)
      } finally {
        conn.close()
      }
    }
  }

  /**
   * Returns the database operation executed in test setup.
   */
  def getSetUpOperation(): DatabaseOperation = {
    DatabaseOperation.INSERT
  }

  /**
   * Returns the database operation executed in test cleanup.
   */
  def getTearDownOperation(): DatabaseOperation = {
    DatabaseOperation.NONE
  }

  /**
   * Truncate all database tables. To add a table to be truncate, just add a filter pattern as shown below.
   *
   * @param connection DbUnit database connection.
   */
  def truncateDatabase(connection: IDatabaseConnection) {
    val filter = new IncludeTableFilter()
    filter.includeTable("*")
    val dataset = new FilteredDataSet(filter, connection.createDataSet())
    val tableFilter = new SequenceTableFilter(getTruncationOrder())
    val filteredDataset = new FilteredDataSet(tableFilter, dataset)
    DatabaseOperation.DELETE_ALL.execute(connection, filteredDataset)
  }
  
  def getTruncationOrder(): Array[String] = {
     val insertSequenceOrderFile = new File("test/integration/insertSequence.json")
     val insertOrderAsString = FileUtils.readFileToString(insertSequenceOrderFile)
     val insertOrder = Json.fromJson[Seq[TableDefinition]](Json.parse(insertOrderAsString)).get
     val tablesInReverseOrder = insertOrder.map(_.name) //.reverse
     val tablesReverseOrder = new Array[String](tablesInReverseOrder.size)
     tablesInReverseOrder.copyToArray(tablesReverseOrder)
     //for(t <- tablesInReverseOrder) println(t)
     tablesReverseOrder
  }

}
