package net.osmand.plus.backup;

public class UserNotRegisteredException extends Exception {
	private static final long serialVersionUID = -8005954380280822845L;

	public UserNotRegisteredException() {
		super("User is not registered");
	}
}
