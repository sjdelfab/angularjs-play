package controllers

import javax.inject.Singleton
import javax.inject.Inject
import play.api.Configuration
import org.apache.commons.codec.binary.Base64
import play.api.Play

trait IndirectReferenceMapper {
  
  def getExternalisedId(applicationRefId: String): Option[Long]
  
  def convertInternalIdToExternalised(internalId: Long): String
  
}

@Singleton
class CryptoIndirectReferenceMapper @Inject()(configuration: Configuration) extends IndirectReferenceMapper with security.Crypto {
  
  def getExternalisedId(applicationRefId: String): Option[Long] = {
    try {
      val decryptedValue = decrypt(applicationRefId.replaceAll("-","+").replaceAll("_","/"))
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
    
  private def getConfig(key: String) = configuration.getString(key)
  
  def transformation: String = getConfig("application.crypto.aes.transformation").getOrElse("AES")
  
  // TODO secret based on session key not global secret
  def secret: String = {
    configuration.getString("play.crypto.secret") match {      
      case Some(s) => s
      case _ => throw new RuntimeException("No application secret found")
    }
  }
}