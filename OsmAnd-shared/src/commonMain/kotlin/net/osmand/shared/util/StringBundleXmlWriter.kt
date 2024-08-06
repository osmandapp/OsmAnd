package net.osmand.shared.util

import net.osmand.shared.xml.XmlSerializer
import net.osmand.shared.util.StringBundle.Item
import net.osmand.shared.util.StringBundle.StringItem
import net.osmand.shared.util.StringBundle.StringListItem
import net.osmand.shared.util.StringBundle.StringMapItem
import net.osmand.shared.util.StringBundle.ItemType

class StringBundleXmlWriter(bundle: StringBundle, private val serializer: XmlSerializer?) : StringBundleWriter(bundle) {

	companion object {
		private val log = LoggerFactory.getLogger("StringBundleXmlWriter")
	}

	override fun writeItem(name: String, item: Item<*>) {
		if (serializer != null) {
			try {
				writeItemImpl(name, item)
			} catch (e: Exception) {
				log.error("Error writing string bundle as xml", e)
			}
		}
	}

	override fun writeBundle() {
		if (serializer != null) {
			super.writeBundle()
			try {
				serializer.flush()
			} catch (e: Exception) {
				log.error("Error writing string bundle as xml", e)
			}
		}
	}

	private fun writeItemImpl(name: String, item: Item<*>) {
		if (serializer != null) {
			when (item.type) {
				ItemType.STRING -> {
					val stringItem = item as StringItem
					if (stringItem.value != null) {
						serializer.attribute(null, name, stringItem.value)
					}
				}
				ItemType.LIST -> {
					val listItem = item as StringListItem
					serializer.startTag(null, name)
					val list = listItem.value
					if (list != null) {
						for (i in list) {
							if (i.type == ItemType.STRING) {
								writeItemImpl(i.name, i)
							}
						}
					}
					if (list != null) {
						for (i in list) {
							if (i.type != ItemType.STRING) {
								writeItemImpl(i.name, i)
							}
						}
					}
					serializer.endTag(null, name)
				}
				ItemType.MAP -> {
					val mapItem = item as StringMapItem
					serializer.startTag(null, name)
					mapItem.value?.forEach {(key, value) ->
						if (value.type == ItemType.STRING) {
							writeItemImpl(key, value)
						}
					}
					mapItem.value?.forEach {(key, value) ->
						if (value.type != ItemType.STRING) {
							writeItemImpl(key, value)
						}
					}
					serializer.endTag(null, name)
				}
			}
		}
	}
}
