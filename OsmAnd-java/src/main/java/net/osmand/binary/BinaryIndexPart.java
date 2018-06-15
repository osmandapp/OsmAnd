package net.osmand.binary;

public abstract class BinaryIndexPart {

	String name;
	int length;
	int filePointer;
	
	abstract public String getPartName();

	abstract public int getFieldNumber();
	
	public int getLength() {
		return length;
	}
	
	public void setLength(int length) {
		this.length = length;
	}
	
	public int getFilePointer() {
		return filePointer;
	}
	
	public void setFilePointer(int filePointer) {
		this.filePointer = filePointer;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
}
