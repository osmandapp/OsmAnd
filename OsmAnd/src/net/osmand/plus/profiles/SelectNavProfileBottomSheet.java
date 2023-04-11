package net.osmand.plus.profiles;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.CallbackWithObject;
import net.osmand.IndexConstants;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.LongDescriptionItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.onlinerouting.EngineParameter;
import net.osmand.plus.onlinerouting.OnlineRoutingHelper;
import net.osmand.plus.onlinerouting.engine.EngineType;
import net.osmand.plus.onlinerouting.engine.OnlineRoutingEngine;
import net.osmand.plus.onlinerouting.ui.OnlineRoutingEngineFragment;
import net.osmand.plus.profiles.data.PredefinedProfilesGroup;
import net.osmand.plus.profiles.data.ProfileDataObject;
import net.osmand.plus.profiles.data.ProfilesGroup;
import net.osmand.plus.profiles.data.RoutingDataObject;
import net.osmand.plus.profiles.data.RoutingDataUtils;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.fragments.NavigationFragment;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.multistatetoggle.TextToggleButton.TextRadioItem;
import net.osmand.plus.widgets.tools.ClickableSpanTouchListener;
import net.osmand.router.RoutingConfiguration.Builder;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static net.osmand.plus.importfiles.ImportHelper.ImportType.ROUTING;
import static net.osmand.plus.onlinerouting.engine.OnlineRoutingEngine.NONE_VEHICLE;

public class SelectNavProfileBottomSheet extends SelectProfileBottomSheet {

	private static final String DOWNLOADED_PREDEFINED_JSON = "downloaded_predefined_json";
	private static final String DIALOG_TYPE = "dialog_type";

	private RoutingDataUtils dataUtils;

	private List<ProfilesGroup> predefinedGroups;
	private List<ProfilesGroup> profileGroups = new ArrayList<>();
	private boolean triedToDownload;
	private DialogMode dialogMode;
	private String predefinedJson;

	public enum DialogMode {
		OFFLINE(R.string.shared_string_offline),
		ONLINE(R.string.shared_string_online);

		DialogMode(int titleId) {
			this.titleId = titleId;
		}

		int titleId;
	}

	public static void showInstance(@NonNull FragmentActivity activity,
									@Nullable Fragment target,
									ApplicationMode appMode,
									String selectedItemKey,
									boolean usedOnMap) {
		FragmentManager fragmentManager = activity.getSupportFragmentManager();
		if (!fragmentManager.isStateSaved()) {
			SelectNavProfileBottomSheet fragment = new SelectNavProfileBottomSheet();
			Bundle args = new Bundle();
			args.putString(SELECTED_KEY, selectedItemKey);
			fragment.setArguments(args);
			fragment.setUsedOnMap(usedOnMap);
			fragment.setAppMode(appMode);
			fragment.setTargetFragment(target, 0);
			boolean isOnline = OnlineRoutingEngine.isOnlineEngineKey(selectedItemKey);
			fragment.setDialogMode(isOnline ? DialogMode.ONLINE : DialogMode.OFFLINE);
			fragment.show(fragmentManager, TAG);
		}
	}

	@Override
	public void createMenuItems(@Nullable Bundle savedInstanceState) {
		readFromBundle(savedInstanceState);
		createHeader();
		if (dialogMode == DialogMode.ONLINE) {
			if (predefinedGroups == null) {
				if (triedToDownload) {
					addNonePredefinedView();
				} else {
					addProgressWithTitleItem(getString(R.string.loading_list_of_routing_services));
					tryDownloadPredefinedItems();
				}
			}
			createProfilesList();
			createOnlineFooter();
		} else {
			createProfilesList();
			createOfflineFooter();
		}
		addSpaceItem(getDimen(R.dimen.empty_state_text_button_padding_top));
	}

	public void readFromBundle(Bundle savedState) {
		if (savedState != null) {
			if (savedState.containsKey(DIALOG_TYPE)) {
				dialogMode = DialogMode.valueOf(savedState.getString(DIALOG_TYPE));
			}
			if (savedState.containsKey(DOWNLOADED_PREDEFINED_JSON)) {
				predefinedJson = savedState.getString(DOWNLOADED_PREDEFINED_JSON);
				predefinedGroups = getDataUtils().parsePredefinedEngines(predefinedJson);
				refreshProfiles();
			}
		}
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		if (predefinedJson != null) {
			outState.putString(DOWNLOADED_PREDEFINED_JSON, predefinedJson);
		}
		if (dialogMode != null) {
			outState.putString(DIALOG_TYPE, dialogMode.name());
		}
	}

