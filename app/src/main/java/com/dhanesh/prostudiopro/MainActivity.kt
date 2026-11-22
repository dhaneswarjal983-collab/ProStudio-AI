package com.dhanesh.prostudiopro

import android.content.ContentValues
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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.roundToInt

private val Ink = Color(0xFF101114)
private val SurfaceDark = Color(0xFF1B1D22)
private val SurfaceRaised = Color(0xFF252830)
private val Accent = Color(0xFFD5F36A)
private val SecondaryText = Color(0xFFA7ABB4)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(16, 17, 20)
        window.navigationBarColor = android.graphics.Color.rgb(16, 17, 20)
        setContent { StudioApp() }
    }
}

private enum class StudioTab(val label: String) {
    HOME("Home"), LIBRARY("Library"), EDIT("Edit"), PRESETS("Looks"), PROFILE("Profile")
}

private data class Adjustments(
    val exposure: Float = 0f,
    val contrast: Float = 1f,
    val highlights: Float = 0f,
    val shadows: Float = 0f,
    val whites: Float = 0f,
    val blacks: Float = 0f,
    val toneShadows: Float = 0f,
    val toneMidtones: Float = 0f,
    val toneHighlights: Float = 0f,
    val temperature: Float = 0f,
    val tint: Float = 0f,
    val saturation: Float = 1f,
    val vibrance: Float = 0f,
    val clarity: Float = 0f,
    val dehaze: Float = 0f,
    val vignette: Float = 0f,
    val grain: Float = 0f,
    val sharpening: Float = 0f,
    val noiseReduction: Float = 0f,
    val shadowTintHue: Float = 220f,
    val shadowTintAmount: Float = 0f,
    val highlightTintHue: Float = 45f,
    val highlightTintAmount: Float = 0f,
    val midtoneTintHue: Float = 35f,
    val midtoneTintAmount: Float = 0f,
    val colorGradeBalance: Float = 0f,
    val rotation: Int = 0,
    val rotationFine: Float = 0f,
    val flipHorizontal: Boolean = false,
    val flipVertical: Boolean = false,
    val cropRatio: Float = 0f,
    val cropOffsetX: Float = 0f,
    val cropOffsetY: Float = 0f,
    val colorMixer: List<ColorMix> = List(8) { ColorMix() }
)

private data class ColorMix(
    val hue: Float = 0f,
    val saturation: Float = 0f,
    val luminance: Float = 0f
)

private data class Look(val name: String, val values: Adjustments)

private val looks = listOf(
    Look("Original", Adjustments()),
    Look("Soft Light", Adjustments(exposure = .25f, contrast = .92f, highlights = -.18f, shadows = .22f, saturation = 1.04f)),
    Look("Golden Hour", Adjustments(exposure = .12f, temperature = .32f, tint = .06f, saturation = 1.12f, vibrance = .18f)),
    Look("Cinematic", Adjustments(contrast = 1.22f, highlights = -.25f, shadows = -.08f, temperature = -.12f, saturation = .88f, dehaze = .12f, vignette = .2f)),
    Look("Matte", Adjustments(contrast = .86f, blacks = .18f, highlights = -.16f, saturation = .9f, grain = .12f)),
    Look("Mono", Adjustments(saturation = 0f, contrast = 1.12f))
)

@Composable
private fun StudioApp() {
    var tab by remember { mutableStateOf(StudioTab.HOME) }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var savedCount by remember { mutableIntStateOf(0) }
    var pendingLook by remember { mutableStateOf(Adjustments()) }
    MaterialTheme(colorScheme = darkColorScheme(
        background = Ink, surface = SurfaceDark, primary = Accent,
        onPrimary = Ink, onBackground = Color.White, onSurface = Color.White
    )) {
        Scaffold(
            containerColor = Ink,
            bottomBar = {
                NavigationBar(containerColor = SurfaceDark, contentColor = Color.White) {
                    StudioTab.entries.forEach { item ->
                        NavigationBarItem(
                            selected = tab == item,
                            onClick = { tab = item },
                            icon = { Text(item.label.take(1), fontWeight = FontWeight.Bold) },
                            label = { Text(item.label, fontSize = 10.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Accent, selectedTextColor = Accent,
                                indicatorColor = SurfaceRaised, unselectedIconColor = SecondaryText,
                                unselectedTextColor = SecondaryText
                            )
                        )
                    }
                }
            }
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                when (tab) {
                    StudioTab.HOME -> HomeScreen(
                        hasPhoto = selectedBitmap != null,
                        onOpen = { tab = StudioTab.EDIT },
                        onPick = { tab = StudioTab.LIBRARY }
                    )
                    StudioTab.LIBRARY -> LibraryScreen(
                        selectedUri = selectedUri,
                        onSelected = { uri, bitmap ->
                            selectedUri = uri
                            selectedBitmap = bitmap
                            tab = StudioTab.EDIT
                        }
                    )
                    StudioTab.EDIT -> EditorScreen(
                        source = selectedBitmap,
                        initialValues = pendingLook,
                        onPick = { tab = StudioTab.LIBRARY },
                        onSaved = { savedCount++ }
                    )
                    StudioTab.PRESETS -> LooksScreen(
                        onApply = { look -> pendingLook = look.values; tab = if (selectedBitmap != null) StudioTab.EDIT else StudioTab.LIBRARY }
                    )
                    StudioTab.PROFILE -> ProfileScreen(savedCount)
                }
            }
        }
    }
}

