package net.osmand.binary;

import net.osmand.binary.StringBundle.Item;

import java.util.Map.Entry;

public abstract class StringBundleWriter {

	private StringBundle bundle;

	public StringBundleWriter(StringBundle bundle) {
		this.bundle = bundle;
	}

	public StringBundle getBundle() {
		return bundle;
	}

	protected abstract void writeItem(String name, Item<?> item);

	public void writeBundle() {
		for (Entry<String, Item<?>> entry : bundle.getMap().entrySet()) {
			writeItem("osmand:" + entry.getKey(), entry.getValue());
		}
	}
}
