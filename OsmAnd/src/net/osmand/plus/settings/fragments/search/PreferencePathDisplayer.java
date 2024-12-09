package net.osmand.plus.settings.fragments.search;

import android.content.Context;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.TextAppearanceSpan;

import net.osmand.plus.R;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import de.KnollFrank.lib.settingssearch.PreferencePath;
import de.KnollFrank.lib.settingssearch.db.preference.pojo.SearchablePreferencePOJO;

class PreferencePathDisplayer implements de.KnollFrank.lib.settingssearch.results.recyclerview.PreferencePathDisplayer {

	private final Context context;
	private final Set<String> applicationModeKeys;

	public PreferencePathDisplayer(final Context context, final Set<String> applicationModeKeys) {
		this.context = context;
		this.applicationModeKeys = applicationModeKeys;
	}

	@Override
	public CharSequence display(final PreferencePath preferencePath) {
		final List<SpannableString> titles = getTitles(preferencePath);
		highlightApplicationModeAtStartOfLongPreferencePath(preferencePath, titles);
		return join(titles, new SpannableString(" > "));
	}

	private static List<SpannableString> getTitles(final PreferencePath preferencePath) {
		return preferencePath
				.preferences()
				.stream()
				.map(PreferencePathDisplayer::getTitle)
				.collect(Collectors.toList());
	}

	private static SpannableString getTitle(final SearchablePreferencePOJO searchablePreferencePOJO) {
		return searchablePreferencePOJO
				.getTitle()
				.map(SpannableString::new)
				.orElseGet(() -> new SpannableString("?"));
	}

	private void highlightApplicationModeAtStartOfLongPreferencePath(final PreferencePath preferencePath, final List<SpannableString> titles) {
		if (preferencePath.preferences().size() >= 2 && isApplicationMode(preferencePath.preferences().get(0))) {
			highlight(titles.get(0));
		}
	}

	private boolean isApplicationMode(final SearchablePreferencePOJO preference) {
		return preference
				.getKey()
				.filter(applicationModeKeys::contains)
				.isPresent();
	}

	private void highlight(final Spannable spannable) {
		spannable.setSpan(
				new TextAppearanceSpan(context, R.style.PreferencePathTextAppearance),
				0,
				spannable.length(),
				Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
	}

	private static SpannableString join(final List<SpannableString> titles, final SpannableString delimiter) {
		return appendAll(insert(titles, delimiter));
	}

	private static <T> List<T> insert(final List<T> ts, final T delimiter) {
		return ts
				.stream()
				.flatMap(t -> Stream.of(t, delimiter))
				.limit(ts.size() * 2L - 1)
				.collect(Collectors.toList());
	}

	private static SpannableString appendAll(final List<? extends CharSequence> charSequences) {
		final SpannableStringBuilder builder = new SpannableStringBuilder();
		charSequences.forEach(builder::append);
		return new SpannableString(builder);
	}
}
