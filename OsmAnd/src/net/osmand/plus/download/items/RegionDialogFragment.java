package net.osmand.plus.download.items;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.WorldRegion;
import net.osmand.plus.download.DownloadActivity;
import net.osmand.plus.download.newimplementation.DownloadsUiHelper;

import org.apache.commons.logging.Log;

public class RegionDialogFragment extends DialogFragment {
	private static final Log LOG = PlatformUtil.getLog(RegionDialogFragment.class);
	public static final String TAG = "RegionDialogFragment";
	private static final String REGION_DLG_KEY = "world_region_dialog_key";
	private WorldRegion region;
	private DownloadsUiHelper.MapDownloadListener mProgressListener;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		boolean isLightTheme = ((OsmandApplication) getActivity().getApplication())
				.getSettings().OSMAND_THEME.get() == OsmandSettings.OSMAND_LIGHT_THEME;
		int themeId = isLightTheme ? R.style.OsmandLightTheme : R.style.OsmandDarkTheme;
		setStyle(STYLE_NO_FRAME, themeId);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		final View view = inflater.inflate(R.layout.maps_in_category_fragment, container, false);

		WorldRegion region = null;
		if (savedInstanceState != null) {
			Object regionObj = savedInstanceState.getSerializable(REGION_DLG_KEY);
			if (regionObj != null) {
				region = (WorldRegion)regionObj;
			}
		}
		if (region == null) {
			Object regionObj = getArguments().getSerializable(REGION_DLG_KEY);
			if (regionObj != null) {
				region = (WorldRegion)regionObj;
			}
		}

		this.region = region;

		Toolbar toolbar = (Toolbar) view.findViewById(R.id.toolbar);
		toolbar.setNavigationIcon(getMyApplication().getIconsCache().getIcon(R.drawable.abc_ic_ab_back_mtrl_am_alpha));
		toolbar.setNavigationOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dismiss();
			}
		});

		if (this.region != null) {
			Fragment fragment = getChildFragmentManager().findFragmentById(R.id.fragmentContainer);
			if (fragment == null) {
				getChildFragmentManager().beginTransaction().add(R.id.fragmentContainer,
						RegionItemsFragment.createInstance(region)).commit();
			}
			toolbar.setTitle(this.region.getName());
		}
		DownloadsUiHelper.initFreeVersionBanner(view, getMyApplication(),
				getResources());

		mProgressListener = new DownloadsUiHelper.MapDownloadListener(view, getResources()){
			@Override
			public void onProgressUpdate(int progressPercentage, int activeTasks) {
				super.onProgressUpdate(progressPercentage, activeTasks);
			}

			@Override
			public void onFinished() {
				super.onFinished();
				DownloadsUiHelper.initFreeVersionBanner(view,
						getMyApplication(), getResources());
			}
		};

		return view;
	}

	@Override
	public void onResume() {
		super.onResume();
		LOG.debug(region.getName() + " onResume()");
		getMyActivity().setOnProgressUpdateListener(mProgressListener);
	}

	@Override
	public void onPause() {
		super.onPause();
		getMyActivity().setOnProgressUpdateListener(null);
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putSerializable(REGION_DLG_KEY, region);
		super.onSaveInstanceState(outState);
	}

	private OsmandApplication getMyApplication() {
		return (OsmandApplication) getActivity().getApplication();
	}

	private DownloadActivity getMyActivity() {
		return (DownloadActivity) getActivity();
	}

	public void onRegionSelected(WorldRegion region) {
		DownloadsUiHelper.showDialog(getActivity(), createInstance(region));
	}

	public static RegionDialogFragment createInstance(WorldRegion region) {
		Bundle bundle = new Bundle();
		bundle.putSerializable(REGION_DLG_KEY, region);
		RegionDialogFragment fragment = new RegionDialogFragment();
		fragment.setArguments(bundle);
		return fragment;
	}
}
