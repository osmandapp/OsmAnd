package net.osmand;

/**
 * Easy matcher to be able to publish results immediately
 * 
 */
public interface ResultMatcher<T> {

	/**
	 * @param name
	 * @return true if result should be added to final list
	 */
	boolean publish(T object);
	
	/**
	 * @returns true to stop processing
	 */
	boolean isCancelled();

}
