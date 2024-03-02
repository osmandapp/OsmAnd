package net.osmand.binary;

public abstract class BinaryIndexPart {

	String name;
	long length;
	long filePointer;
	
	abstract public String getPartName();

	abstract public int getFieldNumber();
	
	public long getLength() {
		return length;
	}
	
	public void setLength(long length) {
		this.length = length;
	}
	
	public long getFilePointer() {
		return filePointer;
	}
	
	public void setFilePointer(long filePointer) {
		this.filePointer = filePointer;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
}
