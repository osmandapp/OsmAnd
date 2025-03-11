package net.osmand.data;

import net.osmand.plus.R;

public enum DataSourceType {
    OFFLINE(R.string.shared_string_offline),
    ONLINE(R.string.shared_string_online);

    public final int name;

    DataSourceType(int name) {
        this.name = name;
    }
}
