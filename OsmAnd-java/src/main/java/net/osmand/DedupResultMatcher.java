package net.osmand;

/**
 * Easy matcher to be able to publish results immediately
 * 
 */
public interface DedupResultMatcher<T> extends ResultMatcher<T> {

	/**
	 * @returns true if object is skipped because it is a duplicate
	 */
	boolean isSkippedDuplication();

}
