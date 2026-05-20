package net.osmand.plus.views.mapwidgets.widgetstates;

import android.content.Context;

import androidx.annotation.DrawableRes;
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
		return app.getString(R.string.map_widget_memory_info);
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
		USED(R.string.memory_used_settings, R.string.shared_string_heap, R.string.shared_string_used),
		ALLOCATED(R.string.memory_allocated_settings, R.string.shared_string_heap, R.string.shared_string_allocated),
		LIMIT(R.string.memory_limit_settings, R.string.shared_string_heap, R.string.shared_string_limit),
		NATIVE(R.string.memory_native_settings, R.string.shared_string_native, R.string.shared_string_allocated),
		GRAPHICS(R.string.memory_graphics_settings, R.string.shared_string_gpu, R.string.shared_string_memory);

		@StringRes
		public final int titleId;
		@StringRes
		public final int prefixId;
		@StringRes
		public final int suffixId;

		MemoryInfoType(@StringRes int titleId, @StringRes int prefixId, @StringRes int suffixId) {
			this.titleId = titleId;
			this.prefixId = prefixId;
			this.suffixId = suffixId;
		}

		@DrawableRes
		public int getIconId(boolean nightMode) {
			if (this == GRAPHICS) {
				return nightMode ? R.drawable.widget_developer_memory_gpu_night : R.drawable.widget_developer_memory_gpu_day;
			}
			return nightMode ? R.drawable.widget_developer_ram_night : R.drawable.widget_developer_ram_day;
		}

		@NonNull
		public String getMapLabel(@NonNull Context ctx) {
			return ctx.getString(R.string.ltr_or_rtl_combine_via_colon, ctx.getString(prefixId), ctx.getString(suffixId));
		}

		@NonNull
		public MemoryInfoType next() {
			int nextItemIndex = (ordinal() + 1) % values().length;
			return values()[nextItemIndex];
		}
	}
}