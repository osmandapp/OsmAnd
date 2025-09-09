package net.osmand.plus.base;

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.WindowInsetsCompat;

import net.osmand.plus.utils.InsetsUtils.InsetSide;

import java.util.List;
import java.util.Set;

public interface ISupportInsets {

	@Nullable
	Set<InsetSide> getRootInsetSides();

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
