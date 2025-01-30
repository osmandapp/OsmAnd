package net.osmand.plus.settings.preferences;

import android.content.Context;
import android.util.AttributeSet;

import androidx.preference.SwitchPreferenceCompat;

import net.osmand.plus.settings.fragments.search.SearchableInfoProvider;

import java.util.Optional;
import java.util.stream.Collectors;

import de.KnollFrank.lib.settingssearch.common.Optionals;

public class SwitchPreferenceEx extends SwitchPreferenceCompat implements SearchableInfoProvider {

	private String description;

	public SwitchPreferenceEx(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
	}

	public SwitchPreferenceEx(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	public SwitchPreferenceEx(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public SwitchPreferenceEx(Context context) {
		super(context);
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void setDescription(int titleResId) {
		setDescription(getContext().getString(titleResId));
	}

	@Override
	protected void onClick() {
		if (getFragment() == null && getIntent() == null) {
			getPreferenceManager().showDialog(this);
		}
	}

	@Override
	public String getSearchableInfo() {
		return Optionals
				.streamOfPresentElements(
						Optional.ofNullable(getSummaryOff()),
						Optional.ofNullable(getSummaryOn()),
						Optional.ofNullable(getDescription()))
				.collect(Collectors.joining(", "));
	}
}