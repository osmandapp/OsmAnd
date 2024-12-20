package net.osmand.plus.voice;


/**
 * Exception on CommandPlayer
 * 
 * @author Pavol Zibrita <pavol.zibrita@gmail.com>
 */
public class CommandPlayerException extends Exception {

	private static final long serialVersionUID = 8413902962574061832L;
	private final String error;

	public CommandPlayerException(String error) {
		this.error = error;
	}

	public String getError() {
		return error;
	}

}
