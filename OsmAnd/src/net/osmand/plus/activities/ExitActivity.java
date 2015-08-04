package net.osmand.plus.activities;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import android.app.Activity;
import android.os.Bundle;


public class ExitActivity extends Activity {
	public final static String DISABLE_SERVICE  = "DISABLE_SERVICE";
	private boolean dis;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.help_activity);
		dis = getIntent().getBooleanExtra(DISABLE_SERVICE, true);
		
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		getMyApplication().closeApplicationAnywayImpl(this, dis);
	}

	private OsmandApplication getMyApplication() {
		return (OsmandApplication) getApplication();
	}	
}
