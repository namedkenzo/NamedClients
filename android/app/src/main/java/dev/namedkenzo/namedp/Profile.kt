package dev.namedkenzo.namedp

import java.net.URI
import java.net.URLDecoder
import java.util.Base64

data class Profile(val id: String, val name: String, val link: String) {
    val host: String get() = URI(link).host ?: ""
    val port: Int get() = query(link)["lport"]?.toInt() ?: 1080
    companion object {
        private fun query(link: String): Map<String,String> {
            val uri = URI(link)
            val result = linkedMapOf<String,String>()
            uri.rawQuery.orEmpty().split('&').filter { it.isNotEmpty() }.forEach { item ->
                val pair = item.split('=', limit=2)
                val key = URLDecoder.decode(pair[0], "UTF-8")
                require(!result.containsKey(key)) { "Duplicate profile parameter" }
                result[key] = URLDecoder.decode(pair.getOrElse(1){""}, "UTF-8")
            }
            return result
        }
        fun validate(link: String) {
            require(link.length <= 8192) { "Connection link is too long" }
            val uri = URI(link)
            require(uri.scheme == "pingtunnel" && !uri.host.isNullOrBlank() && uri.port == -1 && uri.userInfo == null && uri.path.orEmpty().isEmpty() && uri.fragment == null) { "Paste a complete pingtunnel:// link" }
            require(!uri.host.contains(':')) { "IPv4 server or domain required" }
            val q = query(link)
            require(q.keys.all { it in setOf("key","encrypt","encrypt_key","lport","mode") }) { "Unsupported connection parameter" }
            require((q["lport"] ?: "1080").toIntOrNull()?.let { it in 1..65535 } == true) { "Invalid local port" }
            require((q["mode"] ?: "vpn") in setOf("vpn","proxy")) { "Invalid mode" }
            when (q["encrypt"].orEmpty()) {
                "", "none" -> require(q["key"]?.toIntOrNull()?.let { it > 0 } == true && !q.containsKey("encrypt_key")) { "Numeric mode needs one numeric key" }
                "aes128", "aes-128", "aes256", "aes-256", "chacha20" -> {
                    require(!q.containsKey("key")) { "Use encryption OR a numeric key" }
                    val key = try { Base64.getDecoder().decode(q["encrypt_key"].orEmpty()) } catch (_: IllegalArgumentException) { byteArrayOf() }
                    val size = if(q["encrypt"] in setOf("aes128","aes-128")) 16 else 32
                    require(key.size == size) { "Encryption key incomplete; paste the whole link" }
                }
                else -> error("Unsupported encryption")
            }
        }
    }
}
