package net.osmand.plus.profiles;

import static net.osmand.plus.activities.SettingsNavigationActivity.INTENT_SKIP_DIALOG;
import static net.osmand.plus.profiles.ProfileBottomSheetDialogFragment.DIALOG_TYPE;
import static net.osmand.plus.profiles.ProfileBottomSheetDialogFragment.SELECTED_KEY;
import static net.osmand.plus.profiles.ProfileBottomSheetDialogFragment.TYPE_APP_PROFILE;
import static net.osmand.plus.profiles.ProfileBottomSheetDialogFragment.TYPE_NAV_PROFILE;
import static net.osmand.plus.profiles.SettingsProfileFragment.IS_NEW_PROFILE;
import static net.osmand.plus.profiles.SettingsProfileFragment.IS_USER_PROFILE;
import static net.osmand.plus.profiles.SettingsProfileFragment.PROFILE_STRING_KEY;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.osmand.PlatformUtil;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.OsmandActionBarActivity;
import net.osmand.plus.activities.SettingsNavigationActivity;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.profiles.ProfileBottomSheetDialogFragment.ProfileTypeDialogListener;
import net.osmand.plus.profiles.SelectIconBottomSheetDialogFragment.IconIdListener;
import net.osmand.plus.profiles.SelectProfileBottomSheetDialogFragment.SelectProfileListener;
import net.osmand.plus.routing.RouteProvider.RouteService;
import net.osmand.plus.widgets.OsmandTextFieldBoxes;
import net.osmand.router.GeneralRouter;
import net.osmand.util.Algorithms;
import org.apache.commons.logging.Log;
import studio.carbonylgroup.textfieldboxes.ExtendedEditText;

public class EditProfileFragment extends BaseOsmAndFragment {

	private static final Log LOG = PlatformUtil.getLog(EditProfileFragment.class);

	public static final String OPEN_CONFIG_ON_MAP = "openConfigOnMap";
	public static final String MAP_CONFIG = "openMapConfigMenu";
	public static final String NAV_CONFIG = "openNavConfigMenu";
	public static final String SCREEN_CONFIG = "openScreenConfigMenu";
	public static final String SELECTED_PROFILE = "editedProfile";
	public static final String SELECTED_ICON = "selectedIcon";

	OsmandApplication app;

	ApplicationMode mode = null;
	ApplicationProfileObject profile = null;
	List<RoutingProfileDataObject> routingProfileDataObjects;
	RoutingProfileDataObject selectedRoutingProfileDataObject = null;

	private boolean isNew = false;
	private boolean isUserProfile = false;
	private boolean isDataChanged = false;

	private SelectProfileListener navTypeListener = null;
	private IconIdListener iconIdListener = null;
	private SelectProfileListener baseTypeListener = null;

	private ImageView profileIcon;
	private LinearLayout profileIconBtn;
	private ExtendedEditText profileNameEt;
	private OsmandTextFieldBoxes profileNameTextBox;
	private ExtendedEditText navTypeEt;
	private OsmandTextFieldBoxes navTypeTextBox;
	private FrameLayout selectNavTypeBtn;
	private Button cancelBtn;
	private Button saveButton;
	private View mapConfigBtn;
	private View screenConfigBtn;
	private View navConfigBtn;
	private LinearLayout buttonsLayout;
	private FrameLayout clickBlockLayout;
	private LinearLayout typeSelectionBtn;
	private ImageView baseModeIcon;
	private TextView baseModeTitle;
	private ScrollView scrollContainer;
	private LinearLayout buttonsLayoutSV;
	private Button cancelBtnSV;
	private Button saveButtonSV;

