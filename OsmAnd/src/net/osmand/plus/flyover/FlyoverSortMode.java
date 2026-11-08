package net.osmand.plus.flyover;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import net.osmand.plus.R;

import java.io.File;
import java.util.Comparator;

/** How My Places > Flyovers orders the videos. */
public enum FlyoverSortMode {

	DATE_NEWEST(R.string.sort_date_ascending, R.drawable.ic_action_sort_date_31),
	DATE_OLDEST(R.string.sort_date_descending, R.drawable.ic_action_sort_date_1),
	NAME_ASCENDING(R.string.sort_name_ascending, R.drawable.ic_action_sort_by_name_ascending),
	NAME_DESCENDING(R.string.sort_name_descending, R.drawable.ic_action_sort_by_name_descending),
	DURATION_LONGEST(R.string.sort_duration_descending, R.drawable.ic_action_sort_duration_long_to_short),
	DURATION_SHORTEST(R.string.sort_duration_ascending, R.drawable.ic_action_sort_duration_short_to_long),
	SIZE_LARGEST(R.string.sort_size_descending, R.drawable.ic_action_sort_big_to_small);

	@StringRes
	private final int nameId;
	@DrawableRes
	private final int iconId;

	FlyoverSortMode(@StringRes int nameId, @DrawableRes int iconId) {
		this.nameId = nameId;
		this.iconId = iconId;
	}

	@StringRes
	public int getNameId() {
		return nameId;
	}

	@DrawableRes
	public int getIconId() {
		return iconId;
	}

	/** Whether this mode needs the video durations, which are read from the files in the background. */
	public boolean needsDuration() {
		return this == DURATION_LONGEST || this == DURATION_SHORTEST;
	}

	/** The last mode before this one in the sheet gets a divider after it. */
	public boolean showDividerAfter() {
		return this == DATE_OLDEST || this == NAME_DESCENDING;
	}

	@NonNull
	public static FlyoverSortMode valueOf(@NonNull String key, @NonNull FlyoverSortMode fallback) {
		try {
			return valueOf(key);
		} catch (IllegalArgumentException e) {
			return fallback;
		}
	}

	/** Durations in ms are looked up per file; an unknown duration sorts last. */
	@NonNull
	public Comparator<File> comparator(@NonNull java.util.function.ToLongFunction<File> durationMs) {
		switch (this) {
			case DATE_OLDEST:
				return Comparator.comparingLong(File::lastModified);
			case NAME_ASCENDING:
				return Comparator.comparing(f -> f.getName().toLowerCase());
			case NAME_DESCENDING:
				return Comparator.comparing((File f) -> f.getName().toLowerCase()).reversed();
			case DURATION_LONGEST:
				return Comparator.comparingLong(durationMs).reversed();
			case DURATION_SHORTEST:
				return Comparator.comparingLong(f -> {
					long d = durationMs.applyAsLong(f);
					return d < 0 ? Long.MAX_VALUE : d;
				});
			case SIZE_LARGEST:
				return Comparator.comparingLong(File::length).reversed();
			default:
				return Comparator.comparingLong(File::lastModified).reversed();
		}
	}
}
