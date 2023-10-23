package net.osmand.plus.keyevent.fragments;

import static net.osmand.plus.settings.fragments.BaseSettingsFragment.APP_MODE_KEY;
import static net.osmand.plus.utils.AndroidUtils.setBackground;
import static net.osmand.plus.utils.UiUtilities.getColoredSelectableDrawable;

import android.graphics.Typeface;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.keyevent.InputDeviceHelper;
import net.osmand.plus.keyevent.keybinding.KeyBinding;
import net.osmand.plus.keyevent.interfaces.OnKeyCodeSelected;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.widgets.OsmandTextFieldBoxes;
import net.osmand.plus.widgets.dialogbutton.DialogButton;
import net.osmand.plus.widgets.dialogbutton.DialogButtonType;

import java.util.Objects;

import studio.carbonylgroup.textfieldboxes.ExtendedEditText;

public class EditKeyBindingFragment extends BaseOsmAndFragment implements OnKeyCodeSelected {

	public static final String TAG = EditKeyBindingFragment.class.getSimpleName();

	private static final String ATTR_KEY_CODE = "attr_key_code";
	private static final String ATTR_COMMAND_ID = "attr_command_id";
	private static final String ATTR_DEVICE_ID = "attr_device_id";

	private ApplicationMode appMode;
	private InputDeviceHelper deviceHelper;

	private DialogButton applyButton;

	private KeyBinding keyBinding;
	private int initialKeyCode = KeyEvent.KEYCODE_UNKNOWN;
	private String initialCommandId;
	private String deviceId;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		deviceHelper = app.getInputDeviceHelper();

		Bundle arguments = requireArguments();
		initialKeyCode = arguments.getInt(ATTR_KEY_CODE);
		initialCommandId = arguments.getString(ATTR_COMMAND_ID);
		deviceId = arguments.getString(ATTR_DEVICE_ID);
		String appModeKey = arguments.getString(APP_MODE_KEY);
		appMode = ApplicationMode.valueOfStringKey(appModeKey, settings.getApplicationMode());

		int keyCode;
		String commandId;
		if (savedInstanceState != null) {
			keyCode = savedInstanceState.getInt(ATTR_KEY_CODE);
			commandId = savedInstanceState.getString(ATTR_COMMAND_ID);
		} else {
			keyCode = initialKeyCode;
			commandId = initialCommandId;
		}
		keyBinding = new KeyBinding(keyCode, commandId);
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
		if (keyBinding != null) {
			setupActionNameRow(view);
			setupActionTypeRow(view);
			setupActionKeyRow(view);
			setupApplyButton(view);
		}
		return view;
	}

	private void setupToolbar(@NonNull View view) {
		ImageView closeButton = view.findViewById(R.id.close_button);
		closeButton.setOnClickListener(v -> dismiss());
	}

	private void setupActionNameRow(@NonNull View view) {
		OsmandTextFieldBoxes textBox = view.findViewById(R.id.text_box);
		textBox.setEnabled(false);
		ExtendedEditText editText = view.findViewById(R.id.edit_text);
		editText.setText(keyBinding.getCommandTitle(app));
	}

	private void setupActionTypeRow(@NonNull View view) {
		View actionButton = view.findViewById(R.id.action_button);
		TextView title = actionButton.findViewById(R.id.title);
		title.setText(R.string.shared_string_action);
		TextView summary = actionButton.findViewById(R.id.description);
		summary.setText(keyBinding.getCommandTitle(app));
	}

	private void setupActionKeyRow(@NonNull View view) {
		View keyButton = view.findViewById(R.id.key_button);
		TextView title = keyButton.findViewById(R.id.title);
		title.setText(R.string.shared_string_button);
		TextView summary = keyButton.findViewById(R.id.description);
		summary.setText(keyBinding.getKeySymbol());
		summary.setTypeface(summary.getTypeface(), Typeface.BOLD);
		View backgroundView = keyButton.findViewById(R.id.selectable_list_item);
		setupSelectableBackground(backgroundView, appMode.getProfileColor(nightMode));
		keyButton.setOnClickListener(v -> {
			FragmentActivity activity = getActivity();
			if (activity != null) {
				Fragment thisFragment = EditKeyBindingFragment.this;
				FragmentManager fm = activity.getSupportFragmentManager();
				SelectKeyCodeFragment.showInstance(fm, thisFragment, appMode, deviceId, keyBinding);
			}
		});
		AndroidUiHelper.updateVisibility(keyButton.findViewById(R.id.bottom_divider), false);
	}

	private void setupApplyButton(@NonNull View view) {
		applyButton = view.findViewById(R.id.dismiss_button);
		applyButton.setOnClickListener(v -> {
			int newKeyCode = keyBinding.getKeyCode();
			String commandId = keyBinding.getCommandId();
			deviceHelper.updateKeyBinding(deviceId, commandId, initialKeyCode, newKeyCode);
			dismiss();
		});
		applyButton.setButtonType(DialogButtonType.PRIMARY);
		applyButton.setTitleId(R.string.shared_string_save);
		enableDisableApplyButton();
	}

	private void enableDisableApplyButton() {
		applyButton.setEnabled(hasAnyChanges());
	}

	private boolean hasAnyChanges() {
		return keyBinding != null
				&& (initialKeyCode != keyBinding.getKeyCode()
				|| !Objects.equals(initialCommandId, keyBinding.getCommandId()));
	}

	@Override
	public void onKeyCodeSelected(int newKeyCode) {
		View view = getView();
		keyBinding = new KeyBinding(newKeyCode, keyBinding.getCommandId());
		if (view != null) {
			updateViewContent(view);
		}
	}

	private void updateViewContent(@NonNull View view) {
		setupActionNameRow(view);
		setupActionTypeRow(view);
		setupActionKeyRow(view);
	}

	@Override
	public void onResume() {
		super.onResume();
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mapActivity.disableDrawer();
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mapActivity.enableDrawer();
		}
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		if (keyBinding != null) {
			outState.putInt(ATTR_KEY_CODE, keyBinding.getKeyCode());
			outState.putString(ATTR_COMMAND_ID, keyBinding.getCommandId());
		}
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
		return ColorUtilities.getListBgColorId(nightMode);
	}

	public static void showInstance(@NonNull FragmentManager manager,
	                                @NonNull ApplicationMode appMode,
	                                @NonNull KeyBinding keyBinding,
	                                @NonNull String deviceId) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			EditKeyBindingFragment fragment = new EditKeyBindingFragment();
			Bundle arguments = new Bundle();
			arguments.putString(APP_MODE_KEY, appMode.getStringKey());
			arguments.putString(ATTR_COMMAND_ID, keyBinding.getCommandId());
			arguments.putString(ATTR_DEVICE_ID, deviceId);
			arguments.putInt(ATTR_KEY_CODE, keyBinding.getKeyCode());
			fragment.setArguments(arguments);
			manager.beginTransaction()
					.replace(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(TAG)
					.commitAllowingStateLoss();
		}
	}
}
