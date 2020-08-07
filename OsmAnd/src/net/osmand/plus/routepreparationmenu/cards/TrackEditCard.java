package net.osmand.plus.routepreparationmenu.cards;

import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;

import net.osmand.AndroidUtils;
import net.osmand.IndexConstants;
import net.osmand.plus.GPXDatabase.GpxDataItem;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.GpxUiHelper;
import net.osmand.plus.helpers.GpxUiHelper.GPXInfo;
import net.osmand.util.Algorithms;

import java.io.File;

public class TrackEditCard extends BaseCard {

	private GPXInfo gpxInfo;

	public TrackEditCard(MapActivity mapActivity, GPXInfo gpxInfo) {
		super(mapActivity);
		this.gpxInfo = gpxInfo;
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.gpx_track_item;
	}

	private GpxDataItem getDataItem(GpxUiHelper.GPXInfo info) {
		return app.getGpxDbHelper().getItem(new File(app.getAppPath(IndexConstants.GPX_INDEX_DIR), info.getFileName()));
	}

	@Override
	protected void updateContent() {
		int minCardHeight = app.getResources().getDimensionPixelSize(R.dimen.setting_list_item_large_height);
		int listContentPadding = app.getResources().getDimensionPixelSize(R.dimen.list_content_padding);

		String fileName = Algorithms.getFileWithoutDirs(gpxInfo.getFileName());
		String title = GpxUiHelper.getGpxTitle(fileName);
		GpxDataItem dataItem = getDataItem(gpxInfo);
		GpxUiHelper.updateGpxInfoView(view, title, gpxInfo, dataItem, false, app);

		ImageView trackIcon = view.findViewById(R.id.icon);
		trackIcon.setImageDrawable(getActiveIcon(R.drawable.ic_action_polygom_dark));
		trackIcon.setVisibility(View.VISIBLE);

		ImageButton editButton = view.findViewById(R.id.show_on_map);
		editButton.setVisibility(View.VISIBLE);
		editButton.setImageDrawable(getContentIcon(R.drawable.ic_action_edit_dark));
		editButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				CardListener listener = getListener();
				if (listener != null) {
					listener.onCardPressed(TrackEditCard.this);
				}
			}
		});

		LinearLayout container = view.findViewById(R.id.container);
		container.setMinimumHeight(minCardHeight);
		AndroidUtils.setPadding(container, listContentPadding, 0, 0, 0);

		int activeColor = getActiveColor();
		int bgColor = UiUtilities.getColorWithAlpha(activeColor, 0.1f);
		view.setBackgroundDrawable(new ColorDrawable(bgColor));
	}
}