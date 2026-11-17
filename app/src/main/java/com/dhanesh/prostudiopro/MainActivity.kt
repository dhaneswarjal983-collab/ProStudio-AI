package com.dhanesh.prostudiopro

import android.content.ContentValues
import android.content.Context
import android.graphics.*
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.OutputStream
import kotlin.math.roundToInt

data class CanvasLayer(
    val id: Long = System.currentTimeMillis(),
    val text: String = "",
    val imageBitmap: Bitmap? = null,
    val isSticker: Boolean = false,
    val color: Color = Color.White,
    val opacity: Float = 1f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val scale: Float = 1f,
    val rotation: Float = 0f
)

data class EditorSnapshot(
    val bitmap: Bitmap,
    val params: EditParams,
    val bgColor: Int?
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(background = Color(0xFF0B0C10))) {
                ProStudioV2App()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProStudioV2App() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var baseBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var displayBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var params by remember { mutableStateOf(EditParams()) }
    var studioBgColor by remember { mutableStateOf<Int?>(null) }
    var histogramBins by remember { mutableStateOf(FloatArray(32)) }
    var showHistogram by remember { mutableStateOf(true) }

    // Undo / Redo History
    val undoStack = remember { mutableStateListOf<EditorSnapshot>() }
    val redoStack = remember { mutableStateListOf<EditorSnapshot>() }

    fun pushHistory() {
        val bmp = baseBitmap ?: return
        undoStack.add(EditorSnapshot(bmp, params, studioBgColor))
        if (undoStack.size > 15) undoStack.removeAt(0)
        redoStack.clear()
    }

    // Layers (Text, Stickers & Overlay Photos)
    val layers = remember { mutableStateListOf<CanvasLayer>() }
    var selectedLayerId by remember { mutableStateOf<Long?>(null) }
    var showTextDialog by remember { mutableStateOf(false) }
    var newTextInput by remember { mutableStateOf("") }
    var newTextColor by remember { mutableStateOf(Color.White) }

    // Saved User Presets
    val customPresets = remember { mutableStateListOf<Pair<String, EditParams>>() }

    // Tools & Sub-states
    val tools = listOf("Crop", "Light", "Color", "Color Mix", "Grading", "Effects", "Heal", "AI & BG", "Layers", "Presets")
    var activeTool by remember { mutableStateOf("Light") }
    var selectedHslColor by remember { mutableStateOf("Red") }
    var gradingZone by remember { mutableStateOf("Shadows") }
    var brushSize by remember { mutableFloatStateOf(45f) }
    var isProcessing by remember { mutableStateOf(false) }
    var showOriginal by remember { mutableStateOf(false) }

    // Export Dialog States
    var showExportDialog by remember { mutableStateOf(false) }
    var exportAsPng by remember { mutableStateOf(false) }
    var exportQuality by remember { mutableFloatStateOf(95f) }

    // Canvas Zoom & Viewport
    var canvasScale by remember { mutableFloatStateOf(1f) }
    var canvasOffset by remember { mutableStateOf(Offset.Zero) }
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }

    // Real-Time GPU Render + Live Histogram
    LaunchedEffect(baseBitmap, params, studioBgColor) {
        val src = baseBitmap ?: return@LaunchedEffect
        val (rendered, hist) = withContext(Dispatchers.Default) {
            val graded = GpuColorEngine.render(context, src, params)
            val finalFrame = if (studioBgColor != null) {
                val out = Bitmap.createBitmap(graded.width, graded.height, Bitmap.Config.ARGB_8888)
                val c = Canvas(out)
                c.drawColor(studioBgColor!!)
                c.drawBitmap(graded, 0f, 0f, null)
                out
            } else graded
            val bins = GpuColorEngine.computeHistogram(finalFrame)
            Pair(finalFrame, bins)
        }
        displayBitmap = rendered
        histogramBins = hist
    }

    // Main Photo Picker
    val mainPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            val raw = decodeUriToBitmap(context, it, 2048) ?: return@let
            originalBitmap = raw
            baseBitmap = raw
            displayBitmap = raw
            params = EditParams()
            studioBgColor = null
            layers.clear()
            undoStack.clear()
            redoStack.clear()
            canvasScale = 1f
            canvasOffset = Offset.Zero
        }
    }

    // Overlay Photo Picker (Add Photo Layer)
    val overlayPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            val overlayBmp = decodeUriToBitmap(context, it, 1024) ?: return@let
            val l = CanvasLayer(imageBitmap = overlayBmp)
            layers.add(l)
            selectedLayerId = l.id
        }
    }

    Scaffold(
        containerColor = Color(0xFF0B0C10),
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF13151C)),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { mainPicker.launch("image/*") }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Open", tint = Color(0xFF60A5FA))
                        }
                        Spacer(Modifier.width(4.dp))
                        Text("STUDIO PRO", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = Color.White, maxLines = 1)
                    }
                },
                actions = {
                    if (baseBitmap != null) {
                        IconButton(
                            onClick = { showHistogram = !showHistogram },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.BarChart, contentDescription = "Histogram", tint = if (showHistogram) Color(0xFF38BDF8) else Color(0xFF64748B))
                        }
                        IconButton(
                            onClick = {
                                if (undoStack.isNotEmpty()) {
                                    redoStack.add(EditorSnapshot(baseBitmap!!, params, studioBgColor))
                                    val prev = undoStack.removeAt(undoStack.lastIndex)
                                    baseBitmap = prev.bitmap
                                    params = prev.params
                                    studioBgColor = prev.bgColor
                                }
                            },
                            enabled = undoStack.isNotEmpty(),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Undo, contentDescription = "Undo", tint = if (undoStack.isNotEmpty()) Color.White else Color(0xFF475569))
                        }
                        IconButton(
                            onClick = {
                                if (redoStack.isNotEmpty()) {
                                    undoStack.add(EditorSnapshot(baseBitmap!!, params, studioBgColor))
                                    val next = redoStack.removeAt(redoStack.lastIndex)
                                    baseBitmap = next.bitmap
                                    params = next.params
                                    studioBgColor = next.bgColor
                                }
                            },
                            enabled = redoStack.isNotEmpty(),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Redo, contentDescription = "Redo", tint = if (redoStack.isNotEmpty()) Color.White else Color(0xFF475569))
                        }
                        Button(
                            onClick = { showExportDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            modifier = Modifier.padding(end = 6.dp).height(32.dp)
                        ) {
                            Text("EXPORT", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 1. Interactive GPU + Healing Brush + Layer Canvas
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color(0xFF07080B))
                    .onSizeChanged { viewportSize = it }
                    .pointerInput(activeTool) {
                        if (activeTool == "Heal") {
                            detectDragGestures(
                                onDragStart = { pushHistory() },
                                onDrag = { change, _ ->
                                    val src = baseBitmap ?: return@detectDragGestures
                                    baseBitmap = applySpotHealBrush(src, change.position, viewportSize, brushSize)
                                }
                            )
                        } else {
                            detectTapGestures(
                                onPress = {
                                    selectedLayerId = null
                                    showOriginal = true
                                    tryAwaitRelease()
                                    showOriginal = false
                                },
                                onDoubleTap = {
                                    canvasScale = if (canvasScale > 1f) 1f else 2.5f
                                    canvasOffset = Offset.Zero
                                }
                            )
                        }
                    }
                    .pointerInput(activeTool, selectedLayerId) {
                        if (activeTool != "Heal") {
                            detectTransformGestures { _, pan, zoom, _ ->
                                if (selectedLayerId == null) {
                                    canvasScale = (canvasScale * zoom).coerceIn(1f, 5f)
                                    canvasOffset = if (canvasScale > 1f) canvasOffset + pan else Offset.Zero
                                }
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                val activeImg = if (showOriginal) originalBitmap else displayBitmap
                if (activeImg != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = canvasScale,
                                scaleY = canvasScale,
                                translationX = canvasOffset.x,
                                translationY = canvasOffset.y
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = activeImg.asImageBitmap(),
                            contentDescription = "Photo",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Interactive Text, Sticker & Photo Overlay Layers
                        layers.forEachIndexed { idx, layer ->
                            val isSelected = selectedLayerId == layer.id
                            Box(
                                modifier = Modifier
                                    .offset { IntOffset(layer.offsetX.roundToInt(), layer.offsetY.roundToInt()) }
                                    .graphicsLayer(
                                        scaleX = layer.scale,
                                        scaleY = layer.scale,
                                        rotationZ = layer.rotation,
                                        alpha = layer.opacity
                                    )
                                    .border(
                                        width = if (isSelected) 1.5.dp else 0.dp,
                                        color = if (isSelected) Color(0xFF3B82F6) else Color.Transparent,
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .pointerInput(layer.id) {
                                        detectTapGestures { selectedLayerId = layer.id }
                                    }
                                    .pointerInput(layer.id) {
                                        detectTransformGestures { _, pan, zoom, rot ->
                                            selectedLayerId = layer.id
                                            layers[idx] = layer.copy(
                                                offsetX = layer.offsetX + pan.x,
                                                offsetY = layer.offsetY + pan.y,
                                                scale = (layer.scale * zoom).coerceIn(0.3f, 4.5f),
                                                rotation = layer.rotation + rot
                                            )
                                        }
                                    }
                                    .padding(4.dp)
                            ) {
                                if (layer.imageBitmap != null) {
                                    Image(
                                        bitmap = layer.imageBitmap.asImageBitmap(),
                                        contentDescription = "Layer",
                                        modifier = Modifier.size(140.dp),
                                        contentScale = ContentScale.Fit
                                    )
                                } else {
                                    Text(
                                        text = layer.text,
                                        color = layer.color,
                                        fontSize = if (layer.isSticker) 44.sp else 26.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }
                    }

                    // Live DSLR Histogram Overlay (Top Right)
                    if (showHistogram && !showOriginal) {
                        Canvas(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(10.dp)
                                .size(width = 96.dp, height = 44.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.Black.copy(alpha = 0.65f))
                                .padding(4.dp)
                        ) {
                            val barW = size.width / histogramBins.size
                            histogramBins.forEachIndexed { i, v ->
                                val h = v * size.height
                                drawRect(
                                    color = Color(0xFF60A5FA).copy(alpha = 0.85f),
                                    topLeft = Offset(i * barW, size.height - h),
                                    size = androidx.compose.ui.geometry.Size(barW * 0.85f, h)
                                )
                            }
                        }
                    }

                    if (showOriginal) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.75f),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.align(Alignment.TopCenter).padding(top = 10.dp)
                        ) {
                            Text("BEFORE (ORIGINAL)", color = Color.White, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                        }
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFF3B82F6), modifier = Modifier.size(54.dp))
                        Spacer(Modifier.height(10.dp))
                        Text("PROSTUDIO V2 — CINEMA & AI ENGINE", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("16-Bit Shaders • 3-Way Grading • Offline AI", color = Color(0xFF94A3B8), fontSize = 12.sp)
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { mainPicker.launch("image/*") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                        ) {
                            Text("OPEN PHOTO FROM GALLERY", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (isProcessing) {
                    Box(
                        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Color(0xFF3B82F6))
                            Spacer(Modifier.height(10.dp))
                            Text("Processing On-Device AI...", color = Color.White, fontSize = 13.sp)
                        }
                    }
                }
            }

            // 2. Bottom Controls Panel
            if (baseBitmap != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF13151C))
                        .padding(top = 8.dp, bottom = 6.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 125.dp, max = 185.dp)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 14.dp)
                    ) {
                        when (activeTool) {
                            "Crop" -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf("1:1" to 1f, "4:5" to 0.8f, "9:16" to 0.5625f, "16:9" to 1.777f).forEach { (label, r) ->
                                        StudioChip(label) {
                                            pushHistory()
                                            baseBitmap = GpuColorEngine.cropAndTransform(baseBitmap!!, r, 0f, false)
                                        }
                                    }
                                    StudioChip("Rotate 90°") {
                                        pushHistory()
                                        baseBitmap = GpuColorEngine.cropAndTransform(baseBitmap!!, null, 90f, false)
                                    }
                                    StudioChip("Flip H") {
                                        pushHistory()
                                        baseBitmap = GpuColorEngine.cropAndTransform(baseBitmap!!, null, 0f, true)
                                    }
                                }
                            }

                            "Light" -> {
                                StudioSlider("Exposure", params.exposure, -2f..2f) { params = params.copy(exposure = it) }
                                StudioSlider("Contrast", params.contrast, 0.5f..2f) { params = params.copy(contrast = it) }
                                StudioSlider("Highlights", params.highlights, 0f..1f) { params = params.copy(highlights = it) }
                                StudioSlider("Shadows", params.shadows, 0f..1f) { params = params.copy(shadows = it) }
                                StudioSlider("Whites", params.whites, -0.5f..0.5f) { params = params.copy(whites = it) }
                                StudioSlider("Blacks", params.blacks, -0.5f..0.5f) { params = params.copy(blacks = it) }
                            }

                            "Color" -> {
                                StudioSlider("Temp", params.temperature, 2500f..8500f) { params = params.copy(temperature = it) }
                                StudioSlider("Tint", params.tint, -80f..80f) { params = params.copy(tint = it) }
                                StudioSlider("Vibrance", params.vibrance, -1f..1f) { params = params.copy(vibrance = it) }
                                StudioSlider("Saturation", params.saturation, 0f..2f) { params = params.copy(saturation = it) }
                            }

                            "Color Mix" -> {
                                val channels = listOf(
                                    "Red" to Color(0xFFEF4444),
                                    "Orange" to Color(0xFFF97316),
                                    "Yellow" to Color(0xFFEAB308),
                                    "Green" to Color(0xFF22C55E),
                                    "Aqua" to Color(0xFF06B6D4),
                                    "Blue" to Color(0xFF3B82F6)
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    channels.forEach { (name, col) ->
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(col)
                                                .border(if (selectedHslColor == name) 2.5.dp else 0.dp, Color.White, CircleShape)
                                                .clickable { selectedHslColor = name }
                                        )
                                    }
                                }
                                val cur = params.hslChannels[selectedHslColor] ?: HslChannel()
                                StudioSlider("$selectedHslColor Hue", cur.hue, -1f..1f) {
                                    params = params.copy(hslChannels = params.hslChannels + (selectedHslColor to cur.copy(hue = it)))
                                }
                                StudioSlider("$selectedHslColor Sat", cur.saturation, -1f..1f) {
                                    params = params.copy(hslChannels = params.hslChannels + (selectedHslColor to cur.copy(saturation = it)))
                                }
                                StudioSlider("$selectedHslColor Lum", cur.luminance, -1f..1f) {
                                    params = params.copy(hslChannels = params.hslChannels + (selectedHslColor to cur.copy(luminance = it)))
                                }
                            }

                            "Grading" -> {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 6.dp)) {
                                    listOf("Shadows", "Midtones", "Highlights").forEach { z ->
                                        StudioChip(if (gradingZone == z) "✓ $z" else z) { gradingZone = z }
                                    }
                                }
                                val currentTint = when (gradingZone) {
                                    "Shadows" -> params.shadowTint
                                    "Midtones" -> params.midtoneTint
                                    else -> params.highlightTint
                                }
                                fun updateZone(newArr: FloatArray) {
                                    params = when (gradingZone) {
                                        "Shadows" -> params.copy(shadowTint = newArr)
                                        "Midtones" -> params.copy(midtoneTint = newArr)
                                        else -> params.copy(highlightTint = newArr)
                                    }
                                }
                                StudioSlider("Red / Cyan", currentTint[0], -1f..1f) { updateZone(floatArrayOf(it, currentTint[1], currentTint[2])) }
                                StudioSlider("Green / Mag", currentTint[1], -1f..1f) { updateZone(floatArrayOf(currentTint[0], it, currentTint[2])) }
                                StudioSlider("Blue / Warm", currentTint[2], -1f..1f) { updateZone(floatArrayOf(currentTint[0], currentTint[1], it)) }
                            }

                            "Effects" -> {
                                StudioSlider("Clarity", params.clarity, -0.4f..1f) { params = params.copy(clarity = it) }
                                StudioSlider("Dehaze", params.dehaze, 0f..0.8f) { params = params.copy(dehaze = it) }
                                StudioSlider("Sharpen", params.sharpen, 0f..3.5f) { params = params.copy(sharpen = it) }
                                StudioSlider("Vignette", 1f - params.vignette, 0f..0.65f) { params = params.copy(vignette = 1f - it) }
                                StudioSlider("Film Grain", params.grain, 0f..0.35f) { params = params.copy(grain = it) }
                            }

                            "Heal" -> {
                                Text("SPOT HEAL / SKIN SMOOTH BRUSH", color = Color(0xFF60A5FA), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text("Swipe your finger directly on blemishes or spots on the photo to blend them.", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                Spacer(Modifier.height(6.dp))
                                StudioSlider("Brush Size", brushSize, 15f..90f) { brushSize = it }
                            }

                            "AI & BG" -> {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                    Button(
                                        onClick = {
                                            val src = baseBitmap ?: return@Button
                                            scope.launch {
                                                pushHistory()
                                                isProcessing = true
                                                baseBitmap = withContext(Dispatchers.Default) { OfflineAiEngine.removeBackground(src) }
                                                isProcessing = false
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                                        modifier = Modifier.weight(1f)
                                    ) { Text("AI CUTOUT", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1) }

                                    Button(
                                        onClick = {
                                            val src = baseBitmap ?: return@Button
                                            scope.launch {
                                                pushHistory()
                                                isProcessing = true
                                                baseBitmap = withContext(Dispatchers.Default) { OfflineAiEngine.applyPortraitBlur(context, src, 22f) }
                                                isProcessing = false
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                        modifier = Modifier.weight(1f)
                                    ) { Text("DSLR BLUR", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1) }
                                }
                                Spacer(Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf(
                                        "None" to null,
                                        "White" to android.graphics.Color.WHITE,
                                        "Dark" to android.graphics.Color.parseColor("#111827"),
                                        "Royal Blue" to android.graphics.Color.parseColor("#1D4ED8"),
                                        "Crimson" to android.graphics.Color.parseColor("#B91C1C"),
                                        "Emerald" to android.graphics.Color.parseColor("#047857")
                                    ).forEach { (label, col) ->
                                        StudioChip(label) {
                                            pushHistory()
                                            studioBgColor = col
                                        }
                                    }
                                }
                            }

                            "Layers" -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    StudioChip("+ Text") { showTextDialog = true }
                                    StudioChip("+ Add Photo") { overlayPicker.launch("image/*") }
                                    if (selectedLayerId != null) {
                                        StudioChip("Delete Layer") {
                                            layers.removeAll { it.id == selectedLayerId }
                                            selectedLayerId = null
                                        }
                                    }
                                }
                                val selIdx = layers.indexOfFirst { it.id == selectedLayerId }
                                if (selIdx >= 0) {
                                    StudioSlider("Opacity", layers[selIdx].opacity, 0.1f..1f) { newOp ->
                                        layers[selIdx] = layers[selIdx].copy(opacity = newOp)
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    listOf("🔥", "👑", "⚡", "🎬", "✨", "💯", "🎧", "😎").forEach { emoji ->
                                        TextButton(onClick = {
                                            val l = CanvasLayer(text = emoji, isSticker = true)
                                            layers.add(l)
                                            selectedLayerId = l.id
                                        }) { Text(emoji, fontSize = 22.sp) }
                                    }
                                }
                            }

                            "Presets" -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    StudioChip("+ Save Preset") {
                                        customPresets.add("My Look ${customPresets.size + 1}" to params)
                                        Toast.makeText(context, "Preset Saved!", Toast.LENGTH_SHORT).show()
                                    }
                                    customPresets.forEach { (name, savedParams) ->
                                        StudioChip("★ $name") {
                                            pushHistory()
                                            params = savedParams
                                        }
                                    }
                                    StudioChip("Cinema Teal") {
                                        pushHistory()
                                        params = EditParams(
                                            contrast = 1.24f, clarity = 0.25f, vignette = 0.65f,
                                            shadowTint = floatArrayOf(-0.25f, 0.05f, 0.3f),
                                            highlightTint = floatArrayOf(0.25f, 0.08f, -0.2f)
                                        )
                                    }
                                    StudioChip("Golden Hour") {
                                        pushHistory()
                                        params = EditParams(exposure = 0.12f, temperature = 6600f, tint = 14f, vibrance = 0.35f)
                                    }
                                    StudioChip("Moody Dark") {
                                        pushHistory()
                                        params = EditParams(exposure = -0.2f, contrast = 1.35f, saturation = 0.72f, dehaze = 0.25f, vignette = 0.55f)
                                    }
                                    StudioChip("B&W Noir") {
                                        pushHistory()
                                        params = EditParams(contrast = 1.48f, saturation = 0f, clarity = 0.35f, vignette = 0.6f)
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = Color(0xFF1E293B), thickness = 1.dp)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        tools.forEach { tool ->
                            val isSelected = activeTool == tool
                            Surface(
                                color = if (isSelected) Color(0xFF1E3A8A) else Color.Transparent,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.clickable { activeTool = tool }
                            ) {
                                Text(
                                    text = tool,
                                    color = if (isSelected) Color(0xFF60A5FA) else Color(0xFF94A3B8),
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showTextDialog) {
        AlertDialog(
            onDismissRequest = { showTextDialog = false },
            title = { Text("Add Studio Text") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newTextInput,
                        onValueChange = { newTextInput = it },
                        placeholder = { Text("Enter text...") },
                        singleLine = true
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(Color.White, Color.Black, Color(0xFFFACC15), Color(0xFFEF4444), Color(0xFF38BDF8)).forEach { col ->
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(col)
                                    .border(if (newTextColor == col) 2.dp else 0.dp, Color.Green, CircleShape)
                                    .clickable { newTextColor = col }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (newTextInput.isNotBlank()) {
                        val l = CanvasLayer(text = newTextInput, color = newTextColor)
                        layers.add(l)
                        selectedLayerId = l.id
                        newTextInput = ""
                    }
                    showTextDialog = false
                }) { Text("ADD") }
            },
            dismissButton = { TextButton(onClick = { showTextDialog = false }) { Text("CANCEL") } }
        )
    }

    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("Export High-Res Image") },
            text = {
                Column {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StudioChip(if (!exportAsPng) "✓ JPG (Photo)" else "JPG (Photo)") { exportAsPng = false }
                        StudioChip(if (exportAsPng) "✓ PNG (Transparent)" else "PNG (Transparent)") { exportAsPng = true }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("Quality: ${exportQuality.toInt()}%", fontSize = 13.sp)
                    Slider(value = exportQuality, onValueChange = { exportQuality = it }, valueRange = 60f..100f)
                }
            },
            confirmButton = {
                Button(onClick = {
                    val base = displayBitmap ?: return@Button
                    showExportDialog = false
                    scope.launch(Dispatchers.IO) {
                        val composited = compositeLayersOnBitmap(base, layers, viewportSize)
                        val ok = saveFinalImage(context, composited, exportAsPng, exportQuality.toInt())
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, if (ok) "Saved HD to DCIM/ProStudioAI!" else "Export Error", Toast.LENGTH_SHORT).show()
                        }
                    }
                }) { Text("SAVE TO GALLERY") }
            },
            dismissButton = { TextButton(onClick = { showExportDialog = false }) { Text("CANCEL") } }
        )
    }
}

