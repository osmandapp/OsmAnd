package net.osmand.plus.activities;

import static net.osmand.plus.settings.enums.ThemeUsageContext.APP;
import static net.osmand.plus.utils.InsetsUtils.InsetSide.BOTTOM;
import static net.osmand.plus.utils.InsetsUtils.InsetSide.TOP;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.annotation.ColorRes;
import androidx.appcompat.app.ActionBar;

import net.osmand.plus.R;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.InsetsUtils;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

@SuppressLint("Registered")
public class OsmandActionBarActivity extends OsmandInAppPurchaseActivity {

	private final List<ActivityResultListener> resultListeners = new ArrayList<>();

	@ColorRes
	protected int getStatusBarColorId() {
		boolean nightMode = app.getDaynightHelper().isNightMode(APP);
		return ColorUtilities.getStatusBarColorId(nightMode);
	}

	@ColorRes
	protected int getNavigationBarColorId() {
		return -1;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		if (InsetsUtils.isEdgeToEdgeSupported()) {
			EdgeToEdge.enable(this);
		}
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

	@Override
	public void onContentChanged() {
		super.onContentChanged();

		setupHomeButton();
		updateStatusBarColor();

		View root = findViewById(R.id.root);
		if (root != null) {
			InsetsUtils.setWindowInsetsListener(root, EnumSet.of(TOP, BOTTOM));
		}
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
		int colorId = getNavigationBarColorId();
		if (colorId != -1) {
			//AndroidUiHelper.setNavigationBarColor(this, getColor(colorId));
		}
	}
}
