package net.osmand.plus.settings.fragments.search;

import net.osmand.plus.settings.preferences.SwitchPreferenceEx;

import de.KnollFrank.lib.preferencesearch.search.provider.DefaultSummarySetter;
import de.KnollFrank.lib.preferencesearch.search.provider.ISummaryResetter;

class SwitchPreferenceExSummaryResetter implements ISummaryResetter {

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
