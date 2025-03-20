package net.osmand.plus.widgets.alert;

import android.content.Context;

import androidx.preference.Preference;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

public class TitleByKey2PreferencesConverter {

	private final Context context;

	public TitleByKey2PreferencesConverter(final Context context) {
		this.context = context;
	}

	public Collection<Preference> asPreferences(final Map<String, ? extends CharSequence> titleByKey) {
		return titleByKey
				.entrySet()
				.stream()
				.map(key_title_entry -> asPreference(key_title_entry.getKey(), key_title_entry.getValue()))
				.collect(Collectors.toUnmodifiableList());
	}

	private Preference asPreference(final String key, final CharSequence title) {
		final Preference preference = new Preference(context);
		preference.setKey(key);
		preference.setTitle(title);
		return preference;
	}
}
