package net.osmand.data.preparation;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility to keep references between binary blocks
 * while generating binary file 
 */
public class BinaryFileReferences<T> {
	
	private Map<T, Long> positionOfBlocks = new HashMap<T, Long>(); 
	private Map<T, Long> positionToWriteReference = new HashMap<T, Long>();
	
	
	public void registerReference(T key, long referenceFixed32, long blockStart){
		positionOfBlocks.put(key, blockStart);
		positionToWriteReference.put(key, referenceFixed32);
	}
	
	
	public static BinaryFileReference createBinaryFileReference(){
		
	}
	
	public static class BinaryFileReference {
		
		
		
	}

}
