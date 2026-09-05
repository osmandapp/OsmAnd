package net.osmand.shared.xml

import net.osmand.shared.io.KFile
import okio.IOException
import okio.Source
import okio.buffer
import okio.use

// Pure-Kotlin pull parser: no Kotlin/Native <-> Objective-C boundary at all. The previous
// implementation forwarded every single call (next/getName/getText/getAttributeValue...) to
// an Obj-C object (OAXmlStreamReader, backed first by Qt then by libxml2); every such call
// paid for an objc_msgSend dispatch plus, for every string, two copies (C/Obj-C string ->
// NSString -> Kotlin String) and Kotlin/Native runtime bookkeeping for the bridged object.
// This version reads the whole input once via okio (already pure Kotlin/Native, no bridging -
// see KFile.source()) and tokenizes the decoded text entirely in Kotlin.
//
// Matches the real, exercised contract of the two commonMain callers (GpxUtilities.loadGpxFile
// and ImportGpx.parseKmlStreaming): only next()/getEventType() ever surface START_DOCUMENT,
// START_TAG, TEXT, END_TAG, END_DOCUMENT (comments/processing instructions are skipped,
// CDATA is coalesced into text, entities are expanded) - this is the standard next() contract
// used by org.xmlpull.v1.XmlPullParser, i.e. exactly what Android's actual (backed by
// android.util.Xml) already provides. getAttributeValue(namespace, name) is always called
// with namespace = "" by real callers, i.e. a plain attribute-name lookup.
actual class XmlPullParser actual constructor() {

	actual companion object {
		actual const val NO_NAMESPACE: String = ""
		actual const val START_DOCUMENT: Int = 0
		actual const val END_DOCUMENT: Int = 1
		actual const val START_TAG: Int = 2
		actual const val END_TAG: Int = 3
		actual const val TEXT: Int = 4
		actual const val CDSECT: Int = 5
		actual const val ENTITY_REF: Int = 6
		actual const val IGNORABLE_WHITESPACE: Int = 7
		actual const val PROCESSING_INSTRUCTION: Int = 8
		actual const val COMMENT: Int = 9
		actual const val DOCDECL: Int = 10

		private const val FEATURE_PROCESS_NAMESPACES =
			"http://xmlpull.org/v1/doc/features.html#process-namespaces"
	}

	private var text: String = ""
	private var pos: Int = 0
	private var currentTokenType: Int = START_DOCUMENT
	private var currentName: String? = null   // raw name as written, prefix included if any
	private var currentText: String? = null
	private var attributes: List<Pair<String, String>> = emptyList() // raw name -> decoded value
	private var pendingEndTagName: String? = null // set right after reporting an empty element's START_TAG
	private val openTags = mutableListOf<String>()
	// org.xmlpull.v1.XmlPullParser defaults to namespaces NOT processed (raw/qualified names);
	// ImportGpx explicitly turns this off for KML's gx: prefix, implying GpxUtilities relies on
	// the default being "processed" (local names) - matches the previous Qt/libxml2 behaviour.
	private var processNamespaces: Boolean = true

	private fun beginParsing(bytes: ByteArray) {
		var decoded = bytes.decodeToString()
		if (decoded.isNotEmpty() && decoded[0].code == 0xFEFF) {
			decoded = decoded.substring(1) // strip UTF-8 BOM if present
		}
		text = decoded
		pos = 0
		currentTokenType = START_DOCUMENT
		currentName = null
		currentText = null
		attributes = emptyList()
		pendingEndTagName = null
		openTags.clear()
	}

	@Throws(XmlParserException::class)
	actual fun setFeature(name: String, state: Boolean) {
		if (name == FEATURE_PROCESS_NAMESPACES) {
			processNamespaces = state
		}
	}

	actual fun getFeature(name: String): Boolean {
		return if (name == FEATURE_PROCESS_NAMESPACES) processNamespaces else true
	}

	@Throws(XmlParserException::class)
	actual fun setProperty(name: String, value: Any?) {
		// Not implemented - unused by real callers.
	}

	actual fun getProperty(name: String): Any? = null

	@Throws(XmlParserException::class)
	actual fun setInput(file: KFile, inputEncoding: String?) {
		val bytes = file.source().buffer().use { it.readByteArray() }
		beginParsing(bytes)
	}

	@Throws(IOException::class)
	actual fun close() {
		text = ""
		pos = 0
		currentTokenType = START_DOCUMENT
		currentName = null
		currentText = null
		attributes = emptyList()
		pendingEndTagName = null
		openTags.clear()
	}

	@Throws(XmlParserException::class)
	actual fun setInput(input: Source, inputEncoding: String?) {
		val byteArray = input.buffer().readByteArray()
		beginParsing(byteArray)
	}

	actual fun getInputEncoding(): String? = "UTF-8"

	@Throws(XmlParserException::class)
	actual fun defineEntityReplacementText(entityName: String, replacementText: String) {
		// Not implemented - unused by real callers (only the 5 predefined XML entities and
		// numeric character references are decoded).
	}

	@Throws(XmlParserException::class)
	actual fun getNamespaceCount(depth: Int): Int = -1 // Not implemented

	@Throws(XmlParserException::class)
	actual fun getNamespacePrefix(pos: Int): String? = null // Not implemented

	@Throws(XmlParserException::class)
	actual fun getNamespaceUri(pos: Int): String? = null // Not implemented

	actual fun getNamespace(prefix: String?): String? = null // Not implemented

	actual fun getDepth(): Int = openTags.size

	actual fun getPositionDescription(): String? = null // Not implemented

	actual fun getLineNumber(): Int = 0 // Not implemented - unused by real callers

	actual fun getColumnNumber(): Int = 0 // Not implemented - unused by real callers

	@Throws(XmlParserException::class)
	actual fun isWhitespace(): Boolean {
		val t = currentText ?: return false
		return t.isNotEmpty() && t.all { it == ' ' || it == '\t' || it == '\n' || it == '\r' }
	}

	actual fun getText(): String? = currentText

	actual fun getTextCharacters(holderForStartAndLength: IntArray): CharArray? = null // Not implemented

	actual fun getNamespace(): String? = null // namespace URI resolution not implemented; unused by real callers

	actual fun getName(): String? = applyNamespaceMode(currentName)

	actual fun getPrefix(): String? = currentName?.let { prefixOf(it) }

	@Throws(XmlParserException::class)
	actual fun isEmptyElementTag(): Boolean {
		return currentTokenType == START_TAG && pendingEndTagName != null
	}

	actual fun getAttributeCount(): Int = attributes.size

	actual fun getAttributeNamespace(index: Int): String? = null // not resolved; unused by real callers

	actual fun getAttributeName(index: Int): String? {
		return attributes.getOrNull(index)?.first?.let { applyNamespaceMode(it) }
	}

	actual fun getAttributePrefix(index: Int): String? {
		return attributes.getOrNull(index)?.first?.let { prefixOf(it) }
	}

	actual fun getAttributeType(index: Int): String? = null // Not implemented

	actual fun isAttributeDefault(index: Int): Boolean = true // Not implemented

	actual fun getAttributeValue(index: Int): String? = attributes.getOrNull(index)?.second

	actual fun getAttributeValue(namespace: String?, name: String?): String? {
		if (name == null) return null
		for ((attrName, attrValue) in attributes) {
			if (applyNamespaceMode(attrName) == name) return attrValue
		}
		return null
	}

	@Throws(XmlParserException::class)
	actual fun getEventType(): Int = currentTokenType

	@Throws(XmlParserException::class, IOException::class)
	actual fun next(): Int {
		if (pendingEndTagName != null) {
			val name = pendingEndTagName!!
			pendingEndTagName = null
			popOpenTag(name)
			currentName = name
			currentText = null
			attributes = emptyList()
			currentTokenType = END_TAG
			return currentTokenType
		}

		val textBuilder = StringBuilder()
		var sawText = false

		while (true) {
			if (pos >= text.length) {
				if (openTags.isNotEmpty()) {
					throw XmlParserException("Premature end of document, unclosed tag: <${openTags.last()}>")
				}
				currentTokenType = END_DOCUMENT
				currentName = null
				currentText = null
				return currentTokenType
			}

			if (text[pos] == '<') {
				if (startsWithAt("<!--")) {
					skipComment()
					continue
				}
				if (startsWithAt("<![CDATA[")) {
					sawText = true
					textBuilder.append(scanCData())
					continue
				}
				if (startsWithAt("<!DOCTYPE") || startsWithAt("<!doctype")) {
					skipBalancedMarkup()
					continue
				}
				if (startsWithAt("<?")) {
					skipProcessingInstruction()
					continue
				}
				if (sawText) {
					// Coalesce: emit the accumulated text now, the tag itself is handled by
					// the next call to next().
					currentTokenType = TEXT
					currentName = null
					currentText = textBuilder.toString()
					return currentTokenType
				}
				return if (pos + 1 < text.length && text[pos + 1] == '/') readEndTag() else readStartTag()
			} else {
				sawText = true
				val start = pos
				while (pos < text.length && text[pos] != '<') pos++
				textBuilder.append(decodeEntities(text.substring(start, pos)))
			}
		}
	}

	@Throws(XmlParserException::class, IOException::class)
	actual fun nextToken(): Int = -1 // Not implemented - unused by real callers

	@Throws(XmlParserException::class, IOException::class)
	actual fun require(type: Int, namespace: String?, name: String?) {
		// Not implemented (no-op) - matches the previous behaviour; unused by real callers.
	}

	@Throws(XmlParserException::class, IOException::class)
	actual fun nextText(): String {
		if (currentTokenType != START_TAG) {
			throw XmlParserException("nextText() called while not positioned on START_TAG")
		}
		return when (val type = next()) {
			TEXT -> {
				val result = currentText ?: ""
				if (next() != END_TAG) {
					throw XmlParserException("nextText(): expected END_TAG right after TEXT")
				}
				result
			}
			END_TAG -> ""
			else -> throw XmlParserException("nextText(): unexpected event type $type")
		}
	}

	@Throws(XmlParserException::class, IOException::class)
	actual fun nextTag(): Int = -1 // Not implemented - unused by real callers

	// --- tokenizer internals -------------------------------------------------------------

	private fun applyNamespaceMode(rawName: String?): String? {
		if (rawName == null) return null
		if (!processNamespaces) return rawName
		val idx = rawName.indexOf(':')
		return if (idx >= 0) rawName.substring(idx + 1) else rawName
	}

	private fun prefixOf(rawName: String): String? {
		val idx = rawName.indexOf(':')
		return if (idx >= 0) rawName.substring(0, idx) else null
	}

	private fun startsWithAt(prefix: String): Boolean {
		if (pos + prefix.length > text.length) return false
		for (i in prefix.indices) {
			if (text[pos + i] != prefix[i]) return false
		}
		return true
	}

	private fun skipWhitespace() {
		while (pos < text.length && isXmlWhitespace(text[pos])) pos++
	}

	private fun isXmlWhitespace(c: Char) = c == ' ' || c == '\t' || c == '\n' || c == '\r'

	private fun isNameStartChar(c: Char) = c.isLetter() || c == '_' || c == ':'

	private fun isNameChar(c: Char) = c.isLetterOrDigit() || c == '_' || c == ':' || c == '-' || c == '.'

	private fun scanName(): String {
		val start = pos
		if (pos >= text.length || !isNameStartChar(text[pos])) {
			throw XmlParserException("Expected an element/attribute name at position $pos")
		}
		pos++
		while (pos < text.length && isNameChar(text[pos])) pos++
		return text.substring(start, pos)
	}

	private fun skipComment() {
		val end = text.indexOf("-->", pos + 4)
		pos = if (end >= 0) end + 3 else text.length
	}

	private fun skipProcessingInstruction() {
		val end = text.indexOf("?>", pos + 2)
		pos = if (end >= 0) end + 2 else text.length
	}

	private fun scanCData(): String {
		val contentStart = pos + 9 // "<![CDATA[".length
		val end = text.indexOf("]]>", contentStart)
		return if (end >= 0) {
			val content = text.substring(contentStart, end)
			pos = end + 3
			content
		} else {
			val content = text.substring(contentStart)
			pos = text.length
			content
		}
	}

	// Balances nested '<'/'>' - used for <!DOCTYPE ...> which may contain an internal
	// subset ("[ ... ]") with its own '<!ENTITY ...>' declarations.
	private fun skipBalancedMarkup() {
		var depth = 0
		do {
			if (pos >= text.length) return
			when (text[pos]) {
				'<' -> depth++
				'>' -> depth--
			}
			pos++
		} while (depth > 0)
	}

	private fun readStartTag(): Int {
		pos++ // consume '<'
		val rawName = scanName()
		val attrs = mutableListOf<Pair<String, String>>()
		var selfClosing = false
		while (true) {
			skipWhitespace()
			if (pos >= text.length) {
				throw XmlParserException("Unexpected end of document inside tag <$rawName>")
			}
			val c = text[pos]
			if (c == '/' && pos + 1 < text.length && text[pos + 1] == '>') {
				selfClosing = true
				pos += 2
				break
			}
			if (c == '>') {
				pos++
				break
			}
			val attrName = scanName()
			skipWhitespace()
			if (pos >= text.length || text[pos] != '=') {
				throw XmlParserException("Expected '=' after attribute name '$attrName' in <$rawName>")
			}
			pos++ // consume '='
			skipWhitespace()
			if (pos >= text.length || (text[pos] != '"' && text[pos] != '\'')) {
				throw XmlParserException("Expected a quoted value for attribute '$attrName' in <$rawName>")
			}
			val quote = text[pos]
			pos++
			val valueStart = pos
			val valueEnd = text.indexOf(quote, pos)
			if (valueEnd < 0) {
				throw XmlParserException("Unterminated value for attribute '$attrName' in <$rawName>")
			}
			attrs.add(attrName to decodeEntities(text.substring(valueStart, valueEnd)))
			pos = valueEnd + 1
		}

		currentName = rawName
		currentText = null
		attributes = attrs
		currentTokenType = START_TAG

		if (selfClosing) {
			pendingEndTagName = rawName
		} else {
			openTags.add(rawName)
		}
		return currentTokenType
	}

	private fun readEndTag(): Int {
		pos += 2 // consume '</'
		val rawName = scanName()
		skipWhitespace()
		if (pos >= text.length || text[pos] != '>') {
			throw XmlParserException("Malformed closing tag for '$rawName'")
		}
		pos++ // consume '>'
		popOpenTag(rawName)
		currentName = rawName
		currentText = null
		attributes = emptyList()
		currentTokenType = END_TAG
		return currentTokenType
	}

	private fun popOpenTag(name: String) {
		if (openTags.isEmpty()) {
			throw XmlParserException("Unexpected closing tag </$name>: no matching open tag")
		}
		val top = openTags.removeAt(openTags.lastIndex)
		if (top != name) {
			throw XmlParserException("Mismatched closing tag: expected </$top>, found </$name>")
		}
	}

	private fun decodeEntities(raw: String): String {
		if (raw.indexOf('&') < 0) return raw
		val sb = StringBuilder(raw.length)
		var i = 0
		while (i < raw.length) {
			val c = raw[i]
			if (c == '&') {
				val semi = raw.indexOf(';', i + 1)
				val resolved = if (semi >= 0) resolveEntity(raw.substring(i + 1, semi)) else null
				if (resolved != null) {
					sb.append(resolved)
					i = semi + 1
					continue
				}
			}
			sb.append(c)
			i++
		}
		return sb.toString()
	}

	private fun resolveEntity(entity: String): String? {
		return when {
			entity == "amp" -> "&"
			entity == "lt" -> "<"
			entity == "gt" -> ">"
			entity == "quot" -> "\""
			entity == "apos" -> "'"
			entity.startsWith("#x") || entity.startsWith("#X") ->
				entity.substring(2).toIntOrNull(16)?.let(::codePointToString)
			entity.startsWith("#") ->
				entity.substring(1).toIntOrNull()?.let(::codePointToString)
			else -> null // unknown/custom entity - leave the raw "&name;" text untouched
		}
	}

	private fun codePointToString(codePoint: Int): String? {
		if (codePoint < 0 || codePoint > 0x10FFFF) return null
		return if (codePoint <= 0xFFFF) {
			codePoint.toChar().toString()
		} else {
			val cp = codePoint - 0x10000
			val high = (0xD800 + (cp shr 10)).toChar()
			val low = (0xDC00 + (cp and 0x3FF)).toChar()
			charArrayOf(high, low).concatToString()
		}
	}
}
