package net.osmand.plus.keyevent.fragments.editassignment;

import static net.osmand.plus.settings.fragments.BaseSettingsFragment.APP_MODE_KEY;
import static net.osmand.plus.utils.ColorUtilities.getPrimaryIconColor;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.keyevent.listener.EventType;
import net.osmand.plus.keyevent.listener.InputDevicesEventListener;
import net.osmand.plus.keyevent.fragments.selectkeycode.OnKeyCodeSelectedCallback;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;

public class EditKeyAssignmentFragment extends BaseOsmAndFragment
		implements OnKeyCodeSelectedCallback, InputDevicesEventListener {

	public static final String TAG = EditKeyAssignmentFragment.class.getSimpleName();

	private static final String ATTR_DEVICE_ID = "attr_device_id";
	private static final String ATTR_ASSIGNMENT_ID = "attr_key_assignment";

	private EditKeyAssignmentAdapter adapter;
	private EditKeyAssignmentController controller;
	private ApplicationMode appMode;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Fragment thisFragment = this;
		Bundle arguments = requireArguments();
		String appModeKey = arguments.getString(APP_MODE_KEY);
		appMode = ApplicationMode.valueOfStringKey(appModeKey, settings.getApplicationMode());
		String deviceId = arguments.getString(ATTR_DEVICE_ID, "");
		String assignmentId = arguments.getString(ATTR_ASSIGNMENT_ID, "");
		controller = new EditKeyAssignmentController(app, appMode, thisFragment, deviceId, assignmentId, isUsedOnMap());
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater,
	                         @Nullable ViewGroup container,
	                         @Nullable Bundle savedInstanceState) {
		updateNightMode();
		View view = inflate(R.layout.fragment_edit_key_assignment, container);
		AndroidUtils.addStatusBarPadding21v(requireMyActivity(), view);
		setupToolbar(view);

		adapter = new EditKeyAssignmentAdapter(app, appMode, controller, isUsedOnMap());
		RecyclerView recyclerView = view.findViewById(R.id.recycler_view);
		recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
		recyclerView.setAdapter(adapter);
		updateViewContent(view);
		return view;
	}

	private void setupToolbar(@NonNull View view) {
		AppBarLayout appBar = view.findViewById(R.id.appbar);
		appBar.setExpanded(AndroidUiHelper.isOrientationPortrait(requireActivity()));

		int color = getPrimaryIconColor(app, nightMode);
		Toolbar toolbar = view.findViewById(R.id.toolbar);
		toolbar.setNavigationIcon(getPaintedContentIcon(R.drawable.ic_action_close, color));
		toolbar.setNavigationContentDescription(R.string.shared_string_close);
		toolbar.setNavigationOnClickListener(v -> {
			dismiss();
		});

		toolbar.inflateMenu(R.menu.edit_key_assignment_menu);
		toolbar.setOnMenuItemClickListener(item -> {
			int itemId = item.getItemId();
			if (itemId == R.id.action_add_new_button) {
				controller.askAddKeyCode();
				return true;
			} else if (itemId == R.id.action_clear_key_assignment) {
				controller.askClearKeyCodes();
				return true;
			}
			return false;
		});
	}

	@Override
	public void onKeyCodeSelected(int oldKeyCode, int newKeyCode) {
		controller.addOrUpdateKeyCode(oldKeyCode, newKeyCode);
	}

	@Override
	public void processInputDevicesEvent(@NonNull ApplicationMode appMode, @NonNull EventType event) {
		View view = getView();
		if (view != null && event.isAssignmentRelated()) {
			updateViewContent(view);
		}
	}

	private void updateViewContent(@NonNull View view) {
		updateToolbarTitle(view);
		adapter.setScreenData(controller.populateScreenItems());
	}

	private void updateToolbarTitle(@NonNull View view) {
		CollapsingToolbarLayout collapsingToolbarLayout = view.findViewById(R.id.toolbar_layout);
		collapsingToolbarLayout.setTitle(controller.getCustomNameSummary());
	}

	@Override
	public void onResume() {
		super.onResume();
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mapActivity.disableDrawer();
			controller.setActivity(mapActivity);
		}
		app.getInputDeviceHelper().addListener(this);
	}

	@Override
	public void onPause() {
		super.onPause();
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mapActivity.enableDrawer();
		}
		controller.setActivity(null);
		app.getInputDeviceHelper().removeListener(this);
	}

	private void dismiss() {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			activity.onBackPressed();
		}
	}

	@Nullable
	private MapActivity getMapActivity() {
		return (MapActivity) getActivity();
	}

	@Override
	public int getStatusBarColorId() {
		AndroidUiHelper.setStatusBarContentColor(getView(), nightMode);
		return ColorUtilities.getStatusBarSecondaryColorId(nightMode);
	}

	@Override
	public boolean getContentStatusBarNightMode() {
		return nightMode;
	}

	public static void showInstance(@NonNull FragmentManager manager,
	                                @NonNull ApplicationMode appMode,
	                                @NonNull String deviceId,
	                                @NonNull String assignmentId) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			EditKeyAssignmentFragment fragment = new EditKeyAssignmentFragment();
			Bundle arguments = new Bundle();
			arguments.putString(ATTR_DEVICE_ID, deviceId);
			arguments.putString(ATTR_ASSIGNMENT_ID, assignmentId);
			arguments.putString(APP_MODE_KEY, appMode.getStringKey());
			fragment.setArguments(arguments);
			manager.beginTransaction()
					.replace(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(TAG)
					.commitAllowingStateLoss();
		}
	}
}
