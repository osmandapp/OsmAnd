package net.osmand.plus.settings.fragments.search;

import net.osmand.plus.settings.preferences.SwitchPreferenceEx;

import de.KnollFrank.lib.preferencesearch.search.provider.DefaultSummarySetter;
import de.KnollFrank.lib.preferencesearch.search.provider.ISummarySetter;

class SwitchPreferenceExSummarySetter implements ISummarySetter<SwitchPreferenceEx> {

    @Override
    public void setSummary(final SwitchPreferenceEx switchPreferenceEx, final CharSequence summary) {
        switchPreferenceEx.setSummaryOn(null);
        switchPreferenceEx.setSummaryOff(null);
        new DefaultSummarySetter().setSummary(switchPreferenceEx, summary);
    }
}
