package net.osmand.plus.routepreparationmenu.cards;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewTreeObserver;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.Collator;
import net.osmand.IndexConstants;
import net.osmand.OsmAndCollator;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.track.helpers.GPXInfo;
import net.osmand.plus.routepreparationmenu.FollowTrackFragment;
import net.osmand.plus.settings.enums.TracksSortByMode;
import net.osmand.plus.track.GpxTrackAdapter;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.chips.ChipItem;
import net.osmand.plus.widgets.chips.HorizontalChipsView;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TracksToFollowCard extends MapBaseCard {

	private final Fragment target;

	private final List<GPXInfo> gpxInfoList;
	private Map<String, List<GPXInfo>> gpxInfoCategories;

	private GpxTrackAdapter tracksAdapter;
	private TracksSortByMode sortByMode = TracksSortByMode.BY_DATE;

	private final String defaultCategory;
	private final String visibleCategory;
	private String selectedCategory;

	public TracksToFollowCard(@NonNull MapActivity mapActivity, @NonNull Fragment target, @NonNull List<GPXInfo> gpxInfoList, @NonNull String selectedCategory) {
		super(mapActivity);
		this.target = target;
		this.gpxInfoList = gpxInfoList;
		this.selectedCategory = selectedCategory;
		defaultCategory = app.getString(R.string.shared_string_all);
		visibleCategory = app.getString(R.string.shared_string_visible);
		gpxInfoCategories = getGpxInfoCategories();
	}

	public void setSortByMode(TracksSortByMode sortByMode) {
		this.sortByMode = sortByMode;
		gpxInfoCategories = getGpxInfoCategories();
		updateTracksAdapter();
	}

	public List<GPXInfo> getGpxInfoList() {
		return gpxInfoList;
	}

	public String getSelectedCategory() {
		return selectedCategory;
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.follow_track_card;
	}

	@Override
	protected void updateContent() {
		setupTracksItems();
	}

	private void setupTracksItems() {
		RecyclerView filesRecyclerView = view.findViewById(R.id.track_list);
		filesRecyclerView.setLayoutManager(new LinearLayoutManager(view.getContext()));
		filesRecyclerView.setNestedScrollingEnabled(false);
		filesRecyclerView.getViewTreeObserver().addOnScrollChangedListener(new ViewTreeObserver.OnScrollChangedListener() {
			@Override
			public void onScrollChanged() {
				if (target instanceof FollowTrackFragment) {
					boolean scrollToBottomAvailable = filesRecyclerView.canScrollVertically(1);
					FollowTrackFragment followTrackFragment = (FollowTrackFragment) target;
					if (scrollToBottomAvailable) {
						followTrackFragment.showShadowButton();
					} else {
						followTrackFragment.hideShadowButton();
					}
				}
			}
		});

		tracksAdapter = new GpxTrackAdapter(view.getContext(), gpxInfoList, false, showFoldersName());
		tracksAdapter.setAdapterListener(new GpxTrackAdapter.OnItemClickListener() {
			@Override
			public void onItemClick(int position) {
				if (position != RecyclerView.NO_POSITION) {
					GPXInfo gpxInfo = tracksAdapter.getGpxInfoList().get(position);
					int index = gpxInfoList.indexOf(gpxInfo);
					notifyButtonPressed(index);
				}
			}
		});
		tracksAdapter.setCategoriesChipView(createCategoriesChipsView());
		filesRecyclerView.setAdapter(tracksAdapter);
	}

	private HorizontalChipsView createCategoriesChipsView() {
		LayoutInflater inflater = UiUtilities.getInflater(activity, nightMode);
		View view = inflater.inflate(R.layout.gpx_track_select_category_item, null, false);
		HorizontalChipsView chipsView = view.findViewById(R.id.track_categories);

		List<ChipItem> items = new ArrayList<>();
		for (String title : gpxInfoCategories.keySet()) {
			ChipItem item = new ChipItem(title);
			item.title = title;
			items.add(item);
		}
		chipsView.setItems(items);

		ChipItem selected = chipsView.getChipById(selectedCategory);
		chipsView.setSelected(selected);

		chipsView.setOnSelectChipListener(chip -> {
			chipsView.smoothScrollTo(chip);
			selectedCategory = chip.title;
			tracksAdapter.setShowFolderName(showFoldersName());
			updateTracksAdapter();
			return true;
		});
		return chipsView;
	}

	private void updateTracksAdapter() {
		List<GPXInfo> items = gpxInfoCategories.get(selectedCategory);
		tracksAdapter.setGpxInfoList(items != null ? items : new ArrayList<GPXInfo>());
		tracksAdapter.notifyDataSetChanged();
	}

	private boolean showFoldersName() {
		return defaultCategory.equals(selectedCategory) || visibleCategory.equals(selectedCategory);
	}

	private Map<String, List<GPXInfo>> getGpxInfoCategories() {
		Map<String, List<GPXInfo>> gpxInfoCategories = new LinkedHashMap<>();

		gpxInfoCategories.put(visibleCategory, new ArrayList<GPXInfo>());
		gpxInfoCategories.put(defaultCategory, new ArrayList<GPXInfo>());

		sortGPXInfoItems(gpxInfoList);
		for (GPXInfo info : gpxInfoList) {
			if (info.isSelected()) {
				addGpxInfoCategory(gpxInfoCategories, info, visibleCategory);
			}
			if (!Algorithms.isEmpty(info.getFileName())) {
				File file = new File(info.getFileName());
				String dirName = file.getParent();
				if (dirName != null && !IndexConstants.GPX_INDEX_DIR.equals(dirName)) {
					addGpxInfoCategory(gpxInfoCategories, info, dirName);
				}
			}
			addGpxInfoCategory(gpxInfoCategories, info, defaultCategory);
		}

		return gpxInfoCategories;
	}

	private void addGpxInfoCategory(Map<String, List<GPXInfo>> data, GPXInfo info, String category) {
		List<GPXInfo> items = data.get(category);
		if (items == null) {
			items = new ArrayList<>();
			data.put(category, items);
		}
		items.add(info);
	}

	public void sortGPXInfoItems(List<GPXInfo> gpxInfoList) {
		Collator collator = OsmAndCollator.primaryCollator();
		Collections.sort(gpxInfoList, new Comparator<GPXInfo>() {
			@Override
			public int compare(GPXInfo i1, GPXInfo i2) {
				if (sortByMode == TracksSortByMode.BY_NAME_ASCENDING) {
					return collator.compare(i1.getFileName(), i2.getFileName());
				} else if (sortByMode == TracksSortByMode.BY_NAME_DESCENDING) {
					return -collator.compare(i1.getFileName(), i2.getFileName());
				} else {
					long time1 = i1.getLastModified();
					long time2 = i2.getLastModified();
					if (time1 == time2) {
						return collator.compare(i1.getFileName(), i2.getFileName());
					}
					return -((time1 < time2) ? -1 : ((time1 == time2) ? 0 : 1));
				}
			}
		});
	}
}