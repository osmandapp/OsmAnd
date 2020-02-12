package net.osmand.plus.activities;

import android.annotation.SuppressLint;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;

@SuppressLint("Registered")
public class OsmandActionBarActivity extends OsmandInAppPurchaseActivity {

	protected boolean haveHomeButton = true;

    //should be called after set content view
    protected void setupHomeButton() {
    	boolean lightTheme = getMyApplication().getSettings().isLightContent();
        Drawable back = ((OsmandApplication)getApplication()).getUIUtilities().getIcon(R.drawable.ic_arrow_back,
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
