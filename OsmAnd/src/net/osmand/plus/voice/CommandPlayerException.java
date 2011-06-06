package net.osmand.plus.voice;

import android.content.Intent;

/**
 * Exception on CommandPlayer
 * 
 * @author Pavol Zibrita <pavol.zibrita@gmail.com>
 */
public class CommandPlayerException extends Exception {

	private static final long serialVersionUID = 8413902962574061832L;
	private final String error;
	private final Intent intent;

	public CommandPlayerException(Intent intent) {
		this(null, intent);
	}

	public CommandPlayerException(String error) {
		this(error, null);
	}

	public CommandPlayerException(String error, Intent intent) {
		this.error = error;
		this.intent = intent;
	}

	public String getError() {
		return error;
	}

	public Intent getIntent() {
		return intent;
	}

}
