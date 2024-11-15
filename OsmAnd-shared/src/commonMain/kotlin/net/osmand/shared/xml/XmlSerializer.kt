package net.osmand.shared.xml

import net.osmand.shared.io.KFile
import okio.IOException
import okio.Sink

expect class  XmlSerializer() {

	@Throws(IllegalArgumentException::class, IllegalStateException::class)
	fun setFeature(name: String, state: Boolean)

	@Throws(IllegalArgumentException::class)
	fun getFeature(name: String): Boolean

	@Throws(IllegalArgumentException::class, IllegalStateException::class)
	fun setProperty(name: String, value: Any?)

	@Throws(IllegalArgumentException::class)
	fun getProperty(name: String): Any?

	@Throws(IOException::class, IllegalArgumentException::class, IllegalStateException::class)
	fun setOutput(file: KFile)

	@Throws(IOException::class, IllegalArgumentException::class, IllegalStateException::class)
	fun setOutput(output: Sink)

	@Throws(IOException::class, IllegalArgumentException::class, IllegalStateException::class)
	fun startDocument(encoding: String?, standalone: Boolean?)

	@Throws(IOException::class, IllegalArgumentException::class, IllegalStateException::class)
	fun endDocument()

	@Throws(IOException::class, IllegalArgumentException::class, IllegalStateException::class)
	fun setPrefix(prefix: String, namespace: String)

	@Throws(IllegalArgumentException::class)
	fun getPrefix(namespace: String, generatePrefix: Boolean): String?

	fun getDepth(): Int

	fun getNamespace(): String?

	fun getName(): String?

	@Throws(IOException::class, IllegalArgumentException::class, IllegalStateException::class)
	fun startTag(namespace: String?, name: String): XmlSerializer

	@Throws(IOException::class, IllegalArgumentException::class, IllegalStateException::class)
	fun attribute(namespace: String?, name: String, value: String): XmlSerializer

	@Throws(IOException::class, IllegalArgumentException::class, IllegalStateException::class)
	fun endTag(namespace: String?, name: String): XmlSerializer

	@Throws(IOException::class, IllegalArgumentException::class, IllegalStateException::class)
	fun text(text: String): XmlSerializer

	@Throws(IOException::class, IllegalArgumentException::class, IllegalStateException::class)
	fun text(buf: CharArray, start: Int, len: Int): XmlSerializer

	@Throws(IOException::class, IllegalArgumentException::class, IllegalStateException::class)
	fun cdsect(text: String)

	@Throws(IOException::class, IllegalArgumentException::class, IllegalStateException::class)
	fun entityRef(text: String)

	@Throws(IOException::class, IllegalArgumentException::class, IllegalStateException::class)
	fun processingInstruction(text: String)

	@Throws(IOException::class, IllegalArgumentException::class, IllegalStateException::class)
	fun comment(text: String)

	@Throws(IOException::class, IllegalArgumentException::class, IllegalStateException::class)
	fun docdecl(text: String)

	@Throws(IOException::class, IllegalArgumentException::class, IllegalStateException::class)
	fun ignorableWhitespace(text: String)

	@Throws(IOException::class)
	fun flush()

	@Throws(IOException::class)
	fun close()
}
