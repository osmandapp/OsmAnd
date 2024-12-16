package net.osmand.plus.settings.preferences;

import android.content.Context;
import android.util.AttributeSet;

import androidx.preference.EditTextPreference;

import net.osmand.plus.settings.fragments.search.SearchableInfoProvider;

import java.util.Optional;
import java.util.stream.Collectors;

import de.KnollFrank.lib.settingssearch.common.Optionals;

public class EditTextPreferenceEx extends EditTextPreference implements SearchableInfoProvider {

	private String description;

	public EditTextPreferenceEx(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
	}

	public EditTextPreferenceEx(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	public EditTextPreferenceEx(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public EditTextPreferenceEx(Context context) {
		super(context);
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void setDescription(int descriptionResId) {
		setDescription(getContext().getString(descriptionResId));
	}

	@Override
	public String getSearchableInfo() {
		return Optionals
				.streamOfPresentElements(
						Optional.ofNullable(getText()),
						Optional.ofNullable(getDescription()))
				.collect(Collectors.joining(", "));
	}
}
