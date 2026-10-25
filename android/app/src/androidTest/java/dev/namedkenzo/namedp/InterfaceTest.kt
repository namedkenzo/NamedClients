package dev.namedkenzo.namedp

import android.content.Intent
import android.content.ClipData
import android.content.ClipboardManager
import android.app.AlertDialog
import android.view.View
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.graphics.Bitmap
import android.graphics.Canvas
import android.content.ContentValues
import android.provider.MediaStore
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*
import java.io.File

@RunWith(AndroidJUnit4::class)
class InterfaceTest {
 @Test fun releaseUpgradePreservesEncryptedProfiles(){
  val phase=InstrumentationRegistry.getArguments().getString("upgrade")
  org.junit.Assume.assumeTrue(phase=="prepare" || phase=="verify")
  val context=InstrumentationRegistry.getInstrumentation().targetContext
  val store=ProfileStore(context)
  val link="pingtunnel://upgrade.example?lport=1080&mode=proxy&key=123456"
  if(phase=="prepare")store.save(mutableListOf(Profile("upgrade-fixture","Saved across upgrade",link)))
  else{
   val profile=store.read().single();assertEquals("upgrade-fixture",profile.id);assertEquals(link,profile.link)
   assertEquals(0,context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE)
   store.save(mutableListOf())
  }
 }
 @Test fun polishedNavigationKeepsConnectVisibleAndSupportsPersian(){
  val context=InstrumentationRegistry.getInstrumentation().targetContext
  val prefs=context.getSharedPreferences("namedp-ui",0);prefs.edit().clear().putString("language","fa").commit()
  TunnelStatus.snapshot=TunnelSnapshot()
  ActivityScenario.launch<MainActivity>(Intent(context,MainActivity::class.java)).use{scenario->
   InstrumentationRegistry.getInstrumentation().waitForIdleSync()
   scenario.onActivity{activity->
    val view=activity.window.decorView
    val connect=view.findViewWithTag<View>("connect")
    val rect=android.graphics.Rect();assertTrue("Primary action must stay visible without scrolling",connect.getGlobalVisibleRect(rect));assertTrue(rect.height()>0)
    assertEquals(View.LAYOUT_DIRECTION_RTL,connect.layoutDirection)
    assertNotNull(view.findViewWithTag<View>("connection-hero"))
    val hero=view.findViewWithTag<View>("connection-hero")
    assertTrue("Hero must not expand to a tall, empty row",hero.height < 400*activity.resources.displayMetrics.density)
    assertTrue("Companion must be visible without scrolling",view.findViewWithTag<View>("companion-art").getGlobalVisibleRect(android.graphics.Rect()))
    assertNotNull(view.findViewWithTag<View>("connection-status"))
    view.findViewWithTag<View>("tab-profiles").performClick();assertNotNull(view.findViewWithTag<View>("paste-profile"))
    view.findViewWithTag<View>("tab-settings").performClick();assertNotNull(view.findViewWithTag<View>("gaming-mode"))
   }
  };prefs.edit().clear().commit()
 }
 private fun screenshot(activity:MainActivity,name:String){
  val view=activity.window.decorView.rootView
  val bitmap=Bitmap.createBitmap(view.width,view.height,Bitmap.Config.ARGB_8888);view.draw(Canvas(bitmap))
  val values=ContentValues().apply{put(MediaStore.Images.Media.DISPLAY_NAME,"preview-"+name+".png");put(MediaStore.Images.Media.MIME_TYPE,"image/png");put(MediaStore.Images.Media.RELATIVE_PATH,"Pictures/namedP-tests");put(MediaStore.Images.Media.IS_PENDING,1)}
  val uri=activity.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values)?:error("No screenshot URI")
  activity.contentResolver.openOutputStream(uri)!!.use{bitmap.compress(Bitmap.CompressFormat.PNG,100,it)}
  activity.contentResolver.update(uri,ContentValues().apply{put(MediaStore.Images.Media.IS_PENDING,0)},null,null);bitmap.recycle()
 }
 @Test fun pasteImportsWholeLinkWithoutEditorOrRequiredName() {
  val context=InstrumentationRegistry.getInstrumentation().targetContext
  context.getSharedPreferences("namedP-private",0).edit().clear().commit()
  context.getSharedPreferences("namedp-ui",0).edit().clear().commit()
  ActivityScenario.launch<MainActivity>(Intent(context,MainActivity::class.java)).use { scenario ->
   scenario.onActivity { activity ->
    val clipboard=activity.getSystemService(ClipboardManager::class.java)
    clipboard.setPrimaryClip(ClipData.newPlainText("Test profile","pingtunnel://example.com?mode=proxy&key=123456&lport=1081"))
    activity.window.decorView.findViewWithTag<View>("paste-profile").performClick()
    val saved=ProfileStore(activity).read().single()
    assertEquals("example.com",saved.name);assertEquals("proxy",saved.mode);assertEquals(1081,saved.port)
    assertEquals("idle",TunnelStatus.snapshot.state) // Import never fakes a connection.
    activity.window.decorView.findViewWithTag<View>("paste-profile").performClick()
    assertEquals(1,ProfileStore(activity).read().size)
    val key=java.util.Base64.getEncoder().encodeToString(ByteArray(32){0xfb.toByte()})
    clipboard.setPrimaryClip(ClipData.newPlainText("Test encrypted profile","pingtunnel://example.com?mode=vpn&encrypt_mode=chacha20&encrypt-key=$key"))
    activity.window.decorView.findViewWithTag<View>("paste-profile").performClick()
    val encrypted=ProfileStore(activity).read().last().link
    assertTrue(encrypted.contains("&encrypt=chacha20&encrypt_key="));assertTrue(encrypted.contains("%2B"));assertFalse(encrypted.contains("&key="))
    clipboard.clearPrimaryClip()
   }
   scenario.recreate()
   scenario.onActivity {assertEquals(2,ProfileStore(it).read().size)}
  }
  context.getSharedPreferences("namedP-private",0).edit().clear().commit()
  context.getSharedPreferences("namedp-ui",0).edit().clear().commit()
 }
 @Test fun plusOffersSeparateServerAndCredentialFields() {
  val context=InstrumentationRegistry.getInstrumentation().targetContext
  context.getSharedPreferences("namedP-private",0).edit().clear().commit()
  context.getSharedPreferences("namedp-ui",0).edit().clear().commit()
  ActivityScenario.launch<MainActivity>(Intent(context,MainActivity::class.java)).use { scenario ->
   scenario.onActivity { it.window.decorView.findViewWithTag<View>("manual-profile").performClick() }
   InstrumentationRegistry.getInstrumentation().waitForIdleSync()
   scenario.onActivity { activity ->
    val view=activity.window.decorView
    val credential=view.findViewWithTag<EditText>("manual-credential")
    assertTrue("Keys must be masked before explicit reveal",credential.transformationMethod is android.text.method.PasswordTransformationMethod)
    view.findViewWithTag<View>("reveal-key").performClick();assertNull(credential.transformationMethod)
    view.findViewWithTag<View>("reveal-key").performClick();assertTrue(credential.transformationMethod is android.text.method.PasswordTransformationMethod)
    view.findViewWithTag<EditText>("manual-server").setText("example.com")
    view.findViewWithTag<EditText>("manual-credential").setText("123456")
    view.findViewWithTag<View>("manual-save").performClick()
    val saved=ProfileStore(activity).read()
    assertTrue("Manual save failed: "+view.findViewWithTag<TextView>("manual-error").text,saved.isNotEmpty())
    assertEquals("example.com",saved.single().name)
    assertEquals("vpn",saved.single().mode)
    view.findViewWithTag<View>("detail-back").performClick()
    activity.window.decorView.findViewWithTag<View>("manual-profile").performClick()
   }
   InstrumentationRegistry.getInstrumentation().waitForIdleSync()
   scenario.onActivity { it.window.decorView.findViewWithTag<Spinner>("manual-auth").setSelection(2) }
   InstrumentationRegistry.getInstrumentation().waitForIdleSync()
   scenario.onActivity { activity ->
    val view=activity.window.decorView
    view.findViewWithTag<EditText>("manual-server").setText("example.com")
    view.findViewWithTag<EditText>("manual-credential").setText(java.util.Base64.getEncoder().encodeToString(ByteArray(32){0xfb.toByte()}))
    view.findViewWithTag<View>("manual-save").performClick()
    val saved=ProfileStore(activity).read()
    assertEquals("Encrypted save failed: "+view.findViewWithTag<TextView>("manual-error").text,2,saved.size)
    assertTrue(saved.last().link.contains("&encrypt=aes256&encrypt_key="));assertFalse(saved.last().link.contains("&key="))
   }
  }
  context.getSharedPreferences("namedP-private",0).edit().clear().commit()
  context.getSharedPreferences("namedp-ui",0).edit().clear().commit()
 }
 @Test fun importedModeSurvivesRestartAndDetailsEditUpdatesSameProfile() {
  val context=InstrumentationRegistry.getInstrumentation().targetContext
  context.getSharedPreferences("namedP-private",0).edit().clear().commit()
  context.getSharedPreferences("namedp-ui",0).edit().clear().commit()
  ActivityScenario.launch<MainActivity>(Intent(context,MainActivity::class.java)).use { scenario ->
   scenario.onActivity { activity ->
    activity.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("Demo","pingtunnel://example.com?mode=proxy&key=123456"))
    activity.window.decorView.findViewWithTag<View>("paste-profile").performClick()
    activity.getSharedPreferences("namedp-ui",0).edit().putString("mode","vpn").apply() // Legacy global toggle must not override the URI.
   }
   scenario.recreate()
   scenario.onActivity { it.window.decorView.findViewWithTag<View>("profile-card").performClick() }
   InstrumentationRegistry.getInstrumentation().waitForIdleSync()
   var id=""
   scenario.onActivity { activity ->
    val view=activity.window.decorView
    id=ProfileStore(activity).read().single().id
    assertEquals(0,view.findViewWithTag<Spinner>("manual-mode").selectedItemPosition)
    assertNotNull(view.findViewWithTag<View>("test-profile"))
    view.findViewWithTag<Spinner>("manual-mode").setSelection(1)
   }
   InstrumentationRegistry.getInstrumentation().waitForIdleSync()
   scenario.onActivity { activity ->
    activity.window.decorView.findViewWithTag<View>("manual-save").performClick()
    val saved=ProfileStore(activity).read().single()
    assertEquals(id,saved.id);assertEquals("vpn",saved.mode);assertTrue(saved.link.contains("&key=123456"))
    activity.window.decorView.findViewWithTag<View>("copy-profile").performClick()
    assertEquals(saved.link,activity.getSystemService(ClipboardManager::class.java).primaryClip!!.getItemAt(0).text.toString())
    activity.getSystemService(ClipboardManager::class.java).clearPrimaryClip()
   }
   InstrumentationRegistry.getInstrumentation().waitForIdleSync()
   scenario.onActivity { screenshot(it,"details") }
  }
  context.getSharedPreferences("namedP-private",0).edit().clear().commit()
  context.getSharedPreferences("namedp-ui",0).edit().clear().commit()
 }
 @Test fun nativeEngineOpensRootlessPingSocket() {
  val context=InstrumentationRegistry.getInstrumentation().targetContext
  val process=ProcessBuilder(File(context.applicationInfo.nativeLibraryDir,"libnamedp.so").absolutePath,"--self-test-socket").redirectErrorStream(true).start()
  assertTrue("Native socket self-test timeout",process.waitFor(8,java.util.concurrent.TimeUnit.SECONDS))
  val output=process.inputStream.bufferedReader().readText()
  assertEquals(output,0,process.exitValue())
  assertTrue(output,output.contains("ping-socket-ready"))
 }
 @Test fun nativeEngineOpensRootlessIPv6PingSocket() {
  val context=InstrumentationRegistry.getInstrumentation().targetContext
  val process=ProcessBuilder(File(context.applicationInfo.nativeLibraryDir,"libnamedp.so").absolutePath,"--self-test-socket6").redirectErrorStream(true).start()
  assertTrue(process.waitFor(8,java.util.concurrent.TimeUnit.SECONDS))
  val output=process.inputStream.bufferedReader().readText()
  assertEquals(output,0,process.exitValue());assertTrue(output,output.contains("ping-socket-ready"))
 }
 @Test fun ipv6ProfileKeepsItsFamilyWhenEditedAndCopied() {
  val context=InstrumentationRegistry.getInstrumentation().targetContext
  context.getSharedPreferences("namedP-private",0).edit().clear().commit()
  context.getSharedPreferences("namedp-ui",0).edit().clear().commit()
  ActivityScenario.launch<MainActivity>(Intent(context,MainActivity::class.java)).use{scenario->
   scenario.onActivity{activity->
    activity.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("IPv6 example","pingtunnel://[2001:db8::25]?key=123456&mode=vpn&family=ipv6"))
    activity.window.decorView.findViewWithTag<View>("paste-profile").performClick()
    activity.window.decorView.findViewWithTag<View>("profile-card").performClick()
   }
   InstrumentationRegistry.getInstrumentation().waitForIdleSync()
   scenario.onActivity{activity->
    val view=activity.window.decorView
    assertEquals(1,view.findViewWithTag<Spinner>("manual-family").selectedItemPosition)
    assertEquals("2001:db8::25",view.findViewWithTag<EditText>("manual-server").text.toString())
    view.findViewWithTag<View>("manual-save").performClick()
    val saved=ProfileStore(activity).read().single();assertEquals("ipv6",saved.parameters()["family"])
    assertTrue(saved.link.startsWith("pingtunnel://[2001:db8::25]?"))
    view.findViewWithTag<View>("copy-profile").performClick()
    assertEquals(saved.link,activity.getSystemService(ClipboardManager::class.java).primaryClip!!.getItemAt(0).text.toString())
    activity.getSystemService(ClipboardManager::class.java).clearPrimaryClip()
   }
  }
  context.getSharedPreferences("namedP-private",0).edit().clear().commit()
  context.getSharedPreferences("namedp-ui",0).edit().clear().commit()
 }
 @Test fun gamingPolicyIsOptInPersistentAndLockedWhileConnected() {
  val context=InstrumentationRegistry.getInstrumentation().targetContext
  val prefs=context.getSharedPreferences("namedp-ui",0);prefs.edit().clear().commit();TunnelStatus.snapshot=TunnelSnapshot()
  ActivityScenario.launch<MainActivity>(Intent(context,MainActivity::class.java)).use{scenario->
   scenario.onActivity{activity->
    activity.window.decorView.findViewWithTag<View>("tab-settings").performClick()
    val toggle=activity.window.decorView.findViewWithTag<android.widget.Switch>("gaming-mode")
    assertFalse(toggle.isChecked);toggle.performClick();assertTrue(prefs.getBoolean("gaming",false))
   }
   scenario.recreate()
   scenario.onActivity{activity->
    TunnelStatus.snapshot=TunnelSnapshot("connected","Test fixture")
    activity.window.decorView.findViewWithTag<View>("tab-settings").performClick()
    val toggle=activity.window.decorView.findViewWithTag<android.widget.Switch>("gaming-mode")
    assertTrue(toggle.isChecked);assertFalse(toggle.isEnabled)
   }
  }
  TunnelStatus.snapshot=TunnelSnapshot();prefs.edit().clear().commit()
 }
 @Test fun nativeRelayCanReadAndroidSystemTLSRoots() {
  val context=InstrumentationRegistry.getInstrumentation().targetContext
  val process=ProcessBuilder(File(context.applicationInfo.nativeLibraryDir,"libnamedp.so").absolutePath,"--self-test-relay-roots").redirectErrorStream(true).start()
  assertTrue(process.waitFor(8,java.util.concurrent.TimeUnit.SECONDS))
  val output=process.inputStream.bufferedReader().readText()
  assertEquals(output,0,process.exitValue());assertTrue(output,output.contains("relay-roots-ready"))
 }
 @Test fun relayDelayIsVisibleAndCanBeTestedWithoutStoppingConnectedProfile() {
  val context=InstrumentationRegistry.getInstrumentation().targetContext
  context.getSharedPreferences("namedP-private",0).edit().clear().commit()
  context.getSharedPreferences("namedp-ui",0).edit().putString("language","en").commit()
  TunnelStatus.snapshot=TunnelSnapshot()
  ActivityScenario.launch<MainActivity>(Intent(context,MainActivity::class.java)).use{scenario->
   scenario.onActivity{activity->
    activity.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("Test","pingtunnel://example.com?key=123456"))
    activity.window.decorView.findViewWithTag<View>("paste-profile").performClick()
    assertTrue(activity.window.decorView.findViewWithTag<TextView>("home-relay-delay").text.toString().contains("not tested"))
    assertTrue(activity.window.decorView.findViewWithTag<View>("home-test-delay").isEnabled)
    TunnelStatus.snapshot=TunnelSnapshot("connected","Test-only fixture")
    activity.window.decorView.findViewWithTag<View>("profile-card").performClick()
   }
   InstrumentationRegistry.getInstrumentation().waitForIdleSync()
   scenario.onActivity{activity->
    val view=activity.window.decorView
    assertTrue("Relay test must not require disconnect",view.findViewWithTag<View>("test-profile").isEnabled)
    assertFalse(view.findViewWithTag<View>("manual-save").isEnabled)
    assertTrue(view.findViewWithTag<TextView>("diagnostic-status").text.toString().contains("TCP + TLS + HTTP"))
    assertEquals("connected",TunnelStatus.snapshot.state)
    activity.getSystemService(ClipboardManager::class.java).clearPrimaryClip()
   }
  }
  TunnelStatus.snapshot=TunnelSnapshot()
  context.getSharedPreferences("namedP-private",0).edit().clear().commit()
  context.getSharedPreferences("namedp-ui",0).edit().clear().commit()
 }
 @Test fun rendersThemesAndConnectionStates() {
  val context=InstrumentationRegistry.getInstrumentation().targetContext
  ActivityScenario.launch<MainActivity>(Intent(context,MainActivity::class.java)).use { scenario ->
   for((name,dark,state)in listOf(Triple("light-idle",false,"idle"),Triple("dark-idle",true,"idle"),Triple("dark-connecting",true,"connecting"),Triple("dark-connected-preview",true,"connected"))) {
    scenario.onActivity { it.previewForTest(dark,state) }
    Thread.sleep(350)
    scenario.onActivity { activity ->
     val view=activity.window.decorView.rootView
     assertTrue("Preview must show the companion",view.findViewWithTag<View>("companion-art").getGlobalVisibleRect(android.graphics.Rect()))
     assertTrue("Preview hero must stay compact",view.findViewWithTag<View>("connection-hero").height < 400*activity.resources.displayMetrics.density)
     assertTrue("Window must be laid out",view.width>0 && view.height>0)
     val bitmap=Bitmap.createBitmap(view.width,view.height,Bitmap.Config.ARGB_8888)
     view.draw(Canvas(bitmap))
     val values=ContentValues().apply {
      put(MediaStore.Images.Media.DISPLAY_NAME,"preview-$name.png")
      put(MediaStore.Images.Media.MIME_TYPE,"image/png")
      put(MediaStore.Images.Media.RELATIVE_PATH,"Pictures/namedP-tests")
      put(MediaStore.Images.Media.IS_PENDING,1)
     }
     val uri=context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values) ?: error("Screenshot output unavailable")
     context.contentResolver.openOutputStream(uri)!!.use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) }
     context.contentResolver.update(uri,ContentValues().apply {put(MediaStore.Images.Media.IS_PENDING,0)},null,null)
     bitmap.recycle()
    }
   }
  }
 }
}
