package com.example.torrent

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.Dns
import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.net.URI
import java.net.URLEncoder
import java.net.UnknownHostException
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

enum class ExpectedResponseKind {
    JSON,
    XML,
    HTML,
    TORRENT_BINARY,
    ANY
}

enum class AntiBotBlockReason(val descriptionRu: String) {
    CLOUDFLARE_CHALLENGE("Защита Cloudflare (JS/Turnstile проверка)"),
    DDOS_GUARD("Защита DDoS-Guard"),
    CAPTCHA_WALL("Капча / антибот-проверка"),
    ISP_BLOCK("Блокировка домена провайдером"),
    UNEXPECTED_HTML_WALL("Зеркало вернуло HTML-заглушку вместо API"),
    HTTP_BLOCKED("Доступ отклонён защитой узла")
}

data class MirrorStats(
    val lastSuccessEpochMs: Long = 0L,
    val avgLatencyMs: Long = 0L,
    val consecutiveFailures: Int = 0,
    val cooldownUntilEpochMs: Long = 0L,
    val lastErrorReason: String? = null
)

object MirrorHealthRegistry {
    private val statsMap = ConcurrentHashMap<String, MirrorStats>()

    private const val SHORT_COOLDOWN_MS = 90_000L
    private const val MEDIUM_COOLDOWN_MS = 4 * 60_000L
    private const val ANTIBOT_COOLDOWN_MS = 8 * 60_000L

    fun recordSuccess(mirrorUrl: String, latencyMs: Long, nowMs: Long = System.currentTimeMillis()) {
        val key = normalizeKey(mirrorUrl)
        statsMap.compute(key) { _, prev ->
            val safeLatency = latencyMs.coerceAtLeast(1L)
            val smoothed = if (prev == null || prev.avgLatencyMs <= 0L) {
                safeLatency
            } else {
                ((prev.avgLatencyMs * 2L) + safeLatency) / 3L
            }
            MirrorStats(
                lastSuccessEpochMs = nowMs,
                avgLatencyMs = smoothed,
                consecutiveFailures = 0,
                cooldownUntilEpochMs = 0L,
                lastErrorReason = null
            )
        }
    }

    fun recordFailure(
        mirrorUrl: String,
        reason: String,
        isAntiBot: Boolean = false,
        nowMs: Long = System.currentTimeMillis()
    ) {
        val key = normalizeKey(mirrorUrl)
        statsMap.compute(key) { _, prev ->
            val failures = (prev?.consecutiveFailures ?: 0) + 1
            val cooldownDuration = when {
                isAntiBot -> ANTIBOT_COOLDOWN_MS
                failures >= 2 -> MEDIUM_COOLDOWN_MS
                else -> SHORT_COOLDOWN_MS
            }
            MirrorStats(
                lastSuccessEpochMs = prev?.lastSuccessEpochMs ?: 0L,
                avgLatencyMs = prev?.avgLatencyMs ?: 0L,
                consecutiveFailures = failures,
                cooldownUntilEpochMs = nowMs + cooldownDuration,
                lastErrorReason = reason
            )
        }
    }

    fun rankMirrors(candidates: List<String>, nowMs: Long = System.currentTimeMillis()): List<String> {
        if (candidates.size <= 1) return candidates
        val indexed = candidates.mapIndexed { idx, url -> idx to url }
        return indexed.sortedWith { (idxA, urlA), (idxB, urlB) ->
            val stA = statsMap[normalizeKey(urlA)]
            val stB = statsMap[normalizeKey(urlB)]
            val coolA = (stA?.cooldownUntilEpochMs ?: 0L) > nowMs
            val coolB = (stB?.cooldownUntilEpochMs ?: 0L) > nowMs

            if (coolA != coolB) {
                return@sortedWith if (coolA) 1 else -1
            }
            if (coolA && coolB) {
                val cmpCool = (stA?.cooldownUntilEpochMs ?: 0L).compareTo(stB?.cooldownUntilEpochMs ?: 0L)
                if (cmpCool != 0) return@sortedWith cmpCool
            }

            val hasSuccessA = (stA?.lastSuccessEpochMs ?: 0L) > 0L && (stA?.consecutiveFailures ?: 0) == 0
            val hasSuccessB = (stB?.lastSuccessEpochMs ?: 0L) > 0L && (stB?.consecutiveFailures ?: 0) == 0
            if (hasSuccessA != hasSuccessB) {
                return@sortedWith if (hasSuccessA) -1 else 1
            }
            if (hasSuccessA && hasSuccessB) {
                val latA = stA?.avgLatencyMs ?: Long.MAX_VALUE
                val latB = stB?.avgLatencyMs ?: Long.MAX_VALUE
                if (kotlin.math.abs(latA - latB) > 120L) {
                    return@sortedWith latA.compareTo(latB)
                }
            }
            idxA.compareTo(idxB)
        }.map { it.second }
    }

