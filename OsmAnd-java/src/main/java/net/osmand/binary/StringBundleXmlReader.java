package net.osmand.binary;

import net.osmand.PlatformUtil;

import org.apache.commons.logging.Log;
import org.xmlpull.v1.XmlPullParser;

public class StringBundleXmlReader extends StringBundleReader {

	public final static Log log = PlatformUtil.getLog(StringBundleXmlReader.class);

	private XmlPullParser parser;

	public StringBundleXmlReader(XmlPullParser parser) {
		this.parser = parser;
	}

	@Override
	public void readBundle() {
		StringBundle bundle = getBundle();
		for (int i = 0; i < parser.getAttributeCount(); i++) {
			String name = parser.getAttributeName(i);
			String value = parser.getAttributeValue(i);
			bundle.putString(name, value);
		}
	}
}
