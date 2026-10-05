package net.osmand.shared.xml

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.StableRef
import kotlinx.cinterop.asStableRef
import kotlinx.cinterop.set
import kotlinx.cinterop.pointed
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.staticCFunction
import kotlinx.cinterop.toKString
import libxml2.xmlError
import libxml2.xmlFreeTextReader
import libxml2.xmlInitParser
import libxml2.xmlReaderForFile
import libxml2.xmlReaderForIO
import libxml2.xmlTextReaderAttributeCount
import libxml2.xmlTextReaderConstEncoding
import libxml2.xmlTextReaderConstLocalName
import libxml2.xmlTextReaderConstName
import libxml2.xmlTextReaderConstNamespaceUri
import libxml2.xmlTextReaderConstPrefix
import libxml2.xmlTextReaderConstValue
import libxml2.xmlTextReaderDepth
import libxml2.xmlTextReaderGetParserColumnNumber
import libxml2.xmlTextReaderGetParserLineNumber
import libxml2.xmlTextReaderIsEmptyElement
import libxml2.xmlTextReaderIsNamespaceDecl
import libxml2.xmlTextReaderMoveToAttributeNo
import libxml2.xmlTextReaderMoveToElement
import libxml2.xmlTextReaderNodeType
import libxml2.xmlTextReaderPtr
import libxml2.xmlTextReaderRead
import libxml2.xmlTextReaderSetStructuredErrorHandler
import net.osmand.shared.io.KFile
import okio.BufferedSource
import okio.IOException
import okio.Source
import okio.buffer

