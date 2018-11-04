package org.houxg.leamonax.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream

class ImageUtils {
    companion object {
        fun createCompressedImage(path: String, format: Bitmap.CompressFormat, dest: File): Boolean {
            val src = decodeBitmap(path, DisplayUtils.screenWidth()) ?: return false
            return try {
                val out = FileOutputStream(dest)
                src.compress(format, 100, out)
                src.recycle()
                true
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
                false
            }

        }

        private fun decodeBitmap(path: String, maxWidth: Int): Bitmap? {
            val boundsOpt = BitmapFactory.Options()
            boundsOpt.inJustDecodeBounds = true
            BitmapFactory.decodeFile(path, boundsOpt)
            return if (boundsOpt.outWidth > maxWidth) {
                val outputOpt = BitmapFactory.Options()
                outputOpt.inJustDecodeBounds = false
                outputOpt.inSampleSize = calculateSampleSize(boundsOpt.outWidth, maxWidth)
                val temp = BitmapFactory.decodeFile(path, outputOpt)
                val result = Bitmap.createScaledBitmap(temp, maxWidth, maxWidth * boundsOpt.outHeight / boundsOpt.outWidth, false)
                temp.recycle()
                result
            } else {
                BitmapFactory.decodeFile(path)
            }
        }

        private fun calculateSampleSize(origin: Int, expected: Int): Int {
            var sample = 1
            while (origin / sample > expected) {
                sample *= 2
            }
            if (sample > 1) {
                sample /= 2
            }
            return sample
        }
    }
}