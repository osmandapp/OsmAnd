package net.osmand.plus.activities;

import android.app.Activity;
import android.content.res.TypedArray;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Spinner;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;

public abstract class ActionBarPreferenceActivity extends PreferenceActivity {
	private Toolbar toolbar;
	private View shadowView;

	public Toolbar getToolbar() {
		return toolbar;
	}

	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.preference_activity);
		toolbar = (Toolbar) findViewById(R.id.toolbar);
		if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
			shadowView = findViewById(R.id.shadowView);
			final ViewGroup parent = (ViewGroup) shadowView.getParent();
			parent.removeView(shadowView);
			shadowView = null;
		}
		toolbar.setClickable(true);
		Drawable back = getResources().getDrawable(R.drawable.abc_ic_ab_back_mtrl_am_alpha);
		back.setColorFilter(0xffffffff, PorterDuff.Mode.MULTIPLY);
		toolbar.setNavigationIcon(back);
		toolbar.setBackgroundColor(getResources().getColor(getResIdFromAttribute(this, R.attr.pstsTabBackground)));
		toolbar.setTitleTextColor(getResources().getColor(getResIdFromAttribute(this, R.attr.pstsTextColor)));
		toolbar.setNavigationOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(final View v) {
				finish();
			}
		});

		getSpinner().setVisibility(View.GONE);
		setProgressVisibility(false);
	}

	private static int getResIdFromAttribute(final Activity activity, final int attr) {
		if (attr == 0)
			return 0;
		final TypedValue typedvalueattr = new TypedValue();
		activity.getTheme().resolveAttribute(attr, typedvalueattr, true);
		return typedvalueattr.resourceId;
	}

	protected void setEnabledActionBarShadow(final boolean enable) {
		if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
			ViewCompat.setElevation(toolbar, enable ? 4 : 0);
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