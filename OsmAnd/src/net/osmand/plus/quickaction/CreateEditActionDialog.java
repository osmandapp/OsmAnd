package net.osmand.plus.quickaction;

import static net.osmand.plus.quickaction.AddQuickActionDialog.QUICK_ACTION_BUTTON_KEY;
import static net.osmand.plus.quickaction.QuickActionListFragment.showConfirmDeleteAnActionBottomSheet;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.CallbackWithObject;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.plugins.osmedit.quickactions.AddPOIAction;
import net.osmand.plus.quickaction.ConfirmationBottomSheet.OnConfirmButtonClickListener;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.mapwidgets.configure.buttons.QuickActionButtonState;
import net.osmand.plus.widgets.tools.SimpleTextWatcher;

import java.util.List;

/**
 * Created by rosty on 12/27/16.
 */

public class CreateEditActionDialog extends DialogFragment implements CallbackWithObject<Object>, OnConfirmButtonClickListener {

	public static final String TAG = CreateEditActionDialog.class.getSimpleName();

	public static final String KEY_ACTION_ID = "action_id";
	public static final String KEY_ACTION_TYPE = "action_type";
	public static final String KEY_ACTION_IS_NEW = "action_is_new";

	private OsmandApplication app;
	private OsmandSettings settings;
	private UiUtilities uiUtilities;
	private MapButtonsHelper mapButtonsHelper;
	private QuickAction action;
	private QuickActionButtonState buttonState;

	private boolean isNew;
	private boolean nightMode;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		app = (OsmandApplication) requireActivity().getApplication();
		settings = app.getSettings();
		uiUtilities = app.getUIUtilities();
		mapButtonsHelper = app.getMapButtonsHelper();

		Bundle args = getArguments();
		String key = args != null ? args.getString(QUICK_ACTION_BUTTON_KEY) : null;
		if (key != null) {
			buttonState = mapButtonsHelper.getButtonStateById(key);
		}
		nightMode = !settings.isLightContent() || app.getDaynightHelper().isNightMode();
		setStyle(DialogFragment.STYLE_NORMAL, nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme);

		int type = savedInstanceState == null ? getArguments().getInt(KEY_ACTION_TYPE) : savedInstanceState.getInt(KEY_ACTION_TYPE);
		long actionId = savedInstanceState == null ? getArguments().getLong(KEY_ACTION_ID) : savedInstanceState.getLong(KEY_ACTION_ID);
		isNew = savedInstanceState == null ? actionId == 0 : savedInstanceState.getBoolean(KEY_ACTION_IS_NEW);
		action = MapButtonsHelper.produceAction(isNew ? mapButtonsHelper.newActionByType(type) : buttonState.getQuickAction(actionId));
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		Dialog dialog = new Dialog(UiUtilities.getThemedContext(getActivity(), nightMode, R.style.Dialog90Light, R.style.Dialog90Dark), getTheme());
		dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
		dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
		dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