	boolean isNightMode;

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
		isNightMode = !app.getSettings().isLightContent();
		routingProfileDataObjects = getRoutingProfiles(app);
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
		@Nullable Bundle savedInstanceState) {

		final View view = inflater.inflate(R.layout.fragment_selected_profile, container, false);

		profileIcon = view.findViewById(R.id.select_icon_btn_img);
		profileIconBtn = view.findViewById(R.id.profile_icon_layout);
		profileNameEt = view.findViewById(R.id.profile_name_et);
		profileNameTextBox = view.findViewById(R.id.profile_name_otfb);
		navTypeEt = view.findViewById(R.id.navigation_type_et);
		navTypeTextBox = view.findViewById(R.id.navigation_type_otfb);
		selectNavTypeBtn = view.findViewById(R.id.select_nav_type_btn);
		cancelBtn = view.findViewById(R.id.cancel_button);
		saveButton = view.findViewById(R.id.save_profile_btn);
		mapConfigBtn = view.findViewById(R.id.map_config_btn);
		screenConfigBtn = view.findViewById(R.id.screen_config_btn);
		navConfigBtn = view.findViewById(R.id.nav_settings_btn);
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
		profileNameEt.setSelectAllOnFocus(true);
		profileIconBtn.setBackgroundResource(R.drawable.rounded_background_3dp);
		GradientDrawable selectIconBtnBackground = (GradientDrawable) profileIconBtn
			.getBackground();

		if (isNightMode) {
			profileNameTextBox
				.setPrimaryColor(ContextCompat.getColor(app, R.color.color_dialog_buttons_dark));
			navTypeTextBox
				.setPrimaryColor(ContextCompat.getColor(app, R.color.color_dialog_buttons_dark));
			selectIconBtnBackground
				.setColor(app.getResources().getColor(R.color.text_field_box_dark));
		} else {
			selectIconBtnBackground
				.setColor(app.getResources().getColor(R.color.text_field_box_light));
		}

		String title = "New Profile";

		int startIconId = R.drawable.map_world_globe_dark;

		if (isNew) {
			isDataChanged = true;
			startIconId = profile.parent.getSmallIconDark();
			profile.iconId = startIconId;
		} else if (isUserProfile) {
			title = profile.userProfileTitle;
			profileNameEt.setText(title);
			startIconId = profile.iconId;
			isDataChanged = false;
		} else if (profile.key != -1) {
			title = getResources().getString(profile.key);
			profileNameEt.setText(profile.key);
			startIconId = profile.iconId;
			clickBlockLayout.setClickable(true);
		}
		profile.userProfileTitle = title;

		if (profile.parent != null) {
			setupBaseProfileView(profile.parent.getStringKey());
		} else if (profile.key != -1) {
			baseModeTitle.setText(profile.key);
			baseModeIcon.setImageDrawable(
				app.getUIUtilities().getIcon(profile.iconId, R.color.icon_color));
		}
		typeSelectionBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (isUserProfile || isNew) {
					//final ProfileBottomSheetDialogFragment dialog = new ProfileBottomSheetDialogFragment();
					final SelectProfileBottomSheetDialogFragment dialog = new SelectProfileBottomSheetDialogFragment();
					Bundle bundle = new Bundle();
					if (profile.parent != null) {
						bundle.putString(SELECTED_KEY, profile.parent.getStringKey());
					}
					bundle.putString(DIALOG_TYPE, TYPE_APP_PROFILE);
					dialog.setArguments(bundle);
					if (getActivity() != null) {
						getActivity().getSupportFragmentManager().beginTransaction()
							.add(dialog, "select_base_type").commitAllowingStateLoss();
					}
				}
			}
		});

		if (!Algorithms.isEmpty(mode.getRoutingProfile())) {
			for (RoutingProfileDataObject r : routingProfileDataObjects) {
				if (mode.getRoutingProfile().equals(r.getStringKey())) {
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

		if (getActivity() != null
			&& ((EditProfileActivity) getActivity()).getSupportActionBar() != null) {
			((EditProfileActivity) getActivity()).getSupportActionBar().setTitle(title);
			((EditProfileActivity) getActivity()).getSupportActionBar().setElevation(5.0f);
		}

		int iconColor;
		if (!isUserProfile) {
			iconColor = R.color.icon_color;
		} else {
			iconColor = isNightMode
				? R.color.active_buttons_and_links_dark
				: R.color.active_buttons_and_links_light;
		}

		profileIcon.setImageDrawable(app.getUIUtilities().getIcon(startIconId, iconColor));

		profileNameEt.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}

			@Override
			public void afterTextChanged(Editable s) {
				if (getActivity() instanceof OsmandActionBarActivity) {
					ActionBar actionBar = ((OsmandActionBarActivity) getActivity())
						.getSupportActionBar();
					if (actionBar != null) {
						actionBar.setTitle(s.toString());
						profile.userProfileTitle = s.toString();
					}
				}
			}
		});

		selectNavTypeBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (isNew || isUserProfile) {
					//ProfileBottomSheetDialogFragment fragment = new ProfileBottomSheetDialogFragment();
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

		profileIconBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				final SelectIconBottomSheetDialogFragment iconSelectDialog = new SelectIconBottomSheetDialogFragment();
				iconSelectDialog.setIconIdListener(iconIdListener);
				Bundle bundle = new Bundle();
				bundle.putInt(SELECTED_ICON, profile.iconId);
				iconSelectDialog.setArguments(bundle);
				if (getActivity() != null) {
					getActivity().getSupportFragmentManager().beginTransaction()
						.add(iconSelectDialog, "select_icon")
						.commitAllowingStateLoss();
				}
			}
		});

		mapConfigBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (isDataChanged) {
					needSaveDialog();
				} else if (getSettings() != null) {
					activateMode(mode);
					getSettings().APPLICATION_MODE.set(mode);
					Intent i = new Intent(getActivity(), MapActivity.class);
					i.putExtra(OPEN_CONFIG_ON_MAP, MAP_CONFIG);
					i.putExtra(SELECTED_PROFILE, profile.stringKey);
					startActivity(i);
				}
			}
		});

		screenConfigBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (isDataChanged) {
					needSaveDialog();
				} else if (getSettings() != null) {
					activateMode(mode);
					getSettings().APPLICATION_MODE.set(mode);
					Intent i = new Intent(getActivity(), MapActivity.class);
					i.putExtra(OPEN_CONFIG_ON_MAP, SCREEN_CONFIG);
					i.putExtra(SELECTED_PROFILE, profile.stringKey);
					startActivity(i);
				}
			}
		});

		navConfigBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (isDataChanged) {
					needSaveDialog();
				} else if (getSettings() != null) {
					activateMode(mode);
					getSettings().APPLICATION_MODE.set(mode);
					Intent i = new Intent(getActivity(), SettingsNavigationActivity.class);
					i.putExtra(INTENT_SKIP_DIALOG, true);
					i.putExtra(OPEN_CONFIG_ON_MAP, NAV_CONFIG);
					i.putExtra(SELECTED_PROFILE, profile.stringKey);
					startActivity(i);
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

		view.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
			@Override
			public void onGlobalLayout() {
				int marginShow = 66;
				int marginHide = 0;

				float d = getResources().getDisplayMetrics().density;
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
		baseTypeListener = new SelectProfileListener() {
			@Override
			public void onSelectedType(int pos) {
				String key = SettingsProfileFragment.getBaseProfiles(getMyApplication()).get(pos)
					.getStringKey();
				setupBaseProfileView(key);
				profile.parent = ApplicationMode.valueOfStringKey(key, ApplicationMode.DEFAULT);
			}
		};

		navTypeListener = new SelectProfileListener() {
			@Override
			public void onSelectedType(int pos) {
				updateRoutingProfile(pos);
			}
		};

		iconIdListener = new IconIdListener() {
			@Override
			public void selectedIconId(int iconRes) {
				isDataChanged = true;
				profile.iconId = iconRes;
				profileIcon.setImageDrawable(app.getUIUtilities().getIcon(iconRes,
					isNightMode ? R.color.active_buttons_and_links_dark
						: R.color.active_buttons_and_links_light));
			}
		};
		super.onResume();
	}

	IconIdListener getIconListener() {
		if (iconIdListener == null) {
			iconIdListener = new IconIdListener() {
				@Override
				public void selectedIconId(int iconRes) {
					isDataChanged = true;
					profile.iconId = iconRes;
					profileIcon.setImageDrawable(app.getUIUtilities().getIcon(iconRes,
						isNightMode ? R.color.active_buttons_and_links_dark
							: R.color.active_buttons_and_links_light));
					profile.iconId = iconRes;
				}
			};
		}
		return iconIdListener;
	}

	SelectProfileListener getBaseProfileListener() {
		if (baseTypeListener == null) {
			baseTypeListener = new SelectProfileListener() {
				@Override
				public void onSelectedType(int pos) {
					String key = SettingsProfileFragment.getBaseProfiles(getMyApplication())
						.get(pos).getStringKey();
					setupBaseProfileView(key);
					profile.parent = ApplicationMode.valueOfStringKey(key, ApplicationMode.DEFAULT);
				}
			};
		}
		return baseTypeListener;
	}

	SelectProfileListener getNavProfileListener() {
		if (navTypeListener == null) {
			navTypeListener = new SelectProfileListener() {
				@Override
				public void onSelectedType(int pos) {
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
		isDataChanged = true;
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
			ApplicationMode.changeProfileStatus(mode, true, getMyApplication());
		}
	}

	private void setupBaseProfileView(String stringKey) {
		for (ApplicationMode am : ApplicationMode.getDefaultValues()) {
			if (am.getStringKey().equals(stringKey)) {
				baseModeIcon.setImageDrawable(
					app.getUIUtilities().getIcon(am.getSmallIconDark(), R.color.icon_color));
				baseModeTitle.setText(Algorithms.capitalizeFirstLetter(am.toHumanString(app)));
				isDataChanged = false;
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
			|| profile.userProfileTitle.replace(" ", "").length() < 1) {
			showSaveWarningDialog(
				getString(R.string.profile_alert_need_profile_name_title),
				getString(R.string.profile_alert_need_profile_name_msg),
				getActivity()
			);
			return false;
		}

		for (ApplicationMode m : ApplicationMode.allPossibleValues()) {
			if (m.getUserProfileName() != null && getActivity() != null) {
				if (m.getUserProfileName().equals(profile.userProfileTitle)) {
					if (isNew || !Algorithms.isEmpty(mode.getUserProfileName())
						&& !mode.getUserProfileName().equals(profile.userProfileTitle)) {
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
			}
		}

		if (isUserProfile && !isNew) {
			ApplicationMode.deleteCustomMode(mode.getUserProfileName(), getMyApplication());
		}

		String customStringKey = profile.stringKey;
		if (isNew && profile.parent != null) {
			customStringKey =
				profile.parent.getStringKey() + "_" + System.currentTimeMillis();
		}

		ApplicationMode.ApplicationModeBuilder builder = ApplicationMode
			.createCustomMode(profile.userProfileTitle.trim(), customStringKey)
			.parent(profile.parent)
			.icon(profile.iconId, profile.iconId);

		if (profile.routingProfileDataObject != null) {
			builder.setRoutingProfile(profile.routingProfileDataObject.getStringKey());
		}

		ApplicationMode mode = builder.customReg();
		ApplicationMode.saveCustomModeToSettings(getSettings());

		if (!ApplicationMode.values(app).contains(mode)) {
			boolean save = ApplicationMode.changeProfileStatus(mode, true, getMyApplication());

			if (save && getSettings() != null) {
				if (profile.routingProfileDataObject.getStringKey()
					.equals(RoutingProfilesResources.STRAIGHT_LINE_MODE.toString())) {
					getSettings().ROUTER_SERVICE.setModeValue(mode, RouteService.STRAIGHT);
				} else if (profile.routingProfileDataObject.getStringKey()
					.equals(RoutingProfilesResources.BROUTER_MODE.toString())) {
					getSettings().ROUTER_SERVICE.setModeValue(mode, RouteService.BROUTER);
				} else {
					getSettings().ROUTER_SERVICE.setModeValue(mode, RouteService.OSMAND);
				}
			}

		}
		isDataChanged = false;
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
							ApplicationMode
								.deleteCustomMode(profile.userProfileTitle, getMyApplication());
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

	static List<RoutingProfileDataObject> getRoutingProfiles(OsmandApplication context) {
		List<RoutingProfileDataObject> profilesObjects = new ArrayList<>();
		profilesObjects.add(new RoutingProfileDataObject(
			RoutingProfilesResources.STRAIGHT_LINE_MODE.toString(),
			context.getString(RoutingProfilesResources.STRAIGHT_LINE_MODE.getStringRes()),
			context.getString(R.string.special_routing_type),
			RoutingProfilesResources.STRAIGHT_LINE_MODE.getIconRes(),
			false, null));
		if (context.getBRouterService() != null) {
			profilesObjects.add(new RoutingProfileDataObject(
				RoutingProfilesResources.BROUTER_MODE.toString(),
				context.getString(RoutingProfilesResources.BROUTER_MODE.getStringRes()),
				context.getString(R.string.third_party_routing_type),
				RoutingProfilesResources.BROUTER_MODE.getIconRes(),
				false, null));
		}

		Map<String, GeneralRouter> inputProfiles = context.getRoutingConfig().getAllRoutes();
		for (Entry<String, GeneralRouter> e : inputProfiles.entrySet()) {
			int iconRes = R.drawable.ic_action_world_globe;
			String name = e.getValue().getProfileName();
			String description;
			if (e.getValue().getFilename() == null) {
				iconRes = RoutingProfilesResources.valueOf(name.toUpperCase()).getIconRes();
				name = context
					.getString(RoutingProfilesResources.valueOf(name.toUpperCase()).getStringRes());
				description = context.getString(R.string.osmand_default_routing);
			} else {
				description = context.getString(R.string.custom_routing);
			}
			profilesObjects
				.add(new RoutingProfileDataObject(e.getKey(), name, description, iconRes, false,
					e.getValue().getFilename()));
		}
		return profilesObjects;
	}

	public enum RoutingProfilesResources {
		STRAIGHT_LINE_MODE(R.string.routing_profile_straightline,R.drawable.ic_action_split_interval),
		BROUTER_MODE(R.string.routing_profile_broutrer, R.drawable.ic_action_split_interval),
		CAR(R.string.rendering_value_car_name, R.drawable.ic_action_car_dark),
		PEDESTRIAN(R.string.rendering_value_pedestrian_name, R.drawable.map_action_pedestrian_dark),
		BICYCLE(R.string.rendering_value_bicycle_name, R.drawable.map_action_bicycle_dark),
		PUBLIC_TRANSPORT(R.string.app_mode_public_transport, R.drawable.map_action_bus_dark),
		BOAT(R.string.app_mode_boat, R.drawable.map_action_sail_boat_dark),
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
	}

	private class ApplicationProfileObject {

		int key = -1;
		String stringKey;
		String userProfileTitle = "";
		ApplicationMode parent = null;
		int iconId = R.drawable.map_world_globe_dark;
		RoutingProfileDataObject routingProfileDataObject = null;

		ApplicationProfileObject(ApplicationMode mode, boolean isNew, boolean isUserProfile) {
			if (isNew) {
				stringKey = mode.getStringKey() + System.currentTimeMillis();
				parent = mode;
			} else if (isUserProfile) {
				stringKey = mode.getStringKey();
				parent = mode.getParent();
				iconId = mode.getSmallIconDark();
				userProfileTitle = mode.getUserProfileName();
			} else {
				key = mode.getStringResource();
				stringKey = mode.getStringKey();
				iconId = mode.getSmallIconDark();
			}
		}
	}
}
