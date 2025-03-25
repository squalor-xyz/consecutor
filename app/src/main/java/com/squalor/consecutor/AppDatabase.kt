package com.squalor.consecutor

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec

/**
 * Room database with SQLCipher encryption for storing events securely.
 */
@Database(entities = [Event::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun eventDao(): EventDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Provides a singleton instance of the encrypted database.
         * @param context Application context
         */
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val passphrase = getOrGenerateSqlCipherKey(context)
                val factory = SupportFactory(SQLiteDatabase.getBytes(passphrase.toCharArray()))
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "consecutor.db"
                )
                    .openHelperFactory(factory)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private fun getOrGenerateSqlCipherKey(context: Context): String {
            val prefs: SharedPreferences = context.getSharedPreferences("db_prefs", Context.MODE_PRIVATE)
            val encryptedKeyStr = prefs.getString("encrypted_sqlcipher_key", null)
            val keystoreKey = getKeystoreKey()

            if (encryptedKeyStr != null) {
                // Decrypt the existing key
                val encryptedKey = android.util.Base64.decode(encryptedKeyStr, android.util.Base64.DEFAULT)
                val iv = encryptedKey.copyOfRange(0, 16) // First 16 bytes are the IV
                val encryptedSqlCipherKey = encryptedKey.copyOfRange(16, encryptedKey.size)
                val cipher = Cipher.getInstance("AES/CBC/PKCS7Padding")
                cipher.init(Cipher.DECRYPT_MODE, keystoreKey, IvParameterSpec(iv))
                val sqlCipherKey = cipher.doFinal(encryptedSqlCipherKey)
                return String(sqlCipherKey, Charsets.UTF_8)
            } else {
                // Generate a new SQLCipher key
                val sqlCipherKey = ByteArray(32) // 256-bit key
                SecureRandom().nextBytes(sqlCipherKey)
                val sqlCipherKeyStr = android.util.Base64.encodeToString(sqlCipherKey, android.util.Base64.DEFAULT)

                // Encrypt it with the Keystore key
                val cipher = Cipher.getInstance("AES/CBC/PKCS7Padding")
                cipher.init(Cipher.ENCRYPT_MODE, keystoreKey)
                val iv = cipher.iv
                val encryptedSqlCipherKey = cipher.doFinal(sqlCipherKey)
                val storedData = iv + encryptedSqlCipherKey // Combine IV and encrypted key
                val storedDataStr = android.util.Base64.encodeToString(storedData, android.util.Base64.DEFAULT)

                // Store the encrypted key
                prefs.edit().putString("encrypted_sqlcipher_key", storedDataStr).apply()
                return sqlCipherKeyStr
            }
        }

        private fun getKeystoreKey(): SecretKey {
            val keyStore = KeyStore.getInstance("AndroidKeyStore")
            keyStore.load(null)
            val alias = "consecutor_db_key"

            if (keyStore.containsAlias(alias)) {
                val keyEntry = keyStore.getEntry(alias, null) as KeyStore.SecretKeyEntry
                return keyEntry.secretKey
            } else {
                val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
                val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                    alias,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                    .setKeySize(256)
                    .build()
                keyGenerator.init(keyGenParameterSpec)
                return keyGenerator.generateKey()
            }
        }
    }
}