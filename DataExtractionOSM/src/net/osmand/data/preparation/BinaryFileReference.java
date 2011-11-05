package net.osmand.data.preparation;


import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Utility to keep references between binary blocks
 * while generating binary file 
 */
public class BinaryFileReference {
	
	private long pointerToWrite;
	private long pointerToCalculateShiftFrom;
	private long pointerToCalculateShiftTo;
	
	public BinaryFileReference(long pointerToWrite, long pointerToCalculateShiftFrom) {
		this.pointerToWrite = pointerToWrite;
		this.pointerToCalculateShiftFrom = pointerToCalculateShiftFrom;
	}
	
	public long getStartPointer() {
		return pointerToCalculateShiftFrom;
	}
	
	public int writeReference(RandomAccessFile raf, long pointerToCalculateShifTo) throws IOException {
		this.pointerToCalculateShiftTo = pointerToCalculateShifTo;
		long currentPosition = raf.getFilePointer();
		raf.seek(pointerToWrite);
		int val = (int) (pointerToCalculateShiftTo - pointerToCalculateShiftFrom);
		raf.writeInt(val);
		raf.seek(currentPosition);
		return val;
	}
	
	public static BinaryFileReference createSizeReference(long pointerToWrite){
		return new BinaryFileReference(pointerToWrite, pointerToWrite + 4);
	}
	
	public static BinaryFileReference createShiftReference(long pointerToWrite, long pointerShiftFrom){
		return new BinaryFileReference(pointerToWrite, pointerShiftFrom);
	}
	

}
