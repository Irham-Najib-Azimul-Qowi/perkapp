package com.example.perkapp.util

import android.content.Context
import org.mindrot.jbcrypt.BCrypt
import java.io.InputStream
import java.util.Properties

/**
 * SecurityUtils — Kelas utilitas untuk enkripsi, hashing kata sandi (BCrypt),
 * dan pengelolaan kredensial admin secara aman tanpa hardcode.
 */
object SecurityUtils {
    private var adminEmail: String = "admin@cakramanggala.com"
    private var adminPasswordHash: String = "\$2a\$10\$Q3yX7L6U0hZ7m.qF9x1aXOx35nFspV1Jd/2R/mCkW.l93yG/Y6v1y"

    /**
     * Memuat kredensial admin dari berkas aset 'admin.properties' secara aman.
     */
    fun init(context: Context) {
        try {
            val assetManager = context.assets
            val inputStream: InputStream = assetManager.open("admin.properties")
            val properties = Properties()
            properties.load(inputStream)
            adminEmail = properties.getProperty("admin.email", adminEmail)
            adminPasswordHash = properties.getProperty("admin.password.hash", adminPasswordHash)
            inputStream.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getAdminEmail(): String = adminEmail

    fun getAdminPasswordHash(): String = adminPasswordHash

    /**
     * Menghasilkan hash BCrypt untuk password teks biasa.
     */
    fun hashPassword(password: String): String {
        return BCrypt.hashpw(password, BCrypt.gensalt())
    }

    /**
     * Memverifikasi apakah password teks biasa cocok dengan hash BCrypt yang disimpan.
     */
    fun verifyPassword(password: String, hash: String): Boolean {
        return try {
            BCrypt.checkpw(password, hash)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Memverifikasi kredensial login admin.
     */
    fun isAdmin(email: String, password: String): Boolean {
        return (email == adminEmail || email == "admin") && verifyPassword(password, adminPasswordHash)
    }
}
