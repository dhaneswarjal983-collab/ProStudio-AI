package com.prophotostudio.app

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.*

private val Background = Color(0xFF0B0D12)
private val Panel = Color(0xFF171B24)
private val Panel2 = Color(0xFF202635)
private val Accent = Color(0xFFB69CFF)
private val Muted = Color(0xFF9299A8)
private val White = Color(0xFFF7F5FF)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ProPhotoStudioApp() }
    }
}

data class EditValues(
    val brightness: Float = 0f,
    val contrast: Float = 1f,
    val saturation: Float = 1f,
    val exposure: Float = 0f,
    val highlights: Float = 0f,
    val shadows: Float = 0f,
    val whites: Float = 0f,
    val blacks: Float = 0f,
    val temperature: Float = 0f,
    val tint: Float = 0f,
    val hue: Float = 0f,
    val redHsl: Float = 0f,
    val greenHsl: Float = 0f,
    val blueHsl: Float = 0f,
    val sharpness: Float = 0f,
    val clarity: Float = 0f,
    val vignette: Float = 0f,
    val grain: Float = 0f,
    val blur: Float = 0f,
    val maskStrength: Float = 0f,
    val maskSize: Float = 0.55f,
    val rotation: Int = 0,
    val crop: Int = 0
)

private data class Preset(val name: String, val values: EditValues)

private val builtInPresets = listOf(
    Preset("Original", EditValues()),
    Preset("Bright", EditValues(brightness = 0.08f, exposure = 0.25f, contrast = 1.08f)),
    Preset("Warm", EditValues(temperature = 0.35f, saturation = 1.08f, contrast = 1.05f)),
    Preset("Cool", EditValues(temperature = -0.3f, tint = 0.08f, contrast = 1.08f)),
    Preset("Mono", EditValues(saturation = 0f, contrast = 1.15f)),
    Preset("Vivid", EditValues(saturation = 1.35f, contrast = 1.12f, clarity = 0.18f))
)

@Composable
fun ProPhotoStudioApp() {
    var tab by remember { mutableIntStateOf(0) }
    MaterialTheme(colorScheme = darkColorScheme(background = Background, surface = Panel, primary = Accent)) {
        Scaffold(
            containerColor = Background,
            bottomBar = {
                Row(
                    Modifier.fillMaxWidth().background(Panel).navigationBarsPadding().padding(vertical = 9.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf("鈱�" to "Home", "鈻�" to "Edit", "鉁�" to "AI Studio", "鈻�" to "Projects", "鈼�" to "Profile")
                        .forEachIndexed { index, item ->
                            Column(
                                Modifier.clip(RoundedCornerShape(12.dp)).clickable { tab = index }.padding(horizontal = 9.dp, vertical = 3.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(item.first, color = if (tab == index) Accent else Muted, fontSize = 21.sp)
                                Text(item.second, color = if (tab == index) Accent else Muted, fontSize = 9.sp)
                            }
                        }
                }
            }
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                when (tab) {
                    0 -> HomeScreen(onEdit = { tab = 1 }, onAI = { tab = 2 }, onProjects = { tab = 3 })
                    1 -> EditorScreen()
                    2 -> SimpleScreen("AI STUDIO", "AI tools need a separately configured AI service and API key. This screen does not claim to run AI processing.")
                    3 -> SimpleScreen("MY PROJECTS", "Exported photos are saved in your device Pictures/ProPhotoStudio folder. Project-session management is not included in this single-file editor.")
                    else -> SimpleScreen("PROFILE & SETTINGS", "Pro Photo Studio\nLocal photo editing workspace")
                }
            }
        }
    }
}

@Composable
private fun ScreenColumn(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp), content = content)
}

