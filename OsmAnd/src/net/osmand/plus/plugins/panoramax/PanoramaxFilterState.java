package net.osmand.plus.plugins.panoramax;

import static net.osmand.plus.plugins.panoramax.PanoramaxImage.ACCOUNT_ID_KEY;
import static net.osmand.plus.plugins.panoramax.PanoramaxImage.TYPE_EQUIRECTANGULAR;
import static net.osmand.plus.plugins.panoramax.PanoramaxImage.TYPE_KEY;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import java.util.Objects;

/**
 * The Panoramax filter settings as one immutable value, shared by the legacy layer and the
 * OpenGL tiles provider so both hide exactly the same features.
 *
 * Two states are equal when they produce the same rendered output, which is what decides
 * whether the OpenGL provider's cached tiles are still valid. Contributor and dates only count
 * while the filter is enabled, so the constructor clears them when it is not.
 */
public final class PanoramaxFilterState {

	private final boolean enabled;
	private final String userKey;
	private final long from;
	private final long to;
	private final boolean panoOnly;
	// Derived, but part of the identity below: the same millis land on different days per zone.
	private final LocalDate fromDate;
	private final LocalDate toDate;

	public PanoramaxFilterState(boolean enabled, @Nullable String userKey, long from, long to, boolean panoOnly) {
		this(enabled, userKey, from, to, panoOnly, ZoneId.systemDefault());
	}

	// The zone only resolves the day boundaries below; it is not part of the state itself.
	PanoramaxFilterState(boolean enabled, @Nullable String userKey, long from, long to, boolean panoOnly,
	                     @NonNull ZoneId zoneId) {
		this.enabled = enabled;
		this.userKey = enabled && userKey != null ? userKey : "";
		this.from = enabled ? from : 0;
		this.to = enabled ? to : 0;
		this.panoOnly = panoOnly;
		this.fromDate = toLocalDate(this.from, zoneId);
		this.toDate = toLocalDate(this.to, zoneId);
	}

	// Each boundary is the first or the last millisecond of a local day, so the day it was
	// picked for reads back exactly and a date only feature can be compared against it.
	@Nullable
	private static LocalDate toLocalDate(long time, @NonNull ZoneId zoneId) {
		return time == 0 ? null : Instant.ofEpochMilli(time).atZone(zoneId).toLocalDate();
	}

	@NonNull
	public static PanoramaxFilterState read(@NonNull PanoramaxPlugin plugin) {
		return new PanoramaxFilterState(
				plugin.USE_PANORAMAX_FILTER.get(),
				plugin.PANORAMAX_FILTER_USER_KEY.get(),
				plugin.PANORAMAX_FILTER_FROM_DATE.get(),
				plugin.PANORAMAX_FILTER_TO_DATE.get(),
				plugin.PANORAMAX_FILTER_PANO.get());
	}

	/**
	 * @return true when the tile feature must not be drawn.
	 */
	public boolean filtered(@Nullable Object data) {
		if (!(data instanceof Map<?, ?> userData)) {
			return true;
		}
		if (enabled) {
			if (!userKey.isEmpty()) {
				Object accountId = userData.get(ACCOUNT_ID_KEY);
				if (accountId == null || !userKey.equals(accountId.toString())) {
					return true;
				}
			}
			if (from != 0 || to != 0) {
				LocalDate date = PanoramaxImage.parseCaptureDate(userData);
				if (date != null) {
					if ((fromDate != null && date.isBefore(fromDate))
							|| (toDate != null && date.isAfter(toDate))) {
						return true;
					}
				} else {
					long capturedAt = PanoramaxImage.parseCaptureTime(userData);
					if ((from != 0 && capturedAt < from) || (to != 0 && capturedAt > to)) {
						return true;
					}
				}
			}
		}
		// The 360 filter applies whether or not the rest of the filter is enabled.
		if (panoOnly) {
			Object type = userData.get(TYPE_KEY);
			return type == null || !TYPE_EQUIRECTANGULAR.equalsIgnoreCase(type.toString());
		}
		return false;
	}

	/**
	 * Returns a versioned identity of the rendered state for the persistent raster cache.
	 */
	@NonNull
	public String getCacheKey() {
		return "v1;enabled=" + enabled
				+ ";pano=" + panoOnly
				+ ";from=" + from
				+ ";to=" + to
				+ ";fromDate=" + date(fromDate)
				+ ";toDate=" + date(toDate)
				+ ";user=" + userKey;
	}

	@NonNull
	private static String date(@Nullable LocalDate date) {
		return date == null ? "none" : date.toString();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof PanoramaxFilterState other)) {
			return false;
		}
		return enabled == other.enabled
				&& panoOnly == other.panoOnly
				&& from == other.from
				&& to == other.to
				&& userKey.equals(other.userKey)
				&& Objects.equals(fromDate, other.fromDate)
				&& Objects.equals(toDate, other.toDate);
	}

	@Override
	public int hashCode() {
		return Objects.hash(enabled, userKey, from, to, panoOnly, fromDate, toDate);
	}
}
