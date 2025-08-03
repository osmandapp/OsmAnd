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
	public SwitchBackupTypesAdapter createUiAdapter(@NonNull Context context) {
		return new SwitchBackupTypesAdapter(context, this);
	}

	@NonNull
	protected abstract Map<ExportType, List<?>> collectSelectedItems();

	@NonNull
	public Map<ExportType, List<?>> getSelectedItems() {
		return selectedItemsMap;
	}

	public boolean hasSelectedItemsOfType(@NonNull ExportType exportType) {
		return getSelectedItemsOfType(exportType) != null;
	}

	@Nullable
	public List<?> getSelectedItemsOfType(@NonNull ExportType exportType) {
		return selectedItemsMap.get(exportType);
	}

	@Override
	public void onCategorySelected(ExportCategory exportCategory, boolean selected) {
		boolean hasItemsToDelete = false;
		SettingsCategoryItems categoryItems = data.get(exportCategory);
		List<ExportType> exportTypes = categoryItems.getTypes();
		for (ExportType exportType : exportTypes) {
			if (isExportTypeAvailable(exportType)) {
				List<?> items = getItemsForType(exportType);
				hasItemsToDelete |= !Algorithms.isEmpty(items);
				selectedItemsMap.put(exportType, selected ? items : null);
			}
		}
		if (!selected && hasItemsToDelete) {
			showClearTypesBottomSheet(exportTypes);
		}
	}

	@Override
	public void onTypeSelected(@NonNull ExportType exportType, boolean selected) {
		if (isExportTypeAvailable(exportType)) {
			List<?> items = getItemsForType(exportType);
			selectedItemsMap.put(exportType, selected ? items : null);
			if (!selected && !Algorithms.isEmpty(items)) {
				showClearTypesBottomSheet(Collections.singletonList(exportType));
			}
		} else {
			FragmentActivity activity = getActivity();
			if (activity != null) {
				OsmAndProPlanFragment.showInstance(activity);
			}
		}
	}

	// TODO: delete
	protected void showClearTypesBottomSheet(List<ExportType> types) {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			ClearTypesBottomSheet.showInstance(activity, this, types);
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
