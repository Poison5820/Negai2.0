package com.example.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppVersionInfo
import com.example.ui.i18n.LocalIsEnglish
import com.example.ui.i18n.tr
import com.example.ui.theme.NegaiBrandFontFamily
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

@Immutable
data class PlanetaryFeatureOrbit(
    val index: Int,
    val badge: String,
    val titleRu: String,
    val titleEn: String,
    val subtitleRu: String,
    val subtitleEn: String,
    val specsRu: String,
    val specsEn: String,
    val color: Color,
    val orbitRadiusFraction: Float,
    val speedMultiplier: Float,
    val initialPhaseRad: Float,
    val planetRadiusDp: Float,
    val hasMoon: Boolean,
    val icon: ImageVector
)

private val PLANETARY_FEATURES = listOf(
    PlanetaryFeatureOrbit(
        index = 0,
        badge = "GLSL • Anime4K",
        titleRu = "Апскейлинг Anime4K в реальном времени",
        titleEn = "Real-Time Anime4K GLSL Upscaling",
        subtitleRu = "Многопроходная реконструкция контуров Собеля, CNN / CNNX и живое сравнение До/После",
        subtitleEn = "Multi-pass Sobel contour reconstruction, CNN / CNNX shaders & live split-screen compare",
        specsRu = "OpenGL ES 2.0 • 4 пресета • 0 задержки кадра",
        specsEn = "OpenGL ES 2.0 • 4 Presets • Zero frame latency",
        color = Color(0xFF00E5FF),
        orbitRadiusFraction = 0.28f,
        speedMultiplier = 1.6f,
        initialPhaseRad = 0.4f,
        planetRadiusDp = 5.5f,
        hasMoon = false,
        icon = Icons.Default.AutoAwesome
    ),
    PlanetaryFeatureOrbit(
        index = 1,
        badge = "MEMC • LTPO",
        titleRu = "Интерполяция кадров MEMC и адаптивная герцовка",
        titleEn = "MEMC Frame Interpolation & LTPO Cadence",
        subtitleRu = "Фазово-согласованный двунаправленный варпинг до 165 Гц с защитой от артефактов на сменах сцен",
        subtitleEn = "Phase-consistent bidirectional motion warp up to 165Hz with scene-cut artifact protection",
        specsRu = "24/30 → 60/90/120/165 Гц • Highp векторы движения",
        specsEn = "24/30 → 60/90/120/165Hz • Highp motion vectors",
        color = Color(0xFFFF4D8D),
        orbitRadiusFraction = 0.48f,
        speedMultiplier = 1.1f,
        initialPhaseRad = 2.1f,
        planetRadiusDp = 6.5f,
        hasMoon = true,
        icon = Icons.Default.Speed
    ),
    PlanetaryFeatureOrbit(
        index = 2,
        badge = "DSP • ±5s Sync",
        titleRu = "Аудиосинхронизация ±5 с и авто-подхват озвучек",
        titleEn = "Audio Sync ±5s, DSP Boost & Auto-Matching",
        subtitleRu = "Регулировка задержки звука с шагом ±50/250 мс, ресинхронизация потока, усиление до 200% и внешние .mka/.flac",
        subtitleEn = "Fine ±50/250ms audio delay adjustment, instant stream resync, 200% DSP boost & external .mka/.flac",
        specsRu = "PCM AudioProcessor • +12 дБ • Авто-привязка папок",
        specsEn = "PCM AudioProcessor • +12 dB • Smart folder matching",
        color = Color(0xFF69F0AE),
        orbitRadiusFraction = 0.68f,
        speedMultiplier = -0.8f,
        initialPhaseRad = 3.9f,
        planetRadiusDp = 6.0f,
        hasMoon = false,
        icon = Icons.Default.GraphicEq
    ),
    PlanetaryFeatureOrbit(
        index = 3,
        badge = "TorrServer • MKV",
        titleRu = "TorrServer P2P, встроенные главы и субтитры ASS/SRT",
        titleEn = "TorrServer P2P, Embedded Chapters & ASS/SRT",
        subtitleRu = "Прямой стриминг торрентов в 4K, парсер глав Matroska EBML / MP4 Nero и раздельные окна серий и глав",
        subtitleEn = "Direct 4K torrent streaming, Matroska EBML / MP4 Nero chapter parser & dedicated Episodes/Chapters sheets",
        specsRu = "HTTP Range • DoH • Раздельные окна Серии / Главы",
        specsEn = "HTTP Range • DoH • Dedicated Episodes / Chapters UI",
        color = Color(0xFFFFAB40),
        orbitRadiusFraction = 0.88f,
        speedMultiplier = 0.55f,
        initialPhaseRad = 5.2f,
        planetRadiusDp = 7.0f,
        hasMoon = true,
        icon = Icons.Default.CloudDownload
    )
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun VersionPlanetaryModalSheet(
    onDismiss: () -> Unit
) {
    val isEnglish = LocalIsEnglish.current
    var selectedOrbitIndex by rememberSaveable { mutableIntStateOf(0) }
    val selectedFeature = PLANETARY_FEATURES.getOrElse(selectedOrbitIndex) { PLANETARY_FEATURES.first() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.testTag("version_planetary_modal_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "negai",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontFamily = NegaiBrandFontFamily,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f))
                    ) {
                        Text(
                            text = AppVersionInfo.BADGE_TEXT,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Text(
                    text = tr(
                        "Build #${AppVersionInfo.VERSION_CODE} • Release",
                        "Build #${AppVersionInfo.VERSION_CODE} • Release"
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Minimalist Planetary System Orbital Animation Card
            Surface(
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.dp, selectedFeature.color.copy(alpha = 0.45f)),
                color = Color(0xFF060911),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(228.dp)
                    .testTag("planetary_system_card")
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    PlanetarySystemMinimalCanvas(
                        orbits = PLANETARY_FEATURES,
                        selectedOrbitIndex = selectedOrbitIndex,
                        onSelectOrbit = { selectedOrbitIndex = it },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Top-left minimal orbital legend badge
                    Surface(
                        color = Color(0xAA0D1322),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.14f)),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(10.dp)
                    ) {
                        Text(
                            text = tr("ОРБИТАЛЬНАЯ АРХИТЕКТУРА ЯДРА", "CORE ORBITAL ARCHITECTURE"),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                letterSpacing = 0.9.sp
                            ),
                            color = Color.White.copy(alpha = 0.72f),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    // Bottom-right active planet indicator pill
                    Surface(
                        color = Color(0xCC0D1322),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, selectedFeature.color.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(selectedFeature.color)
                            )
                            Text(
                                text = selectedFeature.badge,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // Orbit selector pills
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                PLANETARY_FEATURES.forEach { orbit ->
                    val isSelected = orbit.index == selectedOrbitIndex
                    Surface(
                        color = if (isSelected) {
                            orbit.color.copy(alpha = 0.18f)
                        } else {
                            MaterialTheme.colorScheme.surfaceContainer
                        },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) orbit.color else MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { selectedOrbitIndex = orbit.index }
                            .testTag("planetary_orbit_chip_${orbit.index}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(orbit.color)
                            )
                            Text(
                                text = orbit.badge,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) orbit.color else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Highlighted Selected Orbit Detail Card
            Card(
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, selectedFeature.color.copy(alpha = 0.55f)),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = selectedFeature.icon,
                            contentDescription = null,
                            tint = selectedFeature.color,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = if (isEnglish) selectedFeature.titleEn else selectedFeature.titleRu,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = if (isEnglish) selectedFeature.subtitleEn else selectedFeature.subtitleRu,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (isEnglish) selectedFeature.specsEn else selectedFeature.specsRu,
                        style = MaterialTheme.typography.labelSmall,
                        color = selectedFeature.color,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Full Capabilities List
            Text(
                text = tr("Основные возможности плеера", "Core Player Capabilities"),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            PLANETARY_FEATURES.forEach { feature ->
                val isSelected = feature.index == selectedOrbitIndex
                Surface(
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.surfaceContainerHighest
                    } else {
                        MaterialTheme.colorScheme.surfaceContainer
                    },
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) feature.color.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { selectedOrbitIndex = feature.index }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(feature.color.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = feature.icon,
                                contentDescription = null,
                                tint = feature.color,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isEnglish) feature.titleEn else feature.titleRu,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (isEnglish) feature.specsEn else feature.specsRu,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Release v2.5 Changelog Card
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = tr("Что нового в релизе v2.5", "What's New in Release v2.5"),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    val notes = if (isEnglish) {
                        listOf(
                            "• Clean icon-only bottom player buttons; episode counter badge (e.g. 1/12) on the Episodes button",
                            "• Unified Audio Boost control inside the Audio menu",
                            "• Chapters button moved above the timeline on the left with live chapter title (hidden when no chapters exist)",
                            "• Custom Skip Intro interval + direct long-press configuration on the Skip Intro button",
                            "• Automatic screen DPI scaling + smooth 75%–140% player UI & text scale slider",
                            "• Configurable HUD auto-hide timeout (2–15s) and timer reset while scrolling bottom buttons",
                            "• Reliable playback position persistence when closing PiP or switching episodes",
                            "• Detailed torrent release sheet with full untruncated title, description, and file list"
                        )
                    } else {
                        listOf(
                            "• Чистые иконки без подписей на нижней панели плеера; возле кнопки серий — только счётчик (например, 1/12)",
                            "• Усиление звука (DSP) объединено внутри меню Аудио",
                            "• Кнопка глав вынесена слева над полосой прокрутки с названием текущей главы (скрыта, если глав нет)",
                            "• Произвольный интервал пропуска опенинга и быстрая настройка долгим нажатием на кнопку пропуска",
                            "• Автоучёт DPI экрана и плавный ползунок масштаба элементов и текста плеера (75%–140%)",
                            "• Настройка времени автоскрытия элементов (2–15 сек) и сброс таймера при прокрутке нижних кнопок",
                            "• Надёжное сохранение позиции в режиме PiP при закрытии окна или переходе к другой серии",
                            "• Подробная карточка торрент-раздачи с полным названием, описанием и списком файлов"
                        )
                    }
                    notes.forEach { line ->
                        Text(
                            text = line,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlanetarySystemMinimalCanvas(
    orbits: List<PlanetaryFeatureOrbit>,
    selectedOrbitIndex: Int,
    onSelectOrbit: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "planetary_orbit_transition")
    val masterPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 24000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "master_orbital_phase"
    )
    val corePulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "core_star_pulse"
    )

    // Precomputed deterministic starlight coordinates in normalized [0..1] space
    val stars = remember {
        listOf(
            Triple(0.11f, 0.18f, 1.3f),
            Triple(0.24f, 0.78f, 1.0f),
            Triple(0.38f, 0.14f, 1.5f),
            Triple(0.67f, 0.21f, 1.2f),
            Triple(0.84f, 0.16f, 1.7f),
            Triple(0.91f, 0.64f, 1.1f),
            Triple(0.76f, 0.84f, 1.4f),
            Triple(0.16f, 0.52f, 1.2f),
            Triple(0.54f, 0.88f, 1.0f),
            Triple(0.88f, 0.36f, 1.3f)
        )
    }

    Canvas(
        modifier = modifier.pointerInput(orbits) {
            detectTapGestures { tapOffset ->
                val cx = size.width / 2f
                val cy = size.height / 2f
                val maxR = min(size.width, size.height) * 0.44f
                val tapDist = hypot(tapOffset.x - cx, tapOffset.y - cy)
                val closest = orbits.minByOrNull { orbit ->
                    kotlin.math.abs(tapDist - orbit.orbitRadiusFraction * maxR)
                }
                if (closest != null) {
                    onSelectOrbit(closest.index)
                }
            }
        }
    ) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val center = Offset(cx, cy)
        val maxOrbitRadius = min(size.width, size.height) * 0.44f

        // 1. Subtle deep-space radial nebula glow
        val activeColor = orbits.getOrNull(selectedOrbitIndex)?.color ?: Color(0xFF00E5FF)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    activeColor.copy(alpha = 0.16f * corePulse),
                    Color(0xFF0B1426).copy(alpha = 0.35f),
                    Color.Transparent
                ),
                center = center,
                radius = maxOrbitRadius * 1.15f
            ),
            radius = maxOrbitRadius * 1.15f,
            center = center
        )

        // 2. Minimalist background starfield
        stars.forEachIndexed { idx, (nx, ny, radiusDp) ->
            val alpha = if (idx % 2 == 0) 0.25f * corePulse else 0.18f
            drawCircle(
                color = Color.White.copy(alpha = alpha.coerceIn(0.08f, 0.45f)),
                radius = radiusDp.dp.toPx(),
                center = Offset(nx * size.width, ny * size.height)
            )
        }

        // 3. Crosshair minimalist axis ticks
        val axisColor = Color.White.copy(alpha = 0.05f)
        drawLine(
            color = axisColor,
            start = Offset(cx - maxOrbitRadius * 1.05f, cy),
            end = Offset(cx + maxOrbitRadius * 1.05f, cy),
            strokeWidth = 1.dp.toPx()
        )
        drawLine(
            color = axisColor,
            start = Offset(cx, cy - maxOrbitRadius * 1.05f),
            end = Offset(cx, cy + maxOrbitRadius * 1.05f),
            strokeWidth = 1.dp.toPx()
        )

        // 4. Orbital rings, trailing arcs, planets, and moons
        orbits.forEach { orbit ->
            val isSelected = orbit.index == selectedOrbitIndex
            val orbitRadius = maxOrbitRadius * orbit.orbitRadiusFraction
            val ringAlpha = if (isSelected) 0.42f else 0.14f
            val ringStroke = if (isSelected) 1.5.dp.toPx() else 1.dp.toPx()

            // Base minimalist orbit circle
            drawCircle(
                color = if (isSelected) orbit.color.copy(alpha = ringAlpha) else Color.White.copy(alpha = ringAlpha),
                radius = orbitRadius,
                center = center,
                style = Stroke(width = ringStroke)
            )

            // Current planet angle in radians
            val angleRad = orbit.initialPhaseRad + masterPhase * orbit.speedMultiplier
            val px = cx + orbitRadius * cos(angleRad)
            val py = cy + orbitRadius * sin(angleRad)
            val planetCenter = Offset(px, py)

            // Subtle orbital wake arc behind the planet
            val degAngle = Math.toDegrees(angleRad.toDouble()).toFloat()
            val sweepDeg = if (orbit.speedMultiplier >= 0f) -42f else 42f
            drawArc(
                color = orbit.color.copy(alpha = if (isSelected) 0.65f else 0.32f),
                startAngle = degAngle,
                sweepAngle = sweepDeg,
                useCenter = false,
                topLeft = Offset(cx - orbitRadius, cy - orbitRadius),
                size = androidx.compose.ui.geometry.Size(orbitRadius * 2f, orbitRadius * 2f),
                style = Stroke(width = (ringStroke * 1.6f), cap = StrokeCap.Round)
            )

            // Planet outer halo glow
            val pRadiusPx = orbit.planetRadiusDp.dp.toPx() * (if (isSelected) 1.22f else 1.0f)
            drawCircle(
                color = orbit.color.copy(alpha = if (isSelected) 0.34f else 0.16f),
                radius = pRadiusPx * 2.3f,
                center = planetCenter
            )

            // Planet body
            drawCircle(
                color = orbit.color,
                radius = pRadiusPx,
                center = planetCenter
            )
            // Specular highlight core on planet
            drawCircle(
                color = Color.White.copy(alpha = 0.75f),
                radius = pRadiusPx * 0.35f,
                center = Offset(px - pRadiusPx * 0.25f, py - pRadiusPx * 0.25f)
            )

            // Optional minimalist satellite moon
            if (orbit.hasMoon) {
                val moonOrbitR = pRadiusPx * 2.5f
                val moonAngle = angleRad * 3.2f
                val mx = px + moonOrbitR * cos(moonAngle)
                val my = py + moonOrbitR * sin(moonAngle)
                drawCircle(
                    color = Color.White.copy(alpha = 0.18f),
                    radius = moonOrbitR,
                    center = planetCenter,
                    style = Stroke(width = 0.7.dp.toPx())
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.85f),
                    radius = 1.8.dp.toPx(),
                    center = Offset(mx, my)
                )
            }
        }

        // 5. Central Star ("negai" core)
        val starRadius = 11.dp.toPx()
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White,
                    activeColor.copy(alpha = 0.75f),
                    Color.Transparent
                ),
                center = center,
                radius = starRadius * 2.8f * corePulse
            ),
            radius = starRadius * 2.8f * corePulse,
            center = center
        )
        drawCircle(
            color = Color(0xFF0A101D),
            radius = starRadius,
            center = center
        )
        drawCircle(
            color = activeColor,
            radius = starRadius,
            center = center,
            style = Stroke(width = 1.8.dp.toPx())
        )
        drawCircle(
            color = Color.White,
            radius = 3.5.dp.toPx(),
            center = center
        )
    }
}
