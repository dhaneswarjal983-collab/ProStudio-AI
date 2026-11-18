package com.prophotostudio.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
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
        setContent {
            ProPhotoStudioApp()
        }
    }
}

@Composable
fun ProPhotoStudioApp() {
    var selectedTab by remember { mutableIntStateOf(0) }

    MaterialTheme(
        colorScheme = darkColorScheme(
            background = Background,
            surface = Panel,
            primary = Accent
        )
    ) {
        Scaffold(
            containerColor = Background,
            bottomBar = {
                BottomNavigation(selectedTab) {
                    selectedTab = it
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
                        onEditor = { selectedTab = 1 },
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
}

@Composable
fun BottomNavigation(
    selectedTab: Int,
    onSelect: (Int) -> Unit
) {
    val items = listOf(
        "⌂" to "Home",
        "▧" to "Edit",
        "✧" to "AI Studio",
        "▤" to "Projects",
        "◉" to "Profile"
    )

    Row(
        Modifier
            .fillMaxWidth()
            .background(Panel)
            .navigationBarsPadding()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        items.forEachIndexed { index, item ->
            Column(
                Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onSelect(index) }
                    .padding(horizontal = 9.dp, vertical = 3.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    item.first,
                    color = if (selectedTab == index) Accent else Muted,
                    fontSize = 21.sp
                )
                Text(
                    item.second,
                    color = if (selectedTab == index) Accent else Muted,
                    fontSize = 9.sp
                )
            }
        }
    }
}

@Composable
fun ScreenColumn(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        content = content
    )
}

@Composable
fun Header(title: String, subtitle: String) {
    Spacer(Modifier.height(18.dp))
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                title,
                color = White,
                fontSize = 19.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                subtitle,
                color = Muted,
                fontSize = 9.sp,
                letterSpacing = 2.sp
            )
        }
        Text(
            "✦ PRO",
            color = Accent,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        )
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        title,
        color = Muted,
        fontSize = 11.sp,
        letterSpacing = 2.sp,
        fontWeight = FontWeight.SemiBold
    )
    Spacer(Modifier.height(10.dp))
}

@Composable
fun HomeScreen(
    onEditor: () -> Unit,
    onAI: () -> Unit,
    onProjects: () -> Unit
) {
    ScreenColumn {
        Header("PRO PHOTO STUDIO", "CREATE WITHOUT LIMITS")
        Spacer(Modifier.height(25.dp))

        Text(
            "Your creative space.",
            color = White,
            fontSize = 27.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Turn your imagination into art.",
            color = Muted,
            fontSize = 14.sp
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
                            Color(0xFF48366F),
                            Color(0xFF222B48),
                            Color(0xFF151925)
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column(Modifier.align(Alignment.CenterStart)) {
                Text(
                    "CREATIVE STUDIO",
                    color = Color(0xFFD5C5FF),
                    fontSize = 10.sp,
                    letterSpacing = 2.sp
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "Make every\npixel matter.",
                    color = White,
                    fontSize = 27.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 33.sp
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    "Professional photo editing",
                    color = Color.LightGray,
                    fontSize = 12.sp
                )
            }
            Text(
                "✦",
                Modifier.align(Alignment.TopEnd).padding(12.dp),
                color = Color(0xFFE2D5FF),
                fontSize = 45.sp
            )
        }

        Spacer(Modifier.height(25.dp))
        SectionTitle("CREATE")

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ActionCard(
                Modifier.weight(1f),
                "▧",
                "Photo Editor",
                "Professional tools",
                Color(0xFF59458D),
                onEditor
            )
            ActionCard(
                Modifier.weight(1f),
                "✧",
                "AI Studio",
                "AI creativity",
                Color(0xFF285A65),
                onAI
            )
        }

        Spacer(Modifier.height(25.dp))
        SectionTitle("AI CREATIVE STUDIO")

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            AiTool("✦", "AI Image Generator", "Create images from text")
            AiTool("◈", "Background Editor", "Background editing tools")
            AiTool("✧", "AI Photo Enhancement", "Explore image enhancement")
            AiTool("◇", "Object Removal", "Intelligent object editing")
        }

        Spacer(Modifier.height(25.dp))
        SectionTitle("YOUR WORKSPACE")

        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Panel)
                .clickable { onProjects() }
                .padding(20.dp)
        ) {
            Column {
                Text(
                    "No projects yet",
                    color = White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    "Your edited photos and projects will appear here.",
                    color = Muted,
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(10.dp))
                Text("VIEW PROJECTS  →", color = Accent, fontSize = 11.sp)
            }
        }

        Spacer(Modifier.height(20.dp))
    }
}

