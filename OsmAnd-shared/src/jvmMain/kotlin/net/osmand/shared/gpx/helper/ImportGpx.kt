package net.osmand.shared.gpx.helper

import net.osmand.shared.IndexConstants
import net.osmand.shared.gpx.GpxFile
import net.osmand.shared.gpx.GpxUtilities.loadGpxFile
import net.osmand.shared.io.SourceInputStream
import net.osmand.shared.util.LoggerFactory
import okio.Source
import okio.buffer
import okio.source
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.io.StringWriter
import java.io.UnsupportedEncodingException
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import javax.xml.transform.TransformerException
import javax.xml.transform.TransformerFactory
import javax.xml.transform.TransformerFactoryConfigurationError
import javax.xml.transform.stream.StreamResult
import javax.xml.transform.stream.StreamSource

actual object ImportGpx {
	private val LOG = LoggerFactory.getLogger(ImportGpx::class.simpleName!!)

	private fun kml2Gpx(kml: Source): String? {
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
	actual fun loadGpx(source: Source, fileName: String): Pair<GpxFile, Long>? {
		val gpxInfo: Pair<GpxFile, Long>? = when {
			fileName.endsWith(IndexConstants.KML_SUFFIX) -> loadGPXFileFromKml(source)
			fileName.endsWith(IndexConstants.KMZ_SUFFIX) -> loadGPXFileFromKmz(source)
			fileName.endsWith(IndexConstants.ZIP_EXT) -> loadGPXFileFromZip(source)
			else -> null
		}
		return gpxInfo
	}

	@Throws(IOException::class)
	fun loadGPXFileFromKml(source: Source): Pair<GpxFile, Long>? {
		val gpxStream: InputStream? = convertKmlToGpxStream(source)
		if (gpxStream != null) {
			val fileSize = gpxStream.available().toLong()
			return Pair(loadGpxFile(gpxStream.source()), fileSize)
		}
		return null
	}

	private fun convertKmlToGpxStream(source: Source): InputStream? {
		val result = kml2Gpx(source)
		if (result != null) {
			try {
				return ByteArrayInputStream(result.toByteArray(charset("UTF-8")))
			} catch (e: UnsupportedEncodingException) {
				LOG.error(e.message, e)
			}
		}
		return null
	}

	@Throws(IOException::class)
	private fun loadGPXFileFromKmz(source: Source): Pair<GpxFile, Long>? {
		val stream = ZipInputStream(SourceInputStream(source))
		var entry: ZipEntry
		while ((stream.nextEntry.also { entry = it }) != null) {
			if (entry.name.endsWith(IndexConstants.KML_SUFFIX)) {
				return loadGPXFileFromKml(stream.source())
			}
		}
		return null
	}

	@Throws(IOException::class)
	private fun loadGPXFileFromZip(source: Source): Pair<GpxFile, Long>? {
		val stream = ZipInputStream(SourceInputStream(source))
		var entry: ZipEntry
		while ((stream.nextEntry.also { entry = it }) != null) {
			if (entry.name.endsWith(IndexConstants.GPX_FILE_EXT)) {
				val fileSize = entry.size
				return Pair(loadGpxFile(stream.source()), fileSize)
			}
		}
		return null
	}
}
