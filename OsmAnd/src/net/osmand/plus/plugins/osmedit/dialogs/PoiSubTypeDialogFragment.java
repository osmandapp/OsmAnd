package net.osmand.plus.plugins.osmedit.dialogs;

import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentManager;

import net.osmand.osm.MapPoiTypes;
import net.osmand.osm.PoiCategory;
import net.osmand.osm.PoiType;
import net.osmand.plus.base.BaseAlertDialogFragment;
import net.osmand.plus.utils.AndroidUtils;

import java.util.Set;
import java.util.TreeSet;

public class PoiSubTypeDialogFragment extends BaseAlertDialogFragment {

	private static final String TAG = PoiSubTypeDialogFragment.class.getSimpleName();
	private static final String KEY_POI_CATEGORY = "amenity";
	private OnItemSelectListener onItemSelectListener;

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		updateNightMode();
		MapPoiTypes poiTypes = app.getPoiTypes();
		AlertDialog.Builder builder = createDialogBuilder();
		PoiCategory a = poiTypes.getPoiCategoryByName(requireArguments().getString(KEY_POI_CATEGORY));
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
		builder.setItems(subCats, (dialog, which) -> {
			onItemSelectListener.select(subCats[which]);
			dismiss();
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

	public void setOnItemSelectListener(OnItemSelectListener onItemSelectListener) {
		this.onItemSelectListener = onItemSelectListener;
	}

	public static void showInstance(@NonNull FragmentManager childFragmentManager,
	                                @NonNull PoiCategory category,
	                                @NonNull OnItemSelectListener listener) {
		if (AndroidUtils.isFragmentCanBeAdded(childFragmentManager, TAG)) {
			PoiSubTypeDialogFragment fragment = new PoiSubTypeDialogFragment();
			Bundle args = new Bundle();
			args.putString(KEY_POI_CATEGORY, category.getKeyName());
			fragment.setArguments(args);
			fragment.setOnItemSelectListener(listener);
			fragment.show(childFragmentManager, TAG);
		}
	}

	public interface OnItemSelectListener {
		void select(String category);
	}
}