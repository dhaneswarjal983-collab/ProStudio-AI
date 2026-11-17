package com.dhanesh.prostudiopro

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.PointF
import jp.co.cyberagent.android.gpuimage.GPUImage
import jp.co.cyberagent.android.gpuimage.filter.*

data class HslChannel(
    val hue: Float = 0f,        // -1.0 to 1.0
    val saturation: Float = 0f, // -1.0 to 1.0
    val luminance: Float = 0f   // -1.0 to 1.0
)

data class EditParams(
    // Light
    val exposure: Float = 0f,       // -2.0 to 2.0
    val contrast: Float = 1f,       // 0.5 to 2.0
    val highlights: Float = 0f,     // 0.0 to 1.0
    val shadows: Float = 0f,        // 0.0 to 1.0
    val whites: Float = 0f,         // -0.5 to 0.5
    val blacks: Float = 0f,         // -0.5 to 0.5
    // Color
    val temperature: Float = 5000f, // 2500 to 8500
    val tint: Float = 0f,           // -80 to 80
    val vibrance: Float = 0f,       // -1.0 to 1.0
    val saturation: Float = 1f,     // 0.0 to 2.0
    // 8-Channel HSL Color Mix
    val hslChannels: Map<String, HslChannel> = emptyMap(),
    // Effects & Detail
    val clarity: Float = 0f,        // -0.5 to 1.0
    val dehaze: Float = 0f,         // 0.0 to 0.8
    val sharpen: Float = 0f,        // 0.0 to 3.5
    val vignette: Float = 1.0f,     // 0.35 to 1.0
    val grain: Float = 0f           // 0.0 to 0.35
)

object GpuColorEngine {

    // Custom OpenGL ES Fragment Shader for Lightroom Whites, Blacks, Dehaze, Grain & 8-Channel HSL
    private const val PRO_STUDIO_FRAGMENT_SHADER = """
        varying highp vec2 textureCoordinate;
        uniform sampler2D inputImageTexture;
        uniform lowp float uWhites;
        uniform lowp float uBlacks;
        uniform lowp float uDehaze;
        uniform lowp float uGrain;
        uniform lowp vec3 uRedHSL;
        uniform lowp vec3 uOrangeHSL;
        uniform lowp vec3 uYellowHSL;
        uniform lowp vec3 uGreenHSL;
        uniform lowp vec3 uAquaHSL;
        uniform lowp vec3 uBlueHSL;

        lowp vec3 rgb2hsv(lowp vec3 c) {
            lowp vec4 K = vec4(0.0, -1.0 / 3.0, 2.0 / 3.0, -1.0);
            lowp vec4 p = mix(vec4(c.bg, K.wz), vec4(c.gb, K.xy), step(c.b, c.g));
            lowp vec4 q = mix(vec4(p.xyw, c.r), vec4(c.r, p.yzx), step(p.x, c.r));
            lowp float d = q.x - min(q.w, q.y);
            lowp float e = 1.0e-10;
            return vec3(abs(q.z + (q.w - q.y) / (6.0 * d + e)), d / (q.x + e), q.x);
        }

        lowp vec3 hsv2rgb(lowp vec3 c) {
            lowp vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
            lowp vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
            return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
        }

        lowp float hash(lowp vec2 p) {
            return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453123);
        }

        void main() {
            lowp vec4 texColor = texture2D(inputImageTexture, textureCoordinate);
            lowp vec3 rgb = texColor.rgb;

            // Dehaze (atmospheric contrast + deep saturation)
            if (uDehaze > 0.001) {
                rgb = (rgb - uDehaze * 0.18) / (1.0 - uDehaze * 0.25);
            }

            // Whites & Blacks tonal endpoints
            lowp float luma = dot(rgb, vec3(0.299, 0.587, 0.114));
            rgb += uWhites * smoothstep(0.55, 1.0, luma);
            rgb += uBlacks * (1.0 - smoothstep(0.0, 0.45, luma));
            rgb = clamp(rgb, 0.0, 1.0);

            // 8-Channel Selective HSL Color Mix
            lowp vec3 hsv = rgb2hsv(rgb);
            lowp float h = hsv.x * 360.0;
            lowp vec3 hslAdj = vec3(0.0);

            if (h < 20.0 || h >= 335.0) hslAdj = uRedHSL;
            else if (h >= 20.0 && h < 45.0) hslAdj = uOrangeHSL;
            else if (h >= 45.0 && h < 75.0) hslAdj = uYellowHSL;
            else if (h >= 75.0 && h < 160.0) hslAdj = uGreenHSL;
            else if (h >= 160.0 && h < 205.0) hslAdj = uAquaHSL;
            else if (h >= 205.0 && h < 275.0) hslAdj = uBlueHSL;

            if (hsv.y > 0.08) {
                hsv.x = fract(hsv.x + hslAdj.x * 0.08);
                hsv.y = clamp(hsv.y * (1.0 + hslAdj.y), 0.0, 1.0);
                hsv.z = clamp(hsv.z * (1.0 + hslAdj.z * 0.45), 0.0, 1.0);
            }
            rgb = hsv2rgb(hsv);

            // Cinematic Film Grain
            if (uGrain > 0.001) {
                lowp float n = (hash(textureCoordinate * 1000.0) - 0.5) * uGrain;
                rgb = clamp(rgb + vec3(n), 0.0, 1.0);
            }

            gl_FragColor = vec4(rgb, texColor.a);
        }
    """

