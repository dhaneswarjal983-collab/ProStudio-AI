package com.prophotostudio.app

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.horizontalScroll
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
import kotlin.math.abs
import kotlin.math.pow

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

@Composable
fun ProPhotoStudioApp() {
    var tab by remember { mutableIntStateOf(0) }
    MaterialTheme(colorScheme = darkColorScheme(background = Background, surface = Panel, primary = Accent)) {
        Scaffold(containerColor = Background, bottomBar = { BottomNavigation(tab) { tab = it } }) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                when (tab) {
                    0 -> HomeScreen({ tab = 1 }, { tab = 2 }, { tab = 3 })
                    1 -> EditorScreen()
                    2 -> AIStudioScreen()
                    3 -> ProjectsScreen()
                    else -> ProfileScreen()
                }
            }
        }
    }
}

@Composable
private fun BottomNavigation(selected: Int, onSelect: (Int) -> Unit) {
    val items = listOf("⌂" to "Home", "▧" to "Edit", "✧" to "AI Studio", "▤" to "Projects", "◉" to "Profile")
    Row(Modifier.fillMaxWidth().background(Panel).navigationBarsPadding().padding(vertical = 10.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
        items.forEachIndexed { i, item ->
            Column(Modifier.clip(RoundedCornerShape(12.dp)).clickable { onSelect(i) }.padding(horizontal = 8.dp, vertical = 3.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(item.first, color = if (selected == i) Accent else Muted, fontSize = 21.sp)
                Text(item.second, color = if (selected == i) Accent else Muted, fontSize = 9.sp)
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
        Text("✦ PRO", color = Accent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(title, color = Muted, fontSize = 11.sp, letterSpacing = 2.sp, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(9.dp))
}

@Composable
private fun HomeScreen(onEdit: () -> Unit, onAi: () -> Unit, onProjects: () -> Unit) {
    ScreenColumn {
        Header("PRO PHOTO STUDIO", "CREATE WITHOUT LIMITS")
        Spacer(Modifier.height(28.dp))
        Text("Your creative space.", color = White, fontSize = 27.sp, fontWeight = FontWeight.Bold)
        Text("Turn your imagination into art.", color = Muted, fontSize = 14.sp)
        Spacer(Modifier.height(22.dp))
        Box(Modifier.fillMaxWidth().height(185.dp).clip(RoundedCornerShape(24.dp)).background(Brush.linearGradient(listOf(Color(0xFF48366F), Color(0xFF222B48), Color(0xFF151925)))).padding(20.dp)) {
            Column(Modifier.align(Alignment.CenterStart)) {
                Text("CREATIVE STUDIO", color = Color(0xFFD5C5FF), fontSize = 10.sp)
                Spacer(Modifier.height(10.dp))
                Text("Make every\npixel matter.", color = White, fontSize = 27.sp, fontWeight = FontWeight.Bold, lineHeight = 33.sp)
                Text("Professional photo editing", color = Color.LightGray, fontSize = 12.sp)
            }
            Text("✦", Modifier.align(Alignment.TopEnd), color = Color(0xFFE2D5FF), fontSize = 45.sp)
        }
        Spacer(Modifier.height(25.dp)); SectionTitle("CREATE")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HomeCard(Modifier.weight(1f), "▧", "Photo Editor", "Professional tools", Color(0xFF59458D), onEdit)
            HomeCard(Modifier.weight(1f), "✧", "AI Studio", "Creative workspace", Color(0xFF285A65), onAi)
        }
        Spacer(Modifier.height(25.dp)); SectionTitle("CREATIVE TOOLS")
        listOf("Light & Exposure" to "Control brightness and tones", "Color Mixer" to "Adjust color and mood", "Creative Effects" to "Blur, grain, glow and glitch", "Presets" to "Cinematic looks and styles").forEach { (a,b) -> SimpleRow("✦", a, b) }
        Spacer(Modifier.height(24.dp)); SectionTitle("YOUR WORKSPACE")
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Panel).clickable { onProjects() }.padding(18.dp)) {
            Column { Text("Your projects", color = White, fontWeight = FontWeight.Bold); Text("Open your creative library", color = Muted, fontSize = 12.sp); Text("VIEW PROJECTS  →", color = Accent, fontSize = 11.sp) }
        }
        Spacer(Modifier.height(25.dp))
    }
}

@Composable
private fun HomeCard(modifier: Modifier, icon: String, title: String, subtitle: String, color: Color, click: () -> Unit) {
    Column(modifier.clip(RoundedCornerShape(20.dp)).background(Panel).clickable { click() }.padding(15.dp)) {
        Box(Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(color), contentAlignment = Alignment.Center) { Text(icon, color = White, fontSize = 24.sp) }
        Spacer(Modifier.height(13.dp)); Text(title, color = White, fontWeight = FontWeight.Bold, fontSize = 14.sp); Text(subtitle, color = Muted, fontSize = 10.sp)
    }
}

private data class Adjustments(
    val brightness: Float = 0f, val contrast: Float = 1f, val saturation: Float = 1f,
    val exposure: Float = 0f, val warmth: Float = 0f, val tint: Float = 0f,
    val highlights: Float = 0f, val shadows: Float = 0f, val blur: Float = 0f,
    val vignette: Float = 0f, val grain: Float = 0f, val hue: Float = 0f
)

@Composable
private fun EditorScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var source by remember { mutableStateOf<Bitmap?>(null) }
    var rendered by remember { mutableStateOf<Bitmap?>(null) }
    var edits by remember { mutableStateOf(Adjustments()) }
    var history by remember { mutableStateOf(listOf(Adjustments())) }
    var historyIndex by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var before by remember { mutableStateOf(false) }
    var zoom by remember { mutableFloatStateOf(1f) }
    var panX by remember { mutableFloatStateOf(0f) }
    var panY by remember { mutableFloatStateOf(0f) }

    fun commit(v: Adjustments) {
        val h = history.take(historyIndex + 1) + v
        history = h; historyIndex = h.lastIndex; edits = v
    }
    fun rerender(v: Adjustments) {
        val b = source ?: return
        scope.launch { rendered = withContext(Dispatchers.Default) { renderBitmap(b, v) } }
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) scope.launch {
            loading = true; message = ""
            val bitmap = withContext(Dispatchers.IO) { loadBitmap(context, uri) }
            source = bitmap; edits = Adjustments(); history = listOf(Adjustments()); historyIndex = 0
            rendered = bitmap; zoom = 1f; panX = 0f; panY = 0f; loading = false
            if (bitmap == null) message = "Photo load nahi hui. Dusri photo try karein."
        }
    }

    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        zoom = (zoom * zoomChange).coerceIn(1f, 5f)
        panX += panChange.x
        panY += panChange.y
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 14.dp)) {
        Header("PHOTO EDITOR", "PROFESSIONAL EDITING WORKSPACE")
        Spacer(Modifier.height(12.dp))
        Box(Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(18.dp)).background(Color(0xFF080A0F)), contentAlignment = Alignment.Center) {
            val shown = if (before) source else rendered
            if (shown != null) {
                Image(
                    bitmap = shown.asImageBitmap(),
                    contentDescription = "Photo preview",
                    modifier = Modifier.fillMaxSize()
                        .graphicsLayer { scaleX = zoom; scaleY = zoom; translationX = panX; translationY = panY }
                        .transformable(transformState)
                        .pointerInput(Unit) { detectTapGestures(onDoubleTap = { zoom = 1f; panX = 0f; panY = 0f }) },
                    contentScale = ContentScale.Fit
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("▧", color = Accent, fontSize = 48.sp)
                    Text("Choose a photo to start", color = White, fontSize = 15.sp)
                    Text("Your editing preview will appear here", color = Muted, fontSize = 11.sp)
                }
            }
            if (loading) CircularProgressIndicator(color = Accent)
            if (message.isNotEmpty()) Text(message, Modifier.align(Alignment.BottomCenter).padding(12.dp), color = Color.Red, fontSize = 11.sp)
            if (shown != null) Text(if (before) "BEFORE" else "AFTER", Modifier.align(Alignment.TopStart).padding(10.dp).background(Panel.copy(alpha = 0.8f), RoundedCornerShape(8.dp)).padding(7.dp), color = White, fontSize = 10.sp)
        }

        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Accent)) { Text("Open Photo", color = Background) }
            OutlinedButton(onClick = { before = !before }, enabled = source != null) { Text("Before / After") }
            OutlinedButton(onClick = { zoom = 1f; panX = 0f; panY = 0f }) { Text("Fit") }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = { if (historyIndex > 0) { historyIndex--; edits = history[historyIndex]; rerender(edits) } }, enabled = historyIndex > 0) { Text("↶ Undo") }
            TextButton(onClick = { if (historyIndex < history.lastIndex) { historyIndex++; edits = history[historyIndex]; rerender(edits) } }, enabled = historyIndex < history.lastIndex) { Text("↷ Redo") }
            TextButton(onClick = { val reset = Adjustments(); commit(reset); rerender(reset); before = false }, enabled = source != null) { Text("Reset") }
            TextButton(onClick = { val b = rendered; if (b != null) scope.launch { val ok = withContext(Dispatchers.IO) { saveBitmap(context, b) }; message = if (ok) "Saved to Pictures / Pro Photo Studio" else "Save failed" } }, enabled = rendered != null) { Text("Save") }
        }

        Spacer(Modifier.height(8.dp))
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            SectionTitle("LIGHT & COLOR")
            EditorSlider("Brightness", edits.brightness, -1f..1f, source != null, { edits = edits.copy(brightness = it); rerender(edits) }, { commit(edits) })
            EditorSlider("Contrast", edits.contrast, 0.2f..2.5f, source != null, { edits = edits.copy(contrast = it); rerender(edits) }, { commit(edits) })
            EditorSlider("Saturation", edits.saturation, 0f..2.5f, source != null, { edits = edits.copy(saturation = it); rerender(edits) }, { commit(edits) })
            EditorSlider("Exposure", edits.exposure, -2f..2f, source != null, { edits = edits.copy(exposure = it); rerender(edits) }, { commit(edits) })
            EditorSlider("Highlights", edits.highlights, -1f..1f, source != null, { edits = edits.copy(highlights = it); rerender(edits) }, { commit(edits) })
            EditorSlider("Shadows", edits.shadows, -1f..1f, source != null, { edits = edits.copy(shadows = it); rerender(edits) }, { commit(edits) })
            EditorSlider("Warmth", edits.warmth, -1f..1f, source != null, { edits = edits.copy(warmth = it); rerender(edits) }, { commit(edits) })
            EditorSlider("Tint", edits.tint, -1f..1f, source != null, { edits = edits.copy(tint = it); rerender(edits) }, { commit(edits) })
            EditorSlider("Hue", edits.hue, -180f..180f, source != null, { edits = edits.copy(hue = it); rerender(edits) }, { commit(edits) })
            Spacer(Modifier.height(14.dp)); SectionTitle("CREATIVE EFFECTS")
            EditorSlider("Blur", edits.blur, 0f..12f, source != null, { edits = edits.copy(blur = it); rerender(edits) }, { commit(edits) })
            EditorSlider("Vignette", edits.vignette, 0f..1f, source != null, { edits = edits.copy(vignette = it); rerender(edits) }, { commit(edits) })
            EditorSlider("Grain", edits.grain, 0f..1f, source != null, { edits = edits.copy(grain = it); rerender(edits) }, { commit(edits) })
            Spacer(Modifier.height(14.dp)); SectionTitle("ONE-TAP LOOKS")
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Cinematic" to Adjustments(contrast = 1.18f, saturation = 0.85f, warmth = -0.12f, vignette = 0.25f), "Golden Hour" to Adjustments(saturation = 1.12f, warmth = 0.35f, highlights = 0.15f), "B&W" to Adjustments(saturation = 0f, contrast = 1.15f), "Vintage" to Adjustments(contrast = 0.9f, saturation = 0.75f, warmth = 0.2f, grain = 0.08f, vignette = 0.2f)).forEach { (name, preset) ->
                    OutlinedButton(onClick = { commit(preset); before = false; rerender(preset) }, enabled = source != null) { Text(name) }
                }
            }
            Spacer(Modifier.height(25.dp))
        }
    }
}@Composable
private fun EditorSlider(
    title: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    enabled: Boolean,
    change: (Float) -> Unit,
    finish: () -> Unit
) {
    Column {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title, color = White, fontSize = 13.sp)
            Text(String.format("%.2f", value), color = Accent, fontSize = 11.sp)
        }
        Slider(
            value = value,
            onValueChange = change,
            onValueChangeFinished = finish,
            valueRange = range,
            enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = Accent,
                activeTrackColor = Accent
            )
        )
    }
}

