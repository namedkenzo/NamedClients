package dev.namedkenzo.namedp

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import org.json.JSONArray
import org.json.JSONObject

class ProfileStore(context: Context) {
    private val prefs = context.getSharedPreferences("namedP-private", Context.MODE_PRIVATE)
    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey("namedp-profiles-v1", null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder("namedp-profiles-v1",KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }
    fun read(): MutableList<Profile> {
        val saved = prefs.getString("profiles",null) ?: return mutableListOf()
        val bytes = Base64.decode(saved,Base64.NO_WRAP)
        require(bytes.size >= 28) { "Stored profiles are damaged" }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE,key(),GCMParameterSpec(128,bytes.copyOfRange(0,12)))
        val array = JSONArray(String(cipher.doFinal(bytes.copyOfRange(12,bytes.size)),Charsets.UTF_8))
        return (0 until array.length()).map { i -> array.getJSONObject(i).let { Profile(it.getString("id"),it.getString("name"),it.getString("link")) } }.toMutableList()
    }
    fun save(profiles: List<Profile>) {
        val array = JSONArray()
        profiles.forEach { array.put(JSONObject().put("id",it.id).put("name",it.name).put("link",it.link)) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,key())
        val bytes = cipher.doFinal(array.toString().toByteArray(Charsets.UTF_8))
        check(prefs.edit().putString("profiles",Base64.encodeToString(cipher.iv+bytes,Base64.NO_WRAP)).commit()) { "Could not save profile" }
    }
}