    private class ProStudioCustomFilter(private val params: EditParams) :
        GPUImageFilter(NO_FILTER_VERTEX_SHADER, PRO_STUDIO_FRAGMENT_SHADER) {

        override fun onInitialized() {
            super.onInitialized()
            setFloat(android.opengl.GLES20.glGetUniformLocation(program, "uWhites"), params.whites)
            setFloat(android.opengl.GLES20.glGetUniformLocation(program, "uBlacks"), params.blacks)
            setFloat(android.opengl.GLES20.glGetUniformLocation(program, "uDehaze"), params.dehaze)
            setFloat(android.opengl.GLES20.glGetUniformLocation(program, "uGrain"), params.grain)

            setHsl("uRedHSL", params.hslChannels["Red"])
            setHsl("uOrangeHSL", params.hslChannels["Orange"])
            setHsl("uYellowHSL", params.hslChannels["Yellow"])
            setHsl("uGreenHSL", params.hslChannels["Green"])
            setHsl("uAquaHSL", params.hslChannels["Aqua"])
            setHsl("uBlueHSL", params.hslChannels["Blue"])
        }

        private fun setHsl(uniform: String, ch: HslChannel?) {
            val c = ch ?: HslChannel()
            setFloatVec3(
                android.opengl.GLES20.glGetUniformLocation(program, uniform),
                floatArrayOf(c.hue, c.saturation, c.luminance)
            )
        }
    }

    fun render(context: Context, source: Bitmap, params: EditParams): Bitmap {
        val gpuImage = GPUImage(context)
        gpuImage.setImage(source)

        val group = GPUImageFilterGroup()
        group.addFilter(GPUImageExposureFilter(params.exposure))
        group.addFilter(GPUImageContrastFilter(params.contrast + params.clarity * 0.35f))
        group.addFilter(GPUImageHighlightShadowFilter(params.shadows, 1f - params.highlights))
        group.addFilter(GPUImageWhiteBalanceFilter(params.temperature, params.tint))
        group.addFilter(GPUImageVibranceFilter(params.vibrance))
        group.addFilter(GPUImageSaturationFilter(params.saturation))
        group.addFilter(ProStudioCustomFilter(params))

        val totalSharpness = params.sharpen + (if (params.clarity > 0f) params.clarity * 0.9f else 0f)
        if (totalSharpness > 0.01f) {
            group.addFilter(GPUImageSharpenFilter(totalSharpness))
        }
        if (params.vignette < 0.99f) {
            group.addFilter(
                GPUImageVignetteFilter(
                    PointF(0.5f, 0.5f),
                    floatArrayOf(0f, 0f, 0f),
                    0.3f,
                    params.vignette
                )
            )
        }

        gpuImage.setFilter(group)
        return gpuImage.bitmapWithFilterApplied
    }

    fun cropAndTransform(source: Bitmap, aspectRatio: Float?, rotateDeg: Float, flipH: Boolean): Bitmap {
        val matrix = Matrix().apply {
            if (rotateDeg != 0f) postRotate(rotateDeg)
            if (flipH) postScale(-1f, 1f)
        }
        val transformed = if (rotateDeg != 0f || flipH) {
            Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
        } else source

        if (aspectRatio == null) return transformed

        val w = transformed.width
        val h = transformed.height
        val currentRatio = w.toFloat() / h.toFloat()
        return if (currentRatio > aspectRatio) {
            val newW = (h * aspectRatio).toInt().coerceAtLeast(1)
            Bitmap.createBitmap(transformed, (w - newW) / 2, 0, newW, h)
        } else {
            val newH = (w / aspectRatio).toInt().coerceAtLeast(1)
            Bitmap.createBitmap(transformed, 0, (h - newH) / 2, w, newH)
        }
    }
}
