package security

import javax.crypto._
import javax.crypto.spec.SecretKeySpec
import org.apache.commons.codec.binary.Base64
import play.api.libs.Codecs

trait Crypto {
    
  def encryptAES(value: String): String = {
    encryptAES(value, secret.substring(0, 16))
  }
  
  def encryptAES(value: String, privateKey: String): String = {
    val raw = privateKey.getBytes("utf-8")
    val skeySpec = new SecretKeySpec(raw, "AES")
    val cipher = Cipher.getInstance(transformation)
    cipher.init(Cipher.ENCRYPT_MODE, skeySpec)
    Codecs.toHexString(cipher.doFinal(value.getBytes("utf-8")))
  }
  
  def decryptAES(value: String): String = {
    decryptAES(value, secret.substring(0, 16))
  }
  
  def decryptAES(value: String, privateKey: String): String = {
    val raw = privateKey.getBytes("utf-8")
    val skeySpec = new SecretKeySpec(raw, "AES")
    val cipher = Cipher.getInstance(transformation)
    cipher.init(Cipher.DECRYPT_MODE, skeySpec)
    new String(cipher.doFinal(Codecs.hexStringToByte(value)))
  }
    
  def transformation: String
  
  def secret: String
}