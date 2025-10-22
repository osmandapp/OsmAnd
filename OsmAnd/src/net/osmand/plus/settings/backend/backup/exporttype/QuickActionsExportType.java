package net.osmand.plus.settings.backend.backup.exporttype;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.download.local.LocalItemType;
import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.plus.quickaction.MapButtonsHelper;
import net.osmand.plus.settings.backend.ExportCategory;
import net.osmand.plus.settings.backend.backup.SettingsItemType;
import net.osmand.plus.settings.backend.backup.items.FileSettingsItem.FileSubtype;
import net.osmand.plus.settings.backend.backup.items.QuickActionsSettingsItem;
import net.osmand.plus.settings.backend.backup.items.SettingsItem;
import net.osmand.plus.views.mapwidgets.configure.buttons.ButtonStateBean;
import net.osmand.plus.views.mapwidgets.configure.buttons.QuickActionButtonState;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class QuickActionsExportType extends AbstractExportType {

	@Override
	public int getTitleId() {
		return R.string.shared_string_quick_actions;
	}

	@Override
	public int getIconId() {
		return R.drawable.ic_quick_action;
	}

	@NonNull
	@Override
	public List<?> fetchExportData(@NonNull OsmandApplication app, boolean offlineBackup) {
		List<ButtonStateBean> stateBeans = new ArrayList<>();
		MapButtonsHelper buttonsHelper = app.getMapButtonsHelper();
		List<QuickActionButtonState> buttonStates = buttonsHelper.getQuickActionButtonsStates();

		if (buttonStates.size() == 1) {
			QuickActionButtonState state = buttonStates.get(0);
			if (state.isDefaultButton() && Algorithms.isEmpty(state.getQuickActions())) {
				return Collections.emptyList();
			}
		}
		for (QuickActionButtonState buttonState : buttonStates) {
			stateBeans.add(ButtonStateBean.toStateBean(buttonState));
		}
		return stateBeans;
	}

	@NonNull
	@Override
	public List<?> fetchImportData(@NonNull SettingsItem settingsItem, boolean importCompleted) {
		QuickActionsSettingsItem item = (QuickActionsSettingsItem) settingsItem;
		return Collections.singletonList(item.getStateBean());
	}

	@Override
	public boolean isRelatedObject(@NonNull OsmandApplication app, @NonNull Object object) {
		return object instanceof ButtonStateBean;
	}

	@NonNull
	@Override
	public ExportCategory getRelatedExportCategory() {
		return ExportCategory.SETTINGS;
	}

	@NonNull
	@Override
	public SettingsItemType getRelatedSettingsItemType() {
		return SettingsItemType.QUICK_ACTIONS;
	}

	@NonNull
	@Override
	public List<FileSubtype> getRelatedFileSubtypes() {
		return Collections.emptyList();
	}

	@Nullable
	@Override
	public LocalItemType getRelatedLocalItemType() {
		return null;
	}
}
