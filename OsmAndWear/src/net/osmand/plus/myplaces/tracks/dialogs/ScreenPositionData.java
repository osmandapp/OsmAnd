package net.osmand.plus.myplaces.tracks.dialogs;

import androidx.annotation.NonNull;

public class ScreenPositionData {

	private final Object referenceObject;
	private final int referenceItemOnScreenY;

	public ScreenPositionData(@NonNull Object referenceObject, int referenceItemOnScreenY) {
		this.referenceObject = referenceObject;
		this.referenceItemOnScreenY = referenceItemOnScreenY;
	}

	@NonNull
	public Object getReferenceObject() {
		return referenceObject;
	}

	public int getReferenceItemOnScreenY() {
		return referenceItemOnScreenY;
	}
}
