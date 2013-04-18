package net.osmand;

/**
 * That class is designed to be common interface for callbacks with one param
 * @author victor
 * @param <T>
 */
public interface CallbackWithObject<T> {
	
	
	/**
	 * Calls on processing result
	 * @param result could be null depends on usage
	 * @return processed or not
	 */
	public boolean processResult(T result);

}