	private void createHeader() {
		items.add(new TitleItem(getString(R.string.select_nav_profile_dialog_title)));
		items.add(new LongDescriptionItem(getString(R.string.select_nav_profile_dialog_message)));
		TextRadioItem offline = createRadioButton(DialogMode.OFFLINE);
		TextRadioItem online = createRadioButton(DialogMode.ONLINE);
		TextRadioItem selectedItem = dialogMode == DialogMode.ONLINE ? online : offline;
		addToggleButton(selectedItem, offline, online);
	}

	private TextRadioItem createRadioButton(DialogMode mode) {
		String title = getString(mode.titleId);
		TextRadioItem item = new TextRadioItem(title);
		item.setOnClickListener((radioItem, view) -> {
			if (dialogMode != mode) {
				dialogMode = mode;
				predefinedGroups = null;
				triedToDownload = false;
				updateMenuItems();
				return true;
			}
			return false;
		});
		return item;
	}

	private void tryDownloadPredefinedItems() {
		getDataUtils().downloadPredefinedEngines(result -> {
			triedToDownload = true;
			predefinedJson = result;
			if (result != null) {
				predefinedGroups = getDataUtils().parsePredefinedEngines(predefinedJson);
			}
			updateMenuItems();
			return true;
		});
	}

	private void createProfilesList() {
		for (ProfilesGroup group : profileGroups) {
			List<RoutingDataObject> items = group.getProfiles();
			if (!Algorithms.isEmpty(items)) {
				addGroupHeader(group);
				for (RoutingDataObject item : items) {
					addProfileItem(item, group);
				}
				addDivider();
			}
		}
	}

	private void addNonePredefinedView() {
		int padding = getDimen(R.dimen.content_padding_half);
		addGroupHeader(getString(R.string.shared_string_predefined));
		addMessageWithRoundedBackground(
				getString(R.string.failed_loading_predefined_engines), 0, padding);

		if (OnlineRoutingEngine.isPredefinedEngineKey(selectedItemKey)) {
			ProfileDataObject selectedProfile = getDataUtils().getOnlineEngineByKey(selectedItemKey);
			addProfileItem(selectedProfile);
		}

		addDivider();
	}

	private void createOfflineFooter() {
		items.add(new LongDescriptionItem(app.getString(R.string.osmand_routing_promo)));
		addButtonItem(R.string.import_routing_file, R.drawable.ic_action_folder, v -> {
			MapActivity mapActivity = getMapActivity();
			if (mapActivity == null) {
				return;
			}
			mapActivity.getImportHelper().chooseFileToImport(ROUTING, (CallbackWithObject<Builder>) builder -> {
				Fragment targetFragment = getTargetFragment();
				if (targetFragment instanceof NavigationFragment) {
					((NavigationFragment) targetFragment).updateRoutingProfiles();
				}
				updateMenuItems();
				return false;
			});
		});
	}

	private void createOnlineFooter() {
		items.add(new LongDescriptionItem(app.getString(R.string.osmand_online_routing_promo)));
		addButtonItem(R.string.add_online_routing_engine, R.drawable.ic_action_plus, v -> {
			if (getActivity() != null) {
				OnlineRoutingEngineFragment.showInstance(getActivity(), getAppMode(), null);
			}
			dismiss();
		});
	}

	private void addGroupHeader(ProfilesGroup group) {
		CharSequence title = group.getTitle();
		CharSequence description = group.getDescription(app, nightMode);
		Context themedCtx = UiUtilities.getThemedContext(app, nightMode);
		LayoutInflater inflater = UiUtilities.getInflater(themedCtx, nightMode);
		View view = inflater.inflate(R.layout.bottom_sheet_item_title_with_description_large, null);
		View container = view.findViewById(R.id.container);

		if (isGroupImported(group)) {
			container.setOnLongClickListener(getGroupLongClickListener(group));
		}

		TextView tvTitle = view.findViewById(R.id.title);
		TextView tvDescription = view.findViewById(R.id.description);
		tvTitle.setText(title);
		if (description != null) {
			tvDescription.setText(description);
			tvDescription.setOnTouchListener(new ClickableSpanTouchListener());
		} else {
			tvDescription.setVisibility(View.GONE);
		}

		items.add(new BaseBottomSheetItem.Builder()
				.setCustomView(view)
				.create()
		);
	}

	private boolean isGroupImported(ProfilesGroup group) {
		for (RoutingDataObject profile : group.getProfiles()) {
			String fileName = profile.getFileName();
			if (fileName == null || !fileName.contentEquals(group.getTitle())) {
				return false;
			}
		}
		return true;
	}

	@Nullable
	private ProfileDataObject getSelectedRoutingProfile(ProfilesGroup group) {
		for (RoutingDataObject profile : group.getProfiles()) {
			if (isSelected(profile)) {
				return profile;
			}
		}
		return null;
	}

