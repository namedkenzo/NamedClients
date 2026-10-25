package dev.namedkenzo.namedp
import org.junit.Assert.*
import org.junit.Test
import java.util.Base64

class ProfileTest {
 private fun failure(link:String):ProfileInputException {
  try { Profile.normalize(link);fail("Accepted invalid link") } catch(e:ProfileInputException) {return e}
  error("Unreachable")
 }
 @Test fun preservesBothLiteralAndEncodedBase64Plus() {
  val secret=Base64.getEncoder().encodeToString(ByteArray(32){0xfb.toByte()})
  assertTrue(secret.contains('+'));assertTrue(secret.contains('/'))
  val raw=Profile.normalize("pingtunnel://example.com?encrypt=chacha20&encrypt_key=$secret")
  val escaped=Profile.normalize("pingtunnel://example.com?encrypt=chacha20&encrypt_key="+java.net.URLEncoder.encode(secret,"UTF-8"))
  assertEquals(raw,escaped);assertTrue(raw.contains("%2B"));assertEquals(raw,Profile.normalize(raw))
 }
 @Test fun acceptsUpstreamEncryptionAliasesAndCanonicalizesForNativeEngine() {
  val secret=Base64.getEncoder().encodeToString(ByteArray(16){0xfb.toByte()})
  for(method in listOf("encrypt","encrypt_mode","encryptMode","encrypt-mode","enc")) {
   for(key in listOf("encrypt_key","encrypt-key","encryptKey")) {
    val canonical=Profile.normalize("pingtunnel://example.com?local_port=1081&vpn=1&$method=aes128&$key=$secret")
    assertTrue(canonical.startsWith("pingtunnel://example.com?lport=1081&mode=vpn&encrypt=aes128&encrypt_key="))
    assertEquals(canonical,Profile.normalize(canonical))
   }
  }
 }
 @Test fun manualNumericProfileMatchesPanelLinkWithoutRequiringName() {
  val link=Profile.manualLink(" example.com ","۱۰۸۰","vpn","none","۱۲۳۴۵۶")
  assertEquals("pingtunnel://example.com?lport=1080&mode=vpn&key=123456",link)
  assertEquals(link,Profile.normalize("\uFEFF pingtunnel://example.com?key=123456 "))
 }
 @Test fun manualEncryptedProfilesContainNoNumericKey() {
  for(method in listOf("aes128","aes256","chacha20")) {
   val secret=Base64.getEncoder().encodeToString(ByteArray(if(method=="aes128")16 else 32){0xff.toByte()})
   val link=Profile.manualLink("example.com","1080","proxy",method,secret)
   assertTrue(link.contains("&encrypt=$method&encrypt_key="));assertFalse(link.contains("&key="))
   assertEquals("proxy",Profile("id","name",link).mode)
  }
 }
 @Test fun missingKeyHasActionableMessageAndDoesNotInventCredentials() {
  val e=failure("pingtunnel://example.com?lport=1080&mode=vpn")
  assertEquals("credential",e.field);assertTrue(e.message!!.contains("has no key"));assertFalse(e.message!!.contains("example.com"))
  assertEquals("credential",failure("pingtunnel://example.com?encrypt_key=private").field)
 }
 @Test fun refusesAmbiguousAliasesAndMalformedLinks() {
  for(link in listOf("pingtunnel://example.com?key=1&local_port=1080&lport=1081","pingtunnel://example.com?encrypt=aes128&enc=aes256","pingtunnel://example.com?key=0","pingtunnel://example.com?key=2147483648","pingtunnel://example.com?key=123#fragment","pingtunnel://example.com?key=%ZZ","pingtunnel://example.com?key=123&mode=other"))failure(link)
 }
 @Test fun modeIsOwnedByTheProfileAndPerAppOnlyChangesLocalRouting() {
  for(mode in listOf("vpn","proxy","proxy_per_app")) {
   val link=Profile.manualLink("example.com","1080",mode,"none","123",listOf("org.example.browser","org.example.chat"))
   val p=Profile("id","name",link)
   assertEquals(mode,p.mode)
   assertEquals(if(mode=="proxy")"proxy"else"vpn",Profile("id","name",p.engineLink()).mode)
   assertFalse(p.engineLink().contains("apps="))
   if(mode=="proxy_per_app")assertEquals(2,p.apps.size)else assertTrue(p.apps.isEmpty())
  }
 }
 @Test fun detailsCanRoundTripEachCredentialWithoutChangingTheKey() {
  val secret=Base64.getEncoder().encodeToString(ByteArray(32){0xfb.toByte()})
  val link=Profile.normalize("pingtunnel://example.com?mode=vpn&encrypt=aes256&encrypt_key="+java.net.URLEncoder.encode(secret,"UTF-8"))
  val profile=Profile("id","name",link);val q=profile.parameters()
  assertEquals(link,Profile.manualLink(profile.host,profile.port.toString(),profile.mode,q["encrypt"]!!,q["encrypt_key"]!!))
  failure("pingtunnel://example.com?key=123&mode=proxy_per_app&apps=bad/package")
 }
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