private fun decodeUriToBitmap(context: Context, uri: Uri, maxDim: Int): Bitmap? {
    return try {
        val raw = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val src = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(src) { d, _, _ ->
                d.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                d.isMutableRequired = true
            }
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        }
        val ratio = minOf(maxDim.toFloat() / raw.width, maxDim.toFloat() / raw.height, 1f)
        if (ratio < 1f) {
            Bitmap.createScaledBitmap(raw, (raw.width * ratio).toInt(), (raw.height * ratio).toInt(), true)
        } else raw
    } catch (e: Exception) {
        null
    }
}

private fun applySpotHealBrush(source: Bitmap, touchPos: Offset, viewport: IntSize, radius: Float): Bitmap {
    if (viewport.width <= 0 || viewport.height <= 0) return source
    val out = source.copy(Bitmap.Config.ARGB_8888, true)
    val scaleX = out.width.toFloat() / viewport.width.toFloat()
    val scaleY = out.height.toFloat() / viewport.height.toFloat()
    val cx = (touchPos.x * scaleX).toInt().coerceIn(0, out.width - 1)
    val cy = (touchPos.y * scaleY).toInt().coerceIn(0, out.height - 1)
    val r = (radius * scaleX * 0.5f).toInt().coerceIn(8, 120)

    val left = (cx - r).coerceAtLeast(0)
    val top = (cy - r).coerceAtLeast(0)
    val right = (cx + r).coerceAtMost(out.width)
    val bottom = (cy + r).coerceAtMost(out.height)
    if (right - left < 4 || bottom - top < 4) return out

    val patch = Bitmap.createBitmap(out, left, top, right - left, bottom - top)
    val tiny = Bitmap.createScaledBitmap(patch, 4, 4, true)
    val smooth = Bitmap.createScaledBitmap(tiny, patch.width, patch.height, true)

    val canvas = Canvas(out)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { alpha = 190 }
    val path = android.graphics.Path().apply {
        addCircle((cx - left).toFloat(), (cy - top).toFloat(), r.toFloat(), android.graphics.Path.Direction.CW)
    }
    canvas.save()
    canvas.translate(left.toFloat(), top.toFloat())
    canvas.clipPath(path)
    canvas.drawBitmap(smooth, 0f, 0f, paint)
    canvas.restore()
    return out
}

