package net.osmand.plus.activities;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;

import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;

import net.osmand.AndroidUtils;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;

public class PrivacyAndSecurityActivity extends OsmandActionBarActivity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		OsmandApplication app = getMyApplication();
		int themeId = !app.getSettings().isLightContent() ? R.style.OsmandDarkTheme_NoActionbar : R.style.OsmandLightTheme_NoActionbar;
		setTheme(themeId);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.privacy_settings_layout);
		Toolbar tb = (Toolbar) findViewById(R.id.toolbar);
		tb.setTitle(R.string.settings_privacy_and_security);

		tb.setClickable(true);
		Drawable icBack = ((OsmandApplication) getApplication()).getUIUtilities().getIcon(AndroidUtils.getNavigationIconResId(app));
		tb.setNavigationIcon(icBack);
		tb.setNavigationContentDescription(R.string.access_shared_string_navigate_up);
		tb.setBackgroundColor(getResources().getColor(resolveResourceId(this, R.attr.pstsTabBackground)));
		tb.setTitleTextColor(getResources().getColor(resolveResourceId(this, R.attr.pstsTextColor)));
		tb.setNavigationOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(final View v) {
				finish();
			}
		});

		View downloadedMapsContainer = findViewById(R.id.downloaded_maps_container);
		final SwitchCompat downloadedMapsButton = (SwitchCompat) findViewById(R.id.downloaded_maps_button);
		downloadedMapsButton.setChecked(app.getSettings().SEND_ANONYMOUS_MAP_DOWNLOADS_DATA.get());
		downloadedMapsButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				OsmandApplication app = getMyApplication();
				app.getSettings().SEND_ANONYMOUS_MAP_DOWNLOADS_DATA.set(isChecked);
			}
		});
		downloadedMapsContainer.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				downloadedMapsButton.setChecked(!downloadedMapsButton.isChecked());
			}
		});

		View visitedScreensContainer = findViewById(R.id.visited_screens_container);
		final SwitchCompat visitedScreensButton = (SwitchCompat) findViewById(R.id.visited_screens_button);
		visitedScreensButton.setChecked(app.getSettings().SEND_ANONYMOUS_APP_USAGE_DATA.get());
		visitedScreensButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				OsmandApplication app = getMyApplication();
				app.getSettings().SEND_ANONYMOUS_APP_USAGE_DATA.set(isChecked);
			}
		});
		visitedScreensContainer.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				visitedScreensButton.setChecked(!visitedScreensButton.isChecked());
			}
		});
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

	private int resolveResourceId(final Activity activity, final int attr) {
		final TypedValue typedvalueattr = new TypedValue();
		activity.getTheme().resolveAttribute(attr, typedvalueattr, true);
		return typedvalueattr.resourceId;
	}
}