private fun renderBitmap(source: Bitmap, a: Adjustments): Bitmap {
    val w = source.width
    val h = source.height
    val input = IntArray(w * h)
    source.getPixels(input, 0, w, 0, 0, w, h)

    val output = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    val contrast = a.contrast
    val exposure = 2.0.pow(a.exposure.toDouble()).toFloat()
    val sat = a.saturation
    val warmth = a.warmth * 28f
    val tint = a.tint * 20f

    for (i in input.indices) {
        val c = input[i]
        val alpha = c ushr 24

        var r = ((c shr 16) and 255).toFloat()
        var g = ((c shr 8) and 255).toFloat()
        var b = (c and 255).toFloat()

        val lum = (r * 0.299f + g * 0.587f + b * 0.114f) / 255f
        val tone = (a.shadows * (1f - lum) + a.highlights * lum) * 35f

        r = ((r - 128f) * contrast + 128f + a.brightness * 100f + tone) * exposure + warmth - tint * 0.4f
        g = ((g - 128f) * contrast + 128f + a.brightness * 100f + tone) * exposure + tint
        b = ((b - 128f) * contrast + 128f + a.brightness * 100f + tone) * exposure - warmth - tint * 0.4f

        val gray = r * 0.299f + g * 0.587f + b * 0.114f
        r = gray + (r - gray) * sat
        g = gray + (g - gray) * sat
        b = gray + (b - gray) * sat

        if (abs(a.hue) > 0.01f) {
            val hsv = FloatArray(3)
            android.graphics.Color.colorToHSV(
                android.graphics.Color.rgb(
                    r.toInt().coerceIn(0, 255),
                    g.toInt().coerceIn(0, 255),
                    b.toInt().coerceIn(0, 255)
                ),
                hsv
            )
            hsv[0] = (hsv[0] + a.hue + 360f) % 360f
            val shifted = android.graphics.Color.HSVToColor(alpha, hsv)
            r = ((shifted shr 16) and 255).toFloat()
            g = ((shifted shr 8) and 255).toFloat()
            b = (shifted and 255).toFloat()
        }

        val x = i % w
        val y = i / w

        if (a.vignette > 0f) {
            val dx = (x - w / 2f) / (w / 2f)
            val dy = (y - h / 2f) / (h / 2f)
            val shade = (
                1f - a.vignette * ((dx * dx + dy * dy) * 0.38f)
                    .coerceIn(0f, 1f)
                )
            r *= shade
            g *= shade
            b *= shade
        }

        if (a.grain > 0f) {
            val noise = (
                (((i * 1103515245L + 12345L) shr 16).toInt() and 255)
                    / 127.5f - 1f
                )
            val n = noise * a.grain * 100f
            r += n
            g += n
            b += n
        }

        output.setPixel(
            x,
            y,
            android.graphics.Color.argb(
                alpha,
                r.toInt().coerceIn(0, 255),
                g.toInt().coerceIn(0, 255),
                b.toInt().coerceIn(0, 255)
            )
        )
    }

    if (a.blur > 0.1f) {
        val blurred = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(blurred)

        paint.alpha = (a.blur / 12f * 150f).toInt().coerceIn(0, 150)
        canvas.drawBitmap(output, 0f, 0f, paint)

        paint.alpha = 255
        paint.maskFilter = android.graphics.BlurMaskFilter(
            a.blur,
            android.graphics.BlurMaskFilter.Blur.NORMAL
        )
        canvas.drawBitmap(output, 0f, 0f, paint)
        paint.maskFilter = null

        return blurred
    }

    return output
}

