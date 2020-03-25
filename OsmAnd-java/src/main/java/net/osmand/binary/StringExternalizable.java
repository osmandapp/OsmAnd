package net.osmand.binary;

public interface StringExternalizable<T extends StringBundle> {

	public void writeToBundle(T bundle);

	public void readFromBundle(T bundle);

}
