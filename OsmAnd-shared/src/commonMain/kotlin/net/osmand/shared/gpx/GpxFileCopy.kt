package net.osmand.shared.gpx

import net.osmand.shared.io.KFile
import net.osmand.shared.io.exclusiveSink
import net.osmand.shared.xml.XmlPullParser
import okio.Buffer
import okio.BufferedSink
import okio.FileSystem
import okio.IOException
import okio.SYSTEM
import okio.Sink
import okio.buffer
import okio.use

/** Copies a GPX without round-tripping its contents through the GPX model. */
class GpxFileCopy internal constructor(
	private val fileSystem: FileSystem,
	private val createDestination: (KFile) -> Sink
) {
	constructor() : this(FileSystem.SYSTEM, KFile::exclusiveSink)

	/**
	 * Copies a UTF-8 GPX 1.1 document, updating metadata/name and the non-empty name of its
	 * sole track. Multiple tracks keep their individual names. Missing metadata/name is added.
	 * All bytes outside these edits (including unknown XML, comments, BOM and line endings)
	 * are preserved. This deliberately does not use GpxUtilities.writeGpxFile.
	 *
	 * The caller chooses the final filename and whether this policy applies to the creator.
	 * Run off the UI thread: the source XML is held in memory. Malformed XML, DTDs, other
	 * encodings and ambiguous name elements fail before creating the destination.
	 * The parent must exist. Exclusive creation refuses any existing destination, including
	 * symbolic links and the source itself, without a check-then-open race.
	 *
	 * Register/show the copy only after success. Write/close failures remove the incomplete
	 * destination; cleanup failures are attached to the original exception. A process crash
	 * can leave a partial copy, but never truncates the source or an existing destination.
	 * The caller owns the new path during this synchronous operation: other code must not
	 * delete or replace it before completion. There is no cancellation or crash-durability API.
	 */
	@Throws(IOException::class)
	fun copyWithUpdatedNames(source: KFile, destination: KFile, newName: String) {
		val edits = try {
			val xml = fileSystem.read(source.path) { readByteArray() }
				.decodeToString(throwOnInvalidSequence = true)
			GpxNameEdits(xml).also { it.prepare(newName) }
		} catch (e: Exception) {
			throw IOException("Cannot update GPX names: ${e.message}", e)
		}
		// Do not delete anything if exclusive creation fails: we do not own that file.
		val sink = createDestination(destination)
		try {
			sink.buffer().use { edits.writeTo(it) }
		} catch (failure: Throwable) {
			try {
				fileSystem.delete(destination.path)
			} catch (cleanup: Throwable) {
				failure.addSuppressed(cleanup)
			}
			throw failure
		}
	}
}

/** The shared parser supplies XML structure; the scanner supplies lossless source ranges. */
private class GpxNameEdits(private val xml: String) {

	private enum class Role { ROOT, METADATA, TRACK, NAME, OTHER }
	private class Element(val tag: Tag, val role: Role) {
		var end = tag.start
		var name: Element? = null
		var hasElements = false
		val text = StringBuilder()
	}
	private data class Edit(val start: Int, val end: Int, val replacement: String)
	private data class Tag(val start: Int, val end: Int, val qualifiedName: String, val closing: Boolean, val empty: Boolean)

	private val edits = mutableListOf<Edit>()

