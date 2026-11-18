package com.dhanesh.prostudiopro

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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

private val BG = Color(0xFF0B0D12)
private val PANEL = Color(0xFF171B24)
private val ACCENT = Color(0xFFB69CFF)
private val WHITE = Color(0xFFF7F5FF)
private val MUTED = Color(0xFF9299A8)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    background = BG,
                    surface = PANEL,
                    primary = ACCENT
                )
            ) {
                ProStudioApp()
            }
        }
    }
}

@Composable
fun ProStudioApp() {
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        containerColor = BG,
        bottomBar = {
            NavigationBar(containerColor = PANEL) {
                val items = listOf(
                    "Home", "Editor", "AI Studio", "Projects", "Profile"
                )
                val icons = listOf("⌂", "▧", "✧", "▤", "◉")

                items.forEachIndexed { index, title ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = {
                            Text(
                                icons[index],
                                fontSize = 20.sp,
                                color = if (selectedTab == index) ACCENT else MUTED
                            )
                        },
                        label = {
                            Text(title, fontSize = 9.sp)
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = ACCENT,
                            selectedTextColor = ACCENT,
                            indicatorColor = Color(0xFF302747),
                            unselectedTextColor = MUTED
                        )
                    )
                }
            }
        }
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (selectedTab) {
                0 -> HomeScreen(
                    onEdit = { selectedTab = 1 },
                    onAI = { selectedTab = 2 },
                    onProjects = { selectedTab = 3 }
                )
                1 -> EditorScreen()
                2 -> AIStudioScreen()
                3 -> ProjectsScreen()
                4 -> ProfileScreen()
            }
        }
    }
}

@Composable
private fun Header(title: String, subtitle: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 18.dp, bottom = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                title,
                color = WHITE,
                fontSize = 19.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                subtitle,
                color = MUTED,
                fontSize = 9.sp,
                letterSpacing = 2.sp
            )
        }
        Text(
            "✦ PRO",
            color = ACCENT,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ScreenColumn(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp),
        content = content
    )
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        title,
        color = MUTED,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 2.sp
    )
    Spacer(Modifier.height(10.dp))
}

@Composable
private fun HomeScreen(
    onEdit: () -> Unit,
    onAI: () -> Unit,
    onProjects: () -> Unit
) {
    ScreenColumn {
        Header("PRO PHOTO STUDIO", "YOUR CREATIVE WORKSPACE")

        Text(
            "Create something amazing.",
            color = WHITE,
            fontSize = 27.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Professional editing starts here.",
            color = MUTED
        )

        Spacer(Modifier.height(22.dp))

        Box(
            Modifier
                .fillMaxWidth()
                .height(190.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color(0xFF594080),
                            Color(0xFF263C59),
                            Color(0xFF151925)
                        )
                    )
                )
                .padding(22.dp)
        ) {
            Column(Modifier.align(Alignment.CenterStart)) {
                Text("CREATIVE STUDIO", color = Color(0xFFE0D2FF))
                Spacer(Modifier.height(10.dp))
                Text(
                    "Make every\npixel matter.",
                    color = WHITE,
                    fontSize = 29.sp,
                    fontWeight = FontWeight.Bold
                )
                Text("Your photo. Your creativity.", color = Color.LightGray)
            }
            Text(
                "✦",
                Modifier.align(Alignment.TopEnd),
                color = WHITE,
                fontSize = 45.sp
            )
        }

        Spacer(Modifier.height(25.dp))
        SectionTitle("CREATE")

        StudioCard("▧", "Photo Editor", "Professional photo editing", onEdit)
        Spacer(Modifier.height(10.dp))
        StudioCard("✧", "AI Studio", "Explore AI creative tools", onAI)
        Spacer(Modifier.height(10.dp))
        StudioCard("▤", "My Projects", "Your saved creative work", onProjects)

        Spacer(Modifier.height(25.dp))
        SectionTitle("TOOLS")

        listOf(
            "Light & Exposure",
            "Color Mixer",
            "Creative Effects",
            "Portrait Editing",
            "Professional Presets"
        ).forEach {
            Text(
                "✦   $it",
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                color = WHITE
            )
        }

        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun StudioCard(
    icon: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(17.dp))
            .background(PANEL)
            .clickable { onClick() }
            .padding(17.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF302747)),
            contentAlignment = Alignment.Center
        ) {
            Text(icon, color = ACCENT, fontSize = 25.sp)
        }

        Spacer(Modifier.width(14.dp))

        Column {
            Text(title, color = WHITE, fontWeight = FontWeight.Bold)
            Text(subtitle, color = MUTED, fontSize = 11.sp)
        }
    }
}

