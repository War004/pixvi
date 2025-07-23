package com.example.pixvi.utils

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import com.example.pixvi.network.response.Detail.IllustData
import com.example.pixvi.network.response.Detail.PixivNovelResponse
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode

// Data structure for the final parsed result
data class ParsedNovelResult(
    val novelResponse: PixivNovelResponse,
    val contentBlocks: List<ContentBlock>
)

// The different types of content blocks for Jetpack Compose
sealed class ContentBlock {
    data class Text(val annotatedString: AnnotatedString) : ContentBlock()
    data class Image(val imageUrl: String) : ContentBlock()
    data object PageBreak : ContentBlock()
    data class Chapter(val title: String) : ContentBlock()
}

object NovelParser {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true // Helps with potentially malformed JSON snippets
    }

    /**
     * Main entry point for the parser.
     * Takes the full HTML response and returns a structured result.
     */
    fun parseFullResponse(html: String): ParsedNovelResult {
        // Step 1: Extract the JSON string from the HTML
        val jsonString = extractJsonFromHtml(html)

        // Step 2: Deserialize the JSON into our data models
        val novelResponse = json.decodeFromString<PixivNovelResponse>(jsonString)

        val illustsMap: Map<String, IllustData> = when (val illustsElement = novelResponse.novel.illusts) {
            is JsonObject -> {
                // If it's an object, decode it and this value will be assigned to illustsMap.
                json.decodeFromJsonElement(illustsElement)
            }
            else -> {
                // Otherwise, this empty map will be assigned to illustsMap.
                emptyMap()
            }
        }

        // Step 3: Parse the novel's main text into content blocks
        val contentBlocks = parseNovelTextToBlocks(novelResponse.novel.text, illustsMap)

        return ParsedNovelResult(novelResponse, contentBlocks)
    }

    /**
     * Extracts the 'pixiv' JSON object from the raw HTML string AND cleans it.
     */
    private fun extractJsonFromHtml(html: String): String {
        val document = Jsoup.parse(html)
        val scriptElement = document.select("head > script").firstOrNull { !it.hasAttr("src") }
            ?: throw IllegalStateException("Could not find the JSON data script in the HTML.")

        val scriptContent = scriptElement.html()

        val startIndex = scriptContent.indexOf("value: ") + "value: ".length

        // Find the end of the JSON object by counting braces to ensure we get the complete object
        var endIndex = startIndex
        var braceCount = 0
        var inString = false
        var escapeNext = false

        for (i in startIndex until scriptContent.length) {
            val char = scriptContent[i]

            if (escapeNext) {
                escapeNext = false
                continue
            }

            when (char) {
                '\\' -> escapeNext = true
                '"' -> if (!escapeNext) inString = !inString
                '{' -> if (!inString) braceCount++
                '}' -> if (!inString) {
                    braceCount--
                    if (braceCount == 0) {
                        endIndex = i + 1
                        break
                    }
                }
            }
        }

        // Check if both start and end were found correctly
        if (startIndex < "value: ".length || endIndex <= startIndex) {
            throw IllegalStateException("Could not extract JSON object from the script.")
        }

        // Extract the raw JSON using the calculated end index
        val rawJson = scriptContent.substring(startIndex, endIndex)

        // Keep the previous fix to clean any trailing commas within the extracted string
        val trailingCommaRegex = """,\s*([}\]])""".toRegex()
        return trailingCommaRegex.replace(rawJson) {
            it.groupValues[1]
        }
    }

    /**
     * Parses the raw novel text string into a list of [ContentBlock]s.
     */
    private fun parseNovelTextToBlocks(
        rawText: String,
        illustsMap: Map<String, IllustData>
    ): List<ContentBlock> {
        val contentBlocks = mutableListOf<ContentBlock>()
        val paragraphBuilder = StringBuilder() //Text building for a singel paragrpah

        //in the string json the paragrpah are indicated by \n\n
        fun processParagraph() {
            if (paragraphBuilder.isNotEmpty()) {
                // Parse the entire accumulated paragraph at once.
                val annotatedString = parseFormattedText(paragraphBuilder.toString())
                contentBlocks.add(ContentBlock.Text(annotatedString))
                // Reset the builder for the next paragraph.
                paragraphBuilder.clear()
            }
        }

        // Split the text by newlines to process line-by-line for special tags
        rawText.lines().forEach { line ->
            val trimmedText = line.trim()
            when {
                trimmedText.startsWith("[newpage]") -> {
                    processParagraph()
                    contentBlocks.add(ContentBlock.PageBreak)
                }
                trimmedText.startsWith("[chapter:") -> {
                    processParagraph()

                    val title = trimmedText.substringAfter(":").substringBefore("]")
                    contentBlocks.add(ContentBlock.Chapter(title))
                }
                trimmedText.startsWith("[pixivimage:") -> {
                    processParagraph()
                    val imageId = trimmedText.substringAfter(":").substringBefore("]")
                    val illustData = illustsMap[imageId]
                    if (illustData != null) {
                        //medium is available, and small and original are null

                        val mediumUrl = illustData.details.images.medium

                        mediumUrl?.let { url ->
                            contentBlocks.add(ContentBlock.Image(url))
                        }

                        /*
                        val bestAvailableUrl = illustData.details.images.original
                            ?: illustData.details.images.medium
                            ?: illustData.details.images.small

                        // Only add the image block if we actually found a valid URL.
                        bestAvailableUrl?.let { url ->
                            contentBlocks.add(ContentBlock.Image(url))
                        }*/
                    }
                }
                trimmedText.isBlank() -> {
                    processParagraph()
                }
                // Otherwise, it's a regular line of text to be added to the current paragraph.
                else -> {
                    // Add a space between lines to prevent words from merging.
                    // This check prevents adding a leading space at the start of a paragraph.
                    if (paragraphBuilder.isNotEmpty()) {
                        paragraphBuilder.append(" ")
                    }
                    paragraphBuilder.append(line)
                }
            }
        }
        processParagraph()
        return contentBlocks
    }

    /**
     * Parses a single line of text for HTML and Pixiv-specific formatting.
     */
    fun parseFormattedText(text: String): AnnotatedString {
        return buildAnnotatedString {
            val document = Jsoup.parse(text)
            document.body().childNodes().forEach { node ->
                appendNode(node)
            }
        }
    }

    private fun AnnotatedString.Builder.appendNode(node: Node) {
        when (node) {
            is TextNode -> appendPixivText(node.text())
            is Element -> {
                val style = when (node.tagName()) {
                    "a" -> {
                        val url = node.attr("href")
                        addStringAnnotation(tag = "URL", annotation = url, start = this.length, end = this.length + node.text().length)
                        SpanStyle(color = Color(0xFF007BFF), textDecoration = TextDecoration.Underline)
                    }
                    "span" -> parseSpanStyle(node.attr("style"))
                    "strong", "b" -> SpanStyle(fontWeight = FontWeight.Bold)
                    "em", "i" -> SpanStyle(fontWeight = FontWeight.Normal) // Can be customized with fonts
                    "br" -> {
                        append("\n")
                        SpanStyle()
                    }
                    else -> SpanStyle()
                }

                withStyle(style) {
                    node.childNodes().forEach { child ->
                        appendNode(child)
                    }
                }
            }
        }
    }

    private fun AnnotatedString.Builder.appendPixivText(text: String) {
        val rubyRegex = """\[\[rb:(.+?) > (.+?)\]\]""".toRegex()
        var lastIndex = 0

        rubyRegex.findAll(text).forEach { matchResult ->
            append(text.substring(lastIndex, matchResult.range.first))

            val baseText = matchResult.groupValues[1]
            val rubyText = matchResult.groupValues[2]

            // Annotate for custom rendering in Compose
            pushStringAnnotation(tag = "RUBY", annotation = rubyText)
            append(baseText)
            pop()

            lastIndex = matchResult.range.last + 1
        }

        if (lastIndex < text.length) {
            append(text.substring(lastIndex))
        }
    }

    private fun parseSpanStyle(styleAttr: String): SpanStyle {
        // Simplified style parser
        if (styleAttr.contains("color")) {
            val colorString = styleAttr.substringAfter("color:").substringBefore(';').trim()
            try {
                return SpanStyle(color = Color(android.graphics.Color.parseColor(colorString)))
            } catch (e: Exception) { /* Fallback */ }
        }
        return SpanStyle()
    }
}