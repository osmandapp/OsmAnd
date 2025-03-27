package net.osmand.plus.settings.preferences;

import android.content.Context;
import android.util.AttributeSet;

import androidx.preference.Preference;

public abstract class BaseCustomPreference extends Preference {
	public BaseCustomPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		setLayoutResource(getLayoutId());
	}

	protected abstract int getLayoutId();
}