	fun prepare(newName: String) {
		val escaped = escapeName(newName)
		// Preflight BEFORE invoking the platform parser, whose entity handling differs by target.
		// A DTD must never be expanded or resolved while locating edits in the original text.
		val preflight = Scanner(xml)
		while (preflight.nextTag() != null) { /* Validate lexical boundaries and reject DTDs. */ }

		val scanner = Scanner(xml)
		val parser = XmlPullParser()
		val stack = mutableListOf<Element>()
		var root: Element? = null
		var namespace = ""
		var metadata: Element? = null
		var firstTrack: Element? = null
		var trackCount = 0
		val input = Buffer().writeUtf8(xml)
		try {
			parser.setInput(input, "UTF-8")
			while (true) {
				when (parser.next()) {
					XmlPullParser.START_TAG -> {
						val tag = scanner.nextTag() ?: error("Missing start tag")
						check(!tag.closing && tag.empty == parser.isEmptyElementTag()) { "XML token mismatch" }
						val localName = parser.getName()
						val prefix = parser.getPrefix().orEmpty()
						check(tag.qualifiedName == if (prefix.isEmpty()) localName else "$prefix:$localName") {
							"XML name mismatch"
						}
						val parent = stack.lastOrNull()
						val elementNamespace = parser.getNamespace().orEmpty()
						val role = if (parent == null) {
							check(root == null && localName == "gpx") { "Expected a single GPX root" }
							namespace = elementNamespace
							check(namespace.isEmpty() || namespace == "http://www.topografix.com/GPX/1/1") {
								"Only GPX 1.1 is supported"
							}
							for (i in 0 until parser.getAttributeCount()) {
								if (parser.getAttributeName(i) == "version" && parser.getAttributeNamespace(i).isNullOrEmpty()) {
									check(parser.getAttributeValue(i) == "1.1") { "Only GPX 1.1 is supported" }
								}
							}
							Role.ROOT
						} else if (elementNamespace == namespace) {
							when {
								parent.role == Role.ROOT && localName == "metadata" -> Role.METADATA
								parent.role == Role.ROOT && localName == "trk" -> Role.TRACK
								(parent.role == Role.METADATA || parent.role == Role.TRACK) && localName == "name" -> Role.NAME
								else -> Role.OTHER
							}
						} else Role.OTHER
						val element = Element(tag, role)
						parent?.hasElements = true
						when (role) {
							Role.ROOT -> root = element
							Role.METADATA -> {
								check(metadata == null) { "Multiple metadata elements" }
								metadata = element
							}
							Role.TRACK -> {
								if (trackCount++ == 0) firstTrack = element
							}
							Role.NAME -> {
								check(parent!!.name == null) { "Multiple name elements" }
								parent.name = element
							}
							else -> Unit
						}
						stack.add(element)
					}
					XmlPullParser.END_TAG -> {
						val element = stack.removeAt(stack.lastIndex)
						if (!element.tag.empty) {
							val endTag = scanner.nextTag() ?: error("Missing end tag")
							check(endTag.closing && endTag.qualifiedName == element.tag.qualifiedName) { "XML end tag mismatch" }
							element.end = endTag.start
						}
					}
					XmlPullParser.TEXT -> stack.lastOrNull()?.let {
						if (it.role == Role.NAME) it.text.append(parser.getText().orEmpty())
					}
					XmlPullParser.END_DOCUMENT -> break
				}
			}
			check(stack.isEmpty() && scanner.nextTag() == null) { "Incomplete XML document" }
		} finally {
			parser.close()
			input.close()
		}
		val gpx = root ?: error("Missing GPX root")
		check(metadata?.name?.hasElements != true && (trackCount != 1 || firstTrack?.name?.hasElements != true)) {
			"A name contains child elements"
		}
		if (metadata == null) {
			val prefix = gpx.tag.qualifiedName.substringBefore(':', "").let { if (it.isEmpty()) "" else "$it:" }
			insertChild(gpx, "<${prefix}metadata><${prefix}name>$escaped</${prefix}name></${prefix}metadata>")
		} else {
			val name = metadata.name
			if (name == null) {
				val prefix = metadata.tag.qualifiedName.substringBefore(':', "").let { if (it.isEmpty()) "" else "$it:" }
				insertChild(metadata, "<${prefix}name>$escaped</${prefix}name>")
			} else if (name.text.toString() != newName) {
				replaceName(name, escaped)
			}
		}
		if (trackCount == 1) {
			firstTrack?.name?.let {
				if (it.text.isNotEmpty() && it.text.toString() != newName) replaceName(it, escaped)
			}
		}
		edits.sortBy { it.start }
		var end = 0
		for (edit in edits) {
			check(edit.start >= end && edit.end >= edit.start && edit.end <= xml.length) { "Invalid name edits" }
			end = edit.end
		}
	}

	fun writeTo(sink: BufferedSink) {
		var offset = 0
		for (edit in edits) {
			sink.writeUtf8(xml, offset, edit.start)
			sink.writeUtf8(edit.replacement)
			offset = edit.end
		}
		sink.writeUtf8(xml, offset, xml.length)
	}

