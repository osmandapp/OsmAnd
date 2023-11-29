package net.osmand.plus.keyevent.fragments;

import static net.osmand.plus.settings.fragments.BaseSettingsFragment.APP_MODE_KEY;
import static net.osmand.plus.utils.AndroidUtils.setBackground;
import static net.osmand.plus.utils.ColorUtilities.getActiveColor;
import static net.osmand.plus.utils.ColorUtilities.getPrimaryIconColor;
import static net.osmand.plus.utils.UiUtilities.getColoredSelectableDrawable;

import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;

import net.osmand.CallbackWithObject;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.keyevent.InputDevicesHelper;
import net.osmand.plus.keyevent.listener.EventType;
import net.osmand.plus.keyevent.listener.InputDevicesEventListener;
import net.osmand.plus.keyevent.assignment.KeyAssignment;
import net.osmand.plus.keyevent.fragments.selectkeycode.OnKeyCodeSelectedCallback;
import net.osmand.plus.keyevent.fragments.selectkeycode.SelectKeyCodeFragment;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.widgets.alert.AlertDialogData;
import net.osmand.plus.widgets.alert.AlertDialogExtra;
import net.osmand.plus.widgets.alert.CustomAlert;
import net.osmand.util.Algorithms;

import java.util.List;
import java.util.Objects;

