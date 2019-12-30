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
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithCompoundButton;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.LongDescriptionItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.settings.MainSettingsFragment;
import net.osmand.plus.settings.NavigationFragment;
import net.osmand.plus.settings.ProfileAppearanceFragment;

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
		int activeColorRes = nightMode ? R.color.active_color_primary_dark : R.color.active_color_primary_light;
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
						.getIcon(profile.getIconRes(), activeColorRes);
				} else {
					drawableIcon = getMyApplication().getUIUtilities()
						.getIcon(profile.getIconRes(), R.color.icon_color_default_light);
				}

				items.add(new BottomSheetItemWithCompoundButton.Builder()
					.setCompoundButtonColorId(activeColorRes)
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
							listener.onSelectedType(pos, "");
							dismiss();
						}
					})
					.create());
			}

		} else if (type.equals(TYPE_NAV_PROFILE)){
			items.add(new TitleItem(getString(R.string.select_nav_profile_dialog_title)));
			items.add(new LongDescriptionItem(getString(R.string.select_nav_profile_dialog_message)));
			for (int i = 0; i < profiles.size(); i++) {
				final int pos = i;
				final ProfileDataObject profile = profiles.get(i);
				final boolean isSelected = profile.getStringKey().equals(selectedItemKey);
				final Drawable drawableIcon;
				if (isSelected) {
					drawableIcon = getMyApplication().getUIUtilities()
						.getIcon(profile.getIconRes(), activeColorRes);
				} else {
					drawableIcon = getMyApplication().getUIUtilities()
						.getIcon(profile.getIconRes(), R.color.icon_color_default_light);
				}

				items.add(new BottomSheetItemWithCompoundButton.Builder()
					.setCompoundButtonColorId(activeColorRes)
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
							listener.onSelectedType(pos, "");
							dismiss();
						}
					})
					.create());
			}
		} else if (type.equals(TYPE_ICON)) {
			items.add(new TitleItem(getString(R.string.select_icon_profile_dialog_title)));
			for (final ApplicationMode.ProfileIcons icon : ApplicationMode.ProfileIcons.values()) {
				Drawable drawableIcon;
				boolean isSelected = icon.getResStringId().equals(selectedIconRes);
				int iconRes = icon.getResId();
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
						.setCompoundButtonColorId(activeColorRes)
						.setChecked(icon.getResStringId().equals(selectedIconRes))
					.setButtonTintList(isSelected
						? ColorStateList.valueOf(getResolvedColor(getActiveColorId()))
						: null)
						.setTitle(getMyApplication().getString(icon.getTitleId()))
					.setIcon(drawableIcon)
					.setLayoutId(R.layout.bottom_sheet_item_with_radio_btn)
					.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View v) {
							if(listener == null) {
								getListener();
							}
							listener.onSelectedType(icon.getResId(), icon.getResStringId());
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
			SettingsProfileFragment settingsProfileFragment = (SettingsProfileFragment) fragmentManager.findFragmentByTag(SettingsProfileFragment.class.getName());
			NavigationFragment navigationFragment = (NavigationFragment) fragmentManager.findFragmentByTag(NavigationFragment.class.getName());
			ProfileAppearanceFragment profileAppearanceFragment = (ProfileAppearanceFragment) fragmentManager.findFragmentByTag(ProfileAppearanceFragment.TAG);
			MainSettingsFragment mainSettingsFragment = (MainSettingsFragment) fragmentManager.findFragmentByTag(MainSettingsFragment.TAG);

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
			} else if (navigationFragment != null) {
				listener = navigationFragment.getNavProfileListener();
			} else if (profileAppearanceFragment != null) {
				listener = profileAppearanceFragment.getParentProfileListener();
			} else if (mainSettingsFragment != null) {
				listener = mainSettingsFragment.getParentProfileListener();
			}
		}
	}

	public interface SelectProfileListener {
		void onSelectedType(int pos, String stringRes);
	}
}
