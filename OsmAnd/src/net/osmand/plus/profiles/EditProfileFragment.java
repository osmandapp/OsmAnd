package net.osmand.plus.profiles;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AlertDialog.Builder;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListPopupWindow;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import net.osmand.AndroidUtils;
import net.osmand.PlatformUtil;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.ApplicationMode.ProfileIconColors;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.profiles.SelectProfileBottomSheetDialogFragment.SelectProfileListener;
import net.osmand.plus.routing.RouteProvider.RouteService;
import net.osmand.plus.settings.BaseSettingsFragment;
import net.osmand.plus.settings.BaseSettingsFragment.SettingsScreenType;
import net.osmand.plus.widgets.OsmandTextFieldBoxes;
import net.osmand.router.GeneralRouter;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import studio.carbonylgroup.textfieldboxes.ExtendedEditText;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.DRAWER_SETTINGS_ID;
import static net.osmand.plus.profiles.SelectProfileBottomSheetDialogFragment.DIALOG_TYPE;
import static net.osmand.plus.profiles.SelectProfileBottomSheetDialogFragment.SELECTED_KEY;
import static net.osmand.plus.profiles.SelectProfileBottomSheetDialogFragment.TYPE_BASE_APP_PROFILE;
import static net.osmand.plus.profiles.SelectProfileBottomSheetDialogFragment.TYPE_ICON;
import static net.osmand.plus.profiles.SelectProfileBottomSheetDialogFragment.TYPE_NAV_PROFILE;
import static net.osmand.plus.profiles.SettingsProfileFragment.IS_NEW_PROFILE;
import static net.osmand.plus.profiles.SettingsProfileFragment.IS_USER_PROFILE;
import static net.osmand.plus.profiles.SettingsProfileFragment.PROFILE_STRING_KEY;

public class EditProfileFragment extends BaseOsmAndFragment {

	private static final Log LOG = PlatformUtil.getLog(EditProfileFragment.class);

	public static final String TAG = EditProfileFragment.class.getSimpleName();

	public static final String OPEN_CONFIG_PROFILE = "openConfigProfile";
	public static final String OPEN_SETTINGS = "openSettings";
	public static final String OPEN_CONFIG_ON_MAP = "openConfigOnMap";
	public static final String MAP_CONFIG = "openMapConfigMenu";
	public static final String NAV_CONFIG = "openNavConfigMenu";
	public static final String SCREEN_CONFIG = "openScreenConfigMenu";
	public static final String SELECTED_ITEM = "editedProfile";
	public static final String SELECTED_ICON = "selectedIcon";

	OsmandApplication app;

	ApplicationMode mode = null;
	ApplicationProfileObject profile = null;
	List<RoutingProfileDataObject> routingProfileDataObjects;
	RoutingProfileDataObject selectedRoutingProfileDataObject = null;

	private boolean isNew = false;
	private boolean isUserProfile = false;
	private boolean dataChanged = false;
	private boolean nightMode;

	private SelectProfileListener navTypeListener = null;
	private SelectProfileListener iconIdListener = null;
	private SelectProfileListener baseTypeListener = null;

