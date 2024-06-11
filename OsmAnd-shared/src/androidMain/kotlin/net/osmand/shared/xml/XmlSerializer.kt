package net.osmand.shared.xml

import net.osmand.shared.io.KFile
import net.osmand.shared.io.SinkStringWriter
import okio.IOException
import okio.Sink
import java.io.File
import java.io.OutputStreamWriter

actual class XmlSerializer actual constructor() {
	private val serializer = org.kxml2.io.KXmlSerializer()

	@Throws(IllegalArgumentException::class, IllegalStateException::class)
	actual fun setFeature(name: String, state: Boolean) = serializer.setFeature(name, state)

	@Throws(IllegalArgumentException::class)
	actual fun getFeature(name: String): Boolean = serializer.getFeature(name)

	@Throws(IllegalArgumentException::class, IllegalStateException::class)
	actual fun setProperty(name: String, value: Any?) = serializer.setProperty(name, value)

	@Throws(IllegalArgumentException::class)
	actual fun getProperty(name: String): Any? = serializer.getProperty(name)

	@Throws(IOException::class, IllegalArgumentException::class, IllegalStateException::class)
	actual fun setOutput(file: KFile) {
		val fout = File(file.absolutePath())
		var output: OutputStreamWriter? = null
		try {
			fout.parentFile?.mkdirs()
			output = OutputStreamWriter(fout.outputStream(), "UTF-8")
		} finally {
			output?.close()
		}
		serializer.setOutput(output)
	}

	@Throws(IOException::class, IllegalArgumentException::class, IllegalStateException::class)
	actual fun setOutput(output: Sink) {
		val writer = SinkStringWriter(output)
		serializer.setOutput(writer)
	}

	@Throws(IOException::class, IllegalArgumentException::class, IllegalStateException::class)
	actual fun startDocument(encoding: String?, standalone: Boolean?) =
		serializer.startDocument(encoding, standalone)

	@Throws(IOException::class, IllegalArgumentException::class, IllegalStateException::class)
	actual fun endDocument() = serializer.endDocument()

	@Throws(IOException::class, IllegalArgumentException::class, IllegalStateException::class)
	actual fun setPrefix(prefix: String, namespace: String) =
		serializer.setPrefix(prefix, namespace)

	@Throws(IllegalArgumentException::class)
	actual fun getPrefix(namespace: String, generatePrefix: Boolean): String? =
		serializer.getPrefix(namespace, generatePrefix)

	actual fun getDepth(): Int = serializer.depth

	actual fun getNamespace(): String? = serializer.namespace

	actual fun getName(): String? = serializer.name

	@Throws(IOException::class, IllegalArgumentException::class, IllegalStateException::class)
	actual fun startTag(namespace: String?, name: String): XmlSerializer {
		serializer.startTag(namespace, name)
		return this
	}

	@Throws(IOException::class, IllegalArgumentException::class, IllegalStateException::class)
	actual fun attribute(namespace: String?, name: String, value: String): XmlSerializer {
		serializer.attribute(namespace, name, value)
		return this
	}

	@Throws(IOException::class, IllegalArgumentException::class, IllegalStateException::class)
	actual fun endTag(namespace: String?, name: String): XmlSerializer {
		serializer.endTag(namespace, name)
		return this
	}

	@Throws(IOException::class, IllegalArgumentException::class, IllegalStateException::class)
	actual fun text(text: String): XmlSerializer {
		serializer.text(text)
		return this
	}

	@Throws(IOException::class, IllegalArgumentException::class, IllegalStateException::class)
	actual fun text(buf: CharArray, start: Int, len: Int): XmlSerializer {
		serializer.text(buf, start, len)
		return this
	}

	@Throws(IOException::class, IllegalArgumentException::class, IllegalStateException::class)
	actual fun cdsect(text: String) = serializer.cdsect(text)

	@Throws(IOException::class, IllegalArgumentException::class, IllegalStateException::class)
	actual fun entityRef(text: String) = serializer.entityRef(text)

	@Throws(IOException::class, IllegalArgumentException::class, IllegalStateException::class)
	actual fun processingInstruction(text: String) = serializer.processingInstruction(text)

	@Throws(IOException::class, IllegalArgumentException::class, IllegalStateException::class)
	actual fun comment(text: String) = serializer.comment(text)

	@Throws(IOException::class, IllegalArgumentException::class, IllegalStateException::class)
	actual fun docdecl(text: String) = serializer.docdecl(text)

	@Throws(IOException::class, IllegalArgumentException::class, IllegalStateException::class)
	actual fun ignorableWhitespace(text: String) = serializer.ignorableWhitespace(text)

	@Throws(IOException::class)
	actual fun flush() = serializer.flush()
}