@Composable
fun ActionCard(
    modifier: Modifier = Modifier,
    icon: String,
    title: String,
    subtitle: String,
    color: Color,
    onClick: () -> Unit
) {
    Column(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Panel)
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Box(
            Modifier
                .size(43.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(color),
            contentAlignment = Alignment.Center
        ) {
            Text(icon, color = White, fontSize = 24.sp)
        }
        Spacer(Modifier.height(15.dp))
        Text(title, color = White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Spacer(Modifier.height(4.dp))
        Text(subtitle, color = Muted, fontSize = 10.sp)
    }
}

@Composable
fun AiTool(icon: String, title: String, subtitle: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(15.dp))
            .background(Panel)
            .clickable { }
            .padding(15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, color = Accent, fontSize = 25.sp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = Muted, fontSize = 10.sp)
        }
        Text("›", color = Muted, fontSize = 24.sp)
    }
}

// ---------------------------------------------------------
// STAGE 5 - EDITING ENGINE
// ---------------------------------------------------------

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
    val tint: Float = 0f
)

@Composable
fun EditorScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var values by remember { mutableStateOf(EditValues()) }
    var history by remember { mutableStateOf(listOf(EditValues())) }
    var historyIndex by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showOriginal by remember { mutableStateOf(false) }

    var zoom by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    fun resetZoom() {
        zoom = 1f
        offsetX = 0f
        offsetY = 0f
    }

    fun applyEdit(newValues: EditValues) {
        val updated = history.take(historyIndex + 1) + newValues
        history = updated
        historyIndex = updated.lastIndex
        values = newValues
    }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            isLoading = true
            errorMessage = null

            scope.launch {
                val bitmap = withContext(Dispatchers.IO) {
                    loadPreviewBitmap(context, uri)
                }

                originalBitmap = bitmap
                values = EditValues()
                history = listOf(EditValues())
                historyIndex = 0
                showOriginal = false
                resetZoom()
                isLoading = false

                if (bitmap == null) {
                    errorMessage = "Photo load nahi ho payi. Dusri photo try karo."
                }
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp)
    ) {
        Header("PHOTO EDITOR", "PROFESSIONAL WORKSPACE")

        // Fixed preview area: controls scroll independently below it.
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 7.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("LIVE PREVIEW", color = Muted, fontSize = 10.sp)
            Text(
                if (showOriginal) "ORIGINAL" else "EDITED",
                color = Accent,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Box(
            Modifier
                .fillMaxWidth()
                .weight(0.9f)
                .clip(RoundedCornerShape(20.dp))
                .background(Panel2),
            contentAlignment = Alignment.Center
        ) {
            when {
                isLoading -> CircularProgressIndicator(color = Accent)

                originalBitmap != null -> {
                    val matrix = remember(values, showOriginal) {
                        createEditColorMatrix(
                            if (showOriginal) EditValues() else values
                        )
                    }

                    Image(
                        bitmap = originalBitmap!!.asImageBitmap(),
                        contentDescription = "Photo preview",
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(originalBitmap) {
                                detectTransformGestures { _, pan, zoomChange, _ ->
                                    zoom = (zoom * zoomChange).coerceIn(1f, 5f)

                                    if (zoom > 1f) {
                                        offsetX += pan.x
                                        offsetY += pan.y
                                    } else {
                                        offsetX = 0f
                                        offsetY = 0f
                                    }
                                }
                            }
                            .pointerInput(originalBitmap) {
                                awaitPointerEventScope {
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        if (event.changes.size == 2 &&
                                            event.changes.all { it.pressed }
                                        ) {
                                            // Pinch is handled by transform gesture.
                                        }
                                    }
                                }
                            }
                            .graphicsLayer {
                                scaleX = zoom
                                scaleY = zoom
                                translationX = offsetX
                                translationY = offsetY
                            },
                        contentScale = ContentScale.Fit,
                        colorFilter = ColorFilter.colorMatrix(matrix)
                    )
                }

                else -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("▧", color = Accent, fontSize = 55.sp)
                        Spacer(Modifier.height(10.dp))
                        Text("Your photo preview", color = White, fontSize = 17.sp)
                        Text(
                            "Select a photo to begin editing",
                            color = Muted,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        if (originalBitmap != null) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Zoom: ${(zoom * 100).toInt()}%",
                    color = Muted,
                    fontSize = 10.sp
                )
                Text(
                    "DOUBLE TAP NOT AVAILABLE",
                    color = Muted,
                    fontSize = 9.sp
                )
                Text(
                    "RESET ZOOM",
                    color = Accent,
                    fontSize = 10.sp,
                    modifier = Modifier.clickable { resetZoom() }
                )
            }
        }

        if (errorMessage != null) {
            Text(
                errorMessage!!,
                color = Color(0xFFFF8A8A),
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 5.dp)
            )
        }

        Button(
            onClick = {
                picker.launch(
                    PickVisualMediaRequest(
                        ActivityResultContracts.PickVisualMedia.ImageOnly
                    )
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            enabled = !isLoading,
            colors = ButtonDefaults.buttonColors(containerColor = Accent),
            shape = RoundedCornerShape(13.dp)
        ) {
            Text(
                if (originalBitmap == null) "＋  Select Photo" else "▧  Change Photo",
                color = Background,
                fontWeight = FontWeight.Bold
            )
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            OutlinedButton(
                onClick = {
                    if (historyIndex > 0) {
                        historyIndex--
                        values = history[historyIndex]
                    }
                },
                enabled = originalBitmap != null && historyIndex > 0,
                modifier = Modifier.weight(1f)
            ) {
                Text("↶ Undo")
            }

            OutlinedButton(
                onClick = {
                    if (historyIndex < history.lastIndex) {
                        historyIndex++
                        values = history[historyIndex]
                    }
                },
                enabled = originalBitmap != null && historyIndex < history.lastIndex,
                modifier = Modifier.weight(1f)
            ) {
                Text("↷ Redo")
            }

            OutlinedButton(
                onClick = {
                    if (originalBitmap != null) {
                        applyEdit(EditValues())
                        showOriginal = false
                    }
                },
                enabled = originalBitmap != null,
                modifier = Modifier.weight(1f)
            ) {
                Text("Reset")
            }
        }

        OutlinedButton(
            onClick = { showOriginal = !showOriginal },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 3.dp),
            enabled = originalBitmap != null
        ) {
            Text(if (showOriginal) "Show Edited Photo" else "Before / After")
        }

        // Scrollable editing controls, while preview remains visible.
        Column(
            Modifier
                .weight(1.1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(top = 12.dp, bottom = 18.dp)
        ) {
            SectionTitle("BASIC LIGHT & COLOR")

            EditSlider(
                "Brightness", values.brightness,
                "${(values.brightness * 100).toInt()}",
                -1f..1f, originalBitmap != null,
                {
                    showOriginal = false
                    values = values.copy(brightness = it)
                },
                { applyEdit(values) }
            )

            EditSlider(
                "Contrast", values.contrast,
                "${(values.contrast * 100).toInt()}%",
                0f..2f, originalBitmap != null,
                {
                    showOriginal = false
                    values = values.copy(contrast = it)
                },
                { applyEdit(values) }
            )

            EditSlider(
                "Saturation", values.saturation,
                "${(values.saturation * 100).toInt()}%",
                0f..2f, originalBitmap != null,
                {
                    showOriginal = false
                    values = values.copy(saturation = it)
                },
                { applyEdit(values) }
            )

            Spacer(Modifier.height(18.dp))
            SectionTitle("PROFESSIONAL LIGHT & EXPOSURE")

            EditSlider(
                "Exposure", values.exposure,
                String.format("%.2f EV", values.exposure),
                -2f..2f, originalBitmap != null,
                {
                    showOriginal = false
                    values = values.copy(exposure = it)
                },
                { applyEdit(values) }
            )

            EditSlider(
                "Highlights", values.highlights,
                "${(values.highlights * 100).toInt()}",
                -1f..1f, originalBitmap != null,
                {
                    showOriginal = false
                    values = values.copy(highlights = it)
                },
                { applyEdit(values) }
            )

            EditSlider(
                "Shadows", values.shadows,
                "${(values.shadows * 100).toInt()}",
                -1f..1f, originalBitmap != null,
                {
                    showOriginal = false
                    values = values.copy(shadows = it)
                },
                { applyEdit(values) }
            )

            EditSlider(
                "Whites", values.whites,
                "${(values.whites * 100).toInt()}",
                -1f..1f, originalBitmap != null,
                {
                    showOriginal = false
                    values = values.copy(whites = it)
                },
                { applyEdit(values) }
            )

            EditSlider(
                "Blacks", values.blacks,
                "${(values.blacks * 100).toInt()}",
                -1f..1f, originalBitmap != null,
                {
                    showOriginal = false
                    values = values.copy(blacks = it)
                },
                { applyEdit(values) }
            )

            Spacer(Modifier.height(18.dp))
            SectionTitle("WHITE BALANCE")

            EditSlider(
                "Temperature", values.temperature,
                "${(values.temperature * 100).toInt()}",
                -1f..1f, originalBitmap != null,
                {
                    showOriginal = false
                    values = values.copy(temperature = it)
                },
                { applyEdit(values) }
            )

            EditSlider(
                "Tint", values.tint,
                "${(values.tint * 100).toInt()}",
                -1f..1f, originalBitmap != null,
                {
                    showOriginal = false
                    values = values.copy(tint = it)
                },
                { applyEdit(values) }
            )

            Spacer(Modifier.height(18.dp))
            SectionTitle("MORE PROFESSIONAL TOOLS")

            ToolRow("◐", "Light & Exposure", "Exposure and tonal controls")
            Spacer(Modifier.height(8.dp))
            ToolRow("◈", "Color Mixer", "Advanced HSL controls planned")
            Spacer(Modifier.height(8.dp))
            ToolRow("◇", "Masking", "Brush, gradients and selections planned")
            Spacer(Modifier.height(8.dp))
            ToolRow("◎", "Detail", "Sharpening and noise reduction planned")
            Spacer(Modifier.height(8.dp))
            ToolRow("⌑", "Crop & Geometry", "Crop, rotate and perspective planned")
            Spacer(Modifier.height(8.dp))
            ToolRow("✧", "Effects", "Texture, clarity, grain and vignette planned")
            Spacer(Modifier.height(8.dp))
            ToolRow("◉", "Presets", "Save and apply editing styles planned")
        }
    }
}

