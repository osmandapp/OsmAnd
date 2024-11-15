package net.osmand.shared.xml

import android.util.Xml
import net.osmand.shared.io.KFile
import net.osmand.shared.io.SourceInputStream
import okio.IOException
import okio.Source
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.File
import java.io.FileInputStream
import java.io.InputStream

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
	}

	private val parser: XmlPullParser = Xml.newPullParser()
	private var inputStream: InputStream? = null

	@Throws(XmlParserException::class)
	actual fun setFeature(name: String, state: Boolean) = try {
		parser.setFeature(name, state)
	} catch (e: XmlPullParserException) {
		throw XmlParserException(e.message, e)
	}

	actual fun getFeature(name: String): Boolean = parser.getFeature(name)

	@Throws(XmlParserException::class)
	actual fun setProperty(name: String, value: Any?) = try {
		parser.setProperty(name, value)
	} catch (e: XmlPullParserException) {
		throw XmlParserException(e.message, e)
	}

	actual fun getProperty(name: String): Any? = parser.getProperty(name)

	@Throws(XmlParserException::class)
	actual fun setInput(file: KFile, inputEncoding: String?) {
		inputStream = FileInputStream(File(file.absolutePath()))
		parser.setInput(inputStream, inputEncoding)
	}

	@Throws(IOException::class)
	actual fun close() {
		inputStream?.close()
	}

	@Throws(XmlParserException::class)
	actual fun setInput(input: Source, inputEncoding: String?) {
		val inputStream = SourceInputStream(input)
		parser.setInput(inputStream, inputEncoding)
	}

	actual fun getInputEncoding(): String? = parser.inputEncoding

	@Throws(XmlParserException::class)
	actual fun defineEntityReplacementText(entityName: String, replacementText: String) = try {
		parser.defineEntityReplacementText(entityName, replacementText)
	} catch (e: XmlPullParserException) {
		throw XmlParserException(e.message, e)
	}

	@Throws(XmlParserException::class)
	actual fun getNamespaceCount(depth: Int): Int = try {
		parser.getNamespaceCount(depth)
	} catch (e: XmlPullParserException) {
		throw XmlParserException(e.message, e)
	}

	@Throws(XmlParserException::class)
	actual fun getNamespacePrefix(pos: Int): String? = try {
		parser.getNamespacePrefix(pos)
	} catch (e: XmlPullParserException) {
		throw XmlParserException(e.message, e)
	}

	@Throws(XmlParserException::class)
	actual fun getNamespaceUri(pos: Int): String? = try {
		parser.getNamespaceUri(pos)
	} catch (e: XmlPullParserException) {
		throw XmlParserException(e.message, e)
	}

	actual fun getNamespace(prefix: String?): String? = parser.namespace

	actual fun getDepth(): Int = parser.depth

	actual fun getPositionDescription(): String? = parser.positionDescription

	actual fun getLineNumber(): Int = parser.lineNumber

	actual fun getColumnNumber(): Int = parser.columnNumber

	@Throws(XmlParserException::class)
	actual fun isWhitespace(): Boolean = try {
		parser.isWhitespace
	} catch (e: XmlPullParserException) {
		throw XmlParserException(e.message, e)
	}

	actual fun getText(): String? = parser.text

	actual fun getTextCharacters(holderForStartAndLength: IntArray): CharArray? =
		parser.getTextCharacters(holderForStartAndLength)

	actual fun getNamespace(): String? = parser.namespace

	actual fun getName(): String? = parser.name

	actual fun getPrefix(): String? = parser.prefix

	@Throws(XmlParserException::class)
	actual fun isEmptyElementTag(): Boolean = try {
		parser.isEmptyElementTag
	} catch (e: XmlPullParserException) {
		throw XmlParserException(e.message, e)
	}

	actual fun getAttributeCount(): Int = parser.attributeCount

	actual fun getAttributeNamespace(index: Int): String? = parser.getAttributeNamespace(index)

	actual fun getAttributeName(index: Int): String? = parser.getAttributeName(index)

	actual fun getAttributePrefix(index: Int): String? = parser.getAttributePrefix(index)

	actual fun getAttributeType(index: Int): String? = parser.getAttributeType(index)

	actual fun isAttributeDefault(index: Int): Boolean = parser.isAttributeDefault(index)

	actual fun getAttributeValue(index: Int): String? = parser.getAttributeValue(index)

	actual fun getAttributeValue(namespace: String?, name: String?): String? =
		parser.getAttributeValue(namespace, name)

	@Throws(XmlParserException::class)
	actual fun getEventType(): Int = try {
		parser.eventType
	} catch (e: XmlPullParserException) {
		throw XmlParserException(e.message, e)
	}

	@Throws(XmlParserException::class, IOException::class)
	actual fun next(): Int = try {
		parser.next()
	} catch (e: XmlPullParserException) {
		throw XmlParserException(e.message, e)
	}

	@Throws(XmlParserException::class, IOException::class)
	actual fun nextToken(): Int = try {
		parser.nextToken()
	} catch (e: XmlPullParserException) {
		throw XmlParserException(e.message, e)
	}

	@Throws(XmlParserException::class, IOException::class)
	actual fun require(type: Int, namespace: String?, name: String?) = try {
		parser.require(type, namespace, name)
	} catch (e: XmlPullParserException) {
		throw XmlParserException(e.message, e)
	}

	@Throws(XmlParserException::class, IOException::class)
	actual fun nextText(): String = try {
		parser.nextText()
	} catch (e: XmlPullParserException) {
		throw XmlParserException(e.message, e)
	}

	@Throws(XmlParserException::class, IOException::class)
	actual fun nextTag(): Int = try {
		parser.nextTag()
	} catch (e: XmlPullParserException) {
		throw XmlParserException(e.message, e)
	}
}