package net.osmand.plus.activities;

import android.app.Activity;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import net.osmand.plus.R;

public abstract class ActionBarPreferenceActivity extends PreferenceActivity {
	private Toolbar _toolbar;
	private View _shadowView;

	public Toolbar getToolbar() {
		return _toolbar;
	}

	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.preference_activity);
		_toolbar = (Toolbar) findViewById(R.id.toolbar);
		if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
			_shadowView = findViewById(R.id.shadowView);
			final ViewGroup parent = (ViewGroup) _shadowView.getParent();
			parent.removeView(_shadowView);
			_shadowView = null;
		}
		_toolbar.setClickable(true);
		_toolbar.setNavigationIcon(getResIdFromAttribute(this, R.attr.homeAsUpIndicator));
		_toolbar.setNavigationOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(final View v) {
				finish();
			}
		});
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
			ViewCompat.setElevation(_toolbar, enable ? 4 : 0);
		} else {
			if (_shadowView == null)
				_shadowView = findViewById(R.id.shadowView);
			_shadowView.setVisibility(enable ? View.VISIBLE : View.GONE);
		}
	}
}