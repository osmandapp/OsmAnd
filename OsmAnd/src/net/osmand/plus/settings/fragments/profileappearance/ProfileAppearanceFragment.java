package net.osmand.plus.settings.fragments.profileappearance;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.DRAWER_SETTINGS_ID;
import static net.osmand.plus.utils.ColorUtilities.getListBgColorId;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.PlatformUtil;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.card.color.palette.main.ColorsPaletteCard;
import net.osmand.plus.card.icon.IconsPaletteCard;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.routepreparationmenu.cards.BaseCard;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.fragments.BaseSettingsFragment;
import net.osmand.plus.settings.fragments.ProfileOptionsDialogController;
import net.osmand.plus.settings.fragments.SettingsScreenType;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.OsmandTextFieldBoxes;
import net.osmand.plus.widgets.dialogbutton.DialogButton;
import net.osmand.plus.widgets.dialogbutton.DialogButtonType;
import net.osmand.plus.widgets.tools.SimpleTextWatcher;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.List;

public class ProfileAppearanceFragment extends BaseSettingsFragment implements IProfileAppearanceScreen {

	private static final Log LOG = PlatformUtil.getLog(ProfileAppearanceFragment.class);

	public static final String TAG = ProfileAppearanceFragment.class.getName();

	private final static String PROFILE_NAME = "profile_name";
	private final static String COLORS_CARD_HEADER = "colors_card_header";
	private final static String COLORS_CARD = "colors_card";
	private final static String PROFILE_ICON_CARD_HEADER = "profile_icon_card_header";
	private final static String PROFILE_ICON_CARD = "profile_icon_card";
	private final static String PROFILE_ICON_CARD_DIVIDER = "profile_icon_card_divider";
	private final static String RESTING_POSITION_ICON_CARD_HEADER = "resting_position_icon_card_header";
	private final static String RESTING_POSITION_ICON_CARD = "resting_position_icon_card";
	private final static String NAVIGATION_POSITION_ICON_CARD_HEADER = "navigation_position_icon_card_header";
	private final static String NAVIGATION_POSITION_ICON_CARD = "navigation_position_icon_card";
	private final static String OPTIONS_CARD_HEADER = "options_card_header";
	private final static String OPTIONS_CARD = "options_card";

	private static final String BASE_PROFILE_FOR_NEW = "base_profile_for_new";
	private static final String IS_BASE_PROFILE_IMPORTED = "is_base_profile_imported";

	private ProfileAppearanceController screenController;
	private ProgressDialog progress;
	private EditText profileName;
	private OsmandTextFieldBoxes profileNameOtfb;
	private DialogButton applyButton;

