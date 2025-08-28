package net.osmand.search.core;

import net.osmand.Collator;
import net.osmand.OsmAndCollator;

public class MutableSearchPhrase extends SearchPhrase {

    private MutableSearchPhrase(SearchSettings settings, Collator clt) {
        super(settings, clt);
        if (this.settings.getSearchVersion() == SearchSettings.SearchVersion.EXPAND_ABBREVIATIONS) {
            this.settings.setRegionLang("en");
        }
    }

    public static MutableSearchPhrase emptyPhrase() {
        return emptyPhrase(null);
    }

    public static MutableSearchPhrase emptyPhrase(SearchSettings settings) {
        return emptyPhrase(settings, OsmAndCollator.primaryCollator());
    }

    public static MutableSearchPhrase emptyPhrase(SearchSettings settings, Collator clt) {
        return new MutableSearchPhrase(settings, clt);
    }

    @Override
    public MutableSearchPhrase mutablePhrase(SearchSettings settings, Collator clt) {
        return new MutableSearchPhrase(settings, clt);
    }
}
