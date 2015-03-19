package utils

import org.apache.commons.codec.digest.UnixCrypt

object PasswordCrypt {

  /**
   * Encrypt a String using UnixCrypt
   *
   * @param in
   * @return Encrypted String
   */
  def encrypt(in: String): Option[String] = {
    if (in == null || !validate(in)) {
      None
    }
    var salt = ""
    for (i <- 0 until in.length) {
      salt += in.charAt(i);
    }
    Some(UnixCrypt.crypt(salt, in))
  }

  private def validate(in: String) = {
    var ascii = in.replaceAll("\\P{ASCII}", "");
    if (in.length() - ascii.length() > 0) {
      false
    }
    true
  }

  /*
  def main(args: Array[String]) {
     println(encrypt("password").get)
  }
  */
}