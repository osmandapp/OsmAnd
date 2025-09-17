package net.osmand.plus.keyevent.fragments.selectkeycode;

import static android.graphics.Typeface.BOLD;
import static net.osmand.plus.utils.ColorUtilities.getPrimaryIconColor;
import static net.osmand.plus.utils.UiUtilities.createSpannableString;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.appbar.AppBarLayout;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseFullScreenFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.keyevent.InputDevicesHelper;
import net.osmand.plus.keyevent.KeyEventHelper;
import net.osmand.plus.keyevent.KeySymbolMapper;
import net.osmand.plus.keyevent.assignment.KeyAssignment;
import net.osmand.plus.keyevent.devices.InputDeviceProfile;
import net.osmand.plus.keyevent.fragments.editassignment.EditKeyAssignmentController;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.widgets.dialogbutton.DialogButton;
import net.osmand.plus.widgets.dialogbutton.DialogButtonType;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SelectKeyCodeFragment extends BaseFullScreenFragment implements KeyEvent.Callback {

	public static final String TAG = SelectKeyCodeFragment.class.getSimpleName();

	private static final String ATTR_KEY_CODE = "attr_key_code";
	private static final String ATTR_DEVICE_ID = "attr_input_device_id";
	private static final String ATTR_ASSIGNMENT_ID = "attr_key_assignment_id";
	private static final String ATTR_HAS_INPUT_FROM_USER = "attr_has_input_from_user";

	private static final int PULSE_DELAY_MS = 1000;

	private KeyEventHelper keyEventHelper;
	private DialogButton applyButton;

	private Integer keyCode = null;
	private int initialKeyCode;
	private String assignmentId;
	private InputDeviceProfile inputDevice;
	private boolean hasInputFromUser = false;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		InputDevicesHelper deviceHelper = app.getInputDeviceHelper();
		keyEventHelper = app.getKeyEventHelper();

		Bundle arguments = requireArguments();
		initialKeyCode = arguments.getInt(ATTR_KEY_CODE);
		assignmentId = arguments.getString(ATTR_ASSIGNMENT_ID);

		String deviceId = Objects.requireNonNull(arguments.getString(ATTR_DEVICE_ID));
		inputDevice = deviceHelper.getDeviceById(appMode, deviceId);

		if (savedInstanceState != null) {
			keyCode = savedInstanceState.containsKey(ATTR_KEY_CODE)
					? savedInstanceState.getInt(ATTR_KEY_CODE)
					: null;
			hasInputFromUser = savedInstanceState.containsKey(ATTR_HAS_INPUT_FROM_USER)
					&& savedInstanceState.getBoolean(ATTR_HAS_INPUT_FROM_USER);
		} else {
			keyCode = initialKeyCode;
		}
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		updateNightMode();
		View view = inflate(R.layout.fragment_select_key_code, container, false);
		AndroidUtils.addStatusBarPadding21v(requireMyActivity(), view);
		setupToolbar(view);
		setupDescription(view);
		setupApplyButton(view);
		return view;
	}

	@Nullable
	@Override
	public List<Integer> getCollapsingAppBarLayoutId() {
		List<Integer> ids = new ArrayList<>();
		ids.add(R.id.appbar);
		return ids;
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		onValueChanged();
	}

	private void setupToolbar(@NonNull View view) {
		AppBarLayout appBar = view.findViewById(R.id.appbar);
		appBar.setExpanded(AndroidUiHelper.isOrientationPortrait(requireActivity()));

		int color = getPrimaryIconColor(app, nightMode);
		Toolbar toolbar = view.findViewById(R.id.toolbar);
		toolbar.setNavigationIcon(getPaintedIcon(R.drawable.ic_action_close, color));
		toolbar.setNavigationContentDescription(R.string.shared_string_close);
		toolbar.setNavigationOnClickListener(v -> {
			dismiss();
		});
	}

	private void setupDescription(@NonNull View view) {
		EditKeyAssignmentController controller = EditKeyAssignmentController.getExistedInstance(app);
		QuickAction quickAction = controller != null ? controller.getSelectedAction() : null;
		if (quickAction != null) {
			String action = quickAction.getName(app);
			String message = getString(R.string.press_button_to_link_with_action, action);
			TextView description = view.findViewById(R.id.description);
			description.setText(createSpannableString(message, BOLD, action));
		}
	}

	private void startPulseAnimation(@NonNull View view) {
		AlphaAnimation animation = new AlphaAnimation(0.0f, 1.0f);
		animation.setDuration(PULSE_DELAY_MS);
		animation.setRepeatMode(Animation.REVERSE);
		animation.setRepeatCount(Animation.INFINITE);
		view.startAnimation(animation);
	}

	private void setupApplyButton(@NonNull View view) {
		applyButton = view.findViewById(R.id.dismiss_button);
		applyButton.setOnClickListener(v -> {
			EditKeyAssignmentController controller = EditKeyAssignmentController.getExistedInstance(app);
			if (controller != null) {
				controller.onKeyCodeSelected(initialKeyCode, keyCode);
			}
			dismiss();
		});
		applyButton.setButtonType(DialogButtonType.PRIMARY);
		applyButton.setTitleId(R.string.shared_string_save);
		updateApplyButtonState();
	}

	private void onValueChanged() {
		View view = getView();
		if (view != null) {
			updateKeyLabel(view);
			updateCursor(view);
			updateWarningMessageState(view);
			updateApplyButtonState();
		}
	}

	private void updateKeyLabel(@NonNull View view) {
		TextView keyLabel = view.findViewById(R.id.key_label);
		keyLabel.setText(KeySymbolMapper.getKeySymbol(app, keyCode));
	}

	private void updateCursor(@NonNull View view) {
		View cursor = view.findViewById(R.id.cursor);
		View errorCursor = view.findViewById(R.id.error_cursor);
		if (isKeyCodeFree()) {
			errorCursor.clearAnimation();
			AndroidUiHelper.updateVisibility(cursor, true);
			AndroidUiHelper.updateVisibility(errorCursor, false);
			startPulseAnimation(cursor);
		} else {
			cursor.clearAnimation();
			AndroidUiHelper.updateVisibility(cursor, false);
			AndroidUiHelper.updateVisibility(errorCursor, true);
			startPulseAnimation(errorCursor);
		}
	}

	private void updateWarningMessageState(@NonNull View view) {
		View warning = view.findViewById(R.id.warning);
		View warningIcon = view.findViewById(R.id.warning_icon);
		TextView warningMessage = view.findViewById(R.id.warning_message);
		KeyAssignment assignmentDuplication = getAssignmentDuplication(keyCode);
		if (assignmentDuplication != null) {
			AndroidUiHelper.updateVisibility(warning, true);
			AndroidUiHelper.updateVisibility(warningIcon, true);
			String keyLabel = KeySymbolMapper.getKeySymbol(app, keyCode);
			String actionName = assignmentDuplication.getName(app);
			String message = getString(R.string.key_is_already_assigned_error, keyLabel, actionName);
			warningMessage.setText(createSpannableString(message, BOLD, keyLabel, actionName));
		} else if (isKeyCodeAlreadyAssignedToThisAction() && hasInputFromUser) {
			AndroidUiHelper.updateVisibility(warning, true);
			AndroidUiHelper.updateVisibility(warningIcon, false);
			warningMessage.setText(R.string.button_already_assigned_to_this_action);
		} else {
			AndroidUiHelper.updateVisibility(warning, false);
		}
	}

	private void updateApplyButtonState() {
		boolean assignedToThisAction = isKeyCodeAlreadyAssignedToThisAction();
		applyButton.setEnabled(isKeyCodeChanged() && !assignedToThisAction);
		boolean keyCodeFree = isKeyCodeFree();
		applyButton.setButtonType(keyCodeFree ? DialogButtonType.PRIMARY : DialogButtonType.PRIMARY_HARMFUL);
		applyButton.setTitleId(keyCodeFree || assignedToThisAction ? R.string.shared_string_save : R.string.shared_string_reassign);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		return isKeyCodeSupported(keyCode);
	}

	@Override
	public boolean onKeyLongPress(int keyCode, KeyEvent event) {
		return isKeyCodeSupported(keyCode);
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if (isKeyCodeSupported(keyCode)) {
			this.keyCode = keyCode;
			onValueChanged();
			return true;
		}
		return false;
	}

	@Override
	public boolean onKeyMultiple(int keyCode, int count, KeyEvent event) {
		return isKeyCodeSupported(keyCode);
	}

	private boolean isKeyCodeSupported(int keyCode) {
		hasInputFromUser = true;
		return keyCode != KeyEvent.KEYCODE_BACK;
	}

	private boolean isKeyCodeChanged() {
		return initialKeyCode != keyCode;
	}

	private boolean isKeyCodeFree() {
		return getAssignmentDuplication(keyCode) == null;
	}

	private boolean isKeyCodeAlreadyAssignedToThisAction() {
		EditKeyAssignmentController controller = EditKeyAssignmentController.getExistedInstance(app);
		return controller != null && controller.isKeyCodeAlreadyAssignedToThisAction(keyCode);
	}

	@Nullable
	private KeyAssignment getAssignmentDuplication(int keyCode) {
		if (inputDevice != null) {
			KeyAssignment assignment = inputDevice.findAssignment(keyCode);
			if (assignment != null && !Objects.equals(getKeyAssignment(), assignment)) {
				return assignment;
			}
		}
		return null;
	}

	@Nullable
	private KeyAssignment getKeyAssignment() {
		return inputDevice.findAssignment(assignmentId);
	}

	@Override
	public void onResume() {
		super.onResume();
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mapActivity.disableDrawer();
		}
		keyEventHelper.setExternalCallback(this);
	}

	@Override
	public void onPause() {
		super.onPause();
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mapActivity.enableDrawer();
		}
		keyEventHelper.setExternalCallback(null);
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt(ATTR_KEY_CODE, keyCode);
		outState.putBoolean(ATTR_HAS_INPUT_FROM_USER, hasInputFromUser);
	}

	private void dismiss() {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			activity.onBackPressed();
		}
	}

	@Override
	public int getStatusBarColorId() {
		AndroidUiHelper.setStatusBarContentColor(getView(), nightMode);
		return ColorUtilities.getStatusBarSecondaryColorId(nightMode);
	}

	public boolean getContentStatusBarNightMode() {
		return nightMode;
	}

	public static void showInstance(@NonNull FragmentManager manager,
	                                @NonNull ApplicationMode appMode,
	                                @NonNull String deviceId,
									@NonNull String assignmentId,
	                                @NonNull Integer keyCode) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			SelectKeyCodeFragment fragment = new SelectKeyCodeFragment();
			Bundle arguments = new Bundle();
			arguments.putString(APP_MODE_KEY, appMode.getStringKey());
			arguments.putString(ATTR_DEVICE_ID, deviceId);
			arguments.putString(ATTR_ASSIGNMENT_ID, assignmentId);
			arguments.putInt(ATTR_KEY_CODE, keyCode);
			fragment.setArguments(arguments);
			manager.beginTransaction()
					.replace(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(TAG)
					.commitAllowingStateLoss();
		}
	}
}