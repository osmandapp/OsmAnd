package net.osmand.plus.activities;

import static net.osmand.plus.settings.enums.ThemeUsageContext.APP;

import android.os.Bundle;
import android.widget.FrameLayout;

import androidx.appcompat.app.ActionBar;

import net.osmand.plus.R;

/**
 * Host of the OsmAnd builds screen ({@link BuildsFragment}). Started by the Development plugin by
 * class name, because the class exists only in the flavours that may install builds.
 */
public class ContributionVersionActivity extends OsmandActionBarActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		app.applyTheme(this);
		super.onCreate(savedInstanceState);
		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			// the Material 3 screen has its own app bar
			actionBar.hide();
		}
		FrameLayout container = new FrameLayout(this);
		container.setId(R.id.fragmentContainer);
		setContentView(container);
		if (savedInstanceState == null) {
			getSupportFragmentManager().beginTransaction()
					.replace(R.id.fragmentContainer, new BuildsFragment(), BuildsFragment.class.getName())
					.commit();
		}
	}

	@Override
	protected int getStatusBarColorId() {
		return app.getDaynightHelper().isNightMode(APP) ? R.color.surface_dark : R.color.surface_light;
	}
}
