package net.osmand.plus.base.dialog.interfaces.other;

import androidx.annotation.NonNull;

public interface IDialogStateListener {
	default void onDialogRegistered(@NonNull String processId) {
	}
	default void onDialogUnregistered(@NonNull String processId) {
	}
}
