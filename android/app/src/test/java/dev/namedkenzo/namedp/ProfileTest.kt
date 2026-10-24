package dev.namedkenzo.namedp
import org.junit.Assert.*
import org.junit.Test
import java.util.Base64

class ProfileTest {
 @Test fun acceptsEachCredentialMode() {
  Profile.validate("pingtunnel://example.com?key=123&mode=proxy")
  for(mode in listOf("aes128","aes256","chacha20")) {
   val key=Base64.getEncoder().encodeToString(ByteArray(if(mode=="aes128")16 else 32))
   Profile.validate("pingtunnel://example.com?encrypt=$mode&encrypt_key=$key")
  }
 }
 @Test fun rejectsTruncatedAndCombinedCredentials() {
  for(link in listOf("pingtunnel://example.com?encrypt=aes256","pingtunnel://example.com?key=12&key=13","pingtunnel://example.com?key=12&encrypt=aes256","pingtunnel://example.com?key=12&lport=0")) {
   try { Profile.validate(link);fail("Accepted invalid link") } catch(_:IllegalArgumentException) { }
  }
 }
}
