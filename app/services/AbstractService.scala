package services

import org.postgresql.util.PSQLException
import scala.util.Try
import scala.util.Failure
import scala.util.Success

trait AbstractService {

  def translateDatabaseUpdate(databaseOperation: Try[Int]):DatabaseResult = {
    databaseOperation match {
        case Success(rowsUpdated) => SuccessUpdate[Int](rowsUpdated)
        case Failure(t: PSQLException) => {
            val errorState = t.getServerErrorMessage().getSQLState()
            if (errorState == "23505") {
                UniqueConstraintViolation()    
            } else if (errorState == "23503") {
                ForeignKeyConstraintViolation()
            } else {
               FatalDatabaseError()
            }
        }
        case Failure(_) => {
            FatalDatabaseError()
        }
    }
  }
  
  def translateDatabaseInsert(databaseOperation: Try[Long]):DatabaseResult = {
    databaseOperation match {
        case Success(newId) => {
          SuccessInsert(newId)
        }
        case Failure(t: PSQLException) => {
            val errorState = t.getServerErrorMessage().getSQLState()
            if (errorState == "23505") {
                UniqueConstraintViolation()    
            } else {
               FatalDatabaseError()
            }
        }
        case Failure(_) => {
            FatalDatabaseError()
        }
    }
  }
  
  def translateDatabaseBatchInsert(databaseOperation: Try[Int], expectedRows: Int):DatabaseResult = {
    databaseOperation match {
      case Success(rowsInserted) => {
        if (rowsInserted != expectedRows) {
          UnsuccessfulUpdate()
        } else {
          SuccessUpdate(rowsInserted)
        }        
      }
      case Failure(t: PSQLException) => {
          val errorState = t.getServerErrorMessage().getSQLState()
          if (errorState == "23505") {
              UniqueConstraintViolation()    
          } else {
              FatalDatabaseError()
          }
      }
      case Failure(ex: java.sql.BatchUpdateException) => {
          val t:PSQLException = ex.getNextException.asInstanceOf[PSQLException]
          val errorState = t.getServerErrorMessage().getSQLState()
          if (errorState == "23505") {
              UniqueConstraintViolation()    
          } else {
              FatalDatabaseError()
          }
      }
      case Failure(_) => {
          FatalDatabaseError()
      }
    }
  }
  
  def translateDatabaseBatchInsertOption(databaseOperation: Try[Option[Int]], expectedRows: Int):DatabaseResult = {
    databaseOperation match {
      case Success(rowsInsertedOption) => {
        val rowsInserted = rowsInsertedOption.get
        if (rowsInserted != expectedRows) {
          UnsuccessfulUpdate()
        } else {
          SuccessUpdate(rowsInserted)
        }        
      }
      case Failure(t: PSQLException) => {
          val errorState = t.getServerErrorMessage().getSQLState()
          if (errorState == "23505") {
              UniqueConstraintViolation()    
          } else {
              FatalDatabaseError()
          }
      }
      case Failure(ex: java.sql.BatchUpdateException) => {
          val t:PSQLException = ex.getNextException.asInstanceOf[PSQLException]
          val errorState = t.getServerErrorMessage().getSQLState()
          if (errorState == "23505") {
              UniqueConstraintViolation()    
          } else {
              FatalDatabaseError()
          }
      }
      case Failure(_) => {
          FatalDatabaseError()
      }
    }
  }
  
}