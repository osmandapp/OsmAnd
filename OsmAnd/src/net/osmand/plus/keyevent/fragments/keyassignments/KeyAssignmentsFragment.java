package net.osmand.plus.keyevent.fragments.keyassignments;

import static net.osmand.plus.utils.AndroidUtils.getNavigationIconResId;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.transition.Hold;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseFullScreenFragment;
import net.osmand.plus.base.dialog.interfaces.dialog.IAskRefreshDialogCompletely;
import net.osmand.plus.keyevent.InputDevicesHelper;
import net.osmand.plus.keyevent.listener.EventType;
import net.osmand.plus.keyevent.listener.InputDevicesEventListener;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.widgets.dialogbutton.DialogButton;

public class KeyAssignmentsFragment extends BaseFullScreenFragment
		implements IAskRefreshDialogCompletely, InputDevicesEventListener {

	public static final String TAG = KeyAssignmentsFragment.class.getSimpleName();

	private KeyAssignmentsAdapter adapter;
	private KeyAssignmentsController controller;
	private InputDevicesHelper deviceHelper;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		deviceHelper = app.getInputDeviceHelper();
		controller = KeyAssignmentsController.getExistedInstance(app);
		if (controller != null) {
			controller.registerDialog(this);
		} else {
			dismiss();
		}
		setExitTransition(new Hold());
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
	                         @Nullable Bundle savedInstanceState) {
		updateNightMode();
		View view = inflate(R.layout.fragment_key_assignments_list, container, false);
		AndroidUtils.addStatusBarPadding21v(requireMyActivity(), view);
		setupToolbar(view);

		adapter = new KeyAssignmentsAdapter(app, appMode, controller);
		RecyclerView recyclerView = view.findViewById(R.id.recycler_view);
		recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
		recyclerView.setAdapter(adapter);
		updateScreen(view);
		return view;
	}

	private void setupToolbar(@NonNull View view) {
		Toolbar toolbar = view.findViewById(R.id.toolbar);
		ImageView closeButton = toolbar.findViewById(R.id.close_button);
		closeButton.setOnClickListener(v -> {
			if (controller.isInEditMode()) {
				controller.askExitEditMode(getActivity());
			} else {
				dismiss();
			}
		});
		toolbar.findViewById(R.id.toolbar_subtitle).setVisibility(View.GONE);

		View actionButton = toolbar.findViewById(R.id.action_button);
		if (controller.isDeviceTypeEditable()) {
			actionButton.setOnClickListener(v -> {
				if (controller.isInEditMode()) {
					controller.askRemoveAllAssignments();
				} else {
					controller.enterEditMode();
				}
			});
		} else {
			actionButton.setVisibility(View.GONE);
		}
		ViewCompat.setElevation(view.findViewById(R.id.appbar), 5.0f);
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
		updateToolbar(view);
		updateSaveButton(view);
		updateFabButton(view);
		updateListContent();
	}

	private void updateToolbar(@NonNull View view) {
		Toolbar toolbar = view.findViewById(R.id.toolbar);
		boolean editMode = controller.isInEditMode();

		ImageView closeButton = toolbar.findViewById(R.id.close_button);
		int navIconId = editMode ? R.drawable.ic_action_close : getNavigationIconResId(app);
		closeButton.setImageResource(navIconId);

		TextView title = toolbar.findViewById(R.id.toolbar_title);
		title.setText(editMode ? R.string.shared_string_edit : R.string.key_assignments);

		View actionButton = toolbar.findViewById(R.id.action_button);
		ImageButton ivActionButton = actionButton.findViewById(R.id.action_button_icon);

		boolean enabled = controller.hasAssignments() || editMode;
		int actionIconId = editMode ? R.drawable.ic_action_key_assignment_remove : R.drawable.ic_action_edit_outlined;
		int actionIconColor = enabled ? ColorUtilities.getPrimaryIconColor(app, nightMode) : ColorUtilities.getDisabledTextColor(app, nightMode);
		ivActionButton.setImageDrawable(getPaintedIcon(actionIconId, actionIconColor));
		actionButton.setEnabled(enabled);
	}

	private void updateSaveButton(@NonNull View view) {
		View bottomButtons = view.findViewById(R.id.bottom_buttons_container);
		bottomButtons.setVisibility(controller.isInEditMode() ? View.VISIBLE : View.GONE);
		DialogButton applyButton = view.findViewById(R.id.save_button);
		applyButton.setOnClickListener(v -> controller.askSaveChanges());
		applyButton.setEnabled(controller.hasChanges());
	}

	private void updateFabButton(@NonNull View view) {
		FloatingActionButton addButton = view.findViewById(R.id.fab);
		addButton.setVisibility(controller.isDeviceTypeEditable() && !controller.isInEditMode() ? View.VISIBLE : View.GONE);
		addButton.setOnClickListener(v -> controller.askAddAssignment(addButton));
	}

	private void updateListContent() {
		adapter.setScreenData(controller.populateScreenItems(), controller.isDeviceTypeEditable());
	}

	@Override
	public void onResume() {
		super.onResume();
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mapActivity.disableDrawer();
			controller.setActivity(mapActivity);
		}
		deviceHelper.addListener(this);
	}

	@Override
	public void onPause() {
		super.onPause();
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mapActivity.enableDrawer();
		}
		controller.setActivity(null);
		deviceHelper.removeListener(this);
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

	@Override
	public int getStatusBarColorId() {
		return ColorUtilities.getStatusBarSecondaryColorId(nightMode);
	}

	public boolean getContentStatusBarNightMode() {
		return nightMode;
	}

	public static boolean showInstance(@NonNull FragmentManager manager,
	                                   @NonNull ApplicationMode appMode) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			KeyAssignmentsFragment fragment = new KeyAssignmentsFragment();
			Bundle arguments = new Bundle();
			arguments.putString(APP_MODE_KEY, appMode.getStringKey());
			fragment.setArguments(arguments);
			manager.beginTransaction()
					.replace(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(TAG)
					.commitAllowingStateLoss();
			return true;
		}
		return false;
	}
}
