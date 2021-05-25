package net.osmand.plus.profiles;

import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import net.osmand.AndroidUtils;
import net.osmand.CallbackWithObject;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.DividerItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.LongDescriptionItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.onlinerouting.ui.OnlineRoutingEngineFragment;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.router.RoutingConfiguration;
import net.osmand.router.RoutingConfiguration.Builder;

import java.util.List;

import static net.osmand.plus.importfiles.ImportHelper.ImportType.ROUTING;

public class SelectNavProfileBottomSheet extends SelectProfileBottomSheet {

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
		List<ProfileDataObject> profiles = getProfiles();
		items.add(new TitleItem(getString(R.string.select_nav_profile_dialog_title)));
		items.add(new LongDescriptionItem(getString(R.string.select_nav_profile_dialog_message)));
		for (int i = 0; i < profiles.size(); i++) {
			addProfileItem(profiles.get(i));
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
				mapActivity.getImportHelper().chooseFileToImport(ROUTING, new CallbackWithObject<Builder>() {
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
		View bottomSpaceView = new View(app);
		int space = (int) getResources().getDimension(R.dimen.empty_state_text_button_padding_top);
		bottomSpaceView.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, space));
		items.add(new BaseBottomSheetItem.Builder()
				.setCustomView(bottomSpaceView)
				.create());
	}

	@Override
	protected void setupSpecificProfileItemAppearance(final ProfileDataObject profile,
	                                                  View itemView,
	                                                  BaseBottomSheetItem.Builder builder) {
		if (!isOnlineEngine(profile)) {
			super.setupSpecificProfileItemAppearance(profile, itemView, builder);
			return;
		}

		View basePart = itemView.findViewById(R.id.basic_item_body);
		View endBtn = itemView.findViewById(R.id.end_button);
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

	@Override
	protected void fillProfilesList(List<ProfileDataObject> profiles) {
		profiles.addAll(ProfileDataUtils.getSortedRoutingProfiles(app));
	}

	@Override
	protected int getIconColor(ProfileDataObject profile) {
		int iconColorResId = isSelected(profile) ? getActiveColorId() : getDefaultIconColorId();
		return ContextCompat.getColor(app, iconColorResId);
	}

	@Override
	protected boolean shouldShowBottomDivider(ProfileDataObject profile) {
		List<ProfileDataObject> profiles = getProfiles();
		boolean showBottomDivider = false;
		int i = profiles.indexOf(profile);
		if (i != -1 && profile instanceof RoutingProfileDataObject) {
			final RoutingProfileDataObject routingProfile = (RoutingProfileDataObject) profiles.get(i);
			if (i < profiles.size() - 1) {
				if (profiles.get(i + 1) instanceof RoutingProfileDataObject) {
					RoutingProfileDataObject nextProfile = (RoutingProfileDataObject) profiles.get(i + 1);
					if (routingProfile.getFileName() == null) {
						showBottomDivider = nextProfile.getFileName() != null;
					} else {
						showBottomDivider = !routingProfile.getFileName().equals(nextProfile.getFileName());
					}
				} else {
					showBottomDivider = true;
				}
			}
		}
		return showBottomDivider;
	}

	@Override
	protected boolean isProfileListUpdated(ProfileDataObject profile) {
		return isOnlineEngine(profile);
	}

	@Override
	protected int getItemLayoutId(ProfileDataObject profile) {
		return isOnlineEngine(profile) ?
				R.layout.bottom_sheet_item_with_descr_radio_and_icon_btn :
				super.getItemLayoutId(profile);
	}

	private boolean isOnlineEngine(ProfileDataObject profile) {
		return profile instanceof OnlineRoutingEngineDataObject;
	}
}
