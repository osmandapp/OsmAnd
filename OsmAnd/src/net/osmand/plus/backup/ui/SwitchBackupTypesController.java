package net.osmand.plus.backup.ui;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.backup.PrepareBackupResult.RemoteFilesType;
import net.osmand.plus.backup.UserNotRegisteredException;
import net.osmand.plus.backup.ui.ClearTypesBottomSheet.BackupClearType;
import net.osmand.plus.chooseplan.OsmAndProPlanFragment;
import net.osmand.plus.settings.backend.ExportCategory;
import net.osmand.plus.settings.backend.backup.exporttype.ExportType;
import net.osmand.plus.settings.fragments.SettingsCategoryItems;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public abstract class SwitchBackupTypesController extends BaseBackupTypesController {

	protected Map<ExportType, List<?>> selectedItemsMap = new EnumMap<>(ExportType.class);

	public SwitchBackupTypesController(@NonNull OsmandApplication app,
	                                   @NonNull BackupClearType clearType,
	                                   @NonNull RemoteFilesType remoteFilesType) {
		super(app, clearType, remoteFilesType);
	}

	@Override
	public void updateData() {
		super.updateData();
		this.selectedItemsMap = collectSelectedItems();
	}

	@NonNull
	@Override
	public BackupTypesAdapter createUiAdapter(@NonNull Context context) {
		return new SwitchBackupTypesAdapter(context, this);
	}

	@NonNull
	protected abstract Map<ExportType, List<?>> collectSelectedItems();

	@NonNull
	public Map<ExportType, List<?>> getSelectedItems() {
		return selectedItemsMap;
	}

	@NonNull
	public List<ExportType> getSelectedTypes(@NonNull List<ExportType> exportTypes) {
		List<ExportType> result = new ArrayList<>();
		for (ExportType exportType : exportTypes) {
			if (getSelectedItemsOfType(exportType) != null) {
				result.add(exportType);
			}
		}
		return result;
	}

	@Nullable
	public List<?> getSelectedItemsOfType(@NonNull ExportType exportType) {
		return selectedItemsMap.get(exportType);
	}

	@Override
	public void onCategorySelected(ExportCategory exportCategory, boolean selected) {
		SettingsCategoryItems categoryItems = getCategoryItems(exportCategory);
		List<ExportType> exportTypes = categoryItems.getTypes();
		applyTypesSelection(exportTypes, selected);
	}

	@Override
	public void onTypeSelected(@NonNull ExportType exportType, boolean selected) {
		if (isExportTypeAvailable(exportType)) {
			applyTypesSelection(Collections.singletonList(exportType), selected);
		} else {
			showProPlanFragment();
		}
	}

	protected void applyTypesSelection(@NonNull List<ExportType> exportTypes, boolean selected) {
		boolean hasItems = false;
		for (ExportType exportType : exportTypes) {
			if (isExportTypeAvailable(exportType)) {
				List<?> items = getItemsForType(exportType);
				selectedItemsMap.put(exportType, selected ? items : null);
				applyTypePreference(exportType, selected);
				hasItems |= !Algorithms.isEmpty(items);
			}
		}
		if (!selected && hasItems && shouldProposeToClearData()) {
			showClearTypesBottomSheet(exportTypes);
		}
	}

	protected abstract void applyTypePreference(@NonNull ExportType exportType, boolean selected);

	protected boolean shouldProposeToClearData() {
		return false;
	}

	protected void showClearTypesBottomSheet(@NonNull List<ExportType> types) {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			ClearTypesBottomSheet.showInstance(activity, this, types);
		}
	}

	protected void showProPlanFragment() {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			OsmAndProPlanFragment.showInstance(activity);
		}
	}

	@Override
	public void onClearTypesConfirmed(@NonNull List<ExportType> types) {
		try {
			screen.updateProgressVisibility(true);
			clearDataTypes(types);
		} catch (UserNotRegisteredException e) {
			screen.updateProgressVisibility(false);
			LOG.error(e);
		}
	}

	protected abstract void clearDataTypes(@NonNull List<ExportType> types) throws UserNotRegisteredException;
}
