package dev.namedkenzo.namedp

import android.Manifest
import android.app.*
import android.content.*
import android.content.res.Configuration
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.net.Uri
import android.net.VpnService
import android.os.*
import android.text.InputType
import android.view.*
import android.widget.*
import java.util.Locale
import java.util.UUID

class MainActivity:Activity() {
    private val handler=Handler(Looper.getMainLooper())
    private val prefs by lazy { getSharedPreferences("namedp-ui",MODE_PRIVATE) }
    private val store by lazy { ProfileStore(this) }
    private var profiles= mutableListOf<Profile>()
    private var locked=false
    private var page="home"
    private var mode="vpn"
    private var lastState=""
    private var forceNight:Boolean?=null
    private var preview:TunnelSnapshot?=null
    private lateinit var root:LinearLayout
    private var traffic:TextView?=null
    private var elapsed:TextView?=null
    private var bg=0;private var surface=0;private var ink=0;private var muted=0;private var accent=0;private var soft=0
    private var night=false
    private val fa get()=prefs.getString("language",Locale.getDefault().language)=="fa"
    private fun tr(en:String,faText:String)=if(fa)faText else en
    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
    private fun snapshot()=preview ?: TunnelStatus.snapshot
    private fun active()=snapshot().state in setOf("connecting","connected")
    private fun selected()=profiles.find { it.id==prefs.getString("selected",null) } ?: profiles.firstOrNull()
    private val tick=object:Runnable {override fun run(){
        val state=snapshot();if(state.state!=lastState){lastState=state.state;render()}
        traffic?.text="↑ ${bytes(state.upload)}     ↓ ${bytes(state.download)}"
        elapsed?.text=if(state.since>0){val seconds=(SystemClock.elapsedRealtime()-state.since)/1000;String.format(Locale.US,"%02d:%02d",seconds/60,seconds%60)}else "00:00"
        handler.postDelayed(this,750)
    }}
    override fun onCreate(savedInstanceState:Bundle?){
        super.onCreate(savedInstanceState)
        mode=prefs.getString("mode","vpn") ?: "vpn"
        try{profiles=store.read()}catch(_:Exception){locked=true;TunnelStatus.snapshot=TunnelSnapshot("error", "Saved profiles could not be unlocked. Clear app data only if you have backup links.")}
        render()
    }
    override fun onResume(){super.onResume();handler.post(tick)}
    override fun onPause(){handler.removeCallbacks(tick);super.onPause()}
    private fun text(value:String,size:Float=14f,color:Int=ink,bold:Boolean=false):TextView=TextView(this).apply {
        this.text=value;textSize=size;setTextColor(color);includeFontPadding=false
        if(bold)typeface=Typeface.create("sans-serif-medium",Typeface.NORMAL)
        layoutParams=LinearLayout.LayoutParams(-1,-2)
    }
    private fun gap(parent:LinearLayout,height:Int){parent.addView(Space(this),LinearLayout.LayoutParams(1,dp(height)))}
    private fun rounded(color:Int,radius:Int=20)=GradientDrawable().apply {setColor(color);cornerRadius=dp(radius).toFloat()}
    private fun button(label:String,primary:Boolean=false,action:()->Unit):Button=Button(this).apply {
        text=label;isAllCaps=false;textSize=15f;setTextColor(if(primary)bg else ink);typeface=Typeface.create("sans-serif-medium",Typeface.NORMAL)
        background=RippleDrawable(ColorStateList.valueOf(if(night)0x22ffffff else 0x16000000),rounded(if(primary)accent else soft),null)
        minHeight=0;minimumHeight=0;minWidth=0;minimumWidth=0;setPadding(dp(12),dp(9),dp(12),dp(9))
        layoutParams=LinearLayout.LayoutParams(-1,dp(52));setOnClickListener{action()}
    }
    private fun row():LinearLayout=LinearLayout(this).apply {orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;layoutParams=LinearLayout.LayoutParams(-1,-2)}
    private fun card():LinearLayout=LinearLayout(this).apply {orientation=LinearLayout.VERTICAL;background=rounded(surface);setPadding(dp(18),dp(17),dp(18),dp(17));layoutParams=LinearLayout.LayoutParams(-1,-2)}
    private fun render(){
        night=forceNight ?: when(prefs.getString("theme","system")){"dark"->true;"light"->false;else->resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK==Configuration.UI_MODE_NIGHT_YES}
        bg=Color.parseColor(if(night)"#101B19" else "#F5F5EF");surface=Color.parseColor(if(night)"#192824" else "#FFFFFF")
        ink=Color.parseColor(if(night)"#F0F5EC" else "#173A31");muted=Color.parseColor(if(night)"#9BB1A7" else "#6B8076")
        accent=Color.parseColor(if(night)"#6DD4AE" else "#258C78");soft=Color.parseColor(if(night)"#263C32" else "#E2EEE6")
        window.statusBarColor=bg;window.navigationBarColor=bg
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility=if(night)0 else View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        root=LinearLayout(this).apply {orientation=LinearLayout.VERTICAL;setBackgroundColor(bg);setPadding(dp(22),dp(12),dp(22),dp(10))}
        root.layoutDirection=if(fa)View.LAYOUT_DIRECTION_RTL else View.LAYOUT_DIRECTION_LTR
        setContentView(root)
        if(Build.VERSION.SDK_INT>=35) {
            root.setOnApplyWindowInsetsListener { view,insets ->
                val bars=insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
                view.setPadding(dp(22)+bars.left,dp(12)+bars.top,dp(22)+bars.right,dp(10)+bars.bottom)
                insets
            };root.requestApplyInsets()
        }
        val header=row()
        header.addView(LinearLayout(this).apply {orientation=LinearLayout.VERTICAL;layoutParams=LinearLayout.LayoutParams(0,-2,1f);addView(text("namedP",28f,ink,true));addView(text(tr("YOUR LITTLE CONNECTION COMPANION","همراه کوچک اتصال شما"),9f,muted))})
        header.addView(button("◐"){themePicker()}.apply {layoutParams=LinearLayout.LayoutParams(dp(42),dp(42));contentDescription=tr("Choose color theme","انتخاب تم")})
        root.addView(header);gap(root,12)
        val scroll=ScrollView(this).apply {isFillViewport=true;isVerticalScrollBarEnabled=false;layoutParams=LinearLayout.LayoutParams(-1,0,1f)}
        val content=LinearLayout(this).apply {orientation=LinearLayout.VERTICAL;setPadding(0,dp(6),0,dp(14))};scroll.addView(content);root.addView(scroll)
        traffic=null;elapsed=null
        when(page){"profiles"->profilesPage(content);"settings"->settingsPage(content);else->home(content)}
        val tabs=row().apply {background=rounded(surface,24);setPadding(dp(5),dp(5),dp(5),dp(5))}
        for((id,en,local)in listOf(Triple("home","Home","خانه"),Triple("profiles","Connections","اتصال‌ها"),Triple("settings","Settings","تنظیمات"))){
            tabs.addView(button(tr(en,local)){page=id;render()}.apply {layoutParams=LinearLayout.LayoutParams(0,dp(44),1f);textSize=12f;background=rounded(if(page==id)soft else surface,18);setTextColor(if(page==id)accent else muted)})
        };root.addView(tabs)
    }
    private fun home(parent:LinearLayout){
        val state=snapshot()
        if(preview!=null){parent.addView(text("INTERFACE PREVIEW · NOT A LIVE TUNNEL",10f,accent,true));gap(parent,4)}
        val stage=FrameLayout(this).apply {layoutParams=LinearLayout.LayoutParams(-1,dp(178))}
        stage.addView(View(this).apply {background=GradientDrawable().apply {shape=GradientDrawable.OVAL;setColor(soft)}},FrameLayout.LayoutParams(dp(154),dp(154),Gravity.CENTER))
        stage.addView(DachshundView(this).apply {night=this@MainActivity.night;this.state=state.state},FrameLayout.LayoutParams(-1,dp(174),Gravity.CENTER))
        parent.addView(stage)
        parent.addView(text(when(state.state){"connected"->tr("You're in good company.","با همراهت متصل شدی.");"connecting"->tr("Finding your way through…","در حال برقراری اتصال…");else->tr("A calmer way to connect.","راهی آرام‌تر برای اتصال.")},23f,ink,true).apply{gravity=Gravity.CENTER})
        gap(parent,9)
        parent.addView(text(when(state.state){"connected"->"●  "+tr("Connected","متصل");"connecting"->"◌  "+tr("Connecting","در حال اتصال");"error"->tr("Connection needs attention","اتصال نیاز به بررسی دارد");else->tr("Ready when you are","آمادهٔ اتصال")},12f,if(active())accent else muted).apply{gravity=Gravity.CENTER})
        gap(parent,18)
        val modes=row().apply {background=rounded(soft,18);setPadding(dp(4),dp(4),dp(4),dp(4))}
        for((id,label)in listOf("vpn" to tr("Device VPN","VPN دستگاه"),"proxy" to tr("Local proxy","پروکسی محلی"))){
            modes.addView(button(label){mode=id;prefs.edit().putString("mode",id).apply();render()}.apply{layoutParams=LinearLayout.LayoutParams(0,dp(38),1f);textSize=12f;background=rounded(if(mode==id)surface else soft,14);isEnabled=!active()&&preview==null;alpha=if(isEnabled)1f else 0.7f})
        };parent.addView(modes);gap(parent,12)
        val profile=selected()
        parent.addView(card().apply{
            addView(text(tr("CONNECTION","اتصال"),9f,muted,true));gap(this,7)
            addView(text(if(preview!=null)"Demo profile" else profile?.name ?: tr("Add your first connection","اولین اتصال را اضافه کنید"),16f,ink,true));gap(this,4)
            addView(text(if(preview!=null)"demo.namedp.example" else profile?.host ?: tr("Paste a link exported by your panel","لینک خروجی پنل را وارد کنید"),12f,muted))
            setOnClickListener{page="profiles";render()}
        });gap(parent,12)
        if(state.state=="error") {parent.addView(text(state.message,12f,Color.parseColor(if(night)"#F2AE9D" else "#A54832")));gap(parent,8)}
        parent.addView(button(if(active())tr("Disconnect","قطع اتصال")else tr("Connect →","اتصال ←"),true){toggle()}.apply{isEnabled=preview==null && !locked})
        gap(parent,13)
        val stats=row();traffic=text("↑ ${bytes(state.upload)}     ↓ ${bytes(state.download)}",12f,muted).apply {layoutParams=LinearLayout.LayoutParams(0,-2,1f)}
        elapsed=text("00:00",12f,muted).apply{layoutParams=LinearLayout.LayoutParams(-2,-2)};stats.addView(traffic);stats.addView(elapsed);parent.addView(stats)
        gap(parent,14);parent.addView(text(if(mode=="vpn")tr("TCP + UDP through your server. Rootless ICMP support varies by device.","TCP و UDP از سرور شما؛ پشتیبانی ICMP بدون روت به دستگاه بستگی دارد.")else tr("Configure apps to use SOCKS5 on 127.0.0.1:${profile?.port ?: 1080}.","برنامه‌ها را روی SOCKS5 به آدرس 127.0.0.1:${profile?.port ?: 1080} تنظیم کنید."),11f,muted).apply{gravity=Gravity.CENTER})
    }
    private fun profilesPage(parent:LinearLayout){
        parent.addView(text(tr("Your connections","اتصال‌های شما"),24f,ink,true));gap(parent,7)
        parent.addView(text(tr("One link. Your own server. No shared accounts.","یک لینک؛ سرور خودتان و حساب‌های مستقل."),12f,muted));gap(parent,20)
        parent.addView(button(tr("+ Add connection","+ افزودن اتصال"),true){editProfile()}.apply{isEnabled=!locked});gap(parent,16)
        profiles.forEach { profile ->
            parent.addView(card().apply{
                addView(text((if(selected()?.id==profile.id)"●  " else "○  ")+profile.name,16f,ink,true));gap(this,5);addView(text(profile.host,12f,muted));gap(this,5);addView(text(tr("Tap to select · hold to remove","برای انتخاب بزنید؛ برای حذف نگه دارید"),10f,muted))
                setOnClickListener{if(active()){notice(tr("Disconnect before changing profiles","پیش از تغییر پروفایل اتصال را قطع کنید"))}else{prefs.edit().putString("selected",profile.id).apply();page="home";render()}}
                setOnLongClickListener{if(active())notice(tr("Disconnect before removing a profile","پیش از حذف پروفایل اتصال را قطع کنید"))else dialog().setTitle(tr("Remove connection?","اتصال حذف شود؟")).setMessage(profile.name).setNegativeButton(tr("Cancel","انصراف"),null).setPositiveButton(tr("Remove","حذف")){_,_->try{profiles.remove(profile);store.save(profiles);render()}catch(e:Exception){notice(e.message?:"Could not save")}}.show();true}
            });gap(parent,12)
        }
        if(profiles.isEmpty())parent.addView(text(tr("Copy a complete pingtunnel:// link from Users → Config in your panel.","لینک کامل pingtunnel:// را از بخش Users ← Config در پنل کپی کنید."),13f,muted))
    }
    private fun settingsPage(parent:LinearLayout){
        parent.addView(text(tr("Make it yours","مطابق سلیقهٔ شما"),24f,ink,true));gap(parent,20)
        parent.addView(button(tr("Appearance · ${prefs.getString("theme","system")}","ظاهر · ${prefs.getString("theme","system")}")){themePicker()});gap(parent,10)
        parent.addView(button(tr("Language · English / فارسی","زبان · English / فارسی")){dialog().setTitle("Language / زبان").setItems(arrayOf("English","فارسی")){_,i->prefs.edit().putString("language",if(i==0)"en"else"fa").apply();render()}.show()});gap(parent,18)
        parent.addView(card().apply{
            addView(text(tr("Connection notes","نکته‌های اتصال"),16f,ink,true));gap(this,10)
            addView(text(tr("VPN uses Android's permission dialog and excludes namedP's own UID so ICMP cannot loop into its tunnel. IPv6 is captured by the VPN route rather than allowed to bypass it. Local proxy mode only affects configured apps. Your server's HTTP outbound cannot carry UDP.","VPN از مجوز رسمی اندروید استفاده می‌کند و خود برنامه را از تونل خارج می‌کند تا حلقهٔ اتصال ایجاد نشود. مسیر IPv6 نیز وارد VPN می‌شود تا از آن عبور نکند. حالت پروکسی فقط برنامه‌های تنظیم‌شده را پوشش می‌دهد. خروجی HTTP سرور از UDP پشتیبانی نمی‌کند."),12f,muted))
        });gap(parent,15)
        parent.addView(button(tr("Source code ↗","کد منبع ↗")){startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://github.com/namedkenzo/NamedClients")))});gap(parent,18)
        parent.addView(text("namedP Android · ${BuildConfig.VERSION_NAME}\n"+tr("Initial alpha · real-device network testing required.\nProfiles are encrypted with Android Keystore; cloud backup is disabled.","نسخهٔ اولیهٔ آلفا؛ نیازمند تست روی دستگاه واقعی.\nپروفایل‌ها با Android Keystore محافظت شده‌اند و بکاپ ابری غیرفعال است."),11f,muted))
    }
    private fun themePicker(){val values=arrayOf("system","light","dark");dialog().setTitle(tr("Appearance","ظاهر")).setSingleChoiceItems(arrayOf(tr("Follow system","مطابق سیستم"),tr("Light","روشن"),tr("Dark","تیره")),values.indexOf(prefs.getString("theme","system"))){d,i->prefs.edit().putString("theme",values[i]).apply();forceNight=null;d.dismiss();render()}.show()}
    private fun dialog()=AlertDialog.Builder(this,if(night)android.R.style.Theme_Material_Dialog_Alert else android.R.style.Theme_Material_Light_Dialog_Alert)
    private fun notice(message:String){dialog().setMessage(message).setPositiveButton("OK",null).show()}
    private fun editProfile(){
        val box=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(20),dp(8),dp(20),0)}
        val name=EditText(this).apply{hint=tr("Connection name","نام اتصال");isSingleLine=true;setTextColor(ink);setHintTextColor(muted)}
        val link=EditText(this).apply{hint="pingtunnel://…";inputType=InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD;isSingleLine=true;setTextColor(ink);setHintTextColor(muted);importantForAutofill=View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS}
        box.addView(name);box.addView(link);box.addView(button(tr("Paste link","چسباندن لینک")){val clip=getSystemService(ClipboardManager::class.java).primaryClip;if(clip!=null&&clip.itemCount>0)link.setText(clip.getItemAt(0).coerceToText(this).toString().trim())})
        val d=dialog().setTitle(tr("New connection","اتصال جدید")).setView(box).setNegativeButton(tr("Cancel","انصراف"),null).setPositiveButton(tr("Save","ذخیره"),null).create()
        d.setOnShowListener{d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener{try{
            val value=link.text.toString().trim();Profile.validate(value);require(name.text.toString().trim().length in 1..60){tr("Enter a name, up to 60 characters","نامی بین ۱ تا ۶۰ کاراکتر وارد کنید")}
            val p=Profile(UUID.randomUUID().toString(),name.text.toString().trim(),value);val updated=profiles.toMutableList().apply{add(p)};store.save(updated);profiles=updated
            prefs.edit().putString("selected",p.id).apply();d.dismiss();page="home";render()
        }catch(e:Exception){link.error=e.message ?: "Invalid profile"}}};d.show()
    }
    private fun toggle(){
        if(active()){startService(Intent(this,TunnelService::class.java).setAction("stop"));return}
        if(selected()==null){page="profiles";render();editProfile();return}
        if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED)requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS),42)
        if(mode=="vpn"){val permission=VpnService.prepare(this);if(permission!=null){startActivityForResult(permission,100);return}}
        startTunnel()
    }
    private fun startTunnel(){val profile=selected()?:return;startForegroundService(Intent(this,TunnelService::class.java).setAction("start").putExtra("link",profile.link).putExtra("mode",mode))}
    @Deprecated("Activity result compatibility") override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?){super.onActivityResult(requestCode,resultCode,data);if(requestCode==100 && resultCode==RESULT_OK)startTunnel()}
    fun previewForTest(dark:Boolean,state:String){check(BuildConfig.DEBUG);forceNight=dark;preview=TunnelSnapshot(state,"UI preview",153600,2841600,if(state=="connected")SystemClock.elapsedRealtime()-65000 else 0);page="home";render()}
    private fun bytes(n:Long)=when{n<1024->"$n B";n<1048576->String.format(Locale.US,"%.1f KiB",n/1024.0);else->String.format(Locale.US,"%.1f MiB",n/1048576.0)}
}