@Composable
private fun Page(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
        content = content
    )
}

@Composable
private fun BrandHeader(kicker: String = "PRO PHOTO STUDIO") {
    Row(
        Modifier.fillMaxWidth().padding(top = 22.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text("LUMEN", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black, letterSpacing = 3.sp)
            Text(kicker, color = SecondaryText, fontSize = 9.sp, letterSpacing = 2.sp)
        }
        Box(Modifier.size(38.dp).clip(RoundedCornerShape(13.dp)).background(Accent), contentAlignment = Alignment.Center) {
            Text("L", color = Ink, fontWeight = FontWeight.Black, fontSize = 23.sp)
        }
    }
}

@Composable
private fun HomeScreen(hasPhoto: Boolean, onOpen: () -> Unit, onPick: () -> Unit) {
    Page {
        BrandHeader("CREATIVE PHOTO WORKSPACE")
        Text("Your photos.\nYour vision.", color = Color.White, fontSize = 34.sp, lineHeight = 39.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        Text("A powerful mobile editing workspace, built to grow with your creativity.", color = SecondaryText, fontSize = 14.sp)
        Spacer(Modifier.height(26.dp))
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp))
                .background(SurfaceRaised).padding(22.dp)
        ) {
            Text("THE CREATIVE EDIT", color = Accent, fontSize = 10.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(14.dp))
            Text(if (hasPhoto) "Continue your edit" else "Start with a photo", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("Light, color and detail controls in one focused editor.", color = SecondaryText, fontSize = 13.sp)
            Spacer(Modifier.height(22.dp))
            Button(
                onClick = if (hasPhoto) onOpen else onPick,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Accent),
                shape = RoundedCornerShape(15.dp)
            ) { Text(if (hasPhoto) "CONTINUE EDITING" else "CHOOSE A PHOTO", color = Ink, fontWeight = FontWeight.Bold) }
        }
        Spacer(Modifier.height(26.dp))
        Text("YOUR TOOLKIT", color = SecondaryText, fontSize = 11.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FeatureTile("01", "Light", "Exposure & tone", Modifier.weight(1f))
            FeatureTile("02", "Color", "Temperature & mix", Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FeatureTile("03", "Effects", "Clarity & grain", Modifier.weight(1f))
            FeatureTile("04", "Looks", "Creative presets", Modifier.weight(1f))
        }
        Spacer(Modifier.height(28.dp))
        Text("Designed as an original editing app. Feature availability will expand as each audited workflow is implemented and tested.", color = SecondaryText, fontSize = 11.sp)
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun FeatureTile(number: String, title: String, detail: String, modifier: Modifier = Modifier) {
    Column(modifier.clip(RoundedCornerShape(18.dp)).background(SurfaceDark).padding(15.dp)) {
        Text(number, color = Accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text(detail, color = SecondaryText, fontSize = 10.sp)
    }
}

@Composable
private fun LibraryScreen(selectedUri: Uri?, onSelected: (Uri, Bitmap) -> Unit) {
    val context = LocalContext.current
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            loading = true
            error = ""
            scope.launch {
                val bitmap = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
                }
                loading = false
                if (bitmap != null) onSelected(uri, bitmap) else error = "Photo load nahi hui. Dusri image select karein."
            }
        }
    }
    Page {
        BrandHeader("PHOTO LIBRARY")
        Text("Bring a photo into your workspace.", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        Text("Photos are selected using Android's system photo picker. The original file is not changed.", color = SecondaryText, fontSize = 13.sp)
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            modifier = Modifier.fillMaxWidth().height(54.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Accent),
            shape = RoundedCornerShape(15.dp)
        ) { Text(if (loading) "LOADING PHOTO..." else "SELECT FROM DEVICE", color = Ink, fontWeight = FontWeight.Bold) }
        if (error.isNotBlank()) Text(error, color = Color(0xFFFF8B8B), modifier = Modifier.padding(top = 12.dp))
        if (selectedUri != null) Text("A photo is loaded in the editor.", color = Accent, modifier = Modifier.padding(top = 18.dp))
        Spacer(Modifier.height(30.dp))
        Text("SUPPORTED WORKFLOW", color = SecondaryText, fontSize = 10.sp, letterSpacing = 2.sp)
        Spacer(Modifier.height(10.dp))
        Text("Select → Edit → Compare → Export", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun EditorScreen(source: Bitmap?, initialValues: Adjustments, onPick: () -> Unit, onSaved: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var values by remember(source) { mutableStateOf(initialValues) }
    var rendered by remember { mutableStateOf<Bitmap?>(null) }
    var busy by remember { mutableStateOf(false) }
    var showOriginal by remember { mutableStateOf(false) }
    var exportFormat by remember { mutableStateOf("JPEG") }
    var exportQuality by remember { mutableFloatStateOf(96f) }
    var notice by remember { mutableStateOf("") }
    var undo by remember(source) { mutableStateOf(listOf(initialValues)) }
    var undoIndex by remember(source) { mutableIntStateOf(0) }

    fun update(next: Adjustments) {
        undo = (undo.take(undoIndex + 1) + next).takeLast(30)
        undoIndex = undo.lastIndex
        values = next
    }

    LaunchedEffect(source, values) {
        if (source != null) {
            busy = true
            rendered = withContext(Dispatchers.Default) { renderPhoto(source, values, 1500) }
            busy = false
        } else rendered = null
    }

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            BrandHeader("PHOTO EDITOR")
            if (source == null) {
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(SurfaceDark).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Your editing canvas", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text("Choose a photo to begin editing.", color = SecondaryText)
                    Spacer(Modifier.height(18.dp))
                    Button(onClick = onPick, colors = ButtonDefaults.buttonColors(containerColor = Accent)) {
                        Text("CHOOSE PHOTO", color = Ink, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Box(
                    Modifier.fillMaxWidth().height(310.dp).clip(RoundedCornerShape(20.dp)).background(SurfaceRaised),
                    contentAlignment = Alignment.Center
                ) {
                    val image = if (showOriginal) source else rendered
                    if (image != null) Image(
                        bitmap = image.asImageBitmap(),
                        contentDescription = "Edited photo preview",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                    if (busy) CircularProgressIndicator(color = Accent, modifier = Modifier.align(Alignment.TopEnd).padding(12.dp).size(22.dp), strokeWidth = 2.dp)
                }
                if (rendered != null && !showOriginal) {
                    val levels = remember(rendered) { sampleChannelLevels(rendered!!) }
                    val histogram = remember(rendered) { sampleLuminanceHistogram(rendered!!) }
                    Column(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp)) {
                        Text("LUMINANCE HISTOGRAM", color = Accent, fontSize = 10.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
                        Canvas(Modifier.fillMaxWidth().height(74.dp).padding(top = 7.dp, bottom = 5.dp)) {
                            val maxBin = (histogram.maxOrNull() ?: 1).coerceAtLeast(1).toFloat()
                            val slot = size.width / histogram.size
                            histogram.forEachIndexed { index, count ->
                                val barHeight = (count / maxBin) * size.height
                                val x = index * slot
                                drawLine(
                                    color = Color(0xFFB7D66A),
                                    start = Offset(x + slot * .5f, size.height),
                                    end = Offset(x + slot * .5f, size.height - barHeight),
                                    strokeWidth = (slot * .82f).coerceAtLeast(1f)
                                )
                            }
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("SHADOWS", color = SecondaryText, fontSize = 8.sp)
                            Text("MIDTONES", color = SecondaryText, fontSize = 8.sp)
                            Text("HIGHLIGHTS", color = SecondaryText, fontSize = 8.sp)
                        }
                        Text("RGB LEVELS", color = Accent, fontSize = 10.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
                        listOf("R" to levels[0], "G" to levels[1], "B" to levels[2]).forEach { (channel, level) ->
                            Row(Modifier.fillMaxWidth().padding(top = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(channel, color = SecondaryText, fontSize = 10.sp, modifier = Modifier.width(18.dp))
                                Box(Modifier.weight(1f).height(5.dp).clip(RoundedCornerShape(4.dp)).background(SurfaceDark)) {
                                    Box(Modifier.fillMaxWidth(level.coerceIn(0f, 1f)).height(5.dp).background(Accent, RoundedCornerShape(4.dp)))
                                }
                                Text("${(level * 100).roundToInt()}%", color = SecondaryText, fontSize = 9.sp, modifier = Modifier.width(36.dp), textAlign = TextAlign.End)
                            }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SmallAction("BEFORE / AFTER", { showOriginal = !showOriginal }, Modifier.weight(1f))
                    SmallAction("UNDO", { if (undoIndex > 0) { undoIndex--; values = undo[undoIndex] } }, Modifier.weight(1f), undoIndex == 0)
                    SmallAction("REDO", { if (undoIndex < undo.lastIndex) { undoIndex++; values = undo[undoIndex] } }, Modifier.weight(1f), undoIndex >= undo.lastIndex)
                    SmallAction("RESET", { update(Adjustments()) }, Modifier.weight(1f))
                }
                Text("TRANSFORM", color = Accent, fontSize = 11.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SmallAction("ROTATE 90°", { update(values.copy(rotation = (values.rotation + 90) % 360)) }, Modifier.weight(1f))
                    SmallAction("FLIP H", { update(values.copy(flipHorizontal = !values.flipHorizontal)) }, Modifier.weight(1f))
                    SmallAction("FLIP V", { update(values.copy(flipVertical = !values.flipVertical)) }, Modifier.weight(1f))
                }
                Text("CROP & ASPECT", color = Accent, fontSize = 11.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
                Text("Center crop presets. Original keeps the full frame.", color = SecondaryText, fontSize = 11.sp)
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    listOf("ORIGINAL" to 0f, "1:1" to 1f, "4:3" to (4f / 3f), "3:2" to 1.5f, "16:9" to (16f / 9f)).forEach { (label, ratio) ->
                        SmallAction(label, { update(values.copy(cropRatio = ratio)) }, Modifier.weight(1f), values.cropRatio == ratio)
                    }
                }
                AdjustmentSlider("Crop horizontal", values.cropOffsetX, -1f, 1f) { update(values.copy(cropOffsetX = it)) }
                AdjustmentSlider("Crop vertical", values.cropOffsetY, -1f, 1f) { update(values.copy(cropOffsetY = it)) }
                AdjustmentSlider("Straighten", values.rotationFine, -45f, 45f) { update(values.copy(rotationFine = it)) }
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("LIGHT", color = Accent, fontSize = 11.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
                    SmallAction("AUTO", { source?.let { update(autoAdjust(it, values)) } })
                }
                AdjustmentSlider("Exposure", values.exposure, -1f, 1f) { update(values.copy(exposure = it)) }
                AdjustmentSlider("Contrast", values.contrast, .5f, 1.8f) { update(values.copy(contrast = it)) }
                AdjustmentSlider("Highlights", values.highlights, -1f, 1f) { update(values.copy(highlights = it)) }
                AdjustmentSlider("Shadows", values.shadows, -1f, 1f) { update(values.copy(shadows = it)) }
                AdjustmentSlider("Whites", values.whites, -1f, 1f) { update(values.copy(whites = it)) }
                AdjustmentSlider("Blacks", values.blacks, -1f, 1f) { update(values.copy(blacks = it)) }
                Spacer(Modifier.height(14.dp))
                Text("TONE CURVE", color = Accent, fontSize = 11.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
                Text("Parametric control for shadow, midtone and highlight regions.", color = SecondaryText, fontSize = 11.sp)
                AdjustmentSlider("Shadow curve", values.toneShadows, -1f, 1f) { update(values.copy(toneShadows = it)) }
                AdjustmentSlider("Midtone curve", values.toneMidtones, -1f, 1f) { update(values.copy(toneMidtones = it)) }
                AdjustmentSlider("Highlight curve", values.toneHighlights, -1f, 1f) { update(values.copy(toneHighlights = it)) }
                Spacer(Modifier.height(14.dp))
                Text("COLOR", color = Accent, fontSize = 11.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
                AdjustmentSlider("Temperature", values.temperature, -1f, 1f) { update(values.copy(temperature = it)) }
                AdjustmentSlider("Tint", values.tint, -1f, 1f) { update(values.copy(tint = it)) }
                AdjustmentSlider("Saturation", values.saturation, 0f, 2f) { update(values.copy(saturation = it)) }
                AdjustmentSlider("Vibrance", values.vibrance, -1f, 1f) { update(values.copy(vibrance = it)) }
                Spacer(Modifier.height(14.dp))
                var selectedMix by remember { mutableIntStateOf(0) }
                val mixNames = listOf("Red", "Orange", "Yellow", "Green", "Aqua", "Blue", "Purple", "Magenta")
                Text("COLOR MIX", color = Accent, fontSize = 11.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
                Text("Adjust individual color ranges without changing every color in the photo.", color = SecondaryText, fontSize = 11.sp)
                Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    mixNames.forEachIndexed { index, name ->
                        SmallAction(name.uppercase(), { selectedMix = index }, Modifier.weight(1f))
                    }
                }
                val activeMix = values.colorMixer[selectedMix]
                AdjustmentSlider("Hue", activeMix.hue, -100f, 100f) {
                    val updated = values.colorMixer.toMutableList()
                    updated[selectedMix] = activeMix.copy(hue = it)
                    update(values.copy(colorMixer = updated))
                }
                AdjustmentSlider("Saturation", activeMix.saturation, -100f, 100f) {
                    val updated = values.colorMixer.toMutableList()
                    updated[selectedMix] = activeMix.copy(saturation = it)
                    update(values.copy(colorMixer = updated))
                }
                AdjustmentSlider("Luminance", activeMix.luminance, -100f, 100f) {
                    val updated = values.colorMixer.toMutableList()
                    updated[selectedMix] = activeMix.copy(luminance = it)
                    update(values.copy(colorMixer = updated))
                }
                Spacer(Modifier.height(14.dp))
                Text("EFFECTS", color = Accent, fontSize = 11.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
                AdjustmentSlider("Clarity", values.clarity, 0f, 1f) { update(values.copy(clarity = it)) }
                AdjustmentSlider("Dehaze", values.dehaze, 0f, 1f) { update(values.copy(dehaze = it)) }
                AdjustmentSlider("Vignette", values.vignette, 0f, 1f) { update(values.copy(vignette = it)) }
                AdjustmentSlider("Grain", values.grain, 0f, 1f) { update(values.copy(grain = it)) }
                Spacer(Modifier.height(14.dp))
                Text("DETAIL", color = Accent, fontSize = 11.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
                AdjustmentSlider("Sharpening", values.sharpening, 0f, 1f) { update(values.copy(sharpening = it)) }
                AdjustmentSlider("Noise reduction", values.noiseReduction, 0f, 1f) { update(values.copy(noiseReduction = it)) }
                Spacer(Modifier.height(14.dp))
                Text("COLOR GRADING", color = Accent, fontSize = 11.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
                Text("Tint shadows and highlights independently.", color = SecondaryText, fontSize = 11.sp)
                AdjustmentSlider("Shadow hue", values.shadowTintHue, 0f, 360f) { update(values.copy(shadowTintHue = it)) }
                AdjustmentSlider("Shadow amount", values.shadowTintAmount, 0f, 1f) { update(values.copy(shadowTintAmount = it)) }
                AdjustmentSlider("Highlight hue", values.highlightTintHue, 0f, 360f) { update(values.copy(highlightTintHue = it)) }
                AdjustmentSlider("Highlight amount", values.highlightTintAmount, 0f, 1f) { update(values.copy(highlightTintAmount = it)) }
                AdjustmentSlider("Midtone hue", values.midtoneTintHue, 0f, 360f) { update(values.copy(midtoneTintHue = it)) }
                AdjustmentSlider("Midtone amount", values.midtoneTintAmount, 0f, 1f) { update(values.copy(midtoneTintAmount = it)) }
                AdjustmentSlider("Balance", values.colorGradeBalance, -1f, 1f) { update(values.copy(colorGradeBalance = it)) }
                Spacer(Modifier.height(14.dp))
                Text("QUICK LOOKS", color = Accent, fontSize = 11.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    looks.take(3).forEach { look ->
                        SmallAction(look.name.uppercase(), { update(look.values) }, Modifier.weight(1f))
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                    looks.drop(3).forEach { look ->
                        SmallAction(look.name.uppercase(), { update(look.values) }, Modifier.weight(1f))
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text("EXPORT SETTINGS", color = Accent, fontSize = 11.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
                Text("Choose an output format and JPEG quality.", color = SecondaryText, fontSize = 11.sp)
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SmallAction("JPEG", { exportFormat = "JPEG" }, Modifier.weight(1f), exportFormat == "JPEG")
                    SmallAction("PNG", { exportFormat = "PNG" }, Modifier.weight(1f), exportFormat == "PNG")
                }
                if (exportFormat == "JPEG") {
                    AdjustmentSlider("JPEG quality", exportQuality, 70f, 100f) { exportQuality = it }
                }
                Button(
                    onClick = {
                        val original = source
                        if (original != null) scope.launch {
                            busy = true
                            val output = withContext(Dispatchers.Default) { renderPhoto(original, values, Int.MAX_VALUE) }
                            val saved = withContext(Dispatchers.IO) { savePhoto(context, output, exportFormat, exportQuality.roundToInt()) }
                            busy = false
                            notice = if (saved) "Photo exported to Pictures/Lumen Studio." else "Export failed. Please try again."
                            if (saved) onSaved()
                        }
                    },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Accent),
                    shape = RoundedCornerShape(15.dp)
                ) { Text(if (busy) "PROCESSING..." else "EXPORT PHOTO", color = Ink, fontWeight = FontWeight.Bold) }
                if (notice.isNotBlank()) Text(notice, color = Accent, fontSize = 12.sp, modifier = Modifier.padding(top = 10.dp))
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SmallAction(label: String, action: () -> Unit, modifier: Modifier = Modifier, disabled: Boolean = false) {
    OutlinedButton(
        onClick = action, enabled = !disabled, modifier = modifier.height(42.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Accent)
    ) { Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1) }
}

@Composable
private fun AdjustmentSlider(label: String, value: Float, min: Float, max: Float, onChange: (Float) -> Unit) {
    Column(Modifier.padding(top = 10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = Color.White, fontSize = 13.sp)
            Text(String.format("%.2f", value), color = SecondaryText, fontSize = 11.sp)
        }
        Slider(
            value = value.coerceIn(min, max),
            onValueChange = onChange,
            valueRange = min..max,
            colors = SliderDefaults.colors(
                thumbColor = Accent, activeTrackColor = Accent,
                inactiveTrackColor = SurfaceRaised
            )
        )
    }
}

@Composable
private fun LooksScreen(onApply: (Look) -> Unit) {
    Page {
        BrandHeader("CREATIVE LOOKS")
        Text("A starting point, not a limit.", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("Original built-in looks can be applied in the editor and fine-tuned with sliders.", color = SecondaryText, fontSize = 13.sp)
        Spacer(Modifier.height(20.dp))
        looks.forEach { look ->
            Row(
                Modifier.fillMaxWidth().padding(bottom = 10.dp).clip(RoundedCornerShape(17.dp))
                    .background(SurfaceDark).clickable { onApply(look) }.padding(17.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(Accent.copy(alpha = .18f)), contentAlignment = Alignment.Center) {
                    Text(look.name.take(1), color = Accent, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
                Column(Modifier.weight(1f).padding(start = 13.dp)) {
                    Text(look.name, color = Color.White, fontWeight = FontWeight.Bold)
                    Text("Original Lumen preset", color = SecondaryText, fontSize = 11.sp)
                }
                Text("OPEN EDITOR  ›", color = Accent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ProfileScreen(savedCount: Int) {
    Page {
        BrandHeader("YOUR STUDIO")
        Text("Made for your creative workflow.", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(20.dp))
        InfoRow("App", "Lumen Photo Studio")
        InfoRow("Workspace", "On-device editing")
        InfoRow("Exports this session", savedCount.toString())
        InfoRow("Original photos", "Kept unchanged")
        Spacer(Modifier.height(22.dp))
        Text("AI-powered tools, cloud sync, advanced masking, RAW workflow and additional audited controls are planned modules. They are not represented as active until implemented and tested.", color = SecondaryText, fontSize = 12.sp)
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = SecondaryText, fontSize = 13.sp)
        Text(value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
    HorizontalDivider(color = SurfaceRaised)
}

private fun sampleLuminanceHistogram(bitmap: Bitmap): IntArray {
    val sample = Bitmap.createScaledBitmap(bitmap, 128, 128, true)
    val pixels = IntArray(128 * 128)
    sample.getPixels(pixels, 0, 128, 0, 0, 128, 128)
    val bins = IntArray(32)
    pixels.forEach { pixel ->
        val r = (pixel shr 16) and 255
        val g = (pixel shr 8) and 255
        val b = pixel and 255
        val luma = (0.2126f * r + 0.7152f * g + 0.0722f * b).roundToInt().coerceIn(0, 255)
        bins[(luma * bins.size / 256).coerceIn(0, bins.lastIndex)]++
    }
    if (sample !== bitmap && sample != bitmap) sample.recycle()
    return bins
}

private fun sampleChannelLevels(bitmap: Bitmap): FloatArray {
    val sample = Bitmap.createScaledBitmap(bitmap, 32, 32, true)
    val pixels = IntArray(32 * 32)
    sample.getPixels(pixels, 0, 32, 0, 0, 32, 32)
    var red = 0L
    var green = 0L
    var blue = 0L
    pixels.forEach { pixel ->
        red += (pixel shr 16) and 255
        green += (pixel shr 8) and 255
        blue += pixel and 255
    }
    val count = pixels.size.toFloat() * 255f
    if (sample !== bitmap && sample != bitmap) sample.recycle()
    return floatArrayOf(red / count, green / count, blue / count)
}

private fun autoAdjust(source: Bitmap, current: Adjustments): Adjustments {
    val sample = Bitmap.createScaledBitmap(source, 64, 64, true)
    val pixels = IntArray(64 * 64)
    sample.getPixels(pixels, 0, 64, 0, 0, 64, 64)
    var sum = 0.0
    var sumSquares = 0.0
    pixels.forEach { pixel ->
        val r = ((pixel shr 16) and 255).toDouble()
        val g = ((pixel shr 8) and 255).toDouble()
        val b = (pixel and 255).toDouble()
        val luma = .2126 * r + .7152 * g + .0722 * b
        sum += luma
        sumSquares += luma * luma
    }
    val mean = (sum / pixels.size).coerceAtLeast(1.0)
    val variance = (sumSquares / pixels.size - mean * mean).coerceAtLeast(0.0)
    val deviation = kotlin.math.sqrt(variance)
    val exposureCorrection = (kotlin.math.ln(118.0 / mean) / kotlin.math.ln(2.0)).toFloat().coerceIn(-1f, 1f)
    val contrastCorrection = (1.12f - deviation.toFloat() / 255f * .35f).coerceIn(.85f, 1.25f)
    return current.copy(
        exposure = exposureCorrection,
        contrast = contrastCorrection,
        highlights = if (mean > 145.0) -.12f else 0f,
        shadows = if (mean < 105.0) .18f else 0f
    )
}

private fun renderPhoto(source: Bitmap, a: Adjustments, maxDimension: Int): Bitmap {
    val scale = if (maxDimension == Int.MAX_VALUE) 1f else minOf(1f, maxDimension.toFloat() / maxOf(source.width, source.height))
    val scaledW = (source.width * scale).roundToInt().coerceAtLeast(1)
    val scaledH = (source.height * scale).roundToInt().coerceAtLeast(1)
    val scaled = if (scaledW != source.width || scaledH != source.height) Bitmap.createScaledBitmap(source, scaledW, scaledH, true) else source
    val matrix = android.graphics.Matrix().apply {
        postRotate(a.rotation.toFloat() + a.rotationFine)
        postScale(if (a.flipHorizontal) -1f else 1f, if (a.flipVertical) -1f else 1f)
    }
    val input = if (a.rotation % 360 != 0 || a.rotationFine != 0f || a.flipHorizontal || a.flipVertical)
        Bitmap.createBitmap(scaled, 0, 0, scaled.width, scaled.height, matrix, true) else scaled
    val cropped = if (a.cropRatio > 0f) {
        val currentRatio = input.width.toFloat() / input.height.toFloat()
        if (currentRatio > a.cropRatio) {
            val cropW = (input.height * a.cropRatio).roundToInt().coerceIn(1, input.width)
            val maxLeft = input.width - cropW
            val left = ((maxLeft / 2f) + a.cropOffsetX * maxLeft / 2f).roundToInt().coerceIn(0, maxLeft)
            Bitmap.createBitmap(input, left, 0, cropW, input.height)
        } else {
            val cropH = (input.width / a.cropRatio).roundToInt().coerceIn(1, input.height)
            val maxTop = input.height - cropH
            val top = ((maxTop / 2f) + a.cropOffsetY * maxTop / 2f).roundToInt().coerceIn(0, maxTop)
            Bitmap.createBitmap(input, 0, top, input.width, cropH)
        }
    } else input
    val w = cropped.width
    val h = cropped.height
    val output = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val row = IntArray(w)
    val rowAbove = IntArray(w)
    val rowBelow = IntArray(w)
    val exposure = Math.pow(2.0, a.exposure.toDouble()).toFloat()
    val shadowTint = android.graphics.Color.HSVToColor(floatArrayOf(a.shadowTintHue, 1f, 1f))
    val highlightTint = android.graphics.Color.HSVToColor(floatArrayOf(a.highlightTintHue, 1f, 1f))
    val midtoneTint = android.graphics.Color.HSVToColor(floatArrayOf(a.midtoneTintHue, 1f, 1f))
    val shadowTintR = ((shadowTint shr 16) and 255).toFloat()
    val shadowTintG = ((shadowTint shr 8) and 255).toFloat()
    val shadowTintB = (shadowTint and 255).toFloat()
    val highlightTintR = ((highlightTint shr 16) and 255).toFloat()
    val highlightTintG = ((highlightTint shr 8) and 255).toFloat()
    val highlightTintB = (highlightTint and 255).toFloat()
    val midtoneTintR = ((midtoneTint shr 16) and 255).toFloat()
    val midtoneTintG = ((midtoneTint shr 8) and 255).toFloat()
    val midtoneTintB = (midtoneTint and 255).toFloat()
    for (y in 0 until h) {
        cropped.getPixels(row, 0, w, 0, y, w, 1)
        cropped.getPixels(rowAbove, 0, w, 0, (y - 1).coerceAtLeast(0), w, 1)
        cropped.getPixels(rowBelow, 0, w, 0, (y + 1).coerceAtMost(h - 1), w, 1)
        for (x in 0 until w) {
            val p = row[x]
            var r = ((p shr 16) and 255).toFloat()
            var g = ((p shr 8) and 255).toFloat()
            var b = (p and 255).toFloat()
            if (a.sharpening > 0f || a.noiseReduction > 0f) {
                val left = row[(x - 1).coerceAtLeast(0)]
                val right = row[(x + 1).coerceAtMost(w - 1)]
                val up = rowAbove[x]
                val down = rowBelow[x]
                val neighborR = (((left shr 16) and 255) + ((right shr 16) and 255) + ((up shr 16) and 255) + ((down shr 16) and 255)) * .25f
                val neighborG = (((left shr 8) and 255) + ((right shr 8) and 255) + ((up shr 8) and 255) + ((down shr 8) and 255)) * .25f
                val neighborB = ((left and 255) + (right and 255) + (up and 255) + (down and 255)) * .25f
                val sharpen = a.sharpening * .9f
                val denoise = a.noiseReduction * .45f
                r += (r - neighborR) * sharpen
                g += (g - neighborG) * sharpen
                b += (b - neighborB) * sharpen
                r += (neighborR - r) * denoise
                g += (neighborG - g) * denoise
                b += (neighborB - b) * denoise
            }
            val luma = .2126f * r + .7152f * g + .0722f * b
            val shadowMask = 1f - (luma / 255f).coerceIn(0f, 1f)
            val highlightMask = (luma / 255f).coerceIn(0f, 1f)
            val midtoneMask = (1f - abs(luma / 255f - .5f) * 2f).coerceIn(0f, 1f)
            val tonal = (a.shadows * shadowMask * 42f) + (a.highlights * highlightMask * 42f) +
                (a.whites * highlightMask * 24f) + (a.blacks * shadowMask * 24f) +
                (a.toneShadows * shadowMask * 32f) + (a.toneMidtones * midtoneMask * 32f) +
                (a.toneHighlights * highlightMask * 32f)
            r = (r * exposure + tonal - 128f) * a.contrast + 128f
            g = (g * exposure + tonal - 128f) * a.contrast + 128f
            b = (b * exposure + tonal - 128f) * a.contrast + 128f
            val warmth = a.temperature * 28f
            r += warmth + a.tint * 8f
            b -= warmth + a.tint * 8f
            g += a.tint * 4f
            val average = (r + g + b) / 3f
            val saturation = (a.saturation + a.vibrance * (1f - abs(luma - 128f) / 128f)).coerceIn(0f, 2.5f)
            r = average + (r - average) * saturation
            g = average + (g - average) * saturation
            b = average + (b - average) * saturation
            val hsv = FloatArray(3)
            android.graphics.Color.RGBToHSV(
                r.roundToInt().coerceIn(0, 255),
                g.roundToInt().coerceIn(0, 255),
                b.roundToInt().coerceIn(0, 255),
                hsv
            )
            if (hsv[1] > .015f) {
                val centers = floatArrayOf(0f, 30f, 60f, 120f, 180f, 220f, 275f, 320f)
                var hueShift = 0f
                var satShift = 0f
                var lumShift = 0f
                var totalWeight = 0f
                centers.forEachIndexed { index, center ->
                    var distance = abs(hsv[0] - center)
                    distance = minOf(distance, 360f - distance)
                    val weight = (1f - distance / 42f).coerceIn(0f, 1f)
                    if (weight > 0f) {
                        val mix = a.colorMixer.getOrNull(index) ?: ColorMix()
                        hueShift += mix.hue * 0.36f * weight
                        satShift += mix.saturation * 0.01f * weight
                        lumShift += mix.luminance * 0.0025f * weight
                        totalWeight += weight
                    }
                }
                if (totalWeight > 0f) {
                    hsv[0] = (hsv[0] + hueShift).let { (it % 360f + 360f) % 360f }
                    hsv[1] = (hsv[1] + satShift).coerceIn(0f, 1f)
                    hsv[2] = (hsv[2] + lumShift).coerceIn(0f, 1f)
                    val mixed = android.graphics.Color.HSVToColor(hsv)
                    r = ((mixed shr 16) and 255).toFloat()
                    g = ((mixed shr 8) and 255).toFloat()
                    b = (mixed and 255).toFloat()
                }
            }
            val shadowGradeWeight = (1f - luma / 255f - a.colorGradeBalance * .35f).coerceIn(0f, 1f) * a.shadowTintAmount
            val highlightGradeWeight = (luma / 255f + a.colorGradeBalance * .35f).coerceIn(0f, 1f) * a.highlightTintAmount
            val shadowBlend = shadowGradeWeight * .22f
            val highlightBlend = highlightGradeWeight * .22f
            val midtoneWeight = (1f - abs(luma - 128f) / 128f).coerceIn(0f, 1f) * a.midtoneTintAmount
            val midtoneBlend = midtoneWeight * .18f
            val totalGradeBlend = shadowBlend + highlightBlend + midtoneBlend
            r = r * (1f - totalGradeBlend) + shadowTintR * shadowBlend + highlightTintR * highlightBlend + midtoneTintR * midtoneBlend
            g = g * (1f - totalGradeBlend) + shadowTintG * shadowBlend + highlightTintG * highlightBlend + midtoneTintG * midtoneBlend
            b = b * (1f - totalGradeBlend) + shadowTintB * shadowBlend + highlightTintB * highlightBlend + midtoneTintB * midtoneBlend
            val nx = (x.toFloat() / w - .5f) * 2f
            val ny = (y.toFloat() / h - .5f) * 2f
            val radius = kotlin.math.sqrt(nx * nx + ny * ny)
            val vignette = 1f - a.vignette * (radius * .55f).coerceIn(0f, .65f)
            r *= vignette; g *= vignette; b *= vignette
            if (a.grain > 0f) {
                val pixelIndex = y.toLong() * w + x
                val noise = (((pixelIndex * 1103515245L + 12345L) shr 16).toInt() and 255) / 127.5f - 1f
                val amount = noise * a.grain * 14f
                r += amount; g += amount; b += amount
            }
            if (a.clarity > 0f || a.dehaze > 0f) {
                val detail = (a.clarity * .08f + a.dehaze * .05f) * (luma - 128f)
                r += detail; g += detail; b += detail
            }
            row[x] = android.graphics.Color.argb(
                android.graphics.Color.alpha(p),
                r.roundToInt().coerceIn(0, 255),
                g.roundToInt().coerceIn(0, 255),
                b.roundToInt().coerceIn(0, 255)
            )
        }
        output.setPixels(row, 0, w, 0, y, w, 1)
    }
    return output
}
private fun savePhoto(context: android.content.Context, bitmap: Bitmap, format: String, quality: Int): Boolean {
    return try {
        val isPng = format == "PNG"
        val extension = if (isPng) "png" else "jpg"
        val mimeType = if (isPng) "image/png" else "image/jpeg"
        val compressFormat = if (isPng) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "Lumen_${System.currentTimeMillis()}.$extension")
            put(MediaStore.Images.Media.MIME_TYPE, mimeType)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Lumen Studio")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return false
        val success = resolver.openOutputStream(uri)?.use { bitmap.compress(compressFormat, quality.coerceIn(70, 100), it) } ?: false
        if (!success) {
            resolver.delete(uri, null, null)
            return false
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val ready = ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }
            resolver.update(uri, ready, null, null)
        }
        true
    } catch (_: Exception) { false }
}
