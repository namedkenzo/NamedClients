package dev.namedkenzo.namedp

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*
import java.io.File

@RunWith(AndroidJUnit4::class)
class InterfaceTest {
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
     File(context.filesDir,"preview-$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) }
     bitmap.recycle()
    }
   }
  }
 }
}