@Composable
private fun EditSlider(
    title: String,
    value: Float,
    valueText: String,
    range: ClosedFloatingPointRange<Float>,
    enabled: Boolean,
    onValueChange: (Float) -> Unit,
    onFinished: () -> Unit
) {
    Column {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, color = White, fontSize = 14.sp)
            Text(valueText, color = Accent, fontSize = 11.sp)
        }

        Slider(
            value = value,
            onValueChange = onValueChange,
            onValueChangeFinished = onFinished,
            valueRange = range,
            enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = Accent,
                activeTrackColor = Accent
            )
        )
    }
}

private fun channelMatrix(
    redGain: Float = 1f,
    greenGain: Float = 1f,
    blueGain: Float = 1f,
    redOffset: Float = 0f,
    greenOffset: Float = 0f,
    blueOffset: Float = 0f
): ColorMatrix {
    return ColorMatrix(
        floatArrayOf(
            redGain, 0f, 0f, 0f, redOffset,
            0f, greenGain, 0f, 0f, greenOffset,
            0f, 0f, blueGain, 0f, blueOffset,
            0f, 0f, 0f, 1f, 0f
        )
    )
}

private fun createEditColorMatrix(values: EditValues): ColorMatrix {
    val saturation = values.saturation
    val sr = (1f - saturation) * 0.213f
    val sg = (1f - saturation) * 0.715f
    val sb = (1f - saturation) * 0.072f

    val saturationMatrix = ColorMatrix(
        floatArrayOf(
            sr + saturation, sg, sb, 0f, 0f,
            sr, sg + saturation, sb, 0f, 0f,
            sr, sg, sb + saturation, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
    )

    val contrast = values.contrast
    val exposureGain = 2.0.pow(values.exposure.toDouble()).toFloat()

    val baseOffset = values.brightness * 255f + 128f * (1f - contrast)

    val exposureMatrix = channelMatrix(
        redGain = exposureGain,
        greenGain = exposureGain,
        blueGain = exposureGain
    )

    val contrastMatrix = channelMatrix(
        redGain = contrast,
        greenGain = contrast,
        blueGain = contrast,
        redOffset = baseOffset,
        greenOffset = baseOffset,
        blueOffset = baseOffset
    )

    val highlightGain = 1f + values.highlights * 0.18f
    val shadowOffset = values.shadows * 32f
    val whiteOffset = values.whites * 28f
    val blackOffset = values.blacks * 28f

    val tonalMatrix = channelMatrix(
        redGain = highlightGain,
        greenGain = highlightGain,
        blueGain = highlightGain,
        redOffset = shadowOffset + whiteOffset + blackOffset,
        greenOffset = shadowOffset + whiteOffset + blackOffset,
        blueOffset = shadowOffset + whiteOffset + blackOffset
    )

    val temperatureOffset = values.temperature * 24f
    val tintOffset = values.tint * 18f

    val whiteBalanceMatrix = channelMatrix(
        redOffset = temperatureOffset - tintOffset * 0.5f,
        greenOffset = tintOffset,
        blueOffset = -temperatureOffset - tintOffset * 0.5f
    )

    val result = ColorMatrix(saturationMatrix.values)
    result *= exposureMatrix
    result *= contrastMatrix
    result *= tonalMatrix
    result *= whiteBalanceMatrix

    return result
}

@Composable
fun ToolRow(icon: String, title: String, subtitle: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(15.dp))
            .background(Panel)
            .padding(15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, color = Accent, fontSize = 24.sp)
        Spacer(Modifier.width(14.dp))
        Column {
            Text(title, color = White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = Muted, fontSize = 10.sp)
        }
    }
}

