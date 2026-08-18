package net.osmand.plus.settings.fragments;

import static net.osmand.plus.settings.backend.backup.items.FileSettingsItem.FileSubtype;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.IndexConstants;
import net.osmand.PlatformUtil;
import net.osmand.map.ITileSource;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.avoidroads.AvoidRoadInfo;
import net.osmand.plus.helpers.ColorsPaletteUtils;
import net.osmand.plus.helpers.FileNameTranslationHelper;
import net.osmand.plus.helpers.SearchHistoryHelper.HistoryEntry;
import net.osmand.plus.mapmarkers.ItineraryType;
import net.osmand.plus.mapmarkers.MapMarker;
import net.osmand.plus.mapmarkers.MapMarkersGroup;
import net.osmand.plus.myplaces.favorites.FavoriteGroup;
import net.osmand.plus.onlinerouting.engine.OnlineRoutingEngine;
import net.osmand.plus.plugins.audionotes.AudioVideoNotesPlugin;
import net.osmand.plus.plugins.audionotes.AudioVideoNotesPlugin.Recording;
import net.osmand.plus.poi.PoiUIFilter;
import net.osmand.plus.profiles.ProfileIconColors;
import net.osmand.plus.profiles.data.RoutingProfilesResources;
import net.osmand.plus.render.RenderingIcons;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.ApplicationModeBean;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.mapwidgets.configure.buttons.QuickActionButtonState;
import net.osmand.shared.gpx.GpxHelper;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.io.File;
import java.util.List;

