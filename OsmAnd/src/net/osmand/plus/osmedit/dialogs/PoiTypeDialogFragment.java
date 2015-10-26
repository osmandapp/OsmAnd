package net.osmand.plus.osmedit.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;

import net.osmand.data.Amenity;
import net.osmand.osm.MapPoiTypes;
import net.osmand.osm.PoiCategory;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.osmedit.EditPoiDialogFragment;

import java.util.ArrayList;
import java.util.List;

public class PoiTypeDialogFragment extends DialogFragment {
	private static final String KEY_AMENITY = "amenity";

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		MapPoiTypes poiTypes = ((OsmandApplication) getActivity().getApplication()).getPoiTypes();
		final Amenity amenity = (Amenity) getArguments().getSerializable(KEY_AMENITY);
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		final List<PoiCategory> categories = poiTypes.getCategories(false);
		ArrayList<String> vals = new ArrayList<>(categories.size());
		ArrayList<PoiCategory> toDelete = new ArrayList<>();
		// TODO replace with constants
		for (PoiCategory category : categories) {
			if (category.getKeyName().equals("user_defined_other")
					|| category.getKeyName().equals("osmwiki")) {
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
				if (aType != amenity.getType()) {
					amenity.setType(aType);
					amenity.setSubType(""); //$NON-NLS-1$
					((EditPoiDialogFragment) getParentFragment()).updateType(amenity);
				}
				dismiss();
			}
		});
		return builder.create();
	}

	public static PoiTypeDialogFragment createInstance(Amenity amenity) {
		PoiTypeDialogFragment poiTypeDialogFragment = new PoiTypeDialogFragment();
		Bundle args = new Bundle();
		args.putSerializable(KEY_AMENITY, amenity);
		poiTypeDialogFragment.setArguments(args);
		return poiTypeDialogFragment;
	}
}