// ---------------------------------------------------------
// AI STUDIO
// ---------------------------------------------------------

@Composable
fun AIStudioScreen() {
    ScreenColumn {
        Header("AI STUDIO", "INTELLIGENT CREATIVE TOOLS")
        Spacer(Modifier.height(25.dp))

        Text(
            "Imagine. Create. Transform.",
            color = White,
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold
        )
        Text("Explore the AI creative workspace.", color = Muted, fontSize = 13.sp)

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
                Spacer(Modifier.height(10.dp))
                Text(
                    "Your ideas,\nreimagined.",
                    color = White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "A home for intelligent photo creation.",
                    color = Color.LightGray,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(Modifier.height(25.dp))
        SectionTitle("AI TOOLS")

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ToolRow("✦", "AI Image Generator", "Text-to-image creation")
            ToolRow("◈", "Background Removal", "Separate subject from background")
            ToolRow("◎", "AI Enhancement", "Improve image details")
            ToolRow("◇", "Object Removal", "Intelligent object editing")
            ToolRow("✧", "Portrait Studio", "Portrait editing tools")
            ToolRow("▧", "AI Upscale", "High-resolution enhancement")
            ToolRow("◐", "AI Relight", "Creative lighting controls")
            ToolRow("⌑", "Generative Expand", "Expand image composition")
        }

        Spacer(Modifier.height(20.dp))
        Text(
            "AI tools are interface previews. Actual AI processing requires implementation and an AI service.",
            color = Muted,
            fontSize = 11.sp
        )
        Spacer(Modifier.height(25.dp))
    }
}

