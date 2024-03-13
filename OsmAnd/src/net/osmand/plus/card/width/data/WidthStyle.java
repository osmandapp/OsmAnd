package net.osmand.plus.card.width.data;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import java.util.Objects;

public class WidthStyle {

	private final String key;
	@StringRes
	private final int titleId;

	public WidthStyle(@Nullable String key, @StringRes int titleId) {
		this.key = key;
		this.titleId = titleId;
	}

	@Nullable
	public String getKey() {
		return key;
	}

	@NonNull
	public String toHumanString(@NonNull Context context) {
		return context.getString(titleId);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof WidthStyle)) return false;

		WidthStyle that = (WidthStyle) o;
		return Objects.equals(getKey(), that.getKey());
	}

	@Override
	public int hashCode() {
		String id = getKey();
		return id != null ? id.hashCode() : 0;
	}
}
