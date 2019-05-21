package net.osmand.plus.profiles;

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
	public final static String SELECTED_KEY = "selected_base";

	String type;

	private SelectProfileListener listener;

	private List<ProfileDataObject> profiles = new ArrayList<>();
	private String selectedItemKey;

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
					.setDescription(profile.getDescription())
					.setTitle(profile.getName())
					.setIcon(drawableIcon)
					.setLayoutId(R.layout.bottom_sheet_item_with_descr_and_checkbox_56dp)
					.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View v) {
							if (listener == null) {
								getProfileListener();
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
					.setDescription(profile.getDescription())
					.setTitle(profile.getName())
					.setIcon(drawableIcon)
					.setLayoutId(R.layout.bottom_sheet_item_with_descr_and_checkbox_56dp)
					.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View v) {
							if (listener == null) {
								getProfileListener();
							}
							if (listener != null) {
								listener.onSelectedType(pos);
							}

							dismiss();
						}
					})
					.create());
			}
		}
	}

	private void getProfileListener() {
		if (getActivity() != null && getActivity() instanceof  EditProfileActivity) {
			EditProfileFragment f = (EditProfileFragment) getActivity().getSupportFragmentManager()
				.findFragmentByTag(EditProfileActivity.EDIT_PROFILE_FRAGMENT_TAG);
			if (type.equals(TYPE_APP_PROFILE)) {
				listener = f.getBaseProfileListener();
			} else if (type.equals(TYPE_NAV_PROFILE)) {
				listener = f.getNavProfileListener();
			}
		} else if (getActivity() != null && getActivity() instanceof SettingsProfileActivity) {
			SettingsProfileFragment f = (SettingsProfileFragment) getActivity().getSupportFragmentManager()
				.findFragmentByTag(SettingsProfileActivity.SETTINGS_PROFILE_FRAGMENT_TAG);
			listener = f.getBaseProfileListener();

		}
	}

	interface SelectProfileListener {
		void onSelectedType(int pos);
	}
}
