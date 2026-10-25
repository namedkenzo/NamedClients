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
    private var lastState=""
    private var forceNight:Boolean?=null
    private var preview:TunnelSnapshot?=null
    private var detailId:String?=null
    private var detailForm:LinearLayout?=null
    private var detailValues:(()->Pair<String,String>)?=null
    private var detailStatus:TextView?=null
    private var detailError:TextView?=null
    private var diagnosticText:TextView?=null
    private var diagnostic=""
    private var testing=false
    @Volatile private var testProcess:java.lang.Process?=null
    @Volatile private var testGeneration=0
    private var testingProfileId:String?=null
    private val testLock=Any()
    private val relayResults=mutableMapOf<String,Pair<String,RelayDelayResult>>()
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
        val state=snapshot();if(state.state!=lastState){lastState=state.state;if(page=="details"){updateDetailStatus()}else render()}
        traffic?.text="↑ ${bytes(state.upload)}     ↓ ${bytes(state.download)}"
        elapsed?.text=if(state.since>0){val seconds=(SystemClock.elapsedRealtime()-state.since)/1000;String.format(Locale.US,"%02d:%02d",seconds/60,seconds%60)}else "00:00"
        handler.postDelayed(this,1000)
    }}
    override fun onCreate(savedInstanceState:Bundle?){
        super.onCreate(savedInstanceState)
        try{profiles=store.read()}catch(_:Exception){locked=true;TunnelStatus.snapshot=TunnelSnapshot("error", "Saved profiles could not be unlocked. Clear app data only if you have backup links.")}
        render()
    }
    override fun onResume(){super.onResume();handler.post(tick)}
    override fun onPause(){handler.removeCallbacks(tick);super.onPause()}
    override fun onDestroy(){cancelRelayTest();super.onDestroy()}
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
        setOnTouchListener{view,event->
            if(android.animation.ValueAnimator.areAnimatorsEnabled())when(event.actionMasked){
                MotionEvent.ACTION_DOWN->view.animate().scaleX(0.97f).scaleY(0.97f).setDuration(90).setInterpolator(android.view.animation.DecelerateInterpolator()).start()
                MotionEvent.ACTION_UP,MotionEvent.ACTION_CANCEL->view.animate().scaleX(1f).scaleY(1f).setDuration(170).setInterpolator(android.view.animation.DecelerateInterpolator()).start()
            };false
        }
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
        if(page=="details"){
            header.addView(button("‹"){page="home";render()}.apply{tag="detail-back";layoutParams=LinearLayout.LayoutParams(dp(42),dp(42));contentDescription=tr("Back","بازگشت")})
            header.addView(text(selected()?.takeIf{it.id==detailId}?.name ?: tr("New connection","اتصال جدید"),20f,ink,true).apply{layoutParams=LinearLayout.LayoutParams(0,-2,1f);maxLines=1;ellipsize=android.text.TextUtils.TruncateAt.END})
            header.addView(button("⧉"){copyDetail()}.apply{tag="copy-profile";layoutParams=LinearLayout.LayoutParams(dp(42),dp(42));contentDescription=tr("Copy configuration","کپی کانفیگ")})
        }else{
            header.addView(LinearLayout(this).apply {orientation=LinearLayout.VERTICAL;layoutParams=LinearLayout.LayoutParams(0,-2,1f);addView(text("namedP",28f,ink,true));addView(text(tr("YOUR LITTLE CONNECTION COMPANION","همراه کوچک اتصال شما"),9f,muted))})
            header.addView(button("◐"){themePicker()}.apply {layoutParams=LinearLayout.LayoutParams(dp(42),dp(42));contentDescription=tr("Choose color theme","انتخاب تم")})
        }
        root.addView(header);gap(root,12)
        val scroll=ScrollView(this).apply {isFillViewport=true;isVerticalScrollBarEnabled=false;layoutParams=LinearLayout.LayoutParams(-1,0,1f)}
        val content=LinearLayout(this).apply {orientation=LinearLayout.VERTICAL;setPadding(0,dp(6),0,dp(14))};scroll.addView(content);root.addView(scroll)
        traffic=null;elapsed=null
        when(page){"details"->detailsPage(content);"profiles"->profilesPage(content);"settings"->settingsPage(content);else->home(content)}
        if(page=="details"){
            root.addView(row().apply{
                addView(button(if(testing)tr("Testing…","در حال تست…")else tr("Test delay","تست تأخیر")){testDetail()}.apply{tag="test-profile";layoutParams=LinearLayout.LayoutParams(0,dp(52),1f);isEnabled=!testing && snapshot().state!="connecting" && preview==null})
                addView(button(if(active())tr("Disconnect","قطع اتصال")else tr("Connect","اتصال"),true){if(active())toggle()else if(commitDetail()!=null)toggle()}.apply{tag="connect";layoutParams=LinearLayout.LayoutParams(0,dp(52),2f).apply{marginStart=dp(8)}})
            });updateDetailStatus();return
        }
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
        val profile=selected()
        parent.addView(card().apply{
            tag="profile-card";addView(text(tr("CONNECTION","اتصال")+" · "+modeLabel(profile?.mode ?: "vpn"),9f,muted,true));gap(this,7)
            addView(text(if(preview!=null)"Demo profile" else profile?.name ?: tr("Add your first connection","اولین اتصال را اضافه کنید"),16f,ink,true));gap(this,4)
            addView(text(if(preview!=null)"demo.namedp.example" else profile?.host ?: tr("Paste a link exported by your panel","لینک خروجی پنل را وارد کنید"),12f,muted))
            setOnClickListener{if(profile==null){page="profiles";render()}else openDetails(profile)}
        });gap(parent,12)
        importButtons(parent);gap(parent,10)
        if(state.state=="error") {parent.addView(text(state.message,12f,Color.parseColor(if(night)"#F2AE9D" else "#A54832")));gap(parent,8)}
        parent.addView(button(if(active())tr("Disconnect","قطع اتصال")else tr("Connect →","اتصال ←"),true){toggle()}.apply{tag="connect";isEnabled=preview==null && !locked})
        gap(parent,13)
        val stats=row();traffic=text("↑ ${bytes(state.upload)}     ↓ ${bytes(state.download)}",12f,muted).apply {layoutParams=LinearLayout.LayoutParams(0,-2,1f)}
        elapsed=text("00:00",12f,muted).apply{layoutParams=LinearLayout.LayoutParams(-2,-2)};stats.addView(traffic);stats.addView(elapsed);parent.addView(stats)
        gap(parent,10)
        parent.addView(row().apply{
            addView(text(relayCaption(profile),12f,muted).apply{tag="home-relay-delay";layoutParams=LinearLayout.LayoutParams(0,-2,1f);accessibilityLiveRegion=View.ACCESSIBILITY_LIVE_REGION_POLITE})
            addView(button(if(testing)tr("Testing…","در حال تست…")else tr("Test delay","تست تأخیر")){selected()?.let{startRelayTest(it)}}.apply{tag="home-test-delay";layoutParams=LinearLayout.LayoutParams(dp(112),dp(40));textSize=12f;isEnabled=profile!=null && !testing && state.state!="connecting" && preview==null})
        })
        gap(parent,14);parent.addView(text(if(profile?.mode!="proxy")tr("Mode follows this configuration. TCP + UDP through Android VPN.","حالت اتصال از همین کانفیگ خوانده می‌شود؛ TCP و UDP از VPN اندروید.")else tr("Configure apps to use SOCKS5 on 127.0.0.1:${profile.port}.","برنامه‌ها را روی SOCKS5 به آدرس 127.0.0.1:${profile.port} تنظیم کنید."),11f,muted).apply{gravity=Gravity.CENTER})
    }
    private fun profilesPage(parent:LinearLayout){
        parent.addView(text(tr("Your connections","اتصال‌های شما"),24f,ink,true));gap(parent,7)
        parent.addView(text(tr("One link. Your own server. No shared accounts.","یک لینک؛ سرور خودتان و حساب‌های مستقل."),12f,muted));gap(parent,20)
        importButtons(parent);gap(parent,16)
        profiles.forEach { profile ->
            parent.addView(card().apply{
                addView(text((if(selected()?.id==profile.id)"●  " else "○  ")+profile.name,16f,ink,true));gap(this,5);addView(text(profile.host+" · "+modeLabel(profile.mode),12f,muted));gap(this,5);addView(text(relayCaption(profile),11f,muted));gap(this,5);addView(text(tr("Tap for details · hold to remove","جزئیات با لمس؛ حذف با نگه‌داشتن"),10f,muted))
                setOnClickListener{openDetails(profile)}
                setOnLongClickListener{if(active())notice(tr("Disconnect before removing a profile","پیش از حذف پروفایل اتصال را قطع کنید"))else dialog().setTitle(tr("Remove connection?","اتصال حذف شود؟")).setMessage(profile.name).setNegativeButton(tr("Cancel","انصراف"),null).setPositiveButton(tr("Remove","حذف")){_,_->try{profiles.remove(profile);store.save(profiles);render()}catch(e:Exception){notice(e.message?:"Could not save")}}.show();true}
            });gap(parent,12)
        }
        if(profiles.isEmpty())parent.addView(text(tr("Copy the Config link from your panel, tap Paste, then Connect. Or use + to enter the server and key manually.","لینک Config پنل را کپی کنید، Paste و سپس اتصال را بزنید؛ یا با + سرور و رمز را دستی وارد کنید."),13f,muted))
    }
    private fun settingsPage(parent:LinearLayout){
        parent.addView(text(tr("Make it yours","مطابق سلیقهٔ شما"),24f,ink,true));gap(parent,20)
        parent.addView(Switch(this).apply{tag="gaming-mode";text=tr("Gaming · low-latency UDP","گیمینگ · UDP کم‌تأخیر");setTextColor(ink);isChecked=prefs.getBoolean("gaming",false);isEnabled=!active();setOnCheckedChangeListener{_,checked->prefs.edit().putBoolean("gaming",checked).apply()}})
        parent.addView(text(tr("Reconnect to apply. Keeps UDP queues short and drops stale queued packets after 150 ms under load. May increase packet loss; not a guaranteed lower ping. Use Direct or UDP-capable SOCKS outbound, not HTTP. Pause downloads; per-app mode can include only your game.","برای اعمال دوباره وصل شوید. صف UDP کوتاه می‌شود و بسته‌های مانده بیش از ۱۵۰ میلی‌ثانیه حذف می‌شوند؛ ممکن است افت بسته بیشتر شود و کاهش پینگ تضمینی نیست. از خروجی مستقیم یا SOCKS دارای UDP استفاده کنید، نه HTTP. دانلودها را متوقف کنید؛ حالت برنامه‌ای می‌تواند فقط بازی را پوشش دهد."),12f,muted));gap(parent,14)
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
    private fun inputError(error:Exception)=if(error is ProfileInputException && fa)error.persian else if(error is ProfileInputException)error.message.orEmpty() else tr("Could not save the connection. Please try again.","ذخیرهٔ اتصال انجام نشد؛ دوباره تلاش کنید.")
    private fun importButtons(parent:LinearLayout){
        val controls=row()
        controls.addView(button(tr("Paste","چسباندن")){pasteProfile()}.apply{tag="paste-profile";layoutParams=LinearLayout.LayoutParams(0,dp(48),1f);isEnabled=!locked && !active() && preview==null})
        controls.addView(button("+"){editProfile()}.apply{tag="manual-profile";contentDescription=tr("Add connection manually","افزودن دستی اتصال");layoutParams=LinearLayout.LayoutParams(dp(52),dp(48)).apply{marginStart=dp(8)};textSize=24f;isEnabled=!locked && !active() && preview==null})
        parent.addView(controls)
    }
    private fun saveProfile(value:String,name:String="",editingId:String?=null,homeAfter:Boolean=true):Profile{
        check(!locked && !active())
        val normalized=Profile.normalize(value)
        val p=if(editingId!=null)Profile(editingId,name.ifBlank{Profile("","",normalized).host},normalized)else profiles.find{it.link==normalized} ?: Profile(UUID.randomUUID().toString(),name.ifBlank{Profile("","",normalized).host},normalized)
        val updated=profiles.toMutableList().apply{val index=indexOfFirst{it.id==p.id};if(index<0)add(p)else set(index,p)}
        store.save(updated);profiles=updated
        prefs.edit().putString("selected",p.id).remove("mode").apply()
        // Clear an old connection error only after a valid import was saved.
        TunnelStatus.snapshot=TunnelSnapshot();if(homeAfter){page="home";render()};return p
    }
    private fun pasteProfile(){
        if(locked || active())return
        try{
            val clip=getSystemService(ClipboardManager::class.java).primaryClip
            if(clip==null || clip.itemCount==0){notice(tr("Clipboard is empty. Copy the Config link from your panel first.","کلیپ‌بورد خالی است؛ ابتدا لینک Config پنل را کپی کنید."));return}
            // Read the complete clipboard item directly. No single-line password
            // editor, selection range or rendered/ellipsized text is involved.
            val value=clip.getItemAt(0).text?.toString()
            if(value.isNullOrBlank()){notice(tr("Clipboard has no text link. Copy the full Config link from your panel.","کلیپ‌بورد لینک متنی ندارد؛ لینک کامل Config پنل را کپی کنید."));return}
            saveProfile(value)
            Toast.makeText(this,tr("Imported. Tap Connect when ready.","وارد شد؛ برای اتصال دکمهٔ اتصال را بزنید."),Toast.LENGTH_SHORT).show()
        }catch(e:Exception){notice(inputError(e))}
    }
    private fun modeLabel(value:String)=when(value){"proxy"->tr("Proxy","پروکسی");"proxy_per_app"->tr("Proxy per app","پروکسی برنامه‌ها");else->"VPN"}
    private fun openDetails(profile:Profile?){
        if(locked)return
        if(active() && profile?.id!=selected()?.id){notice(tr("Disconnect before changing profiles","پیش از تغییر پروفایل اتصال را قطع کنید"));return}
        if(testing && profile?.id!=testingProfileId)cancelRelayTest()
        if(profile!=null)prefs.edit().putString("selected",profile.id).apply()
        detailId=profile?.id;diagnostic=if(testing)tr("Testing HTTPS through this profile…","در حال تست HTTPS از داخل این کانفیگ…")else relayResult(profile)?.let{relayDescription(it)}.orEmpty();page="details";render()
    }
    private fun editProfile(){openDetails(null)}
    private fun detailsPage(parent:LinearLayout){
        val profile=profiles.find{it.id==detailId}
        val q=profile?.parameters().orEmpty()
        parent.addView(card().apply{
            detailStatus=text("",15f,ink,true);addView(detailStatus)
        });gap(parent,12)
        val box=card();detailForm=box;parent.addView(box)
        box.addView(row().apply{
            addView(text(tr("Details","جزئیات"),18f,ink,true).apply{layoutParams=LinearLayout.LayoutParams(0,-2,1f)})
            addView(button(tr("Save","ذخیره"),true){if(commitDetail()!=null)Toast.makeText(this@MainActivity,tr("Saved","ذخیره شد"),Toast.LENGTH_SHORT).show()}.apply{tag="manual-save";layoutParams=LinearLayout.LayoutParams(dp(86),dp(42))})
        });gap(box,14)
        val formContext=ContextThemeWrapper(this,if(night)android.R.style.Theme_Material else android.R.style.Theme_Material_Light)
        fun field(id:String,label:String,value:String="",type:Int=InputType.TYPE_CLASS_TEXT):EditText{
            box.addView(text(label,12f,muted));gap(box,3)
            return EditText(formContext).apply{
                tag="manual-"+id;inputType=type;isSingleLine=true;setTextColor(ink);setHintTextColor(muted)
                importantForAutofill=View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
                setText(value);box.addView(this,LinearLayout.LayoutParams(-1,dp(48)));gap(box,12)
            }
        }
        val name=field("name",tr("Name (optional)","نام (اختیاری)"),profile?.name.orEmpty())
        val server=field("server",tr("Host","سرور"),profile?.host.orEmpty(),InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI).apply{hint="vpn.example.com";textDirection=View.TEXT_DIRECTION_LTR}
        box.addView(text(tr("Endpoint IP family","خانوادهٔ IP اتصال"),12f,muted))
        val families=listOf("ipv4","ipv6")
        val ipFamily=Spinner(formContext).apply{tag="manual-family";adapter=ArrayAdapter(formContext,android.R.layout.simple_spinner_dropdown_item,listOf("IPv4","IPv6"));setSelection(if(q["family"]=="ipv6" || profile?.host?.contains(':')==true)1 else 0);box.addView(this,LinearLayout.LayoutParams(-1,dp(48)))}
        gap(box,12)
        box.addView(text(tr("Mode","حالت"),12f,muted))
        val modes=listOf("proxy","vpn","proxy_per_app")
        val connectionMode=Spinner(formContext).apply{
            tag="manual-mode";adapter=ArrayAdapter(formContext,android.R.layout.simple_spinner_dropdown_item,modes.map{modeLabel(it)})
            setSelection(modes.indexOf(profile?.mode ?: "vpn").coerceAtLeast(0));box.addView(this,LinearLayout.LayoutParams(-1,dp(48)))
        };gap(box,12)
        val methods=listOf("none","aes128","aes256","chacha20")
        box.addView(text(tr("Encryption","رمزنگاری"),12f,muted))
        val auth=Spinner(formContext).apply{
            tag="manual-auth";adapter=ArrayAdapter(formContext,android.R.layout.simple_spinner_dropdown_item,listOf(tr("None · numeric key","خاموش · رمز عددی"),"AES-128","AES-256","ChaCha20"))
            setSelection(methods.indexOf(q["encrypt"] ?: "none").coerceAtLeast(0));box.addView(this,LinearLayout.LayoutParams(-1,dp(48)))
        };gap(box,12)
        // setSingleLine replaces Android's transformation method. Apply the
        // password transform last so the unchecked reveal box really masks it.
        val secret=field("credential",tr("Key","کلید"),q["key"] ?: q["encrypt_key"].orEmpty(),InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD).apply{textDirection=View.TEXT_DIRECTION_LTR;transformationMethod=android.text.method.PasswordTransformationMethod.getInstance()}
        val reveal=CheckBox(formContext).apply{
            tag="reveal-key";text=tr("Show key","نمایش کلید");setTextColor(muted);textSize=12f
            setOnCheckedChangeListener{_,checked->secret.transformationMethod=if(checked)null else android.text.method.PasswordTransformationMethod.getInstance()}
        };box.addView(reveal);gap(box,8)
        auth.onItemSelectedListener=object:AdapterView.OnItemSelectedListener{
            private var previous=auth.selectedItemPosition
            override fun onNothingSelected(parent:AdapterView<*>?){}
            override fun onItemSelected(parent:AdapterView<*>?,view:View?,position:Int,id:Long){
                if(position!=previous)secret.setText("");previous=position
                secret.hint=if(position==0)tr("Numeric key from panel","رمز عددی پنل")else tr("Base64 encryption key","کلید Base64 رمزنگاری")
            }
        }
        val port=field("port",tr("Local SOCKS port","پورت محلی SOCKS"),q["lport"] ?: "1080",InputType.TYPE_CLASS_NUMBER).apply{textDirection=View.TEXT_DIRECTION_LTR}
        var chosen=profile?.apps ?: emptyList()
        val appsButton=button(tr("Choose applications","انتخاب برنامه‌ها")){}.apply{tag="manual-apps"}
        fun appCaption(){appsButton.text=tr("Applications: ","برنامه‌ها: ")+chosen.size}
        appsButton.setOnClickListener{
            val intent=Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            val entries=packageManager.queryIntentActivities(intent,0).filter{it.activityInfo.packageName!=packageName}.distinctBy{it.activityInfo.packageName}.sortedBy{it.loadLabel(packageManager).toString().lowercase()}
            val selected=chosen.toMutableSet()
            dialog().setTitle(tr("Only selected apps use this VPN","فقط برنامه‌های انتخاب‌شده از VPN استفاده می‌کنند"))
                .setMultiChoiceItems(entries.map{it.loadLabel(packageManager).toString()}.toTypedArray(),entries.map{it.activityInfo.packageName in selected}.toBooleanArray()){_,index,checked->val id=entries[index].activityInfo.packageName;if(checked)selected.add(id)else selected.remove(id)}
                .setNegativeButton(tr("Cancel","انصراف"),null).setPositiveButton(tr("Save","ذخیره")){_,_->chosen=selected.sorted();appCaption()}.show()
        }
        appCaption();box.addView(appsButton)
        connectionMode.onItemSelectedListener=object:AdapterView.OnItemSelectedListener{
            override fun onNothingSelected(parent:AdapterView<*>?){}
            override fun onItemSelected(parent:AdapterView<*>?,view:View?,position:Int,id:Long){appsButton.visibility=if(position==2)View.VISIBLE else View.GONE}
        };appsButton.visibility=if(connectionMode.selectedItemPosition==2)View.VISIBLE else View.GONE
        detailError=text("",12f,Color.parseColor(if(night)"#F2AE9D" else "#A54832")).apply{tag="manual-error";accessibilityLiveRegion=View.ACCESSIBILITY_LIVE_REGION_POLITE};box.addView(detailError)
        detailValues={
            val title=name.text.toString().trim()
            if(title.length>60)throw ProfileInputException("name","Name must be at most 60 characters.","نام باید حداکثر ۶۰ کاراکتر باشد.")
            Profile.manualLink(server.text.toString(),port.text.toString(),modes[connectionMode.selectedItemPosition.coerceIn(modes.indices)],methods[auth.selectedItemPosition.coerceIn(methods.indices)],secret.text.toString(),chosen,families[ipFamily.selectedItemPosition.coerceIn(families.indices)]) to title
        }
        gap(parent,12)
        parent.addView(card().apply{
            addView(text(tr("Diagnostics","عیب‌یابی"),17f,ink,true));gap(this,8)
            diagnosticText=text(diagnostic.ifBlank{tr("Not tested. Relay delay measures TCP + TLS + HTTP through this profile; it is not download speed or raw ICMP ping.","آزمایش نشده؛ تأخیر از مسیر TCP و TLS و HTTP همین کانفیگ اندازه‌گیری می‌شود؛ سرعت دانلود یا پینگ خام ICMP نیست.")},12f,muted).apply{tag="diagnostic-status";accessibilityLiveRegion=View.ACCESSIBILITY_LIVE_REGION_POLITE};addView(diagnosticText)
            gap(this,8);addView(text(RelayDelayResult.URL,10f,muted).apply{textDirection=View.TEXT_DIRECTION_LTR})
        })
        updateDetailStatus()
    }
    private fun setInputsEnabled(view:View,enabled:Boolean){
        view.isEnabled=enabled
        if(view is ViewGroup)for(i in 0 until view.childCount)setInputsEnabled(view.getChildAt(i),enabled)
    }
    private fun updateDetailStatus(){
        val state=snapshot()
        detailStatus?.text=when(state.state){"connected"->tr("Connected","متصل");"connecting"->tr("Connecting…","در حال اتصال…");"error"->state.message;else->tr("Disconnected · not connected","قطع · اتصال برقرار نیست")}
        detailForm?.let{setInputsEnabled(it,!active() && !testing)}
        root.findViewWithTag<Button>("test-profile")?.apply{isEnabled=!testing && state.state!="connecting" && preview==null;text=if(testing)tr("Testing…","در حال تست…")else tr("Test delay","تست تأخیر")}
    }
    private fun commitDetail():Profile?{
        if(active())return profiles.find{it.id==detailId}
        return try{
            val (value,name)=checkNotNull(detailValues).invoke()
            val profile=saveProfile(value,name,detailId,false);detailId=profile.id;detailError?.text="";profile
        }catch(e:Exception){detailError?.text=inputError(e);null}
    }
    private fun copyDetail(){
        val value=if(active())profiles.find{it.id==detailId}?.link else try{detailValues?.invoke()?.first}catch(e:Exception){detailError?.text=inputError(e);null}
        if(value==null)return
        val clip=ClipData.newPlainText("namedP configuration",value)
        clip.description.extras=PersistableBundle().apply{putBoolean("android.content.extra.IS_SENSITIVE",true)}
        getSystemService(ClipboardManager::class.java).setPrimaryClip(clip)
        Toast.makeText(this,tr("Configuration copied","کانفیگ کپی شد"),Toast.LENGTH_SHORT).show()
    }
    private fun relayResult(profile:Profile?)=profile?.let{relayResults[it.id]?.takeIf{entry->entry.first==it.link}?.second}
    private fun relayCaption(profile:Profile?):String {
        if(testing && profile?.id==testingProfileId)return tr("Delay: testing…","تأخیر: در حال تست…")
        val result=relayResult(profile)
        return when{result?.milliseconds!=null->tr("Delay: ","تأخیر: ")+result.milliseconds+" ms";result?.failure!=null->tr("Delay: failed","تأخیر: ناموفق");else->tr("Delay: not tested","تأخیر: تست نشده")}
    }
    private fun relayDescription(result:RelayDelayResult)=result.milliseconds?.let{tr("Relay delay · ","تأخیر مسیر · ")+it+" ms · HTTPS 204"} ?: result.failure.orEmpty()
    private fun cancelRelayTest(){synchronized(testLock){testGeneration++;testProcess?.destroyForcibly();testProcess=null};testing=false;testingProfileId=null}
    private fun testDetail(){
        if(testing || snapshot().state=="connecting" || preview!=null)return
        val profile=commitDetail() ?: return
        startRelayTest(profile)
    }
    private fun startRelayTest(profile:Profile){
        if(testing || snapshot().state=="connecting" || preview!=null || locked)return
        testing=true;testingProfileId=profile.id;diagnostic=tr("Testing HTTPS through this profile…","در حال تست HTTPS از داخل این کانفیگ…")
        if(page=="details"){diagnosticText?.text=diagnostic;updateDetailStatus()}else render()
        val generation=++testGeneration
        Thread{
            var process:java.lang.Process?=null
            val result=try{
                process=ProcessBuilder(java.io.File(applicationInfo.nativeLibraryDir,"libnamedp.so").absolutePath).directory(filesDir).start()
                synchronized(testLock){check(generation==testGeneration){"Test cancelled"};testProcess=process}
                val input=org.json.JSONObject().put("link",profile.engineLink()).put("mode",if(profile.mode=="proxy")"proxy"else"vpn").put("relay_delay_only",true).put("gaming_mode",prefs.getBoolean("gaming",false))
                process.outputStream.write((input.toString()+"\n").toByteArray(Charsets.UTF_8));process.outputStream.flush()
                if(!process.waitFor(30,java.util.concurrent.TimeUnit.SECONDS)){process.destroyForcibly();error(tr("Relay test timed out","مهلت تست تأخیر تمام شد"))}
                if(process.exitValue()!=0)error(process.errorStream.bufferedReader().readText().trim().take(180).ifBlank{tr("Relay test failed","تست تأخیر ناموفق بود")})
                val event=org.json.JSONObject(process.inputStream.bufferedReader().readText())
                RelayDelayResult.verified(event.optString("event"),event.optString("url"),event.optInt("status"),event.optLong("latency_ms",-1))
            }catch(e:Exception){RelayDelayResult(null,tr("Relay test failed","تست تأخیر ناموفق بود")+": "+(e.message ?: tr("Unknown error","خطای نامشخص")))}
            finally{try{process?.outputStream?.close()}catch(_:Exception){};process?.destroyForcibly();synchronized(testLock){if(testProcess===process)testProcess=null}}
            handler.post{if(generation==testGeneration && !isFinishing && !isDestroyed){
                testing=false;testingProfileId=null;relayResults[profile.id]=profile.link to result
                if(page=="details" && detailId==profile.id){diagnostic=relayDescription(result);diagnosticText?.text=diagnostic;updateDetailStatus()}else if(page!="details")render()
            }}
        }.start()
    }
    private fun toggle(){
        if(active()){cancelRelayTest();startService(Intent(this,TunnelService::class.java).setAction("stop"));return}
        if(selected()==null){notice(tr("Tap Paste to import your copied Config link, or + to enter it manually.","با Paste لینک Config کپی‌شده را وارد کنید یا با + دستی اضافه کنید."));return}
        if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED)requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS),42)
        val profile=selected()?:return
        if(profile.mode=="proxy_per_app" && profile.apps.isEmpty()){openDetails(profile);notice(tr("Choose applications in connection details first","ابتدا برنامه‌ها را در جزئیات اتصال انتخاب کنید"));return}
        if(profile.mode!="proxy"){val permission=VpnService.prepare(this);if(permission!=null){startActivityForResult(permission,100);return}}
        startTunnel()
    }
    private fun startTunnel(){val profile=selected()?:return;startForegroundService(Intent(this,TunnelService::class.java).setAction("start").putExtra("link",profile.link))}
    @Deprecated("Activity result compatibility") override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?){super.onActivityResult(requestCode,resultCode,data);if(requestCode==100 && resultCode==RESULT_OK)startTunnel()}
    fun previewForTest(dark:Boolean,state:String){check(BuildConfig.DEBUG);forceNight=dark;preview=TunnelSnapshot(state,"UI preview",153600,2841600,if(state=="connected")SystemClock.elapsedRealtime()-65000 else 0);page="home";render()}
    private fun bytes(n:Long)=when{n<1024->"$n B";n<1048576->String.format(Locale.US,"%.1f KiB",n/1024.0);else->String.format(Locale.US,"%.1f MiB",n/1048576.0)}
}
