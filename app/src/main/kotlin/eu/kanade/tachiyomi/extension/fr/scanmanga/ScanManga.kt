package eu.kanade.tachiyomi.extension.fr.scanmanga

import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.io.Serializable
import java.net.URI
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.regex.Pattern

// --- DÉFINITIONS DU PROTOCOLE MIHON ---
interface SManga : Serializable {
    var url: String
    var title: String
    var artist: String?
    var author: String?
    var description: String?
    var genre: String?
    var status: Int
    var thumbnail_url: String?
    var update_strategy: Int
    var initialized: Boolean

    fun setUrlWithoutDomain(url: String) {
        this.url = if (url.startsWith("http")) {
            runCatching {
                val uri = URI(url)
                val out = uri.path + if (uri.query != null) "?" + uri.query else ""
                if (out.isEmpty()) "/" else out
            }.getOrDefault(url)
        } else url
    }

    companion object {
        const val UNKNOWN = 0
        const val ONGOING = 1
        const val COMPLETED = 2
        fun create(): SManga = SMangaImpl()
    }
}

class SMangaImpl : SManga {
    override var url: String = ""
    override var title: String = ""
    override var artist: String? = null
    override var author: String? = null
    override var description: String? = null
    override var genre: String? = null
    override var status: Int = SManga.UNKNOWN
    override var thumbnail_url: String? = null
    override var update_strategy: Int = 0
    override var initialized: Boolean = false
}

interface SChapter : Serializable {
    var url: String
    var name: String
    var date_upload: Long
    var chapter_number: Float
    var scanlator: String?

    fun setUrlWithoutDomain(url: String) {
        this.url = if (url.startsWith("http")) {
            runCatching {
                val uri = URI(url)
                val out = uri.path + if (uri.query != null) "?" + uri.query else ""
                if (out.isEmpty()) "/" else out
            }.getOrDefault(url)
        } else url
    }

    companion object {
        fun create(): SChapter = SChapterImpl()
    }
}

class SChapterImpl : SChapter {
    override var url: String = ""
    override var name: String = ""
    override var date_upload: Long = 0L
    override var chapter_number: Float = -1f
    override var scanlator: String? = null
}

class Page(val index: Int, val url: String = "", var imageUrl: String? = null)
class FilterList(val filters: List<Any> = emptyList())

abstract class ParsedHttpSource {
    abstract val name: String
    abstract val baseUrl: String
    abstract val lang: String
    abstract val supportsLatest: Boolean

    open val client: OkHttpClient = OkHttpClient()
    open val headers: Headers get() = headersBuilder().build()
    open fun headersBuilder(): Headers.Builder = Headers.Builder()

    abstract fun popularMangaRequest(page: Int): Request
    abstract fun popularMangaSelector(): String
    abstract fun popularMangaFromElement(element: Element): SManga
    abstract fun popularMangaNextPageSelector(): String?

    abstract fun latestUpdatesRequest(page: Int): Request
    abstract fun latestUpdatesSelector(): String
    abstract fun latestUpdatesFromElement(element: Element): SManga
    abstract fun latestUpdatesNextPageSelector(): String?

    abstract fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request
    abstract fun searchMangaSelector(): String
    abstract fun searchMangaFromElement(element: Element): SManga
    abstract fun searchMangaNextPageSelector(): String?

    abstract fun mangaDetailsParse(document: Document): SManga
    abstract fun chapterListSelector(): String
    abstract fun chapterFromElement(element: Element): SChapter
    abstract fun pageListParse(document: Document): List<Page>
    abstract fun imageUrlParse(document: Document): String
}

// --- IMPLÉMENTATION SOURCE SCAN-MANGA ---
class ScanManga : ParsedHttpSource() {

    override val name = "Scan-Manga"
    override val baseUrl = "https://www.scan-manga.com"
    override val lang = "fr"
    override val supportsLatest = true

    override fun headersBuilder(): Headers.Builder = Headers.Builder()
        .add("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36")
        .add("Referer", "$baseUrl/")

