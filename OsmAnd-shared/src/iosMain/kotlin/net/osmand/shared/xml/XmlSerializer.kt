package net.osmand.shared.xml

import net.osmand.shared.io.CommonFile
import okio.IOException
import okio.Sink

actual class XmlSerializer actual constructor() {
	@Throws(IllegalArgumentException::class, IllegalStateException::class)
	actual fun setFeature(name: String, state: Boolean) {
	}

	@Throws(IllegalArgumentException::class)
	actual fun getFeature(name: String): Boolean {
		TODO("Not yet implemented")
	}

	@Throws(IllegalArgumentException::class, IllegalStateException::class)
	actual fun setProperty(name: String, value: Any?) {
	}

	@Throws(IllegalArgumentException::class)
	actual fun getProperty(name: String): Any? {
		TODO("Not yet implemented")
	}

	@Throws(IOException::class, IllegalArgumentException::class, IllegalStateException::class)
	actual fun setOutput(file: CommonFile) {
	}

	@Throws(IOException::class, IllegalArgumentException::class, IllegalStateException::class)
	actual fun setOutput(output: Sink) {
	}

	@Throws(IOException::class, IllegalArgumentException::class, IllegalStateException::class)
	actual fun startDocument(encoding: String?, standalone: Boolean?) {
	}

	@Throws(IOException::class, IllegalArgumentException::class, IllegalStateException::class)
	actual fun endDocument() {
	}

	@Throws(IOException::class, IllegalArgumentException::class, IllegalStateException::class)
	actual fun setPrefix(prefix: String, namespace: String) {
	}

	@Throws(IllegalArgumentException::class)
	actual fun getPrefix(namespace: String, generatePrefix: Boolean): String? {
		TODO("Not yet implemented")
	}

	actual fun getDepth(): Int {
		TODO("Not yet implemented")
	}

	actual fun getNamespace(): String? {
		TODO("Not yet implemented")
	}

	actual fun getName(): String? {
		TODO("Not yet implemented")
	}

	@Throws(IOException::class, IllegalArgumentException::class, IllegalStateException::class)
	actual fun startTag(
		namespace: String?,
		name: String
	): XmlSerializer {
		TODO("Not yet implemented")
	}

	@Throws(IOException::class, IllegalArgumentException::class, IllegalStateException::class)
	actual fun attribute(
		namespace: String?,
		name: String,
		value: String
	): XmlSerializer {
		TODO("Not yet implemented")
	}

	@Throws(IOException::class, IllegalArgumentException::class, IllegalStateException::class)
	actual fun endTag(
		namespace: String?,
		name: String
	): XmlSerializer {
		TODO("Not yet implemented")
	}

	@Throws(IOException::class, IllegalArgumentException::class, IllegalStateException::class)
	actual fun text(text: String): XmlSerializer {
		TODO("Not yet implemented")
	}

	@Throws(IOException::class, IllegalArgumentException::class, IllegalStateException::class)
	actual fun text(
		buf: CharArray,
		start: Int,
		len: Int
	): XmlSerializer {
		TODO("Not yet implemented")
	}

	@Throws(IOException::class, IllegalArgumentException::class, IllegalStateException::class)
	actual fun cdsect(text: String) {
	}

	@Throws(IOException::class, IllegalArgumentException::class, IllegalStateException::class)
	actual fun entityRef(text: String) {
	}

	@Throws(IOException::class, IllegalArgumentException::class, IllegalStateException::class)
	actual fun processingInstruction(text: String) {
	}

	@Throws(IOException::class, IllegalArgumentException::class, IllegalStateException::class)
	actual fun comment(text: String) {
	}

	@Throws(IOException::class, IllegalArgumentException::class, IllegalStateException::class)
	actual fun docdecl(text: String) {
	}

	@Throws(IOException::class, IllegalArgumentException::class, IllegalStateException::class)
	actual fun ignorableWhitespace(text: String) {
	}

	@Throws(IOException::class)
	actual fun flush() {
	}
}