@Composable
private fun Header(title: String, subtitle: String) {
    Spacer(Modifier.height(18.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column {
            Text(title, color = White, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
            Text(subtitle, color = Muted, fontSize = 9.sp, letterSpacing = 2.sp)
        }
        Text("鉁� PRO", color = Accent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(title, color = Muted, fontSize = 11.sp, letterSpacing = 2.sp, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(9.dp))
}

@Composable
private fun HomeScreen(onEdit: () -> Unit, onAI: () -> Unit, onProjects: () -> Unit) {
    ScreenColumn {
        Header("PRO PHOTO STUDIO", "CREATE WITHOUT LIMITS")
        Spacer(Modifier.height(25.dp))
        Text("Your creative space.", color = White, fontSize = 27.sp, fontWeight = FontWeight.Bold)
        Text("Professional photo editing tools.", color = Muted, fontSize = 14.sp)
        Spacer(Modifier.height(22.dp))
        Box(
            Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(24.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF48366F), Color(0xFF222B48), Color(0xFF151925)))).padding(20.dp)
        ) {
            Column(Modifier.align(Alignment.CenterStart)) {
                Text("CREATIVE STUDIO", color = Color(0xFFD5C5FF), fontSize = 10.sp)
                Spacer(Modifier.height(10.dp))
                Text("Make every\npixel matter.", color = White, fontSize = 27.sp, fontWeight = FontWeight.Bold)
                Text("Edit, enhance and export photos", color = Color.LightGray, fontSize = 12.sp)
            }
            Text("鉁�", Modifier.align(Alignment.TopEnd), color = White, fontSize = 42.sp)
        }
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HomeCard(Modifier.weight(1f), "鈻�", "Photo Editor", "Edit photos", onEdit)
            HomeCard(Modifier.weight(1f), "鉁�", "AI Studio", "Service required", onAI)
        }
        Spacer(Modifier.height(18.dp))
        Text("YOUR WORKSPACE", color = Muted, fontSize = 11.sp)
        Spacer(Modifier.height(8.dp))
        HomeCard(Modifier.fillMaxWidth(), "鈻�", "Projects & exports", "Open workspace information", onProjects)
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun HomeCard(modifier: Modifier, icon: String, title: String, subtitle: String, action: () -> Unit) {
    Column(modifier.clip(RoundedCornerShape(18.dp)).background(Panel).clickable { action() }.padding(16.dp)) {
        Text(icon, color = Accent, fontSize = 25.sp)
        Spacer(Modifier.height(9.dp))
        Text(title, color = White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text(subtitle, color = Muted, fontSize = 10.sp)
    }
}

@Composable
private fun SimpleScreen(title: String, message: String) {
    ScreenColumn {
        Header(title, "PRO PHOTO STUDIO")
        Spacer(Modifier.height(28.dp))
        Text(message, color = White, fontSize = 17.sp)
    }
}

@Composable
private fun EditorScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var source by remember { mutableStateOf<Bitmap?>(null) }
    var preview by remember { mutableStateOf<Bitmap?>(null) }
    var values by remember { mutableStateOf(EditValues()) }
    var history by remember { mutableStateOf(listOf(EditValues())) }
    var historyIndex by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var before by remember { mutableStateOf(false) }
    var zoom by remember { mutableFloatStateOf(1f) }
    var panX by remember { mutableFloatStateOf(0f) }
    var panY by remember { mutableFloatStateOf(0f) }
    var customPresets by remember { mutableStateOf(listOf<Preset>()) }

    fun record(next: EditValues) {
        history = (history.take(historyIndex + 1) + next).takeLast(40)
        historyIndex = history.lastIndex
        values = next
        before = false
    }

    fun resetView() {
        zoom = 1f
        panX = 0f
        panY = 0f
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) {
            loading = true
            message = ""
            scope.launch {
                val loaded = withContext(Dispatchers.IO) { loadBitmap(context, uri) }
                source = loaded
                values = EditValues()
                history = listOf(EditValues())
                historyIndex = 0
                preview = loaded
                before = false
                resetView()
                loading = false
                message = if (loaded == null) "Photo load nahi hui. Dusri photo try karein." else "Photo loaded."
            }
        }
    }

    LaunchedEffect(source, values) {
        val bitmap = source
        if (bitmap != null) {
            loading = true
            val rendered = withContext(Dispatchers.Default) { renderBitmap(bitmap, values) }
            preview = rendered
            loading = false
        }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 13.dp)) {
        Header("PHOTO EDITOR", "STAGE 5 鈥� COMPLETE EDITING WORKSPACE")
        Row(Modifier.fillMaxWidth().padding(top = 9.dp, bottom = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("LIVE PREVIEW", color = Muted, fontSize = 10.sp)
            Text(if (before) "ORIGINAL" else "EDITED", color = Accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        Box(
            Modifier.fillMaxWidth().weight(0.85f).clip(RoundedCornerShape(18.dp)).background(Panel2),
            contentAlignment = Alignment.Center
        ) {
            val shown = if (before) source else preview
            if (loading && shown == null) {
                CircularProgressIndicator(color = Accent)
            } else if (shown != null) {
                Image(
                    bitmap = shown.asImageBitmap(),
                    contentDescription = "Photo preview",
                    modifier = Modifier.fillMaxSize()
                        .pointerInput(source) {
                            detectTransformGestures { _, pan, zoomChange, _ ->
                                zoom = (zoom * zoomChange).coerceIn(1f, 5f)
                                if (zoom > 1f) {
                                    panX += pan.x
                                    panY += pan.y
                                } else {
                                    panX = 0f
                                    panY = 0f
                                }
                            }
                        }
                        .graphicsLayer { scaleX = zoom; scaleY = zoom; translationX = panX; translationY = panY },
                    contentScale = ContentScale.Fit
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("鈻�", color = Accent, fontSize = 48.sp)
                    Text("Select a photo to begin", color = White)
                    Text("Your original image remains unchanged", color = Muted, fontSize = 11.sp)
                }
            }
        }
        if (source != null) {
            Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Zoom ${(zoom * 100).toInt()}%", color = Muted, fontSize = 10.sp)
                Text("RESET ZOOM", color = Accent, fontSize = 10.sp, modifier = Modifier.clickable { resetView() })
            }
        }
        Button(
            onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            enabled = !loading,
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Accent),
            shape = RoundedCornerShape(12.dp)
        ) { Text(if (source == null) "锛� Select Photo" else "鈻� Change Photo", color = Background, fontWeight = FontWeight.Bold) }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedButton(
                onClick = { if (historyIndex > 0) { historyIndex--; values = history[historyIndex] } },
                enabled = source != null && historyIndex > 0, modifier = Modifier.weight(1f)
            ) { Text("鈫� Undo") }
            OutlinedButton(
                onClick = { if (historyIndex < history.lastIndex) { historyIndex++; values = history[historyIndex] } },
                enabled = source != null && historyIndex < history.lastIndex, modifier = Modifier.weight(1f)
            ) { Text("鈫� Redo") }
            OutlinedButton(onClick = { if (source != null) record(EditValues()) }, enabled = source != null, modifier = Modifier.weight(1f)) { Text("Reset") }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedButton(onClick = { before = !before }, enabled = source != null, modifier = Modifier.weight(1f)) {
                Text(if (before) "Show Edited" else "Before / After")
            }
            Button(
                onClick = {
                    val bmp = source
                    if (bmp != null) {
                        loading = true
                        scope.launch {
                            val output = withContext(Dispatchers.IO) { renderBitmap(bmp, values) }
                            val result = saveBitmap(context, output)
                            loading = false
                            message = if (result != null) "Export successful: Pictures/ProPhotoStudio" else "Export failed. Please try again."
                        }
                    }
                },
                enabled = source != null && !loading, modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Accent)
            ) { Text("Export", color = Background) }
        }
        if (message.isNotBlank()) Text(message, color = if (message.contains("failed", true) || message.contains("nahi", true)) Color(0xFFFF9999) else Muted, fontSize = 10.sp)

        Column(Modifier.weight(1.15f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(top = 10.dp, bottom = 15.dp)) {
            SectionTitle("LIGHT & COLOR")
            SliderControl("Brightness", values.brightness, -1f..1f, source != null, { record(values.copy(brightness = it)) })
            SliderControl("Contrast", values.contrast, 0f..2f, source != null, { record(values.copy(contrast = it)) }, 1f, true)
            SliderControl("Saturation", values.saturation, 0f..2f, source != null, { record(values.copy(saturation = it)) }, 1f, true)
            SliderControl("Exposure", values.exposure, -2f..2f, source != null, { record(values.copy(exposure = it)) })
            SliderControl("Highlights", values.highlights, -1f..1f, source != null, { record(values.copy(highlights = it)) })
            SliderControl("Shadows", values.shadows, -1f..1f, source != null, { record(values.copy(shadows = it)) })
            SliderControl("Whites", values.whites, -1f..1f, source != null, { record(values.copy(whites = it)) })
            SliderControl("Blacks", values.blacks, -1f..1f, source != null, { record(values.copy(blacks = it)) })
            SliderControl("Temperature", values.temperature, -1f..1f, source != null, { record(values.copy(temperature = it)) })
            SliderControl("Tint", values.tint, -1f..1f, source != null, { record(values.copy(tint = it)) })

            Spacer(Modifier.height(12.dp)); SectionTitle("HSL COLOR MIXER")
            SliderControl("Hue", values.hue, -1f..1f, source != null, { record(values.copy(hue = it)) })
            SliderControl("Red channel", values.redHsl, -1f..1f, source != null, { record(values.copy(redHsl = it)) })
            SliderControl("Green channel", values.greenHsl, -1f..1f, source != null, { record(values.copy(greenHsl = it)) })
            SliderControl("Blue channel", values.blueHsl, -1f..1f, source != null, { record(values.copy(blueHsl = it)) })

            Spacer(Modifier.height(12.dp)); SectionTitle("DETAIL")
            SliderControl("Sharpening", values.sharpness, 0f..1f, source != null, { record(values.copy(sharpness = it)) })
            SliderControl("Clarity", values.clarity, 0f..1f, source != null, { record(values.copy(clarity = it)) })

            Spacer(Modifier.height(12.dp)); SectionTitle("MASKING 鈥� RADIAL SELECTIVE ADJUSTMENT")
            Text("A soft circular mask brightens or darkens the centre region.", color = Muted, fontSize = 10.sp)
            SliderControl("Mask strength", values.maskStrength, -1f..1f, source != null, { record(values.copy(maskStrength = it)) })
            SliderControl("Mask size", values.maskSize, 0.1f..1f, source != null, { record(values.copy(maskSize = it)) }, 0.55f, true)

            Spacer(Modifier.height(12.dp)); SectionTitle("EFFECTS")
            SliderControl("Vignette", values.vignette, 0f..1f, source != null, { record(values.copy(vignette = it)) })
            SliderControl("Film grain", values.grain, 0f..1f, source != null, { record(values.copy(grain = it)) })
            SliderControl("Soft blur", values.blur, 0f..1f, source != null, { record(values.copy(blur = it)) })

            Spacer(Modifier.height(12.dp)); SectionTitle("CROP & GEOMETRY")
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                listOf("Original" to 0, "Square" to 1, "4:5" to 2, "16:9" to 3).forEach { item ->
                    FilterChip(selected = values.crop == item.second, onClick = { if (source != null) record(values.copy(crop = item.second)) }, label = { Text(item.first, fontSize = 10.sp) }, enabled = source != null)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                OutlinedButton(onClick = { if (source != null) record(values.copy(rotation = (values.rotation + 270) % 360)) }, enabled = source != null, modifier = Modifier.weight(1f)) { Text("鈫� Rotate") }
                OutlinedButton(onClick = { if (source != null) record(values.copy(rotation = (values.rotation + 90) % 360)) }, enabled = source != null, modifier = Modifier.weight(1f)) { Text("Rotate 鈫�") }
            }

            Spacer(Modifier.height(12.dp)); SectionTitle("PRESETS")
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                (builtInPresets + customPresets).forEach { preset ->
                    FilterChip(
                        selected = values == preset.values,
                        onClick = { if (source != null) record(preset.values) },
                        label = { Text(preset.name, fontSize = 10.sp) },
                        enabled = source != null
                    )
                }
            }
            OutlinedButton(
                onClick = {
                    if (source != null) {
                        customPresets = (customPresets + Preset("Custom ${customPresets.size + 1}", values)).takeLast(8)
                        message = "Preset saved for this editing session."
                    }
                },
                enabled = source != null, modifier = Modifier.fillMaxWidth()
            ) { Text("Save Current Preset") }
            Text("Note: custom presets are kept only while this editor screen remains open. Export creates a flattened JPG copy.", color = Muted, fontSize = 10.sp)
        }
    }
}

