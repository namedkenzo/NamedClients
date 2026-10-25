package dev.namedkenzo.namedp

import android.app.*
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.LocalServerSocket
import android.net.VpnService
import android.os.*
import org.json.JSONObject
import java.io.File
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

data class TunnelSnapshot(val state: String="idle",val message: String="Ready when you are",val upload:Long=0,val download:Long=0,val since:Long=0)
object TunnelStatus { @Volatile var snapshot = TunnelSnapshot() }

class TunnelService: VpnService() {
    private val generation = AtomicInteger()
    private val pool = Executors.newCachedThreadPool()
    private var engine: java.lang.Process? = null
    private var tun: ParcelFileDescriptor? = null
    private var socket: LocalServerSocket? = null
    private val lock = Any()
    private val ui = Handler(Looper.getMainLooper())

    override fun onStartCommand(intent:Intent?, flags:Int,startId:Int):Int {
        if(intent==null || intent.action=="stop") { stopTunnel();return START_NOT_STICKY }
        if(engine!=null || TunnelStatus.snapshot.state=="connecting") return START_NOT_STICKY
        val link = intent.getStringExtra("link") ?: return START_NOT_STICKY
        TunnelStatus.snapshot=TunnelSnapshot("connecting","Checking your tunnel…")
        foreground("Connecting…")
        val token = generation.incrementAndGet()
        pool.execute { startTunnel(link,token) }
        return START_NOT_STICKY
    }
    private fun current(token:Int) = generation.get()==token
    private fun startTunnel(link:String,token:Int) {
        try {
            val normalizedLink=Profile.normalize(link)
            val profile=Profile("","",normalizedLink);val mode=profile.mode
            if(mode=="proxy_per_app")require(profile.apps.isNotEmpty() && packageName !in profile.apps){"Select at least one application (not namedP) in connection details"}
            val nativeDir=applicationInfo.nativeLibraryDir
            val executable=File(nativeDir,"libnamedp.so")
            val bridge=File(nativeDir,"libtun2socks.so")
            check(executable.isFile && (mode=="proxy" || bridge.isFile)) { "This APK is missing its native engine" }
            val socketName="namedp-"+UUID.randomUUID().toString().replace("-", "")
            synchronized(lock) { if(!current(token))return; if(mode!="proxy") socket=LocalServerSocket(socketName) }
            val process=ProcessBuilder(executable.absolutePath).directory(filesDir).start()
            synchronized(lock) { if(!current(token)){process.destroy();return};engine=process }
            val input=JSONObject().put("link",profile.engineLink()).put("mode",if(mode=="proxy")"proxy"else"vpn").put("tun_socket",socketName).put("tun_binary",bridge.absolutePath)
            process.outputStream.write((input.toString()+"\n").toByteArray(Charsets.UTF_8));process.outputStream.flush()
            val stderr=StringBuilder()
            pool.execute { process.errorStream.bufferedReader().useLines { lines -> lines.forEach { line -> synchronized(stderr){if(stderr.length<1500)stderr.append(line).append('\n')} } } }
            // An authenticated probe precedes proxy_ready. Create the VPN only
            // then, exclude our UID, and pass its descriptor over an authenticated
            // local socket. Closing the pipe ends the child and its TUN bridge.
            process.inputStream.bufferedReader().useLines { lines -> lines.forEach { line ->
                if(!current(token))return@forEach
                val event=JSONObject(line)
                when(event.optString("event")) {
                    "proxy_ready" -> if(mode!="proxy") establishVpn(token,profile)
                    "ready" -> {
                        if(current(token)) {
                            TunnelStatus.snapshot=TunnelSnapshot("connected",if(mode=="vpn")"Device VPN · TCP + UDP" else if(mode=="proxy_per_app")"Selected apps · TCP + UDP" else "SOCKS5 · 127.0.0.1:${event.getInt("port")}",since=SystemClock.elapsedRealtime())
                            ui.post { if(current(token))foreground("Connected · $mode") }
                        }
                    }
                    "stats" -> if(current(token)) {
                        val old=TunnelStatus.snapshot
                        TunnelStatus.snapshot=old.copy(upload=event.optLong("upload"),download=event.optLong("download"))
                    }
                }
            } }
            process.waitFor()
            if(current(token)) {
                val detail=synchronized(stderr){stderr.toString().trim().take(250)}
                fail(detail.ifBlank { "Connection stopped. Check the server and reconnect." },token)
            }
        } catch(error:Exception) { fail(error.message ?: "Could not start the tunnel",token) }
    }
    private fun establishVpn(token:Int,profile:Profile) {
        if(!current(token))return
        val builder=Builder().setSession("namedP").setMtu(1280)
            .addAddress("198.18.0.1",32).addRoute("0.0.0.0",0)
            .addAddress("fd42:6e70::1",128).addRoute("::",0)
            .addDnsServer("1.1.1.1").addDnsServer("1.0.0.1")
        if(profile.mode=="proxy_per_app") {
            require(profile.apps.isNotEmpty() && packageName !in profile.apps)
            profile.apps.forEach{builder.addAllowedApplication(it)}
        } else builder.addDisallowedApplication(packageName)
        val descriptor=builder.establish() ?: error("VPN permission is required")
        val server:LocalServerSocket
        synchronized(lock) {
            if(!current(token)){descriptor.close();return}
            tun=descriptor;server=socket ?: error("Descriptor socket missing")
        }
        server.accept().use { peer ->
            check(peer.peerCredentials.uid==android.os.Process.myUid()) { "Untrusted descriptor peer" }
            peer.setFileDescriptorsForSend(arrayOf(descriptor.fileDescriptor))
            peer.outputStream.write(1);peer.outputStream.flush()
        }
        synchronized(lock){socket?.close();socket=null}
    }
    private fun fail(message:String,token:Int) {
        ui.post { if(current(token)){stopTunnel();TunnelStatus.snapshot=TunnelSnapshot("error",message)} }
    }
    @Synchronized private fun stopTunnel() {
        generation.incrementAndGet()
        synchronized(lock) {
            try{socket?.close()}catch(_:Exception){};socket=null
            try{tun?.close()}catch(_:Exception){};tun=null
            engine?.let { p ->
                try{p.outputStream.close()}catch(_:Exception){}
                pool.execute { try{if(!p.waitFor(3,java.util.concurrent.TimeUnit.SECONDS))p.destroyForcibly()}catch(_:Exception){p.destroy()} }
            };engine=null
        }
        TunnelStatus.snapshot=TunnelSnapshot()
        stopForeground(STOP_FOREGROUND_REMOVE);stopSelf()
    }
    override fun onRevoke(){stopTunnel();super.onRevoke()}
    override fun onDestroy(){stopTunnel();pool.shutdown();super.onDestroy()}
    private fun foreground(text:String) {
        val manager=getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel("namedp","namedP connection",NotificationManager.IMPORTANCE_LOW))
        val open=PendingIntent.getActivity(this,1,Intent(this,MainActivity::class.java),PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val stop=PendingIntent.getService(this,2,Intent(this,TunnelService::class.java).setAction("stop"),PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val note=Notification.Builder(this,"namedp").setSmallIcon(dev.namedkenzo.namedp.R.drawable.ic_notification)
            .setContentTitle("namedP").setContentText(text).setOngoing(true).setContentIntent(open)
            .addAction(Notification.Action.Builder(null,"Disconnect",stop).build()).build()
        if(Build.VERSION.SDK_INT>=34)startForeground(17,note,ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE) else startForeground(17,note)
    }
}
