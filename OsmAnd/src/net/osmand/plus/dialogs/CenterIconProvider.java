package net.osmand.plus.dialogs;

import androidx.annotation.Nullable;

import net.osmand.plus.views.PointImageDrawable;

public interface CenterIconProvider {
	@Nullable
	PointImageDrawable getCenterPointIcon();
}
