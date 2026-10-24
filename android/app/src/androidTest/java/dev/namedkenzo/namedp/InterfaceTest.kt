package dev.namedkenzo.namedp

import android.content.Intent
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
