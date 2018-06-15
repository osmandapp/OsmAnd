package net.osmand;

/**
 * Wrapper of java.text. Collator  
 */
public interface Collator extends java.util.Comparator<Object>, Cloneable {
		
	public boolean equals(String source, String target);
	
	public abstract int compare(String source, String target);
}