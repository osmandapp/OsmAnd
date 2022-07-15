package net.osmand.plus.helpers;

import android.annotation.TargetApi;
import net.osmand.PlatformUtil;
import org.apache.commons.logging.Log;

import javax.xml.transform.*;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.InputStream;
import java.io.StringWriter;

/**
 * @author Koen Rabaey
 */
public class Kml2Gpx {

	public static final Log LOG = PlatformUtil.getLog(Kml2Gpx.class);

	@TargetApi(8)
	public static String toGpx(InputStream kml) {
		try {
			Source xmlSource = new StreamSource(kml);
			Source xsltSource = new StreamSource(Kml2Gpx.class.getResourceAsStream("kml2gpx.xslt"));

			StringWriter sw = new StringWriter();

			TransformerFactory.newInstance().newTransformer(xsltSource).transform(xmlSource, new StreamResult(sw));

			return sw.toString();

		} catch (TransformerConfigurationException e) {
			LOG.error(e.toString(), e);
		} catch (TransformerFactoryConfigurationError e) {
			LOG.error(e.toString(), e);
		} catch (TransformerException e) {
			LOG.error(e.toString(), e);
		}

		return null;
	}
}
