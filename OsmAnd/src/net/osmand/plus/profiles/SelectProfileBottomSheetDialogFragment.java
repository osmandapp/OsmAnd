package net.osmand.plus.profiles;

import static net.osmand.plus.profiles.EditProfileFragment.SELECTED_ICON;

import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import java.util.ArrayList;
import java.util.List;
import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithCompoundButton;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.LongDescriptionItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import org.apache.commons.logging.Log;

public class SelectProfileBottomSheetDialogFragment extends MenuBottomSheetDialogFragment {

	private static final Log LOG = PlatformUtil
		.getLog(SelectProfileBottomSheetDialogFragment.class);
	public static final String TAG = "SelectProfileBottomSheetDialogFragment";

	public final static String DIALOG_TYPE = "dialog_type";
	public final static String TYPE_APP_PROFILE = "base_profiles";
	public final static String TYPE_NAV_PROFILE = "routing_profiles";
	public final static String TYPE_ICON = "icon_type";
	public final static String SELECTED_KEY = "selected_base";

	String type;

	private SelectProfileListener listener;

	private List<ProfileDataObject> profiles = new ArrayList<>();
	private List<IconResWithDescr> icons;
	private String selectedItemKey;
	private int selectedIconRes;

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
			} else if (type.equals(TYPE_APP_PROFILE)) {
				profiles.addAll(SettingsProfileFragment.getBaseProfiles(app));
			} else if (type.equals(TYPE_ICON)) {
				selectedIconRes = args.getInt(SELECTED_ICON, -1);
				icons = getProfileIcons();
			} else {
				LOG.error("Check intent data!");
				dismiss();
			}
		}
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {

		if (type.equals(TYPE_APP_PROFILE)) {
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
							? R.color.active_buttons_and_links_dark
							: R.color.active_buttons_and_links_light);
				} else {
					drawableIcon = getMyApplication().getUIUtilities()
						.getIcon(profile.getIconRes(), R.color.icon_color);
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
								listener.onSelectedType(pos);
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
							? R.color.active_buttons_and_links_dark
							: R.color.active_buttons_and_links_light);
				} else {
					drawableIcon = getMyApplication().getUIUtilities()
						.getIcon(profile.getIconRes(), R.color.icon_color);
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
								listener.onSelectedType(pos);
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
				boolean isSelected = icon.resId == selectedIconRes;
				if (isSelected) {
					drawableIcon = getMyApplication().getUIUtilities()
						.getIcon(icon.resId, nightMode
							? R.color.active_buttons_and_links_dark
							: R.color.active_buttons_and_links_light);
				} else {
					drawableIcon = getMyApplication().getUIUtilities()
						.getIcon(icon.resId, R.color.icon_color);
				}

				items.add(new BottomSheetItemWithCompoundButton.Builder()
					.setChecked(icon.resId == selectedIconRes)
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
								listener.onSelectedType(icon.resId);
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
		if (getActivity() != null && getActivity() instanceof  EditProfileActivity) {
			EditProfileFragment f = (EditProfileFragment) getActivity().getSupportFragmentManager()
				.findFragmentByTag(EditProfileActivity.EDIT_PROFILE_FRAGMENT_TAG);
			if (type.equals(TYPE_APP_PROFILE)) {
				listener = f.getBaseProfileListener();
			} else if (type.equals(TYPE_NAV_PROFILE)) {
				listener = f.getNavProfileListener();
			} else if (type.equals(TYPE_ICON)) {
				listener = f.getIconListener();
			}
		} else if (getActivity() != null && getActivity() instanceof SettingsProfileActivity) {
			SettingsProfileFragment f = (SettingsProfileFragment) getActivity().getSupportFragmentManager()
				.findFragmentByTag(SettingsProfileActivity.SETTINGS_PROFILE_FRAGMENT_TAG);
			listener = f.getBaseProfileListener();
		}
	}

	private List<IconResWithDescr> getProfileIcons() {
		List<IconResWithDescr> icons = new ArrayList<>();
		icons.add(new IconResWithDescr(R.drawable.ic_action_car_dark, R.string.rendering_value_car_name,false));
		icons.add(new IconResWithDescr(R.drawable.ic_action_bicycle_dark, R.string.rendering_value_bicycle_name,false));
		icons.add(new IconResWithDescr(R.drawable.ic_action_pedestrian_dark, R.string.rendering_value_pedestrian_name,false));
		icons.add(new IconResWithDescr(R.drawable.ic_action_bus_dark, R.string.app_mode_bus,false));
		icons.add(new IconResWithDescr(R.drawable.ic_action_sail_boat_dark, R.string.app_mode_boat,false));
		icons.add(new IconResWithDescr(R.drawable.ic_action_aircraft, R.string.app_mode_aircraft,false));
		icons.add(new IconResWithDescr(R.drawable.ic_action_truck_dark, R.string.app_mode_truck,false));
		icons.add(new IconResWithDescr(R.drawable.ic_action_motorcycle_dark, R.string.app_mode_motorcycle,false));
		icons.add(new IconResWithDescr(R.drawable.ic_action_trekking_dark, R.string.app_mode_hiking,false));
		return icons;
	}

	interface SelectProfileListener {
		void onSelectedType(int pos);
	}

	private class IconResWithDescr {
		private int resId;
		private int titleId;
		private boolean isSelected;

		public IconResWithDescr(int resId, int titleId, boolean isSelected) {
			this.resId = resId;
			this.titleId = titleId;
			this.isSelected = isSelected;
		}
	}

}
