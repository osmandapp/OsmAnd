package net.osmand.plus.settings.fragments.search;

import static net.osmand.plus.settings.fragments.search.SearchableInfoProviderHelper.joinNonNullElements;

import net.osmand.plus.settings.preferences.SwitchPreferenceEx;

import java.util.Arrays;

import de.KnollFrank.lib.preferencesearch.search.provider.DefaultSummarySetter;
import de.KnollFrank.lib.preferencesearch.search.provider.ISummaryResetter;
import de.KnollFrank.lib.preferencesearch.search.provider.ISummarySetter;
import de.KnollFrank.lib.preferencesearch.search.provider.PreferenceDescription;
import de.KnollFrank.lib.preferencesearch.search.provider.SearchableInfoProvider;

class SwitchPreferenceExDescriptionFactory {

	public static PreferenceDescription<SwitchPreferenceEx> getSwitchPreferenceExDescription() {
		return new PreferenceDescription<>(
				SwitchPreferenceEx.class,
				new SearchableInfoProvider<SwitchPreferenceEx>() {

					@Override
					public String getSearchableInfo(final SwitchPreferenceEx preference) {
						return joinNonNullElements(
								", ",
								Arrays.asList(
										preference.getSummaryOff(),
										preference.getSummaryOn(),
										preference.getDescription()));
					}
				},
				new ISummarySetter<SwitchPreferenceEx>() {

					@Override
					public void setSummary(final SwitchPreferenceEx switchPreferenceEx, final CharSequence summary) {
						switchPreferenceEx.setSummaryOn(null);
						switchPreferenceEx.setSummaryOff(null);
						new DefaultSummarySetter().setSummary(switchPreferenceEx, summary);
					}
				},
				SwitchPreferenceExSummaryResetter::new);
	}

	static class SwitchPreferenceExSummaryResetter implements ISummaryResetter {

		private final SwitchPreferenceEx switchPreferenceEx;
		private final CharSequence summary;
		private final CharSequence summaryOn;
		private final CharSequence summaryOff;

		public SwitchPreferenceExSummaryResetter(final SwitchPreferenceEx switchPreferenceEx) {
			this.switchPreferenceEx = switchPreferenceEx;
			this.summary = switchPreferenceEx.getSummary();
			this.summaryOn = switchPreferenceEx.getSummaryOn();
			this.summaryOff = switchPreferenceEx.getSummaryOff();
		}

		@Override
		public void resetSummary() {
			switchPreferenceEx.setSummaryOn(summaryOn);
			switchPreferenceEx.setSummaryOff(summaryOff);
			new DefaultSummarySetter().setSummary(switchPreferenceEx, summary);
		}
	}
}
