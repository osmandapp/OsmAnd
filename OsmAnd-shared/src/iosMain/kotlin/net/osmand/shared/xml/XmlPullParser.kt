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

		// High half (bytes 0x80..0xFF) of the single-byte code pages that turn up in legacy
		// GPX/KML exports. Index 0 == byte 0x80; bytes 0x00..0x7F are plain ASCII.
		private val LATIN1_HIGH: IntArray = IntArray(128) { 0x80 + it }

		private val CP1252_HIGH: IntArray = run {
			// 0x80..0x9F differ from Latin-1; 0xA0..0xFF are identical to Latin-1.
			val overrides = intArrayOf(
				0x20AC, 0x0081, 0x201A, 0x0192, 0x201E, 0x2026, 0x2020, 0x2021,
				0x02C6, 0x2030, 0x0160, 0x2039, 0x0152, 0x008D, 0x017D, 0x008F,
				0x0090, 0x2018, 0x2019, 0x201C, 0x201D, 0x2022, 0x2013, 0x2014,
				0x02DC, 0x2122, 0x0161, 0x203A, 0x0153, 0x009D, 0x017E, 0x0178
			)
			IntArray(128) { if (it < overrides.size) overrides[it] else 0x80 + it }
		}

		private val CP1251_HIGH: IntArray = run {
			// 0x80..0xBF are a lookup table; 0xC0..0xFF map linearly to U+0410..U+044F.
			val low = intArrayOf(
				0x0402, 0x0403, 0x201A, 0x0453, 0x201E, 0x2026, 0x2020, 0x2021,
				0x20AC, 0x2030, 0x0409, 0x2039, 0x040A, 0x040C, 0x040B, 0x040F,
				0x0452, 0x2018, 0x2019, 0x201C, 0x201D, 0x2022, 0x2013, 0x2014,
				0xFFFD, 0x2122, 0x0459, 0x203A, 0x045A, 0x045C, 0x045B, 0x045F,
				0x00A0, 0x040E, 0x045E, 0x0408, 0x00A4, 0x0490, 0x00A6, 0x00A7,
				0x0401, 0x00A9, 0x0404, 0x00AB, 0x00AC, 0x00AD, 0x00AE, 0x0407,
				0x00B0, 0x00B1, 0x0406, 0x0456, 0x0491, 0x00B5, 0x00B6, 0x00B7,
				0x0451, 0x2116, 0x0454, 0x00BB, 0x0458, 0x0405, 0x0455, 0x0457
			)
			IntArray(128) { if (it < 64) low[it] else 0x0410 + (it - 64) }
		}
	}

	private var text: String = ""
	private var pos: Int = 0
	private var currentTokenType: Int = START_DOCUMENT
	private var currentName: String? = null   // raw name as written, prefix included if any
	private var currentText: String? = null
	private var attributes: List<Pair<String, String>> = emptyList() // raw name -> decoded value
	private var pendingEndTagName: String? = null // set right after reporting an empty element's START_TAG
	private val openTags = mutableListOf<String>()
	private var detectedEncoding: String = "UTF-8" // resolved from BOM / XML declaration in decodeBytes()
	// org.xmlpull.v1.XmlPullParser defaults to namespaces NOT processed (raw/qualified names);
	// ImportGpx explicitly turns this off for KML's gx: prefix, implying GpxUtilities relies on
	// the default being "processed" (local names) - matches the previous Qt/libxml2 behaviour.
	private var processNamespaces: Boolean = true

	private fun beginParsing(bytes: ByteArray) {
		var decoded = decodeBytes(bytes)
		if (decoded.isNotEmpty() && decoded[0].code == 0xFEFF) {
			decoded = decoded.substring(1) // strip a BOM code point if one survived decoding
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

	// QXmlStreamReader (the previous Obj-C backend) auto-detected the input encoding from the
	// byte-order mark and the <?xml ... encoding="..."?> declaration. decodeToString() is UTF-8
	// only, so the detection is reproduced here for the encodings that actually occur in
	// GPX/KML: UTF-8, UTF-16 (both endians) and the common single-byte legacy code pages.
	private fun decodeBytes(bytes: ByteArray): String {
		if (bytes.size >= 3 &&
			bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()) {
			detectedEncoding = "UTF-8"
			return bytes.decodeToString(startIndex = 3)
		}
		if (bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte()) {
			detectedEncoding = "UTF-16BE"
			return decodeUtf16(bytes, 2, bigEndian = true)
		}
		if (bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte()) {
			detectedEncoding = "UTF-16LE"
			return decodeUtf16(bytes, 2, bigEndian = false)
		}
		val declared = sniffDeclaredEncoding(bytes)
		detectedEncoding = declared ?: "UTF-8"
		return when (declared) {
			null, "utf-8", "utf8", "us-ascii", "ascii" -> bytes.decodeToString()
			"utf-16", "utf-16le", "utf16", "unicode" -> decodeUtf16(bytes, 0, bigEndian = false)
			"utf-16be" -> decodeUtf16(bytes, 0, bigEndian = true)
			"iso-8859-1", "iso8859-1", "iso_8859-1", "latin1", "latin-1", "l1", "cp819" ->
				decodeSingleByte(bytes, LATIN1_HIGH)
			"windows-1252", "windows1252", "cp1252" -> decodeSingleByte(bytes, CP1252_HIGH)
			"windows-1251", "windows1251", "cp1251" -> decodeSingleByte(bytes, CP1251_HIGH)
			else -> bytes.decodeToString() // unknown label: best-effort UTF-8, as before
		}
	}

	// Reads the leading bytes as ASCII (NUL padding skipped, so an unmarked UTF-16 declaration
	// stays legible) and extracts encoding="..." / encoding='...' from the <?xml ... ?> prolog.
	private fun sniffDeclaredEncoding(bytes: ByteArray): String? {
		val limit = minOf(bytes.size, 512)
		val sb = StringBuilder(limit)
		for (i in 0 until limit) {
			val v = bytes[i].toInt() and 0xFF
			if (v in 0x09..0x7E) sb.append(v.toChar())
		}
		val decl = sb.toString()
		if (!decl.startsWith("<?xml")) return null
		val declEnd = decl.indexOf("?>")
		if (declEnd < 0) return null
		val header = decl.substring(0, declEnd)
		val key = header.indexOf("encoding")
		if (key < 0) return null
		var p = key + 8 // "encoding".length
		while (p < header.length && header[p].isDeclSpace()) p++
		if (p >= header.length || header[p] != '=') return null
		p++
		while (p < header.length && header[p].isDeclSpace()) p++
		if (p >= header.length) return null
		val quote = header[p]
		if (quote != '"' && quote != '\'') return null
		val close = header.indexOf(quote, p + 1)
		if (close < 0) return null
		return header.substring(p + 1, close).trim().lowercase()
	}

	private fun Char.isDeclSpace(): Boolean = this == ' ' || this == '\t' || this == '\n' || this == '\r'

	private fun decodeUtf16(bytes: ByteArray, startIndex: Int, bigEndian: Boolean): String {
		val sb = StringBuilder((bytes.size - startIndex).coerceAtLeast(0) / 2)
		var i = startIndex
		while (i + 1 < bytes.size) {
			val b0 = bytes[i].toInt() and 0xFF
			val b1 = bytes[i + 1].toInt() and 0xFF
			val unit = if (bigEndian) (b0 shl 8) or b1 else (b1 shl 8) or b0
			sb.append(unit.toChar()) // raw UTF-16 code unit; surrogate pairs carry over unchanged
			i += 2
		}
		return sb.toString()
	}

	// table maps bytes 0x80..0xFF (128 entries, index 0 == byte 0x80) to code points.
	private fun decodeSingleByte(bytes: ByteArray, table: IntArray): String {
		val sb = StringBuilder(bytes.size)
		for (b in bytes) {
			val v = b.toInt() and 0xFF
			sb.append(if (v < 0x80) v.toChar() else table[v - 0x80].toChar())
		}
		return sb.toString()
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
		detectedEncoding = "UTF-8"
	}

	@Throws(XmlParserException::class)
	actual fun setInput(input: Source, inputEncoding: String?) {
		val byteArray = input.buffer().readByteArray()
		beginParsing(byteArray)
	}

	actual fun getInputEncoding(): String? = detectedEncoding

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
			// Synthetic END_TAG for a self-closing element. readStartTag() never pushed it onto
			// openTags, so this must NOT pop the stack (doing so would consume the real parent).
			val name = pendingEndTagName!!
			pendingEndTagName = null
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
