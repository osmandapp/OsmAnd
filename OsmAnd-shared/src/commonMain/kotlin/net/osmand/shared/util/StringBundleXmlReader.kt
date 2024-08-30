package net.osmand.shared.util

import net.osmand.shared.xml.XmlPullParser

class StringBundleXmlReader(private val parser: XmlPullParser) : StringBundleReader() {

	companion object {
		private val log = LoggerFactory.getLogger("StringBundleXmlReader")
	}

	override fun readBundle() {
		val bundle = getBundle()
		for (i in 0 until parser.getAttributeCount()) {
			val name = parser.getAttributeName(i)
			val value = parser.getAttributeValue(i)
			bundle.putString(name!!, value)
		}
	}
}