private data class PhotoSettings(
    val brightness: Float = 0f,
    val contrast: Float = 1f,
    val saturation: Float = 1f,
    val exposure: Float = 0f,
    val highlights: Float = 0f,
    val shadows: Float = 0f,
    val warmth: Float = 0f,
    val tint: Float = 0f,
    val hue: Float = 0f,
    val vignette: Float = 0f,
    val grain: Float = 0f,
    val blur: Float = 0f
)

@Composable
private fun EditorScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var original by remember { mutableStateOf<Bitmap?>(null) }
    var preview by remember { mutableStateOf<Bitmap?>(null) }

    var settings by remember { mutableStateOf(PhotoSettings()) }
    var history by remember { mutableStateOf(listOf(PhotoSettings())) }
    var historyIndex by remember { mutableIntStateOf(0) }

    var loading by remember { mutableStateOf(false) }
    var showBefore by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }

    var zoom by remember { mutableFloatStateOf(1f) }
    var panX by remember { mutableFloatStateOf(0f) }
    var panY by remember { mutableFloatStateOf(0f) }

    fun saveHistory(value: PhotoSettings) {
        val newHistory = history.take(historyIndex + 1) + value
        history = newHistory
        historyIndex = newHistory.lastIndex
        settings = value
    }

    fun updatePreview(value: PhotoSettings) {
        val bitmap = original ?: return

        scope.launch {
            loading = true
            val result = withContext(Dispatchers.Default) {
                processPhoto(bitmap, value)
            }
            preview = result
            loading = false
        }
    }

    fun applyPreset(value: PhotoSettings) {
        saveHistory(value)
        showBefore = false
        updatePreview(value)
    }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                loading = true
                message = ""

                val bitmap = withContext(Dispatchers.IO) {
                    readPhoto(context, uri)
                }

                original = bitmap
                preview = bitmap
                settings = PhotoSettings()
                history = listOf(PhotoSettings())
                historyIndex = 0
                zoom = 1f
                panX = 0f
                panY = 0f

                if (bitmap == null) {
                    message = "Photo load nahi hui."
                }

                loading = false
            }
        }
    }

    val transformState = rememberTransformableState {
            zoomChange, panChange, _ ->
        zoom = (zoom * zoomChange).coerceIn(1f, 5f)
        panX += panChange.x
        panY += panChange.y
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp)
    ) {
        Header("PHOTO EDITOR", "PROFESSIONAL WORKSPACE")

        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFF05070B)),
            contentAlignment = Alignment.Center
        ) {
            val shown = if (showBefore) original else preview

            if (shown != null) {
                Image(
                    bitmap = shown.asImageBitmap(),
                    contentDescription = "Photo preview",
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = zoom
                            scaleY = zoom
                            translationX = panX
                            translationY = panY
                        }
                        .transformable(transformState)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onDoubleTap = {
                                    zoom = 1f
                                    panX = 0f
                                    panY = 0f
                                }
                            )
                        },
                    contentScale = ContentScale.Fit
                )

                Text(
                    if (showBefore) "BEFORE" else "AFTER",
                    Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                        .background(PANEL, RoundedCornerShape(7.dp))
                        .padding(7.dp),
                    color = WHITE,
                    fontSize = 10.sp
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("▧", color = ACCENT, fontSize = 45.sp)
                    Text("Choose a photo", color = WHITE)
                    Text("Your preview appears here", color = MUTED, fontSize = 11.sp)
                }
            }

            if (loading) {
                CircularProgressIndicator(color = ACCENT)
            }

            if (message.isNotEmpty()) {
                Text(
                    message,
                    Modifier.align(Alignment.BottomCenter).padding(10.dp),
                    color = Color.Red
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Button(
                onClick = {
                    picker.launch(
                        PickVisualMediaRequest(
                            ActivityResultContracts.PickVisualMedia.ImageOnly
                        )
                    )
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ACCENT
                )
            ) {
                Text("Open Photo", color = BG)
            }

            OutlinedButton(
                onClick = { showBefore = !showBefore },
                enabled = original != null
            ) {
                Text("Before/After")
            }
        }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            TextButton(
                onClick = {
                    if (historyIndex > 0) {
                        historyIndex--
                        settings = history[historyIndex]
                        updatePreview(settings)
                    }
                },
                enabled = historyIndex > 0
            ) { Text("↶ Undo") }

            TextButton(
                onClick = {
                    if (historyIndex < history.lastIndex) {
                        historyIndex++
                        settings = history[historyIndex]
                        updatePreview(settings)
                    }
                },
                enabled = historyIndex < history.lastIndex
            ) { Text("↷ Redo") }

            TextButton(
                onClick = {
                    val reset = PhotoSettings()
                    saveHistory(reset)
                    updatePreview(reset)
                    showBefore = false
                },
                enabled = original != null
            ) { Text("Reset") }

            TextButton(
                onClick = {
                    val bitmap = preview
                    if (bitmap != null) {
                        scope.launch {
                            loading = true
                            val success = withContext(Dispatchers.IO) {
                                savePhoto(context, bitmap)
                            }
                            loading = false
                            message = if (success) {
                                "Photo Gallery mein save ho gayi."
                            } else {
                                "Photo save nahi hui."
                            }
                        }
                    }
                },
                enabled = preview != null
            ) { Text("Save") }
        }

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            SectionTitle("LIGHT & EXPOSURE")

            PhotoSlider("Brightness", settings.brightness, -1f..1f, original != null) {
                settings = settings.copy(brightness = it)
                updatePreview(settings)
            }
            PhotoSlider("Contrast", settings.contrast, 0.2f..2.5f, original != null) {
                settings = settings.copy(contrast = it)
                updatePreview(settings)
            }
            PhotoSlider("Exposure", settings.exposure, -2f..2f, original != null) {
                settings = settings.copy(exposure = it)
                updatePreview(settings)
            }
            PhotoSlider("Highlights", settings.highlights, -1f..1f, original != null) {
                settings = settings.copy(highlights = it)
                updatePreview(settings)
            }
            PhotoSlider("Shadows", settings.shadows, -1f..1f, original != null) {
                settings = settings.copy(shadows = it)
                updatePreview(settings)
            }

            Spacer(Modifier.height(12.dp))
            SectionTitle("COLOR MIXER")

            PhotoSlider("Saturation", settings.saturation, 0f..2.5f, original != null) {
                settings = settings.copy(saturation = it)
                updatePreview(settings)
            }
            PhotoSlider("Temperature", settings.warmth, -1f..1f, original != null) {
                settings = settings.copy(warmth = it)
                updatePreview(settings)
            }
            PhotoSlider("Tint", settings.tint, -1f..1f, original != null) {
                settings = settings.copy(tint = it)
                updatePreview(settings)
            }
            PhotoSlider("Hue", settings.hue, -180f..180f, original != null) {
                settings = settings.copy(hue = it)
                updatePreview(settings)
            }

            Spacer(Modifier.height(12.dp))
            SectionTitle("CREATIVE EFFECTS")

            PhotoSlider("Vignette", settings.vignette, 0f..1f, original != null) {
                settings = settings.copy(vignette = it)
                updatePreview(settings)
            }
            PhotoSlider("Grain", settings.grain, 0f..1f, original != null) {
                settings = settings.copy(grain = it)
                updatePreview(settings)
            }
            PhotoSlider("Blur", settings.blur, 0f..8f, original != null) {
                settings = settings.copy(blur = it)
                updatePreview(settings)
            }

            Spacer(Modifier.height(15.dp))
            SectionTitle("ONE-TAP PRESETS")

            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                OutlinedButton(onClick = {
                    applyPreset(
                        PhotoSettings(
                            contrast = 1.2f,
                            saturation = 0.85f,
                            warmth = -0.1f,
                            vignette = 0.25f
                        )
                    )
                }, enabled = original != null) {
                    Text("Cinematic")
                }

                OutlinedButton(onClick = {
                    applyPreset(
                        PhotoSettings(
                            saturation = 1.15f,
                            warmth = 0.4f,
                            highlights = 0.15f
                        )
                    )
                }, enabled = original != null) {
                    Text("Golden Hour")
                }

                OutlinedButton(onClick = {
                    applyPreset(
                        PhotoSettings(
                            saturation = 0f,
                            contrast = 1.2f
                        )
                    )
                }, enabled = original != null) {
                    Text("B&W")
                }

                OutlinedButton(onClick = {
                    applyPreset(
                        PhotoSettings(
                            contrast = 0.9f,
                            saturation = 0.75f,
                            warmth = 0.2f,
                            grain = 0.1f,
                            vignette = 0.2f
                        )
                    )
                }, enabled = original != null) {
                    Text("Vintage")
                }
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun PhotoSlider(
    title: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    enabled: Boolean,
    onChange: (Float) -> Unit
) {
    Column {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title, color = WHITE, fontSize = 12.sp)
            Text(
                String.format("%.2f", value),
                color = ACCENT,
                fontSize = 11.sp
            )
        }

        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = range,
            enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = ACCENT,
                activeTrackColor = ACCENT
            )
        )
    }
}

