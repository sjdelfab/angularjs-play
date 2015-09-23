package controllers

import play.api.Play.current
import javax.crypto._
import javax.crypto.spec.SecretKeySpec
import org.apache.commons.codec.binary.Base64
import play.api.Play
import play.api.libs.Codecs

object IndirectReferenceMapper {
  
  def getExternalisedId(applicationRefId: String): Option[Long] = {
    try {
      var decryptedValue = decrypt(applicationRefId.replaceAll("-","+").replaceAll("_","/"))
      Some(java.lang.Long.parseLong(decryptedValue))
    } catch {
      case ex: Throwable =>  {
        //ex.printStackTrace()
        None      
      }
    }
  }
  
  def convertInternalIdToExternalised(internalId: Long): String = {
    val id = encrypt(internalId.toString)
    id.replaceAll("\\+", "-").replaceAll("/","_")
  }
 
  def encrypt(internalId: String): String = {
      new String(Base64.encodeBase64(encryptAES(internalId).getBytes("UTF-8")),"UTF-8")
  }
  
  def decrypt(externalId: String): String = {
    new String(decryptAES(new String(Base64.decodeBase64(externalId.getBytes("UTF-8")))));
  }
  
  // Copied from
  // https://github.com/playframework/playframework/blob/2.3.6/framework/src/play/src/main/scala/play/api/libs/Crypto.scala#L187-L277
  private def encryptAES(value: String): String = {
    encryptAES(value, secret.substring(0, 16))
  }
  
  private def encryptAES(value: String, privateKey: String): String = {
    val raw = privateKey.getBytes("utf-8")
    val skeySpec = new SecretKeySpec(raw, "AES")
    val cipher = provider.map(p => Cipher.getInstance(transformation, p)).getOrElse(Cipher.getInstance(transformation))
    cipher.init(Cipher.ENCRYPT_MODE, skeySpec)
    Codecs.toHexString(cipher.doFinal(value.getBytes("utf-8")))
  }
  
  private def decryptAES(value: String): String = {
    decryptAES(value, secret.substring(0, 16))
  }
  
  private def decryptAES(value: String, privateKey: String): String = {
    val raw = privateKey.getBytes("utf-8")
    val skeySpec = new SecretKeySpec(raw, "AES")
    val cipher = provider.map(p => Cipher.getInstance(transformation, p)).getOrElse(Cipher.getInstance(transformation))
    cipher.init(Cipher.DECRYPT_MODE, skeySpec)
    new String(cipher.doFinal(Codecs.hexStringToByte(value)))
  }
  
  private def getConfig(key: String) = Play.maybeApplication.flatMap(_.configuration.getString(key))
  
  private lazy val provider: Option[String] = getConfig("application.crypto.provider")

  private lazy val transformation: String = getConfig("application.crypto.aes.transformation").getOrElse("AES")
  
  private def secret: String = {
    val app = Play.current
    app.configuration.getString("play.crypto.secret") match {      
      case Some(s) => s
      case _ => throw new RuntimeException("No application secret found")
    }
  }
}