	private TextView toolbarTitle;
	private ImageView profileIcon;
	private LinearLayout profileIconBtn;
	private ImageView colorSample;
	private LinearLayout selectColorBtn;
	private ExtendedEditText profileNameEt;
	private OsmandTextFieldBoxes profileNameTextBox;
	private ExtendedEditText navTypeEt;
	private OsmandTextFieldBoxes navTypeTextBox;
	private FrameLayout selectNavTypeBtn;
	private Button cancelBtn;
	private Button saveButton;
	private View profileConfigBtn;
	private LinearLayout buttonsLayout;
	private FrameLayout clickBlockLayout;
	private LinearLayout typeSelectionBtn;
	private ImageView baseModeIcon;
	private TextView baseModeTitle;
	private ScrollView scrollContainer;
	private LinearLayout buttonsLayoutSV;
	private Button cancelBtnSV;
	private Button saveButtonSV;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = getMyApplication();
		if (getArguments() != null) {
			String modeName = getArguments().getString(PROFILE_STRING_KEY, "car");
			isNew = getArguments().getBoolean(IS_NEW_PROFILE, false);
			isUserProfile = getArguments().getBoolean(IS_USER_PROFILE, false);
			mode = ApplicationMode.valueOfStringKey(modeName, ApplicationMode.DEFAULT);
			profile = new ApplicationProfileObject(mode, isNew, isUserProfile);
		}
		nightMode = !app.getSettings().isLightContent();
		routingProfileDataObjects = getRoutingProfiles(app);
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
	                         @Nullable Bundle savedInstanceState) {
		final FragmentActivity activity = getActivity();

		int themeRes = nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;
		Context themedContext = new ContextThemeWrapper(getContext(), themeRes);
		final View view = inflater.cloneInContext(themedContext).inflate(R.layout.fragment_selected_profile, container, false);

		setupToolbar(view);

		toolbarTitle = view.findViewById(R.id.toolbar_title);
		profileIcon = view.findViewById(R.id.profile_icon_img);
		profileIconBtn = view.findViewById(R.id.select_icon_button);
		colorSample = view.findViewById(R.id.color_sample_img);
		selectColorBtn = view.findViewById(R.id.select_icon_color_button);
		profileNameEt = view.findViewById(R.id.profile_name_et);
		profileNameTextBox = view.findViewById(R.id.profile_name_otfb);
		navTypeEt = view.findViewById(R.id.navigation_type_et);
		navTypeTextBox = view.findViewById(R.id.navigation_type_otfb);
		selectNavTypeBtn = view.findViewById(R.id.select_nav_type_btn);
		cancelBtn = view.findViewById(R.id.cancel_button);
		saveButton = view.findViewById(R.id.save_profile_btn);
		profileConfigBtn = view.findViewById(R.id.profile_config_btn);
		buttonsLayout = view.findViewById(R.id.buttons_layout);
		clickBlockLayout = view.findViewById(R.id.click_block_layout);
		typeSelectionBtn = view.findViewById(R.id.type_selection_button);
		baseModeIcon = view.findViewById(R.id.mode_icon);
		baseModeTitle = view.findViewById(R.id.mode_title);
		scrollContainer = view.findViewById(R.id.scroll_view_container);
		buttonsLayoutSV = view.findViewById(R.id.buttons_layout_sv);
		cancelBtnSV = view.findViewById(R.id.cancel_button_sv);
		saveButtonSV = view.findViewById(R.id.save_profile_btn_sv);

		profileNameEt.setFocusable(true);
		profileNameTextBox.getEditText().setSelection(profileNameEt.getText().length());

		String title = getResources().getString(R.string.new_profile);

		int startIconId = profile.iconId;

		if (isNew) {
			dataChanged = true;
			startIconId = profile.parent.getIconRes();
			profile.iconId = startIconId;
			profile.iconStringName = profile.parent.getIconName();
		} else if (isUserProfile) {
			title = profile.userProfileTitle;
			profileNameEt.setText(title);
			dataChanged = false;
		} else if (profile.stringKeyName != -1) {
			title = getResources().getString(profile.stringKeyName);
			profileNameEt.setText(title);
			clickBlockLayout.setClickable(true);
		}
		profile.userProfileTitle = title;

		if (profile.parent != null) {
			setupBaseProfileView(profile.parent.getStringKey());
		} else if (profile.stringKeyName != -1) {
			baseModeTitle.setText(profile.stringKeyName);
			baseModeIcon.setImageDrawable(
				app.getUIUtilities().getIcon(profile.iconId, R.color.icon_color_default_light));
		}
		if (isUserProfile || isNew) {
			typeSelectionBtn.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					final SelectProfileBottomSheetDialogFragment dialog = new SelectProfileBottomSheetDialogFragment();
					Bundle bundle = new Bundle();
					if (profile.parent != null) {
						bundle.putString(SELECTED_KEY, profile.parent.getStringKey());
					}
					bundle.putString(DIALOG_TYPE, TYPE_BASE_APP_PROFILE);
					dialog.setArguments(bundle);
					if (getActivity() != null) {
						getActivity().getSupportFragmentManager().beginTransaction()
							.add(dialog, "select_base_type").commitAllowingStateLoss();
					}
				}
			});
		} else {
			typeSelectionBtn.setClickable(false);
		}

		if (!Algorithms.isEmpty(mode.getRoutingProfile()) || mode.getRouteService() != RouteService.OSMAND) {
			for (RoutingProfileDataObject r : routingProfileDataObjects) {
				if (mode.getRoutingProfile() != null && mode.getRoutingProfile().equals(r.getStringKey()) 
					|| (mode.getRouteService() == RouteService.BROUTER
					&& r.getStringKey().equals(RoutingProfilesResources.BROUTER_MODE.name())) 
					|| (mode.getRouteService() == RouteService.STRAIGHT
					&& r.getStringKey().equals(RoutingProfilesResources.STRAIGHT_LINE_MODE.name()))) {
					profile.routingProfileDataObject = r;
					r.setSelected(true);
					navTypeEt.setText(r.getName());
					navTypeEt.clearFocus();
				}
			}
		} else {
			for (RoutingProfileDataObject rp : routingProfileDataObjects) {
				if (profile.stringKey.equals(rp.getStringKey())) {
					navTypeEt.setText(
						RoutingProfilesResources.valueOf(rp.getStringKey().toUpperCase())
							.getStringRes());
				}
			}
			navTypeEt.clearFocus();
		}
		profileNameEt.clearFocus();

		updateToolbar(title);

		int iconColor = profile.iconColor.getColor(nightMode);

		profileIcon.setImageDrawable(app.getUIUtilities().getIcon(startIconId, iconColor));
		colorSample.setImageDrawable(app.getUIUtilities().getIcon(R.drawable.ic_action_circle, iconColor));

		profileNameEt.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}

			@Override
			public void afterTextChanged(Editable s) {
				profile.userProfileTitle = s.toString();
				dataChanged = true;

				updateToolbar(s.toString());
			}
		});

		selectNavTypeBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (isNew || isUserProfile) {
					hideKeyboard();
					final SelectProfileBottomSheetDialogFragment fragment = new SelectProfileBottomSheetDialogFragment();
					Bundle bundle = new Bundle();
					if (profile.routingProfileDataObject != null) {
						bundle.putString(SELECTED_KEY,
							profile.routingProfileDataObject.getStringKey());
					}
					bundle.putString(DIALOG_TYPE, TYPE_NAV_PROFILE);
					fragment.setArguments(bundle);
					if (getActivity() != null) {
						getActivity().getSupportFragmentManager().beginTransaction()
							.add(fragment, "select_nav_type").commitAllowingStateLoss();
					}
					navTypeEt.setCursorVisible(false);
					navTypeEt.setTextIsSelectable(false);
					navTypeEt.clearFocus();
					navTypeEt.requestFocus(ExtendedEditText.FOCUS_UP);
				}
			}
		});

		if (isUserProfile || isNew) {
			profileIconBtn.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					final SelectProfileBottomSheetDialogFragment iconSelectDialog = new SelectProfileBottomSheetDialogFragment();
					Bundle bundle = new Bundle();
					bundle.putString(DIALOG_TYPE, TYPE_ICON);
					bundle.putString(SELECTED_ICON, profile.iconStringName);
					iconSelectDialog.setArguments(bundle);
					hideKeyboard();
					if (getActivity() != null) {
						getActivity().getSupportFragmentManager().beginTransaction()
							.add(iconSelectDialog, "select_icon")
							.commitAllowingStateLoss();
					}
				}
			});

			selectColorBtn.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					final ListPopupWindow popupWindow = new ListPopupWindow(activity);
					popupWindow.setAnchorView(selectColorBtn);
					popupWindow.setContentWidth(AndroidUtils.dpToPx(activity, 200f));
					popupWindow.setModal(true);
					if (Build.VERSION.SDK_INT >= VERSION_CODES.KITKAT) {
						popupWindow.setDropDownGravity(Gravity.TOP | Gravity.RIGHT);
					}
					popupWindow.setVerticalOffset(AndroidUtils.dpToPx(activity, -48f));
					popupWindow.setHorizontalOffset(AndroidUtils.dpToPx(activity, -6f));
					final ProfileColorAdapter profileColorAdapter = new ProfileColorAdapter(activity, mode.getIconColorInfo());
					popupWindow.setAdapter(profileColorAdapter);
					popupWindow.setOnItemClickListener(new OnItemClickListener() {
						@Override
						public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
							dataChanged = true;
							profile.iconColor = ProfileIconColors.values()[position];
							profileIcon.setImageDrawable(app.getUIUtilities().getIcon(profile.iconId, profile.iconColor.getColor(nightMode)));
							colorSample.setImageDrawable(app.getUIUtilities().getIcon(R.drawable.ic_action_circle, profile.iconColor.getColor(nightMode)));
							popupWindow.dismiss();
						}
					});
					popupWindow.show();
				}
			});
		} else {
			if (VERSION.SDK_INT > VERSION_CODES.LOLLIPOP) {
				selectColorBtn.setBackground(null);
				profileIconBtn.setBackground(null);
			} else {
				selectColorBtn.setBackgroundDrawable(null);
				profileIconBtn.setBackgroundDrawable(null);
			}

		}


		profileConfigBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (dataChanged) {
					needSaveDialog();
				} else if (getSettings() != null) {
					activateMode(mode);
					getSettings().APPLICATION_MODE.set(mode);

					if (activity instanceof EditProfileActivity) {
						Intent i = new Intent(getActivity(), MapActivity.class);
						i.putExtra(OPEN_SETTINGS, OPEN_CONFIG_PROFILE);
						i.putExtra(SELECTED_ITEM, profile.stringKey);
						startActivity(i);
					} else {
						BaseSettingsFragment.showInstance(activity, SettingsScreenType.CONFIGURE_PROFILE);
					}
				}
			}
		});

		cancelBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (getActivity() != null) {
					getActivity().onBackPressed();
				}
			}
		});

		cancelBtnSV.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (getActivity() != null) {
					getActivity().onBackPressed();
				}
			}
		});

		if (!isNew && !isUserProfile) {
			saveButtonSV.setEnabled(false);
			saveButton.setEnabled(false);
		} else {
			saveButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					if (saveNewProfile()) {
						activateMode(mode);
						getActivity().onBackPressed();
					}
				}
			});

			saveButtonSV.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					if (saveNewProfile()) {
						activateMode(mode);
						getActivity().onBackPressed();
					}
				}
			});
		}

		final float d = getResources().getDisplayMetrics().density;
		view.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
			@Override
			public void onGlobalLayout() {
				int marginShow = 66;
				int marginHide = 0;

				Rect r = new Rect();
				view.getWindowVisibleDisplayFrame(r);
				int screenHeight = view.getRootView().getHeight();
				int keypadHeight = screenHeight - r.bottom;
				if (keypadHeight > screenHeight * 0.15) {
					buttonsLayout.setVisibility(View.GONE);
					buttonsLayoutSV.setVisibility(View.VISIBLE);
					setMargins(scrollContainer, 0, 0, 0, (int) (marginHide * d));
				} else {
					buttonsLayoutSV.setVisibility(View.GONE);
					buttonsLayout.setVisibility(View.VISIBLE);
					setMargins(scrollContainer, 0, 0, 0, (int) (marginShow * d));
				}
			}
		});
		return view;
	}

	@Override
	public void onResume() {
		getBaseProfileListener();
		getNavProfileListener();
		getIconListener();
		super.onResume();
	}

	@Override
	public int getStatusBarColorId() {
		return nightMode ? R.color.status_bar_color_dark : R.color.status_bar_color_light;
	}

	public boolean onBackPressedAllowed() {
		return !dataChanged;
	}

	SelectProfileListener getIconListener() {
		if (iconIdListener == null) {
			iconIdListener = new SelectProfileListener() {
				@Override
				public void onSelectedType(int pos, String stringRes) {
					dataChanged = true;
					profile.iconId = pos;
					profile.iconStringName = stringRes;
					profileIcon.setImageDrawable(app.getUIUtilities().getIcon(pos,
						profile.iconColor.getColor(nightMode)));

				}
			};
		}
		return iconIdListener;
	}

	SelectProfileListener getBaseProfileListener() {
		if (baseTypeListener == null) {
			baseTypeListener = new SelectProfileListener() {
				@Override
				public void onSelectedType(int pos, String stringRes) {
					String key = SettingsProfileFragment.getBaseProfiles(getMyApplication())
						.get(pos).getStringKey();
					setupBaseProfileView(key);
					profile.parent = ApplicationMode.valueOfStringKey(key, ApplicationMode.DEFAULT);
					dataChanged = true;
				}
			};
		}
		return baseTypeListener;
	}

	SelectProfileListener getNavProfileListener() {
		if (navTypeListener == null) {
			navTypeListener = new SelectProfileListener() {
				@Override
				public void onSelectedType(int pos, String stringRes) {
					updateRoutingProfile(pos);
				}
			};
		}
		return navTypeListener;
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		if (!isUserProfile && !isNew) {
			profileNameEt.setFocusable(false);
			navTypeEt.setFocusable(false);
		}

		if (isNew) {
			profileNameEt.requestFocus();
		} else {
			scrollContainer.requestFocus();
		}

	}

	void updateRoutingProfile(int pos) {
		dataChanged = true;
		for (int i = 0; i < routingProfileDataObjects.size(); i++) {
			if (i == pos) {
				routingProfileDataObjects.get(i).setSelected(true);
			} else {
				routingProfileDataObjects.get(i).setSelected(false);
			}
		}
		selectedRoutingProfileDataObject = routingProfileDataObjects.get(pos);
		navTypeEt.setText(selectedRoutingProfileDataObject.getName());
		profile.routingProfileDataObject = selectedRoutingProfileDataObject;
	}

	void activateMode(ApplicationMode mode) {
		if (!ApplicationMode.values(app).contains(mode)) {
			ApplicationMode.changeProfileAvailability(mode, true, getMyApplication());
		}
	}

	private void setupToolbar(View view) {
		FragmentActivity activity = getActivity();
		AppBarLayout appBar = (AppBarLayout) view.findViewById(R.id.appbar);

		if ((activity instanceof EditProfileActivity)) {
			EditProfileActivity editProfileActivity = (EditProfileActivity) activity;
			if (editProfileActivity.getSupportActionBar() != null) {
				editProfileActivity.getSupportActionBar().setElevation(5.0f);
			}
			AndroidUiHelper.updateVisibility(appBar, false);
		} else {
			AndroidUtils.addStatusBarPadding21v(activity, view);
			ViewCompat.setElevation(appBar, 5.0f);

			View closeButton = view.findViewById(R.id.close_button);
			closeButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					FragmentActivity fragmentActivity = getActivity();
					if (fragmentActivity != null) {
						fragmentActivity.onBackPressed();
					}
				}
			});

			View deleteBtn = view.findViewById(R.id.delete_button);
			deleteBtn.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					onDeleteProfileClick();
				}
			});
		}
	}

	private void updateToolbar(String title) {
		FragmentActivity activity = getActivity();
		if (activity instanceof EditProfileActivity) {
			EditProfileActivity editProfileActivity = (EditProfileActivity) activity;
			if (editProfileActivity.getSupportActionBar() != null) {
				editProfileActivity.getSupportActionBar().setTitle(title);
			}
		} else {
			toolbarTitle.setText(title);
		}
	}

	private void setupBaseProfileView(String stringKey) {
		for (ApplicationMode am : ApplicationMode.getDefaultValues()) {
			if (am.getStringKey().equals(stringKey)) {
				baseModeIcon.setImageDrawable(
					app.getUIUtilities().getIcon(am.getIconRes(), R.color.icon_color_default_light));
				baseModeTitle.setText(Algorithms.capitalizeFirstLetter(am.toHumanString(app)));
				dataChanged = false;
			}
		}
	}

	private void setMargins(View v, int l, int t, int r, int b) {
		if (v.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
			ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
			p.setMargins(l, t, r, b);
			v.requestLayout();
		}
	}

	private boolean saveNewProfile() {
		if (profile.routingProfileDataObject == null) {
			showSaveWarningDialog(
				getString(R.string.profile_alert_need_routing_type_title),
				getString(R.string.profile_alert_need_routing_type_msg),
				getActivity());
			return false;
		}

		if (profile.userProfileTitle.isEmpty()
			|| profile.userProfileTitle.replace(" ", "").length() < 1 
			|| profileNameEt.getText().toString().replace(" ", "").length() < 1) {
			showSaveWarningDialog(
				getString(R.string.profile_alert_need_profile_name_title),
				getString(R.string.profile_alert_need_profile_name_msg),
				getActivity()
			);
			return false;
		}

		for (ApplicationMode m : ApplicationMode.allPossibleValues()) {
			if (m.getCustomProfileName() != null && getActivity() != null &&
					m.getCustomProfileName().equals(profile.userProfileTitle) && isNew) {
				AlertDialog.Builder bld = new AlertDialog.Builder(getActivity());
				bld.setTitle(R.string.profile_alert_duplicate_name_title);
				bld.setMessage(R.string.profile_alert_duplicate_name_msg);
				bld.setNegativeButton(R.string.shared_string_dismiss, null);
				bld.show();
				bld.setOnDismissListener(new OnDismissListener() {
					@Override
					public void onDismiss(DialogInterface dialog) {
						profileNameEt.requestFocus();
					}
				});
				return false;
			}
		}
		String customStringKey = profile.stringKey;
		if (isNew) {
			customStringKey =
				profile.parent.getStringKey() + "_" + System.currentTimeMillis();
		}

		ApplicationMode.ApplicationModeBuilder builder = ApplicationMode
			.createCustomMode(profile.parent, profile.userProfileTitle.trim(), customStringKey)
			.icon(app, profile.iconStringName);

		if(profile.routingProfileDataObject.getStringKey().equals(
				RoutingProfilesResources.STRAIGHT_LINE_MODE.name())) {
			builder.setRouteService(RouteService.STRAIGHT);
		} else if(profile.routingProfileDataObject.getStringKey().equals(
				RoutingProfilesResources.BROUTER_MODE.name())) {
			builder.setRouteService(RouteService.BROUTER);
		} else if (profile.routingProfileDataObject != null) {
			builder.setRoutingProfile(profile.routingProfileDataObject.getStringKey());
		}
		builder.setColor(profile.iconColor);

		mode = ApplicationMode.saveCustomProfile(builder, getMyApplication());
		if (!ApplicationMode.values(app).contains(mode)) {
			ApplicationMode.changeProfileAvailability(mode, true, getMyApplication());
		}
		dataChanged = false;
		return true;
	}

	private void needSaveDialog() {
		if (getActivity() != null) {
			AlertDialog.Builder bld = new AlertDialog.Builder(getActivity());
			bld.setTitle(R.string.profile_alert_need_save_title);
			bld.setMessage(R.string.profile_alert_need_save_msg);
			bld.setNegativeButton(R.string.shared_string_ok, null);
			bld.show();
		}
	}

	private void showSaveWarningDialog(String title, String message, Activity activity) {
		AlertDialog.Builder bld = new AlertDialog.Builder(activity);
		bld.setTitle(title);
		bld.setMessage(message);
		bld.setNegativeButton(R.string.shared_string_dismiss, null);
		bld.show();
	}

	public void confirmCancelDialog(final Activity activity) {
		AlertDialog.Builder bld = new Builder(activity);
		bld.setTitle(R.string.shared_string_dismiss);
		bld.setMessage(R.string.exit_without_saving);
		bld.setPositiveButton(R.string.shared_string_ok, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dataChanged = false;
				activity.onBackPressed();
			}
		});
		bld.setNegativeButton(R.string.shared_string_cancel, null);
		bld.show();

	}

	void onDeleteProfileClick() {
		if (getActivity() != null) {
			if (isUserProfile) {
				AlertDialog.Builder bld = new AlertDialog.Builder(getActivity());
				bld.setTitle(R.string.profile_alert_delete_title);
				bld.setMessage(String
					.format(getString(R.string.profile_alert_delete_msg),
						profile.userProfileTitle));
				bld.setPositiveButton(R.string.shared_string_delete,
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							OsmandApplication app = getMyApplication();
							if (app != null) {
								ApplicationMode.deleteCustomMode(mode, app);
								app.getSettings().APPLICATION_MODE.set(ApplicationMode.DEFAULT);
							}

							if (getActivity() != null) {
								getActivity().onBackPressed();
							}
						}
					});
				bld.setNegativeButton(R.string.shared_string_dismiss, null);
				bld.show();
			} else {
				Toast.makeText(getActivity(), R.string.profile_alert_cant_delete_base,
					Toast.LENGTH_SHORT).show();
			}
		}
	}
	
	private void hideKeyboard() {
		View cf = getActivity().getCurrentFocus();
		if (cf != null) {
			InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
			if (imm != null) {
				imm.hideSoftInputFromWindow(cf.getWindowToken(), 0);
			}
		}
	}

	public static boolean showInstance(FragmentManager fragmentManager, boolean newProfile, boolean userProfile, String profileKey) {
		try {
			Bundle args = new Bundle();
			args.putBoolean(IS_NEW_PROFILE, newProfile);
			args.putBoolean(IS_USER_PROFILE, userProfile);
			args.putString(PROFILE_STRING_KEY, profileKey);

			EditProfileFragment editProfileFragment = new EditProfileFragment();
			editProfileFragment.setArguments(args);

			fragmentManager.beginTransaction()
					.replace(R.id.fragmentContainer, editProfileFragment, TAG)
					.addToBackStack(DRAWER_SETTINGS_ID + ".new")
					.commit();
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	static List<RoutingProfileDataObject> getRoutingProfiles(OsmandApplication context) {
		List<RoutingProfileDataObject> profilesObjects = new ArrayList<>();
		profilesObjects.add(new RoutingProfileDataObject(
			RoutingProfilesResources.STRAIGHT_LINE_MODE.name(),
			context.getString(RoutingProfilesResources.STRAIGHT_LINE_MODE.getStringRes()),
			context.getString(R.string.special_routing_type),
			RoutingProfilesResources.STRAIGHT_LINE_MODE.getIconRes(),
			false, null));
		if (context.getBRouterService() != null) {
			profilesObjects.add(new RoutingProfileDataObject(
				RoutingProfilesResources.BROUTER_MODE.name(),
				context.getString(RoutingProfilesResources.BROUTER_MODE.getStringRes()),
				context.getString(R.string.third_party_routing_type),
				RoutingProfilesResources.BROUTER_MODE.getIconRes(),
				false, null));
		}

		Map<String, GeneralRouter> inputProfiles = context.getRoutingConfig().getAllRouters();
		for (Entry<String, GeneralRouter> e : inputProfiles.entrySet()) {
			if (!e.getKey().equals("geocoding")) {
				int iconRes = R.drawable.ic_action_gdirections_dark;
				String name = e.getValue().getProfileName();
				String description = context.getString(R.string.osmand_default_routing);
				if (!Algorithms.isEmpty(e.getValue().getFilename())) {
					description = e.getValue().getFilename();
				} else if (RoutingProfilesResources.isRpValue(name.toUpperCase())){
					iconRes = RoutingProfilesResources.valueOf(name.toUpperCase()).getIconRes();
					name = context
						.getString(RoutingProfilesResources.valueOf(name.toUpperCase()).getStringRes());
				}
				profilesObjects.add(new RoutingProfileDataObject(e.getKey(), name, description,
					iconRes, false, e.getValue().getFilename()));
			}
		}
		return profilesObjects;
	}

	public enum RoutingProfilesResources {
		STRAIGHT_LINE_MODE(R.string.routing_profile_straightline, R.drawable.ic_action_split_interval),
		BROUTER_MODE(R.string.routing_profile_broutrer, R.drawable.ic_action_split_interval),
		CAR(R.string.rendering_value_car_name, R.drawable.ic_action_car_dark),
		PEDESTRIAN(R.string.rendering_value_pedestrian_name, R.drawable.ic_action_pedestrian_dark),
		BICYCLE(R.string.rendering_value_bicycle_name, R.drawable.ic_action_bicycle_dark),
		SKI(R.string.routing_profile_ski, R.drawable.ic_action_skiing),
		PUBLIC_TRANSPORT(R.string.app_mode_public_transport, R.drawable.ic_action_bus_dark),
		BOAT(R.string.app_mode_boat, R.drawable.ic_action_sail_boat_dark),
		GEOCODING(R.string.routing_profile_geocoding, R.drawable.ic_action_world_globe);

		int stringRes;
		int iconRes;

		RoutingProfilesResources(int stringRes, int iconRes) {
			this.stringRes = stringRes;
			this.iconRes = iconRes;
		}

		public int getStringRes() {
			return stringRes;
		}

		public int getIconRes() {
			return iconRes;
		}

		private static final List<String> rpValues = new ArrayList<>();

		static {
			for (RoutingProfilesResources rpr : RoutingProfilesResources.values()) {
				rpValues.add(rpr.name());
			}
		}

		public static boolean isRpValue(String value) {
			return rpValues.contains(value);
		}
	}

	private class ApplicationProfileObject {

		int stringKeyName = -1;
		String stringKey;
		String userProfileTitle = "";
		ApplicationMode parent = null;
		int iconId = R.drawable.ic_world_globe_dark;
		String iconStringName = "ic_world_globe_dark";
		ProfileIconColors iconColor = ProfileIconColors.DEFAULT;
		RoutingProfileDataObject routingProfileDataObject = null;

		ApplicationProfileObject(ApplicationMode mode, boolean isNew, boolean isUserProfile) {
			if (isNew) {
				stringKey = mode.getStringKey() + System.currentTimeMillis();
				parent = mode;
				iconStringName = parent.getIconName();
			} else if (isUserProfile) {
				stringKey = mode.getStringKey();
				parent = mode.getParent();
				iconId = mode.getIconRes();
				iconStringName = mode.getIconName();
				iconColor = mode.getIconColorInfo() == null ? ProfileIconColors.DEFAULT : mode.getIconColorInfo();
				userProfileTitle = mode.getCustomProfileName();
			} else {
				stringKeyName = mode.getNameKeyResource();
				stringKey = mode.getStringKey();
				iconId = mode.getIconRes();
				iconStringName = mode.getIconName();
			}
		}
	}

	public static class ProfileColorAdapter extends ArrayAdapter<ColorListItem> {

		private OsmandApplication app;
		private ProfileIconColors currentColorData;


		public ProfileColorAdapter(Context context, ProfileIconColors iconColorData) {
			super(context, R.layout.rendering_prop_menu_item);
			this.app = (OsmandApplication) getContext().getApplicationContext();
			this.currentColorData = iconColorData;
			init();
		}

		public void init() {
			boolean nightMode = !app.getSettings().isLightContent();
			String currentColorName = app.getString(ProfileIconColors.DEFAULT.getName());
			ColorListItem item = new ColorListItem(currentColorName, currentColorName, ProfileIconColors.DEFAULT.getColor(nightMode));
			add(item);
			for (ProfileIconColors pic : ProfileIconColors.values()) {
				if (pic != ProfileIconColors.DEFAULT) {
					item = new ColorListItem(currentColorName, app.getString(pic.getName()), pic.getColor(nightMode));
					add(item);
				}
			}
			item.setLastItem(true);
		}

		@NonNull
		@Override
		public View getView(int position, View convertView, @NonNull ViewGroup parent) {
			ColorListItem item = getItem(position);
			View v = convertView;
			if (v == null) {
				v = LayoutInflater.from(getContext()).inflate(R.layout.rendering_prop_menu_item, null);
			}
			if (item != null) {
				TextView textView = (TextView) v.findViewById(R.id.text1);
				textView.setText(item.valueName);
				if (item.color == -1) {
					textView.setCompoundDrawablesWithIntrinsicBounds(null, null,
						app.getUIUtilities().getThemedIcon(R.drawable.ic_action_circle), null);
				} else {
					textView.setCompoundDrawablesWithIntrinsicBounds(null, null,
						app.getUIUtilities().getIcon(R.drawable.ic_action_circle, item.color), null);
				}

				textView.setCompoundDrawablePadding(AndroidUtils.dpToPx(getContext(), 10f));
				v.findViewById(R.id.divider).setVisibility(item.lastItem
					&& position < getCount() - 1 ? View.VISIBLE : View.GONE);
			}
			return v;
		}
	}

	public static class ColorListItem {
		private String currentValueName;
		private String valueName;
		private int color;
		private boolean lastItem;

		public ColorListItem(String currentValueName, String valueName, int color) {
			this.currentValueName = currentValueName;
			this.valueName = valueName;
			this.color = color;
		}



		public int getColor() {
			return color;
		}

		public boolean isLastItem() {
			return lastItem;
		}

		public void setLastItem(boolean lastItem) {
			this.lastItem = lastItem;
		}
	}
}
