package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.SettingsCategory
import com.example.torrent.AntiBotBlockReason
import com.example.torrent.BencodeTorrentParser
import com.example.torrent.ExpectedResponseKind
import com.example.torrent.MirrorFetchOutcome
import com.example.torrent.MirrorHealthRegistry
import com.example.torrent.ResilientTrackerNetwork
import com.example.torrent.TorrServerClient
import com.example.torrent.TorrServerFileEntry
import com.example.torrent.TorrServerTorrent
import com.example.torrent.TorrentQualityFilter
import com.example.torrent.TorrentSearchResult
import com.example.torrent.TorrentSearchService
import com.example.torrent.TorrentSortMode
import com.example.torrent.TorrentTrackerGroup
import kotlinx.coroutines.test.runTest
import okhttp3.Headers
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.net.InetAddress
import java.security.MessageDigest

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TorrentAndTorrServerTest {

    @Test
    fun bencodeParser_extractsExactInfoHashNameTrackersAndEnrichesBareMagnets() {
        val fileName = "Movie_1080p.mkv"
        val trackerUrl = "http://bt.t-ru.org/ann?test"
        val infoDict = "d6:lengthi104857600e4:name${fileName.length}:${fileName}e"
        val bencode = "d8:announce${trackerUrl.length}:${trackerUrl}4:info${infoDict}e"
        val torrentBytes = bencode.toByteArray(Charsets.UTF_8)

        val expectedSha1 = MessageDigest.getInstance("SHA-1")
            .digest(infoDict.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

        val parsed = BencodeTorrentParser.parseTorrentBytes(torrentBytes)
        assertNotNull(parsed)
        assertEquals(expectedSha1, parsed!!.infoHashHex)
        assertEquals("Movie_1080p.mkv", parsed.displayName)
        assertEquals(104857600L, parsed.totalLengthBytes)
        assertTrue(parsed.trackers.contains("http://bt.t-ru.org/ann?test"))
        assertTrue(parsed.magnetUri.startsWith("magnet:?xt=urn:btih:$expectedSha1"))

        // Bare magnet enrichment adds RuTracker open retrackers (bt.t-ru.org)
        val bareMagnet = "magnet:?xt=urn:btih:$expectedSha1"
        val enriched = BencodeTorrentParser.enrichMagnetUri(bareMagnet, expectedSha1, "Dune 2")
        assertNotNull(enriched)
        assertTrue(enriched!!.contains("bt.t-ru.org"))
        assertTrue(enriched.contains("dn=Dune%202"))
    }

    @Test
    fun torrServerClient_normalizesUrlsAndMatchesEpisodesVoiceoversAndSubtitles() {
        assertEquals("http://127.0.0.1:8090", TorrServerClient.normalizeBaseUrl(""))
        assertEquals("http://192.168.1.55:8090", TorrServerClient.normalizeBaseUrl("192.168.1.55"))
        assertEquals("http://192.168.1.55:8095", TorrServerClient.normalizeBaseUrl("http://192.168.1.55:8095/"))
        assertEquals("https://torr.example.com", TorrServerClient.normalizeBaseUrl("https://torr.example.com/"))

        val torrent = TorrServerTorrent(
            hash = "0123456789abcdef0123456789abcdef01234567",
            title = "Frieren [BDRip 1080p]",
            torrentSize = 4_000_000_000L,
            files = listOf(
                TorrServerFileEntry(1, "Frieren/Episode 01.mkv", "Episode 01.mkv", "Frieren", 1_500_000_000L),
                TorrServerFileEntry(2, "Frieren/Episode 02.mkv", "Episode 02.mkv", "Frieren", 1_500_000_000L),
                TorrServerFileEntry(3, "Frieren/RUS Sound/StudioBand/Episode 01.mka", "Episode 01.mka", "StudioBand", 120_000_000L),
                TorrServerFileEntry(4, "Frieren/RUS Sound/StudioBand/Episode 02.mka", "Episode 02.mka", "StudioBand", 120_000_000L),
                TorrServerFileEntry(5, "Frieren/RUS Sound/AniLibria/Episode 01.mka", "Episode 01.mka", "AniLibria", 110_000_000L),
                TorrServerFileEntry(6, "Frieren/RUS Subs/Episode 01.ass", "Episode 01.ass", "RUS Subs", 250_000L)
            )
        )

        val bundleDefault = TorrServerClient.buildPlaylistFromTorrent(
            rawBaseUrl = "http://127.0.0.1:8090",
            torrent = torrent,
            selectedVoiceover = null
        )
        assertEquals(2, bundleDefault.episodes.size)
        assertEquals(listOf("AniLibria", "StudioBand"), bundleDefault.availableVoiceovers)
        assertEquals(1, bundleDefault.episodes[0].autoSubtitleTracks.size)

        val bundleSelected = TorrServerClient.buildPlaylistFromTorrent(
            rawBaseUrl = "http://127.0.0.1:8090",
            torrent = torrent,
            selectedVoiceover = "StudioBand"
        )
        assertEquals("StudioBand", bundleSelected.selectedVoiceover)
        assertEquals(1, bundleSelected.episodes[0].autoAudioTracks.size)
        assertTrue(bundleSelected.episodes[0].autoAudioTracks[0].uri.contains("index=3"))
    }

    @Test
    fun torrentSearch_prioritizesRuTrackerAndRutorAndDeduplicatesByHash() {
        val sharedHash = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
        val raw = listOf(
            TorrentSearchResult(
                id = "1",
                title = "Interstellar 2014 2160p UHD BDRemux HDR HEVC",
                trackerName = "Global DHT",
                trackerGroup = TorrentTrackerGroup.OTHER,
                magnetUri = "magnet:?xt=urn:btih:$sharedHash",
                torrentFileUrl = null,
                topicUrl = null,
                infoHash = sharedHash,
                sizeBytes = 65_000_000_000L,
                sizeLabel = "60.5 GB",
                seeders = 200,
                leechers = 15
            ),
            TorrentSearchResult(
                id = "2",
                title = "Интерстеллар / Interstellar (2014) 2160p UHD BDRemux HDR HEVC",
                trackerName = "RuTracker",
                trackerGroup = TorrentTrackerGroup.RUTRACKER,
                magnetUri = "magnet:?xt=urn:btih:$sharedHash",
                torrentFileUrl = null,
                topicUrl = "https://rutracker.org/forum/viewtopic.php?t=1",
                infoHash = sharedHash,
                sizeBytes = 65_000_000_000L,
                sizeLabel = "60.5 GB",
                seeders = 85,
                leechers = 4
            ),
            TorrentSearchResult(
                id = "3",
                title = "Интерстеллар / Interstellar (2014) BDRip 1080p",
                trackerName = "Rutor",
                trackerGroup = TorrentTrackerGroup.RUTOR,
                magnetUri = "magnet:?xt=urn:btih:bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb",
                torrentFileUrl = null,
                topicUrl = "https://rutor.info/torrent/2",
                infoHash = "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb",
                sizeBytes = 15_000_000_000L,
                sizeLabel = "14.0 GB",
                seeders = 150,
                leechers = 9
            )
        )

        val deduped = TorrentSearchService.deduplicateResults(raw)
        assertEquals(2, deduped.size)

        val sortedPriority = TorrentSearchService.sortAndFilter(
            results = deduped,
            trackerFilter = TorrentTrackerGroup.ALL,
            qualityFilter = TorrentQualityFilter.ALL,
            sortMode = TorrentSortMode.PRIORITY
        )
        assertEquals(TorrentTrackerGroup.RUTRACKER, sortedPriority[0].trackerGroup)
        assertEquals(200, sortedPriority[0].seeders) // merged max seeders
        assertEquals(TorrentTrackerGroup.RUTOR, sortedPriority[1].trackerGroup)

        val uhdOnly = TorrentSearchService.sortAndFilter(
            results = deduped,
            trackerFilter = TorrentTrackerGroup.ALL,
            qualityFilter = TorrentQualityFilter.UHD_4K,
            sortMode = TorrentSortMode.PRIORITY
        )
        assertEquals(1, uhdOnly.size)
        assertEquals("RuTracker", uhdOnly[0].trackerName)
    }

    @Test
    fun antiBotAndDoh_detectsCloudflareDdosGuardSinkholesAndFailsOverMirrors() = runTest {
        // 1. Cloudflare JS challenge returning HTTP 200 on JSON endpoint
        val cfReason = ResilientTrackerNetwork.detectAntiBotOrBlock(
            statusCode = 200,
            headers = Headers.headersOf("Server", "cloudflare"),
            body = "<!DOCTYPE html><html><head><title>Just a moment...</title></head><body>__cf_chl_opt</body></html>",
            expectedKind = ExpectedResponseKind.JSON
        )
        assertEquals(AntiBotBlockReason.CLOUDFLARE_CHALLENGE, cfReason)

        // 2. DDoS-Guard challenge
        val ddosReason = ResilientTrackerNetwork.detectAntiBotOrBlock(
            statusCode = 403,
            headers = Headers.headersOf("Server", "ddos-guard"),
            body = "<html><head><title>DDoS-Guard</title></head></html>",
            expectedKind = ExpectedResponseKind.HTML
        )
        assertEquals(AntiBotBlockReason.DDOS_GUARD, ddosReason)

        // 3. ISP block page
        val ispReason = ResilientTrackerNetwork.detectAntiBotOrBlock(
            statusCode = 200,
            headers = null,
            body = "<html><body>Доступ к информационному ресурсу ограничен (eais.rkn.gov.ru)</body></html>",
            expectedKind = ExpectedResponseKind.HTML
        )
        assertEquals(AntiBotBlockReason.ISP_BLOCK, ispReason)

        // 4. Clean JSON passes
        val cleanReason = ResilientTrackerNetwork.detectAntiBotOrBlock(
            statusCode = 200,
            headers = Headers.headersOf("Content-Type", "application/json"),
            body = """[{"title":"Matrix 4K","tracker":"rutracker","hash":"0123456789abcdef0123456789abcdef01234567","sid":42}]""",
            expectedKind = ExpectedResponseKind.JSON
        )
        assertNull(cleanReason)

        // 5. DoH JSON parser filters out poisoned 127.0.0.1 & private IPs while keeping valid public IPs
        val dohJson = """
            {
              "Status": 0,
              "Answer": [
                {"name": "jacred.xyz", "type": 1, "TTL": 300, "data": "127.0.0.1"},
                {"name": "jacred.xyz", "type": 1, "TTL": 300, "data": "10.0.0.5"},
                {"name": "jacred.xyz", "type": 1, "TTL": 300, "data": "104.21.55.12"}
              ]
            }
        """.trimIndent()
        val resolvedIps = ResilientTrackerNetwork.parseDohJsonResponse("jacred.xyz", dohJson)
        assertEquals(1, resolvedIps.size)
        assertEquals("jacred.xyz", resolvedIps[0].hostName)
        assertEquals("104.21.55.12", resolvedIps[0].hostAddress)
        assertFalse(ResilientTrackerNetwork.isValidPublicTrackerIp(InetAddress.getByName("127.0.0.1")))
        assertFalse(ResilientTrackerNetwork.isValidPublicTrackerIp(InetAddress.getByName("192.168.1.1")))

        // 6. Multi-mirror parsing + failover when primary mirror hits Cloudflare anti-bot
        MirrorHealthRegistry.resetAll()
        val mirrors = ResilientTrackerNetwork.parseMirrorUrls(
            rawInput = "https://dead-mirror.example, https://backup-custom.example",
            defaultMirrors = TorrentSearchService.DEFAULT_JACRED_MIRRORS,
            includeDefaultsAsFallback = true
        )
        assertTrue(mirrors.size >= 6)
        assertEquals("https://dead-mirror.example", mirrors[0])
        assertEquals("https://backup-custom.example", mirrors[1])

        val failoverOutcome = TorrentSearchService.queryMirrorsWithFailover(
            candidates = listOf("https://blocked.example", "https://dead.example", "https://healthy.example"),
            batchSize = 2,
            isNonEmpty = { it.isNotEmpty() }
        ) { mirror ->
            when (mirror) {
                "https://blocked.example" -> MirrorFetchOutcome.AntiBotBlocked(
                    AntiBotBlockReason.CLOUDFLARE_CHALLENGE,
                    mirror
                )
                "https://dead.example" -> MirrorFetchOutcome.NetworkFailure("Timeout", mirror)
                else -> MirrorFetchOutcome.Success(listOf("RuTracker Hit"), mirror, 120L)
            }
        }
        assertTrue(failoverOutcome is MirrorFetchOutcome.Success)
        assertEquals("https://healthy.example", (failoverOutcome as MirrorFetchOutcome.Success).mirrorUrl)

        // Healthy mirror must now be ranked #1 ahead of blocked/dead mirrors
        val reranked = MirrorHealthRegistry.rankMirrors(
            listOf("https://blocked.example", "https://dead.example", "https://healthy.example")
        )
        assertEquals("https://healthy.example", reranked.first())
    }

    @Test
    fun parsers_handleRewrittenRutorMirrorsAndAniLibriaJson() {
        val rutorMirrorHtml = """
            <table>
              <tr class="gai">
                <td>25 Окт 25</td>
                <td>
                  <a class="downgif" href="https://6tor.org/download/999111">D</a>
                  <a href="magnet:?xt=urn:btih:1111111111111111111111111111111111111111">M</a>
                  <a href="https://6tor.org/torrent/999111/dune-part-two-2160p">Дюна 2 / Dune: Part Two (2024) UHD BDRemux 2160p HDR</a>
                </td>
                <td align="right">64.5&nbsp;GB</td>
                <td align="center"><span class="green">&nbsp;312</span>&nbsp;<span class="red">18</span></td>
              </tr>
            </table>
        """.trimIndent()
        val rutorParsed = TorrentSearchService.parseRutorHtml(rutorMirrorHtml, "https://6tor.org")
        assertEquals(1, rutorParsed.size)
        assertEquals(312, rutorParsed[0].seeders)
        assertEquals("https://6tor.org/torrent/999111/dune-part-two-2160p", rutorParsed[0].topicUrl)
        assertTrue(rutorParsed[0].magnetUri!!.contains("bt.t-ru.org"))

        val aniLibriaJson = """
            [
              {
                "name": {"main": "Провожающая в последний путь Фрирен", "english": "Sousou no Frieren"},
                "torrents": [
                  {
                    "hash": "2222222222222222222222222222222222222222",
                    "size": 18500000000,
                    "seeders": 94,
                    "leechers": 5,
                    "description": "1-28",
                    "quality": {"description": "WEBRip 1080p HEVC"}
                  }
                ]
              }
            ]
        """.trimIndent()
        val aniParsed = TorrentSearchService.parseAniLibriaJson(aniLibriaJson)
        assertEquals(1, aniParsed.size)
        assertEquals("AniLibria", aniParsed[0].trackerName)
        assertEquals(TorrentTrackerGroup.ANIME, aniParsed[0].trackerGroup)
        assertEquals(94, aniParsed[0].seeders)
        assertTrue(aniParsed[0].magnetUri!!.contains("2222222222222222222222222222222222222222"))
    }

    @Test
    fun resourcesAndSettingsCategories_includeTorrServerAndTorrentsTab() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val ruConfig = android.content.res.Configuration(context.resources.configuration).apply {
            setLocale(java.util.Locale("ru"))
        }
        val enConfig = android.content.res.Configuration(context.resources.configuration).apply {
            setLocale(java.util.Locale("en"))
        }
        assertEquals("Торренты", context.createConfigurationContext(ruConfig).getString(R.string.nav_torrents))
        assertEquals("Torrents", context.createConfigurationContext(enConfig).getString(R.string.nav_torrents))
        assertTrue(SettingsCategory.entries.contains(SettingsCategory.TORRENTS))

        val parsedJson = TorrServerClient.parseTorrentJson(
            json = JSONObject(
                """
                {
                  "hash": "1234567890abcdef1234567890abcdef12345678",
                  "title": "Sample Release",
                  "torrent_size": 2097152,
                  "file_stats": [
                    {"id": 1, "path": "Sample/video.mp4", "length": 2097152}
                  ]
                }
                """.trimIndent()
            ),
            fallbackHash = "",
            fallbackTitle = ""
        )
        assertEquals(1, parsedJson.files.size)
        assertEquals("video.mp4", parsedJson.files[0].fileName)
    }
}
