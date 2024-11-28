package net.osmand.plus.download.local;

import androidx.annotation.NonNull;

public interface LocalSizeCalculationListener {
	void onSizeCalculationEvent(@NonNull LocalItem localItem);
}
