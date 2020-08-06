package net.osmand.plus.routepreparationmenu.cards;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.AndroidUtils;
import net.osmand.CallbackWithObject;
import net.osmand.IndexConstants;
import net.osmand.plus.GPXDatabase.GpxDataItem;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.GpxUiHelper;
import net.osmand.plus.helpers.GpxUiHelper.GPXInfo;
import net.osmand.plus.mapcontextmenu.other.HorizontalSelectionAdapter;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SelectTrackToFollowCard extends BaseCard {

	private Map<String, List<GPXInfo>> data;

	private List<GPXInfo> gpxInfoList;
	private String selectedCategory;
	private CallbackWithObject<GPXInfo> gpxInfoCallback;

	public SelectTrackToFollowCard(MapActivity mapActivity, List<GPXInfo> gpxInfoList) {
		super(mapActivity);
		this.gpxInfoList = gpxInfoList;
		data = getGpxInfoCategories();
	}

	public void setGpxInfoCallback(CallbackWithObject<GPXInfo> gpxInfoCallback) {
		this.gpxInfoCallback = gpxInfoCallback;
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.follow_track_card;
	}

	@Override
	protected void updateContent() {
		setupCategoriesRow();
		setupTracksItems();
	}

	private void setupTracksItems() {
		LinearLayout tracks = view.findViewById(R.id.items);
		tracks.removeAllViews();

		final List<GPXInfo> infoItems = data.get(selectedCategory);
		if (!Algorithms.isEmpty(infoItems)) {
			int minCardHeight = app.getResources().getDimensionPixelSize(R.dimen.route_info_card_row_min_height);
			int contentPadding = app.getResources().getDimensionPixelSize(R.dimen.content_padding);

			LayoutInflater inflater = UiUtilities.getInflater(mapActivity, nightMode);
			for (int i = 0; i < infoItems.size(); i++) {
				final GPXInfo item = infoItems.get(i);
				View trackView = inflater.inflate(R.layout.gpx_track_item, tracks, false);

				String fileName = Algorithms.getFileWithoutDirs(item.getFileName());
				String title = GpxUiHelper.getGpxTitle(fileName);
				GpxDataItem dataItem = getDataItem(item);
				GpxUiHelper.updateGpxInfoView(trackView, title, item, dataItem, false, app);

				ImageView icon = trackView.findViewById(R.id.icon);
				icon.setImageDrawable(getContentIcon(R.drawable.ic_action_polygom_dark));
				icon.setVisibility(View.VISIBLE);

				LinearLayout container = trackView.findViewById(R.id.container);
				container.setMinimumHeight(minCardHeight);
				AndroidUtils.setPadding(container, contentPadding, 0, 0, 0);
				trackView.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						if (gpxInfoCallback != null) {
							gpxInfoCallback.processResult(item);
						}
					}
				});
				tracks.addView(trackView);
			}
		}
	}

	private GpxDataItem getDataItem(GPXInfo info) {
		return app.getGpxDbHelper().getItem(new File(app.getAppPath(IndexConstants.GPX_INDEX_DIR), info.getFileName()));
	}

	private void setupCategoriesRow() {
		final HorizontalSelectionAdapter selectionAdapter = new HorizontalSelectionAdapter(app, nightMode);
		selectionAdapter.setItems(new ArrayList<>(data.keySet()));
		selectionAdapter.setSelectedItem(selectedCategory);
		selectionAdapter.setListener(new HorizontalSelectionAdapter.HorizontalSelectionAdapterListener() {
			@Override
			public void onItemSelected(String item) {
				setSelectedCategory(item);
				selectionAdapter.notifyDataSetChanged();
			}
		});

		RecyclerView iconCategoriesRecyclerView = view.findViewById(R.id.group_name_recycler_view);
		iconCategoriesRecyclerView.setAdapter(selectionAdapter);
		iconCategoriesRecyclerView.setLayoutManager(new LinearLayoutManager(app, RecyclerView.HORIZONTAL, false));
		selectionAdapter.notifyDataSetChanged();
	}

	private void setSelectedCategory(String category) {
		selectedCategory = category;
		setupTracksItems();
	}

	private Map<String, List<GPXInfo>> getGpxInfoCategories() {
		String all = app.getString(R.string.shared_string_all);
		String visible = app.getString(R.string.shared_string_visible);
		Map<String, List<GPXInfo>> gpxInfoCategories = new LinkedHashMap<>();
		for (GPXInfo info : gpxInfoList) {
			if (info.isSelected()) {
				addGpxInfoCategory(gpxInfoCategories, info, visible);
			}
			if (!Algorithms.isEmpty(info.getFileName())) {
				File file = new File(info.getFileName());
				String dirName = file.getParent();
				if (dirName != null && !IndexConstants.GPX_INDEX_DIR.equals(dirName)) {
					addGpxInfoCategory(gpxInfoCategories, info, dirName);
				}
			}
			addGpxInfoCategory(gpxInfoCategories, info, all);
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
}