package net.osmand.plus;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public interface OnDialogFragmentResultListener {
	void onDialogFragmentResult(@NonNull String tag, int resultCode, @Nullable Bundle data);
}