public class EditKeyAssignmentFragment extends BaseOsmAndFragment
		implements OnKeyCodeSelectedCallback, InputDevicesEventListener {

	public static final String TAG = EditKeyAssignmentFragment.class.getSimpleName();

	private static final String ATTR_DEVICE_ID = "attr_device_id";
	private static final String ATTR_ASSIGNMENT_ID = "attr_key_assignment";

	private ApplicationMode appMode;
	private InputDevicesHelper deviceHelper;

	private String deviceId;
	private String assignmentId;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		deviceHelper = app.getInputDeviceHelper();
		Bundle arguments = requireArguments();
		deviceId = arguments.getString(ATTR_DEVICE_ID);
		assignmentId = arguments.getString(ATTR_ASSIGNMENT_ID);
		String appModeKey = arguments.getString(APP_MODE_KEY);
		appMode = ApplicationMode.valueOfStringKey(appModeKey, settings.getApplicationMode());
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater,
	                         @Nullable ViewGroup container,
	                         @Nullable Bundle savedInstanceState) {
		updateNightMode();
		View view = themedInflater.inflate(R.layout.fragment_edit_key_action, container, false);
		AndroidUtils.addStatusBarPadding21v(requireMyActivity(), view);

		setupToolbar(view);
		updateViewContent(view, getAssignment());
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
			if (item.getItemId() == R.id.action_clear_key_assignment) {
				showRemoveKeyAssignmentDialog();
				return true;
			}
			return false;
		});
	}

	private void setupActionNameRow(@NonNull View view, @NonNull KeyAssignment assignment) {
		View customNameButton = view.findViewById(R.id.custom_name_button);
		TextView title = customNameButton.findViewById(R.id.title);
		title.setText(R.string.shared_string_name);

		String name = assignment.getName(app);
		TextView summary = customNameButton.findViewById(R.id.description);
		summary.setText(name);

		customNameButton.setOnClickListener(v -> showEnterNameDialog(name, newName -> {
			onNameEntered(newName);
			return true;
		}));
		View backgroundView = customNameButton.findViewById(R.id.selectable_list_item);
		setupSelectableBackground(backgroundView, appMode.getProfileColor(nightMode));
	}

	private void setupActionTypeRow(@NonNull View view, @NonNull KeyAssignment assignment) {
		View actionButton = view.findViewById(R.id.action_button);
		TextView title = actionButton.findViewById(R.id.title);
		title.setText(R.string.shared_string_action);
		TextView summary = actionButton.findViewById(R.id.description);
		summary.setText(assignment.getCommandTitle(app));
	}

	private void setupKeyButtonRow(@NonNull View view, @NonNull KeyAssignment assignment) {
		View keyButton = view.findViewById(R.id.key_button);
		TextView title = keyButton.findViewById(R.id.title);
		title.setText(R.string.shared_string_button);

		TextView summary = keyButton.findViewById(R.id.description);
		List<String> keyLabels = assignment.getKeyLabels(app);
		String fullSummary = TextUtils.join(", ", keyLabels);
		summary.setText(fullSummary);
		summary.setTextColor(getActiveColor(app, nightMode));
		summary.setTypeface(summary.getTypeface(), Typeface.BOLD);

		keyButton.setOnClickListener(v -> {
			FragmentActivity activity = getActivity();
			if (activity != null) {
				Fragment thisFragment = EditKeyAssignmentFragment.this;
				FragmentManager fm = activity.getSupportFragmentManager();
				int keyCode = Algorithms.isEmpty(assignment.getKeyCodes()) ? KeyEvent.KEYCODE_UNKNOWN : assignment.getKeyCodes().get(0);
				SelectKeyCodeFragment.showInstance(fm, thisFragment, appMode, deviceId, assignment.getCommandId(), keyCode);
			}
		});
		View backgroundView = keyButton.findViewById(R.id.selectable_list_item);
		setupSelectableBackground(backgroundView, appMode.getProfileColor(nightMode));
		AndroidUiHelper.updateVisibility(keyButton.findViewById(R.id.bottom_divider), false);
	}

	private void showEnterNameDialog(@Nullable String oldName,
	                                 @NonNull CallbackWithObject<String> callback) {
		FragmentActivity activity = getActivity();
		if (activity == null) return;

		AlertDialogData dialogData = new AlertDialogData(activity, nightMode)
				.setTitle(getString(R.string.shared_string_name))
				.setControlsColor(getActiveColor(activity, nightMode))
				.setNegativeButton(R.string.shared_string_cancel, null);

		dialogData.setPositiveButton(R.string.shared_string_apply, (dialog, which) -> {
			Object extra = dialogData.getExtra(AlertDialogExtra.EDIT_TEXT);
			if (extra instanceof EditText) {
				EditText editText = (EditText) extra;
				String newName = editText.getText().toString();
				if (Objects.equals(oldName, newName)) {
					return;
				}
				if (Algorithms.isBlank(newName)) {
					app.showToastMessage(R.string.empty_name);
				} else if (deviceHelper.hasAssignmentNameDuplicate(app, appMode, deviceId, newName)) {
					app.showToastMessage(R.string.message_name_is_already_exists);
				} else {
					callback.processResult(newName.trim());
				}
			}
		});
		String caption = activity.getString(R.string.shared_string_name);
		CustomAlert.showInput(dialogData, activity, oldName, caption);
	}

	private void showRemoveKeyAssignmentDialog() {
		FragmentActivity activity = getActivity();
		if (activity == null) return;

		AlertDialogData dialogData = new AlertDialogData(activity, isNightMode())
				.setTitle(R.string.clear_key_assignment)
				.setNegativeButton(R.string.shared_string_cancel, null)
				.setPositiveButton(R.string.shared_string_remove, (dialog, which) -> {
					onResetKeyCodes();
				});
		CustomAlert.showSimpleMessage(dialogData, R.string.clear_key_assignment_desc);
	}

	private void onNameEntered(@NonNull String newName) {
		deviceHelper.renameAssignment(appMode, deviceId, assignmentId, newName);
	}

	private void onResetKeyCodes() {
		deviceHelper.clearAssignmentKeyCodes(appMode, deviceId, assignmentId);
	}

	@Override
	public void onKeyCodeSelected(int oldKeyCode, int newKeyCode) {
		if (oldKeyCode == KeyEvent.KEYCODE_UNKNOWN) {
			deviceHelper.addAssignmentKeyCode(appMode, deviceId, assignmentId, newKeyCode);
		} else {
			deviceHelper.updateAssignmentKeyCode(appMode, deviceId, assignmentId, oldKeyCode, newKeyCode);
		}
	}

	@Override
	public void processInputDevicesEvent(@NonNull ApplicationMode appMode, @NonNull EventType event) {
		if (event.isAssignmentRelated()) {
			updateViewContent(getView(), getAssignment());
		}
	}

	private void updateViewContent(@Nullable View view, @Nullable KeyAssignment assignment) {
		if (view != null && assignment != null) {
			updateToolbarTitle(view, assignment);
			setupActionNameRow(view, assignment);
			setupActionTypeRow(view, assignment);
			setupKeyButtonRow(view, assignment);
		}
	}

	private void updateToolbarTitle(@NonNull View view, @NonNull KeyAssignment assignment) {
		CollapsingToolbarLayout collapsingToolbarLayout = view.findViewById(R.id.toolbar_layout);
		collapsingToolbarLayout.setTitle(assignment.getName(app));
	}

	@Nullable
	private KeyAssignment getAssignment() {
		return deviceHelper.findAssignment(appMode, deviceId, assignmentId);
	}

	@Override
	public void onResume() {
		super.onResume();
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mapActivity.disableDrawer();
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
		deviceHelper.removeListener(this);
	}

	@Nullable
	private MapActivity getMapActivity() {
		return (MapActivity) getActivity();
	}

	private void dismiss() {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			activity.onBackPressed();
		}
	}

	private void setupSelectableBackground(@NonNull View view, @ColorInt int color) {
		setBackground(view, getColoredSelectableDrawable(view.getContext(), color, 0.3f));
	}

	@Override
	public int getStatusBarColorId() {
		AndroidUiHelper.setStatusBarContentColor(getView(), nightMode);
		return ColorUtilities.getStatusBarSecondaryColorId(nightMode);
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
