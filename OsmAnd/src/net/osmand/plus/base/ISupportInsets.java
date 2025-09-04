package net.osmand.plus.base;

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.WindowInsetsCompat;

import net.osmand.plus.utils.InsetsUtils.InsetSide;

import java.util.EnumSet;
import java.util.List;

public interface ISupportInsets {

	@Nullable
	EnumSet<InsetSide> getRootInsetSides();

	@Nullable
	List<Integer> getFabIds();

	@Nullable
	List<Integer> getScrollableViewIds();

	@Nullable
	List<Integer> getBottomContainersIds();

	void onApplyInsets(@NonNull WindowInsetsCompat insets);

	@NonNull
	Activity requireActivity();

	@Nullable
	WindowInsetsCompat getLastRootInsets();

	void setLastRootInsets(@NonNull WindowInsetsCompat rootInsets);
}
