package dev.namedkenzo.namedp

import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.Base64

// Errors must never include a supplied URI/key: these contain credentials.
class ProfileInputException(val field: String, message: String, val persian: String): IllegalArgumentException(message)

data class Profile(val id: String, val name: String, val link: String) {
    val host: String get() = URI(link).host ?: ""
    val port: Int get() = query(link)["lport"]?.toInt() ?: 1080
    val mode: String get() = query(link)["mode"] ?: "vpn"
    companion object {
        private fun invalid(field: String, en: String, fa: String): Nothing = throw ProfileInputException(field,en,fa)
        private fun encode(value: String) = URLEncoder.encode(value,"UTF-8")
        private val aliases = mapOf("local_port" to "lport", "vpn" to "mode",
            "encrypt_mode" to "encrypt", "encryptMode" to "encrypt", "encrypt-mode" to "encrypt", "enc" to "encrypt",
            "encrypt-key" to "encrypt_key", "encryptKey" to "encrypt_key")
        private fun decode(value: String): String = try {
            // Query URIs are not HTML forms. Preserve literal Base64 '+' too.
            URLDecoder.decode(value.replace("+","%2B"),"UTF-8")
        } catch(_: IllegalArgumentException) { invalid("link","Invalid link encoding; copy the link again.","کدگذاری لینک نامعتبر است؛ لینک را دوباره کپی کنید.") }
        private fun query(link: String): Map<String,String> {
            val result = linkedMapOf<String,String>()
            URI(link).rawQuery.orEmpty().split('&').filter { it.isNotEmpty() }.forEach { item ->
                val pair = item.split('=',limit=2)
                val original = decode(pair[0])
                val key = aliases[original] ?: original
                if(result.containsKey(key)) invalid("link","Duplicate connection parameter.","یکی از پارامترهای اتصال تکراری است.")
                result[key] = decode(pair.getOrElse(1){""})
            }
            return result
        }
        private fun digits(value: String) = value.map { c -> when(c) {
            in '۰'..'۹' -> '0'+(c-'۰'); in '٠'..'٩' -> '0'+(c-'٠'); else -> c
        }}.joinToString("")
        fun validate(link: String) { normalize(link) }

        // Pass this canonical URI to the engine too, not just to UI validation.
        fun normalize(input: String): String {
            val link=input.trim().removePrefix("\uFEFF").trim()
            if(link.length>8192) invalid("link","Connection link is too long.","لینک اتصال بیش از حد طولانی است.")
            val uri=try { URI(link) } catch(_: Exception) { invalid("link","Invalid link; use Paste to import the complete original link.","لینک نامعتبر است؛ با Paste لینک اصلی را کامل وارد کنید.") }
            if(!uri.scheme.equals("pingtunnel",ignoreCase=true) || uri.host.isNullOrBlank() || uri.port != -1 || uri.userInfo != null || uri.path.orEmpty() !in setOf("","/") || uri.fragment != null)
                invalid("server","A complete pingtunnel:// link with an IPv4 server or domain is required.","لینک کامل pingtunnel:// با آی‌پی IPv4 یا دامنه لازم است.")
            if(uri.host.contains(':')) invalid("server","IPv4 server or domain required.","آی‌پی IPv4 یا دامنه لازم است.")
            val q = query(link)
            if(q.keys.any { it !in setOf("key","encrypt","encrypt_key","lport","mode") }) invalid("link","This link has unsupported parameters.","این لینک پارامتر پشتیبانی‌نشده دارد.")
            val port=digits(q["lport"] ?: "1080").toIntOrNull()
            if(port==null || port !in 1..65535) invalid("port","Local SOCKS port must be between 1 and 65535.","پورت محلی SOCKS باید بین ۱ و ۶۵۵۳۵ باشد.")
            val mode=when(q["mode"]?.lowercase() ?: "vpn") { "1","vpn" -> "vpn"; "0","proxy" -> "proxy"; else -> invalid("mode","Choose Device VPN or Local proxy.","حالت VPN دستگاه یا پروکسی محلی را انتخاب کنید.") }
            val encryption=when(q["encrypt"]?.lowercase().orEmpty()) {
                "","0","none" -> "none"; "aes128","aes-128" -> "aes128"; "aes256","aes-256" -> "aes256"; "chacha20" -> "chacha20"
                else -> invalid("credential","Unsupported encryption method.","روش رمزنگاری پشتیبانی نمی‌شود.")
            }
            val params=linkedMapOf("lport" to port.toString(),"mode" to mode)
            if(encryption=="none") {
                if(q.containsKey("encrypt_key")) invalid("credential","Encryption key found but its method is missing. Copy the complete link or choose the method with +.","کلید رمزنگاری وجود دارد اما روش آن مشخص نیست؛ لینک کامل را کپی کنید یا با + روش را انتخاب کنید.")
                val key=digits(q["key"].orEmpty())
                if(key.isEmpty()) invalid("credential","The copied link has no key. Copy the full Config link from your panel, or enter server and key using +.","لینک کپی‌شده رمز ندارد؛ لینک کامل Config پنل را کپی کنید یا سرور و رمز را با + وارد کنید.")
                if(!key.all { it in '0'..'9' } || key.toIntOrNull()?.let{it>0}!=true) invalid("credential","Numeric key must be a number from 1 to 2147483647.","رمز عددی باید عددی بین ۱ تا ۲۱۴۷۴۸۳۶۴۷ باشد.")
                params["key"]=key.toInt().toString()
            } else {
                if(q.containsKey("key")) invalid("credential","Use encryption OR a numeric key, not both.","فقط رمزنگاری یا رمز عددی را انتخاب کنید، نه هر دو.")
                val bytes=try { Base64.getDecoder().decode(q["encrypt_key"].orEmpty()) } catch(_: IllegalArgumentException) { byteArrayOf() }
                if(bytes.size != if(encryption=="aes128")16 else 32) invalid("credential","Encryption key is missing or incomplete; copy the complete link.","کلید رمزنگاری ناقص یا خالی است؛ لینک را کامل کپی کنید.")
                params["encrypt"]=encryption;params["encrypt_key"]=Base64.getEncoder().encodeToString(bytes)
            }
            return "pingtunnel://${uri.host}?"+params.entries.joinToString("&"){(key,value)->"${encode(key)}=${encode(value)}"}
        }
        fun manualLink(server: String, port: String, mode: String, encryption: String, credential: String): String {
            val host=server.trim()
            if(host.isEmpty() || host.any{it.isWhitespace() || it in "/?#@:%\\"}) invalid("server","Enter only the server IPv4 address or domain, without a URL or port.","فقط آی‌پی IPv4 یا دامنهٔ سرور را بدون آدرس وب یا پورت وارد کنید.")
            val params=linkedMapOf("lport" to digits(port.trim().ifBlank{"1080"}),"mode" to mode)
            if(encryption=="none") params["key"]=digits(credential.trim())
            else { params["encrypt"]=encryption;params["encrypt_key"]=credential.trim() }
            return normalize("pingtunnel://$host?"+params.entries.joinToString("&"){(key,value)->"${encode(key)}=${encode(value)}"})
        }
    }
}
