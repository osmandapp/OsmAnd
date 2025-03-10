package net.osmand.plus.wikipedia;

import net.osmand.plus.R;

public enum WikiDataSource {
    OFFLINE(R.string.shared_string_offline),
    ONLINE(R.string.shared_string_online);

    public final int name;

    WikiDataSource(int name) {
        this.name = name;
    }
}

