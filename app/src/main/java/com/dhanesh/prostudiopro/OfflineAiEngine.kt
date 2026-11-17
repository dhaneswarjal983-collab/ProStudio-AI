package com.dhanesh.prostudiopro

import android.content.ContentValues
import android.content.Context
import android.graphics.*
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions
import kotlinx.coroutines.tasks.await
import java.io.OutputStream

object OfflineAiEngine {

    private val segmenter by lazy {
        val options = SubjectSegmenterOptions.Builder()
            .enableForegroundBitmap()
            .build()
        SubjectSegmentation.getClient(options)
    }

    // 1. Offline AI Background Remover (Transparent PNG Cutout)
    suspend fun removeBackground(source: Bitmap): Bitmap {
        val inputImage = InputImage.fromBitmap(source, 0)
        val result = segmenter.process(inputImage).await()
        return result.foregroundBitmap ?: source
    }

    // 2. Offline AI DSLR Portrait Blur (Subject sharp, background creamy blur)
    suspend fun applyPortraitBlur(context: Context, source: Bitmap, blurRadius: Float = 22f): Bitmap {
        val foreground = removeBackground(source)
        val blurredBg = fastBlur(context, source, blurRadius.coerceIn(1f, 25f))

        val output = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        canvas.drawBitmap(blurredBg, 0f, 0f, null)
        canvas.drawBitmap(foreground, 0f, 0f, null)
        return output
    }

    @Suppress("DEPRECATION")
    private fun fastBlur(context: Context, bitmap: Bitmap, radius: Float): Bitmap {
        val input = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val output = Bitmap.createBitmap(input.width, input.height, Bitmap.Config.ARGB_8888)
        val rs = RenderScript.create(context)
        val script = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs))
        val inAlloc = Allocation.createFromBitmap(rs, input)
        val outAlloc = Allocation.createFromBitmap(rs, output)
        script.setRadius(radius)
        script.setInput(inAlloc)
        script.forEach(outAlloc)
        outAlloc.copyTo(output)
        rs.destroy()
        return output
    }

    // 3. Direct HD Save to Phone Gallery (DCIM/ProStudioAI)
    fun saveToGallery(context: Context, bitmap: Bitmap, isPng: Boolean = false): Boolean {
        return try {
            val filename = "ProStudio_${System.currentTimeMillis()}.${if (isPng) "png" else "jpg"}"
            val mimeType = if (isPng) "image/png" else "image/jpeg"
            val format = if (isPng) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG

            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_DCIM}/ProStudioAI")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return false
            val stream: OutputStream = resolver.openOutputStream(uri) ?: return false
            bitmap.compress(format, 98, stream)
            stream.close()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
