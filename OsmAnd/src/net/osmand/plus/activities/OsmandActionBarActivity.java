package net.osmand.plus.activities;

import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;

public class OsmandActionBarActivity extends AppCompatActivity {

	protected boolean haveHomeButton = true;

    //should be called after set content view
    protected void setupHomeButton(){
        Drawable back = ((OsmandApplication)getApplication()).getIconsCache().getIcon(R.drawable.ic_arrow_back);
        back.setColorFilter(ContextCompat.getColor(this, R.color.color_white), PorterDuff.Mode.MULTIPLY);
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

	public OsmandApplication getMyApplication() {
		return (OsmandApplication) getApplication();
	}
}
