package net.osmand.plus.mapmarkers.adapters;

import static net.osmand.plus.views.mapwidgets.WidgetType.MARKERS_TOP_BAR;
import static net.osmand.plus.views.mapwidgets.WidgetsPanel.TOP;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;

import net.osmand.IndexConstants;
import net.osmand.plus.shared.SharedUtil;
import net.osmand.data.LatLon;
import net.osmand.shared.gpx.GpxFile;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapmarkers.GroupHeader;
import net.osmand.plus.mapmarkers.ItineraryType;
import net.osmand.plus.mapmarkers.MapMarker;
import net.osmand.plus.mapmarkers.MapMarkersGroup;
import net.osmand.plus.mapmarkers.MapMarkersHelper;
import net.osmand.plus.mapmarkers.SelectWptCategoriesBottomSheetDialogFragment;
import net.osmand.plus.mapmarkers.ShowHideHistoryButton;
import net.osmand.plus.track.GpxSelectionParams;
import net.osmand.plus.track.helpers.GpxSelectionHelper;
import net.osmand.plus.track.helpers.SelectedGpxFile;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.utils.UpdateLocationUtils;
import net.osmand.plus.utils.UpdateLocationUtils.UpdateLocationViewCache;
import net.osmand.plus.views.mapwidgets.WidgetsVisibilityHelper;
import net.osmand.plus.wikivoyage.article.WikivoyageArticleDialogFragment;
import net.osmand.plus.wikivoyage.data.TravelArticle;
import net.osmand.plus.wikivoyage.data.TravelHelper;
import net.osmand.util.Algorithms;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MapMarkersGroupsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

	private static final int HEADER_TYPE = 1;
	private static final int MARKER_TYPE = 2;
	private static final int SHOW_HIDE_HISTORY_TYPE = 3;
	private static final int CATEGORIES_TYPE = 4;


	private final MapActivity mapActivity;
	private final OsmandApplication app;
	private List<Object> items = new ArrayList<>();
	private final boolean nightMode;
	private boolean showDirectionEnabled;
	private List<MapMarker> showDirectionMarkers;
	private Snackbar snackbar;

	private MapMarkersGroupsAdapterListener listener;
	private final UpdateLocationViewCache updateLocationViewCache;

	public void setListener(MapMarkersGroupsAdapterListener listener) {
		this.listener = listener;
	}

	public MapMarkersGroupsAdapter(@NonNull MapActivity mapActivity) {
		this.mapActivity = mapActivity;
		app = mapActivity.getMyApplication();
		updateLocationViewCache = UpdateLocationUtils.getUpdateLocationViewCache(mapActivity);
		nightMode = !app.getSettings().isLightContent();
		updateShowDirectionMarkers();
		createDisplayGroups();
	}

	private void updateShowDirectionMarkers() {
		List<MapMarker> mapMarkers = app.getMapMarkersHelper().getMapMarkers();
		int markersCount = mapMarkers.size();
		showDirectionMarkers = new ArrayList<>(mapMarkers.subList(0, getToIndex(markersCount)));
		showDirectionEnabled = WidgetsVisibilityHelper.isWidgetEnabled(mapActivity, TOP, MARKERS_TOP_BAR.id);
	}

	private int getToIndex(int markersCount) {
		if (markersCount > 0) {
			if (markersCount > 1 && app.getSettings().DISPLAYED_MARKERS_WIDGETS_COUNT.get() == 2) {
				return 2;
			}
			return 1;
		}
		return 0;
	}

	private void createDisplayGroups() {
		items = new ArrayList<>();
		MapMarkersHelper helper = app.getMapMarkersHelper();
		helper.updateGroups();
		List<MapMarkersGroup> groups = new ArrayList<>(helper.getVisibleMapMarkersGroups());
		groups.addAll(helper.getGroupsForDisplayedGpx());
		groups.addAll(helper.getGroupsForSavedArticlesTravelBook());

		// evaluate time constants
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		long currentTimeMillis = System.currentTimeMillis();
		Calendar currentDateCalendar = Calendar.getInstance();
		currentDateCalendar.setTimeInMillis(currentTimeMillis);
		// evaluate today, yesterday, last 7 days
		String today = dateFormat.format(currentDateCalendar.getTime());
		currentDateCalendar.add(Calendar.DAY_OF_YEAR, -1);
		String yesterday = dateFormat.format(currentDateCalendar.getTime());
		currentDateCalendar.set(Calendar.HOUR_OF_DAY, 0);
		currentDateCalendar.set(Calendar.MINUTE, 0);
		currentDateCalendar.add(Calendar.DAY_OF_YEAR, -6);
		long last7Days = currentDateCalendar.getTimeInMillis();
		// evaluate this year & last 3 months
		currentDateCalendar.setTimeInMillis(currentTimeMillis);
		String thisYear = dateFormat.format(currentDateCalendar.getTime()).substring(0, 5);
		currentDateCalendar.add(Calendar.MONTH, -1);
		String monthMinus1 = dateFormat.format(currentDateCalendar.getTime()).substring(0, 8);
		currentDateCalendar.add(Calendar.MONTH, -1);
		String monthMinus2 = dateFormat.format(currentDateCalendar.getTime()).substring(0, 8);
		currentDateCalendar.add(Calendar.MONTH, -1);
		String monthMinus3 = dateFormat.format(currentDateCalendar.getTime()).substring(0, 8);


		Calendar markerCalendar = Calendar.getInstance();

		for (int i = 0; i < groups.size(); i++) {
			MapMarkersGroup group = groups.get(i);
			String markerGroupName = group.getName();

			if (markerGroupName == null) {
				int previousGroupDateId = 0;
				List<MapMarker> groupMarkers = group.getActiveMarkers();
				for (int j = 0; j < groupMarkers.size(); j++) {
					MapMarker marker = groupMarkers.get(j);
					String markerDate = dateFormat.format(new Date(marker.creationDate));
					int currentGroupDateId;
					MarkerGroupItem currentGroupItem = null;
					if (marker.creationDate >= currentTimeMillis || (today.equals(markerDate))) {
						currentGroupDateId = -1;
						currentGroupItem = MarkerGroupItem.TODAY_HEADER;
					} else if (yesterday.equals(markerDate)) {
						currentGroupDateId = -2;
						currentGroupItem = MarkerGroupItem.YESTERDAY_HEADER;
					} else if (marker.creationDate >= last7Days) {
						currentGroupDateId = -3;
						currentGroupItem = MarkerGroupItem.LAST_SEVEN_DAYS_HEADER;
					} else if (markerDate.startsWith(monthMinus1)) {
						currentGroupDateId = -5;
					} else if (markerDate.startsWith(monthMinus2)) {
						currentGroupDateId = -6;
					} else if (markerDate.startsWith(monthMinus3)) {
						currentGroupDateId = -7;
					} else if (markerDate.startsWith(thisYear)) {
						currentGroupItem = MarkerGroupItem.THIS_YEAR_HEADER;
						currentGroupDateId = -4;
					} else {
						markerCalendar.setTimeInMillis(marker.creationDate);
						currentGroupDateId = markerCalendar.get(Calendar.YEAR);
					}
					if (previousGroupDateId != currentGroupDateId) {
						if (currentGroupItem != null) {
							items.add(currentGroupItem);
						} else if (currentGroupDateId < 0) {
							SimpleDateFormat monthdateFormat = new SimpleDateFormat("LLLL", Locale.getDefault());
							String monthStr = monthdateFormat.format(new Date(marker.creationDate));
							if (monthStr.length() > 1) {
								monthStr = Algorithms.capitalizeFirstLetter(monthStr);
							}
							items.add(new MarkerGroupItem(monthStr));
						} else {
							items.add(new MarkerGroupItem(currentGroupDateId + ""));
						}
						previousGroupDateId = currentGroupDateId;
					}
					items.add(marker);
				}
			} else {
				items.add(new GroupHeader(group));
				if (!group.isDisabled()) {
					if (group.getWptCategories() != null && !group.getWptCategories().isEmpty()) {
						CategoriesSubHeader categoriesSubHeader = new CategoriesSubHeader(group);
						items.add(categoriesSubHeader);
					}
					TravelHelper travelHelper = app.getTravelHelper();
					if (travelHelper.isAnyTravelBookPresent()) {
						List<TravelArticle> savedArticles = travelHelper.getBookmarksHelper().getSavedArticles();
						for (TravelArticle art : savedArticles) {
							String gpxName = travelHelper.getGPXName(art);
							File path = app.getAppPath(IndexConstants.GPX_TRAVEL_DIR + gpxName);
							if (path.getAbsolutePath().equals(group.getGpxPath(app))) {
								group.setWikivoyageArticle(art);
							}
						}
					}
				}
				if (Algorithms.isEmpty(group.getWptCategories())) {
					helper.updateGroupWptCategories(group, getGpxFile(group.getGpxPath(app)).getPointsGroups().keySet());
				}
				populateAdapterWithGroupMarkers(group, getItemCount());
			}
		}
	}

	private GpxFile getGpxFile(String filePath) {
		if (filePath != null) {
			SelectedGpxFile selectedGpx = app.getSelectedGpxHelper().getSelectedFileByPath(filePath);
			if (selectedGpx != null) {
				return selectedGpx.getGpxFile();
			}
			return SharedUtil.loadGpxFile(new File(filePath));
		}
		return null;
	}

	private void populateAdapterWithGroupMarkers(MapMarkersGroup group, int position) {
		if (position != RecyclerView.NO_POSITION) {
			ShowHideHistoryButton showHideHistoryButton = group.getShowHideHistoryButton();
			if (!group.isDisabled()) {
				List<Object> objectsToAdd = new ArrayList<>();
				if (showHideHistoryButton != null && showHideHistoryButton.showHistory) {
					objectsToAdd.addAll(group.getMarkers());
				} else {
					objectsToAdd.addAll(group.getActiveMarkers());
				}
				if (showHideHistoryButton != null) {
					objectsToAdd.add(showHideHistoryButton);
				}
				items.addAll(position, objectsToAdd);
			} else {
				items.removeAll(group.getActiveMarkers());
				if (showHideHistoryButton != null) {
					items.remove(showHideHistoryButton);
				}
			}
		}
	}

	public int getGroupHeaderPosition(String groupId) {
		MapMarkersGroup group = app.getMapMarkersHelper().getMapMarkerGroupById(groupId, ItineraryType.MARKERS);
		if (group != null) {
			for (int i = 0; i < items.size(); i++) {
				Object item = items.get(i);
				if (item instanceof GroupHeader) {
					GroupHeader header = (GroupHeader) item;
					if (Algorithms.stringsEqual(header.getGroup().getId(), groupId)) {
						return i;
					}
				}
			}
		}
		return -1;
	}

	public void updateDisplayedData() {
		createDisplayGroups();
		updateShowDirectionMarkers();
		notifyDataSetChanged();
	}

	@NonNull
	@Override
	public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
		if (viewType == MARKER_TYPE) {
			View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.map_marker_item_new, viewGroup, false);
			view.setOnClickListener(v -> listener.onItemClick(v));
			return new MapMarkerItemViewHolder(view);
		} else if (viewType == HEADER_TYPE) {
			View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.map_marker_item_header, viewGroup, false);
			return new MapMarkerHeaderViewHolder(view);
		} else if (viewType == SHOW_HIDE_HISTORY_TYPE) {
			View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.map_marker_item_show_hide_history, viewGroup, false);
			return new MapMarkersShowHideHistoryViewHolder(view);
		} else if (viewType == CATEGORIES_TYPE) {
			View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.map_marker_item_subheader, viewGroup, false);
			return new MapMarkerCategoriesViewHolder(view);
		} else {
			throw new IllegalArgumentException("Unsupported view type");
		}
	}

	@Override
	public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
		UiUtilities iconsCache = app.getUIUtilities();
		if (holder instanceof MapMarkerItemViewHolder) {
			MapMarkerItemViewHolder itemViewHolder = (MapMarkerItemViewHolder) holder;
			MapMarker marker = (MapMarker) getItem(position);

			boolean markerInHistory = marker.history;

			int color;
			if (marker.history) {
				color = R.color.icon_color_default_dark;
			} else {
				color = MapMarker.getColorId(marker.colorIndex);
			}
			ImageView markerImageViewToUpdate;
			int drawableResToUpdate;
			int actionIconColor = nightMode ? R.color.icon_color_primary_dark : R.color.icon_color_primary_light;
			boolean markerToHighlight = showDirectionMarkers.contains(marker);
			if (showDirectionEnabled && markerToHighlight) {
				itemViewHolder.iconDirection.setVisibility(View.GONE);

				itemViewHolder.icon.setImageDrawable(iconsCache.getIcon(R.drawable.ic_arrow_marker_diretion, color));
				itemViewHolder.mainLayout.setBackgroundColor(ContextCompat.getColor(mapActivity, nightMode ? R.color.list_divider_dark : R.color.markers_top_bar_background));
				itemViewHolder.title.setTextColor(ContextCompat.getColor(mapActivity, nightMode ? R.color.text_color_primary_dark : R.color.card_and_list_background_light));
				itemViewHolder.divider.setBackgroundColor(ContextCompat.getColor(mapActivity, R.color.map_markers_on_map_divider_color));
				itemViewHolder.optionsBtn.setBackground(AppCompatResources.getDrawable(mapActivity, R.drawable.marker_circle_background_on_map_with_inset));
				itemViewHolder.optionsBtn.setImageDrawable(iconsCache.getIcon(markerInHistory ? R.drawable.ic_action_reset_to_default_dark : R.drawable.ic_action_marker_passed, R.color.card_and_list_background_light));
				itemViewHolder.description.setTextColor(ContextCompat.getColor(mapActivity, R.color.map_markers_on_map_color));

				drawableResToUpdate = R.drawable.ic_arrow_marker_diretion;
				markerImageViewToUpdate = itemViewHolder.icon;
			} else {
				itemViewHolder.iconDirection.setVisibility(View.VISIBLE);

				itemViewHolder.icon.setImageDrawable(iconsCache.getIcon(R.drawable.ic_action_flag, color));

				itemViewHolder.mainLayout.setBackgroundColor(ColorUtilities.getListBgColor(mapActivity, nightMode));
				itemViewHolder.title.setTextColor(ColorUtilities.getPrimaryTextColor(mapActivity, nightMode));
				itemViewHolder.divider.setBackgroundColor(ContextCompat.getColor(mapActivity, nightMode ? R.color.app_bar_main_dark : R.color.divider_color_light));
				itemViewHolder.optionsBtn.setBackground(AppCompatResources.getDrawable(mapActivity, nightMode ? R.drawable.marker_circle_background_dark_with_inset : R.drawable.marker_circle_background_light_with_inset));
				itemViewHolder.optionsBtn.setImageDrawable(iconsCache.getIcon(markerInHistory ? R.drawable.ic_action_reset_to_default_dark : R.drawable.ic_action_marker_passed, actionIconColor));
				itemViewHolder.description.setTextColor(ColorUtilities.getDefaultIconColor(mapActivity, nightMode));

				drawableResToUpdate = R.drawable.ic_direction_arrow;
				markerImageViewToUpdate = itemViewHolder.iconDirection;
			}

			itemViewHolder.title.setText(marker.getName(app));

			boolean noGroup = marker.groupName == null;
			boolean createdEarly = false;
			if (noGroup && !markerInHistory) {
				Calendar currentDateCalendar = Calendar.getInstance();
				currentDateCalendar.setTimeInMillis(System.currentTimeMillis());
				int currentDay = currentDateCalendar.get(Calendar.DAY_OF_YEAR);
				int currentYear = currentDateCalendar.get(Calendar.YEAR);
				Calendar markerCalendar = Calendar.getInstance();
				markerCalendar.setTimeInMillis(System.currentTimeMillis());
				int markerDay = markerCalendar.get(Calendar.DAY_OF_YEAR);
				int markerYear = markerCalendar.get(Calendar.YEAR);
				createdEarly = currentDay - markerDay >= 2 || currentYear != markerYear;
			}
			if (markerInHistory || createdEarly) {
				itemViewHolder.point.setVisibility(View.VISIBLE);
				itemViewHolder.description.setVisibility(View.VISIBLE);
				long date;
				if (markerInHistory) {
					date = marker.visitedDate;
				} else {
					date = marker.creationDate;
				}
				itemViewHolder.description.setText(app.getString(R.string.passed, OsmAndFormatter.getFormattedDate(app, date)));
			} else {
				itemViewHolder.point.setVisibility(View.GONE);
				itemViewHolder.description.setVisibility(View.GONE);
			}

			itemViewHolder.optionsBtn.setOnClickListener(view -> {
				int adapterPosition = itemViewHolder.getAdapterPosition();
				if (adapterPosition < 0) {
					return;
				}
				if (markerInHistory) {
					app.getMapMarkersHelper().restoreMarkerFromHistory(marker, 0);
				} else {
					app.getMapMarkersHelper().moveMapMarkerToHistory(marker);
				}
				updateDisplayedData();
				if (!markerInHistory) {
					snackbar = Snackbar.make(itemViewHolder.itemView, R.string.marker_moved_to_history, Snackbar.LENGTH_LONG)
							.setAction(R.string.shared_string_undo, v -> {
								app.getMapMarkersHelper().restoreMarkerFromHistory(marker, 0);
								updateDisplayedData();
							});
					UiUtilities.setupSnackbar(snackbar, nightMode);
					snackbar.show();
				}
			});
			itemViewHolder.iconReorder.setVisibility(View.GONE);
			itemViewHolder.flagIconLeftSpace.setVisibility(View.VISIBLE);
			boolean lastItem = position == getItemCount() - 1;
			if ((getItemCount() > position + 1 && getItemViewType(position + 1) == HEADER_TYPE) || lastItem) {
				itemViewHolder.divider.setVisibility(View.GONE);
			} else {
				itemViewHolder.divider.setVisibility(View.VISIBLE);
			}
			itemViewHolder.bottomShadow.setVisibility(lastItem ? View.VISIBLE : View.GONE);

			LatLon markerLatLon = new LatLon(marker.getLatitude(), marker.getLongitude());
			updateLocationViewCache.arrowResId = drawableResToUpdate;
			updateLocationViewCache.arrowColor = markerToHighlight ? color : 0;
			UpdateLocationUtils.updateLocationView(app, updateLocationViewCache, markerImageViewToUpdate, itemViewHolder.distance, markerLatLon);
		} else if (holder instanceof MapMarkerHeaderViewHolder) {
			MapMarkerHeaderViewHolder headerViewHolder = (MapMarkerHeaderViewHolder) holder;
			Object header = getItem(position);
			String headerString;
			if (header instanceof MarkerGroupItem) {
				headerViewHolder.icon.setVisibility(View.GONE);
				headerViewHolder.iconSpace.setVisibility(View.VISIBLE);
				headerString = ((MarkerGroupItem) header).getName(app);
				headerViewHolder.disableGroupSwitch.setVisibility(View.GONE);
				headerViewHolder.articleDescription.setVisibility(View.GONE);
			} else if (header instanceof GroupHeader) {
				GroupHeader groupHeader = (GroupHeader) header;
				MapMarkersGroup group = groupHeader.getGroup();
				String groupName = group.getName();
				if (groupName.isEmpty()) {
					groupName = app.getString(R.string.shared_string_favorites);
				} else if (group.getType() == ItineraryType.TRACK) {
					groupName = groupName.replace(IndexConstants.GPX_FILE_EXT, "").replace("/", " ").replace("_", " ");
				}
				if (group.isDisabled()) {
					headerString = groupName;
				} else {
					headerString = groupName + " \u2014 "
							+ group.getActiveMarkers().size()
							+ "/" + group.getMarkers().size();
				}
				headerViewHolder.icon.setVisibility(View.VISIBLE);
				headerViewHolder.iconSpace.setVisibility(View.GONE);
				headerViewHolder.icon.setImageDrawable(iconsCache.getIcon(groupHeader.getIconRes(), R.color.divider_color));
				boolean groupIsDisabled = group.isDisabled();
				headerViewHolder.disableGroupSwitch.setVisibility(View.VISIBLE);
				TravelArticle article = group.getWikivoyageArticle();
				if (article != null && !groupIsDisabled) {
					headerViewHolder.articleDescription.setVisibility(View.VISIBLE);
					View.OnClickListener openWikivoyageArticle = v -> {
						mapActivity.getSupportFragmentManager();
						WikivoyageArticleDialogFragment.showInstance(app, mapActivity.getSupportFragmentManager(), article.generateIdentifier(), article.getLang());
					};
					if (article.getContent().isEmpty()) {
						headerViewHolder.content.setVisibility(View.GONE);
					} else {
						headerViewHolder.content.setText(article.getContent());
						headerViewHolder.content.setOnClickListener(openWikivoyageArticle);
					}

					headerViewHolder.button.setText(R.string.shared_string_read);
					headerViewHolder.button.setOnClickListener(openWikivoyageArticle);
				} else {
					headerViewHolder.articleDescription.setVisibility(View.GONE);
				}
				CompoundButton.OnCheckedChangeListener checkedChangeListener = (compoundButton, enabled) -> {
					MapMarkersHelper mapMarkersHelper = app.getMapMarkersHelper();
					GpxFile[] gpxFile = new GpxFile[1];
					boolean disabled = !enabled;

					if (groupIsDisabled && !group.wasShown() && group.getWptCategories().size() > 1) {
						group.setWasShown(true);
						Bundle args = new Bundle();
						args.putString(SelectWptCategoriesBottomSheetDialogFragment.GPX_FILE_PATH_KEY, group.getGpxPath(app));
						args.putString(SelectWptCategoriesBottomSheetDialogFragment.ACTIVE_CATEGORIES_KEY, group.getWptCategoriesString());
						args.putBoolean(SelectWptCategoriesBottomSheetDialogFragment.UPDATE_CATEGORIES_KEY, true);

						SelectWptCategoriesBottomSheetDialogFragment fragment = new SelectWptCategoriesBottomSheetDialogFragment();
						fragment.setArguments(args);
						fragment.setUsedOnMap(false);
						fragment.show(mapActivity.getSupportFragmentManager(), SelectWptCategoriesBottomSheetDialogFragment.TAG);
					}
					mapMarkersHelper.updateGroupDisabled(group, disabled);
					if (group.getType() == ItineraryType.TRACK) {
						group.setVisibleUntilRestart(disabled);
						String gpxPath = group.getGpxPath(app);
						SelectedGpxFile selectedGpxFile = app.getSelectedGpxHelper().getSelectedFileByPath(gpxPath);
						if (selectedGpxFile != null) {
							gpxFile[0] = selectedGpxFile.getGpxFile();
						} else {
							// TODO IO load in another thread ?
							gpxFile[0] = SharedUtil.loadGpxFile(new File(gpxPath));
						}
						switchGpxVisibility(gpxFile[0], selectedGpxFile, !disabled);
					}
					if (!disabled) {
						mapMarkersHelper.enableGroup(group);
					} else {
						mapMarkersHelper.runSynchronization(group);
					}

					if (disabled) {
						snackbar = Snackbar.make(holder.itemView, app.getString(R.string.group_will_be_removed_after_restart), Snackbar.LENGTH_LONG)
								.setAction(R.string.shared_string_undo, view -> {
									if (group.getType() == ItineraryType.TRACK && gpxFile[0] != null) {
										switchGpxVisibility(gpxFile[0], null, true);
									}
									mapMarkersHelper.enableGroup(group);
								});
						UiUtilities.setupSnackbar(snackbar, nightMode);
						snackbar.show();
					}
				};
				headerViewHolder.disableGroupSwitch.setOnCheckedChangeListener(null);
				headerViewHolder.disableGroupSwitch.setChecked(!groupIsDisabled);
				headerViewHolder.disableGroupSwitch.setOnCheckedChangeListener(checkedChangeListener);
				UiUtilities.setupCompoundButton(headerViewHolder.disableGroupSwitch, nightMode, UiUtilities.CompoundButtonType.GLOBAL);
			} else {
				throw new IllegalArgumentException("Unsupported header");
			}
			headerViewHolder.title.setText(headerString);
			headerViewHolder.bottomShadow.setVisibility(position == getItemCount() - 1 ? View.VISIBLE : View.GONE);
		} else if (holder instanceof MapMarkersShowHideHistoryViewHolder) {
			MapMarkersShowHideHistoryViewHolder showHideHistoryViewHolder = (MapMarkersShowHideHistoryViewHolder) holder;
			ShowHideHistoryButton showHideHistoryButton = (ShowHideHistoryButton) getItem(position);
			boolean showHistory = showHideHistoryButton.showHistory;
			if (position == getItemCount() - 1) {
				showHideHistoryViewHolder.bottomShadow.setVisibility(View.VISIBLE);
			} else {
				showHideHistoryViewHolder.bottomShadow.setVisibility(View.GONE);
			}
			showHideHistoryViewHolder.title.setText(app.getString(showHistory ? R.string.hide_passed : R.string.show_passed));
			showHideHistoryViewHolder.itemView.setOnClickListener(view -> {
				showHideHistoryButton.showHistory = !showHistory;
				createDisplayGroups();
				notifyDataSetChanged();
			});
		} else if (holder instanceof MapMarkerCategoriesViewHolder) {
			MapMarkerCategoriesViewHolder categoriesViewHolder = (MapMarkerCategoriesViewHolder) holder;
			Object header = getItem(position);
			if (header instanceof CategoriesSubHeader) {
				CategoriesSubHeader categoriesSubHeader = (CategoriesSubHeader) header;
				MapMarkersGroup group = categoriesSubHeader.getGroup();
				View.OnClickListener openChooseCategoriesDialog = view -> {
					if (!group.getWptCategories().isEmpty()) {
						Bundle args = new Bundle();
						args.putString(SelectWptCategoriesBottomSheetDialogFragment.GPX_FILE_PATH_KEY, group.getGpxPath(app));
						args.putBoolean(SelectWptCategoriesBottomSheetDialogFragment.UPDATE_CATEGORIES_KEY, true);
						args.putStringArrayList(SelectWptCategoriesBottomSheetDialogFragment.ACTIVE_CATEGORIES_KEY, new ArrayList<String>(group.getWptCategories()));
						SelectWptCategoriesBottomSheetDialogFragment fragment = new SelectWptCategoriesBottomSheetDialogFragment();
						fragment.setArguments(args);
						fragment.setUsedOnMap(false);
						fragment.show(mapActivity.getSupportFragmentManager(), SelectWptCategoriesBottomSheetDialogFragment.TAG);
					} else {
						app.getMapMarkersHelper().addOrEnableGpxGroup(new File(group.getGpxPath(app)));
					}
				};
				categoriesViewHolder.title.setText(getGroupWptCategoriesString(group));
				categoriesViewHolder.divider.setVisibility(View.VISIBLE);
				categoriesViewHolder.button.setCompoundDrawablesWithIntrinsicBounds(
						null, null, app.getUIUtilities().getIcon(R.drawable.ic_action_filter_dark,
								nightMode ? R.color.active_color_primary_dark : R.color.active_color_primary_light), null);
				categoriesViewHolder.button.setOnClickListener(openChooseCategoriesDialog);
				categoriesViewHolder.title.setOnClickListener(openChooseCategoriesDialog);
			}
		}
	}

	private void switchGpxVisibility(@NonNull GpxFile gpxFile, @Nullable SelectedGpxFile selectedGpxFile, boolean visible) {
		GpxSelectionHelper gpxHelper = app.getSelectedGpxHelper();
		if (!visible && selectedGpxFile != null && selectedGpxFile.selectedByUser) {
			return;
		}
		GpxSelectionParams params = GpxSelectionParams.newInstance().saveSelection();
		if (visible) {
			params.showOnMap().selectedAutomatically();
		} else {
			params.hideFromMap();
		}
		gpxHelper.selectGpxFile(gpxFile, params);
	}

	public void hideSnackbar() {
		if (snackbar != null && snackbar.isShown()) {
			snackbar.dismiss();
		}
	}

	private String getGroupWptCategoriesString(MapMarkersGroup group) {
		StringBuilder sb = new StringBuilder();
		Set<String> categories = group.getWptCategories();
		if (categories != null && !categories.isEmpty()) {
			Iterator<String> it = categories.iterator();
			while (it.hasNext()) {
				String category = it.next();
				if (category.isEmpty()) {
					category = app.getString(R.string.shared_string_waypoints);
				}
				sb.append(category);
				if (it.hasNext()) {
					sb.append(", ");
				}
			}
		}
		return sb.toString();
	}

	@Override
	public int getItemViewType(int position) {
		Object item = items.get(position);
		if (item instanceof MapMarker) {
			return MARKER_TYPE;
		} else if (item instanceof GroupHeader || item instanceof MarkerGroupItem) {
			return HEADER_TYPE;
		} else if (item instanceof ShowHideHistoryButton) {
			return SHOW_HIDE_HISTORY_TYPE;
		} else if (item instanceof CategoriesSubHeader) {
			return CATEGORIES_TYPE;
		} else {
			throw new IllegalArgumentException("Unsupported view type");
		}
	}

	@Override
	public int getItemCount() {
		return items.size();
	}

	public Object getItem(int position) {
		return items.get(position);
	}


	public static class MarkerGroupItem {

		static final MarkerGroupItem TODAY_HEADER = new MarkerGroupItem(R.string.today);
		static final MarkerGroupItem YESTERDAY_HEADER = new MarkerGroupItem(R.string.yesterday);
		static final MarkerGroupItem LAST_SEVEN_DAYS_HEADER = new MarkerGroupItem(R.string.last_seven_days);
		static final MarkerGroupItem THIS_YEAR_HEADER = new MarkerGroupItem(R.string.this_year);

		@StringRes
		private int nameId;
		protected String name;

		public MarkerGroupItem(@StringRes int name) {
			this.nameId = name;
		}

		public MarkerGroupItem(String name) {
			this.name = name;
		}

		public String getName(OsmandApplication app) {
			if (name == null && nameId != 0) {
				name = app.getString(nameId);
			}
			return name;
		}
	}

	public static class CategoriesSubHeader {

		private final MapMarkersGroup group;

		public CategoriesSubHeader(MapMarkersGroup group) {
			this.group = group;
		}

		public MapMarkersGroup getGroup() {
			return group;
		}
	}


	public interface MapMarkersGroupsAdapterListener {

		void onItemClick(View view);
	}
}
