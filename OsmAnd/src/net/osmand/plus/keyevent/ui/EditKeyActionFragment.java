package net.osmand.plus.keyevent.ui;

import static net.osmand.plus.settings.fragments.BaseSettingsFragment.APP_MODE_KEY;
import static net.osmand.plus.utils.AndroidUtils.setBackground;
import static net.osmand.plus.utils.UiUtilities.getColoredSelectableDrawable;

import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.keyevent.InputDeviceHelper;
import net.osmand.plus.keyevent.InputDeviceHelperListener;
import net.osmand.plus.keyevent.commands.KeyEventCommand;
import net.osmand.plus.keyevent.ui.keybindings.ActionItem;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.widgets.OsmandTextFieldBoxes;
import net.osmand.plus.widgets.dialogbutton.DialogButton;
import net.osmand.plus.widgets.dialogbutton.DialogButtonType;

import java.util.Objects;

import studio.carbonylgroup.textfieldboxes.ExtendedEditText;

public class EditKeyActionFragment extends BaseOsmAndFragment implements InputDeviceHelperListener {

	public static final String TAG = EditKeyActionFragment.class.getSimpleName();

	private static final String ATTR_COMMAND_ID = "attr_command_id";
	private static final String ATTR_KEY_CODE = "attr_key_code";

	private ApplicationMode appMode;
	private InputDeviceHelper deviceHelper;

	private DialogButton applyButton;

	private ActionItem actionItem;
	private int initialKeyCode;
	private String initialCommandId;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		deviceHelper = app.getInputDeviceHelper();
		Bundle arguments = requireArguments();
		initialKeyCode = arguments.getInt(ATTR_KEY_CODE);
		initialCommandId = arguments.getString(ATTR_COMMAND_ID);
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
		KeyEventCommand command = deviceHelper.getOrCreateCommand(commandId);
		if (command != null) {
			actionItem = new ActionItem(keyCode, command);
		}
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
		if (actionItem != null) {
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
		editText.setText(actionItem.getCommand().toHumanString(app));
	}

	private void setupActionTypeRow(@NonNull View view) {
		View actionButton = view.findViewById(R.id.action_button);
		TextView title = actionButton.findViewById(R.id.title);
		title.setText(R.string.shared_string_action);
		TextView summary = actionButton.findViewById(R.id.description);
		summary.setText(actionItem.getCommand().toHumanString(app));
	}

	private void setupActionKeyRow(@NonNull View view) {
		View keyButton = view.findViewById(R.id.key_button);
		TextView title = keyButton.findViewById(R.id.title);
		title.setText(R.string.shared_string_button);
		TextView summary = keyButton.findViewById(R.id.description);
		summary.setText(actionItem.getKeySymbol());
		summary.setTypeface(summary.getTypeface(), Typeface.BOLD);
		View backgroundView = keyButton.findViewById(R.id.selectable_list_item);
		setupSelectableBackground(backgroundView, appMode.getProfileColor(nightMode));
		keyButton.setOnClickListener(v -> {
			// todo open select key code screen
		});
		AndroidUiHelper.updateVisibility(keyButton.findViewById(R.id.bottom_divider), false);
	}

	private void setupApplyButton(@NonNull View view) {
		applyButton = view.findViewById(R.id.dismiss_button);
		applyButton.setOnClickListener(v -> {
			// todo save changes
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
		return actionItem != null
				&& (initialKeyCode != actionItem.getKeyCode()
				|| !Objects.equals(initialCommandId, actionItem.getCommand().getId()));
	}

	@Override
	public void onInputDeviceHelperMessage() {
		View view = getView();
		if (view != null && actionItem != null) {
			updateViewContent(view);
		}
	}

	private void updateViewContent(@NonNull View view) {
		View keyButton = view.findViewById(R.id.key_button);
		TextView summary = keyButton.findViewById(R.id.description);
		summary.setText(actionItem.getKeySymbol());
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

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		if (actionItem != null) {
			outState.putInt(ATTR_KEY_CODE, actionItem.getKeyCode());
			outState.putString(ATTR_COMMAND_ID, actionItem.getCommand().getId());
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
	                                @NonNull ActionItem actionItem) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			EditKeyActionFragment fragment = new EditKeyActionFragment();
			Bundle arguments = new Bundle();
			arguments.putString(APP_MODE_KEY, appMode.getStringKey());
			arguments.putString(ATTR_COMMAND_ID, actionItem.getCommand().getId());
			arguments.putInt(ATTR_KEY_CODE, actionItem.getKeyCode());
			fragment.setArguments(arguments);
			manager.beginTransaction()
					.replace(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(TAG)
					.commitAllowingStateLoss();
		}
	}
}