    override fun popularMangaRequest(page: Int): Request =
        Request.Builder().url("$baseUrl/top-mangas.html?page=$page").headers(headers).build()

    override fun popularMangaSelector(): String = "div.content_manga div.element, div.listing_manga div.manga_item, div.manga"

    override fun popularMangaFromElement(element: Element): SManga = SManga.create().apply {
        val link = element.select("div.title a, h3 a, a.titre_manga, a.manga_title").first()!!
        title = link.text().trim()
        setUrlWithoutDomain(link.attr("href"))
        thumbnail_url = element.select("div.image img, img.manga_img, img.cover").attr("abs:src")
    }

    override fun popularMangaNextPageSelector(): String? = "div.pagination a.next, a:contains(Suivant)"

    override fun latestUpdatesRequest(page: Int): Request =
        Request.Builder().url(baseUrl).headers(headers).build()

    override fun latestUpdatesSelector(): String = popularMangaSelector()
    override fun latestUpdatesFromElement(element: Element): SManga = popularMangaFromElement(element)
    override fun latestUpdatesNextPageSelector(): String? = null

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request =
        Request.Builder().url("$baseUrl/recherche?q=${URLEncoder.encode(query, "UTF-8")}").headers(headers).build()

    override fun searchMangaSelector(): String = popularMangaSelector()
    override fun searchMangaFromElement(element: Element): SManga = popularMangaFromElement(element)
    override fun searchMangaNextPageSelector(): String? = null

    override fun mangaDetailsParse(document: Document): SManga = SManga.create().apply {
        description = document.select("div.description, div.synopsis, div.texte_detail").text().trim()
        genre = document.select("div.genres a, div.tags a, span.genre a").joinToString { it.text().trim() }
        val statusText = document.select("div.status, span.statut").text().lowercase()
        status = when {
            statusText.contains("en cours") -> SManga.ONGOING
            statusText.contains("terminé") || statusText.contains("complete") -> SManga.COMPLETED
            else -> SManga.UNKNOWN
        }
        thumbnail_url = document.select("div.cover img, div.image_manga img, div.manga_image img").attr("abs:src")
    }

    override fun chapterListSelector(): String = "div.chapitres_list div.chapitre, ul.chapters_list li, div.element_chapitre"

    override fun chapterFromElement(element: Element): SChapter = SChapter.create().apply {
        val link = element.select("a").first()!!
        setUrlWithoutDomain(link.attr("href"))
        name = link.text().trim()
        date_upload = runCatching {
            SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE).parse(element.select("span.date, span.time").text().trim())?.time ?: 0L
        }.getOrDefault(0L)
    }

    override fun pageListParse(document: Document): List<Page> {
        val pages = mutableListOf<Page>()
        val html = document.html()

        val scriptPattern = Pattern.compile("var\\s+(?:images|pages|lstImages)\\s*=\\s*\\[(.*?)\\];", Pattern.DOTALL)
        val matcher = scriptPattern.matcher(html)

        if (matcher.find()) {
            val rawContent = matcher.group(1) ?: ""
            val urlPattern = Pattern.compile("['\"](https?://[^'\"]+|/[^'\"]+)['\"]")
            val urlMatcher = urlPattern.matcher(rawContent)
            var index = 0
            while (urlMatcher.find()) {
                var url = urlMatcher.group(1)
                if (url.startsWith("/")) url = baseUrl + url
                pages.add(Page(index++, "", url))
            }
        }

        if (pages.isEmpty()) {
            document.select("div.reader-images img, div#lecture img, div.image_scan img").forEachIndexed { index, element ->
                val src = element.attr("data-src").ifEmpty { element.attr("src") }.trim()
                if (src.isNotBlank()) {
                    pages.add(Page(index, "", if (src.startsWith("http")) src else baseUrl + src))
                }
            }
        }

        if (pages.isEmpty()) throw Exception("Structure de lecture non reconnue.")
        return pages
    }

    override fun imageUrlParse(document: Document): String = ""
}
