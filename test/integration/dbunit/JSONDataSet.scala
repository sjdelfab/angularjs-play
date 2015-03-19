package integration.dbunit

import java.io.File
import java.io.InputStream
import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.JavaConversions.mapAsScalaMap
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.Buffer
import scala.collection.mutable.Map
import org.apache.commons.io.FileUtils
import org.dbunit.dataset.AbstractDataSet
import org.dbunit.dataset.Column
import org.dbunit.dataset.DefaultTable
import org.dbunit.dataset.DefaultTableMetaData
import org.dbunit.dataset.ITable
import org.dbunit.dataset.ITableIterator
import org.dbunit.dataset.ITableMetaData
import org.dbunit.dataset.datatype.DataType
import com.fasterxml.jackson.databind.ObjectMapper
import play.api.libs.json.Json
import java.io.FileInputStream

/**
 * DBUnit DataSet format for JSON based datasets. It is similar to the flat XML layout,
 * but has some improvements (columns are calculated by parsing the entire dataset, not just
 * the first row). It uses Jackson, a fast JSON processor.
 * <br/><br/>
 * The format looks like this:
 * <br/>
 * <pre>
 * {
 *    "&lt;table_name&gt;": [
 *        {
 *             "&lt;column&gt;":&lt;value&gt;,
 *             ...
 *        },
 *        ...
 *    ],
 *    ...
 * }
 * </pre>
 * <br/>
 * I.e.:
 * <br/>
 * <pre>
 * {
 *    "test_table": [
 *        {
 *             "id":1,
 *             "code":"JSON dataset",
 *        },
 *        {
 *             "id":2,
 *             "code":"Another row",
 *        }
 *    ],
 *    "another_table": [
 *        {
 *             "id":1,
 *             "description":"Foo",
 *        },
 *        {
 *             "id":2,
 *             "description":"Bar",
 *        }
 *    ],
 *    ...
 * }
 * </pre>
 *
 * @author Lieven DOCLO
 */
class JSONDataSet(datasetPath: String) extends AbstractDataSet {

  var tableParser: JSONITableParser = null
  
  private def initialise() {
     val insertSequenceOrderFile = new File("test/integration/insertSequence.json")
     val insertOrderAsString = FileUtils.readFileToString(insertSequenceOrderFile)
     val insertOrder = Json.fromJson[Seq[TableDefinition]](Json.parse(insertOrderAsString)).get
     tableParser = new JSONITableParser(insertOrder)
     tableParser.parse(new FileInputStream(datasetPath))     
  }
  
  initialise()
  
  override def createIterator(reverse: Boolean): ITableIterator  = {
        tableParser
  }
}

class JSONITableParser(insertOrder: Seq[TableDefinition]) extends ITableIterator {

  val tableMap = Map[String, ITable]()
  val tableNameToTableSchema = Map[String, TableDefinition]()
  var currentTable: String = null;
  var tableIndex = 0

  val mapper: ObjectMapper = new ObjectMapper();

  /**
   * Parses a JSON dataset input stream and returns the list of DBUnit tables contained in
   * that input stream
   * @param jsonStream A JSON dataset input stream
   * @return A list of DBUnit tables
   */
  def parse(jsonStream: InputStream) {
    // get the base object tree from the JSON stream
    val dataset = mapper.readValue(jsonStream, classOf[java.util.Map[String, java.util.List[java.util.Map[String, Object]]]]);
    import collection.JavaConversions._
    // iterate over the tables in the object tree
    for ((k, v) <- dataset) {
      // get the rows for the table
      val meta = getMetaData(k, v);
      // create a table based on the metadata
      val table = new DefaultTable(meta);
      var rowIndex = 0;
      // iterate through the rows and fill the table
      for (row <- v) {
        fillRow(table, row, rowIndex);
        rowIndex += 1
      }
      // add the table to the list of DBUnit tables
      tableMap += (k -> table);
    }
  }

