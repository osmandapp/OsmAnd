package net.osmand.plus.base;

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.WindowInsetsCompat;

import net.osmand.plus.R;
import net.osmand.plus.utils.InsetsUtils;
import net.osmand.plus.utils.InsetsUtils.InsetSide;

import java.util.ArrayList;
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
	default List<Integer> getCollapsingAppBarLayoutId(){
		return null;
	}

	@Nullable
	default List<Integer> getBottomContainersIds(){
		List<Integer> ids = new ArrayList<>();
		ids.add(R.id.bottom_buttons_container);
		return ids;
	}

	void onApplyInsets(@NonNull WindowInsetsCompat insets);

	@NonNull
	Activity requireActivity();

	@Nullable
	WindowInsetsCompat getLastRootInsets();

	void setLastRootInsets(@NonNull WindowInsetsCompat rootInsets);

	default int getNavigationBarColorId(){
		return -1;
	}

	default void updateNavBarColor(){
		InsetsUtils.processNavBarColor(this);
	}
}