	protected View.OnLongClickListener getGroupLongClickListener(ProfilesGroup group) {
		String fileName = String.valueOf(group.getTitle());
		return getDeleteLongClickListener(fileName, (dialogInterface, i) -> {
			File dir = app.getAppPath(IndexConstants.ROUTING_PROFILES_DIR);
			File routingFile = new File(dir, fileName);
			if (routingFile.exists() && routingFile.delete()) {
				updateRouteProfileInAppModes(group.getProfiles());
				ProfileDataObject selectedProfile = getSelectedRoutingProfile(group);
				if (selectedProfile != null) {
					setDefaultRouteProfile(getAppMode());
				}
				app.getCustomRoutingConfigs().remove(fileName);
				updateMenuItems();
			}
		});
	}

	private void addProfileItem(ProfileDataObject profileDataObject, ProfilesGroup group) {
		RoutingDataObject profile = (RoutingDataObject) profileDataObject;
		LayoutInflater inflater = UiUtilities.getInflater(getContext(), nightMode);
		View itemView = inflater.inflate(getItemLayoutId(profile), null);

		TextView tvTitle = itemView.findViewById(R.id.title);
		tvTitle.setText(profile.getName());

		ImageView ivIcon = itemView.findViewById(R.id.icon);
		Drawable drawableIcon = getPaintedIcon(profile.getIconRes(), getIconColor(profile));
		ivIcon.setImageDrawable(drawableIcon);

		CompoundButton compoundButton = itemView.findViewById(R.id.compound_button);
		compoundButton.setChecked(isSelected(profile));
		UiUtilities.setupCompoundButton(compoundButton, nightMode, UiUtilities.CompoundButtonType.GLOBAL);

		BaseBottomSheetItem.Builder builder = new BaseBottomSheetItem.Builder().setCustomView(itemView);

		if (!profile.isOnline() || profile.isPredefined()) {
			builder.setOnClickListener(getItemClickListener(profile));
			if (!Algorithms.isEmpty(profile.getFileName())) {
				builder.setOnLongClickListener(getItemLongClickListener(profile, group));
			}
			items.add(builder.create());
			return;
		} else {
			View basePart = itemView.findViewById(R.id.basic_item_body);
			View endBtn = itemView.findViewById(R.id.end_button);
			TextView tvDescription = itemView.findViewById(R.id.description);
			tvDescription.setText(profile.getDescription());

			ImageView ivEndBtnIcon = itemView.findViewById(R.id.end_button_icon);
			Drawable drawable = getIcon(R.drawable.ic_action_settings, getRouteInfoColorId());
			Drawable activeDrawable = getIcon(R.drawable.ic_action_settings, getActiveColorId());
			drawable = AndroidUtils.createPressedStateListDrawable(drawable, activeDrawable);
			ivEndBtnIcon.setImageDrawable(drawable);

			basePart.setOnClickListener(getItemClickListener(profile));
			endBtn.setOnClickListener(v -> {
				if (getActivity() != null) {
					OnlineRoutingEngineFragment.showInstance(getActivity(), getAppMode(), profile.getStringKey());
				}
				dismiss();
			});
		}
		items.add(builder.create());
	}

	protected View.OnLongClickListener getItemLongClickListener(RoutingDataObject profile, ProfilesGroup group) {
		return getDeleteLongClickListener(profile.getFileName(), (dialogInterface, i) -> {
			File dir = app.getAppPath(IndexConstants.ROUTING_PROFILES_DIR);
			File routingFile = new File(dir, profile.getFileName());
			if (routingFile.exists() && routingFile.delete()) {
				updateRouteProfileInAppModes(group.getProfiles());
				if (isSelected(profile)) {
					setDefaultRouteProfile(getAppMode());
				}
				app.getCustomRoutingConfigs().remove(profile.getFileName());
				updateMenuItems();
			}
		});
	}

	private View.OnLongClickListener getDeleteLongClickListener(String fileName, DialogInterface.OnClickListener positiveAlertButton) {
		return view -> {
			AlertDialog.Builder builder = new AlertDialog.Builder(UiUtilities.getThemedContext(getMapActivity(), isNightMode(app)));
			builder.setTitle(getString(R.string.delete_confirmation_msg, fileName));
			builder.setMessage(getString(R.string.are_you_sure));
			builder.setNegativeButton(R.string.shared_string_cancel, null)
					.setPositiveButton(R.string.shared_string_ok, positiveAlertButton);
			builder.show();
			return true;
		};
	}

