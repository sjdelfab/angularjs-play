package security

class SecurityRole(val roleName: String) extends Role
{
  def getName: String = roleName
}

trait Role {
  
  def getName: String
  
}