// ---------------------------------------------------------
// PROJECTS
// ---------------------------------------------------------

@Composable
fun ProjectsScreen() {
    ScreenColumn {
        Header("MY PROJECTS", "YOUR CREATIVE LIBRARY")
        Spacer(Modifier.height(25.dp))

        Text(
            "Your workspace.",
            color = White,
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold
        )
        Text("All your creations in one place.", color = Muted, fontSize = 13.sp)

        Spacer(Modifier.height(25.dp))

        Box(
            Modifier
                .fillMaxWidth()
                .height(240.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(Panel),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("▤", color = Accent, fontSize = 50.sp)
                Spacer(Modifier.height(12.dp))
                Text(
                    "No projects yet",
                    color = White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Your saved projects will appear here.",
                    color = Muted,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(Modifier.height(25.dp))
        SectionTitle("PROJECT CATEGORIES")
        ToolRow("▧", "Edited Photos", "Your photo editing projects")
        Spacer(Modifier.height(10.dp))
        ToolRow("✧", "AI Creations", "Your AI-generated work")
        Spacer(Modifier.height(10.dp))
        ToolRow("◈", "Presets", "Your saved editing styles")
        Spacer(Modifier.height(25.dp))
    }
}

// ---------------------------------------------------------
// PROFILE
// ---------------------------------------------------------

@Composable
fun ProfileScreen() {
    ScreenColumn {
        Header("PROFILE & SETTINGS", "PERSONALIZE YOUR STUDIO")
        Spacer(Modifier.height(28.dp))

        Box(
            Modifier
                .size(82.dp)
                .clip(CircleShape)
                .background(Panel2),
            contentAlignment = Alignment.Center
        ) {
            Text("◉", color = Accent, fontSize = 38.sp)
        }

        Spacer(Modifier.height(14.dp))
        Text(
            "Creative User",
            color = White,
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold
        )
        Text("Welcome to Pro Photo Studio", color = Muted, fontSize = 12.sp)

        Spacer(Modifier.height(30.dp))
        SectionTitle("APP SETTINGS")

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ToolRow("◐", "Appearance", "Theme and interface preferences")
            ToolRow("⚙", "General Settings", "App preferences")
            ToolRow("⇩", "Storage", "Local files and cache")
            ToolRow("☷", "Export Settings", "Image quality and formats")
            ToolRow("◈", "About", "App version and information")
        }

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

// ---------------------------------------------------------
// BITMAP LOADER
// ---------------------------------------------------------

private fun loadPreviewBitmap(
    context: Context,
    uri: Uri
): Bitmap? {
    return try {
        val resolver = context.contentResolver

        val bounds = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }

        resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, bounds)
        }

        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            return null
        }

        var sampleSize = 1

        while (
            bounds.outWidth / sampleSize > 2048 ||
            bounds.outHeight / sampleSize > 2048
        ) {
            sampleSize *= 2
        }

        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
        }

        resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        }
    } catch (e: Exception) {
        null
    }
}
