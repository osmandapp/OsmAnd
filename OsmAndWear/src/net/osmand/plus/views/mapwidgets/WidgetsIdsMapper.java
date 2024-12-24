package net.osmand.plus.views.mapwidgets;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.COLLAPSED_PREFIX;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.HIDE_PREFIX;

public class WidgetsIdsMapper {

	private final List<Replacement> replacements = new ArrayList<>();

	public void addReplacement(@NonNull String fromId, @NonNull String toId) {
		replacements.add(new Replacement(Collections.singletonList(fromId), toId));
	}

	public void addReplacement(@NonNull List<String> fromIds, @NonNull String toId) {
		replacements.add(new Replacement(fromIds, toId));
	}

	@Nullable
	public String mapId(@NonNull String widgetId) {
		for (Replacement replacement : replacements) {
			String replacedId = replacement.mapId(widgetId);
			if (replacedId != null) {
				boolean hidden = replacedId.startsWith(HIDE_PREFIX);
				if (!hidden && replacement.appliedVisible) {
					return null;
				}
				replacement.appliedVisible = !hidden;
				return replacedId;
			}
		}

		return widgetId;
	}

	public void resetAppliedVisibleReplacements() {
		for (Replacement replacement : replacements) {
			replacement.appliedVisible = false;
		}
	}

	private static class Replacement {

		private final List<String> fromIds;
		private final String toId;

		private boolean appliedVisible;

		public Replacement(@NonNull List<String> fromIds, @NonNull String toId) {
			this.fromIds = fromIds;
			this.toId = toId;
		}

		@Nullable
		public String mapId(@NonNull String fromId) {
			if (fromIds.contains(fromId)) {
				return toId;
			}

			if (fromId.indexOf(COLLAPSED_PREFIX) == 0) {
				String fromIdNoPrefix = fromId.substring(1);
				if (fromIds.contains(fromIdNoPrefix)) {
					return toId; // Without prefix, collapse is no longer supported
				}
			}

			if (fromId.indexOf(HIDE_PREFIX) == 0) {
				String fromIdNoPrefix = fromId.substring(1);
				if (fromIds.contains(fromIdNoPrefix)) {
					return HIDE_PREFIX + toId;
				}
			}

			return null;
		}
	}
}