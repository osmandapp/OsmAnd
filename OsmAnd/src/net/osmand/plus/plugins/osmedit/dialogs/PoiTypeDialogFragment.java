package net.osmand.plus.plugins.osmedit.dialogs;

import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentManager;

import net.osmand.osm.MapPoiTypes;
import net.osmand.osm.PoiCategory;
import net.osmand.plus.base.BaseAlertDialogFragment;
import net.osmand.plus.utils.AndroidUtils;

import java.util.ArrayList;
import java.util.List;

public class PoiTypeDialogFragment extends BaseAlertDialogFragment {

	private static final String TAG = PoiTypeDialogFragment.class.getSimpleName();

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

	public void setOnItemSelectListener(OnItemSelectListener onItemSelectListener) {
		this.onItemSelectListener = onItemSelectListener;
	}

	public static void showInstance(@NonNull FragmentManager fragmentManager,
	                                @NonNull OnItemSelectListener listener) {
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			PoiTypeDialogFragment fragment = new PoiTypeDialogFragment();
			Bundle args = new Bundle();
			fragment.setArguments(args);
			fragment.setOnItemSelectListener(listener);
			fragment.show(fragmentManager, TAG);
		}
	}

	public interface OnItemSelectListener {
		void select(PoiCategory poiCategory);
	}
}