package net.osmand.plus.plugins.osmedit.dialogs;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import net.osmand.osm.MapPoiTypes;
import net.osmand.osm.PoiCategory;
import net.osmand.osm.PoiType;
import net.osmand.plus.OsmandApplication;

import java.util.Set;
import java.util.TreeSet;

public class PoiSubTypeDialogFragment extends DialogFragment {
	private static final String KEY_POI_CATEGORY = "amenity";
	private OnItemSelectListener onItemSelectListener;

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		MapPoiTypes poiTypes = ((OsmandApplication) getActivity().getApplication()).getPoiTypes();
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		PoiCategory a = poiTypes.getPoiCategoryByName(getArguments().getString(KEY_POI_CATEGORY));
		Set<String> strings = new TreeSet<>();
		if(a == poiTypes.getOtherPoiCategory()) {
			for (PoiCategory category : poiTypes.getCategories(false)) {
				if (!category.isNotEditableOsm()) {
					addCategory(category, strings);
				}
			}
		} else {
			addCategory(a, strings);
		}
		String[] subCats = strings.toArray(new String[0]);
		builder.setItems(subCats, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				onItemSelectListener.select(subCats[which]);
				dismiss();
			}
		});
		return builder.create();
	}

	private void addCategory(PoiCategory a, Set<String> strings) {
		for (PoiType s : a.getPoiTypes()) {
			if (!s.isReference() && !s.isNotEditableOsm() && s.getBaseLangType() == null) {
				strings.add(s.getTranslation());
			}
		}
	}

	public static PoiSubTypeDialogFragment createInstance(PoiCategory cat) {
		PoiSubTypeDialogFragment fragment = new PoiSubTypeDialogFragment();
		Bundle args = new Bundle();
		args.putString(KEY_POI_CATEGORY, cat.getKeyName());
		fragment.setArguments(args);
		return fragment;
	}

	public void setOnItemSelectListener(OnItemSelectListener onItemSelectListener) {
		this.onItemSelectListener = onItemSelectListener;
	}

	public interface OnItemSelectListener {
		void select(String category);
	}
}