package net.osmand.plus.profiles;

import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
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
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

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
import net.osmand.plus.base.bottomsheetmenu.simpleitems.SimpleDividerItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.helpers.FontCache;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.bottomsheets.BasePreferenceBottomSheet;
import net.osmand.plus.onlinerouting.ui.OnlineRoutingEngineFragment;
import net.osmand.router.RoutingConfiguration;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.List;

import static net.osmand.plus.importfiles.ImportHelper.ImportType.ROUTING;

public class SelectProfileBottomSheet extends BasePreferenceBottomSheet {

	private static final Log LOG = PlatformUtil.getLog(SelectProfileBottomSheet.class);
	public static final String TAG = "SelectProfileBottomSheet";

	public final static String DIALOG_TYPE = "dialog_type";
	public final static String SELECTED_KEY = "selected_base";

	public final static String PROFILE_KEY_ARG = "profile_key_arg";
	public final static String USE_LAST_PROFILE_ARG = "use_last_profile_arg";
	public final static String PROFILES_LIST_UPDATED_ARG = "is_profiles_list_updated";

	private DialogMode dialogMode;
	private final List<ProfileDataObject> profiles = new ArrayList<>();
	private String selectedItemKey;

	public enum DialogMode {
		BASE_PROFILE,
		NAVIGATION_PROFILE,
		DEFAULT_PROFILE
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		OsmandApplication app = getMyApplication();
		Bundle args = getArguments();
		if (args != null && args.get(DIALOG_TYPE) != null) {
			String dialogModeName = args.getString(DIALOG_TYPE);
			dialogMode = DialogMode.valueOf(dialogModeName);
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

		if (dialogMode == DialogMode.BASE_PROFILE) {
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
												args.putBoolean(PROFILES_LIST_UPDATED_ARG, true);
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

		} else if (dialogMode == DialogMode.NAVIGATION_PROFILE) {
			items.add(new TitleItem(getString(R.string.select_nav_profile_dialog_title)));
			items.add(new LongDescriptionItem(getString(R.string.select_nav_profile_dialog_message)));
			for (int i = 0; i < profiles.size(); i++) {
				boolean showBottomDivider = false;
				if (profiles.get(i) instanceof RoutingProfileDataObject) {
					final RoutingProfileDataObject profile = (RoutingProfileDataObject) profiles.get(i);
					if (i < profiles.size() - 1) {
						if (profiles.get(i + 1) instanceof RoutingProfileDataObject) {
							RoutingProfileDataObject nextProfile = (RoutingProfileDataObject) profiles.get(i + 1);
							if (profile.getFileName() == null) {
								showBottomDivider = nextProfile.getFileName() != null;
							} else {
								showBottomDivider = !profile.getFileName().equals(nextProfile.getFileName());
							}
						} else {
							showBottomDivider = true;
						}
					}
				}
				addProfileItem(profiles.get(i), showBottomDivider);
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
			addButtonItem(R.string.add_online_routing_engine, R.drawable.ic_world_globe_dark, new OnClickListener() {
				@Override
				public void onClick(View v) {
					if (getActivity() != null) {
						OnlineRoutingEngineFragment.showInstance(getActivity(), getAppMode(), null);
					}
					dismiss();
				}
			});
			items.add(new BaseBottomSheetItem.Builder()
					.setCustomView(bottomSpaceView)
					.create());
		} else if (dialogMode == DialogMode.DEFAULT_PROFILE) {
			items.add(new TitleItem(getString(R.string.settings_preset)));
			items.add(new LongDescriptionItem(getString(R.string.profile_by_default_description)));

			boolean useLastAppModeByDefault = app.getSettings().USE_LAST_APPLICATION_MODE_BY_DEFAULT.get();
			addCheckableItem(R.string.shared_string_last_used, useLastAppModeByDefault, new OnClickListener() {
				@Override
				public void onClick(View v) {
					Bundle args = new Bundle();
					args.putBoolean(USE_LAST_PROFILE_ARG, true);
					Fragment target = getTargetFragment();
					if (target instanceof OnSelectProfileCallback) {
						((OnSelectProfileCallback) target).onProfileSelected(args);
					}
					dismiss();
				}
			});

			items.add(new SimpleDividerItem(app));
			for (int i = 0; i < profiles.size(); i++) {
				ProfileDataObject profile = profiles.get(i);
				addProfileItem(profile, false, !useLastAppModeByDefault);
			}
		}
	}

	private void addProfileItem(final ProfileDataObject profile, boolean showBottomDivider) {
		addProfileItem(profile, showBottomDivider, true);
	}

	private void addProfileItem(final ProfileDataObject profile,
	                            boolean showBottomDivider,
	                            boolean setupSelected) {
		OsmandApplication app = requiredMyApplication();

		int activeColorResId = nightMode ? R.color.active_color_primary_dark : R.color.active_color_primary_light;
		int iconDefaultColorResId = nightMode ? R.color.icon_color_default_dark : R.color.icon_color_default_light;
		final boolean onlineRoutingProfile = profile instanceof OnlineRoutingEngineDataObject;

		View itemView = UiUtilities.getInflater(getContext(), nightMode).inflate(
				onlineRoutingProfile ?
						R.layout.bottom_sheet_item_with_descr_radio_and_icon_btn :
						R.layout.bottom_sheet_item_with_descr_and_radio_btn, null);
		TextView tvTitle = itemView.findViewById(R.id.title);
		TextView tvDescription = itemView.findViewById(R.id.description);
		ImageView ivIcon = itemView.findViewById(R.id.icon);
		CompoundButton compoundButton = itemView.findViewById(R.id.compound_button);
		View bottomDivider = itemView.findViewById(R.id.divider_bottom);

		tvTitle.setText(profile.getName());
		tvDescription.setText(profile.getDescription());

		boolean isSelected = setupSelected && Algorithms.objectEquals(profile.getStringKey(), selectedItemKey);
		int iconColor;
		if (dialogMode == DialogMode.NAVIGATION_PROFILE) {
			int iconColorResId = isSelected ? activeColorResId : iconDefaultColorResId;
			iconColor = ContextCompat.getColor(app, iconColorResId);
		} else {
			iconColor = profile.getIconColor(nightMode);
		}

		Drawable drawableIcon = app.getUIUtilities().getPaintedIcon(profile.getIconRes(), iconColor);
		ivIcon.setImageDrawable(drawableIcon);
		compoundButton.setChecked(isSelected);
		UiUtilities.setupCompoundButton(compoundButton, nightMode, UiUtilities.CompoundButtonType.GLOBAL);
		bottomDivider.setVisibility(showBottomDivider ? View.VISIBLE : View.INVISIBLE);

		BaseBottomSheetItem.Builder builder =
				new BaseBottomSheetItem.Builder().setCustomView(itemView);

		OnClickListener listener = new OnClickListener() {
			@Override
			public void onClick(View view) {
				Bundle args = new Bundle();
				args.putString(PROFILE_KEY_ARG, profile.getStringKey());
				args.putBoolean(PROFILES_LIST_UPDATED_ARG, onlineRoutingProfile);
				Fragment target = getTargetFragment();
				if (target instanceof OnSelectProfileCallback) {
					((OnSelectProfileCallback) target).onProfileSelected(args);
				}
				dismiss();
			}
		};

		if (onlineRoutingProfile) {
			View basePart = itemView.findViewById(R.id.basic_item_body);
			View endBtn = itemView.findViewById(R.id.end_button);
			ImageView ivEndBtnIcon = itemView.findViewById(R.id.end_button_icon);

			Drawable drawable = getIcon(R.drawable.ic_action_settings,
					nightMode ?
							R.color.route_info_control_icon_color_dark :
							R.color.route_info_control_icon_color_light);
			if (Build.VERSION.SDK_INT >= 21) {
				Drawable activeDrawable = getIcon(R.drawable.ic_action_settings, activeColorResId);
				drawable = AndroidUtils.createPressedStateListDrawable(drawable, activeDrawable);
			}
			ivEndBtnIcon.setImageDrawable(drawable);

			basePart.setOnClickListener(listener);
			endBtn.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					if (getActivity() != null) {
						OnlineRoutingEngineFragment.showInstance(getActivity(), getAppMode(), profile.getStringKey());
					}
					dismiss();
				}
			});
		} else {
			builder.setOnClickListener(listener);
		}
		items.add(builder.create());
	}

	private void addCheckableItem(int titleId,
	                              boolean isSelected,
	                              OnClickListener listener) {
		OsmandApplication app = requiredMyApplication();
		View itemView = UiUtilities.getInflater(getContext(), nightMode).inflate(R.layout.bottom_sheet_item_with_descr_and_radio_btn, null);

		itemView.findViewById(R.id.icon).setVisibility(View.GONE);
		itemView.findViewById(R.id.description).setVisibility(View.GONE);
		itemView.findViewById(R.id.divider_bottom).setVisibility(View.GONE);

		Typeface typeface = FontCache.getRobotoMedium(app);
		String title = getString(titleId);
		SpannableString spannable = UiUtilities.createCustomFontSpannable(typeface, title, title, title);
		int activeColor = ContextCompat.getColor(app, getActiveColorId());
		ForegroundColorSpan colorSpan = new ForegroundColorSpan(activeColor);
		spannable.setSpan(colorSpan, 0, title.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
		((TextView) itemView.findViewById(R.id.title)).setText(spannable);

		CompoundButton compoundButton = itemView.findViewById(R.id.compound_button);
		compoundButton.setChecked(isSelected);
		UiUtilities.setupCompoundButton(compoundButton, nightMode, UiUtilities.CompoundButtonType.GLOBAL);

		items.add(new BaseBottomSheetItem.Builder()
				.setCustomView(itemView)
				.setOnClickListener(listener)
				.create());
	}

	private void addButtonItem(int titleId, int iconId, OnClickListener listener) {
		OsmandApplication app = requiredMyApplication();
		Context themedCtx = UiUtilities.getThemedContext(app, nightMode);

		int activeColorResId = AndroidUtils.resolveAttribute(themedCtx, R.attr.active_color_basic);

		View buttonView = View.inflate(themedCtx, R.layout.bottom_sheet_item_preference_btn, null);
		TextView tvTitle = buttonView.findViewById(R.id.title);
		tvTitle.setText(getString(titleId));

		ImageView ivIcon = buttonView.findViewById(R.id.icon);
		ivIcon.setImageDrawable(app.getUIUtilities().getIcon(iconId, activeColorResId));

		items.add(new BaseBottomSheetItem.Builder()
				.setCustomView(buttonView)
				.setOnClickListener(listener)
				.create());
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

	private void refreshProfiles(OsmandApplication app) {
		profiles.clear();
		switch (dialogMode) {
			case BASE_PROFILE:
				List<ApplicationMode> appModes = new ArrayList<>(ApplicationMode.allPossibleValues());
				appModes.remove(ApplicationMode.DEFAULT);
				profiles.addAll(ProfileDataUtils.getDataObjects(app, appModes));
				break;

			case NAVIGATION_PROFILE:
				profiles.addAll(ProfileDataUtils.getSortedRoutingProfiles(app));
				break;

			case DEFAULT_PROFILE:
				profiles.addAll(ProfileDataUtils.getDataObjects(app, ApplicationMode.values(app)));
				break;
		}
	}

	@Nullable
	private MapActivity getMapActivity() {
		Activity activity = getActivity();
		if (activity instanceof MapActivity) {
			return (MapActivity) activity;
		}
		return null;
	}

	public static void showInstance(@NonNull FragmentActivity activity,
	                                @NonNull DialogMode dialogMode,
	                                @Nullable Fragment target,
	                                ApplicationMode appMode,
	                                String selectedItemKey,
	                                boolean usedOnMap) {
		SelectProfileBottomSheet fragment = new SelectProfileBottomSheet();
		Bundle args = new Bundle();
		args.putString(DIALOG_TYPE, dialogMode.name());
		args.putString(SELECTED_KEY, selectedItemKey);
		fragment.setArguments(args);
		fragment.setUsedOnMap(usedOnMap);
		fragment.setAppMode(appMode);
		fragment.setTargetFragment(target, 0);
		fragment.show(activity.getSupportFragmentManager(), TAG);
	}

	public interface OnSelectProfileCallback {
		void onProfileSelected(Bundle args);
	}
}
