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
 @Test fun rendersThemesAndConnectionStates() {
  val context=InstrumentationRegistry.getInstrumentation().targetContext
  ActivityScenario.launch<MainActivity>(Intent(context,MainActivity::class.java)).use { scenario ->
   for((name,dark,state)in listOf(Triple("light-idle",false,"idle"),Triple("dark-idle",true,"idle"),Triple("dark-connecting",true,"connecting"),Triple("dark-connected-preview",true,"connected"))) {
    scenario.onActivity { it.previewForTest(dark,state) }
    Thread.sleep(350)
    scenario.onActivity { activity ->
     val view=activity.window.decorView.rootView
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