private fun processPhoto(
    source: Bitmap,
    settings: PhotoSettings
): Bitmap {
    val width = source.width
    val height = source.height

    val output = Bitmap.createBitmap(
        width,
        height,
        Bitmap.Config.ARGB_8888
    )

    val pixels = IntArray(width * height)
    source.getPixels(pixels, 0, width, 0, 0, width, height)

    val exposure = 2.0.pow(
        settings.exposure.toDouble()
    ).toFloat()

    for (i in pixels.indices) {
        val color = pixels[i]
        val alpha = color ushr 24

        var r = ((color shr 16) and 255).toFloat()
        var g = ((color shr 8) and 255).toFloat()
        var b = (color and 255).toFloat()

        val luminance = (
            r * 0.299f +
            g * 0.587f +
            b * 0.114f
        ) / 255f

        val tone = (
            settings.shadows * (1f - luminance) +
            settings.highlights * luminance
        ) * 35f

        val contrast = settings.contrast
        val brightness = settings.brightness * 100f
        val warmth = settings.warmth * 28f
        val tint = settings.tint * 20f

        r = ((r - 128f) * contrast + 128f + brightness + tone) * exposure
        g = ((g - 128f) * contrast + 128f + brightness + tone) * exposure
        b = ((b - 128f) * contrast + 128f + brightness + tone) * exposure

        r += warmth - tint * 0.4f
        g += tint
        b -= warmth + tint * 0.4f

        val gray = r * 0.299f + g * 0.587f + b * 0.114f

        r = gray + (r - gray) * settings.saturation
        g = gray + (g - gray) * settings.saturation
        b = gray + (b - gray) * settings.saturation

        if (abs(settings.hue) > 0.01f) {
            val hsv = FloatArray(3)

            android.graphics.Color.colorToHSV(
                android.graphics.Color.rgb(
                    r.toInt().coerceIn(0, 255),
                    g.toInt().coerceIn(0, 255),
                    b.toInt().coerceIn(0, 255)
                ),
                hsv
            )

            hsv[0] = (hsv[0] + settings.hue + 360f) % 360f

            val shifted = android.graphics.Color.HSVToColor(alpha, hsv)

            r = ((shifted shr 16) and 255).toFloat()
            g = ((shifted shr 8) and 255).toFloat()
            b = (shifted and 255).toFloat()
        }

        val x = i % width
        val y = i / width

        if (settings.vignette > 0f) {
            val dx = (x - width / 2f) / (width / 2f)
            val dy = (y - height / 2f) / (height / 2f)

            val distance = (dx * dx + dy * dy).coerceIn(0f, 1f)
            val shade = 1f - settings.vignette * distance * 0.38f

            r *= shade
            g *= shade
            b *= shade
        }

        if (settings.grain > 0f) {
            val noise = (
                (((i.toLong() * 1103515245L + 12345L) shr 16)
                    .toInt() and 255) / 127.5f
            ) - 1f

            val amount = noise * settings.grain * 35f
            r += amount
            g += amount
            b += amount
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

    if (settings.blur > 0.1f) {
        val scale = (1f - settings.blur * 0.07f).coerceAtLeast(0.4f)
        val small = Bitmap.createScaledBitmap(
            output,
            (width * scale).toInt().coerceAtLeast(1),
            (height * scale).toInt().coerceAtLeast(1),
            true
        )
        return Bitmap.createScaledBitmap(small, width, height, true)
    }

    return output
}

private fun readPhoto(
    context: Context,
    uri: Uri
): Bitmap? {
    return try {
        val bounds = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }

        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, bounds)
        }

        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            return null
        }

        var sample = 1

        while (
            bounds.outWidth / sample > 2048 ||
            bounds.outHeight / sample > 2048
        ) {
            sample *= 2
        }

        val options = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }

        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        }
    } catch (_: Exception) {
        null
    }
}

