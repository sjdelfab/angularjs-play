package integration.dbunit

import play.api.libs.json._
import play.api.libs.functional.syntax._


case class InsertSequence(tables: Seq[TableDefinition])

case class TableDefinition(name: String, columns: Seq[TableColumn])

object TableDefinition extends ((String,Seq[TableColumn]) => TableDefinition) {
  
  implicit val TableDefinitionToJson: Writes[TableDefinition] = (
      (__ \ "name").write[String] ~
      (__ \ "columns").write[Seq[TableColumn]]
  )((table: TableDefinition) => ( 
       table.name,
       table.columns
  ))
  
  implicit val JsonToTableDefinition: Reads[TableDefinition] = (
     (__ \ "name").read[String] ~
     (__ \ "columns").read[Seq[TableColumn]]
  )((name,columns) => TableDefinition(name,columns))
  
}

case class TableColumn(name: String, dataType: String)

object TableColumn extends ((String,String) => TableColumn) {
  
  import play.api.libs.json._
  import play.api.libs.functional.syntax._
  
  implicit val ColumnToJson: Writes[TableColumn] = Writes {
    (column: TableColumn) => Json.obj("name" -> column.name, "dataType" -> column.dataType)
  }
  
  implicit val JsonToColumn: Reads[TableColumn] = (
      (__ \ "name").read[String] ~
      (__ \ "dataType").read[String]
  )((name,dataType) => TableColumn(name,dataType))
}