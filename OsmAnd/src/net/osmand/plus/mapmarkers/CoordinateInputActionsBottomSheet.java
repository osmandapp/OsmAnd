package net.osmand.plus.mapmarkers;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.mapmarkers.adapters.CoordinateInputAdapter;
import net.osmand.plus.utils.AndroidUtils;

public class CoordinateInputActionsBottomSheet extends MenuBottomSheetDialogFragment {

	public static final String TAG = CoordinateInputActionsBottomSheet.class.getSimpleName();

	private CoordinateInputActionsListener listener;

	public void setListener(CoordinateInputActionsListener listener) {
		this.listener = listener;
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		Bundle args = getArguments();
		if (args == null) {
			return;
		}
		int position = args.getInt(CoordinateInputAdapter.ADAPTER_POSITION_KEY);

		items.add(new TitleItem(getString(R.string.shared_string_actions)));

		BaseBottomSheetItem editItem = new SimpleBottomSheetItem.Builder()
				.setIcon(getContentIcon(R.drawable.ic_action_type_edit))
				.setTitle(getString(R.string.shared_string_edit))
				.setLayoutId(R.layout.bottom_sheet_item_simple)
				.setOnClickListener(v -> {
					listener.editItem(position);
					dismiss();
				})
				.create();
		items.add(editItem);

		BaseBottomSheetItem deleteItem = new SimpleBottomSheetItem.Builder()
				.setIcon(getContentIcon(R.drawable.ic_action_type_delete))
				.setTitle(getString(R.string.shared_string_delete))
				.setLayoutId(R.layout.bottom_sheet_item_simple)
				.setOnClickListener(v -> {
					listener.removeItem(position);
					dismiss();
				})
				.create();
		items.add(deleteItem);

	}

	@Override
	protected int getDismissButtonTextId() {
		return R.string.shared_string_cancel;
	}

	public static void showInstance(@NonNull FragmentManager childFragmentManager,
	                                @NonNull CoordinateInputActionsListener listener, int position) {
		if (AndroidUtils.isFragmentCanBeAdded(childFragmentManager, TAG)) {
			Bundle args = new Bundle();
			args.putInt(CoordinateInputAdapter.ADAPTER_POSITION_KEY, position);

			CoordinateInputActionsBottomSheet fragment = new CoordinateInputActionsBottomSheet();
			fragment.setUsedOnMap(false);
			fragment.setArguments(args);
			fragment.setListener(listener);
			fragment.show(childFragmentManager, CoordinateInputActionsBottomSheet.TAG);
		}
	}

	public interface CoordinateInputActionsListener {

		void removeItem(int position);

		void editItem(int position);
	}
}