private fun loadBitmap(context: Context, uri: Uri): Bitmap? {
    return try {
        val bounds = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }

        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, bounds)
        }

        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            null
        } else {
            var sample = 1
            while (
                bounds.outWidth / sample > 2048 ||
                bounds.outHeight / sample > 2048
            ) {
                sample *= 2
            }

            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(
                    it,
                    null,
                    BitmapFactory.Options().apply {
                        inSampleSize = sample
                    }
                )
            }
        }
    } catch (_: Exception) {
        null
    }
}

private fun saveBitmap(context: Context, bitmap: Bitmap): Boolean {
    return try {
        val values = ContentValues().apply {
            put(
                MediaStore.Images.Media.DISPLAY_NAME,
                "ProPhoto_${System.currentTimeMillis()}.jpg"
            )
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")

            if (Build.VERSION.SDK_INT >= 29) {
                put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    "Pictures/Pro Photo Studio"
                )
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val uri = context.contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            values
        )

        if (uri == null) {
            false
        } else {
            val saved = context.contentResolver.openOutputStream(uri)?.use {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it)
            } ?: false

            if (Build.VERSION.SDK_INT >= 29) {
                val done = ContentValues().apply {
                    put(MediaStore.Images.Media.IS_PENDING, 0)
                }
                context.contentResolver.update(uri, done, null, null)
            }

            saved
        }
    } catch (_: Exception) {
        false
    }
}

