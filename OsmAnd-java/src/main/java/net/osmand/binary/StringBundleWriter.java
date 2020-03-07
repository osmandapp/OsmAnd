package net.osmand.binary;

import net.osmand.binary.StringBundle.Item;

import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;

public abstract class StringBundleWriter {

	private StringBundle bundle;
	protected String result;

	public StringBundleWriter(StringBundle bundle) {
		this.bundle = bundle;
	}

	protected StringBundle getBundle() {
		return bundle;
	}

	public String getResult() {
		return result;
	}

	protected abstract void writeItem(String name, Item item);

	public void writeBundle() {
		for (Entry<String, Item> entry : bundle.getMap().entrySet()) {
			writeItem(entry.getKey(), entry.getValue());
		}
	}
}
