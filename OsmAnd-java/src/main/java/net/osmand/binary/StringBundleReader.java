package net.osmand.binary;

public abstract class StringBundleReader {

	private StringBundle bundle = new StringBundle();

	public StringBundle getBundle() {
		return bundle;
	}

	public abstract void readBundle();
}
