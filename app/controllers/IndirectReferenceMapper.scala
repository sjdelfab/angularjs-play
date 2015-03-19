package controllers

import play.api.Play.current
import play.api.libs.Crypto
import javax.crypto.spec.SecretKeySpec
import org.apache.commons.codec.binary.Base64

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
      new String(Base64.encodeBase64(Crypto.encryptAES(internalId).getBytes("UTF-8")),"UTF-8")
  }
  
  def decrypt(externalId: String): String = {
     new String(Crypto.decryptAES(new String(Base64.decodeBase64(externalId.getBytes("UTF-8")))));
  }
}