package com.dhanesh.prostudiopro

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(background = Color(0xFF0D0E12))) {
                ProStudioScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProStudioScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var baseBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var displayBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var params by remember { mutableStateOf(EditParams()) }
    var activeTab by remember { mutableStateOf("Light") }
    var isProcessing by remember { mutableStateOf(false) }
    var showOriginal by remember { mutableStateOf(false) }
    var hasTransparentBg by remember { mutableStateOf(false) }

    // Pinch-to-Zoom & Pan State
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 5f)
        offset = if (scale > 1f) offset + panChange else Offset.Zero
    }

    // Real-Time GPU Render Trigger
    LaunchedEffect(baseBitmap, params) {
        val src = baseBitmap ?: return@LaunchedEffect
        val rendered = withContext(Dispatchers.Default) {
            GpuColorEngine.render(context, src, params)
        }
        displayBitmap = rendered
    }

    // Gallery Image Picker
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            val raw = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val src = ImageDecoder.createSource(context.contentResolver, it)
                ImageDecoder.decodeBitmap(src) { decoder, _, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    decoder.isMutableRequired = true
                }
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(context.contentResolver, it)
            }
            // Limit max dimension to 2048px for ultra-fast 120Hz GPU rendering
            val maxDim = 2048
            val ratio = minOf(maxDim.toFloat() / raw.width, maxDim.toFloat() / raw.height, 1f)
            val scaled = if (ratio < 1f) {
                Bitmap.createScaledBitmap(raw, (raw.width * ratio).toInt(), (raw.height * ratio).toInt(), true)
            } else raw

            baseBitmap = scaled
            displayBitmap = scaled
            params = EditParams()
            hasTransparentBg = false
            scale = 1f
            offset = Offset.Zero
        }
    }

    Scaffold(
        containerColor = Color(0xFF0D0E12),
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF14161D)),
                title = {
                    Text("PROSTUDIO AI", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                },
                actions = {
                    if (baseBitmap != null) {
                        IconButton(onClick = {
                            params = EditParams()
                            scale = 1f
                            offset = Offset.Zero
                        }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Reset", tint = Color(0xFF94A3B8))
                        }
                        Button(
                            onClick = {
                                val finalBmp = displayBitmap ?: return@Button
                                scope.launch(Dispatchers.IO) {
                                    val saved = OfflineAiEngine.saveToGallery(context, finalBmp, hasTransparentBg)
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(
                                            context,
                                            if (saved) "Saved HD in DCIM/ProStudioAI!" else "Save Failed",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("EXPORT", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
            // 1. Main GPU Canvas Viewport
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color(0xFF090A0D))
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                showOriginal = true
                                tryAwaitRelease()
                                showOriginal = false
                            },
                            onDoubleTap = {
                                scale = if (scale > 1f) 1f else 2.5f
                                offset = Offset.Zero
                            }
                        )
                    }
                    .transformable(state = transformState),
                contentAlignment = Alignment.Center
            ) {
                val activeImg = if (showOriginal) baseBitmap else displayBitmap
                if (activeImg != null) {
                    Image(
                        bitmap = activeImg.asImageBitmap(),
                        contentDescription = "Canvas",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offset.x,
                                translationY = offset.y
                            )
                    )
                    if (showOriginal) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 12.dp)
                        ) {
                            Text("ORIGINAL", color = Color.White, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                        }
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = Color(0xFF3B82F6), modifier = Modifier.size(56.dp))
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { picker.launch("image/*") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
                        ) {
                            Text("OPEN PHOTO FROM GALLERY", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (isProcessing) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.65f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Color(0xFF3B82F6))
                            Spacer(Modifier.height(10.dp))
                            Text("AI Processing Offline...", color = Color.White, fontSize = 13.sp)
                        }
                    }
                }
            }

            // 2. Bottom Pro Controls Panel
            if (baseBitmap != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF14161D))
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    // Active Tool Controls
                    when (activeTab) {
                        "Light" -> {
                            ProSlider("Exposure", params.exposure, -2f..2f) { params = params.copy(exposure = it) }
                            ProSlider("Contrast", params.contrast, 0.5f..2f) { params = params.copy(contrast = it) }
                            ProSlider("Highlights", params.highlights, 0f..1f) { params = params.copy(highlights = it) }
                            ProSlider("Shadows", params.shadows, 0f..1f) { params = params.copy(shadows = it) }
                        }
                        "Color" -> {
                            ProSlider("Temp", params.temperature, 2500f..8500f) { params = params.copy(temperature = it) }
                            ProSlider("Tint", params.tint, -80f..80f) { params = params.copy(tint = it) }
                            ProSlider("Vibrance", params.vibrance, -1f..1f) { params = params.copy(vibrance = it) }
                            ProSlider("Saturation", params.saturation, 0f..2f) { params = params.copy(saturation = it) }
                        }
                        "Detail" -> {
                            ProSlider("Sharpen", params.sharpen, 0f..3.5f) { params = params.copy(sharpen = it) }
                            ProSlider("Vignette", 1f - params.vignette, 0f..0.65f) { params = params.copy(vignette = 1f - it) }
                        }
                        "AI Studio" -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = {
                                        val src = baseBitmap ?: return@Button
                                        scope.launch {
                                            isProcessing = true
                                            val cutout = withContext(Dispatchers.Default) {
                                                OfflineAiEngine.removeBackground(src)
                                            }
                                            baseBitmap = cutout
                                            hasTransparentBg = true
                                            isProcessing = false
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("AI BG REMOVE", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = {
                                        val src = baseBitmap ?: return@Button
                                        scope.launch {
                                            isProcessing = true
                                            val blurred = withContext(Dispatchers.Default) {
                                                OfflineAiEngine.applyPortraitBlur(context, src, 22f)
                                            }
                                            baseBitmap = blurred
                                            isProcessing = false
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0EA5E9)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("DSLR BLUR", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        "Presets" -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                PresetChip("Cinematic") { params = EditParams(contrast = 1.25f, temperature = 4400f, vibrance = 0.25f, vignette = 0.65f) }
                                PresetChip("Warm Golden") { params = EditParams(exposure = 0.15f, temperature = 6400f, tint = 12f, saturation = 1.2f) }
                                PresetChip("Moody Dark") { params = EditParams(exposure = -0.25f, contrast = 1.35f, saturation = 0.75f, vignette = 0.55f) }
                                PresetChip("Vibrant Pop") { params = EditParams(contrast = 1.15f, vibrance = 0.55f, saturation = 1.25f, sharpen = 0.8f) }
                                PresetChip("B&W Noir") { params = EditParams(contrast = 1.45f, saturation = 0f, sharpen = 1.0f, vignette = 0.6f) }
                            }
                        }
                    }

                    Spacer(Modifier.height(6.dp))

                    // Bottom Navigation Tabs
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val tabs = listOf("Light", "Color", "Detail", "AI Studio", "Presets")
                        tabs.forEach { tab ->
                            val selected = activeTab == tab
                            TextButton(
                                onClick = { activeTab = tab },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = if (selected) Color(0xFF3B82F6) else Color(0xFF94A3B8)
                                )
                            ) {
                                Text(tab, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(34.dp)
    ) {
        Text(label, color = Color(0xFFCBD5E1), fontSize = 11.sp, modifier = Modifier.width(72.dp))
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF3B82F6),
                activeTrackColor = Color(0xFF3B82F6),
                inactiveTrackColor = Color(0xFF334155)
            ),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun PresetChip(name: String, onClick: () -> Unit) {
    FilledTonalButton(
        onClick = onClick,
        colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFF1E293B), contentColor = Color.White),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(name, fontSize = 12.sp)
    }
}
