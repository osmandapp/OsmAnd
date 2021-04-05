package net.osmand.plus.settings.fragments;

import androidx.annotation.NonNull;

public interface HeaderUiAdapter {

	void onUpdateHeader(@NonNull HeaderInfo headerInfo,
	                    @NonNull String title,
	                    @NonNull String description);

}
