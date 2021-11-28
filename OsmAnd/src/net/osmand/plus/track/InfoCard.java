package net.osmand.plus.track;

import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import net.osmand.AndroidUtils;
import net.osmand.GPXUtilities;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.PlatformUtil;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.myplaces.AvailableGPXFragment.GpxInfo;
import net.osmand.plus.routepreparationmenu.cards.MapBaseCard;
import net.osmand.plus.widgets.TextViewEx;

import org.apache.commons.logging.Log;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class InfoCard extends MapBaseCard {
	private static final Log log = PlatformUtil.getLog(InfoCard.class);

	private final Fragment targetFragment;
	private final GPXFile gpxFile;

	public InfoCard(@NonNull MapActivity mapActivity,
	                       @NonNull Fragment targetFragment,
	                       @NonNull GPXFile gpxFile) {
		super(mapActivity);
		this.gpxFile = gpxFile;
		this.targetFragment = targetFragment;
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.gpx_info_card;
	}

	@Override
	protected void updateContent() {
		if (gpxFile.metadata == null) {
			gpxFile.metadata = new GPXUtilities.Metadata();
		}

		TextViewEx size = view.findViewById(R.id.size);
		TextViewEx createdOn = view.findViewById(R.id.created_on);
		TextViewEx location = view.findViewById(R.id.location);

		LinearLayout timeContainer = view.findViewById(R.id.timeContainer);
		if (gpxFile.metadata.time != 0) timeContainer.setVisibility(View.VISIBLE); // check if track create time is 0, to hide the createOn field

		GpxInfo gpxInfo = new GpxInfo();
		gpxInfo.gpx = gpxFile;
		gpxInfo.file = new File(gpxFile.path);

		String trackSize = AndroidUtils.formatSize(app, gpxInfo.getSize());
		String trackDate = new SimpleDateFormat("dd MMM yyyy", Locale.US).format(gpxFile.metadata.time);
		String directory = gpxInfo.file.getParentFile().getName();
		String trackLocation = directory.substring(0,1).toUpperCase().concat(directory.substring(1));

		size.setText(trackSize);
		createdOn.setText(trackDate);
		location.setText(trackLocation);
	}
}