public class DuplicatesSettingsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

	private static final Log LOG = PlatformUtil.getLog(DuplicatesSettingsAdapter.class.getName());
	private static final int HEADER_TYPE = 0;
	private static final int ITEM_TYPE = 1;

	private final boolean nightMode;
	private final OsmandApplication app;
	private final UiUtilities uiUtilities;
	private final List<? super Object> items;
	private final int activeColorRes;

	DuplicatesSettingsAdapter(OsmandApplication app, List<? super Object> items, boolean nightMode) {
		this.app = app;
		this.items = items;
		this.nightMode = nightMode;
		uiUtilities = app.getUIUtilities();
		activeColorRes = nightMode
				? R.color.icon_color_active_dark
				: R.color.icon_color_active_light;
	}

	@NonNull
	@Override
	public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		LayoutInflater inflater = UiUtilities.getInflater(app, nightMode);
		if (viewType == HEADER_TYPE) {
			View view = inflater.inflate(R.layout.list_item_header_import, parent, false);
			return new HeaderViewHolder(view);
		} else {
			View view = inflater.inflate(R.layout.list_item_import, parent, false);
			return new ItemViewHolder(view);
		}
	}

	@Override
	public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
		Object currentItem = items.get(position);
		if (holder instanceof HeaderViewHolder) {
			HeaderViewHolder headerHolder = (HeaderViewHolder) holder;
			headerHolder.title.setText((String) currentItem);
			headerHolder.subTitle.setText(String.format(app.getString(R.string.listed_exist), currentItem));
			headerHolder.divider.setVisibility(View.VISIBLE);
		} else if (holder instanceof ItemViewHolder) {
			ItemViewHolder itemHolder = (ItemViewHolder) holder;
			itemHolder.subTitle.setVisibility(View.GONE);
			if (currentItem instanceof ApplicationModeBean) {
				ApplicationModeBean modeBean = (ApplicationModeBean) currentItem;
				String profileName = modeBean.userProfileName;
				if (Algorithms.isEmpty(profileName)) {
					ApplicationMode appMode = ApplicationMode.valueOfStringKey(modeBean.stringKey, null);
					if (appMode != null) {
						profileName = appMode.toHumanString();
					} else {
						profileName = Algorithms.capitalizeFirstLetter(modeBean.stringKey);
					}
				}
				itemHolder.title.setText(profileName);
				String routingProfile = "";
				String routingProfileValue = modeBean.routingProfile;
				if (!routingProfileValue.isEmpty()) {
					try {
						routingProfile = app.getString(RoutingProfilesResources.valueOf(routingProfileValue.toUpperCase()).getStringRes());
						routingProfile = Algorithms.capitalizeFirstLetterAndLowercase(routingProfile);
					} catch (IllegalArgumentException e) {
						routingProfile = Algorithms.capitalizeFirstLetterAndLowercase(routingProfileValue);
						LOG.error("Error trying to get routing resource for " + routingProfileValue + "\n" + e);
					}
				}
				if (Algorithms.isEmpty(routingProfile)) {
					itemHolder.subTitle.setVisibility(View.GONE);
				} else {
					itemHolder.subTitle.setText(String.format(
							app.getString(R.string.ltr_or_rtl_combine_via_colon),
							app.getString(R.string.nav_type_hint),
							routingProfile));
					itemHolder.subTitle.setVisibility(View.VISIBLE);
				}
				int profileIconRes = AndroidUtils.getDrawableId(app, modeBean.iconName);
				ProfileIconColors iconColor = modeBean.iconColor;
				Integer customIconColor = modeBean.customIconColor;
				int actualIconColor = customIconColor != null ?
						customIconColor : ContextCompat.getColor(app, iconColor.getColor(nightMode));
				itemHolder.icon.setImageDrawable(uiUtilities.getPaintedIcon(profileIconRes, actualIconColor));
			} else if (currentItem instanceof QuickActionButtonState) {
				QuickActionButtonState buttonState = (QuickActionButtonState) currentItem;
				itemHolder.title.setText(buttonState.getName());
				itemHolder.icon.setImageDrawable(buttonState.getIcon(ColorUtilities.getColor(app, activeColorRes), nightMode, false));
			} else if (currentItem instanceof PoiUIFilter) {
				PoiUIFilter filter = (PoiUIFilter) currentItem;
				itemHolder.title.setText(filter.getName());
				int iconRes = RenderingIcons.getBigIconResourceId(filter.getIconId());
				itemHolder.icon.setImageDrawable(uiUtilities.getIcon(iconRes != 0 ? iconRes : R.drawable.ic_action_user, activeColorRes));
			} else if (currentItem instanceof ITileSource) {
				itemHolder.title.setText(((ITileSource) currentItem).getName());
				itemHolder.icon.setImageDrawable(uiUtilities.getIcon(R.drawable.ic_map, activeColorRes));
			} else if (currentItem instanceof File) {
				File file = (File) currentItem;
				FileSubtype fileSubtype = FileSubtype.getSubtypeByPath(app, file.getPath());
				itemHolder.title.setText(file.getName());
				if (file.getAbsolutePath().contains(IndexConstants.RENDERERS_DIR)) {
					itemHolder.icon.setImageDrawable(uiUtilities.getIcon(R.drawable.ic_action_map_style, activeColorRes));
				} else if (file.getAbsolutePath().contains(IndexConstants.ROUTING_PROFILES_DIR)) {
					itemHolder.icon.setImageDrawable(uiUtilities.getIcon(R.drawable.ic_action_route_distance, activeColorRes));
				} else if (file.getAbsolutePath().contains(IndexConstants.GPX_INDEX_DIR)) {
					itemHolder.title.setText(GpxHelper.INSTANCE.getGpxTitle(file.getName()));
					itemHolder.icon.setImageDrawable(uiUtilities.getIcon(R.drawable.ic_action_route_distance, activeColorRes));
				} else if (file.getAbsolutePath().contains(IndexConstants.AV_INDEX_DIR)) {
					int iconId = AudioVideoNotesPlugin.getIconIdForRecordingFile(file);
					if (iconId == -1) {
						iconId = R.drawable.ic_action_photo_dark;
					}
					itemHolder.title.setText(new Recording(file).getName(app, true));
					itemHolder.icon.setImageDrawable(uiUtilities.getIcon(iconId, activeColorRes));
				} else if (fileSubtype == FileSubtype.FAVORITES_BACKUP) {
					itemHolder.icon.setImageDrawable(uiUtilities.getIcon(R.drawable.ic_action_folder_favorites, activeColorRes));
				} else if (fileSubtype == FileSubtype.COLOR_PALETTE) {
					itemHolder.icon.setImageDrawable(uiUtilities.getIcon(fileSubtype.getIconId(), activeColorRes));
					itemHolder.title.setText(ColorsPaletteUtils.getPaletteName(file));
					itemHolder.subTitle.setText(ColorsPaletteUtils.getPaletteTypeName(app, file));
				} else if (fileSubtype.isMap()
						|| fileSubtype == FileSubtype.TTS_VOICE
						|| fileSubtype == FileSubtype.VOICE) {
					itemHolder.title.setText(FileNameTranslationHelper.getFileNameWithRegion(app, file.getName()));
					itemHolder.icon.setImageDrawable(uiUtilities.getIcon(fileSubtype.getIconId(), activeColorRes));
				}
			} else if (currentItem instanceof AvoidRoadInfo) {
				itemHolder.title.setText(((AvoidRoadInfo) currentItem).getName(app));
				itemHolder.icon.setImageDrawable(app.getUIUtilities().getIcon(R.drawable.ic_action_alert, activeColorRes));
			} else if (currentItem instanceof FavoriteGroup) {
				itemHolder.title.setText(((FavoriteGroup) currentItem).getDisplayName(app));
				itemHolder.icon.setImageDrawable(app.getUIUtilities().getIcon(R.drawable.ic_action_favorite, activeColorRes));
			} else if (currentItem instanceof MapMarker) {
				MapMarker mapMarker = (MapMarker) currentItem;
				itemHolder.title.setText(mapMarker.getName(app));
				itemHolder.icon.setImageDrawable(app.getUIUtilities().getIcon(R.drawable.ic_action_flag, activeColorRes));
			} else if (currentItem instanceof HistoryEntry) {
				itemHolder.title.setText(((HistoryEntry) currentItem).getName().getName());
			} else if (currentItem instanceof OnlineRoutingEngine) {
				itemHolder.title.setText(((OnlineRoutingEngine) currentItem).getName(app));
				itemHolder.icon.setImageDrawable(app.getUIUtilities().getIcon(R.drawable.ic_world_globe_dark, activeColorRes));
			} else if (currentItem instanceof MapMarkersGroup) {
				MapMarkersGroup markersGroup = (MapMarkersGroup) currentItem;
				String groupName = markersGroup.getName();
				if (Algorithms.isEmpty(groupName)) {
					if (markersGroup.getType() == ItineraryType.FAVOURITES) {
						groupName = app.getString(R.string.shared_string_favorites);
					} else if (markersGroup.getType() == ItineraryType.MARKERS) {
						groupName = app.getString(R.string.map_markers);
					}
				}
				itemHolder.title.setText(groupName);
				itemHolder.icon.setImageDrawable(app.getUIUtilities().getIcon(R.drawable.ic_action_flag, activeColorRes));
			}
			itemHolder.divider.setVisibility(shouldShowDivider(position) ? View.VISIBLE : View.GONE);
		}
	}

	@Override
	public int getItemCount() {
		return items.size();
	}

	@Override
	public int getItemViewType(int position) {
		if (items.get(position) instanceof String) {
			return HEADER_TYPE;
		} else {
			return ITEM_TYPE;
		}
	}

	private static class HeaderViewHolder extends RecyclerView.ViewHolder {
		TextView title;
		TextView subTitle;
		View divider;

		HeaderViewHolder(View itemView) {
			super(itemView);
			title = itemView.findViewById(R.id.title);
			subTitle = itemView.findViewById(R.id.sub_title);
			divider = itemView.findViewById(R.id.top_divider);
		}
	}

	private static class ItemViewHolder extends RecyclerView.ViewHolder {
		TextView title;
		TextView subTitle;
		ImageView icon;
		View divider;

		ItemViewHolder(View itemView) {
			super(itemView);
			title = itemView.findViewById(R.id.title);
			subTitle = itemView.findViewById(R.id.sub_title);
			icon = itemView.findViewById(R.id.icon);
			divider = itemView.findViewById(R.id.bottom_divider);
		}
	}

	private boolean shouldShowDivider(int position) {
		boolean isLast = position == items.size() - 1;
		if (isLast) {
			return true;
		} else {
			Object next = items.get(position + 1);
			return next instanceof String;
		}
	}
}