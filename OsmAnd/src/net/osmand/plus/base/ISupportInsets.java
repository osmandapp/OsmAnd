package net.osmand.plus.base;

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.WindowInsetsCompat;

import net.osmand.plus.R;
import net.osmand.plus.utils.InsetTarget;
import net.osmand.plus.utils.InsetTargetsCollection;
import net.osmand.plus.utils.InsetsUtils;

public interface ISupportInsets {

	default InsetTargetsCollection getInsetTargets() {
		InsetTargetsCollection collection = new InsetTargetsCollection();
		collection.add(InsetTarget.createBottomContainer(R.id.bottom_buttons_container));

		return collection;
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
