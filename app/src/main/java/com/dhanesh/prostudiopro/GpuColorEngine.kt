package com.dhanesh.prostudiopro

import android.content.Context
import android.graphics.Bitmap
import jp.co.cyberagent.android.gpuimage.GPUImage
import jp.co.cyberagent.android.gpuimage.filter.*

data class EditParams(
    val exposure: Float = 0f,       // -2.0 to 2.0
    val contrast: Float = 1f,       // 0.5 to 2.0
    val highlights: Float = 0f,     // 0.0 to 1.0
    val shadows: Float = 0f,        // 0.0 to 1.0
    val temperature: Float = 5000f, // 2000 to 10000
    val tint: Float = 0f,           // -100 to 100
    val vibrance: Float = 0f,       // -1.2 to 1.2
    val saturation: Float = 1f,     // 0.0 to 2.0
    val sharpen: Float = 0f,        // 0.0 to 4.0
    val vignette: Float = 1.0f      // 0.3 to 1.0 (end radius)
)

object GpuColorEngine {
    fun render(context: Context, source: Bitmap, params: EditParams): Bitmap {
        val gpuImage = GPUImage(context)
        gpuImage.setImage(source)

        val filterGroup = GPUImageFilterGroup()
        filterGroup.addFilter(GPUImageExposureFilter(params.exposure))
        filterGroup.addFilter(GPUImageContrastFilter(params.contrast))
        filterGroup.addFilter(GPUImageHighlightShadowFilter(params.shadows, 1f - params.highlights))
        filterGroup.addFilter(GPUImageWhiteBalanceFilter(params.temperature, params.tint))
        filterGroup.addFilter(GPUImageVibranceFilter(params.vibrance))
        filterGroup.addFilter(GPUImageSaturationFilter(params.saturation))

        if (params.sharpen > 0.01f) {
            filterGroup.addFilter(GPUImageSharpenFilter(params.sharpen))
        }
        if (params.vignette < 0.99f) {
            filterGroup.addFilter(
                GPUImageVignetteFilter(
                    android.graphics.PointF(0.5f, 0.5f),
                    floatArrayOf(0f, 0f, 0f),
                    0.3f,
                    params.vignette
                )
            )
        }

        gpuImage.setFilter(filterGroup)
        return gpuImage.bitmapWithFilterApplied
    }
}