// iOS actual backed by libxml2's xmlTextReader.
//
// The previous implementation forwarded every call (next, getName, getAttributeValue, ...) to
// an Obj-C object wrapping Qt's QXmlStreamReader, so each token cost an objc_msgSend plus, for
// every string, two copies (C string -> NSString -> Kotlin String) and Kotlin/Native runtime
// bookkeeping for the bridged object.
//
// xmlTextReader is libxml2's pull API - the same shape as org.xmlpull.v1.XmlPullParser - so
// the mapping below is close to one to one, and because it is plain C the interop calls are
// direct: no Obj-C dispatch, no NSString. libxml2 also streams the input and does its own
// encoding detection (BOM plus the <?xml encoding?> declaration, legacy code pages through
// iconv), so the resident cost of a parse is the reader's own buffers rather than the file.
//
// libxml2 ships with the OS and the iOS app already links it, so this adds no dependency to
// the product.
@OptIn(ExperimentalForeignApi::class)
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

		// xmlReaderTypes, libxml/xmlreader.h
		private const val NODE_ELEMENT = 1
		private const val NODE_TEXT = 3
		private const val NODE_CDATA = 4
		private const val NODE_ENTITY_REFERENCE = 5
		private const val NODE_WHITESPACE = 13
		private const val NODE_SIGNIFICANT_WHITESPACE = 14
		private const val NODE_END_ELEMENT = 15

		// xmlParserOption, libxml/parser.h. Entities are substituted and CDATA is merged into
		// text so that next() sees the coalesced text the xmlpull contract promises. DTDLOAD
		// stays off and NONET is set, so a GPX/KML from an untrusted import cannot pull in an
		// external entity.
		private const val XML_PARSE_NOENT = 2
		private const val XML_PARSE_NOWARNING = 64
		private const val XML_PARSE_NONET = 2048
		private const val XML_PARSE_NOCDATA = 16384

		private const val PARSE_OPTIONS =
			XML_PARSE_NOENT or XML_PARSE_NOCDATA or XML_PARSE_NONET or XML_PARSE_NOWARNING

		private const val IO_BUFFER_SIZE = 8 * 1024

		// The reader pulls bytes through this callback; okio does the actual file/stream IO.
		// staticCFunction cannot capture, so the source travels in libxml2's context pointer.
		private val readCallback = staticCFunction<COpaquePointer?, CPointer<ByteVar>?, Int, Int> {
				context, buffer, length ->
			if (context == null || buffer == null || length <= 0) {
				0
			} else {
				val holder = context.asStableRef<SourceHolder>().get()
				try {
					val wanted = minOf(length, holder.scratch.size)
					val read = holder.source.read(holder.scratch, 0, wanted)
					if (read <= 0) {
						0
					} else {
						for (i in 0 until read) {
							buffer[i] = holder.scratch[i]
						}
						read
					}
				} catch (_: Throwable) {
					-1
				}
			}
		}

		private val closeCallback = staticCFunction<COpaquePointer?, Int> { _ ->
			0 // the source is owned by the caller (ImportGpx.parseKmlStreaming never closes it)
		}

		private val errorCallback = staticCFunction<COpaquePointer?, CPointer<xmlError>?, Unit> {
				userData, error ->
			if (userData != null && error != null) {
				val sink = userData.asStableRef<ErrorSink>().get()
				val message = error.pointed.message?.toKString()?.trim()
				if (!message.isNullOrEmpty()) {
					sink.message = message
					sink.line = error.pointed.line
				}
			}
		}
	}

	private class SourceHolder(val source: BufferedSource) {
		val scratch = ByteArray(IO_BUFFER_SIZE)
	}

	private class ErrorSink {
		var message: String? = null
		var line: Int = 0
	}

	private var reader: xmlTextReaderPtr? = null
	private var sourceRef: StableRef<SourceHolder>? = null
	private var errorSink: ErrorSink? = null
	private var errorRef: StableRef<ErrorSink>? = null

	private var processNamespaces: Boolean = true // android.util.Xml.newPullParser() enables it

	private var currentTokenType: Int = START_DOCUMENT
	private var currentText: String? = null
	private var currentDepth: Int = 0

	// libxml2 reports an empty element as a single node and never emits its END_ELEMENT, so the
	// closing event is synthesised. The reader stays parked on the element while it is pending,
	// which keeps getName()/getDepth() correct for the synthetic event.
	private var pendingEndTag: Boolean = false

	// Text is coalesced by reading ahead: when a non-text node ends a text run the reader is
	// already parked on it, and the following next() adopts that node instead of advancing.
	private var adoptCurrentNode: Boolean = false

	private var attributes: List<Attribute>? = null

	private class Attribute(val name: String, val prefix: String?, val namespace: String?, val value: String)

	@Throws(XmlParserException::class)
	actual fun setFeature(name: String, state: Boolean) {
		if (name == FEATURE_PROCESS_NAMESPACES) {
			processNamespaces = state
		}
	}

	actual fun getFeature(name: String): Boolean =
		if (name == FEATURE_PROCESS_NAMESPACES) processNamespaces else false

	@Throws(XmlParserException::class)
	actual fun setProperty(name: String, value: Any?) {
		// Not implemented - no commonMain caller sets a property.
	}

	actual fun getProperty(name: String): Any? = null

	// inputEncoding is deliberately not forwarded: both commonMain callers pass "UTF-8" as a
	// hint, while the Qt-backed implementation this replaces always auto-detected. Passing null
	// keeps that behaviour - libxml2 honours a BOM or an <?xml encoding?> declaration and falls
	// back to UTF-8 when there is neither, which is what the hint asked for anyway.
	@Throws(XmlParserException::class)
	actual fun setInput(file: KFile, inputEncoding: String?) {
		close()
		xmlInitParser() // idempotent; libxml2 guards the one-time setup itself
		val sink = ErrorSink()
		val sinkRef = StableRef.create(sink)
		val created = xmlReaderForFile(file.absolutePath(), null, PARSE_OPTIONS)
		if (created == null) {
			sinkRef.dispose()
			throw XmlParserException("Cannot open ${file.absolutePath()} for parsing")
		}
		errorSink = sink
		errorRef = sinkRef
		reader = created
		xmlTextReaderSetStructuredErrorHandler(created, errorCallback, sinkRef.asCPointer())
		resetState()
	}

	@Throws(XmlParserException::class)
	actual fun setInput(input: Source, inputEncoding: String?) {
		close()
		xmlInitParser() // idempotent; libxml2 guards the one-time setup itself
		val holderRef = StableRef.create(SourceHolder(input.buffer()))
		val sink = ErrorSink()
		val sinkRef = StableRef.create(sink)
		val created = xmlReaderForIO(
			readCallback,
			closeCallback,
			holderRef.asCPointer(),
			null,
			null,
			PARSE_OPTIONS
		)
		if (created == null) {
			holderRef.dispose()
			sinkRef.dispose()
			throw XmlParserException("Cannot start parsing the given source")
		}
		sourceRef = holderRef
		errorSink = sink
		errorRef = sinkRef
		reader = created
		xmlTextReaderSetStructuredErrorHandler(created, errorCallback, sinkRef.asCPointer())
		resetState()
	}

	@Throws(IOException::class)
	actual fun close() {
		reader?.let { xmlFreeTextReader(it) }
		reader = null
		sourceRef?.dispose()
		sourceRef = null
		errorRef?.dispose()
		errorRef = null
		errorSink = null
		resetState()
	}

	private fun resetState() {
		currentTokenType = START_DOCUMENT
		currentText = null
		currentDepth = 0
		pendingEndTag = false
		adoptCurrentNode = false
		attributes = null
	}

	actual fun getInputEncoding(): String? =
		reader?.let { xmlTextReaderConstEncoding(it)?.reinterpret<ByteVar>()?.toKString() }

	@Throws(XmlParserException::class)
	actual fun defineEntityReplacementText(entityName: String, replacementText: String) {
		// Not implemented - only the predefined entities and character references are used.
	}

	@Throws(XmlParserException::class)
	actual fun getNamespaceCount(depth: Int): Int = -1 // Not implemented - no commonMain caller

	@Throws(XmlParserException::class)
	actual fun getNamespacePrefix(pos: Int): String? = null // Not implemented

	@Throws(XmlParserException::class)
	actual fun getNamespaceUri(pos: Int): String? = null // Not implemented

	actual fun getNamespace(prefix: String?): String? = null // Not implemented

	actual fun getDepth(): Int = currentDepth

	actual fun getPositionDescription(): String? = null // Not implemented

	actual fun getLineNumber(): Int =
		reader?.let { xmlTextReaderGetParserLineNumber(it) } ?: 0

	actual fun getColumnNumber(): Int =
		reader?.let { xmlTextReaderGetParserColumnNumber(it) } ?: 0

	@Throws(XmlParserException::class)
	actual fun isWhitespace(): Boolean {
		val value = currentText ?: return false
		return value.isNotEmpty() && value.all { it == ' ' || it == '\t' || it == '\n' || it == '\r' }
	}

	actual fun getText(): String? = currentText

	actual fun getTextCharacters(holderForStartAndLength: IntArray): CharArray? = null // Not implemented

	actual fun getNamespace(): String? = onElement { constString(xmlTextReaderConstNamespaceUri(it)) }

	actual fun getName(): String? = onElement {
		if (processNamespaces) constString(xmlTextReaderConstLocalName(it))
		else constString(xmlTextReaderConstName(it))
	}

	actual fun getPrefix(): String? = onElement { constString(xmlTextReaderConstPrefix(it)) }

	@Throws(XmlParserException::class)
	actual fun isEmptyElementTag(): Boolean = currentTokenType == START_TAG && pendingEndTag

	actual fun getAttributeCount(): Int = loadedAttributes()?.size ?: 0

	actual fun getAttributeNamespace(index: Int): String? = loadedAttributes()?.getOrNull(index)?.namespace

	actual fun getAttributeName(index: Int): String? = loadedAttributes()?.getOrNull(index)?.name

	actual fun getAttributePrefix(index: Int): String? = loadedAttributes()?.getOrNull(index)?.prefix

	actual fun getAttributeType(index: Int): String? = null // Not implemented

	actual fun isAttributeDefault(index: Int): Boolean = false // Not implemented

	actual fun getAttributeValue(index: Int): String? = loadedAttributes()?.getOrNull(index)?.value

	actual fun getAttributeValue(namespace: String?, name: String?): String? {
		if (name == null) return null
		val loaded = loadedAttributes() ?: return null
		for (attribute in loaded) {
			if (attribute.name == name) return attribute.value
		}
		return null
	}

	@Throws(XmlParserException::class)
	actual fun getEventType(): Int = currentTokenType

	@Throws(XmlParserException::class, IOException::class)
	actual fun next(): Int {
		if (pendingEndTag) {
			// The reader is still parked on the empty element, so name and depth stay valid.
			pendingEndTag = false
			currentText = null
			attributes = null
			currentTokenType = END_TAG
			return currentTokenType
		}

		val active = reader ?: run {
			currentTokenType = END_DOCUMENT
			currentText = null
			return currentTokenType
		}

		val textBuilder = StringBuilder()
		var sawText = false
		var textDepth = 0

		while (true) {
			if (adoptCurrentNode) {
				adoptCurrentNode = false
			} else if (!advance(active)) {
				return if (sawText) emitText(textBuilder, textDepth) else emitEndDocument()
			}

			when (xmlTextReaderNodeType(active)) {
				NODE_TEXT, NODE_CDATA, NODE_WHITESPACE, NODE_SIGNIFICANT_WHITESPACE -> {
					if (!sawText) {
						sawText = true
						textDepth = xmlTextReaderDepth(active) + 1
					}
					constString(xmlTextReaderConstValue(active))?.let { textBuilder.append(it) }
				}

				NODE_ELEMENT -> {
					if (sawText) {
						adoptCurrentNode = true
						return emitText(textBuilder, textDepth)
					}
					attributes = null
					currentText = null
					currentDepth = xmlTextReaderDepth(active) + 1
					pendingEndTag = xmlTextReaderIsEmptyElement(active) == 1
					currentTokenType = START_TAG
					return currentTokenType
				}

				NODE_END_ELEMENT -> {
					if (sawText) {
						adoptCurrentNode = true
						return emitText(textBuilder, textDepth)
					}
					attributes = null
					currentText = null
					currentDepth = xmlTextReaderDepth(active) + 1
					currentTokenType = END_TAG
					return currentTokenType
				}

				NODE_ENTITY_REFERENCE -> {
					// Entities are substituted (XML_PARSE_NOENT); an unresolved one carries no
					// text, so there is nothing to append.
				}

				else -> {
					// Comments, processing instructions, the DOCTYPE and the XML declaration are
					// not part of the next() contract.
				}
			}
		}
	}

	private fun emitText(builder: StringBuilder, depth: Int): Int {
		attributes = null
		currentText = builder.toString()
		currentDepth = depth
		currentTokenType = TEXT
		return currentTokenType
	}

	private fun emitEndDocument(): Int {
		attributes = null
		currentText = null
		currentDepth = 0
		currentTokenType = END_DOCUMENT
		return currentTokenType
	}

	@Throws(XmlParserException::class)
	private fun advance(active: xmlTextReaderPtr): Boolean {
		return when (xmlTextReaderRead(active)) {
			1 -> true
			0 -> false
			else -> throw XmlParserException(errorMessage())
		}
	}

	private fun errorMessage(): String {
		val sink = errorSink
		val message = sink?.message
		return when {
			message == null -> "XML parsing failed"
			sink.line > 0 -> "$message (line ${sink.line})"
			else -> message
		}
	}

	@Throws(XmlParserException::class, IOException::class)
	actual fun nextToken(): Int = next() // token-level events are not surfaced by this backend

	@Throws(XmlParserException::class, IOException::class)
	actual fun require(type: Int, namespace: String?, name: String?) {
		if (type != currentTokenType || (name != null && name != getName())) {
			throw XmlParserException("Expected event $type${name?.let { " <$it>" } ?: ""}, got $currentTokenType")
		}
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
	actual fun nextTag(): Int {
		var type = next()
		if (type == TEXT && isWhitespace()) {
			type = next()
		}
		if (type != START_TAG && type != END_TAG) {
			throw XmlParserException("nextTag(): expected a start or end tag, got $type")
		}
		return type
	}

	// The reader is parked on the current element for START_TAG and END_TAG (including the
	// synthetic one), so these can be read straight off it. For every other event the xmlpull
	// contract has no name, which is also what the reader would be pointing at after a
	// read-ahead, so the guard doubles as correctness.
	private inline fun onElement(block: (xmlTextReaderPtr) -> String?): String? {
		if (currentTokenType != START_TAG && currentTokenType != END_TAG) return null
		val active = reader ?: return null
		return block(active)
	}

	private fun loadedAttributes(): List<Attribute>? {
		if (currentTokenType != START_TAG) return null
		attributes?.let { return it }
		val active = reader ?: return null
		val count = xmlTextReaderAttributeCount(active)
		if (count <= 0) {
			val empty = emptyList<Attribute>()
			attributes = empty
			return empty
		}
		val loaded = ArrayList<Attribute>(count)
		for (index in 0 until count) {
			if (xmlTextReaderMoveToAttributeNo(active, index) != 1) continue
			// With namespace processing on, xmlns declarations bind a prefix instead of being
			// reported as attributes - the same as android.util.Xml's parser.
			if (processNamespaces && xmlTextReaderIsNamespaceDecl(active) == 1) continue
			val rawName = if (processNamespaces) constString(xmlTextReaderConstLocalName(active))
			else constString(xmlTextReaderConstName(active))
			val name = rawName ?: continue
			loaded.add(
				Attribute(
					name = name,
					prefix = constString(xmlTextReaderConstPrefix(active)),
					namespace = constString(xmlTextReaderConstNamespaceUri(active)),
					value = constString(xmlTextReaderConstValue(active)) ?: ""
				)
			)
		}
		xmlTextReaderMoveToElement(active)
		attributes = loaded
		return loaded
	}

	// Const* accessors hand back a pointer owned by the reader and valid only until the next
	// read, so the value is copied into a Kotlin string immediately. Nothing here needs freeing.
	private fun constString(pointer: CPointer<*>?): String? =
		pointer?.reinterpret<ByteVar>()?.toKString()
}
