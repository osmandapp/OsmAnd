package net.osmand.plus;

import java.text.Collator;

/**
 * This simple contains string matcher uses collator to check,
 * if the part is contained in matched string.
 * 
 * @author pavol.zibrita
 */
public class ContainsStringMatcher extends CollatorStringMatcher {

	private final String part;

	/**
	 * @param part Search this string in matched base string, see {@link #matches(String)}
	 * @param collator Collator to use
	 */
	public ContainsStringMatcher(String part, Collator collator) {
		super(collator);
		this.part = part;
	}

	@Override
	public boolean matches(String base) {
		return ccontains(getCollator(), part, base);
	}

}
