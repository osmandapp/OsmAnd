package net.osmand.shared.gpx.helper

import net.osmand.shared.IndexConstants
import net.osmand.shared.gpx.GpxFile
import net.osmand.shared.gpx.GpxUtilities.loadGpxFile
import net.osmand.shared.io.SourceInputStream
import net.osmand.shared.util.LoggerFactory
import okio.buffer
import okio.source
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.io.StringWriter
import java.io.UnsupportedEncodingException
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import javax.xml.transform.Source
import javax.xml.transform.TransformerException
import javax.xml.transform.TransformerFactory
import javax.xml.transform.TransformerFactoryConfigurationError
import javax.xml.transform.stream.StreamResult
import javax.xml.transform.stream.StreamSource

actual object ImportGpx {

	private val LOG = LoggerFactory.getLogger(ImportGpx::class.simpleName!!)

	private fun toGpx(kml: okio.Source): String? {
		try {
			val xmlSource: Source = StreamSource(SourceInputStream(kml))
			val xsltSource: Source = StreamSource(this::class.java.getResourceAsStream("/kml2gpx.xslt"))

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
	actual fun loadGpx(source: okio.Source, fileName: String): Pair<GpxFile, Long>? {
		val inputStream: InputStream = source.buffer().inputStream()
		val zis: ZipInputStream
		var gpxInfo: Pair<GpxFile, Long>? = null
		if (fileName.endsWith(IndexConstants.KML_SUFFIX)) {
			gpxInfo = loadGPXFileFromKml(inputStream)
		} else if (fileName.endsWith(IndexConstants.KMZ_SUFFIX)) {
			zis = ZipInputStream(inputStream)
			gpxInfo = loadGPXFileFromKmz(zis)
			zis.close()
		} else if (fileName.endsWith(IndexConstants.ZIP_EXT)) {
			zis = ZipInputStream(inputStream)
			gpxInfo = loadGPXFileFromZip(zis)
			zis.close()
		}
		return gpxInfo
	}

	private fun kml2Gpx(inputStream: InputStream): String? {
		return ImportGpx.toGpx(inputStream.source())
	}

	@Throws(IOException::class)
	private fun loadGPXFileFromKml(stream: InputStream): Pair<GpxFile, Long>? {
		val gpxStream: InputStream? = convertKmlToGpxStream(stream)
		if (gpxStream != null) {
			val fileSize = gpxStream.available().toLong()
			return Pair(loadGpxFile(gpxStream.source()), fileSize)
		}
		return null
	}

	private fun convertKmlToGpxStream(`is`: InputStream): InputStream? {
		val result = kml2Gpx(`is`)
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
	private fun loadGPXFileFromKmz(stream: ZipInputStream): Pair<GpxFile, Long>? {
		var entry: ZipEntry
		while ((stream.nextEntry.also { entry = it }) != null) {
			if (entry.name.endsWith(IndexConstants.KML_SUFFIX)) {
				return loadGPXFileFromKml(stream)
			}
		}
		return null
	}

	@Throws(IOException::class)
	private fun loadGPXFileFromZip(stream: ZipInputStream): Pair<GpxFile, Long>? {
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
