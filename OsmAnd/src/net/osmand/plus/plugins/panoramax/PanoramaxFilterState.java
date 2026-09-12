package net.osmand.plus.plugins.panoramax;

import static net.osmand.plus.plugins.panoramax.PanoramaxImage.ACCOUNT_ID_KEY;
import static net.osmand.plus.plugins.panoramax.PanoramaxImage.TYPE_EQUIRECTANGULAR;
import static net.osmand.plus.plugins.panoramax.PanoramaxImage.TYPE_KEY;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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

	public PanoramaxFilterState(boolean enabled, @Nullable String userKey, long from, long to, boolean panoOnly) {
		this.enabled = enabled;
		this.userKey = enabled && userKey != null ? userKey : "";
		this.from = enabled ? from : 0;
		this.to = enabled ? to : 0;
		this.panoOnly = panoOnly;
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
		if (!(data instanceof Map)) {
			return true;
		}
		Map<?, ?> userData = (Map<?, ?>) data;
		if (enabled) {
			if (!userKey.isEmpty()) {
				Object accountId = userData.get(ACCOUNT_ID_KEY);
				if (accountId == null || !userKey.equals(accountId.toString())) {
					return true;
				}
			}
			if (from != 0 || to != 0) {
				long capturedAt = PanoramaxImage.parseCaptureTime(userData);
				if ((from != 0 && capturedAt < from) || (to != 0 && capturedAt > to)) {
					return true;
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

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof PanoramaxFilterState)) {
			return false;
		}
		PanoramaxFilterState other = (PanoramaxFilterState) o;
		return enabled == other.enabled
				&& panoOnly == other.panoOnly
				&& from == other.from
				&& to == other.to
				&& userKey.equals(other.userKey);
	}

	@Override
	public int hashCode() {
		return Objects.hash(enabled, userKey, from, to, panoOnly);
	}
}
