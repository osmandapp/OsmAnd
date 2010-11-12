package net.osmand.binary;

public class BinaryIndexPart {

	String name;
	int length;
	int filePointer;
	
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
