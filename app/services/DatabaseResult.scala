package services

abstract class DatabaseResult
case class SuccessUpdate[A](data: A) extends DatabaseResult
case class SuccessInsert(data: Long) extends DatabaseResult
case class UniqueConstraintViolation() extends DatabaseResult
case class ForeignKeyConstraintViolation() extends DatabaseResult
case class FatalDatabaseError() extends DatabaseResult
case class UnsuccessfulUpdate() extends DatabaseResult