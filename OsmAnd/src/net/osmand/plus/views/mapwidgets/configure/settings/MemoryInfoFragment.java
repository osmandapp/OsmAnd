package net.osmand.plus.views.mapwidgets.configure.settings;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.plugins.development.widget.MemoryInfoWidget;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.plus.views.mapwidgets.widgetstates.MemoryWidgetState.MemoryInfoType;
import net.osmand.plus.widgets.alert.AlertDialogData;
import net.osmand.plus.widgets.alert.CustomAlert;

public class MemoryInfoFragment extends BaseSimpleWidgetInfoFragment {

	private static final String MEMORY_INFO_TYPE_KEY = "memory_info_type";

	private OsmandPreference<MemoryInfoType> memoryInfoTypePref;
	private MemoryInfoType memoryInfoType;

	@NonNull
	@Override
	public WidgetType getWidget() {
		return WidgetType.DEV_MEMORY;
	}

	@Override
	protected void initParams(@NonNull Bundle bundle) {
		super.initParams(bundle);
		if (widgetInfo != null) {
			MemoryInfoWidget widget = (MemoryInfoWidget) widgetInfo.widget;
			memoryInfoTypePref = widget.getMemoryInfoTypePref();
			memoryInfoType = MemoryInfoType.values()[bundle.getInt(MEMORY_INFO_TYPE_KEY, memoryInfoTypePref.get().ordinal())];
		} else {
			dismiss();
		}
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt(MEMORY_INFO_TYPE_KEY, memoryInfoType.ordinal());
	}

	@Override
	protected void applySettings() {
		super.applySettings();
		memoryInfoTypePref.setModeValue(appMode, memoryInfoType);
	}

	@Override
	protected void setupMainContent(@NonNull ViewGroup container) {
		inflate(R.layout.fragment_widget_settings_memory, container);

		View modeContainer = container.findViewById(R.id.mode_container);
		modeContainer.setOnClickListener(v -> showMemoryInfoTypeSelectionDialog());
		modeContainer.setBackground(getPressedStateDrawable());

		updateModeUI();
	}

	private void showMemoryInfoTypeSelectionDialog() {
		Context context = getContext();
		if (context == null) {
			return;
		}
		CharSequence[] items = new CharSequence[MemoryInfoType.values().length];
		for (MemoryInfoType type : MemoryInfoType.values()) {
			items[type.ordinal()] = getString(type.titleId);
		}
		AlertDialogData dialogData = new AlertDialogData(context, nightMode)
				.setTitle(R.string.shared_string_mode)
				.setControlsColor(ColorUtilities.getActiveColor(app, nightMode));

		CustomAlert.showSingleSelection(dialogData, items, memoryInfoType.ordinal(), v -> {
			int which = (int) v.getTag();
			memoryInfoType = MemoryInfoType.values()[which];
			updateModeUI();
		});
	}

	private void updateModeUI() {
		ImageView modeIcon = view.findViewById(R.id.mode_icon);
		TextView selectedType = view.findViewById(R.id.selected_mode_type);

		selectedType.setText(memoryInfoType.titleId);
		modeIcon.setImageResource(memoryInfoType.getIconId(nightMode));
	}
}