package net.osmand.plus.osmedit.dialogs;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;

import net.osmand.data.Amenity;
import net.osmand.osm.MapPoiTypes;
import net.osmand.osm.PoiCategory;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.osmedit.EditPoiDialogFragment;

import java.util.ArrayList;
import java.util.List;

public class PoiTypeDialogFragment extends DialogFragment {

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		MapPoiTypes poiTypes = ((OsmandApplication) getActivity().getApplication()).getPoiTypes();
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		final List<PoiCategory> categories = poiTypes.getCategories(false);
		ArrayList<String> vals = new ArrayList<>(categories.size());
		ArrayList<PoiCategory> toDelete = new ArrayList<>();
		for (PoiCategory category : categories) {
			if (!category.isNotEditableOsm()) {
				toDelete.add(category);
			} else {
				vals.add(category.getTranslation());
			}
		}
		categories.removeAll(toDelete);
		builder.setItems(vals.toArray(new String[vals.size()]), new Dialog.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				PoiCategory aType = categories.get(which);
				((EditPoiDialogFragment) getParentFragment()).updateType(aType);
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
}