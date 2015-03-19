package security

trait Subject {

  def getRoles: Seq[Role]
  def getIdentifier: String
  
}