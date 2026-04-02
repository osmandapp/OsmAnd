package net.osmand.plus.views.mapwidgets.widgetstates;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.util.Algorithms;

public class MemoryWidgetState extends WidgetState {

	private final OsmandPreference<MemoryInfoType> typePreference;

	public MemoryWidgetState(@NonNull OsmandApplication app, @Nullable String customId) {
		super(app);
		typePreference = registerTypePreference(customId);
	}

	@NonNull
	public MemoryInfoType getMemoryInfoType() {
		return typePreference.get();
	}

	@NonNull
	public OsmandPreference<MemoryInfoType> getMemoryInfoTypePref() {
		return typePreference;
	}

	@NonNull
	@Override
	public String getTitle() {
		return app.getString(R.string.widget_available_ram);
	}

	@Override
	public int getSettingsIconId(boolean nightMode) {
		return WidgetType.DEV_MEMORY.getIconId(nightMode);
	}

	@Override
	public void changeToNextState() {
		typePreference.set(getMemoryInfoType().next());
	}

	@Override
	public void copyPrefs(@NonNull ApplicationMode appMode, @Nullable String customId) {
		registerTypePreference(customId).setModeValue(appMode, getMemoryInfoType());
	}

	@Override
	public void copyPrefsFromMode(@NonNull ApplicationMode sourceAppMode, @NonNull ApplicationMode appMode, @Nullable String customId) {
		registerTypePreference(customId).setModeValue(appMode, typePreference.getModeValue(sourceAppMode));
	}

	@NonNull
	private OsmandPreference<MemoryInfoType> registerTypePreference(@Nullable String customId) {
		String prefId = "memory_info_type";
		if (!Algorithms.isEmpty(customId)) {
			prefId += customId;
		}
		return settings.registerEnumStringPreference(prefId, MemoryInfoType.USED, MemoryInfoType.values(), MemoryInfoType.class).makeProfile();
	}

	public enum MemoryInfoType {
		USED(R.string.memory_used, "Used"),
		AVAILABLE(R.string.widget_available_ram, "Avail"),
		ALLOCATED(R.string.global_app_allocated_memory, "Alloc"),
		LIMIT(R.string.memory_limit, "Limit"),
		NATIVE(R.string.native_app_allocated_memory, "Native"),
		GRAPHICS(R.string.memory_graphics, "GPU");

		@StringRes
		public final int titleId;
		@NonNull
		public final String shortName;

		MemoryInfoType(@StringRes int titleId, @NonNull String shortName) {
			this.titleId = titleId;
			this.shortName = shortName;
		}

		@NonNull
		public MemoryInfoType next() {
			int nextItemIndex = (ordinal() + 1) % values().length;
			return values()[nextItemIndex];
		}
	}
}