package net.osmand.plus.activities;

import android.app.Activity;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Spinner;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;

public abstract class ActionBarPreferenceActivity extends AppCompatPreferenceActivity {
	private Toolbar tb;
	private View shadowView;

	public Toolbar getToolbar() {
		return tb;
	}

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		//settings needed it's own theme because of check boxes not styled properly
		OsmandSettings settings = ((OsmandApplication) getApplication()).getSettings();
		int t = R.style.OsmandLightTheme_NoActionbar_Preferences;
		if (settings.OSMAND_THEME.get() == OsmandSettings.OSMAND_DARK_THEME) {
			t = R.style.OsmandDarkTheme_NoActionbar_Preferences;
		} else if (settings.OSMAND_THEME.get() == OsmandSettings.OSMAND_LIGHT_THEME) {
			t = R.style.OsmandLightTheme_NoActionbar_Preferences;
		}
		setTheme(t);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.preference_activity);
		tb = (Toolbar) findViewById(R.id.toolbar);
		if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
			shadowView = findViewById(R.id.shadowView);
			final ViewGroup parent = (ViewGroup) shadowView.getParent();
			parent.removeView(shadowView);
			shadowView = null;
		}
		tb.setClickable(true);
		tb.setNavigationIcon(((OsmandApplication) getApplication()).getIconsCache().getIcon(R.drawable.ic_arrow_back));
		tb.setNavigationContentDescription(R.string.access_shared_string_navigate_up);
		tb.setBackgroundColor(getResources().getColor(getResIdFromAttribute(this, R.attr.pstsTabBackground)));
		tb.setTitleTextColor(getResources().getColor(getResIdFromAttribute(this, R.attr.pstsTextColor)));
		tb.setNavigationOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(final View v) {
				finish();
			}
		});

		getSpinner().setVisibility(View.GONE);
		setProgressVisibility(false);
	}

	static int getResIdFromAttribute(final Activity activity, final int attr) {
		if (attr == 0)
			return 0;
		final TypedValue typedvalueattr = new TypedValue();
		activity.getTheme().resolveAttribute(attr, typedvalueattr, true);
		return typedvalueattr.resourceId;
	}

	protected void setEnabledActionBarShadow(final boolean enable) {
		if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
			ViewCompat.setElevation(tb, enable ? 4 : 0);
		} else {
			if (shadowView == null)
				shadowView = findViewById(R.id.shadowView);
			shadowView.setVisibility(enable ? View.VISIBLE : View.GONE);
		}
	}

	protected Spinner getSpinner() {
		return (Spinner) findViewById(R.id.spinner_nav);
	}

	protected void setProgressVisibility(boolean visibility) {
		if (visibility) {
			findViewById(R.id.ProgressBar).setVisibility(View.VISIBLE);
		} else {
			findViewById(R.id.ProgressBar).setVisibility(View.GONE);
		}

	}
}