private fun savePhoto(
    context: Context,
    bitmap: Bitmap
): Boolean {
    return try {
        val values = ContentValues().apply {
            put(
                MediaStore.Images.Media.DISPLAY_NAME,
                "ProPhoto_${System.currentTimeMillis()}.jpg"
            )
            put(
                MediaStore.Images.Media.MIME_TYPE,
                "image/jpeg"
            )

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
        ) ?: return false

        val saved = context.contentResolver.openOutputStream(uri)?.use {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it)
        } ?: false

        if (Build.VERSION.SDK_INT >= 29) {
            val completed = ContentValues().apply {
                put(MediaStore.Images.Media.IS_PENDING, 0)
            }
            context.contentResolver.update(uri, completed, null, null)
        }

        saved
    } catch (_: Exception) {
        false
    }
}

@Composable
private fun AIStudioScreen() {
    ScreenColumn {
        Header("AI STUDIO", "INTELLIGENT CREATIVE WORKSPACE")

        Text(
            "Imagine. Create. Transform.",
            color = WHITE,
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(20.dp))

        Box(
            Modifier
                .fillMaxWidth()
                .height(170.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color(0xFF49336E),
                            Color(0xFF1B4553)
                        )
                    )
                )
                .padding(22.dp)
        ) {
            Column {
                Text("✦ AI CREATIVE LAB", color = Color(0xFFE0D2FF))
                Text(
                    "Your ideas,\nreimagined.",
                    color = WHITE,
                    fontSize = 29.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(Modifier.height(24.dp))
        SectionTitle("AI TOOLS")

        val tools = listOf(
            "AI Image Generator" to "Text to image creation",
            "Background Removal" to "Subject separation",
            "AI Enhancement" to "Photo detail enhancement",
            "Object Removal" to "Remove unwanted objects",
            "Portrait Studio" to "Portrait enhancement",
            "AI Upscale" to "Resolution enhancement",
            "AI Relight" to "Lighting adjustments",
            "Generative Expand" to "Expand photo composition"
        )

        tools.forEach {
            StudioCard("✧", it.first, it.second) {}
            Spacer(Modifier.height(9.dp))
        }

        Text(
            "AI features need an actual AI model or online service integration. These are not active yet.",
            color = MUTED,
            fontSize = 11.sp
        )

        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun ProjectsScreen() {
    ScreenColumn {
        Header("MY PROJECTS", "YOUR CREATIVE LIBRARY")

        Text(
            "Your workspace.",
            color = WHITE,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            "Edited photos are saved in your Gallery.",
            color = MUTED
        )

        Spacer(Modifier.height(22.dp))

        Box(
            Modifier
                .fillMaxWidth()
                .height(190.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(PANEL),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("▤", color = ACCENT, fontSize = 48.sp)
                Text("Your project library", color = WHITE)
                Text("In-app project manager coming later", color = MUTED, fontSize = 11.sp)
            }
        }

        Spacer(Modifier.height(20.dp))
        SectionTitle("SAVED PHOTOS")

        Text(
            "Find your exported photos in Pictures / Pro Photo Studio.",
            color = MUTED
        )

        Spacer(Modifier.height(25.dp))
    }
}

@Composable
private fun ProfileScreen() {
    ScreenColumn {
        Header("PROFILE", "PERSONALIZE YOUR STUDIO")

        Box(
            Modifier
                .size(75.dp)
                .clip(CircleShape)
                .background(PANEL),
            contentAlignment = Alignment.Center
        ) {
            Text("◉", color = ACCENT, fontSize = 38.sp)
        }

        Spacer(Modifier.height(12.dp))

        Text(
            "Creative User",
            color = WHITE,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            "Welcome to Pro Photo Studio",
            color = MUTED
        )

        Spacer(Modifier.height(25.dp))
        SectionTitle("APP INFORMATION")

        StudioCard("◐", "Appearance", "Premium dark interface") {}
        Spacer(Modifier.height(9.dp))
        StudioCard("⚙", "Settings", "More preferences coming soon") {}
        Spacer(Modifier.height(9.dp))
        StudioCard("⇩", "Export", "High quality JPEG export") {}

        Spacer(Modifier.height(25.dp))

        Text(
            "PRO PHOTO STUDIO",
            color = ACCENT,
            fontWeight = FontWeight.Bold
        )

        Text(
            "Creative tools. One studio.",
            color = MUTED,
            fontSize = 11.sp
        )

        Spacer(Modifier.height(25.dp))
    }
}
