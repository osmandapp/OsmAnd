package net.osmand.shared.xml

import net.osmand.shared.io.KFile
import net.osmand.shared.util.KAlgorithms
import net.osmand.shared.util.PlatformUtil
import okio.IOException
import okio.Sink

actual class XmlSerializer actual constructor() {

	private val xmlSerializerApi = PlatformUtil.getXmlSerializerApi()

	@Throws(IllegalArgumentException::class, IllegalStateException::class)
	actual fun setFeature(name: String, state: Boolean) {
		xmlSerializerApi.setFeature(name, state)
	}

	@Throws(IllegalArgumentException::class)
	actual fun getFeature(name: String): Boolean {
		return 	xmlSerializerApi.getFeature(name)
	}

	@Throws(IllegalArgumentException::class, IllegalStateException::class)
	actual fun setProperty(name: String, value: Any?) {
		xmlSerializerApi.setProperty(name, value)
	}

	@Throws(IllegalArgumentException::class)
	actual fun getProperty(name: String): Any? {
		return xmlSerializerApi.getProperty(name)
	}

	@Throws(IOException::class, IllegalArgumentException::class, IllegalStateException::class)
	actual fun setOutput(file: KFile) {
		file.getParentFile()?.createDirectories()
		xmlSerializerApi.setOutput(file.absolutePath())
	}

	@Throws(IOException::class, IllegalArgumentException::class, IllegalStateException::class)
	actual fun setOutput(output: Sink) {
		xmlSerializerApi.setOutput(SinkOutputStream(output))
	}

	@Throws(IOException::class, IllegalArgumentException::class, IllegalStateException::class)
	actual fun startDocument(encoding: String?, standalone: Boolean?) {
		xmlSerializerApi.startDocument(encoding, standalone)
	}

	@Throws(IOException::class, IllegalArgumentException::class, IllegalStateException::class)
	actual fun endDocument() {
		xmlSerializerApi.endDocument()
	}

	@Throws(IOException::class, IllegalArgumentException::class, IllegalStateException::class)
	actual fun setPrefix(prefix: String, namespace: String) {
		xmlSerializerApi.setPrefix(prefix, namespace)
	}

	@Throws(IllegalArgumentException::class)
	actual fun getPrefix(namespace: String, generatePrefix: Boolean): String? {
		return xmlSerializerApi.getPrefix(namespace, generatePrefix)
	}

	actual fun getDepth(): Int {
		return xmlSerializerApi.getDepth()
	}

	actual fun getNamespace(): String? {
		return xmlSerializerApi.getNamespace()
	}

	actual fun getName(): String? {
		return xmlSerializerApi.getName()
	}

	@Throws(IOException::class, IllegalArgumentException::class, IllegalStateException::class)
	actual fun startTag(namespace: String?, name: String): XmlSerializer {
		xmlSerializerApi.startTag(namespace, name)
		return this
	}

	@Throws(IOException::class, IllegalArgumentException::class, IllegalStateException::class)
	actual fun attribute(namespace: String?, name: String, value: String): XmlSerializer {
		xmlSerializerApi.attribute(namespace, name, value)
		return this
	}

	@Throws(IOException::class, IllegalArgumentException::class, IllegalStateException::class)
	actual fun endTag(namespace: String?, name: String): XmlSerializer {
		xmlSerializerApi.endTag(namespace, name)
		return this
	}

	@Throws(IOException::class, IllegalArgumentException::class, IllegalStateException::class)
	actual fun text(text: String): XmlSerializer {
		xmlSerializerApi.text(text)
		return this
	}

	@Throws(IOException::class, IllegalArgumentException::class, IllegalStateException::class)
	actual fun text(buf: CharArray, start: Int, len: Int): XmlSerializer {
		xmlSerializerApi.text(buf, start, len)
		return this
	}

	@Throws(IOException::class, IllegalArgumentException::class, IllegalStateException::class)
	actual fun cdsect(text: String) {
		xmlSerializerApi.cdsect(text)
	}

	@Throws(IOException::class, IllegalArgumentException::class, IllegalStateException::class)
	actual fun entityRef(text: String) {
		xmlSerializerApi.entityRef(text)
	}

	@Throws(IOException::class, IllegalArgumentException::class, IllegalStateException::class)
	actual fun processingInstruction(text: String) {
		xmlSerializerApi.processingInstruction(text)
	}

	@Throws(IOException::class, IllegalArgumentException::class, IllegalStateException::class)
	actual fun comment(text: String) {
		xmlSerializerApi.comment(text)
	}

	@Throws(IOException::class, IllegalArgumentException::class, IllegalStateException::class)
	actual fun docdecl(text: String) {
		xmlSerializerApi.docdecl(text)
	}

	@Throws(IOException::class, IllegalArgumentException::class, IllegalStateException::class)
	actual fun ignorableWhitespace(text: String) {
		xmlSerializerApi.ignorableWhitespace(text)
	}

	@Throws(IOException::class)
	actual fun flush() {
		xmlSerializerApi.flush()
	}

	@Throws(IOException::class)
	actual fun close() {
		xmlSerializerApi.close()
	}
}