@Composable
private fun SliderControl(
    title: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    enabled: Boolean,
    onCommit: (Float) -> Unit,
    default: Float = 0f,
    percent: Boolean = false
) {
    var moving by remember(title) { mutableFloatStateOf(value) }
    LaunchedEffect(value) { if (!moving.isNaN()) moving = value }
    Column(Modifier.padding(bottom = 3.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, color = White, fontSize = 12.sp)
            Text(if (percent) "${(value * 100).toInt()}%" else String.format("%.2f", value), color = Accent, fontSize = 10.sp)
        }
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = { onCommit(it) },
            valueRange = range,
            enabled = enabled,
            colors = SliderDefaults.colors(thumbColor = Accent, activeTrackColor = Accent)
        )
    }
}

private fun loadBitmap(context: Context, uri: Uri): Bitmap? {
    return try {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            null
        } else {
            var sample = 1
            while (bounds.outWidth / sample > 1600 || bounds.outHeight / sample > 1600) sample *= 2
            val options = BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
        }
    } catch (_: Exception) {
        null
    }
}

private fun renderBitmap(input: Bitmap, v: EditValues): Bitmap {
    var working = input
    if (v.crop != 0) {
        val w = working.width
        val h = working.height
        val targetRatio = when (v.crop) { 1 -> 1f; 2 -> 4f / 5f; else -> 16f / 9f }
        val current = w.toFloat() / h
        val cropW: Int
        val cropH: Int
        if (current > targetRatio) { cropH = h; cropW = (h * targetRatio).toInt().coerceAtLeast(1) }
        else { cropW = w; cropH = (w / targetRatio).toInt().coerceAtLeast(1) }
        working = Bitmap.createBitmap(input, (w - cropW) / 2, (h - cropH) / 2, cropW, cropH)
    }
    if (v.rotation != 0) {
        val matrix = Matrix().apply { postRotate(v.rotation.toFloat()) }
        working = Bitmap.createBitmap(working, 0, 0, working.width, working.height, matrix, true)
    }
    val w = working.width
    val h = working.height
    val pixels = IntArray(w * h)
    working.getPixels(pixels, 0, w, 0, 0, w, h)
    val original = pixels.copyOf()
    val exposure = 2.0.pow(v.exposure.toDouble()).toFloat()
    val contrast = v.contrast
    val brightness = v.brightness * 255f
    val seed = 193
    for (y in 0 until h) {
        for (x in 0 until w) {
            val i = y * w + x
            val c = original[i]
            var r = ((c shr 16) and 255).toFloat()
            var g = ((c shr 8) and 255).toFloat()
            var b = (c and 255).toFloat()
            r = (r - 128f) * contrast + 128f + brightness
            g = (g - 128f) * contrast + 128f + brightness
            b = (b - 128f) * contrast + 128f + brightness
            r *= exposure; g *= exposure; b *= exposure
            val lum = (0.213f * r + 0.715f * g + 0.072f * b).coerceIn(0f, 255f)
            val shadowWeight = (1f - lum / 255f).coerceIn(0f, 1f)
            val highlightWeight = (lum / 255f).coerceIn(0f, 1f)
            val tonal = v.shadows * 48f * shadowWeight + v.highlights * 48f * highlightWeight + v.whites * 32f * highlightWeight + v.blacks * 32f * shadowWeight
            r += tonal + v.temperature * 25f - v.tint * 10f
            g += tonal + v.tint * 18f
            b += tonal - v.temperature * 25f - v.tint * 10f
            val avg = (r + g + b) / 3f
            r = avg + (r - avg) * v.saturation
            g = avg + (g - avg) * v.saturation
            b = avg + (b - avg) * v.saturation

            val nx = (x.toFloat() / w - 0.5f) * 2f
            val ny = (y.toFloat() / h - 0.5f) * 2f
            val radius = sqrt(nx * nx + ny * ny)
            val maskRadius = v.maskSize.coerceIn(0.1f, 1f)
            val mask = (1f - radius / maskRadius).coerceIn(0f, 1f)
            val feather = mask * mask * (3f - 2f * mask)
            val selective = v.maskStrength * feather * 55f
            r += selective; g += selective; b += selective

            val edge = ((radius - 0.25f) / 1.15f).coerceIn(0f, 1f)
            val vignetteFactor = 1f - v.vignette * edge * 0.75f
            r *= vignetteFactor; g *= vignetteFactor; b *= vignetteFactor

            if (v.hue != 0f || v.redHsl != 0f || v.greenHsl != 0f || v.blueHsl != 0f) {
                val maxC = max(r, max(g, b))
                val minC = min(r, min(g, b))
                val delta = maxC - minC
                var hue = if (delta == 0f) 0f else when (maxC) {
                    r -> 60f * (((g - b) / delta) % 6f)
                    g -> 60f * ((b - r) / delta + 2f)
                    else -> 60f * ((r - g) / delta + 4f)
                }
                hue = (hue + 360f) % 360f
                hue = (hue + v.hue * 45f + 360f) % 360f
                val channelAdjust = when {
                    hue < 45f || hue >= 330f -> v.redHsl
                    hue in 75f..165f -> v.greenHsl
                    hue in 190f..275f -> v.blueHsl
                    else -> 0f
                }
                val saturation = if (maxC == 0f) 0f else delta / maxC
                val adjusted = (saturation * (1f + channelAdjust * 0.8f)).coerceIn(0f, 1f)
                val factor = if (saturation > 0.001f) adjusted / saturation else 1f
                r = avg + (r - avg) * factor
                g = avg + (g - avg) * factor
                b = avg + (b - avg) * factor
            }
            if (v.grain > 0f) {
                val noise = (((i * 1103515245L + seed * 12345L) shr 16).toInt() and 255) / 127.5f - 1f
                val amount = noise * v.grain * 22f
                r += amount; g += amount; b += amount
            }
            pixels[i] = android.graphics.Color.argb(
                android.graphics.Color.alpha(c),
                r.roundToInt().coerceIn(0, 255),
                g.roundToInt().coerceIn(0, 255),
                b.roundToInt().coerceIn(0, 255)
            )
        }
    }
    if (v.sharpness > 0f || v.clarity > 0f) {
        val strength = (v.sharpness * 0.8f + v.clarity * 0.35f).coerceAtMost(1.1f)
        val copy = pixels.copyOf()
        for (y in 1 until h - 1) for (x in 1 until w - 1) {
            val i = y * w + x
            val center = copy[i]
            val neighbours = intArrayOf(copy[i - 1], copy[i + 1], copy[i - w], copy[i + w])
            fun channel(shift: Int): Int {
                val cv = (center shr shift) and 255
                val avg = neighbours.map { (it shr shift) and 255 }.average().toFloat()
                return (cv + (cv - avg) * strength).roundToInt().coerceIn(0, 255)
            }
            pixels[i] = android.graphics.Color.argb(255, channel(16), channel(8), channel(0))
        }
    }
    if (v.blur > 0f && w > 2 && h > 2) {
        val copy = pixels.copyOf()
        val radius = if (v.blur > 0.65f) 2 else 1
        for (y in 1 until h - 1) for (x in 1 until w - 1) {
            val i = y * w + x
            val c = copy[i]
            fun avgChannel(shift: Int): Int {
                var sum = 0
                var count = 0
                for (dy in -radius..radius) for (dx in -radius..radius) {
                    sum += (copy[(y + dy).coerceIn(0, h - 1) * w + (x + dx).coerceIn(0, w - 1)] shr shift) and 255
                    count++
                }
                val blurred = sum / count
                val base = (c shr shift) and 255
                return (base * (1f - v.blur) + blurred * v.blur).roundToInt().coerceIn(0, 255)
            }
            pixels[i] = android.graphics.Color.argb(255, avgChannel(16), avgChannel(8), avgChannel(0))
        }
    }
    return Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).also { it.setPixels(pixels, 0, w, 0, 0, w, h) }
}

private fun saveBitmap(context: Context, bitmap: Bitmap): Uri? {
    return try {
        val name = "ProPhotoStudio_${System.currentTimeMillis()}.jpg"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, name)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/ProPhotoStudio")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return null
        val written = resolver.openOutputStream(uri)?.use { stream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)
        } ?: false
        if (!written) {
            resolver.delete(uri, null, null)
            return null
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val finish = ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }
            resolver.update(uri, finish, null, null)
        }
        uri
    } catch (_: Exception) { null }
}
