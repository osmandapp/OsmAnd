package net.osmand.shared.gpx.helper

import net.osmand.shared.io.SourceInputStream
import net.osmand.shared.util.LoggerFactory
import java.io.StringWriter
import javax.xml.transform.Source
import javax.xml.transform.TransformerException
import javax.xml.transform.TransformerFactory
import javax.xml.transform.TransformerFactoryConfigurationError
import javax.xml.transform.stream.StreamResult
import javax.xml.transform.stream.StreamSource

actual object Kml2Gpx {
	private val LOG = LoggerFactory.getLogger(Kml2Gpx::class.simpleName!!)

	actual fun toGpx(kml: okio.Source): String? {
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
}
