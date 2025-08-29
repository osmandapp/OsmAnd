package net.osmand.plus.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.view.View;

import androidx.appcompat.app.ActionBar;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.settings.enums.ThemeUsageContext;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.InsetsUtils;
import net.osmand.plus.utils.InsetsUtils.InsetSide;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

@SuppressLint("Registered")
public class OsmandActionBarActivity extends OsmandInAppPurchaseActivity {

	protected boolean haveHomeButton = true;
	private final List<ActivityResultListener> resultListeners = new ArrayList<>();


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

	@Override
	public void onContentChanged() {
		super.onContentChanged();

		if (haveHomeButton) {
			setupHomeButton();
		}

		View root = findViewById(R.id.root);
		if (root != null) {
			InsetsUtils.setWindowInsetsListener(root, EnumSet.of(
					InsetSide.TOP,
					InsetSide.BOTTOM
			));
		}
	}

	private void setupHomeButton() {
		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			OsmandApplication app = getMyApplication();
			boolean nightMode = app.getDaynightHelper().isNightMode(ThemeUsageContext.APP);
			int iconId = AndroidUtils.getNavigationIconResId(app);
			int colorId = ColorUtilities.getActiveButtonsAndLinksTextColorId(nightMode);

			actionBar.setHomeButtonEnabled(true);
			actionBar.setDisplayHomeAsUpEnabled(true);
			actionBar.setHomeAsUpIndicator(app.getUIUtilities().getIcon(iconId, colorId));
		}
	}
}
