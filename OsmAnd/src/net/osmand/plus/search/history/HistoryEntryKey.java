package net.osmand.plus.search.history;

import androidx.annotation.NonNull;

import net.osmand.data.PointDescription;
import net.osmand.plus.settings.enums.HistorySource;

import java.util.Objects;

final class HistoryEntryKey {

	private final PointDescription name;
	private final HistorySource source;

	HistoryEntryKey(@NonNull HistoryEntry entry) {
		this(entry.getName(), entry.getSource());
	}

	HistoryEntryKey(@NonNull PointDescription name, @NonNull HistorySource source) {
		this.name = name;
		this.source = source;
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, source);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof HistoryEntryKey other)) {
			return false;
		}
		return Objects.equals(name, other.name) && source == other.source;
	}
}
