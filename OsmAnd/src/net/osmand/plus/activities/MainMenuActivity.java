package net.osmand.plus.activities;

import net.osmand.plus.OsmAndAppCustomization;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.search.SearchActivity;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Window;

public class MainMenuActivity extends Activity {

	private OsmandApplication getMyApplication() {
		return (OsmandApplication) getApplication();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.menu);
		
		final OsmAndAppCustomization appCustomization = getMyApplication().getAppCustomization();
		final Intent settings = new Intent(this, appCustomization.getMainMenuActivity());
		startActivity(settings);
	}
	

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_SEARCH
                && event.getRepeatCount() == 0) {
			final Intent search = new Intent(MainMenuActivity.this, SearchActivity.class);
			search.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
			startActivity(search);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
 
	
}
