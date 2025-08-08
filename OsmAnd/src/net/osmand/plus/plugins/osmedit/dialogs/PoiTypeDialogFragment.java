package net.osmand.plus.plugins.osmedit.dialogs;

import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import net.osmand.osm.MapPoiTypes;
import net.osmand.osm.PoiCategory;
import net.osmand.plus.base.BaseAlertDialogFragment;

import java.util.ArrayList;
import java.util.List;

public class PoiTypeDialogFragment extends BaseAlertDialogFragment {
	private OnItemSelectListener onItemSelectListener;

	@NonNull
	@Override
	public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
		updateNightMode();
		AlertDialog.Builder builder = createDialogBuilder();
		MapPoiTypes poiTypes = app.getPoiTypes();
		List<PoiCategory> categories = new ArrayList<PoiCategory>();
		ArrayList<String> vals = new ArrayList<>();
		for (PoiCategory category : poiTypes.getCategories(false)) {
			if (!category.isNotEditableOsm()) {
				vals.add(category.getTranslation());
				categories.add(category);
			}
		}
		builder.setItems(vals.toArray(new String[0]), (dialog, which) -> {
			PoiCategory aType = categories.get(which);
			onItemSelectListener.select(aType);
			dismiss();
		});
		return builder.create();
	}

	public static PoiTypeDialogFragment createInstance() {
		PoiTypeDialogFragment poiTypeDialogFragment = new PoiTypeDialogFragment();
		Bundle args = new Bundle();
		poiTypeDialogFragment.setArguments(args);
		return poiTypeDialogFragment;
	}

	public void setOnItemSelectListener(OnItemSelectListener onItemSelectListener) {
		this.onItemSelectListener = onItemSelectListener;
	}

	public interface OnItemSelectListener {
		void select(PoiCategory poiCategory);
	}
}