	private fun insertChild(parent: Element, child: String) {
		if (parent.tag.empty) {
			edits.add(Edit(parent.tag.end - 2, parent.tag.end, ">$child</${parent.tag.qualifiedName}>"))
		} else {
			edits.add(Edit(parent.tag.end, parent.tag.end, child))
		}
	}

	private fun replaceName(name: Element, escaped: String) {
		check(!name.hasElements) { "A name contains child elements" }
		if (name.tag.empty) {
			insertChild(name, escaped)
			return
		}
		var offset = name.tag.end
		var inserted = false
		// Keep comments and processing instructions even inside a name. Only character data
		// (including CDATA/entity spellings) is replaced; the original start/end tags stay intact.
		while (offset < name.end) {
			val start = offset
			when {
				xml.startsWith("<!--", offset) -> offset = xml.indexOf("-->", offset + 4) + 3
				xml.startsWith("<?", offset) -> offset = xml.indexOf("?>", offset + 2) + 2
				else -> {
					offset = if (xml.startsWith("<![CDATA[", offset)) {
						xml.indexOf("]]>", offset + 9) + 3
					} else xml.indexOf('<', offset).let { if (it < 0) name.end else minOf(it, name.end) }
					check(offset > start && offset <= name.end) { "Unsupported name content" }
					edits.add(Edit(start, offset, if (inserted) "" else escaped))
					inserted = true
				}
			}
		}
		if (!inserted) edits.add(Edit(name.tag.end, name.tag.end, escaped))
	}

	private fun escapeName(name: String): String {
		// Strict encoding rejects unpaired surrogates. The remaining disallowed XML 1.0
		// characters are controls other than tab/LF/CR, and U+FFFE/U+FFFF.
		name.encodeToByteArray(throwOnInvalidSequence = true)
		check(name.all { it >= ' ' && it != '\uFFFE' && it != '\uFFFF' || it == '\t' || it == '\n' || it == '\r' }) {
			"Invalid XML character in track name"
		}
		return name.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\r", "&#13;")
	}

	/** Finds markup boundaries, never parses XML semantics or derives offsets from line numbers. */
	private class Scanner(private val xml: String) {
		private var offset = 0

		fun nextTag(): Tag? {
			while (true) {
				val start = xml.indexOf('<', offset)
				if (start < 0) return null
				when {
					xml.startsWith("<!--", start) -> skip(start + 4, "-->")
					xml.startsWith("<![CDATA[", start) -> skip(start + 9, "]]>")
					xml.startsWith("<?", start) -> {
						skip(start + 2, "?>")
						if (xml.startsWith("<?xml", start) && xml.getOrNull(start + 5)?.isWhitespace() == true) {
							val declaration = xml.substring(start, offset)
							val encoding = Regex("encoding\\s*=\\s*(['\"])([^'\"]*)\\1").find(declaration)?.groupValues?.get(2)
							check(encoding == null || encoding.equals("UTF-8", ignoreCase = true)) { "Only UTF-8 XML is supported" }
							check(!Regex("version\\s*=\\s*['\"]1\\.1['\"]").containsMatchIn(declaration)) { "Only XML 1.0 is supported" }
						}
					}
					xml.startsWith("<!", start) -> error("DTD/declarations are not supported")
					else -> {
						var quote: Char? = null
						var end = start + 1
						while (end < xml.length) {
							val c = xml[end++]
							if (quote != null) {
								if (c == quote) quote = null
							} else if (c == '\'' || c == '"') quote = c
							else if (c == '>') break
						}
						check(end <= xml.length && xml[end - 1] == '>' && quote == null) { "Unterminated XML tag" }
						val closing = xml.getOrNull(start + 1) == '/'
						val nameStart = start + if (closing) 2 else 1
						var nameEnd = nameStart
						while (nameEnd < end && !xml[nameEnd].isWhitespace() && xml[nameEnd] != '/' && xml[nameEnd] != '>') nameEnd++
						offset = end
						return Tag(start, end, xml.substring(nameStart, nameEnd), closing, xml[end - 2] == '/')
					}
				}
			}
		}

		private fun skip(start: Int, delimiter: String) {
			val end = xml.indexOf(delimiter, start)
			check(end >= 0) { "Unterminated XML markup" }
			offset = end + delimiter.length
		}
	}
}
