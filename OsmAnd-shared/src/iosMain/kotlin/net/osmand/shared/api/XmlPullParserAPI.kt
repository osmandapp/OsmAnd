package net.osmand.shared.api

import net.osmand.shared.xml.XmlParserException
import okio.IOException
import platform.Foundation.NSData

interface XmlPullParserAPI {

	companion object {
		const val NO_ERROR = 0
		const val UNEXPECTED_ELEMENT_ERROR = 1
		const val CUSTOM_ERROR = 2
		const val NOT_WELL_FORMED_ERROR = 3
		const val PREMATURE_END_OF_DOCUMENT_ERROR = 4
	}

	fun hasError(): Boolean

	fun getError(): Int

	fun getErrorString(): String

	@Throws(XmlParserException::class)
	fun setFeature(name: String, state: Boolean)

	fun getFeature(name: String): Boolean

	@Throws(XmlParserException::class)
	fun setProperty(name: String, value: Any?)

	fun getProperty(name: String): Any?

	@Throws(XmlParserException::class)
	fun setInput(filePath: String, inputEncoding: String?)

	@Throws(IOException::class)
	fun close()

	@Throws(XmlParserException::class)
	fun setInput(data: NSData, inputEncoding: String?)

	fun getInputEncoding(): String?

	@Throws(XmlParserException::class)
	fun defineEntityReplacementText(entityName: String, replacementText: String)

	@Throws(XmlParserException::class)
	fun getNamespaceCount(depth: Int): Int

	@Throws(XmlParserException::class)
	fun getNamespacePrefix(pos: Int): String?

	@Throws(XmlParserException::class)
	fun getNamespaceUri(pos: Int): String?

	fun getNamespace(prefix: String?): String?

	fun getDepth(): Int

	fun getPositionDescription(): String?

	fun getLineNumber(): Int

	fun getColumnNumber(): Int

	@Throws(XmlParserException::class)
	fun isWhitespace(): Boolean

	fun getText(): String?

	fun getTextCharacters(holderForStartAndLength: IntArray): CharArray?

	fun getNamespace(): String?

	fun getName(): String?

	fun getPrefix(): String?

	@Throws(XmlParserException::class)
	fun isEmptyElementTag(): Boolean

	fun getAttributeCount(): Int

	fun getAttributeNamespace(index: Int): String?

	fun getAttributeName(index: Int): String?

	fun getAttributePrefix(index: Int): String?

	fun getAttributeType(index: Int): String?

	fun isAttributeDefault(index: Int): Boolean

	fun getAttributeValue(index: Int): String?

	fun getAttributeValue(namespace: String?, name: String?): String?

	@Throws(XmlParserException::class)
	fun getEventType(): Int

	@Throws(XmlParserException::class, IOException::class)
	fun next(): Int

	@Throws(XmlParserException::class, IOException::class)
	fun nextToken(): Int

	@Throws(XmlParserException::class, IOException::class)
	fun require(type: Int, namespace: String?, name: String?)

	@Throws(XmlParserException::class, IOException::class)
	fun nextText(): String

	@Throws(XmlParserException::class, IOException::class)
	fun nextTag(): Int

}