  /**
   * Gets the table meta data based on the rows for a table
   * @param tableName The name of the table
   * @param rows The rows of the table
   * @return The table metadata for the table
   */
  def getMetaData(tableName: String, rows: java.util.List[java.util.Map[String, Object]]): ITableMetaData = {
    var uniqueColumnNames:Set[String]  = Set[String]()
    // iterate through the dataset and add the column names to a set
    import collection.JavaConversions._
    for (row <- rows) {
      for ((k, v) <- row) {
        uniqueColumnNames = uniqueColumnNames + k
      }
    }
    var columns: ArrayBuffer[Column] = ArrayBuffer[Column]();
    for(columnName <- uniqueColumnNames) {
      columns += new Column(columnName, DataType.UNKNOWN)
    }
    val finalColumns = new Array[Column](columns.size)
    columns.copyToArray(finalColumns)
    new DefaultTableMetaData(tableName, finalColumns);
  }

  /**
   * Fill a table row
   * @param table The table to be filled
   * @param row A map containing the column values
   * @param rowIndex The index of the row to te filled
   */
  private def fillRow(table: DefaultTable, row: Map[String, Object], rowIndex: Int) {
    table.addRow();
    // set the column values for the current row
    for ((k, v) <- row) {
      table.setValue(rowIndex, k, v);

    }
  }

  override def getTable(): ITable = {
    createTable(currentTable);
  }

  override def getTableMetaData(): ITableMetaData = {
    createTableMetaData(currentTable);
  }

  override def next(): Boolean = {
    if (tableIndex > insertOrder.size - 1) {
      return false;
    }
    currentTable = getNextTable();
    if (currentTable == null) {
      return false;
    }
    return true;
  }

  private def getNextTable(): String = {
    var tableName: String = null;
    do {
      if (tableIndex > insertOrder.size - 1) {
        return null;
      }
      tableName = insertOrder(tableIndex).name;
      tableIndex += 1;
    } while (!tableMap.contains(tableName));
    return tableName;
  }

  private def createTableMetaData(tableName: String): DefaultTableMetaData = {
    val definitionColumns = tableNameToTableSchema(tableName);
    val columns = Buffer[Column]();
    for (column <- definitionColumns.columns) {
      columns += new Column(column.name.trim(), toDataType(column.dataType));
    }
    val finalColumns = new Array[Column](columns.size)
    columns.copyToArray(finalColumns)
    new DefaultTableMetaData(tableName.trim(), finalColumns);
  }

  private def toDataType(name: String): DataType = {
    if ("VARCHAR".equals(name)) {
      DataType.VARCHAR;
    } else if ("CHAR".equals(name)) {
      DataType.CHAR;
    } else if ("LONGVARCHAR".equals(name)) {
      DataType.LONGVARCHAR;
    } else if ("CLOB".equals(name)) {
      DataType.CLOB;
    } else if ("NUMERIC".equals(name)) {
      DataType.NUMERIC;
    } else if ("DECIMAL".equals(name)) {
      DataType.DECIMAL;
    } else if ("BOOLEAN".equals(name)) {
      DataType.BOOLEAN;
    } else if ("BIT".equals(name)) {
      DataType.BIT;
    } else if ("INTEGER".equals(name)) {
      DataType.INTEGER;
    } else if ("TINYINT".equals(name)) {
      DataType.TINYINT;
    } else if ("SMALLINT".equals(name)) {
      DataType.SMALLINT;
    } else if ("DOUBLE".equals(name)) {
      DataType.DOUBLE;
    } else if ("BIGINT".equals(name)) {
      DataType.BIGINT;
    } else if ("FLOAT".equals(name)) {
      DataType.FLOAT;
    } else if ("DATE".equals(name)) {
      DataType.DATE;
    } else if ("TIME".equals(name)) {
      DataType.TIME;
    } else if ("TIMESTAMP".equals(name)) {
      DataType.TIMESTAMP;
    } else if ("VARBINARY".equals(name)) {
      DataType.VARBINARY;
    } else if ("BINARY".equals(name)) {
      DataType.BINARY;
    } else if ("LONGVARBINARY".equals(name)) {
      DataType.LONGVARBINARY;
    } else if ("BLOB".equals(name)) {
      DataType.BLOB;
    } else {
      DataType.UNKNOWN;
    }

  }

  private def createTable(tableName: String): ITable = {
    tableMap(tableName);
  }
}