		return dialog;
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.quick_action_create_edit_dialog, parent, false);

		setupToolbar(view);
		setupHeader(view, savedInstanceState);
		setupFooter(view);

		action.drawUI(view.findViewById(R.id.container), getMapActivity());

		return view;
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);

		outState.putLong(KEY_ACTION_ID, action.getId());
		outState.putInt(KEY_ACTION_TYPE, action.getType());
		outState.putBoolean(KEY_ACTION_IS_NEW, isNew);
	}

	private void setupToolbar(@NonNull View root) {
		Toolbar toolbar = root.findViewById(R.id.toolbar);
		toolbar.setTitle(isNew ? R.string.quick_action_new_action : R.string.quick_action_edit_action);

		int color = ColorUtilities.getActiveButtonsAndLinksTextColorId(nightMode);
		toolbar.setTitleTextColor(ContextCompat.getColor(app, color));
		toolbar.setNavigationIcon(uiUtilities.getIcon(AndroidUtils.getNavigationIconResId(app), color));
		toolbar.setNavigationContentDescription(R.string.access_shared_string_navigate_up);
		toolbar.setNavigationOnClickListener(v -> dismiss());

		if (!isNew) {
			Menu menu = toolbar.getMenu();
			menu.clear();

			MenuItem item = menu.add(R.string.shared_string_delete).setIcon(R.drawable.ic_action_delete_dark);
			item.setOnMenuItemClickListener(i -> {
				FragmentActivity activity = getActivity();
				if (activity != null) {
					showConfirmDeleteAnActionBottomSheet(activity, CreateEditActionDialog.this, action, false);
				}
				return true;
			});
			item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		}
	}

	private void setupHeader(@NonNull View root, @Nullable Bundle savedInstanceState) {
		EditText nameEditText = root.findViewById(R.id.name);
		nameEditText.setTextColor(ColorUtilities.getActiveButtonsAndLinksTextColor(app, nightMode));
		nameEditText.addTextChangedListener(new SimpleTextWatcher() {
			@Override
			public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
				action.setName(charSequence.toString());
			}
		});
		nameEditText.setEnabled(action.isActionEditable());
		action.setAutoGeneratedTitle(nameEditText);

		if (savedInstanceState == null) {
			String name = action.getName(app);
			if (!action.isActionEditable() && action.getActionNameRes() != 0
					&& !name.contains(getString(action.getActionNameRes()))) {
				String actionName = getString(action.getActionNameRes());
				nameEditText.setText(getString(R.string.ltr_or_rtl_combine_via_dash, actionName, name));
			} else {
				nameEditText.setText(name);
			}
		} else {
			action.setName(nameEditText.getText().toString());
		}
		ImageView image = root.findViewById(R.id.image);
		image.setImageResource(action.getIconRes(app));
	}

	private void setupFooter(@NonNull View root) {
		root.findViewById(R.id.btnApply).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (action instanceof AddPOIAction) {
					saveFirstTagWithEmptyValue();
				}
				if (action.fillParams(((ViewGroup) root.findViewById(R.id.container)).getChildAt(0), (MapActivity) getActivity())) {
					List<QuickAction> actions = buttonState.getQuickActions();
					if (mapButtonsHelper.isActionNameUnique(actions, action)) {
						if (isNew) {
							mapButtonsHelper.addQuickAction(buttonState, action);
						} else {
							mapButtonsHelper.updateQuickAction(buttonState, action);
						}
						dismiss();
					} else {
						action = mapButtonsHelper.generateUniqueActionName(actions, action);
						showDuplicatedDialog();
						((EditText) root.findViewById(R.id.name)).setText(action.getName(app));
					}
				} else {
					app.showShortToastMessage(R.string.quick_action_empty_param_error);
				}
			}

			private void saveFirstTagWithEmptyValue() {
				((ViewGroup) root.findViewById(R.id.container)).getChildAt(0).requestFocus();
			}
		});
	}

	private void showDuplicatedDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
		builder.setTitle(R.string.quick_action_duplicate);
		builder.setMessage(getString(R.string.quick_action_duplicates, action.getName(app)));
		builder.setPositiveButton(R.string.shared_string_ok, (dialog, which) -> {
			if (isNew) {
				mapButtonsHelper.addQuickAction(buttonState, action);
			} else {
				mapButtonsHelper.updateQuickAction(buttonState, action);
			}
			CreateEditActionDialog.this.dismiss();
			dismiss();
		}).create().show();
	}

	@Override
	public boolean processResult(Object result) {
		if (action instanceof SwitchableAction) {
			((SwitchableAction) action).onItemsSelected(getContext(), (List) result);
		} else if (action instanceof FileSelected) {
			View container = getView() != null ? getView().findViewById(R.id.container) : null;
			MapActivity mapActivity = getMapActivity();
			if (container != null && mapActivity != null && result instanceof String) {
				((FileSelected) action).onGpxFileSelected(container, mapActivity, (String) result);
			}
		}
		return false;
	}

	@Override
	public void onConfirmButtonClick() {
		mapButtonsHelper.deleteQuickAction(buttonState, action);
		dismiss();
	}

	public void hide() {
		Dialog dialog = getDialog();
		if (dialog != null) {
			dialog.hide();
		}
	}

	public void show() {
		Dialog dialog = getDialog();
		if (dialog != null) {
			dialog.show();
		}
	}

	@Nullable
	private MapActivity getMapActivity() {
		return getActivity() == null ? null : ((MapActivity) getActivity());
	}

	public interface FileSelected {
		void onGpxFileSelected(@NonNull View container, @NonNull MapActivity mapActivity, @NonNull String gpxFilePath);
	}

	public static void showInstance(@NonNull FragmentManager manager,
	                                @NonNull QuickActionButtonState buttonState, int actionTypeId) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			Bundle args = new Bundle();
			args.putInt(KEY_ACTION_TYPE, actionTypeId);
			args.putString(QUICK_ACTION_BUTTON_KEY, buttonState.getId());

			CreateEditActionDialog dialog = new CreateEditActionDialog();
			dialog.setArguments(args);
			dialog.show(manager, TAG);
		}
	}

	public static void showInstance(@NonNull FragmentManager fragmentManager,
	                                @NonNull QuickActionButtonState buttonState, @NonNull QuickAction action) {
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			Bundle args = new Bundle();
			args.putLong(KEY_ACTION_ID, action.id);
			args.putString(QUICK_ACTION_BUTTON_KEY, buttonState.getId());

			CreateEditActionDialog dialog = new CreateEditActionDialog();
			dialog.setArguments(args);
			dialog.show(fragmentManager, TAG);
		}
	}
}