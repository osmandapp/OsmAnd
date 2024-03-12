package net.osmand.plus.settings.preferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.util.Algorithms;

public class ListParameters {

	public final String[] names;
	public final Object[] values;

	public ListParameters(@NonNull String[] names, @NonNull Object[] values) {
		this.names = names;
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
