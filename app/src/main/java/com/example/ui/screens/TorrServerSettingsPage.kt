package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.model.PlayerSettings
import com.example.torrent.TorrServerClient
import com.example.torrent.TorrentIntentHelper
import com.example.torrent.TorrentSearchService
import com.example.ui.i18n.LocalIsEnglish
import com.example.ui.i18n.tr
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TorrServerSettingsPage(
    settings: PlayerSettings,
    actions: SettingsActions
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isEnglish = LocalIsEnglish.current

    var urlInput by rememberSaveable(settings.torrServerUrl) {
        mutableStateOf(settings.torrServerUrl)
    }
    var usernameInput by rememberSaveable(settings.torrServerUsername) {
        mutableStateOf(settings.torrServerUsername)
    }
    var passwordInput by rememberSaveable(settings.torrServerPassword) {
        mutableStateOf(settings.torrServerPassword)
    }

    var isTestingServer by remember { mutableStateOf(false) }
    var serverStatusText by remember { mutableStateOf<String?>(null) }
    var serverOnline by remember { mutableStateOf<Boolean?>(null) }

    var indexerUrlInput by rememberSaveable(settings.torrentIndexerUrl) {
        mutableStateOf(settings.torrentIndexerUrl)
    }
    var indexerKeyInput by rememberSaveable(settings.torrentIndexerApiKey) {
        mutableStateOf(settings.torrentIndexerApiKey)
    }
    var isTestingSearch by remember { mutableStateOf(false) }
    var searchTestResultText by remember { mutableStateOf<String?>(null) }
    var searchTestOk by remember { mutableStateOf<Boolean?>(null) }

    val isTorrServerInstalled = remember {
        TorrentIntentHelper.isTorrServerAppInstalled(context)
    }

    // 1. TorrServer Connection Card
    SettingsSectionCard(
        title = tr("Подключение к TorrServer (MatriX)", "TorrServer (MatriX) Connection"),
        icon = Icons.Default.Cloud
    ) {
        // Quick address presets
        Text(
            text = tr("Быстрый выбор адреса сервера", "Quick Server Address Presets"),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = urlInput.contains("127.0.0.1") || urlInput.contains("localhost"),
                onClick = {
                    urlInput = "http://127.0.0.1:8090"
                    actions.onUpdateTorrServerUrl(urlInput)
                },
                label = { Text(tr("На этом устройстве (127.0.0.1:8090)", "On This Device (127.0.0.1:8090)")) },
                modifier = Modifier.testTag("torrserver_preset_local")
            )
            FilterChip(
                selected = urlInput.startsWith("http://192.168."),
                onClick = {
                    if (!urlInput.startsWith("http://192.168.")) {
                        urlInput = "http://192.168.1.100:8090"
                        actions.onUpdateTorrServerUrl(urlInput)
                    }
                },
                label = { Text(tr("Домашний ПК / ТВ-бокс (192.168.x.x)", "Home PC / TV Box (192.168.x.x)")) },
                modifier = Modifier.testTag("torrserver_preset_lan")
            )
        }

        OutlinedTextField(
            value = urlInput,
            onValueChange = { urlInput = it },
            label = { Text(tr("URL сервера TorrServer", "TorrServer URL")) },
            placeholder = { Text("http://127.0.0.1:8090") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("settings_torrserver_url_input")
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = usernameInput,
                onValueChange = { usernameInput = it },
                label = { Text(tr("Логин (если включена защита)", "Username (optional)")) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("settings_torrserver_user_input")
            )
            OutlinedTextField(
                value = passwordInput,
                onValueChange = { passwordInput = it },
                label = { Text(tr("Пароль", "Password")) },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("settings_torrserver_pass_input")
            )
        }

        if (serverStatusText != null) {
            Surface(
                color = if (serverOnline == true) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                } else {
                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                },
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(
                    1.dp,
                    if (serverOnline == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (serverOnline == true) {
                            Icons.Default.CheckCircle
                        } else {
                            Icons.Default.ErrorOutline
                        },
                        contentDescription = null,
                        tint = if (serverOnline == true) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = serverStatusText.orEmpty(),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = {
                    val normalized = TorrServerClient.normalizeBaseUrl(urlInput)
                    urlInput = normalized
                    actions.onUpdateTorrServerUrl(normalized)
                    actions.onUpdateTorrServerAuth(usernameInput.trim(), passwordInput)
                    scope.launch {
                        isTestingServer = true
                        val res = TorrServerClient.ping(
                            rawBaseUrl = normalized,
                            username = usernameInput.trim(),
                            password = passwordInput
                        )
                        isTestingServer = false
                        res.fold(
                            onSuccess = { version ->
                                serverOnline = true
                                serverStatusText = if (isEnglish) {
                                    "Connected! TorrServer $version is online ($normalized)"
                                } else {
                                    "Подключено! TorrServer $version на связи ($normalized)"
                                }
                            },
                            onFailure = { err ->
                                serverOnline = false
                                serverStatusText = if (isEnglish) {
                                    "Saved, but server did not respond: ${err.message ?: "check that TorrServer is running"}"
                                } else {
                                    "Сохранено, но сервер не ответил: ${err.message ?: "проверьте запуск TorrServer"}"
                                }
                            }
                        )
                    }
                },
                enabled = !isTestingServer,
                modifier = Modifier
                    .weight(1f)
                    .testTag("settings_torrserver_save_and_test_button")
            ) {
                if (isTestingServer) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(tr("Сохранить и проверить", "Save & Test"))
            }

            if (isTorrServerInstalled) {
                FilledTonalButton(
                    onClick = { TorrentIntentHelper.launchTorrServerApp(context) },
                    modifier = Modifier.testTag("settings_launch_torrserver_app_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(tr("Открыть TorrServer", "Open TorrServer"))
                }
            } else {
                OutlinedButton(
                    onClick = {
                        TorrentIntentHelper.openUrlInBrowser(
                            context,
                            "https://github.com/YouROK/TorrServer/releases"
                        )
                    },
                    modifier = Modifier.testTag("settings_download_torrserver_button")
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(tr("Скачать APK", "Download APK"))
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        SettingToggleRow(
            title = tr(
                "Мгновенный запуск одиночных фильмов",
                "Instant Playback for Single-File Movies"
            ),
            checked = settings.torrServerAutoPlaySingle,
            onCheckedChange = actions.onUpdateTorrServerAutoPlaySingle,
            testTag = "torrserver_autoplay_single_switch"
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        SettingToggleRow(
            title = tr("Сохранять открытые раздачи в списке TorrServer", "Save Opened Torrents to TorrServer List"),
            checked = settings.torrServerSaveToDb,
            onCheckedChange = actions.onUpdateTorrServerSaveToDb,
            testTag = "torrserver_save_db_switch"
        )
    }

    // 2. Tracker Search & RuTracker / Rutor / Torznab Configuration
    SettingsSectionCard(
        title = tr("Поиск по трекерам (RuTracker, Rutor, Аниме)", "Tracker Search (RuTracker, Rutor, Anime)"),
        icon = Icons.Default.Search
    ) {
        SettingToggleRow(
            title = tr("Прямой поиск по Rutor и публичным агрегаторам", "Direct Search on Rutor & Public Aggregators"),
            checked = settings.enableDirectRutorSearch,
            onCheckedChange = actions.onUpdateEnableDirectRutorSearch,
            testTag = "enable_direct_rutor_switch"
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        SettingToggleRow(
            title = tr("Прямой поиск по аниме-трекерам (AniLibria / Nyaa)", "Direct Anime Tracker Search (AniLibria / Nyaa)"),
            checked = settings.enableAnimeTrackerSearch,
            onCheckedChange = actions.onUpdateEnableAnimeTrackerSearch,
            testTag = "enable_anime_tracker_switch"
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        SettingToggleRow(
            title = tr("Обход блокировок зеркал через DNS-over-HTTPS (DoH)", "Bypass Mirror Blocks via DNS-over-HTTPS (DoH)"),
            checked = settings.enableTrackerDoh,
            onCheckedChange = actions.onUpdateEnableTrackerDoh,
            testTag = "enable_tracker_doh_switch"
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        Text(
            text = tr("Индексатор Jackett / Prowlarr / Torznab (RuTracker, Kinozal)", "Jackett / Prowlarr / Torznab Indexer (RuTracker, Kinozal)"),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
        )

        OutlinedTextField(
            value = indexerUrlInput,
            onValueChange = { indexerUrlInput = it },
            label = { Text("URL Jackett / Prowlarr / Torznab") },
            placeholder = { Text("http://192.168.1.100:9117") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("settings_indexer_url_input")
        )

        OutlinedTextField(
            value = indexerKeyInput,
            onValueChange = { indexerKeyInput = it },
            label = { Text(tr("API Key (если требуется)", "API Key (if required)")) },
            placeholder = { Text(tr("Ключ API из Jackett / Prowlarr", "API key from Jackett / Prowlarr")) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("settings_indexer_key_input")
        )

        if (searchTestResultText != null) {
            Surface(
                color = if (searchTestOk == true) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                } else {
                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = searchTestResultText.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = {
                    actions.onUpdateTorrentIndexerConfig(
                        indexerUrlInput.trim(),
                        indexerKeyInput.trim()
                    )
                    searchTestOk = true
                    searchTestResultText = if (isEnglish) {
                        "Indexer settings saved"
                    } else {
                        "Настройки индексатора сохранены"
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("settings_save_indexer_button")
            ) {
                Text(tr("Сохранить индексатор", "Save Indexer"))
            }

            FilledTonalButton(
                onClick = {
                    actions.onUpdateTorrentIndexerConfig(
                        indexerUrlInput.trim(),
                        indexerKeyInput.trim()
                    )
                    scope.launch {
                        isTestingSearch = true
                        val respResult = TorrentSearchService.searchAll(
                            rawQuery = "1080p",
                            indexerUrl = indexerUrlInput.trim(),
                            indexerApiKey = indexerKeyInput.trim(),
                            enableDirectRutor = settings.enableDirectRutorSearch,
                            enableAnimeNyaa = settings.enableAnimeTrackerSearch,
                            enableDoh = settings.enableTrackerDoh
                        )
                        isTestingSearch = false
                        respResult.fold(
                            onSuccess = { resp ->
                                if (resp.results.isNotEmpty()) {
                                    searchTestOk = true
                                    val srcSummary = resp.activeSources.joinToString(", ")
                                    searchTestResultText = if (isEnglish) {
                                        "Search works! Found ${resp.results.size} torrents ($srcSummary)"
                                    } else {
                                        "Поиск работает! Найдено ${resp.results.size} раздач ($srcSummary)"
                                    }
                                } else {
                                    searchTestOk = false
                                    searchTestResultText = resp.warningMessage
                                        ?: if (isEnglish) "Sources returned 0 results" else "Источники вернули 0 результатов"
                                }
                            },
                            onFailure = { err ->
                                searchTestOk = false
                                searchTestResultText = err.message
                                    ?: if (isEnglish) "Network error" else "Сетевая ошибка"
                            }
                        )
                    }
                },
                enabled = !isTestingSearch,
                modifier = Modifier
                    .weight(1f)
                    .testTag("settings_test_search_button")
            ) {
                if (isTestingSearch) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Dns, contentDescription = null, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(tr("Тест поиска", "Test Search"))
            }
        }
    }
}