private fun compositeLayersOnBitmap(base: Bitmap, layers: List<CanvasLayer>, viewport: IntSize): Bitmap {
    if (layers.isEmpty()) return base
    val out = base.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(out)
    val scaleFactor = if (viewport.width > 0) out.width.toFloat() / viewport.width.toFloat() else 1f
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }

    layers.forEach { layer ->
        canvas.save()
        val cx = out.width / 2f + layer.offsetX * scaleFactor
        val cy = out.height / 2f + layer.offsetY * scaleFactor
        canvas.translate(cx, cy)
        canvas.rotate(layer.rotation)
        canvas.scale(layer.scale, layer.scale)
        paint.alpha = (layer.opacity * 255).toInt()
        if (layer.imageBitmap != null) {
            val w = 140f * scaleFactor
            val dst = RectF(-w / 2f, -w / 2f, w / 2f, w / 2f)
            canvas.drawBitmap(layer.imageBitmap, null, dst, paint)
        } else {
            paint.color = layer.color.toArgb()
            paint.textSize = (if (layer.isSticker) 110f else 68f) * scaleFactor
            canvas.drawText(layer.text, 0f, 0f, paint)
        }
        canvas.restore()
    }
    return out
}

private fun saveFinalImage(context: Context, bitmap: Bitmap, isPng: Boolean, quality: Int): Boolean {
    return try {
        val ext = if (isPng) "png" else "jpg"
        val mime = if (isPng) "image/png" else "image/jpeg"
        val fmt = if (isPng) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "ProStudio_${System.currentTimeMillis()}.$ext")
            put(MediaStore.Images.Media.MIME_TYPE, mime)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_DCIM}/ProStudioAI")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return false
        val stream: OutputStream = resolver.openOutputStream(uri) ?: return false
        bitmap.compress(fmt, quality, stream)
        stream.close()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        }
        true
    } catch (e: Exception) {
        false
    }
}

@Composable
fun StudioSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().height(32.dp)
    ) {
        Text(label, color = Color(0xFFCBD5E1), fontSize = 11.sp, maxLines = 1, modifier = Modifier.width(88.dp))
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF3B82F6),
                activeTrackColor = Color(0xFF3B82F6),
                inactiveTrackColor = Color(0xFF1E293B)
            ),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun StudioChip(text: String, onClick: () -> Unit) {
    Surface(
        color = Color(0xFF1E293B),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
        )
    }
}