	private boolean hasNameError;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		String modeKey = null;
		boolean imported = false;
		Bundle args = getArguments();
		if (args != null) {
			modeKey = args.getString(BASE_PROFILE_FOR_NEW);
			imported = args.getBoolean(IS_BASE_PROFILE_IMPORTED);
		}
		screenController = ProfileAppearanceController.getInstance(app, this, modeKey, imported);
		requireActionBarActivity().getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
			public void handleOnBackPressed() {
				FragmentActivity activity = getActivity();
				if (activity != null) {
					screenController.askCloseScreen(activity);
				}
			}
		});
	}

	@SuppressLint("InlinedApi")
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = super.onCreateView(inflater, container, savedInstanceState);
		if (view != null) {
			boolean nightMode = isNightMode();
			FrameLayout preferencesContainer = view.findViewById(android.R.id.list_container);
			LayoutInflater themedInflater = UiUtilities.getInflater(getContext(), nightMode);
			View bottomPanel = themedInflater.inflate(R.layout.control_bottom_buttons, preferencesContainer, false);
			View buttonsContainer = bottomPanel.findViewById(R.id.bottom_buttons_container);
			preferencesContainer.addView(bottomPanel);

			buttonsContainer.findViewById(R.id.dismiss_button).setVisibility(View.GONE);
			buttonsContainer.findViewById(R.id.buttons_divider).setVisibility(View.GONE);
			applyButton = buttonsContainer.findViewById(R.id.right_bottom_button);

			applyButton.setVisibility(View.VISIBLE);
			applyButton.setButtonType(DialogButtonType.PRIMARY);
			applyButton.setTitleId(R.string.shared_string_apply);
			applyButton.setOnClickListener(v -> screenController.onSaveButtonClicked(getActivity()));
			AndroidUtils.setBackground(getContext(), buttonsContainer, getListBgColorId(nightMode));

			getListView().addOnScrollListener(new RecyclerView.OnScrollListener() {
				@Override
				public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
					super.onScrollStateChanged(recyclerView, newState);
					if (newState != RecyclerView.SCROLL_STATE_IDLE) {
						hideKeyboard();
						if (profileName != null) {
							profileName.clearFocus();
						}
					}
				}
			});
		}
		updateApplyButtonEnable();
		return view;
	}

	@Nullable
	@Override
	public List<Integer> getBottomContainersIds() {
		return null;
	}

	@Nullable
	@Override
	public List<Integer> getScrollableViewIds() {
		List<Integer> ids = new ArrayList<>();
		ids.add(android.R.id.list_container);
		return ids;
	}

	@Override
	protected void createToolbar(@NonNull LayoutInflater inflater, @NonNull View view) {
		super.createToolbar(inflater, view);
		ImageView closeButton = view.findViewById(R.id.close_button);
		if (closeButton != null) {
			closeButton.setImageResource(R.drawable.ic_action_close);
		}
		TextView toolbarTitle = view.findViewById(R.id.toolbar_title);
		if (toolbarTitle != null) {
			toolbarTitle.setText(screenController.getToolbarTitle());
		}
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.toolbar_subtitle), false);
	}

	@Override
	protected void setupPreferences() {
		requirePreference(COLORS_CARD_HEADER).setIconSpaceReserved(false);
		requirePreference(PROFILE_ICON_CARD_HEADER).setIconSpaceReserved(false);
		requirePreference(RESTING_POSITION_ICON_CARD_HEADER).setIconSpaceReserved(false);
		requirePreference(NAVIGATION_POSITION_ICON_CARD_HEADER).setIconSpaceReserved(false);
		requirePreference(OPTIONS_CARD_HEADER).setIconSpaceReserved(false);
		if (screenController.isNotAllParamsEditable()) {
			requirePreference(PROFILE_ICON_CARD).setVisible(false);
			requirePreference(PROFILE_ICON_CARD_HEADER).setVisible(false);
			requirePreference(PROFILE_ICON_CARD_DIVIDER).setVisible(false);
		}
		setupViewAnglePref();
		setupLocationRadiusPref();
	}

	@Override
	protected void onBindPreferenceViewHolder(@NonNull Preference preference, @NonNull PreferenceViewHolder holder) {
		super.onBindPreferenceViewHolder(preference, holder);
		MapActivity activity = getMapActivity();
		if (activity == null) {
			return;
		}
		ApplicationMode appMode = getSelectedAppMode();
		String key = preference.getKey();
		if (PROFILE_NAME.equals(preference.getKey())) {
			profileName = (EditText) holder.findViewById(R.id.profile_name_et);
			profileName.setImeOptions(EditorInfo.IME_ACTION_DONE);
			profileName.setRawInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
			profileName.setText(screenController.getProfileName());
			profileName.addTextChangedListener(new SimpleTextWatcher() {
				@Override
				public void afterTextChanged(Editable s) {
					screenController.setProfileName(s.toString());
					if (screenController.isNameEmpty()) {
						disableSaveButtonWithErrorMessage(getString(R.string.please_provide_profile_name_message));
					} else if (screenController.hasNameDuplicate()) {
						disableSaveButtonWithErrorMessage(getString(R.string.profile_alert_duplicate_name_msg));
					} else {
						hasNameError = false;
						updateApplyButtonEnable();
					}
				}
			});
			profileName.setOnFocusChangeListener((v, hasFocus) -> {
				if (hasFocus) {
					profileName.setSelection(profileName.getText().length());
					AndroidUtils.showSoftKeyboard(requireActivity(), profileName);
				}
			});
			if (screenController.isNotAllParamsEditable()) {
				profileName.setFocusableInTouchMode(false);
				profileName.setFocusable(false);
			}
			profileNameOtfb = (OsmandTextFieldBoxes) holder.findViewById(R.id.profile_name_otfb);
			updateProfileNameAppearance();
		} else if (COLORS_CARD.equals(key)) {
			bindCard(holder, new ColorsPaletteCard(activity, screenController.getColorsCardController(), appMode, false));
		} else if (PROFILE_ICON_CARD.equals(key)) {
			bindCard(holder, new IconsPaletteCard<>(activity, screenController.getProfileIconCardController(), appMode, false));
		} else if (RESTING_POSITION_ICON_CARD.equals(key)) {
			bindCard(holder, new IconsPaletteCard<>(activity, screenController.getRestingIconCardController(), appMode, false));
		} else if (NAVIGATION_POSITION_ICON_CARD.equals(key)) {
			bindCard(holder, new IconsPaletteCard<>(activity, screenController.getNavigationIconCardController(), appMode, false));
		} else if (OPTIONS_CARD.equals(key)) {
			ViewGroup container = (ViewGroup) holder.itemView;
			container.removeAllViews();
		}
	}

	@Override
	public boolean onPreferenceClick(Preference preference) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			ApplicationMode appMode = getSelectedAppMode();
			ProfileOptionsDialogController optionsDialogController = screenController.getProfileOptionController();
			if (settings.VIEW_ANGLE_VISIBILITY.getId().equals(preference.getKey())) {
				optionsDialogController.showDialog(mapActivity, appMode, app.getString(R.string.view_angle),
						app.getString(R.string.view_angle_description), settings.VIEW_ANGLE_VISIBILITY);
			} else if (settings.LOCATION_RADIUS_VISIBILITY.getId().equals(preference.getKey())) {
				optionsDialogController.showDialog(mapActivity, appMode, app.getString(R.string.location_radius),
						app.getString(R.string.location_radius_description), settings.LOCATION_RADIUS_VISIBILITY);
			}
		}
		return super.onPreferenceClick(preference);
	}

	private void bindCard(@NonNull PreferenceViewHolder holder, @NonNull BaseCard card) {
		ViewGroup container = (ViewGroup) holder.itemView;
		container.removeAllViews();
		container.addView(card.build());
	}

	@Override
	public void updateColorItems() {
		updateProfileNameAppearance();
	}

	@Override
	public void updateOptionsCard() {
		Preference viewAnglePref = findPreference(settings.VIEW_ANGLE_VISIBILITY.getId());
		viewAnglePref.setSummary(screenController.getViewAngleVisibility().getNameId());
		Preference locationRadiusPref = findPreference(settings.LOCATION_RADIUS_VISIBILITY.getId());
		locationRadiusPref.setSummary(screenController.getLocationRadiusVisibility().getNameId());
	}

	public void updateApplyButtonEnable() {
		applyButton.setEnabled(screenController.hasChanges() && !hasNameError);
	}

	private void updateProfileNameAppearance() {
		if (profileName != null && profileName.isFocusable() && profileName.isFocusableInTouchMode()) {
			int selectedColor = screenController.getActualColor(isNightMode());
			profileNameOtfb.setPrimaryColor(selectedColor);
			profileName.getBackground().mutate().setColorFilter(selectedColor, PorterDuff.Mode.SRC_ATOP);
		}
	}

	public void showNewProfileSavingDialog(@Nullable DialogInterface.OnShowListener showListener) {
		if (progress != null) {
			progress.dismiss();
		}
		progress = new ProgressDialog(getContext());
		progress.setMessage(getString(R.string.saving_new_profile));
		progress.setCancelable(false);
		progress.setOnShowListener(showListener);
		progress.show();
	}

	public void dismissProfileSavingDialog() {
		FragmentActivity activity = getActivity();
		if (progress != null && AndroidUtils.isActivityNotDestroyed(activity)) {
			progress.dismiss();
		}
	}

	@Override
	public void goBackWithoutSaving() {
		screenController.deleteImportedMode();
		if (getActivity() != null) {
			getActivity().onBackPressed();
		}
	}

	public void customProfileSaved() {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			if (activity instanceof MapActivity) {
				((MapActivity) activity).updateApplicationModeSettings();
			}
			FragmentManager fragmentManager = activity.getSupportFragmentManager();
			if (!fragmentManager.isStateSaved()) {
				fragmentManager.popBackStack();
				BaseSettingsFragment.showInstance(activity,
						SettingsScreenType.CONFIGURE_PROFILE, screenController.getChangedAppMode());
			}
		}
	}

	private void setupViewAnglePref() {
		Preference preference = findPreference(settings.VIEW_ANGLE_VISIBILITY.getId());
		preference.setIcon(getActiveIcon(R.drawable.ic_action_location_view_angle));
		preference.setSummary(screenController.getViewAngleVisibility().getNameId());
	}

	private void setupLocationRadiusPref() {
		Preference preference = findPreference(settings.LOCATION_RADIUS_VISIBILITY.getId());
		preference.setIcon(getActiveIcon(R.drawable.ic_action_location_radius));
		preference.setSummary(screenController.getLocationRadiusVisibility().getNameId());
	}

	private void disableSaveButtonWithErrorMessage(String errorMessage) {
		hasNameError = true;
		profileNameOtfb.setError(errorMessage, true);
		updateApplyButtonEnable();
	}

	private void setVerticalScrollBarEnabled(boolean enabled) {
		RecyclerView preferenceListView = getListView();
		if (enabled) {
			preferenceListView.post(() -> preferenceListView.setVerticalScrollBarEnabled(true));
		} else {
			preferenceListView.setVerticalScrollBarEnabled(false);
		}
	}

	@Override
	@ColorRes
	protected int getBackgroundColorRes() {
		return ColorUtilities.getActivityBgColorId(isNightMode());
	}

	@Override
	public void onAskDismissDialog(@NonNull String processId) {
		dismiss();
	}

	@Override
	public void hideKeyboard() {
		Activity activity = getActivity();
		if (activity != null) {
			View cf = activity.getCurrentFocus();
			AndroidUtils.hideSoftKeyboard(activity, cf);
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		screenController.checkSavingProfile();
	}

	@Override
	public void onPause() {
		super.onPause();
		screenController.onScreenPause();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		screenController.onScreenDestroy(getActivity());
	}

	public static boolean showInstance(@NonNull FragmentActivity activity,
	                                   @Nullable String appModeKey, boolean imported) {
		FragmentManager fragmentManager = activity.getSupportFragmentManager();
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			Fragment fragment = Fragment.instantiate(activity, TAG);
			Bundle args = new Bundle();
			if (appModeKey != null) {
				args.putString(BASE_PROFILE_FOR_NEW, appModeKey);
				args.putBoolean(IS_BASE_PROFILE_IMPORTED, imported);
			}
			fragment.setArguments(args);

			fragmentManager.beginTransaction()
					.replace(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(DRAWER_SETTINGS_ID)
					.commitAllowingStateLoss();
			return true;
		}
		return false;
	}
}
