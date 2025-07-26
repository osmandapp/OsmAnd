package net.osmand.plus.settings.preferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.util.Algorithms;

public class ListParameters {

	public final String[] originalNames;
	public final String[] localizedNames;
	public final Object[] values;

	public ListParameters(@NonNull String[] originalNames, @NonNull String[] localizedNames, @NonNull Object[] values) {
		this.originalNames = originalNames;
		this.localizedNames = localizedNames;
		this.values = values;
	}

	public int findIndexOfValue(@Nullable Object value) {
		for (int i = 0; i < values.length; i++) {
			if (Algorithms.objectEquals(values[i], value)) {
				return i;
			}
		}
		return -1;
	}
}
