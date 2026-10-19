package net.osmand.plus.keyevent.fragments.selectkeycode;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public interface OnKeyCodeSelectedCallback {
	void onKeyCodeSelected(@Nullable Integer oldKeyCode, @NonNull Integer newKeyCode);
}
