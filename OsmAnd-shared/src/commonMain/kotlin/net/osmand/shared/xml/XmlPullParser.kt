package net.osmand.shared.xml

import net.osmand.shared.io.KFile
import okio.IOException
import okio.Source

expect class XmlPullParser() {

	companion object {
		val NO_NAMESPACE: String
		val START_DOCUMENT: Int
		val END_DOCUMENT: Int
		val START_TAG: Int
		val END_TAG: Int
		val TEXT: Int
		val CDSECT: Int
		val ENTITY_REF: Int
		val IGNORABLE_WHITESPACE: Int
		val PROCESSING_INSTRUCTION: Int
		val COMMENT: Int
		val DOCDECL: Int
	}

	@Throws(XmlParserException::class)
	fun setFeature(name: String, state: Boolean)

	fun getFeature(name: String): Boolean

	@Throws(XmlParserException::class)
	fun setProperty(name: String, value: Any?)

	fun getProperty(name: String): Any?

	@Throws(XmlParserException::class)
	fun setInput(file: KFile, inputEncoding: String?)

	@Throws(XmlParserException::class)
	fun setInput(input: Source, inputEncoding: String?)

	@Throws(IOException::class)
	fun close()

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