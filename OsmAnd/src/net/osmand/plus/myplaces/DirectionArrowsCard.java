package net.osmand.plus.myplaces;

import android.view.View;
import android.widget.CompoundButton;
import android.widget.TextView;

import androidx.annotation.NonNull;

import net.osmand.GPXUtilities.GPXFile;
import net.osmand.plus.GPXDatabase.GpxDataItem;
import net.osmand.plus.GpxSelectionHelper.SelectedGpxFile;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.routepreparationmenu.cards.BaseCard;

import java.io.File;

public class DirectionArrowsCard extends BaseCard {

	private SelectedGpxFile selectedGpxFile;

	public DirectionArrowsCard(@NonNull MapActivity mapActivity, @NonNull SelectedGpxFile selectedGpxFile) {
		super(mapActivity);
		this.selectedGpxFile = selectedGpxFile;
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.bottom_sheet_item_with_switch;
	}

	@Override
	protected void updateContent() {
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.icon), false);

		TextView titleView = view.findViewById(R.id.title);
		titleView.setText(R.string.gpx_direction_arrows);

		final CompoundButton compoundButton = view.findViewById(R.id.compound_button);
		compoundButton.setChecked(selectedGpxFile.getGpxFile().isShowStartFinish());

		view.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				boolean checked = !compoundButton.isChecked();
				compoundButton.setChecked(checked);
				setShowArrows(checked);
			}
		});
	}

	private void setShowArrows(boolean showArrows) {
		if (selectedGpxFile.getGpxFile() != null) {
			GPXFile gpxFile = selectedGpxFile.getGpxFile();
			gpxFile.setShowArrows(showArrows);
			GpxDataItem gpxDataItem = app.getGpxDbHelper().getItem(new File(gpxFile.path));
			if (gpxDataItem != null) {
				app.getGpxDbHelper().updateShowArrows(gpxDataItem, showArrows);
			}
			mapActivity.refreshMap();
		}
	}
}