package net.osmand;

/**
 * Easy matcher to be able to filter streets,buildings, etc.. using custom
 * rules
 * 
 * @author pavol.zibrita
 */
public interface StringMatcher {

	/**
	 * @param name
	 * @return true if this matcher matches the <code>name</code> String
	 */
	boolean matches(String name);

}
