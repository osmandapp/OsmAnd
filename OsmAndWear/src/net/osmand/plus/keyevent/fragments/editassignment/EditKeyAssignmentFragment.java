package net.osmand.plus.keyevent.fragments.editassignment;

import static net.osmand.plus.keyevent.fragments.editassignment.EditKeyAssignmentController.TRANSITION_NAME;
import static net.osmand.plus.settings.fragments.BaseSettingsFragment.APP_MODE_KEY;
import static net.osmand.plus.utils.ColorUtilities.getPrimaryIconColor;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.base.dialog.interfaces.dialog.IAskDismissDialog;
import net.osmand.plus.base.dialog.interfaces.dialog.IAskRefreshDialogCompletely;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.keyevent.listener.EventType;
import net.osmand.plus.keyevent.listener.InputDevicesEventListener;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.widgets.dialogbutton.DialogButton;

public class EditKeyAssignmentFragment extends BaseOsmAndFragment
		implements IAskRefreshDialogCompletely, IAskDismissDialog, InputDevicesEventListener {

	public static final String TAG = EditKeyAssignmentFragment.class.getSimpleName();

	private EditKeyAssignmentAdapter adapter;
	private EditKeyAssignmentController controller;
	private ApplicationMode appMode;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Bundle arguments = requireArguments();
		String appModeKey = arguments.getString(APP_MODE_KEY);
		appMode = ApplicationMode.valueOfStringKey(appModeKey, settings.getApplicationMode());
		controller = EditKeyAssignmentController.getExistedInstance(app);
		if (controller != null) {
			controller.registerDialog(this);
		} else {
			dismiss();
		}
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater,
	                         @Nullable ViewGroup container,
	                         @Nullable Bundle savedInstanceState) {
		updateNightMode();
		View view = inflate(R.layout.fragment_edit_key_assignment, container);
		AndroidUtils.addStatusBarPadding21v(requireMyActivity(), view);
		if (!settings.DO_NOT_USE_ANIMATIONS.getModeValue(appMode)) {
			AndroidUiHelper.setSharedElementTransition(this, view, TRANSITION_NAME);
		}
		setupToolbar(view);

		adapter = new EditKeyAssignmentAdapter((MapActivity) requireMyActivity(), appMode, controller, isUsedOnMap());
		RecyclerView recyclerView = view.findViewById(R.id.recycler_view);
		recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
		recyclerView.setAdapter(adapter);
		updateScreen(view);
		return view;
	}

	private void setupToolbar(@NonNull View view) {
		AppBarLayout appBar = view.findViewById(R.id.appbar);
		appBar.setExpanded(AndroidUiHelper.isOrientationPortrait(requireActivity()));

		Toolbar toolbar = view.findViewById(R.id.toolbar);
		toolbar.setNavigationIcon(getNavigationIcon());
		toolbar.setNavigationContentDescription(R.string.shared_string_exit);
		toolbar.setNavigationOnClickListener(v -> {
			if (controller.isInEditMode()) {
				controller.askExitEditMode(getActivity());
			} else {
				dismiss();
			}
		});

		toolbar.inflateMenu(R.menu.key_assignment_overview_menu);
		toolbar.setOnMenuItemClickListener(item -> {
			int itemId = item.getItemId();
			if (itemId == R.id.action_edit) {
				controller.enterEditMode();
				return true;
			} else if (itemId == R.id.action_overflow_menu) {
				FragmentActivity activity = getActivity();
				if (activity != null) {
					View itemView = view.findViewById(R.id.action_overflow_menu);
					controller.showOverflowMenu(activity, itemView);
				}
				return true;
			}
			return false;
		});
	}

	@Override
	public void processInputDevicesEvent(@NonNull ApplicationMode appMode, @NonNull EventType event) {
		if (event.isAssignmentRelated()) {
			askUpdateScreen();
		}
	}

	@Override
	public void onAskRefreshDialogCompletely(@NonNull String processId) {
		askUpdateScreen();
	}

	private void askUpdateScreen() {
		View view = getView();
		if (view != null) {
			updateScreen(view);
		}
	}

	private void updateScreen(@NonNull View view) {
		Toolbar toolbar = view.findViewById(R.id.toolbar);
		toolbar.setNavigationIcon(getNavigationIcon());
		boolean editMode = controller.isInEditMode();
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.action_edit), !editMode);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.action_overflow_menu), !editMode);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.bottom_buttons), editMode);
		updateToolbarTitle(view);
		updateSaveButton(view);
		updateViewContent();
	}

	@NonNull
	private Drawable getNavigationIcon() {
		int color = getPrimaryIconColor(app, nightMode);
		int navIconId = controller.isInEditMode() ? R.drawable.ic_action_close : AndroidUtils.getNavigationIconResId(app);
		return getPaintedContentIcon(navIconId, color);
	}

	private void updateViewContent() {
		adapter.setScreenData(controller.populateScreenItems());
	}

	private void updateToolbarTitle(@NonNull View view) {
		CollapsingToolbarLayout collapsingToolbarLayout = view.findViewById(R.id.toolbar_layout);
		collapsingToolbarLayout.setTitle(controller.getDialogTitle());
	}

	private void updateSaveButton(@NonNull View view) {
		DialogButton saveButton = view.findViewById(R.id.save_button);
		saveButton.setEnabled(controller.hasChangesToSave());
		saveButton.setOnClickListener(v -> {
			controller.askSaveChanges();
		});
	}

	@Override
	public void onAskDismissDialog(@NonNull String processId) {
		dismiss();
	}

	@Override
	public void onResume() {
		super.onResume();
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mapActivity.disableDrawer();
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
		app.getInputDeviceHelper().removeListener(this);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		controller.finishProcessIfNeeded(getActivity());
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
		return ColorUtilities.getStatusBarSecondaryColorId(nightMode);
	}

	@Override
	public boolean getContentStatusBarNightMode() {
		return nightMode;
	}

	public static boolean showInstance(@NonNull FragmentActivity activity,
	                                   @NonNull ApplicationMode appMode,
	                                   @Nullable View anchorView) {
		OsmandApplication app = (OsmandApplication) activity.getApplicationContext();
		OsmandSettings settings = app.getSettings();
		FragmentManager manager = activity.getSupportFragmentManager();
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			EditKeyAssignmentFragment fragment = new EditKeyAssignmentFragment();
			Bundle arguments = new Bundle();
			arguments.putString(APP_MODE_KEY, appMode.getStringKey());
			fragment.setArguments(arguments);

			FragmentTransaction transaction = manager.beginTransaction();
			if (anchorView != null && !settings.DO_NOT_USE_ANIMATIONS.getModeValue(appMode)) {
				transaction.addSharedElement(anchorView, TRANSITION_NAME);
			}
			transaction.replace(R.id.fragmentContainer, fragment, TAG);
			transaction.addToBackStack(TAG);
			transaction.commitAllowingStateLoss();
			return true;
		}
		return false;
	}
}
