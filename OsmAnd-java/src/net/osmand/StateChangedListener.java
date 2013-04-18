package net.osmand;

/**
 * Abstract listener represents state changed for a particular object 
 */
public interface StateChangedListener<T> {
	
	void stateChanged(T change);

}
