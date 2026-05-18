package net.osmand.plus.download.ui;

import static net.osmand.data.PointDescription.POINT_TYPE_WORLD_REGION_SHOW_ON_MAP;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BottomSheetDialogFragment;
import net.osmand.plus.download.DownloadActivity;
import net.osmand.plus.utils.AndroidUtils;

public class GoToMapFragment extends BottomSheetDialogFragment {

	public static final String TAG = GoToMapFragment.class.getSimpleName();

	private static final String REGION_NAME_KEY = "region_name_key";
	private static final String REGION_CENTER_KEY = "region_center_key";

	private String regionName;
	private LatLon regionCenter;

	public void setRegionName(String regionName) {
		this.regionName = regionName;
	}

	public void setRegionCenter(LatLon regionCenter) {
		this.regionCenter = regionCenter;
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (savedInstanceState != null) {
			regionName = savedInstanceState.getString(REGION_NAME_KEY, "");
			regionCenter = AndroidUtils.getSerializable(savedInstanceState, REGION_CENTER_KEY, LatLon.class);
			if (regionCenter == null) {
				regionCenter = new LatLon(0, 0);
			}
		}
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.go_to_map_fragment, container, false);

		ImageView icon = view.findViewById(R.id.titleIconImageView);
		icon.setImageDrawable(getIcon(R.drawable.ic_map, R.color.osmand_orange));

		TextView description = view.findViewById(R.id.descriptionTextView);
		description.setText(getString(R.string.map_downloaded_descr, regionName));

		ImageButton closeButton = view.findViewById(R.id.closeImageButton);
		closeButton.setImageDrawable(getContentIcon(R.drawable.ic_action_remove_dark));
		closeButton.setOnClickListener(v -> {
			DownloadActivity activity = (DownloadActivity) getActivity();
			if (activity != null) {
				activity.setDownloadItem(null, null);
			}
			dismiss();
		});

		view.findViewById(R.id.actionButton).setOnClickListener(v -> {
			FragmentActivity activity = getActivity();
			if (activity != null && regionCenter != null) {
				OsmandApplication app = requiredMyApplication();
				app.getSettings().setMapLocationToShow(
						regionCenter.getLatitude(),
						regionCenter.getLongitude(),
						5,
						new PointDescription(POINT_TYPE_WORLD_REGION_SHOW_ON_MAP, ""));

				dismiss();
				MapActivity.launchMapActivityMoveToTop(activity);
			}
		});

		return view;
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		outState.putString(REGION_NAME_KEY, regionName);
		outState.putSerializable(REGION_CENTER_KEY, regionCenter);
	}
}
