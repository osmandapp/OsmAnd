package net.osmand.plus.plugins.osmedit.dialogs;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import net.osmand.osm.MapPoiTypes;
import net.osmand.osm.PoiCategory;
import net.osmand.plus.OsmandApplication;

import java.util.ArrayList;
import java.util.List;

public class PoiTypeDialogFragment extends DialogFragment {
	private OnItemSelectListener onItemSelectListener;

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		MapPoiTypes poiTypes = ((OsmandApplication) getActivity().getApplication()).getPoiTypes();
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		List<PoiCategory> categories = new ArrayList<PoiCategory>();
		ArrayList<String> vals = new ArrayList<>();
		for (PoiCategory category : poiTypes.getCategories(false)) {
			if (!category.isNotEditableOsm()) {
				vals.add(category.getTranslation());
				categories.add(category);
			}
		}
		builder.setItems(vals.toArray(new String[0]), new Dialog.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				PoiCategory aType = categories.get(which);
				onItemSelectListener.select(aType);
				dismiss();
			}
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