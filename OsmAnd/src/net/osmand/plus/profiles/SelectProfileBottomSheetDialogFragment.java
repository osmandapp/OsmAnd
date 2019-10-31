package net.osmand.plus.profiles;

import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.view.View;
import android.view.View.OnClickListener;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithCompoundButton;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.LongDescriptionItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.List;

import static net.osmand.plus.profiles.EditProfileFragment.SELECTED_ICON;

public class SelectProfileBottomSheetDialogFragment extends MenuBottomSheetDialogFragment {

	private static final Log LOG = PlatformUtil
		.getLog(SelectProfileBottomSheetDialogFragment.class);
	public static final String TAG = "SelectProfileBottomSheetDialogFragment";

	public final static String DIALOG_TYPE = "dialog_type";
	public final static String TYPE_BASE_APP_PROFILE = "base_profiles";
	public final static String TYPE_NAV_PROFILE = "routing_profiles";
	public final static String TYPE_ICON = "icon_type";
	public final static String SELECTED_KEY = "selected_base";

	String type;
	int bottomButtonText = R.string.shared_string_cancel;

	private SelectProfileListener listener;

	private final List<ProfileDataObject> profiles = new ArrayList<>();

	private List<IconResWithDescr> icons;
	private String selectedItemKey;
	private String selectedIconRes;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		OsmandApplication app = getMyApplication();
		Bundle args = getArguments();
		if (args != null && args.get(DIALOG_TYPE) != null) {
			type = args.getString(DIALOG_TYPE);
			selectedItemKey = args.getString(SELECTED_KEY, null);
			if (type.equals(TYPE_NAV_PROFILE)) {
				profiles.addAll(EditProfileFragment.getRoutingProfiles(app));
			} else if (type.equals(TYPE_BASE_APP_PROFILE)) {
				profiles.addAll(SettingsProfileFragment.getBaseProfiles(app));
			} else if (type.equals(TYPE_ICON)) {
				selectedIconRes = args.getString(SELECTED_ICON, "");
				icons = getProfileIcons();
			} else {
				LOG.error("Check intent data!");
				dismiss();
			}
		}
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		view.findViewById(R.id.dismiss_button).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				onDismissButtonClickAction();
				dismiss();
			}
		});

		if (type.equals(TYPE_NAV_PROFILE) || type.equals(TYPE_BASE_APP_PROFILE)) {
			if (items.get(items.size()-1).getView() != null) {
				items.get(items.size()-1).getView().findViewById(R.id.divider_bottom).setVisibility(View.INVISIBLE);
			}
		}
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {

		if (type.equals(TYPE_BASE_APP_PROFILE)) {
			items.add(new TitleItem(getString(R.string.select_base_profile_dialog_title)));
			items.add(new LongDescriptionItem(getString(R.string.select_base_profile_dialog_message)));
			for (int i = 0; i < profiles.size(); i++) {
				final int pos = i;
				final ProfileDataObject profile = profiles.get(i);
				final boolean isSelected = profile.getStringKey().equals(selectedItemKey);
				final Drawable drawableIcon;
				if (isSelected) {
					drawableIcon = getMyApplication().getUIUtilities()
						.getIcon(profile.getIconRes(), nightMode
							? R.color.active_color_primary_dark
							: R.color.active_color_primary_light);
				} else {
					drawableIcon = getMyApplication().getUIUtilities()
						.getIcon(profile.getIconRes(), R.color.icon_color_default_light);
				}

				items.add(new BottomSheetItemWithCompoundButton.Builder()
					.setChecked(isSelected)
					.setButtonTintList(isSelected
						? ColorStateList.valueOf(getResolvedColor(getActiveColorId()))
						: null)
					.setDescription(profile.getDescription())
					.setTitle(profile.getName())
					.setIcon(drawableIcon)
					.setLayoutId(R.layout.bottom_sheet_item_with_descr_and_radio_btn)
					.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View v) {
							if (listener == null) {
								getListener();
							}
							if (listener != null) {
								listener.onSelectedType(pos, "");
							}
							dismiss();
						}
					})
					.create());
			}

		} else if (type.equals(TYPE_NAV_PROFILE)){
			items.add(new TitleItem(getString(R.string.select_nav_profile_dialog_title)));
			for (int i = 0; i < profiles.size(); i++) {
				final int pos = i;
				final ProfileDataObject profile = profiles.get(i);
				final boolean isSelected = profile.getStringKey().equals(selectedItemKey);
				final Drawable drawableIcon;
				if (isSelected) {
					drawableIcon = getMyApplication().getUIUtilities()
						.getIcon(profile.getIconRes(), nightMode
							? R.color.active_color_primary_dark
							: R.color.active_color_primary_light);
				} else {
					drawableIcon = getMyApplication().getUIUtilities()
						.getIcon(profile.getIconRes(), R.color.icon_color_default_light);
				}

				items.add(new BottomSheetItemWithCompoundButton.Builder()
					.setChecked(isSelected)
					.setButtonTintList(isSelected
						? ColorStateList.valueOf(getResolvedColor(getActiveColorId()))
						: null)
					.setDescription(profile.getDescription())
					.setTitle(profile.getName())
					.setIcon(drawableIcon)
					.setLayoutId(R.layout.bottom_sheet_item_with_descr_and_radio_btn)
					.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View v) {
							if (listener == null) {
								getListener();
							}
							if (listener != null) {
								listener.onSelectedType(pos, "");
							}
							dismiss();
						}
					})
					.create());
			}
		} else if (type.equals(TYPE_ICON)) {
			items.add(new TitleItem(getString(R.string.select_icon_profile_dialog_title)));
			for (final IconResWithDescr icon : icons) {
				Drawable drawableIcon;
				boolean isSelected = icon.resStringId.equals(selectedIconRes);
				int iconRes = icon.resId;
				if (isSelected) {
					drawableIcon = getMyApplication().getUIUtilities()
						.getIcon(iconRes, nightMode
							? R.color.active_color_primary_dark
							: R.color.active_color_primary_light);
				} else {
					drawableIcon = getMyApplication().getUIUtilities()
						.getIcon(iconRes, R.color.icon_color_default_light);
				}

				items.add(new BottomSheetItemWithCompoundButton.Builder()
					.setChecked(icon.resStringId.equals(selectedIconRes))
					.setButtonTintList(isSelected
						? ColorStateList.valueOf(getResolvedColor(getActiveColorId()))
						: null)
					.setTitle(getMyApplication().getString(icon.titleId))
					.setIcon(drawableIcon)
					.setLayoutId(R.layout.bottom_sheet_item_with_radio_btn)
					.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View v) {
							if(listener == null) {
								getListener();
							}
							if (listener != null) {
								listener.onSelectedType(icon.resId, icon.resStringId);
							}
							dismiss();
						}
					})
					.create()
				);
			}
		}
	}


	private void getListener() {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			FragmentManager fragmentManager = activity.getSupportFragmentManager();
			EditProfileFragment editProfileFragment = (EditProfileFragment) fragmentManager.findFragmentByTag(EditProfileFragment.TAG);
			SettingsProfileFragment settingsProfileFragment = (SettingsProfileFragment) fragmentManager.findFragmentByTag(SettingsProfileFragment.TAG);

			if (editProfileFragment != null) {
				switch (type) {
					case TYPE_BASE_APP_PROFILE:
						listener = editProfileFragment.getBaseProfileListener();
						break;
					case TYPE_NAV_PROFILE:
						listener = editProfileFragment.getNavProfileListener();
						break;
					case TYPE_ICON:
						listener = editProfileFragment.getIconListener();
						break;
				}
			} else if (settingsProfileFragment != null) {
				listener = settingsProfileFragment.getBaseProfileListener();
			}
		}
	}

	private List<IconResWithDescr> getProfileIcons() {
		List<IconResWithDescr> icons = new ArrayList<>();
		icons.add(new IconResWithDescr(R.drawable.ic_action_car_dark, R.string.app_mode_car, "ic_action_car_dark", false));
		icons.add(new IconResWithDescr(R.drawable.ic_action_taxi, R.string.app_mode_taxi, "ic_action_taxi", false));
		icons.add(new IconResWithDescr(R.drawable.ic_action_truck_dark, R.string.app_mode_truck, "ic_action_truck_dark", false));
		icons.add(new IconResWithDescr(R.drawable.ic_action_shuttle_bus, R.string.app_mode_shuttle_bus, "ic_action_shuttle_bus", false));
		icons.add(new IconResWithDescr(R.drawable.ic_action_bus_dark, R.string.app_mode_bus, "ic_action_bus_dark",false));
		icons.add(new IconResWithDescr(R.drawable.ic_action_subway, R.string.app_mode_subway, "ic_action_subway", false));
		icons.add(new IconResWithDescr(R.drawable.ic_action_motorcycle_dark, R.string.app_mode_motorcycle, "ic_action_motorcycle_dark", false));
		icons.add(new IconResWithDescr(R.drawable.ic_action_bicycle_dark, R.string.app_mode_bicycle, "ic_action_bicycle_dark", false));
		icons.add(new IconResWithDescr(R.drawable.ic_action_horse, R.string.app_mode_horse, "ic_action_horse", false));
		icons.add(new IconResWithDescr(R.drawable.ic_action_pedestrian_dark, R.string.app_mode_pedestrian,"ic_action_pedestrian_dark", false));
		icons.add(new IconResWithDescr(R.drawable.ic_action_trekking_dark, R.string.app_mode_hiking, "ic_action_trekking_dark", false));
		icons.add(new IconResWithDescr(R.drawable.ic_action_skiing, R.string.app_mode_skiing, "ic_action_skiing", false));
		icons.add(new IconResWithDescr(R.drawable.ic_action_sail_boat_dark, R.string.app_mode_boat, "ic_action_sail_boat_dark", false));
		icons.add(new IconResWithDescr(R.drawable.ic_action_aircraft, R.string.app_mode_aircraft, "ic_action_aircraft", false));
		icons.add(new IconResWithDescr(R.drawable.ic_action_helicopter, R.string.app_mode_helicopter, "ic_action_helicopter", false));
		icons.add(new IconResWithDescr(R.drawable.ic_action_personal_transporter, R.string.app_mode_personal_transporter, "ic_action_personal_transporter", false));
		icons.add(new IconResWithDescr(R.drawable.ic_action_monowheel, R.string.app_mode_monowheel, "ic_action_monowheel", false));
		icons.add(new IconResWithDescr(R.drawable.ic_action_scooter, R.string.app_mode_scooter, "ic_action_scooter", false));
		icons.add(new IconResWithDescr(R.drawable.ic_action_ufo, R.string.app_mode_ufo, "ic_action_ufo", false));
		icons.add(new IconResWithDescr(R.drawable.ic_action_offroad, R.string.app_mode_offroad, "ic_action_offroad", false));
		icons.add(new IconResWithDescr(R.drawable.ic_action_campervan, R.string.app_mode_campervan, "ic_action_campervan", false));
		icons.add(new IconResWithDescr(R.drawable.ic_action_camper, R.string.app_mode_camper, "ic_action_camper", false));
		icons.add(new IconResWithDescr(R.drawable.ic_action_pickup_truck, R.string.app_mode_pickup_truck, "ic_action_pickup_truck", false));
		icons.add(new IconResWithDescr(R.drawable.ic_action_wagon, R.string.app_mode_wagon, "ic_action_wagon", false));
		icons.add(new IconResWithDescr(R.drawable.ic_action_utv, R.string.app_mode_utv, "ic_action_utv", false));
		return icons;
	}

	interface SelectProfileListener {
		void onSelectedType(int pos, String stringRes);
	}

	private class IconResWithDescr {
		private int resId;
		private int titleId;
		private String resStringId;
		private boolean isSelected;

		public IconResWithDescr(int resId, int titleId, String resStringId, boolean isSelected) {
			this.resId = resId;
			this.titleId = titleId;
			this.isSelected = isSelected;
			this.resStringId = resStringId;
		}
	}

}
