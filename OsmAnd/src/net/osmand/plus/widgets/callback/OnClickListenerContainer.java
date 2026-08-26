package net.osmand.plus.widgets.callback;

import android.view.View;

import androidx.annotation.Nullable;

public interface OnClickListenerContainer {

	@Nullable
	View.OnClickListener getOnClickListener();
}