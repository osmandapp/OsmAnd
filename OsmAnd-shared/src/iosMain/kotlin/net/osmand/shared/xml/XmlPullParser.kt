package net.osmand.shared.xml

import net.osmand.shared.io.KFile
import okio.BufferedSource
import okio.IOException
import okio.Source
import okio.buffer

// Pure-Kotlin pull parser: no Kotlin/Native <-> Objective-C boundary at all. The previous
// implementation forwarded every single call (next/getName/getText/getAttributeValue...) to
// an Obj-C object (OAXmlStreamReader, backed first by Qt then by libxml2); every such call
// paid for an objc_msgSend dispatch plus, for every string, two copies (C/Obj-C string ->
// NSString -> Kotlin String) and Kotlin/Native runtime bookkeeping for the bridged object.
// This version reads the input via okio (already pure Kotlin/Native, no bridging - see
// KFile.source()) and tokenizes it entirely in Kotlin.
//
// The input is streamed, like QXmlStreamReader did: bytes are pulled in BYTE_CHUNK_SIZE
// chunks and decoded into a CHAR_BUFFER_SIZE sliding window, so the resident cost of a parse
// is those two buffers plus the token being built - not the file size. Every lookahead the
// tokenizer needs is bounded (the longest is "<![CDATA[", plus MAX_ENTITY_LENGTH while
// resolving an entity reference), which is what lets the window stay small.
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

		private const val BYTE_CHUNK_SIZE = 8 * 1024
		private const val CHAR_BUFFER_SIZE = 8 * 1024
		private const val SNIFF_LIMIT = 512
		private const val MAX_ENTITY_LENGTH = 64
		// Head room kept free in the char window so that a chunk of N bytes plus up to 3 bytes
		// carried over from the previous chunk always fits as decoded characters.
		private const val DECODE_HEADROOM = 16

		private const val NO_QUOTE = '\u0000'

		private const val MODE_UTF8 = 0
		private const val MODE_UTF16LE = 1
		private const val MODE_UTF16BE = 2
		private const val MODE_SINGLE_BYTE = 3

		private val EMPTY_CHARS = CharArray(0)
		private val EMPTY_BYTES = ByteArray(0)

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

	private var source: BufferedSource? = null
	private var ownsSource: Boolean = false
	private var sourceExhausted: Boolean = true

	private var byteChunk: ByteArray = EMPTY_BYTES
	private val carryBytes: ByteArray = ByteArray(4)
	private var carryLength: Int = 0
	private var pendingCr: Boolean = false

	private var buf: CharArray = EMPTY_CHARS
	private var bufLength: Int = 0
	private var pos: Int = 0
	private var discardedChars: Long = 0 // characters dropped ahead of the window, for error text

	private var encodingMode: Int = MODE_UTF8
	private var singleByteTable: IntArray = LATIN1_HIGH

	private var currentTokenType: Int = START_DOCUMENT
	private var currentName: String? = null   // raw name as written, prefix included if any
	private var currentText: String? = null
	private var attributes: List<Pair<String, String>> = emptyList() // raw name -> decoded value
	private var pendingEndTagName: String? = null // set right after reporting an empty element's START_TAG
	private val openTags = mutableListOf<String>()
	private var detectedEncoding: String = "UTF-8" // resolved from BOM / XML declaration while priming
	// org.xmlpull.v1.XmlPullParser defaults to namespaces NOT processed (raw/qualified names);
	// ImportGpx explicitly turns this off for KML's gx: prefix, implying GpxUtilities relies on
	// the default being "processed" (local names) - matches the previous Qt/libxml2 behaviour.
	private var processNamespaces: Boolean = true

	private fun beginParsing(input: BufferedSource, owns: Boolean) {
		releaseInput()
		source = input
		ownsSource = owns
		sourceExhausted = false
		byteChunk = ByteArray(BYTE_CHUNK_SIZE)
		buf = CharArray(CHAR_BUFFER_SIZE)
		bufLength = 0
		pos = 0
		discardedChars = 0
		carryLength = 0
		pendingCr = false
		currentTokenType = START_DOCUMENT
		currentName = null
		currentText = null
		attributes = emptyList()
		pendingEndTagName = null
		openTags.clear()
		primeEncoding(input)
		if (ensure(1) && buf[pos].code == 0xFEFF) {
			pos++ // BOM that survived decoding (an unmarked UTF-16 stream, say)
		}
	}

	private fun releaseInput() {
		if (ownsSource) {
			source?.close()
		}
		source = null
		ownsSource = false
		sourceExhausted = true
		byteChunk = EMPTY_BYTES
		buf = EMPTY_CHARS
		bufLength = 0
		pos = 0
		carryLength = 0
		pendingCr = false
	}

	// QXmlStreamReader (the previous Obj-C backend) auto-detected the input encoding from the
	// byte-order mark and the <?xml ... encoding="..."?> declaration. Kotlin only decodes UTF-8,
	// so the detection is reproduced here for the encodings that actually occur in GPX/KML:
	// UTF-8, UTF-16 (both endians) and the common single-byte legacy code pages.
	private fun primeEncoding(input: BufferedSource) {
		val prefix = ByteArray(SNIFF_LIMIT)
		var prefixLength = 0
		while (prefixLength < SNIFF_LIMIT) {
			val read = input.read(prefix, prefixLength, SNIFF_LIMIT - prefixLength)
			if (read == -1) {
				sourceExhausted = true
				break
			}
			prefixLength += read
		}

		var start = 0
		if (prefixLength >= 3 &&
			prefix[0] == 0xEF.toByte() && prefix[1] == 0xBB.toByte() && prefix[2] == 0xBF.toByte()) {
			detectedEncoding = "UTF-8"
			encodingMode = MODE_UTF8
			start = 3
		} else if (prefixLength >= 2 && prefix[0] == 0xFE.toByte() && prefix[1] == 0xFF.toByte()) {
			detectedEncoding = "UTF-16BE"
			encodingMode = MODE_UTF16BE
			start = 2
		} else if (prefixLength >= 2 && prefix[0] == 0xFF.toByte() && prefix[1] == 0xFE.toByte()) {
			detectedEncoding = "UTF-16LE"
			encodingMode = MODE_UTF16LE
			start = 2
		} else {
			val declared = sniffDeclaredEncoding(prefix, prefixLength)
			detectedEncoding = declared ?: "UTF-8"
			when (declared) {
				"utf-16", "utf16", "unicode" -> encodingMode = utf16ModeOf(prefix, prefixLength)
				"utf-16le" -> encodingMode = MODE_UTF16LE
				"utf-16be" -> encodingMode = MODE_UTF16BE
				"iso-8859-1", "iso8859-1", "iso_8859-1", "latin1", "latin-1", "l1", "cp819" -> {
					encodingMode = MODE_SINGLE_BYTE
					singleByteTable = LATIN1_HIGH
				}
				"windows-1252", "windows1252", "cp1252" -> {
					encodingMode = MODE_SINGLE_BYTE
					singleByteTable = CP1252_HIGH
				}
				"windows-1251", "windows1251", "cp1251" -> {
					encodingMode = MODE_SINGLE_BYTE
					singleByteTable = CP1251_HIGH
				}
				else -> encodingMode = MODE_UTF8 // unknown label: best-effort UTF-8, as before
			}
		}
		if (prefixLength > start) {
			decodeChunk(prefix, start, prefixLength)
		}
	}

	// A declaration that says "UTF-16" without saying which end, and without a BOM to tell.
	// The XML spec calls that a fatal error; the bytes still say it plainly, because a document
	// reaching this point starts with '<' (U+003C) - 00 3C big-endian, 3C 00 little-endian.
	// Input matching neither falls back to little-endian, the more common of the two in the wild.
	private fun utf16ModeOf(bytes: ByteArray, length: Int): Int {
		val bigEndian = length >= 2 && bytes[0] == 0x00.toByte() && bytes[1] == 0x3C.toByte()
		return if (bigEndian) MODE_UTF16BE else MODE_UTF16LE
	}

	// Reads the leading bytes as ASCII (NUL padding skipped, so an unmarked UTF-16 declaration
	// stays legible) and extracts encoding="..." / encoding='...' from the <?xml ... ?> prolog.
	private fun sniffDeclaredEncoding(bytes: ByteArray, length: Int): String? {
		val sb = StringBuilder(length)
		for (i in 0 until length) {
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

	// --- input window --------------------------------------------------------------------

	// Guarantees that at least [count] characters are readable at [pos], as long as the input
	// holds that many. [count] must stay well below CHAR_BUFFER_SIZE (see the header comment).
	private fun ensure(count: Int): Boolean {
		if (bufLength - pos >= count) return true
		if (pos > 0) {
			buf.copyInto(buf, 0, pos, bufLength)
			bufLength -= pos
			discardedChars += pos
			pos = 0
		}
		while (bufLength < count && fillBuffer()) {
			// keep pulling until the request is satisfied or the input ends
		}
		return bufLength - pos >= count
	}

	private fun fillBuffer(): Boolean {
		val input = source ?: return false
		while (true) {
			val space = buf.size - bufLength
			if (space < DECODE_HEADROOM || sourceExhausted) return false
			val carry = carryLength
			carryLength = 0
			if (carry > 0) carryBytes.copyInto(byteChunk, 0, 0, carry)
			val want = minOf(byteChunk.size - carry, space - 4)
			val read = input.read(byteChunk, carry, want)
			if (read == -1) {
				sourceExhausted = true // an unfinished multi-byte sequence at EOF is dropped
				return false
			}
			val before = bufLength
			decodeChunk(byteChunk, 0, carry + read)
			if (bufLength > before) return true
		}
	}

	private fun decodeChunk(bytes: ByteArray, from: Int, to: Int) {
		when (encodingMode) {
			MODE_UTF16LE -> decodeUtf16Chunk(bytes, from, to, bigEndian = false)
			MODE_UTF16BE -> decodeUtf16Chunk(bytes, from, to, bigEndian = true)
			MODE_SINGLE_BYTE -> decodeSingleByteChunk(bytes, from, to)
			else -> decodeUtf8Chunk(bytes, from, to)
		}
	}

	private fun decodeUtf8Chunk(bytes: ByteArray, from: Int, to: Int) {
		var i = from
		while (i < to) {
			val b0 = bytes[i].toInt() and 0xFF
			if (b0 < 0x80) {
				appendChar(b0.toChar())
				i++
				continue
			}
			val length = when {
				b0 and 0xE0 == 0xC0 -> 2
				b0 and 0xF0 == 0xE0 -> 3
				b0 and 0xF8 == 0xF0 -> 4
				else -> 1
			}
			if (length == 1) {
				appendChar('\uFFFD')
				i++
				continue
			}
			if (i + length > to) {
				carry(bytes, i, to)
				return
			}
			var codePoint = when (length) {
				2 -> b0 and 0x1F
				3 -> b0 and 0x0F
				else -> b0 and 0x07
			}
			var valid = true
			for (k in 1 until length) {
				val bk = bytes[i + k].toInt() and 0xFF
				if (bk and 0xC0 != 0x80) {
					valid = false
					break
				}
				codePoint = (codePoint shl 6) or (bk and 0x3F)
			}
			if (!valid) {
				appendChar('\uFFFD')
				i++
				continue
			}
			i += length
			if (codePoint <= 0xFFFF) {
				appendChar(codePoint.toChar())
			} else {
				val cp = codePoint - 0x10000
				appendChar((0xD800 + (cp shr 10)).toChar())
				appendChar((0xDC00 + (cp and 0x3FF)).toChar())
			}
		}
	}

	private fun decodeUtf16Chunk(bytes: ByteArray, from: Int, to: Int, bigEndian: Boolean) {
		var i = from
		while (i + 1 < to) {
			val b0 = bytes[i].toInt() and 0xFF
			val b1 = bytes[i + 1].toInt() and 0xFF
			val unit = if (bigEndian) (b0 shl 8) or b1 else (b1 shl 8) or b0
			appendChar(unit.toChar()) // raw UTF-16 code unit; surrogate pairs carry over unchanged
			i += 2
		}
		if (i < to) carry(bytes, i, to)
	}

	// singleByteTable maps bytes 0x80..0xFF (128 entries, index 0 == byte 0x80) to code points.
	private fun decodeSingleByteChunk(bytes: ByteArray, from: Int, to: Int) {
		val table = singleByteTable
		for (i in from until to) {
			val v = bytes[i].toInt() and 0xFF
			appendChar(if (v < 0x80) v.toChar() else table[v - 0x80].toChar())
		}
	}

	private fun carry(bytes: ByteArray, from: Int, to: Int) {
		val count = minOf(to - from, carryBytes.size)
		bytes.copyInto(carryBytes, 0, from, from + count)
		carryLength = count
	}

	// XML line-end normalisation: "\r\n" and a lone "\r" both become "\n" (the pending-CR flag
	// carries the rule across chunk boundaries).
	private fun appendChar(c: Char) {
		if (c == '\r') {
			pendingCr = true
			buf[bufLength++] = '\n'
			return
		}
		if (c == '\n' && pendingCr) {
			pendingCr = false
			return
		}
		pendingCr = false
		buf[bufLength++] = c
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

	// The file handle stays open for the whole parse and is released by close(), which
	// GpxUtilities.loadGpxFile calls from its finally block.
	@Throws(XmlParserException::class)
	actual fun setInput(file: KFile, inputEncoding: String?) {
		beginParsing(file.source().buffer(), owns = true)
	}

	@Throws(IOException::class)
	actual fun close() {
		releaseInput()
		currentTokenType = START_DOCUMENT
		currentName = null
		currentText = null
		attributes = emptyList()
		pendingEndTagName = null
		openTags.clear()
		discardedChars = 0
		detectedEncoding = "UTF-8"
		encodingMode = MODE_UTF8
		singleByteTable = LATIN1_HIGH
	}

	// The caller owns the source it hands over here (ImportGpx.parseKmlStreaming never closes
	// the parser), so close() must not close it.
	@Throws(XmlParserException::class)
	actual fun setInput(input: Source, inputEncoding: String?) {
		beginParsing(input.buffer(), owns = false)
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

	// openTags holds the elements that are currently open, which is one short of the reported
	// depth in the two cases the stack cannot express: an empty element (never pushed) and an
	// END_TAG, which reports the depth of the element it closes - the pop already happened.
	actual fun getDepth(): Int {
		val closing = currentTokenType == END_TAG || pendingEndTagName != null
		return openTags.size + if (closing) 1 else 0
	}

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

		var textBuilder: StringBuilder? = null

		while (true) {
			if (!ensure(1)) {
				if (openTags.isNotEmpty()) {
					throw XmlParserException("Premature end of document, unclosed tag: <${openTags.last()}>")
				}
				currentTokenType = END_DOCUMENT
				currentName = null
				currentText = null
				attributes = emptyList()
				return currentTokenType
			}

			if (buf[pos] == '<') {
				if (startsWithAt("<!--")) {
					skipComment()
					continue
				}
				if (startsWithAt("<![CDATA[")) {
					val builder = textBuilder ?: StringBuilder().also { textBuilder = it }
					readCData(builder)
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
				val builder = textBuilder
				if (builder != null) {
					if (openTags.isEmpty() && isWhitespaceOnly(builder)) {
						textBuilder = null // ignorable whitespace of the prolog/epilog
					} else {
						// Coalesce: emit the accumulated text now, the tag itself is handled by
						// the next call to next().
						currentTokenType = TEXT
						currentName = null
						currentText = builder.toString()
						attributes = emptyList()
						return currentTokenType
					}
				}
				return if (ensure(2) && buf[pos + 1] == '/') readEndTag() else readStartTag()
			} else {
				val builder = textBuilder ?: StringBuilder().also { textBuilder = it }
				readTextRun(builder)
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

	private fun isNamespaceDeclaration(rawName: String): Boolean =
		rawName == "xmlns" || rawName.startsWith("xmlns:")

	private fun prefixOf(rawName: String): String? {
		val idx = rawName.indexOf(':')
		return if (idx >= 0) rawName.substring(0, idx) else null
	}

	private fun startsWithAt(prefix: String): Boolean {
		if (!ensure(prefix.length)) return false
		for (i in prefix.indices) {
			if (buf[pos + i] != prefix[i]) return false
		}
		return true
	}

	private fun skipWhitespace() {
		while (ensure(1) && isXmlWhitespace(buf[pos])) pos++
	}

	private fun isXmlWhitespace(c: Char) = c == ' ' || c == '\t' || c == '\n' || c == '\r'

	private fun isWhitespaceOnly(sb: StringBuilder): Boolean {
		for (i in 0 until sb.length) {
			if (!isXmlWhitespace(sb[i])) return false
		}
		return true
	}

	private fun isNameStartChar(c: Char) = c.isLetter() || c == '_' || c == ':'

	private fun isNameChar(c: Char) = c.isLetterOrDigit() || c == '_' || c == ':' || c == '-' || c == '.'

	private fun scanName(): String {
		if (!ensure(1) || !isNameStartChar(buf[pos])) {
			throw XmlParserException("Expected an element/attribute name at position ${discardedChars + pos}")
		}
		val sb = StringBuilder(16)
		sb.append(buf[pos++])
		while (ensure(1) && isNameChar(buf[pos])) sb.append(buf[pos++])
		return sb.toString()
	}

	private fun skipComment() {
		pos += 4 // "<!--".length
		while (true) {
			if (!ensure(3)) {
				pos = bufLength
				return
			}
			if (buf[pos] == '-' && buf[pos + 1] == '-' && buf[pos + 2] == '>') {
				pos += 3
				return
			}
			pos++
		}
	}

	private fun skipProcessingInstruction() {
		pos += 2 // "<?".length
		while (true) {
			if (!ensure(2)) {
				pos = bufLength
				return
			}
			if (buf[pos] == '?' && buf[pos + 1] == '>') {
				pos += 2
				return
			}
			pos++
		}
	}

	private fun readCData(dst: StringBuilder) {
		pos += 9 // "<![CDATA[".length
		while (true) {
			if (ensure(3) && buf[pos] == ']' && buf[pos + 1] == ']' && buf[pos + 2] == '>') {
				pos += 3
				return
			}
			if (!ensure(1)) return
			dst.append(buf[pos++])
		}
	}

	// Balances nested '<'/'>' - used for <!DOCTYPE ...> which may contain an internal
	// subset ("[ ... ]") with its own '<!ENTITY ...>' declarations. Angle brackets inside a
	// quoted literal (<!ENTITY x "a > b">) or inside a comment do not count towards the
	// balance, so the skip ends on the '>' that actually closes the declaration.
	private fun skipBalancedMarkup() {
		var depth = 0
		var quote = NO_QUOTE
		while (true) {
			if (!ensure(1)) return
			if (quote == NO_QUOTE && startsWithAt("<!--")) {
				skipComment()
				continue
			}
			val c = buf[pos]
			when {
				quote != NO_QUOTE -> if (c == quote) quote = NO_QUOTE
				c == '"' || c == '\'' -> quote = c
				c == '<' -> depth++
				c == '>' -> {
					depth--
					if (depth <= 0) {
						pos++
						return
					}
				}
			}
			pos++
		}
	}

	private fun readTextRun(dst: StringBuilder) {
		while (true) {
			var i = pos
			while (i < bufLength) {
				val c = buf[i]
				if (c == '<' || c == '&') break
				i++
			}
			if (i > pos) {
				dst.appendRange(buf, pos, i)
				pos = i
			}
			if (pos < bufLength) {
				if (buf[pos] == '<') return
				appendEntity(dst)
			} else if (!ensure(1)) {
				return
			}
		}
	}

	private fun readStartTag(): Int {
		pos++ // consume '<'
		val rawName = scanName()
		val attrs = mutableListOf<Pair<String, String>>()
		var selfClosing = false
		while (true) {
			skipWhitespace()
			if (!ensure(1)) {
				throw XmlParserException("Unexpected end of document inside tag <$rawName>")
			}
			val c = buf[pos]
			if (c == '/' && ensure(2) && buf[pos + 1] == '>') {
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
			if (!ensure(1) || buf[pos] != '=') {
				throw XmlParserException("Expected '=' after attribute name '$attrName' in <$rawName>")
			}
			pos++ // consume '='
			skipWhitespace()
			if (!ensure(1) || (buf[pos] != '"' && buf[pos] != '\'')) {
				throw XmlParserException("Expected a quoted value for attribute '$attrName' in <$rawName>")
			}
			val quote = buf[pos]
			pos++
			val attrValue = readAttributeValue(quote, attrName, rawName)
			// With namespace processing on, xmlns declarations are not part of the attribute
			// list - same as Android's actual and as the previous QXmlStreamReader backend.
			if (!processNamespaces || !isNamespaceDeclaration(attrName)) {
				attrs.add(attrName to attrValue)
			}
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

	private fun readAttributeValue(quote: Char, attrName: String, tagName: String): String {
		val sb = StringBuilder()
		while (true) {
			var i = pos
			while (i < bufLength) {
				val c = buf[i]
				if (c == quote || c == '&' || c == '\n' || c == '\t') break
				i++
			}
			if (i > pos) {
				sb.appendRange(buf, pos, i)
				pos = i
			}
			if (pos < bufLength) {
				val c = buf[pos]
				if (c == quote) {
					pos++
					return sb.toString()
				}
				// Attribute-value normalisation: literal tabs and line breaks become spaces,
				// while the same characters written as references (&#10;) are kept verbatim.
				if (c == '\n' || c == '\t') {
					sb.append(' ')
					pos++
				} else {
					appendEntity(sb)
				}
			} else if (!ensure(1)) {
				throw XmlParserException("Unterminated value for attribute '$attrName' in <$tagName>")
			}
		}
	}

	private fun readEndTag(): Int {
		pos += 2 // consume '</'
		val rawName = scanName()
		skipWhitespace()
		if (!ensure(1) || buf[pos] != '>') {
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

	// buf[pos] is '&'. An entity reference longer than MAX_ENTITY_LENGTH is not one - the '&'
	// is then kept as literal text, exactly as an unknown/custom entity is.
	private fun appendEntity(dst: StringBuilder) {
		ensure(MAX_ENTITY_LENGTH)
		val available = minOf(bufLength - pos, MAX_ENTITY_LENGTH)
		var semi = -1
		var i = 1
		while (i < available) {
			if (buf[pos + i] == ';') {
				semi = i
				break
			}
			i++
		}
		if (semi > 1) {
			val resolved = resolveEntity(buf.concatToString(pos + 1, pos + semi))
			if (resolved != null) {
				dst.append(resolved)
				pos += semi + 1
				return
			}
		}
		dst.append('&')
		pos++
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
