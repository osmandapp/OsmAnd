package net.osmand.plus.profiles;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.AndroidUtils;
import net.osmand.CallbackWithObject;
import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.DividerItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.LongDescriptionItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.settings.MainSettingsFragment;
import net.osmand.plus.settings.NavigationFragment;
import net.osmand.plus.settings.ProfileAppearanceFragment;
import net.osmand.plus.settings.bottomsheets.BasePreferenceBottomSheet;
import net.osmand.router.RoutingConfiguration;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.List;

import static net.osmand.plus.helpers.ImportHelper.ImportType.ROUTING;

public class SelectProfileBottomSheetDialogFragment extends BasePreferenceBottomSheet {

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
			refreshProfiles(app);
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
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		OsmandApplication app = requiredMyApplication();
		
		View bottomSpaceView = new View(app);
		int space = (int) getResources().getDimension(R.dimen.empty_state_text_button_padding_top);
		bottomSpaceView.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, space));
		
		if (type.equals(TYPE_BASE_APP_PROFILE)) {
			items.add(new TitleItem(getString(R.string.select_base_profile_dialog_title)));
			items.add(new LongDescriptionItem(getString(R.string.select_base_profile_dialog_message)));
			for (int i = 0; i < profiles.size(); i++) {
				ProfileDataObject profile = profiles.get(i);
				addProfileItem(profile, false);
			}
			/*items.add(new DividerItem(app));
			addButtonItem(R.string.import_from_file, R.drawable.ic_action_folder, new OnClickListener() {
				
					@Override
					public void onClick(View v) {
						MapActivity mapActivity = getMapActivity();
						if (mapActivity == null) {
							return;
						}
						mapActivity.getImportHelper().chooseFileToImport(SETTINGS, false,
								new CallbackWithObject<List<SettingsItem>>() {
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
			});
			items.add(new BaseBottomSheetItem.Builder()
					.setCustomView(bottomSpaceView)
					.create());*/

		} else if (type.equals(TYPE_NAV_PROFILE)) {
			items.add(new TitleItem(getString(R.string.select_nav_profile_dialog_title)));
			items.add(new LongDescriptionItem(getString(R.string.select_nav_profile_dialog_message)));
			for (int i = 0; i < profiles.size(); i++) {
				final RoutingProfileDataObject profile = (RoutingProfileDataObject) profiles.get(i);
				boolean showBottomDivider = false;
				if (i < profiles.size() - 1) {
					RoutingProfileDataObject nextProfile = (RoutingProfileDataObject) profiles.get(i + 1);
					if (profile.getFileName() == null) { 
						showBottomDivider = nextProfile.getFileName() != null; 
					} else { 
						showBottomDivider = !profile.getFileName().equals(nextProfile.getFileName()); 
					} 
				}
				addProfileItem(profile, showBottomDivider);
			}
			items.add(new DividerItem(app));
			items.add(new LongDescriptionItem(app.getString(R.string.osmand_routing_promo)));
			addButtonItem(R.string.import_routing_file, R.drawable.ic_action_folder, new OnClickListener() {
				@Override
				public void onClick(View v) {
					MapActivity mapActivity = getMapActivity();
					if (mapActivity == null) {
						return;
					}
					mapActivity.getImportHelper().chooseFileToImport(ROUTING, new CallbackWithObject<RoutingConfiguration.Builder>() {
						@Override
						public boolean processResult(RoutingConfiguration.Builder builder) {
							refreshView();
							return false;
						}
					});
				}
			});
			items.add(new BaseBottomSheetItem.Builder()
					.setCustomView(bottomSpaceView)
					.create());
		}
	}
	
	private void addProfileItem(final ProfileDataObject profile, boolean showBottomDivider) {
		OsmandApplication app = requiredMyApplication();
		
		int activeColorResId = nightMode ? R.color.active_color_primary_dark : R.color.active_color_primary_light;
		int iconDefaultColorResId = nightMode ? R.color.icon_color_default_dark : R.color.icon_color_default_light;
		
		View itemView = UiUtilities.getInflater(getContext(), nightMode).inflate(R.layout.bottom_sheet_item_with_descr_and_radio_btn, null);
		TextView tvTitle = itemView.findViewById(R.id.title);
		TextView tvDescription = itemView.findViewById(R.id.description);
		ImageView ivIcon = itemView.findViewById(R.id.icon);
		CompoundButton compoundButton = itemView.findViewById(R.id.compound_button);
		View bottomDivider = itemView.findViewById(R.id.divider_bottom);

		tvTitle.setText(profile.getName());
		tvDescription.setText(profile.getDescription());
		
		boolean isSelected = profile.getStringKey().equals(selectedItemKey);
		int iconColor;
		if (type.equals(TYPE_BASE_APP_PROFILE)) {
			iconColor = profile.getIconColor(nightMode);
		} else {
			iconColor = isSelected ? activeColorResId : iconDefaultColorResId;
		}

		Drawable drawableIcon = app.getUIUtilities().getIcon(profile.getIconRes(), iconColor);
		ivIcon.setImageDrawable(drawableIcon);
		compoundButton.setChecked(isSelected);
		UiUtilities.setupCompoundButton(compoundButton, nightMode, UiUtilities.CompoundButtonType.GLOBAL);
		bottomDivider.setVisibility(showBottomDivider ? View.VISIBLE : View.INVISIBLE);

		items.add(new BaseBottomSheetItem.Builder()
				.setCustomView(itemView)
				.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View view) {
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
	
	private void addButtonItem(int titleId, int iconId, OnClickListener listener) {
		OsmandApplication app = requiredMyApplication();
		Context themedCtx = UiUtilities.getThemedContext(app, nightMode);
		
		int activeColorResId = AndroidUtils.resolveAttribute(themedCtx, R.attr.active_color_basic);
		
		View buttonView = View.inflate(themedCtx, R.layout.bottom_sheet_item_preference_btn, null);
		TextView tvTitle = buttonView.findViewById(R.id.title);
		tvTitle.setText(app.getString(titleId));
		
		ImageView ivIcon = buttonView.findViewById(R.id.icon);
		ivIcon.setImageDrawable(app.getUIUtilities().getIcon(iconId, activeColorResId));

		items.add(new BaseBottomSheetItem.Builder()
				.setCustomView(buttonView)
				.setOnClickListener(listener)
				.create());
	}

	private void refreshProfiles(OsmandApplication app) {
		profiles.clear();
		if (type.equals(TYPE_NAV_PROFILE)) {
			profiles.addAll(NavigationFragment.getSortedRoutingProfiles(app));
		} else if (type.equals(TYPE_BASE_APP_PROFILE)) {
			profiles.addAll(NavigationFragment.getBaseProfiles(app));
		} else {
			LOG.error("Check data type!");
			dismiss();
		}
	}

	private void refreshView() {
		Activity activity = getActivity();
		View mainView = getView();
		refreshProfiles(getMyApplication());
		if (activity != null && mainView != null) {
			LinearLayout itemsContainer = (LinearLayout) mainView.findViewById(useScrollableItemsContainer()
					? R.id.scrollable_items_container : R.id.non_scrollable_items_container);
			if (itemsContainer != null) {
				itemsContainer.removeAllViews();
			}
			items.clear();
			createMenuItems(null);
			for (BaseBottomSheetItem item : items) {
				item.inflate(activity, itemsContainer, nightMode);
			}
			setupHeightAndBackground(mainView);
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
