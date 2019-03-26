package net.osmand.plus.profiles;

import android.os.Bundle;
import android.view.MenuItem;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.OsmandActionBarActivity;

public class SettingsProfileActivity extends OsmandActionBarActivity {

	private OsmandApplication app;



	@Override
	public void onCreate(Bundle savedInstanceState) {
		app = getMyApplication();
		app.applyTheme(this);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.single_fragment_layout);
		if (savedInstanceState == null) {
			SettingsProfileFragment profileFragment = new SettingsProfileFragment();
			profileFragment.setArguments(getIntent().getExtras());
			getSupportFragmentManager().beginTransaction().add(android.R.id.content, profileFragment).commit();
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int itemId = item.getItemId();
		switch (itemId) {
			case android.R.id.home:
				finish();
				return true;

		}
		return false;
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	public class ProfileItem {
		private int iconRes;
		private String title;
		private String descr;
		private boolean state;

		public ProfileItem(int iconRes, String title, String descr, boolean state) {
			this.iconRes = iconRes;
			this.title = title;
			this.descr = descr;
			this.state = state;
		}

		public int getIconRes() {
			return iconRes;
		}

		public void setIconRes(int iconRes) {
			this.iconRes = iconRes;
		}

		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}

		public String getDescr() {
			return descr;
		}

		public void setDescr(String descr) {
			this.descr = descr;
		}

		public boolean getState() {
			return state;
		}

		public void setState(boolean state) {
			this.state = state;
		}
	}
}
