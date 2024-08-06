package net.osmand.plus.track.cards;

import static net.osmand.IndexConstants.GPX_INDEX_DIR;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.routepreparationmenu.cards.MapBaseCard;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.shared.gpx.GpxFile;
import net.osmand.util.Algorithms;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class GpxInfoCard extends MapBaseCard {

	private final GpxFile gpxFile;
	private final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("d MMM yyyy", Locale.getDefault());

	public GpxInfoCard(@NonNull MapActivity mapActivity, @NonNull GpxFile gpxFile) {
		super(mapActivity);
		this.gpxFile = gpxFile;
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.gpx_info_card;
	}

	@Override
	public void updateContent() {
		if (gpxFile.isShowCurrentTrack()) {
			updateVisibility(false);
			return;
		}
		updateVisibility(true);

		TextView header = view.findViewById(R.id.header);
		header.setText(R.string.general_settings);

		ViewGroup container = view.findViewById(R.id.items_container);
		container.removeAllViews();

		if (gpxFile.getMetadata().getTime() > 0) {
			String trackDate = DATE_FORMAT.format(gpxFile.getMetadata().getTime());
			createItemRow(container, R.string.created_on, R.drawable.ic_action_data, trackDate);
		}

		File file = new File(gpxFile.getPath());
		String trackSize = AndroidUtils.formatSize(app, file.length());
		createItemRow(container, R.string.shared_string_size, R.drawable.ic_sdcard, trackSize);

		String dirName = "";
		File dir = file.getParentFile();
		if (dir != null) {
			dirName = Algorithms.objectEquals(dir, app.getAppPath(GPX_INDEX_DIR)) ? getString(R.string.shared_string_tracks) : Algorithms.capitalizeFirstLetter(dir.getName());
		}
		createItemRow(container, R.string.shared_string_location, R.drawable.ic_action_folder, dirName);

		if (!Algorithms.isEmpty(gpxFile.getMetadata().getDesc())) {
			createItemRow(container, R.string.shared_string_description, R.drawable.ic_action_description, gpxFile.getMetadata().getDesc());
		}
	}

	@NonNull
	private View createItemRow(@NonNull ViewGroup container, @StringRes int titleId, @DrawableRes int iconId, @Nullable String text) {
		View view = themedInflater.inflate(R.layout.item_with_title_desc, container, false);
		container.addView(view);

		ImageView icon = view.findViewById(R.id.icon);
		TextView title = view.findViewById(R.id.title);
		TextView description = view.findViewById(R.id.description);

		title.setText(titleId);
		icon.setImageResource(iconId);
		description.setText(text);

		return view;
	}
}