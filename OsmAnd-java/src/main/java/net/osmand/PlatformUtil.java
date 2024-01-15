package net.osmand;



import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

/**
 * That class is replacing of standard LogFactory due to 
 * problems with Android implementation of LogFactory.
 * See Android analog of  LogUtil
 *
 * That class should be very simple & always use LogFactory methods,
 * there is an intention to delegate all static methods to LogFactory.
 */
public class PlatformUtil {
	
	public static Log getLog(Class<?> cl){
		return LogFactory.getLog(cl);
	}
	
	public static XmlPullParser newXMLPullParser() throws XmlPullParserException{
		org.kxml2.io.KXmlParser xmlParser = new org.kxml2.io.KXmlParser();
		xmlParser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
		return xmlParser;
	}

	public static XmlSerializer newSerializer() {
		return new org.kxml2.io.KXmlSerializer();
	}

}
