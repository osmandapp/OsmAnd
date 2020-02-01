package net.osmand.plus.profiles;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import net.osmand.CallbackWithObject;
import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.SettingsHelper.*;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithCompoundButton;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.DividerItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.LongDescriptionItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.settings.MainSettingsFragment;
import net.osmand.plus.settings.NavigationFragment;
import net.osmand.plus.settings.ProfileAppearanceFragment;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.List;

import static net.osmand.plus.helpers.ImportHelper.ImportType.ROUTING;
import static net.osmand.plus.helpers.ImportHelper.ImportType.SETTINGS;

public class SelectProfileBottomSheetDialogFragment extends MenuBottomSheetDialogFragment {

	private static final Log LOG = PlatformUtil
		.getLog(SelectProfileBottomSheetDialogFragment.class);
	public static final String TAG = "SelectProfileBottomSheetDialogFragment";

	public final static String DIALOG_TYPE = "dialog_type";
	public final static String TYPE_BASE_APP_PROFILE = "base_profiles";
	public final static String TYPE_NAV_PROFILE = "routing_profiles";
	public final static String SELECTED_KEY = "selected_base";

	public final static String PROFILE_KEY_ARG = "profile_key_arg";
	public final static String IS_PROFILE_IMPORTED_ARG = "is_profile_imported_arg";

	String type;

	private SelectProfileListener listener;

	private final List<ProfileDataObject> profiles = new ArrayList<>();

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
				profiles.addAll(NavigationFragment.getRoutingProfiles(app).values());
			} else if (type.equals(TYPE_BASE_APP_PROFILE)) {
				profiles.addAll(NavigationFragment.getBaseProfiles(app));
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
			for (BaseBottomSheetItem item : items) {
				View bottomDivider = item.getView().findViewById(R.id.divider_bottom);
				if (bottomDivider != null) {
					bottomDivider.setVisibility(View.INVISIBLE);
				}
			}
		}
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		int activeColorRes = nightMode ? R.color.active_color_primary_dark : R.color.active_color_primary_light;
		int iconDefaultColorResId = nightMode ? R.color.icon_color_default_dark : R.color.icon_color_default_light;
		OsmandApplication app = getMyApplication();
		
		View bottomSpaceView = new View(app);
		int space = (int) getResources().getDimension(R.dimen.empty_state_text_button_padding_top);
		bottomSpaceView.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, space));
		
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
							Bundle args = new Bundle();
							args.putString(PROFILE_KEY_ARG, profile.getStringKey());
							listener.onSelectedType(args);
							dismiss();
						}
					})
					.create());
			}
			items.add(new DividerItem(app));
			items.add(new SimpleBottomSheetItem.Builder()
					.setTitle(app.getString(R.string.import_from_file))
					.setIcon(app.getUIUtilities().getIcon(R.drawable.ic_action_folder, iconDefaultColorResId))
					.setLayoutId(R.layout.bottom_sheet_item_simple)
					.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View v) {
							MapActivity mapActivity = getMapActivity();
							if (mapActivity == null) {
								return;
							}
							mapActivity.getImportHelper().chooseFileToImport(SETTINGS, new CallbackWithObject<List<SettingsItem>>() {
								@Override
								public boolean processResult(List<SettingsItem> result) {
									for (SettingsItem item : result) {
										if (SettingsItemType.PROFILE.equals(item.getType())) {
											if (listener == null) {
												getListener();
											}
											Bundle args = new Bundle();
											args.putString(PROFILE_KEY_ARG, item.getName());
											args.putBoolean(IS_PROFILE_IMPORTED_ARG, true);
											listener.onSelectedType(args);
											dismiss();
											break;
										}
									}
									return false;
								}
							});
						}
					})
					.create());
			items.add(new BaseBottomSheetItem.Builder()
					.setCustomView(bottomSpaceView)
					.create());

		} else if (type.equals(TYPE_NAV_PROFILE)){
			items.add(new TitleItem(getString(R.string.select_nav_profile_dialog_title)));
			items.add(new LongDescriptionItem(getString(R.string.select_nav_profile_dialog_message)));
			for (int i = 0; i < profiles.size(); i++) {
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
							Bundle args = new Bundle();
							args.putString(PROFILE_KEY_ARG, profile.getStringKey());
							listener.onSelectedType(args);
							dismiss();
						}
					})
					.create());
			}
			items.add(new DividerItem(app));
			items.add(new LongDescriptionItem(app.getString(R.string.osmand_routing_promo)));
			items.add(new SimpleBottomSheetItem.Builder()
					.setTitle(app.getString(R.string.import_routing_file))
					.setIcon(app.getUIUtilities().getIcon(R.drawable.ic_action_folder, iconDefaultColorResId))
					.setLayoutId(R.layout.bottom_sheet_item_simple)
					.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View v) {
							MapActivity mapActivity = getMapActivity();
							if (mapActivity == null) {
								return;
							}
							mapActivity.getImportHelper().chooseFileToImport(ROUTING, new CallbackWithObject<String>() {
								@Override
								public boolean processResult(String profileKey) {
									if (listener == null) {
										getListener();
									}
									Bundle args = new Bundle();
									args.putString(PROFILE_KEY_ARG, profileKey);
									args.putBoolean(IS_PROFILE_IMPORTED_ARG, true);
									listener.onSelectedType(args);
									dismiss();
									return false;
								}
							});
						}
					})
					.create());
			items.add(new BaseBottomSheetItem.Builder()
					.setCustomView(bottomSpaceView)
					.create());
		}
	}

	private void getListener() {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			FragmentManager fragmentManager = activity.getSupportFragmentManager();
			NavigationFragment navigationFragment = (NavigationFragment) fragmentManager.findFragmentByTag(NavigationFragment.class.getName());
			ProfileAppearanceFragment profileAppearanceFragment = (ProfileAppearanceFragment) fragmentManager.findFragmentByTag(ProfileAppearanceFragment.TAG);
			MainSettingsFragment mainSettingsFragment = (MainSettingsFragment) fragmentManager.findFragmentByTag(MainSettingsFragment.TAG);

			if (navigationFragment != null) {
				listener = navigationFragment.getNavProfileListener();
			} else if (profileAppearanceFragment != null) {
				listener = profileAppearanceFragment.getParentProfileListener();
			} else if (mainSettingsFragment != null) {
				listener = mainSettingsFragment.getParentProfileListener();
			}
		}
	}

	public interface SelectProfileListener {
		void onSelectedType(Bundle args);
	}

	@Nullable
	private MapActivity getMapActivity() {
		Activity activity = getActivity();
		if (activity instanceof MapActivity) {
			return (MapActivity) activity;
		}
		return null;
	}
}
