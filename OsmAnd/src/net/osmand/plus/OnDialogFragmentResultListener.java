package net.osmand.plus;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public interface OnDialogFragmentResultListener {
	void onDialogFragmentResult(@NonNull String tag, int resultCode, @Nullable Bundle data);
}
