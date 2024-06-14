package net.osmand.shared.xml

import net.osmand.shared.extensions.toNSData
import net.osmand.shared.io.KFile
import net.osmand.shared.util.PlatformUtil
import okio.IOException
import okio.Source
import okio.buffer

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

	private val xmlPullParserAPI = PlatformUtil.getXmlPullParserApi()

	@Throws(XmlParserException::class)
	actual fun setFeature(name: String, state: Boolean) {
		xmlPullParserAPI.setFeature(name, state)
	}

	actual fun getFeature(name: String): Boolean {
		return xmlPullParserAPI.getFeature(name)
	}

	@Throws(XmlParserException::class)
	actual fun setProperty(name: String, value: Any?) {
		xmlPullParserAPI.setProperty(name, value)
	}

	actual fun getProperty(name: String): Any? {
		return xmlPullParserAPI.getProperty(name)
	}

	@Throws(XmlParserException::class)
	actual fun setInput(file: KFile, inputEncoding: String?) {
		xmlPullParserAPI.setInput(file.absolutePath(), inputEncoding)
	}

	@Throws(IOException::class)
	actual fun close() {
		xmlPullParserAPI.close()
	}

	@Throws(XmlParserException::class)
	actual fun setInput(input: Source, inputEncoding: String?) {
		val inputBuffer = input.buffer()
		val byteArray = inputBuffer.readByteArray()
		xmlPullParserAPI.setInput(byteArray.toNSData(), inputEncoding)
	}

	actual fun getInputEncoding(): String? {
		return xmlPullParserAPI.getInputEncoding()
	}

	@Throws(XmlParserException::class)
	actual fun defineEntityReplacementText(entityName: String, replacementText: String) {
		xmlPullParserAPI.defineEntityReplacementText(entityName, replacementText)
	}

	@Throws(XmlParserException::class)
	actual fun getNamespaceCount(depth: Int): Int {
		return xmlPullParserAPI.getNamespaceCount(depth)
	}

	@Throws(XmlParserException::class)
	actual fun getNamespacePrefix(pos: Int): String? {
		return xmlPullParserAPI.getNamespacePrefix(pos)
	}

	@Throws(XmlParserException::class)
	actual fun getNamespaceUri(pos: Int): String? {
		return xmlPullParserAPI.getNamespaceUri(pos)
	}

	actual fun getNamespace(prefix: String?): String? {
		return xmlPullParserAPI.getNamespace(prefix)
	}

	actual fun getDepth(): Int {
		return xmlPullParserAPI.getDepth()
	}

	actual fun getPositionDescription(): String? {
		return xmlPullParserAPI.getPositionDescription()
	}

	actual fun getLineNumber(): Int {
		return xmlPullParserAPI.getLineNumber()
	}

	actual fun getColumnNumber(): Int {
		return xmlPullParserAPI.getColumnNumber()
	}

	@Throws(XmlParserException::class)
	actual fun isWhitespace(): Boolean {
		return xmlPullParserAPI.isWhitespace()
	}

	actual fun getText(): String? {
		return xmlPullParserAPI.getText()
	}

	actual fun getTextCharacters(holderForStartAndLength: IntArray): CharArray? {
		return xmlPullParserAPI.getTextCharacters(holderForStartAndLength)
	}

	actual fun getNamespace(): String? {
		return xmlPullParserAPI.getNamespace()
	}

	actual fun getName(): String? {
		return xmlPullParserAPI.getName()
	}

	actual fun getPrefix(): String? {
		return xmlPullParserAPI.getPrefix()
	}

	@Throws(XmlParserException::class)
	actual fun isEmptyElementTag(): Boolean {
		return xmlPullParserAPI.isEmptyElementTag()
	}

	actual fun getAttributeCount(): Int {
		return xmlPullParserAPI.getAttributeCount()
	}

	actual fun getAttributeNamespace(index: Int): String? {
		return xmlPullParserAPI.getAttributeNamespace(index)
	}

	actual fun getAttributeName(index: Int): String? {
		return xmlPullParserAPI.getAttributeName(index)
	}

	actual fun getAttributePrefix(index: Int): String? {
		return xmlPullParserAPI.getAttributePrefix(index)
	}

	actual fun getAttributeType(index: Int): String? {
		return xmlPullParserAPI.getAttributeType(index)
	}

	actual fun isAttributeDefault(index: Int): Boolean {
		return xmlPullParserAPI.isAttributeDefault(index)
	}

	actual fun getAttributeValue(index: Int): String? {
		return xmlPullParserAPI.getAttributeValue(index)
	}

	actual fun getAttributeValue(namespace: String?, name: String?): String? {
		return xmlPullParserAPI.getAttributeValue(namespace, name)
	}

	@Throws(XmlParserException::class)
	actual fun getEventType(): Int {
		val res = xmlPullParserAPI.getEventType()
		if (res == -1) {
			throw XmlParserException("Event type unresolved")
		}
		return res;
	}

	@Throws(XmlParserException::class, IOException::class)
	actual fun next(): Int {
		val res = xmlPullParserAPI.next()
		if (xmlPullParserAPI.hasError()) {
			throw XmlParserException(xmlPullParserAPI.getErrorString())
		}
		return res
	}

	@Throws(XmlParserException::class, IOException::class)
	actual fun nextToken(): Int {
		return xmlPullParserAPI.nextToken()
	}

	@Throws(XmlParserException::class, IOException::class)
	actual fun require(type: Int, namespace: String?, name: String?) {
		return xmlPullParserAPI.require(type, namespace, name)
	}

	@Throws(XmlParserException::class, IOException::class)
	actual fun nextText(): String {
		return xmlPullParserAPI.nextText()
	}

	@Throws(XmlParserException::class, IOException::class)
	actual fun nextTag(): Int {
		return xmlPullParserAPI.nextTag()
	}
}