package net.osmand.plus.activities;

import android.annotation.SuppressLint;
import android.view.View;
import android.view.ViewGroup;

import androidx.appcompat.app.ActionBar;

import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.OsmandApplication;

@SuppressLint("Registered")
public class OsmandActionBarActivity extends OsmandInAppPurchaseActivity {

	protected boolean haveHomeButton = true;

	//should be called after set content view
	protected void setupHomeButton() {
		ActionBar supportActionBar = getSupportActionBar();
		if (supportActionBar != null) {
			OsmandApplication app = getMyApplication();
			boolean nightMode = !app.getSettings().isLightContent();
			int iconId = AndroidUtils.getNavigationIconResId(app);
			int colorId = ColorUtilities.getActiveButtonsAndLinksTextColorId(nightMode);

			supportActionBar.setHomeButtonEnabled(true);
			supportActionBar.setDisplayHomeAsUpEnabled(true);
			supportActionBar.setHomeAsUpIndicator(app.getUIUtilities().getIcon(iconId, colorId));
		}
	}

    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);
		if (haveHomeButton) {
			setupHomeButton();
		}
    }

    @Override
    public void setContentView(View view) {
        super.setContentView(view);
		if (haveHomeButton) {
			setupHomeButton();
		}
    }

    @Override
    public void setContentView(View view, ViewGroup.LayoutParams params) {
        super.setContentView(view, params);
		if (haveHomeButton) {
			setupHomeButton();
		}
    }
}