    fun getStats(mirrorUrl: String): MirrorStats? = statsMap[normalizeKey(mirrorUrl)]

    fun resetAll() {
        statsMap.clear()
    }

    private fun normalizeKey(url: String): String =
        url.trim().trimEnd('/').lowercase(Locale.ROOT)
}

class InMemoryTrackerCookieJar : CookieJar {
    private val store = ConcurrentHashMap<String, MutableList<Cookie>>()

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        if (cookies.isEmpty()) return
        val hostKey = url.host.lowercase(Locale.ROOT)
        store.compute(hostKey) { _, existing ->
            val list = (existing ?: mutableListOf()).toMutableList()
            val now = System.currentTimeMillis()
            list.removeAll { c -> c.expiresAt < now || cookies.any { it.name == c.name } }
            list.addAll(cookies.filter { it.expiresAt >= now })
            list
        }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val hostKey = url.host.lowercase(Locale.ROOT)
        val current = store[hostKey] ?: return emptyList()
        val now = System.currentTimeMillis()
        return current.filter { it.expiresAt >= now && it.matches(url) }
    }
}

object ResilientTrackerNetwork {

    const val BROWSER_USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"

    private const val DNS_TTL_MS = 15 * 60_000L

    // Direct IP DoH endpoints so DNS bootstrap itself can never fail on ISP DNS poisoning
    private val DOH_JSON_ENDPOINTS = listOf(
        "https://1.1.1.1/dns-query",
        "https://1.0.0.1/dns-query",
        "https://8.8.8.8/resolve"
    )

    private val IPV4_LITERAL_REGEX = Regex("""^\d{1,3}(?:\.\d{1,3}){3}$""")

    private data class CachedDnsEntry(
        val addresses: List<InetAddress>,
        val expiresAtMs: Long
    )

    private val dnsCache = ConcurrentHashMap<String, CachedDnsEntry>()
    val cookieJar = InMemoryTrackerCookieJar()

