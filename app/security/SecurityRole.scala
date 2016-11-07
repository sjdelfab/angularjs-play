package security

case class SecurityRole(val roleName: String) {
  def getName: String = roleName
}