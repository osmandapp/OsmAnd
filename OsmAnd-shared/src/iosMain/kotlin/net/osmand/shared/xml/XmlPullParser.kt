package net.osmand.shared.xml

import okio.IOException
import okio.Source
import okio.buffer

actual class XmlPullParser actual constructor() {

	@Throws(XmlParserException::class)
	actual fun setFeature(name: String, state: Boolean) {
		TODO("Not yet implemented")
	}

	actual fun getFeature(name: String): Boolean {
		TODO("Not yet implemented")
	}

	@Throws(XmlParserException::class)
	actual fun setProperty(name: String, value: Any?) {
		TODO("Not yet implemented")
	}

	actual fun getProperty(name: String): Any? {
		TODO("Not yet implemented")
	}

	@Throws(XmlParserException::class)
	actual fun setInput(filePath: String, inputEncoding: String?) {
		/*
		inputStream = FileInputStream(File(filePath))
		parser.setInput(inputStream, inputEncoding)
		 */
	}

	@Throws(IOException::class)
	actual fun close() {
		TODO("Not yet implemented")
	}

	@Throws(XmlParserException::class)
	actual fun setInput(input: Source, inputEncoding: String?) {
		val inputBuffer = input.buffer()
		/*
		val inputStream = object : InputStream() {
			override fun read(): Int = inputBuffer.readByte().toInt()

			override fun read(b: ByteArray?): Int = b?.let { inputBuffer.read(it) } ?: -1

			override fun read(b: ByteArray?, off: Int, len: Int): Int =
				b?.let { inputBuffer.read(it, off, len) } ?: -1

			override fun skip(n: Long): Long {
				inputBuffer.skip(n)
				return n
			}

			override fun readNBytes(len: Int): ByteArray = inputBuffer.readByteArray(len.toLong())

			override fun readNBytes(b: ByteArray?, off: Int, len: Int): Int =
				b?.let { inputBuffer.read(it, off, len) } ?: -1

			override fun readAllBytes(): ByteArray = inputBuffer.readByteArray()

			override fun close() = inputBuffer.close()
		}
		parser.setInput(inputStream, inputEncoding)
		 */
	}

	actual fun getInputEncoding(): String? {
		TODO("Not yet implemented")
	}

	@Throws(XmlParserException::class)
	actual fun defineEntityReplacementText(entityName: String, replacementText: String) {
		TODO("Not yet implemented")
	}

	@Throws(XmlParserException::class)
	actual fun getNamespaceCount(depth: Int): Int {
		TODO("Not yet implemented")
	}

	@Throws(XmlParserException::class)
	actual fun getNamespacePrefix(pos: Int): String? {
		TODO("Not yet implemented")
	}

	@Throws(XmlParserException::class)
	actual fun getNamespaceUri(pos: Int): String? {
		TODO("Not yet implemented")
	}

	actual fun getNamespace(prefix: String?): String? {
		TODO("Not yet implemented")
	}

	actual fun getDepth(): Int {
		TODO("Not yet implemented")
	}

	actual fun getPositionDescription(): String? {
		TODO("Not yet implemented")
	}

	actual fun getLineNumber(): Int {
		TODO("Not yet implemented")
	}

	actual fun getColumnNumber(): Int {
		TODO("Not yet implemented")
	}

	@Throws(XmlParserException::class)
	actual fun isWhitespace(): Boolean {
		TODO("Not yet implemented")
	}

	actual fun getText(): String? {
		TODO("Not yet implemented")
	}

	actual fun getTextCharacters(holderForStartAndLength: IntArray): CharArray? {
		TODO("Not yet implemented")
	}

	actual fun getNamespace(): String? {
		TODO("Not yet implemented")
	}

	actual fun getName(): String? {
		TODO("Not yet implemented")
	}

	actual fun getPrefix(): String? {
		TODO("Not yet implemented")
	}

	@Throws(XmlParserException::class)
	actual fun isEmptyElementTag(): Boolean {
		TODO("Not yet implemented")
	}

	actual fun getAttributeCount(): Int {
		TODO("Not yet implemented")
	}

	actual fun getAttributeNamespace(index: Int): String? {
		TODO("Not yet implemented")
	}

	actual fun getAttributeName(index: Int): String? {
		TODO("Not yet implemented")
	}

	actual fun getAttributePrefix(index: Int): String? {
		TODO("Not yet implemented")
	}

	actual fun getAttributeType(index: Int): String? {
		TODO("Not yet implemented")
	}

	actual fun isAttributeDefault(index: Int): Boolean {
		TODO("Not yet implemented")
	}

	actual fun getAttributeValue(index: Int): String? {
		TODO("Not yet implemented")
	}

	actual fun getAttributeValue(namespace: String?, name: String?): String? {
		TODO("Not yet implemented")
	}

	@Throws(XmlParserException::class)
	actual fun getEventType(): Int {
		TODO("Not yet implemented")
	}

	@Throws(XmlParserException::class, IOException::class)
	actual fun next(): Int {
		TODO("Not yet implemented")
	}

	@Throws(XmlParserException::class, IOException::class)
	actual fun nextToken(): Int {
		TODO("Not yet implemented")
	}

	@Throws(XmlParserException::class, IOException::class)
	actual fun require(type: Int, namespace: String?, name: String?) {
		TODO("Not yet implemented")
	}

	@Throws(XmlParserException::class, IOException::class)
	actual fun nextText(): String {
		TODO("Not yet implemented")
	}

	@Throws(XmlParserException::class, IOException::class)
	actual fun nextTag(): Int {
		TODO("Not yet implemented")
	}
}