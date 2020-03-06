package net.osmand.plus.activities;

import android.annotation.SuppressLint;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;

import net.osmand.AndroidUtils;
import androidx.appcompat.app.ActionBar;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;

@SuppressLint("Registered")
public class OsmandActionBarActivity extends OsmandInAppPurchaseActivity {

	protected boolean haveHomeButton = true;

    //should be called after set content view
    protected void setupHomeButton() {
    	boolean lightTheme = getMyApplication().getSettings().isLightContent();
        Drawable back = ((OsmandApplication)getApplication()).getUIUtilities().getIcon(AndroidUtils.getNavigationIconResId(getApplication()),
				lightTheme ? R.color.active_buttons_and_links_text_light : R.color.active_buttons_and_links_text_dark);
        final ActionBar supportActionBar = getSupportActionBar();
        if (supportActionBar != null) {
            supportActionBar.setHomeButtonEnabled(true);
            supportActionBar.setDisplayHomeAsUpEnabled(true);
            supportActionBar.setHomeAsUpIndicator(back);
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
