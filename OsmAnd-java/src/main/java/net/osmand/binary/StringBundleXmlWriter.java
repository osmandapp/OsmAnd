package net.osmand.binary;

import net.osmand.PlatformUtil;
import net.osmand.binary.StringBundle.Item;
import net.osmand.binary.StringBundle.StringItem;
import net.osmand.binary.StringBundle.StringListItem;
import net.osmand.binary.StringBundle.StringMapItem;

import org.apache.commons.logging.Log;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.util.List;
import java.util.Map.Entry;

public class StringBundleXmlWriter extends StringBundleWriter {

	public final static Log log = PlatformUtil.getLog(StringBundleXmlWriter.class);

	private XmlSerializer serializer;

	public StringBundleXmlWriter(StringBundle bundle, XmlSerializer serializer) {
		super(bundle);
		this.serializer = serializer;
	}

	@Override
	protected void writeItem(String name, Item<?> item) {
		if (serializer != null) {
			try {
				writeItemImpl(name, item);
			} catch (Exception e) {
				log.error("Error writing string bundle as xml", e);
			}
		}
	}

	@Override
	public void writeBundle() {
		if (serializer != null) {
			super.writeBundle();
			try {
				serializer.flush();
			} catch (Exception e) {
				log.error("Error writing string bundle as xml", e);
			}
		}
	}

	private void writeItemImpl(String name, Item<?> item) throws IOException {
		if (serializer != null && item != null) {
			switch (item.getType()) {
				case STRING: {
					StringItem stringItem = (StringItem) item;
					serializer.attribute(null, name, stringItem.getValue());
					break;
				}
				case LIST: {
					StringListItem listItem = (StringListItem) item;
					serializer.startTag(null, name);
					List<Item<?>> list = listItem.getValue();
					for (Item<?> i : list) {
						if (i.getType() == StringBundle.ItemType.STRING) {
							writeItemImpl(i.getName(), i);
						}
					}
					for (Item<?> i : list) {
						if (i.getType() != StringBundle.ItemType.STRING) {
							writeItemImpl(i.getName(), i);
						}
					}
					serializer.endTag(null, name);
					break;
				}
				case MAP: {
					StringMapItem mapItem = (StringMapItem) item;
					serializer.startTag(null, name);
					for (Entry<String, Item<?>> entry : mapItem.getValue().entrySet()) {
						Item<?> i = entry.getValue();
						if (i.getType() == StringBundle.ItemType.STRING) {
							writeItemImpl(entry.getKey(), i);
						}
					}
					for (Entry<String, Item<?>> entry : mapItem.getValue().entrySet()) {
						Item<?> i = entry.getValue();
						if (i.getType() != StringBundle.ItemType.STRING) {
							writeItemImpl(entry.getKey(), i);
						}
					}
					serializer.endTag(null, name);
					break;
				}
			}
		}
	}
}
