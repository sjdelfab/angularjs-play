package integration.dbunit

import java.io.File
import java.lang.reflect.Field
import java.sql.Connection
import java.sql.DriverManager

import scala.collection.mutable.ArrayBuffer

import org.apache.commons.io.FileUtils
import org.dbunit.database.CachedResultSetTableFactory
import org.dbunit.database.DatabaseConfig
import org.dbunit.database.DatabaseConnection
import org.dbunit.database.DatabaseSequenceFilter
import org.dbunit.database.IDatabaseConnection
import org.dbunit.dataset.IDataSet
import org.dbunit.dataset.ITableMetaData
import org.dbunit.dataset.OrderedTableNameMap
import org.dbunit.dataset.filter.SequenceTableFilter

import play.api.libs.json.Json

object GenerateDatabaseInsertOrder extends App {

  private def getUserName = "myapp"
  private def getPassword = "myapp"
  private def getSchemaName = "public"
  private def getJdbcURL = "jdbc:postgresql://localhost:5432/myapp"
  
  var jdbcConnection: Connection = null
  
  val tableInsertOrder = getTableInsertOrder
  
  FileUtils.write(new File("test/integration/insertSequence.json"),Json.prettyPrint(Json.toJson(tableInsertOrder.tables)))
  
  private def getTableInsertOrder = {
    var connection: IDatabaseConnection = null
    try {
      Class.forName("org.postgresql.Driver")
      extractInsertSequence(getIDatabaseConnection) 
    } finally {
      if (jdbcConnection != null) {
        try {
          jdbcConnection.close()
        } catch {
          case _: Throwable => {}
        }        
      }
      if (connection != null) {
        try {
          connection.close()
        } catch {
          case _: Throwable => {}
        }        
      }
    }
  }
 
  private def getIDatabaseConnection = {
    if (jdbcConnection == null) {
      jdbcConnection = DriverManager.getConnection(getJdbcURL, getUserName, getPassword)
    } else {
      if (jdbcConnection.isClosed()) {
        jdbcConnection = DriverManager.getConnection(getJdbcURL, getUserName, getPassword)
      }
    }
    val dbConnection = new DatabaseConnection(jdbcConnection, getSchemaName)
    val config = dbConnection.getConfig()
    config.setProperty(DatabaseConfig.PROPERTY_RESULTSET_TABLE_FACTORY, new CachedResultSetTableFactory())
    dbConnection
  }

  private def extractInsertSequence(connection: IDatabaseConnection) = {
    val insertSequenceFilter = new DatabaseSequenceFilter(connection)
    val field: Field = classOf[SequenceTableFilter].getDeclaredField("_tableNameMap")
    field.setAccessible(true)
    val map: OrderedTableNameMap = field.get(insertSequenceFilter).asInstanceOf[OrderedTableNameMap]
    val tableNames: Seq[String] = map.getTableNames()
    val tables: ArrayBuffer[TableDefinition] = ArrayBuffer[TableDefinition]()
    var definitionDataset: IDataSet = connection.createDataSet();
    for (tableName <- tableNames) {
      val columns = toColumns(tableName, definitionDataset.getTableMetaData(tableName));
      tables += new TableDefinition(tableName, columns);
    }
    InsertSequence(tables);
  }

  private def toColumns(tableName: String, metaData: ITableMetaData) = {
    val columns: ArrayBuffer[TableColumn] = ArrayBuffer[TableColumn]()
    val dbColumns = metaData.getColumns()
    for (i <- 0 to dbColumns.length-1) {
      val dbColumn = dbColumns(i)
      columns += new TableColumn(dbColumn.getColumnName(), dbColumn.getDataType().toString())
    }
    columns;
  }
  
}
