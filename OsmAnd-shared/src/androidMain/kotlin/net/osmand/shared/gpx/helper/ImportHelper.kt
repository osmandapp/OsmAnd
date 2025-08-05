package net.osmand.shared.gpx.helper

import android.util.Log
import net.osmand.shared.IndexConstants
import net.osmand.shared.gpx.GpxFile
import net.osmand.shared.gpx.GpxUtilities.loadGpxFile
import net.osmand.shared.gpx.helper.ImportGpx.errorImport
import net.osmand.shared.gpx.helper.ImportGpx.loadGPXFileFromKml
import net.osmand.shared.io.SourceInputStream
import net.osmand.shared.util.LoggerFactory
import okio.Source
import okio.source
import java.io.File
import java.io.IOException
import java.io.StringWriter
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import javax.xml.transform.TransformerException
import javax.xml.transform.TransformerFactory
import javax.xml.transform.TransformerFactoryConfigurationError
import javax.xml.transform.stream.StreamResult
import javax.xml.transform.stream.StreamSource

actual object ImportHelper {

	val LOG = LoggerFactory.getLogger(this::class.simpleName!!)

	actual fun kml2Gpx(kml: Source): String? {
		try {
			val xmlSource: javax.xml.transform.Source = StreamSource(SourceInputStream(kml))
			val xsltSource: javax.xml.transform.Source =
				StreamSource(this::class.java.getResourceAsStream("/kml2gpx.xslt"))
			val sw = StringWriter()

			TransformerFactory.newInstance().newTransformer(xsltSource).transform(xmlSource, StreamResult(sw))

			return sw.toString()
		} catch (e: TransformerFactoryConfigurationError) {
			LOG.error(e.toString(), e)
		} catch (e: TransformerException) {
			LOG.error(e.toString(), e)
		}
		return null
	}

	@Throws(IOException::class)
	actual fun loadGPXFileFromArchive(source: Source): Pair<GpxFile, Long> {
		val stream = ZipInputStream(SourceInputStream(source))
		var entry: ZipEntry
		while ((stream.nextEntry.also { entry = it }) != null) {
			if (entry.name.endsWith(IndexConstants.GPX_FILE_EXT)) {
				val fileSize = entry.size
				return Pair(loadGpxFile(stream.source()), fileSize)
			}
			if (entry.name.endsWith(IndexConstants.KML_SUFFIX)) {
				return loadGPXFileFromKml(stream.source())
			}
		}
		return errorImport("Archive doesn't have GPX/KLM files")
	}

	actual fun readFileContent(filePath: String): String = File(filePath).readText()
	actual fun writeFileContent(filePath: String, content: String) = File(filePath).writeText(content)
	actual fun fileExists(filePath: String): Boolean = File(filePath).exists()
	actual fun printError(message: String) {
		Log.e("KmlGpxConverter", message)
	}
}