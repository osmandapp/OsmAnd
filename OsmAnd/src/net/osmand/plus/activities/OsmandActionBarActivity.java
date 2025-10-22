package net.osmand.plus.activities;

import static net.osmand.plus.settings.enums.ThemeUsageContext.APP;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentManager.FragmentLifecycleCallbacks;

import net.osmand.StateChangedListener;
import net.osmand.plus.R;
import net.osmand.plus.base.BaseOsmAndDialogFragment;
import net.osmand.plus.base.ISupportInsets;
import net.osmand.plus.help.HelpActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.settings.enums.DayNightMode;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.InsetTarget;
import net.osmand.plus.utils.InsetTarget.Type;
import net.osmand.plus.utils.InsetTargetsCollection;
import net.osmand.plus.utils.InsetsUtils;

import java.util.ArrayList;
import java.util.List;

@SuppressLint("Registered")
public class OsmandActionBarActivity extends OsmandInAppPurchaseActivity implements ISupportInsets {

	private final List<ActivityResultListener> resultListeners = new ArrayList<>();
	protected WindowInsetsCompat rootInsets;

	private StateChangedListener<DayNightMode> stateChangedListener;
	@ColorRes
	protected int getStatusBarColorId() {
		boolean nightMode = app.getDaynightHelper().isNightMode(APP);
		return ColorUtilities.getStatusBarColorId(nightMode);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		if (InsetsUtils.isEdgeToEdgeSupported()) {
			EdgeToEdge.enable(this);
		}
		updateNavBarColor();
		stateChangedListener = o -> updateNavBarColor();
		settings.DAYNIGHT_MODE.addListener(stateChangedListener);

		super.onCreate(savedInstanceState);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		for (ActivityResultListener listener : resultListeners) {
			if (listener.processResult(requestCode, resultCode, data)) {
				removeActivityResultListener(listener);
				return;
			}
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	public void registerActivityResultListener(ActivityResultListener listener) {
		if (!resultListeners.contains(listener)) {
			resultListeners.add(listener);
		}
	}

	public void removeActivityResultListener(ActivityResultListener listener) {
		resultListeners.remove(listener);
	}

	protected int getRootViewId(){
		return R.id.root;
	}

	@Override
	public void onContentChanged() {
		super.onContentChanged();

		setupHomeButton();
		updateStatusBarColor();

		View root = findViewById(getRootViewId());
		if (root != null) {
			InsetsUtils.processInsets(this, root, null, false);
		}

		getSupportFragmentManager().registerFragmentLifecycleCallbacks(
				new FragmentLifecycleCallbacks() {
					@Override
					public void onFragmentResumed(@NonNull FragmentManager fm, @NonNull Fragment f) {
						if (f instanceof ISupportInsets) {
							((ISupportInsets) f).updateNavBarColor();
						}
					}

					@Override
					public void onFragmentDestroyed(@NonNull FragmentManager fm, @NonNull Fragment f) {
						super.onFragmentDestroyed(fm, f);
						Fragment top = getTopFragment(fm);
						if (top == null) {
							updateNavBarColor();

						} else if (f instanceof BaseOsmAndDialogFragment) {
							if (top instanceof ISupportInsets) {
								((ISupportInsets) top).updateNavBarColor();
							}
						}
					}
				}, false
		);
	}

	@Nullable
	private Fragment getTopFragment(FragmentManager fm) {
		List<Fragment> fragments = fm.getFragments();
		for (int i = fragments.size() - 1; i >= 0; i--) {
			Fragment frag = fragments.get(i);
			if (frag != null && frag.isVisible()) {
				return frag;
			}
		}
		return null;
	}

	protected void setupHomeButton() {
		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			boolean nightMode = app.getDaynightHelper().isNightMode(APP);
			int iconId = AndroidUtils.getNavigationIconResId(app);
			int colorId = ColorUtilities.getActiveButtonsAndLinksTextColorId(nightMode);

			actionBar.setHomeButtonEnabled(true);
			actionBar.setDisplayHomeAsUpEnabled(true);
			actionBar.setHomeAsUpIndicator(app.getUIUtilities().getIcon(iconId, colorId));
		}
	}

	public void updateStatusBarColor() {
		int colorId = getStatusBarColorId();
		if (colorId != -1) {
			AndroidUiHelper.setStatusBarColor(this, getColor(colorId));
		}
	}

	public void updateNavigationBarColor() {
		updateNavBarColor();
	}

	@Override
	public InsetTargetsCollection getInsetTargets() {
		InsetTargetsCollection collection = new InsetTargetsCollection();
		collection.removeType(Type.FAB);
		collection.removeType(Type.SCROLLABLE);
		collection.removeType(Type.BOTTOM_CONTAINER);
		collection.add(InsetTarget.createRootInset());
		return collection;
	}

	@Override
	public void onApplyInsets(@NonNull WindowInsetsCompat insets) {

	}

	@NonNull
	@Override
	public Activity requireActivity() {
		return this;
	}

	@Nullable
	@Override
	public WindowInsetsCompat getLastRootInsets() {
		return rootInsets;
	}

	@Override
	public void setLastRootInsets(@NonNull WindowInsetsCompat rootInsets) {
		this.rootInsets = rootInsets;
	}
}
