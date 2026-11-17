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
    val text: String,
    val isSticker: Boolean = false,
    val color: Color = Color.White,
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
                ProStudioProApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProStudioProApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var baseBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var displayBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var params by remember { mutableStateOf(EditParams()) }
    var studioBgColor by remember { mutableStateOf<Int?>(null) }

    // Undo / Redo History
    val undoStack = remember { mutableStateListOf<EditorSnapshot>() }
    val redoStack = remember { mutableStateListOf<EditorSnapshot>() }

    fun pushHistory() {
        val bmp = baseBitmap ?: return
        undoStack.add(EditorSnapshot(bmp, params, studioBgColor))
        if (undoStack.size > 15) undoStack.removeAt(0)
        redoStack.clear()
    }

    // Layers (Text & Stickers)
    val layers = remember { mutableStateListOf<CanvasLayer>() }
    var selectedLayerId by remember { mutableStateOf<Long?>(null) }
    var showTextDialog by remember { mutableStateOf(false) }
    var newTextInput by remember { mutableStateOf("") }
    var newTextColor by remember { mutableStateOf(Color.White) }

    // UI Tabs & States
    val tools = listOf("Crop", "Light", "Color", "Color Mix", "Effects", "AI & BG", "Layers", "Presets")
    var activeTool by remember { mutableStateOf("Light") }
    var selectedHslColor by remember { mutableStateOf("Red") }
    var isProcessing by remember { mutableStateOf(false) }
    var showOriginal by remember { mutableStateOf(false) }

    // Export Dialog States
    var showExportDialog by remember { mutableStateOf(false) }
    var exportAsPng by remember { mutableStateOf(false) }
    var exportQuality by remember { mutableFloatStateOf(95f) }

    // Canvas Zoom & Size
    var canvasScale by remember { mutableFloatStateOf(1f) }
    var canvasOffset by remember { mutableStateOf(Offset.Zero) }
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }

    // Real-Time GPU Render Pipeline
    LaunchedEffect(baseBitmap, params, studioBgColor) {
        val src = baseBitmap ?: return@LaunchedEffect
        val rendered = withContext(Dispatchers.Default) {
            val graded = GpuColorEngine.render(context, src, params)
            if (studioBgColor != null) {
                val out = Bitmap.createBitmap(graded.width, graded.height, Bitmap.Config.ARGB_8888)
                val c = Canvas(out)
                c.drawColor(studioBgColor!!)
                c.drawBitmap(graded, 0f, 0f, null)
                out
            } else graded
        }
        displayBitmap = rendered
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            val raw = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val src = ImageDecoder.createSource(context.contentResolver, it)
                ImageDecoder.decodeBitmap(src) { d, _, _ ->
                    d.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    d.isMutableRequired = true
                }
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(context.contentResolver, it)
            }
            val maxDim = 2048
            val ratio = minOf(maxDim.toFloat() / raw.width, maxDim.toFloat() / raw.height, 1f)
            val scaled = if (ratio < 1f) {
                Bitmap.createScaledBitmap(raw, (raw.width * ratio).toInt(), (raw.height * ratio).toInt(), true)
            } else raw

            originalBitmap = scaled
            baseBitmap = scaled
            displayBitmap = scaled
            params = EditParams()
            studioBgColor = null
            layers.clear()
            undoStack.clear()
            redoStack.clear()
            canvasScale = 1f
            canvasOffset = Offset.Zero
        }
    }

    Scaffold(
        containerColor = Color(0xFF0B0C10),
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF13151C)),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { picker.launch("image/*") }, modifier = Modifier.size(34.dp)) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Open", tint = Color(0xFF60A5FA))
                        }
                        Spacer(Modifier.width(4.dp))
                        Text("PROSTUDIO", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = Color.White, maxLines = 1)
                    }
                },
                actions = {
                    if (baseBitmap != null) {
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
                            modifier = Modifier.size(34.dp)
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
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(Icons.Default.Redo, contentDescription = "Redo", tint = if (redoStack.isNotEmpty()) Color.White else Color(0xFF475569))
                        }
                        IconButton(
                            onClick = {
                                pushHistory()
                                baseBitmap = originalBitmap
                                params = EditParams()
                                studioBgColor = null
                                layers.clear()
                                canvasScale = 1f
                                canvasOffset = Offset.Zero
                            },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Reset", tint = Color(0xFF94A3B8))
                        }
                        Button(
                            onClick = { showExportDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                            modifier = Modifier.padding(end = 8.dp).height(34.dp)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(4.dp))
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
            // 1. Main Interactive GPU + Layer Canvas
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color(0xFF07080B))
                    .onSizeChanged { viewportSize = it }
                    .pointerInput(Unit) {
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
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            if (selectedLayerId == null) {
                                canvasScale = (canvasScale * zoom).coerceIn(1f, 5f)
                                canvasOffset = if (canvasScale > 1f) canvasOffset + pan else Offset.Zero
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

                        // Interactive Text & Sticker Layers Overlay
                        layers.forEachIndexed { idx, layer ->
                            val isSelected = selectedLayerId == layer.id
                            Box(
                                modifier = Modifier
                                    .offset { IntOffset(layer.offsetX.roundToInt(), layer.offsetY.roundToInt()) }
                                    .graphicsLayer(
                                        scaleX = layer.scale,
                                        scaleY = layer.scale,
                                        rotationZ = layer.rotation
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
                                                scale = (layer.scale * zoom).coerceIn(0.4f, 4f),
                                                rotation = layer.rotation + rot
                                            )
                                        }
                                    }
                                    .padding(6.dp)
                            ) {
                                Text(
                                    text = layer.text,
                                    color = layer.color,
                                    fontSize = if (layer.isSticker) 42.sp else 26.sp,
                                    fontWeight = FontWeight.ExtraBold
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
                        Text("LIGHTROOM + PICSART STUDIO", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("100% Offline GPU & AI Engine", color = Color(0xFF94A3B8), fontSize = 12.sp)
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { picker.launch("image/*") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                        ) {
                            Text("IMPORT PHOTO FROM GALLERY", fontWeight = FontWeight.Bold)
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
                            Text("Running On-Device Neural Engine...", color = Color.White, fontSize = 13.sp)
                        }
                    }
                }
            }

            // 2. Bottom Pro Editing Controls Panel
            if (baseBitmap != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF13151C))
                        .padding(top = 8.dp, bottom = 6.dp)
                ) {
                    // Tool Sub-Panel
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 125.dp, max = 185.dp)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 14.dp)
                    ) {
                        when (activeTool) {
                            "Crop" -> {
                                Text("ASPECT RATIO & GEOMETRY", color = Color(0xFF94A3B8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val ratios = listOf(
                                        "1:1 Square" to 1f,
                                        "4:5 Portrait" to 0.8f,
                                        "9:16 Story" to 0.5625f,
                                        "16:9 Wide" to 1.777f
                                    )
                                    ratios.forEach { (label, r) ->
                                        StudioChip(label) {
                                            pushHistory()
                                            baseBitmap = GpuColorEngine.cropAndTransform(baseBitmap!!, r, 0f, false)
                                        }
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    StudioChip("Rotate 90°") {
                                        pushHistory()
                                        baseBitmap = GpuColorEngine.cropAndTransform(baseBitmap!!, null, 90f, false)
                                    }
                                    StudioChip("Flip Horizontal") {
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
                                        val isSel = selectedHslColor == name
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(col)
                                                .border(if (isSel) 2.5.dp else 0.dp, Color.White, CircleShape)
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

                            "Effects" -> {
                                StudioSlider("Clarity", params.clarity, -0.4f..1f) { params = params.copy(clarity = it) }
                                StudioSlider("Dehaze", params.dehaze, 0f..0.8f) { params = params.copy(dehaze = it) }
                                StudioSlider("Sharpen", params.sharpen, 0f..3.5f) { params = params.copy(sharpen = it) }
                                StudioSlider("Vignette", 1f - params.vignette, 0f..0.65f) { params = params.copy(vignette = 1f - it) }
                                StudioSlider("Film Grain", params.grain, 0f..0.35f) { params = params.copy(grain = it) }
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
                                Text("STUDIO BACKDROP (AFTER CUTOUT):", color = Color(0xFF94A3B8), fontSize = 10.sp)
                                Spacer(Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    val bgs = listOf(
                                        "None" to null,
                                        "White" to android.graphics.Color.WHITE,
                                        "Dark" to android.graphics.Color.parseColor("#111827"),
                                        "Royal Blue" to android.graphics.Color.parseColor("#1D4ED8"),
                                        "Crimson" to android.graphics.Color.parseColor("#B91C1C"),
                                        "Emerald" to android.graphics.Color.parseColor("#047857")
                                    )
                                    bgs.forEach { (label, col) ->
                                        StudioChip(label) {
                                            pushHistory()
                                            studioBgColor = col
                                        }
                                    }
                                }
                            }

                            "Layers" -> {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                    StudioChip("+ Add Text") { showTextDialog = true }
                                    if (selectedLayerId != null) {
                                        StudioChip("Delete Selected") {
                                            layers.removeAll { it.id == selectedLayerId }
                                            selectedLayerId = null
                                        }
                                    }
                                }
                                Spacer(Modifier.height(6.dp))
                                Text("QUICK STICKERS (DRAG & PINCH ON PHOTO):", color = Color(0xFF94A3B8), fontSize = 10.sp)
                                Row(
                                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    val stickers = listOf("🔥", "👑", "⚡", "🎬", "✨", "💯", "🎧", "😎")
                                    stickers.forEach { emoji ->
                                        TextButton(onClick = {
                                            val layer = CanvasLayer(text = emoji, isSticker = true)
                                            layers.add(layer)
                                            selectedLayerId = layer.id
                                        }) {
                                            Text(emoji, fontSize = 24.sp)
                                        }
                                    }
                                }
                            }

                            "Presets" -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    StudioChip("Teal & Orange") {
                                        pushHistory()
                                        params = EditParams(
                                            contrast = 1.22f, temperature = 4600f, clarity = 0.25f, vignette = 0.65f,
                                            hslChannels = mapOf(
                                                "Orange" to HslChannel(saturation = 0.25f, luminance = 0.1f),
                                                "Blue" to HslChannel(hue = -0.35f, saturation = 0.35f)
                                            )
                                        )
                                    }
                                    StudioChip("Golden Sunset") {
                                        pushHistory()
                                        params = EditParams(exposure = 0.12f, temperature = 6600f, tint = 14f, vibrance = 0.35f)
                                    }
                                    StudioChip("Moody Dark") {
                                        pushHistory()
                                        params = EditParams(exposure = -0.2f, contrast = 1.35f, saturation = 0.72f, dehaze = 0.25f, vignette = 0.55f)
                                    }
                                    StudioChip("Vintage Film") {
                                        pushHistory()
                                        params = EditParams(contrast = 0.92f, blacks = 0.12f, temperature = 5600f, grain = 0.18f, vignette = 0.7f)
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

                    // Fixed Single-Line Horizontal Scrollable Bottom Toolbar (No Vertical Text Wrapping!)
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

    // Add Text Dialog
    if (showTextDialog) {
        AlertDialog(
            onDismissRequest = { showTextDialog = false },
            title = { Text("Add Studio Text") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newTextInput,
                        onValueChange = { newTextInput = it },
                        placeholder = { Text("Enter title or caption...") },
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
            dismissButton = {
                TextButton(onClick = { showTextDialog = false }) { Text("CANCEL") }
            }
        )
    }

    // Pro Export Settings Dialog (PDF 1 & 3)
    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("Export High-Res Image") },
            text = {
                Column {
                    Text("Format:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
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
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) { Text("CANCEL") }
            }
        )
    }
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
        paint.color = layer.color.toArgb()
        paint.textSize = (if (layer.isSticker) 110f else 68f) * scaleFactor
        canvas.drawText(layer.text, 0f, 0f, paint)
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
        Text(label, color = Color(0xFFCBD5E1), fontSize = 11.sp, maxLines = 1, modifier = Modifier.width(84.dp))
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
