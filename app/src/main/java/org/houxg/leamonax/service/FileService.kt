package org.houxg.leamonax.service

import android.graphics.Bitmap
import android.text.TextUtils
import org.houxg.leamonax.Leamonax
import org.houxg.leamonax.model.Account
import java.io.File
import java.util.*

class FileService {
    companion object {

        private fun getExtensions(format: Bitmap.CompressFormat): String {
            return when(format) {
                Bitmap.CompressFormat.JPEG -> ".jpeg"
                Bitmap.CompressFormat.PNG -> ".png"
                Bitmap.CompressFormat.WEBP -> ".webp"
                else -> ".png"
            }
        }

        private fun getRandomName(): String {
            return UUID.randomUUID().toString().toLowerCase(Locale.ENGLISH).replace("-", "")
        }


        fun createLocalImageFile(format: Bitmap.CompressFormat): File? {
            val imageDir = getImageDir() ?: return null
            return File(imageDir, "${getRandomName()}${getExtensions(format)}")
        }

        private fun getUserDir(): File? {
            val account = Account.getCurrent() ?: return null
            return if (TextUtils.isEmpty(account.userId)) {
                null
            } else {
                val dir = File(Leamonax.getContext().filesDir, account.userId)
                if (!dir.isDirectory) {
                    dir.mkdirs()
                }
                dir
            }
        }

        private fun getImageDir(): File? {
            val userDir = getUserDir() ?: return null
            val dir = File(userDir, "images")
            if (!dir.isDirectory) {
                dir.mkdirs()
            }
            return dir
        }
    }
}