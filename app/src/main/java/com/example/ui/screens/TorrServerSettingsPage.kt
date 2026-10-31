package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.model.PlayerSettings
import com.example.torrent.TorrServerClient
import com.example.torrent.TorrentIntentHelper
import com.example.torrent.TorrentSearchService
import com.example.torrent.TrackerSourceDiagnostic
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TorrServerSettingsPage(
    settings: PlayerSettings,
    actions: SettingsActions
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var serverUrlInput by rememberSaveable(settings.torrServerUrl) {
        mutableStateOf(settings.torrServerUrl)
    }
    var usernameInput by rememberSaveable(settings.torrServerUsername) {
        mutableStateOf(settings.torrServerUsername)
    }
    var passwordInput by rememberSaveable(settings.torrServerPassword) {
        mutableStateOf(settings.torrServerPassword)
    }
    var indexerUrlInput by rememberSaveable(settings.torrentIndexerUrl) {
        mutableStateOf(settings.torrentIndexerUrl)
    }
    var indexerApiKeyInput by rememberSaveable(settings.torrentIndexerApiKey) {
        mutableStateOf(settings.torrentIndexerApiKey)
    }

    var isTestingPing by remember { mutableStateOf(false) }
    var pingStatusText by remember { mutableStateOf<String?>(null) }
    var pingSucceeded by remember { mutableStateOf<Boolean?>(null) }

    var isDiagnosingTrackers by remember { mutableStateOf(false) }
    var trackerDiagnostics by remember { mutableStateOf<List<TrackerSourceDiagnostic>>(emptyList()) }

    val isTorrServerInstalled = remember(context) {
        TorrentIntentHelper.isTorrServerAppInstalled(context)
    }

    val runPingTest = {
        scope.launch {
            isTestingPing = true
            val normalized = TorrServerClient.normalizeBaseUrl(serverUrlInput)
            actions.onUpdateTorrServerUrl(normalized)
            actions.onUpdateTorrServerAuth(usernameInput, passwordInput)
            val res = TorrServerClient.ping(normalized, usernameInput, passwordInput)
            isTestingPing = false
            res.fold(
                onSuccess = { ver ->
                    pingSucceeded = true
                    pingStatusText = "Подключено: TorrServer $ver ($normalized)"
                },
                onFailure = { err ->
                    pingSucceeded = false
                    pingStatusText = err.message ?: "Сервер не отвечает по адресу $normalized"
                }
            )
        }
    }

    val runTrackerDiagnostics = {
        scope.launch {
            isDiagnosingTrackers = true
            actions.onUpdateTorrentIndexerConfig(indexerUrlInput, indexerApiKeyInput)
            val report = TorrentSearchService.diagnoseSources(
                indexerUrl = indexerUrlInput,
                indexerApiKey = indexerApiKeyInput,
                enableDirectRutor = settings.enableDirectRutorSearch,
                enableAnimeNyaa = settings.enableAnimeTrackerSearch,
                enableDoh = settings.enableTrackerDoh
            )
            trackerDiagnostics = report
            isDiagnosingTrackers = false
        }
    }

    LaunchedEffect(Unit) {
        val normalized = TorrServerClient.normalizeBaseUrl(settings.torrServerUrl)
        val res = TorrServerClient.ping(
            normalized,
            settings.torrServerUsername,
            settings.torrServerPassword
        )
        res.fold(
            onSuccess = { ver ->
                pingSucceeded = true
                pingStatusText = "Онлайн: TorrServer $ver"
            },
            onFailure = {
                pingSucceeded = false
                pingStatusText = "Сервер сейчас не отвечает ($normalized)"
            }
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Card(
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Подключение к TorrServer (MatriX)",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Для просмотра торрентов на лету укажите локальный адрес TorrServer на этом устройстве или IP-адрес домашнего сервера / ТВ-приставки.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = serverUrlInput.contains("127.0.0.1"),
                        onClick = {
                            serverUrlInput = "http://127.0.0.1:8090"
                            actions.onUpdateTorrServerUrl("http://127.0.0.1:8090")
                        },
                        label = { Text("Локальный (127.0.0.1:8090)") }
                    )
                    FilterChip(
                        selected = serverUrlInput.startsWith("http://192.168."),
                        onClick = {
                            if (!serverUrlInput.startsWith("http://192.168.")) {
                                serverUrlInput = "http://192.168.1.100:8090"
                            }
                        },
                        label = { Text("Домашняя сеть (192.168.x.x)") }
                    )
                }

                OutlinedTextField(
                    value = serverUrlInput,
                    onValueChange = { serverUrlInput = it },
                    label = { Text("Адрес TorrServer") },
                    placeholder = { Text("http://127.0.0.1:8090") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_torrserver_url_input")
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = usernameInput,
                        onValueChange = { usernameInput = it },
                        label = { Text("Логин (опц.)") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it },
                        label = { Text("Пароль (опц.)") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    )
                }

                if (pingStatusText != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Icon(
                            imageVector = if (pingSucceeded == true) {
                                Icons.Default.CheckCircle
                            } else {
                                Icons.Default.ErrorOutline
                            },
                            contentDescription = null,
                            tint = if (pingSucceeded == true) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.error
                            },
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = pingStatusText.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (pingSucceeded == true) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.error
                            }
                        )
                    }
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { runPingTest() },
                        enabled = !isTestingPing,
                        modifier = Modifier.testTag("settings_torrserver_ping_button")
                    ) {
                        if (isTestingPing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Сохранить и проверить")
                    }

                    if (isTorrServerInstalled) {
                        OutlinedButton(
                            onClick = { TorrentIntentHelper.launchTorrServerApp(context) },
                            modifier = Modifier.testTag("settings_launch_torrserver_app_button")
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Открыть TorrServer")
                        }
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 6.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                TorrSettingsToggle(
                    title = "Автозапуск одиночных фильмов",
                    subtitle = "Сразу открывать видеоплеер, если в раздаче только один видеофайл",
                    checked = settings.torrServerAutoPlaySingle,
                    onCheckedChange = actions.onUpdateTorrServerAutoPlaySingle,
                    testTag = "settings_torrserver_autoplay_switch"
                )

                TorrSettingsToggle(
                    title = "Сохранять раздачи в список TorrServer",
                    subtitle = "Торренты останутся доступными в разделе «Торренты сервера»",
                    checked = settings.torrServerSaveToDb,
                    onCheckedChange = actions.onUpdateTorrServerSaveToDb,
                    testTag = "settings_torrserver_save_db_switch"
                )
            }
        }

        Card(
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Устойчивый поиск (RuTracker, Rutor, антибот-обход)",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Поиск защищён от смены адресов и антибот-систем RuTracker / Rutor: используется пул зеркал API с автопереключением, прямые Magnet-ретрекеры (bt.t-ru.org) и защищённый DNS-over-HTTPS. Можно указать несколько своих адресов через запятую.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val mirrors = listOf(
                        "https://jacred.xyz" to "jacred.xyz",
                        "https://jacred.pro" to "jacred.pro",
                        "https://jac-red.ru" to "jac-red.ru",
                        "https://jr.maxvol.pro" to "jr.maxvol.pro",
                        "https://jacred.viewbox.dev" to "viewbox.dev"
                    )
                    mirrors.forEach { (url, label) ->
                        FilterChip(
                            selected = indexerUrlInput.trim().equals(url, ignoreCase = true),
                            onClick = {
                                indexerUrlInput = url
                                actions.onUpdateTorrentIndexerConfig(url, indexerApiKeyInput)
                            },
                            label = { Text(label) }
                        )
                    }
                }

                OutlinedTextField(
                    value = indexerUrlInput,
                    onValueChange = { indexerUrlInput = it },
                    label = { Text("URL индексатора или список зеркал через запятую") },
                    placeholder = { Text("https://jacred.xyz, https://jacred.pro") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_torrent_indexer_url_input")
                )

                OutlinedTextField(
                    value = indexerApiKeyInput,
                    onValueChange = { indexerApiKeyInput = it },
                    label = { Text("API-ключ Jackett / Prowlarr (если свой сервер)") },
                    placeholder = { Text("Пусто для публичного пула RuTracker / JacRed") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_torrent_indexer_apikey_input")
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = {
                            actions.onUpdateTorrentIndexerConfig(indexerUrlInput, indexerApiKeyInput)
                        },
                        modifier = Modifier.testTag("settings_save_indexer_button")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Сохранить зеркала")
                    }

                    OutlinedButton(
                        onClick = { runTrackerDiagnostics() },
                        enabled = !isDiagnosingTrackers,
                        modifier = Modifier.testTag("settings_check_trackers_button")
                    ) {
                        if (isDiagnosingTrackers) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                Icons.Default.NetworkCheck,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Проверить трекеры")
                    }
                }

                if (trackerDiagnostics.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_tracker_diagnostics_panel")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            trackerDiagnostics.forEach { diag ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = if (diag.isHealthy) {
                                            Icons.Default.CheckCircle
                                        } else {
                                            Icons.Default.ErrorOutline
                                        },
                                        contentDescription = null,
                                        tint = if (diag.isHealthy) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.error
                                        },
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = diag.sourceName,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = diag.detailMessage,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (diag.isHealthy) {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            } else {
                                                MaterialTheme.colorScheme.error
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 6.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                TorrSettingsToggle(
                    title = "Обход блокировок DNS (DNS-over-HTTPS)",
                    subtitle = "Разрешать адреса трекеров и зеркал через защищённый DNS (1.1.1.1 / 8.8.8.8), игнорируя заглушки провайдера",
                    checked = settings.enableTrackerDoh,
                    onCheckedChange = actions.onUpdateEnableTrackerDoh,
                    testTag = "settings_tracker_doh_switch"
                )

                TorrSettingsToggle(
                    title = "Прямой поиск по зеркалам Rutor",
                    subtitle = "Параллельно опрашивать пул зеркал Rutor с защитой от блокировок",
                    checked = settings.enableDirectRutorSearch,
                    onCheckedChange = actions.onUpdateEnableDirectRutorSearch,
                    testTag = "settings_direct_rutor_switch"
                )

                TorrSettingsToggle(
                    title = "Аниме-трекеры (Nyaa + AniLibria API)",
                    subtitle = "Подключать резервный пул аниме-раздач и озвучек напрямую",
                    checked = settings.enableAnimeTrackerSearch,
                    onCheckedChange = actions.onUpdateEnableAnimeTrackerSearch,
                    testTag = "settings_anime_nyaa_switch"
                )
            }
        }
    }
}

@Composable
private fun TorrSettingsToggle(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag)
        )
    }
}