@Composable
private fun SimpleRow(icon: String, title: String, subtitle: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(15.dp))
            .background(Panel)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, color = Accent, fontSize = 23.sp)
        Spacer(Modifier.width(13.dp))
        Column {
            Text(
                title,
                color = White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(subtitle, color = Muted, fontSize = 10.sp)
        }
    }
}

@Composable
private fun AIStudioScreen() {
    ScreenColumn {
        Header("AI STUDIO", "INTELLIGENT CREATIVE TOOLS")
        Spacer(Modifier.height(25.dp))

        Text(
            "Imagine. Create. Transform.",
            color = White,
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Your intelligent creative workspace.",
            color = Muted,
            fontSize = 13.sp
        )

        Spacer(Modifier.height(22.dp))

        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF49336E), Color(0xFF1B4553))
                    )
                )
                .padding(22.dp)
        ) {
            Column {
                Text("✦ AI CREATIVE LAB", color = Color(0xFFE0D2FF), fontSize = 11.sp)
                Text(
                    "Your ideas,\nreimagined.",
                    color = White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(Modifier.height(25.dp))
        SectionTitle("PLANNED AI TOOLS")

        listOf(
            "AI Image Generator" to "Text-to-image creation",
            "Background Removal" to "Subject separation",
            "AI Enhancement" to "Detail enhancement",
            "Object Removal" to "Intelligent object editing",
            "Portrait Studio" to "Portrait enhancement",
            "AI Upscale" to "Resolution enhancement",
            "AI Relight" to "Lighting adjustments",
            "Generative Expand" to "Expand composition"
        ).forEach {
            SimpleRow("✧", it.first, it.second)
            Spacer(Modifier.height(9.dp))
        }

        Text(
            "These AI tools need a real on-device model or online AI service. They are not active yet.",
            color = Muted,
            fontSize = 11.sp
        )
        Spacer(Modifier.height(25.dp))
    }
}

@Composable
private fun ProjectsScreen() {
    ScreenColumn {
        Header("MY PROJECTS", "YOUR CREATIVE LIBRARY")
        Spacer(Modifier.height(25.dp))

        Text(
            "Your workspace.",
            color = White,
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Your exported photos are saved in your Gallery.",
            color = Muted,
            fontSize = 13.sp
        )

        Spacer(Modifier.height(22.dp))

        Box(
            Modifier
                .fillMaxWidth()
                .height(190.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(Panel),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("▤", color = Accent, fontSize = 48.sp)
                Text("No in-app projects yet", color = White, fontWeight = FontWeight.Bold)
                Text("Exported photos are in Gallery", color = Muted, fontSize = 12.sp)
            }
        }

        Spacer(Modifier.height(24.dp))
        SectionTitle("LIBRARY")
        SimpleRow("▧", "Edited Photos", "Saved in Pictures / Pro Photo Studio")
        Spacer(Modifier.height(10.dp))
        SimpleRow("✧", "AI Creations", "Available after AI integration")
        Spacer(Modifier.height(25.dp))
    }
}

@Composable
private fun ProfileScreen() {
    ScreenColumn {
        Header("PROFILE & SETTINGS", "PERSONALIZE YOUR STUDIO")
        Spacer(Modifier.height(25.dp))

        Box(
            Modifier
                .size(78.dp)
                .clip(CircleShape)
                .background(Panel2),
            contentAlignment = Alignment.Center
        ) {
            Text("◉", color = Accent, fontSize = 38.sp)
        }

        Spacer(Modifier.height(12.dp))
        Text(
            "Creative User",
            color = White,
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold
        )
        Text("Welcome to Pro Photo Studio", color = Muted, fontSize = 12.sp)

        Spacer(Modifier.height(27.dp))
        SectionTitle("APP SETTINGS")
        SimpleRow("◐", "Appearance", "Premium dark interface")
        Spacer(Modifier.height(10.dp))
        SimpleRow("⚙", "General Settings", "More preferences coming soon")
        Spacer(Modifier.height(10.dp))
        SimpleRow("⇩", "Export Settings", "JPEG high quality export")
        Spacer(Modifier.height(25.dp))

        Text(
            "PRO PHOTO STUDIO",
            color = Accent,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
        Text("Creative tools. One studio.", color = Muted, fontSize = 11.sp)
        Spacer(Modifier.height(25.dp))
    }
}
