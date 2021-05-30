package net.osmand.plus.profiles;

import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import net.osmand.AndroidUtils;
import net.osmand.CallbackWithObject;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.LongDescriptionItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.onlinerouting.ui.OnlineRoutingEngineFragment;
import net.osmand.plus.profiles.dto.ProfilesGroup;
import net.osmand.plus.profiles.dto.ProfileDataObject;
import net.osmand.plus.profiles.dto.RoutingDataObject;
import net.osmand.plus.profiles.dto.RoutingDataUtils;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.widgets.multistatetoggle.RadioItem;
import net.osmand.plus.widgets.multistatetoggle.RadioItem.OnRadioItemClickListener;
import net.osmand.plus.widgets.multistatetoggle.TextToggleButton.TextRadioItem;
import net.osmand.router.RoutingConfiguration;
import net.osmand.router.RoutingConfiguration.Builder;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;

import static net.osmand.plus.importfiles.ImportHelper.ImportType.ROUTING;

public class SelectNavProfileBottomSheet extends SelectProfileBottomSheet {

	private RoutingDataUtils dataUtils;

	private boolean onlineRouting = false;
	private List<ProfilesGroup> predefinedEngines;
	private List<ProfilesGroup> profiles = new ArrayList<>();

	public static void showInstance(@NonNull FragmentActivity activity,
	                                @Nullable Fragment target,
	                                ApplicationMode appMode,
	                                String selectedItemKey,
	                                boolean usedOnMap) {
		SelectNavProfileBottomSheet fragment = new SelectNavProfileBottomSheet();
		Bundle args = new Bundle();
		args.putString(SELECTED_KEY, selectedItemKey);
		fragment.setArguments(args);
		fragment.setUsedOnMap(usedOnMap);
		fragment.setAppMode(appMode);
		fragment.setTargetFragment(target, 0);
		fragment.show(activity.getSupportFragmentManager(), TAG);
	}

	@Override
	public void createMenuItems(@Nullable Bundle savedInstanceState) {
		createHeader();
		if (onlineRouting) {
			if (predefinedEngines != null) {
				createProfilesList();
			} else {
				addProgressWithTitleItem(R.string.loading_list_of_routing_services);
			}
			createOnlineBottom();
		} else {
			createProfilesList();
			createOfflineBottom();
		}
		addSpaceItem(getDimen(R.dimen.empty_state_text_button_padding_top));
	}

	private void createHeader() {
		items.add(new TitleItem(getString(R.string.select_nav_profile_dialog_title)));
		items.add(new LongDescriptionItem(getString(R.string.select_nav_profile_dialog_message)));
		TextRadioItem offline = createRadioButton(false);
		TextRadioItem online = createRadioButton(true);
		addToggleButton(onlineRouting ? online : offline, offline, online);
	}

	private TextRadioItem createRadioButton(final boolean online) {
		String title = getString(online ?
				R.string.shared_string_online : R.string.shared_string_offline);
		TextRadioItem item = new TextRadioItem(title);
		item.setOnClickListener(new OnRadioItemClickListener() {
			@Override
			public boolean onRadioItemClick(RadioItem radioItem, View view) {
				onlineRouting = online;
				predefinedEngines = null;

				if (online) {
					getDataUtils().downloadPredefinedEngines(new CallbackWithObject<List<ProfilesGroup>>() {
						@Override
						public boolean processResult(final List<ProfilesGroup> result) {
							predefinedEngines = result;
							refreshView();
							return false;
						}
					});
				}

				refreshView();
				return true;
			}
		});
		return item;
	}

	private void createProfilesList() {
		for (ProfilesGroup group : profiles) {
			List<ProfileDataObject> items = group.getProfiles();
			if (!Algorithms.isEmpty(items)) {
				addGroupHeaderItem(group);
				for (ProfileDataObject item : items) {
					addProfileItem(item);
				}
				addDivider();
			}
		}
	}

	private void createOfflineBottom() {
		items.add(new LongDescriptionItem(app.getString(R.string.osmand_routing_promo)));
		addButtonItem(R.string.import_routing_file, R.drawable.ic_action_folder, new OnClickListener() {
			@Override
			public void onClick(View v) {
				MapActivity mapActivity = getMapActivity();
				if (mapActivity == null) {
					return;
				}
				mapActivity.getImportHelper().chooseFileToImport(ROUTING, new CallbackWithObject<Builder>() {
					@Override
					public boolean processResult(RoutingConfiguration.Builder builder) {
						refreshView();
						return false;
					}
				});
			}
		});
	}

	private void createOnlineBottom() {
		items.add(new LongDescriptionItem(app.getString(R.string.osmand_online_routing_promo)));
		addButtonItem(R.string.add_online_routing_engine, R.drawable.ic_action_plus, new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (getActivity() != null) {
					OnlineRoutingEngineFragment.showInstance(getActivity(), getAppMode(), null);
				}
				dismiss();
			}
		});
	}

	@Override
	protected void addProfileItem(ProfileDataObject profileDataObject) {
		final RoutingDataObject profile = (RoutingDataObject) profileDataObject;
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
			items.add(builder.create());
			return;
		} else {
			View basePart = itemView.findViewById(R.id.basic_item_body);
			View endBtn = itemView.findViewById(R.id.end_button);
			TextView tvDescription = itemView.findViewById(R.id.description);
			tvDescription.setText(profile.getDescription());

			ImageView ivEndBtnIcon = itemView.findViewById(R.id.end_button_icon);
			Drawable drawable = getIcon(R.drawable.ic_action_settings, getRouteInfoColorId());
			if (Build.VERSION.SDK_INT >= 21) {
				Drawable activeDrawable = getIcon(R.drawable.ic_action_settings, getActiveColorId());
				drawable = AndroidUtils.createPressedStateListDrawable(drawable, activeDrawable);
			}
			ivEndBtnIcon.setImageDrawable(drawable);

			basePart.setOnClickListener(getItemClickListener(profile));
			endBtn.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					if (getActivity() != null) {
						OnlineRoutingEngineFragment.showInstance(getActivity(), getAppMode(), profile.getStringKey());
					}
					dismiss();
				}
			});
		}
		items.add(builder.create());
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
	protected void refreshProfiles() {
		profiles.clear();
		if (onlineRouting) {
			profiles = getDataUtils().getOnlineProfiles(predefinedEngines);
		} else {
			profiles = getDataUtils().getOfflineProfiles();
		}
	}

	private RoutingDataUtils getDataUtils() {
		if (dataUtils == null) {
			dataUtils = new RoutingDataUtils(app);
		}
		return dataUtils;
	}

}