	private void setDefaultRouteProfile(ApplicationMode applicationMode) {
		String routingProfile = applicationMode.getDefaultRoutingProfile();
		String derivedProfile = applicationMode.getDefaultDerivedProfile();
		Bundle args = new Bundle();
		args.putString(PROFILE_KEY_ARG, routingProfile);
		args.putBoolean(PROFILES_LIST_UPDATED_ARG, false);
		if (!Algorithms.isEmpty(derivedProfile)) {
			args.putString(DERIVED_PROFILE_ARG, derivedProfile);
		}
		Fragment target = getTargetFragment();
		if (target instanceof OnSelectProfileCallback) {
			((OnSelectProfileCallback) target).onProfileSelected(args);
		}
		dismiss();
	}

	private void updateRouteProfileInAppModes(List<RoutingDataObject> deletedRoutingProfiles) {
		List<ApplicationMode> applicationModes = ApplicationMode.allPossibleValues();
		Fragment targetFragment = getTargetFragment();
		for (ApplicationMode mode : applicationModes) {
			String routingProfile = mode.getRoutingProfile();
			for (RoutingDataObject deletedRoutingProfile : deletedRoutingProfiles) {
				if (targetFragment instanceof NavigationFragment && routingProfile.equals(deletedRoutingProfile.getStringKey())) {
					((NavigationFragment) targetFragment).updateAppMode(mode, mode.getDefaultRoutingProfile(), mode.getDefaultDerivedProfile());
				}
			}
		}
	}

	@Override
	protected int getIconColor(ProfileDataObject profile) {
		int iconColorResId = isSelected(profile) ? getActiveColorId() : getDefaultIconColorId();
		return ContextCompat.getColor(app, iconColorResId);
	}

	@Override
	protected int getItemLayoutId(ProfileDataObject profile) {
		if (profile instanceof RoutingDataObject) {
			RoutingDataObject routingProfile = (RoutingDataObject) profile;
			if (routingProfile.isOnline() && !routingProfile.isPredefined()) {
				return R.layout.bottom_sheet_item_with_descr_radio_and_icon_btn;
			}
		}
		return R.layout.bottom_sheet_item_with_radio_btn;
	}

	@Override
	protected boolean isProfilesListUpdated(ProfileDataObject profile) {
		return profile instanceof RoutingDataObject && ((RoutingDataObject) profile).isOnline();
	}

	@Override
	protected boolean isSelected(ProfileDataObject profile) {
		boolean isSelected = super.isSelected(profile);
		String derivedProfile = getAppMode().getDerivedProfile();
		if (isSelected && profile instanceof RoutingDataObject) {
			RoutingDataObject data = (RoutingDataObject) profile;
			boolean checkForDerived = !Algorithms.objectEquals(derivedProfile, "default");
			if (checkForDerived) {
				isSelected = Algorithms.objectEquals(derivedProfile, data.getDerivedProfile());
			} else {
				isSelected = data.getDerivedProfile() == null;
			}
		}
		return isSelected;
	}

	@Override
	protected void refreshProfiles() {
		profileGroups.clear();
		if (dialogMode == DialogMode.ONLINE) {
			profileGroups = getDataUtils().getOnlineProfiles(predefinedGroups);
		} else {
			profileGroups = getDataUtils().getOfflineProfiles();
		}
	}

	@Override
	protected void onItemSelected(ProfileDataObject profile) {
		if (((RoutingDataObject) profile).isPredefined()) {
			savePredefinedEngine((RoutingDataObject) profile);
		}
		super.onItemSelected(profile);
	}

	private void savePredefinedEngine(RoutingDataObject profile) {
		String stringKey = profile.getStringKey();
		OnlineRoutingHelper helper = app.getOnlineRoutingHelper();
		ProfilesGroup profilesGroup = findGroupOfProfile(profile);
		if (profilesGroup != null) {
			PredefinedProfilesGroup group = (PredefinedProfilesGroup) profilesGroup;
			String type = group.getType().toUpperCase();
			OnlineRoutingEngine engine = EngineType.getTypeByName(type).newInstance(null);
			engine.put(EngineParameter.KEY, stringKey);
			engine.put(EngineParameter.VEHICLE_KEY, NONE_VEHICLE.getKey());
			engine.put(EngineParameter.CUSTOM_URL, profile.getDescription());
			String namePattern = getString(R.string.ltr_or_rtl_combine_via_dash);
			String name = String.format(namePattern, group.getTitle(), profile.getName());
			engine.put(EngineParameter.CUSTOM_NAME, name);
			helper.saveEngine(engine);
		}
	}

	private ProfilesGroup findGroupOfProfile(ProfileDataObject profile) {
		for (ProfilesGroup group : profileGroups) {
			if (group.getProfiles().contains(profile)) {
				return group;
			}
		}
		return null;
	}

	private RoutingDataUtils getDataUtils() {
		if (dataUtils == null) {
			dataUtils = new RoutingDataUtils(app);
		}
		return dataUtils;
	}

	public void setDialogMode(DialogMode dialogMode) {
		this.dialogMode = dialogMode;
	}

}