    private val dohBootstrapClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(3, TimeUnit.SECONDS)
            .readTimeout(4, TimeUnit.SECONDS)
            .followRedirects(false)
            .build()
    }

    val dohDns: Dns = object : Dns {
        override fun lookup(hostname: String): List<InetAddress> =
            resolveHostnameResilient(hostname, enableDoh = true)
    }

    private val dohHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .dns(dohDns)
            .cookieJar(cookieJar)
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(9, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    private val standardHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .cookieJar(cookieJar)
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(9, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    fun client(enableDoh: Boolean): OkHttpClient =
        if (enableDoh) dohHttpClient else standardHttpClient

    fun parseMirrorUrls(
        rawInput: String,
        defaultMirrors: List<String>,
        includeDefaultsAsFallback: Boolean = true
    ): List<String> {
        val tokens = rawInput
            .split(',', ';', '\n', '\r', '\t', ' ')
            .map { it.trim().trimEnd('/') }
            .filter { it.isNotEmpty() }

        val normalizedUserUrls = tokens.mapNotNull { token ->
            val withScheme = when {
                token.startsWith("http://", ignoreCase = true) ||
                    token.startsWith("https://", ignoreCase = true) -> token
                isLocalOrPrivateHost(token.substringBefore('/').substringBefore(':')) -> "http://$token"
                else -> "https://$token"
            }
            runCatching {
                val u = URI(withScheme)
                if (u.host.isNullOrBlank()) null else withScheme.trimEnd('/')
            }.getOrNull()
        }

        val combined = mutableListOf<String>()
        for (url in normalizedUserUrls) {
            if (combined.none { it.equals(url, ignoreCase = true) }) {
                combined.add(url)
            }
        }
        if (includeDefaultsAsFallback || combined.isEmpty()) {
            for (mirror in defaultMirrors) {
                val clean = mirror.trim().trimEnd('/')
                if (clean.isNotEmpty() && combined.none { it.equals(clean, ignoreCase = true) }) {
                    combined.add(clean)
                }
            }
        }
        return combined
    }

    internal fun isLocalOrPrivateHost(host: String): Boolean {
        val clean = host.trim().trim('[', ']').lowercase(Locale.ROOT)
        if (clean.isEmpty()) return true
        if (clean == "localhost" ||
            clean.endsWith(".local") ||
            clean.endsWith(".lan") ||
            clean.endsWith(".home") ||
            !clean.contains('.')
        ) {
            return true
        }
        if (IPV4_LITERAL_REGEX.matches(clean)) {
            return true
        }
        if (clean.contains(':')) {
            return true // IPv6 literal
        }
        return false
    }

    internal fun isValidPublicTrackerIp(address: InetAddress): Boolean {
        if (address.isLoopbackAddress ||
            address.isAnyLocalAddress ||
            address.isLinkLocalAddress ||
            address.isSiteLocalAddress ||
            address.isMulticastAddress
        ) {
            return false
        }
        if (address is Inet4Address) {
            val b = address.address
            val b0 = b[0].toInt() and 0xFF
            val b1 = b[1].toInt() and 0xFF
            // Reject 0.0.0.0/8, 100.64.0.0/10 (CGNAT often used by ISP block redirects), 127.0.0.0/8
            if (b0 == 0 || b0 == 127) return false
            if (b0 == 100 && b1 in 64..127) return false
        } else if (address is Inet6Address) {
            val b = address.address
            val firstByte = b[0].toInt() and 0xFF
            // Reject IPv6 Unique Local fc00::/7
            if ((firstByte and 0xFE) == 0xFC) return false
        }
        return true
    }

    private fun resolveHostnameResilient(hostname: String, enableDoh: Boolean): List<InetAddress> {
        val normalizedHost = hostname.trim().lowercase(Locale.ROOT)
        if (!enableDoh || isLocalOrPrivateHost(normalizedHost)) {
            return Dns.SYSTEM.lookup(hostname)
        }

        val now = System.currentTimeMillis()
        dnsCache[normalizedHost]?.let { cached ->
            if (cached.expiresAtMs > now && cached.addresses.isNotEmpty()) {
                return cached.addresses
            }
        }

        // 1. Try System DNS first ONLY if it returns valid non-poisoned public IPs, or DoH?
        // In ISP-blocked networks, System DNS either throws UnknownHostException or returns a sinkhole IP.
        // Querying DoH first guarantees authentic unpoisoned records and caches them for 15 minutes!
        val dohAddresses = queryDohProviders(normalizedHost)
        if (dohAddresses.isNotEmpty()) {
            dnsCache[normalizedHost] = CachedDnsEntry(dohAddresses, now + DNS_TTL_MS)
            return dohAddresses
        }

        // 2. Fallback to System DNS filtered against bogon/loopback sinkholes
        val systemLookup = runCatching { Dns.SYSTEM.lookup(hostname) }.getOrDefault(emptyList())
        val filteredSystem = systemLookup.filter { isValidPublicTrackerIp(it) }
        if (filteredSystem.isNotEmpty()) {
            dnsCache[normalizedHost] = CachedDnsEntry(filteredSystem, now + DNS_TTL_MS)
            return filteredSystem
        }

        if (systemLookup.isNotEmpty()) {
            throw UnknownHostException("DNS-ответ для $hostname заблокирован или подменён провайдером")
        }
        throw UnknownHostException("Не удалось разрешить адрес $hostname")
    }

    private fun queryDohProviders(hostname: String): List<InetAddress> {
        val encodedName = URLEncoder.encode(hostname, Charsets.UTF_8.name())
        for (endpoint in DOH_JSON_ENDPOINTS) {
            val resolved = runCatching {
                val req = Request.Builder()
                    .url("$endpoint?name=$encodedName&type=A")
                    .header("Accept", "application/dns-json")
                    .header("User-Agent", BROWSER_USER_AGENT)
                    .get()
                    .build()
                dohBootstrapClient.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) return@use emptyList()
                    val body = resp.body?.string().orEmpty()
                    parseDohJsonResponse(hostname, body)
                }
            }.getOrDefault(emptyList())
            if (resolved.isNotEmpty()) {
                return resolved
            }
        }
        return emptyList()
    }

    internal fun parseDohJsonResponse(hostname: String, jsonBody: String): List<InetAddress> {
        if (jsonBody.isBlank() || !jsonBody.trimStart().startsWith("{")) return emptyList()
        return runCatching {
            val root = JSONObject(jsonBody)
            if (root.optInt("Status", -1) != 0) return@runCatching emptyList()
            val answers = root.optJSONArray("Answer") ?: return@runCatching emptyList()
            val addresses = mutableListOf<InetAddress>()
            for (i in 0 until answers.length()) {
                val rec = answers.optJSONObject(i) ?: continue
                val type = rec.optInt("type", 0)
                val data = rec.optString("data").trim()
                if ((type == 1 || type == 28) && data.isNotEmpty()) {
                    val rawIp = runCatching { InetAddress.getByName(data).address }.getOrNull() ?: continue
                    val boundAddr = InetAddress.getByAddress(hostname, rawIp)
                    if (isValidPublicTrackerIp(boundAddr)) {
                        addresses.add(boundAddr)
                    }
                }
            }
            addresses
        }.getOrDefault(emptyList())
    }

    fun detectAntiBotOrBlock(
        statusCode: Int,
        headers: Headers?,
        body: String,
        expectedKind: ExpectedResponseKind
    ): AntiBotBlockReason? {
        val cfMitigated = headers?.get("cf-mitigated").orEmpty().lowercase(Locale.ROOT)
        if (cfMitigated.contains("challenge")) {
            return AntiBotBlockReason.CLOUDFLARE_CHALLENGE
        }

        if (statusCode == 451) {
            return AntiBotBlockReason.ISP_BLOCK
        }
        if (statusCode in 520..526) {
            return AntiBotBlockReason.CLOUDFLARE_CHALLENGE
        }

        val sample = body.take(8192).lowercase(Locale.ROOT)
        val trimmedStart = sample.trimStart()

        if (sample.contains("eais.rkn.gov.ru") ||
            sample.contains("zapret-info") ||
            sample.contains("warning.rt.ru") ||
            sample.contains("доступ к информационному ресурсу ограничен") ||
            sample.contains("ресурс заблокирован по решению")
        ) {
            return AntiBotBlockReason.ISP_BLOCK
        }

        if (sample.contains("<title>just a moment") ||
            sample.contains("<title>один момент") ||
            sample.contains("attention required! | cloudflare") ||
            sample.contains("__cf_chl_") ||
            sample.contains("cdn-cgi/challenge-platform") ||
            sample.contains("cf-browser-verification")
        ) {
            return AntiBotBlockReason.CLOUDFLARE_CHALLENGE
        }

        val serverHeader = headers?.get("Server").orEmpty().lowercase(Locale.ROOT)
        if (sample.contains("<title>ddos-guard") ||
            sample.contains("check.ddos-guard.net") ||
            (serverHeader.contains("ddos-guard") && (statusCode == 403 || statusCode == 503))
        ) {
            return AntiBotBlockReason.DDOS_GUARD
        }

        if ((statusCode == 403 || statusCode == 429 || statusCode == 503) && statusCode !in 200..299) {
            return AntiBotBlockReason.HTTP_BLOCKED
        }

        val looksLikeHtml = trimmedStart.startsWith("<!doctype html") ||
            trimmedStart.startsWith("<html") ||
            trimmedStart.startsWith("<head") ||
            trimmedStart.startsWith("<body")

        if (expectedKind == ExpectedResponseKind.JSON && looksLikeHtml) {
            return detectHtmlChallengeSubtype(sample) ?: AntiBotBlockReason.UNEXPECTED_HTML_WALL
        }

        if (expectedKind == ExpectedResponseKind.XML && looksLikeHtml &&
            !sample.contains("<rss") && !sample.contains("<feed")
        ) {
            return detectHtmlChallengeSubtype(sample) ?: AntiBotBlockReason.UNEXPECTED_HTML_WALL
        }

        if (expectedKind == ExpectedResponseKind.HTML && body.length < 30_000) {
            detectHtmlChallengeSubtype(sample)?.let { return it }
        }

        return null
    }

    private fun detectHtmlChallengeSubtype(lowerHtmlSample: String): AntiBotBlockReason? {
        return when {
            lowerHtmlSample.contains("smartcaptcha") ||
                lowerHtmlSample.contains("g-recaptcha") ||
                lowerHtmlSample.contains("hcaptcha") ||
                lowerHtmlSample.contains("проверка браузера") -> AntiBotBlockReason.CAPTCHA_WALL
            lowerHtmlSample.contains("ddos-guard") -> AntiBotBlockReason.DDOS_GUARD
            lowerHtmlSample.contains("cloudflare") -> AntiBotBlockReason.CLOUDFLARE_CHALLENGE
            else -> null
        }
    }
}
