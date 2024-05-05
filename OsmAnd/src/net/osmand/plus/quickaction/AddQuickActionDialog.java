package net.osmand.plus.quickaction;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.mapwidgets.configure.buttons.QuickActionButtonState;

import java.util.List;

/**
 * Created by rosty on 12/22/16.
 */

public class AddQuickActionDialog extends MenuBottomSheetDialogFragment {

	private static final String TAG = AddQuickActionDialog.class.getSimpleName();
	public static final String QUICK_ACTION_BUTTON_KEY = "quick_action_button_key";

	private MapButtonsHelper mapButtonsHelper;
	private QuickActionButtonState buttonState;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		OsmandApplication app = requiredMyApplication();
		mapButtonsHelper = app.getMapButtonsHelper();

		Bundle args = getArguments();
		String key = args != null ? args.getString(QUICK_ACTION_BUTTON_KEY) : null;
		if (key != null) {
			buttonState = mapButtonsHelper.getButtonStateById(key);
		}
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		items.add(new TitleItem(getString(R.string.dialog_add_action_title)));

		LayoutInflater inflater = UiUtilities.getInflater(requireContext(), nightMode);
		List<QuickActionType> actions = mapButtonsHelper.produceTypeActionsListWithHeaders(buttonState);
		boolean firstHeader = true;
		for (QuickActionType type : actions) {
			if (type.getId() == 0) {
				View itemView = inflater.inflate(R.layout.quick_action_add_dialog_header, null, false);
				TextView title = itemView.findViewById(R.id.header);
				View divider = itemView.findViewById(R.id.divider);
				title.setText(type.getNameRes());
				divider.setVisibility(firstHeader ? View.GONE : View.VISIBLE);
				items.add(new BaseBottomSheetItem.Builder()
						.setCustomView(itemView)
						.create());
				firstHeader = false;
			} else {
				addActionItem(type, inflater);
			}
		}
	}

	private void addActionItem(@NonNull QuickActionType type, @NonNull LayoutInflater inflater) {
		View itemView = inflater.inflate(R.layout.quick_action_add_dialog_item, null, false);
		TextView title = itemView.findViewById(R.id.title);
		ImageView icon = itemView.findViewById(R.id.image);
		if (type.getActionNameRes() != 0) {
			String name = getString(type.getNameRes());
			String actionName = getString(type.getActionNameRes());
			title.setText(getString(R.string.ltr_or_rtl_combine_via_dash, actionName, name));
		} else {
			title.setText(type.getNameRes());
		}
		icon.setImageResource(type.getIconRes());
		items.add(new BaseBottomSheetItem.Builder()
				.setCustomView(itemView)
				.setOnClickListener(view -> {
					FragmentActivity activity = getActivity();
					if (activity != null) {
						FragmentManager manager = activity.getSupportFragmentManager();
						CreateEditActionDialog.showInstance(manager, buttonState, type.getId());
					}
					dismiss();
				}).create());
	}

	public static void showInstance(@NonNull FragmentManager manager,
	                                @NonNull QuickActionButtonState buttonState, boolean usedOnMap) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			Bundle bundle = new Bundle();
			bundle.putString(QUICK_ACTION_BUTTON_KEY, buttonState.getId());

			AddQuickActionDialog fragment = new AddQuickActionDialog();
			fragment.setArguments(bundle);
			fragment.setUsedOnMap(usedOnMap);
			fragment.show(manager, TAG);
		}
	}
}
