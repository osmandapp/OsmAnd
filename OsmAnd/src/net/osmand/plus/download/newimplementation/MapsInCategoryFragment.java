package net.osmand.plus.download.newimplementation;

import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.download.DownloadActivity;

import org.apache.commons.logging.Log;

public class MapsInCategoryFragment extends DialogFragment {
	private static final Log LOG = PlatformUtil.getLog(IndexItemCategoryWithSubcat.class);
	public static final String TAG = "MapsInCategoryFragment";
	private static final String CATEGORY = "category";
	private MapDownloadListener mProgressListener;

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

		IndexItemCategoryWithSubcat category = getArguments().getParcelable(CATEGORY);
		assert category != null;
		getChildFragmentManager().beginTransaction().add(R.id.fragmentContainer,
				SubcategoriesFragment.createInstance(category)).commit();


		Toolbar toolbar = (Toolbar) view.findViewById(R.id.toolbar);
		toolbar.setTitle(category.getName());
		toolbar.setNavigationIcon(R.drawable.abc_ic_ab_back_mtrl_am_alpha);
		toolbar.setNavigationOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dismiss();
			}
		});

		DownloadsUiInitHelper.initFreeVersionBanner(view,
				getMyActivity().getMyApplication().getSettings(), getResources());
		mProgressListener = new MapDownloadListener(view, getResources()){
			@Override
			public void onFinished() {
				super.onFinished();
				DownloadsUiInitHelper.initFreeVersionBanner(view,
						getMyActivity().getMyApplication().getSettings(), getResources());
			}
		};
		view.findViewById(R.id.downloadProgressLayout).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {

			}
		});
		return view;
	}

	@Override
	public void onResume() {
		super.onResume();
		getMyActivity().setOnProgressUpdateListener(mProgressListener);
	}

	@Override
	public void onPause() {
		super.onPause();
		getMyActivity().setOnProgressUpdateListener(null);
	}

	private DownloadActivity getMyActivity() {
		return (DownloadActivity) getActivity();
	}

	public void onCategorySelected(@NonNull IndexItemCategoryWithSubcat category) {
		LOG.debug("onCategorySelected()");
		createInstance(category).show(getChildFragmentManager(), TAG);
	}

	public static MapsInCategoryFragment createInstance(
			@NonNull IndexItemCategoryWithSubcat category) {
		Bundle bundle = new Bundle();
		bundle.putParcelable(CATEGORY, category);
		MapsInCategoryFragment fragment = new MapsInCategoryFragment();
		fragment.setArguments(bundle);
		return fragment;
	}

	private static class MapDownloadListener implements DownloadActivity.OnProgressUpdateListener {
		private final View freeVersionBanner;
		private final View downloadProgressLayout;
		private final ProgressBar progressBar;
		private final TextView leftTextView;
		private final TextView rightTextView;
		private final Resources resources;

		MapDownloadListener(View view, Resources resources) {
			this.resources = resources;
			freeVersionBanner = view.findViewById(R.id.freeVersionBanner);
			downloadProgressLayout = view.findViewById(R.id.downloadProgressLayout);
			progressBar = (ProgressBar) view.findViewById(R.id.progressBar);
			leftTextView = (TextView) view.findViewById(R.id.leftTextView);
			rightTextView = (TextView) view.findViewById(R.id.rightTextView);
		}
		@Override
		public void onProgressUpdate(int progressPercentage, int activeTasks) {
			if (freeVersionBanner.getVisibility() == View.VISIBLE) {
				freeVersionBanner.setVisibility(View.GONE);
				downloadProgressLayout.setVisibility(View.VISIBLE);
			}
			progressBar.setProgress(progressPercentage);
			final String format = resources.getString(R.string.downloading_number_of_fiels);
			String numberOfTasks = String.format(format, activeTasks);
			leftTextView.setText(numberOfTasks);
			rightTextView.setText(progressPercentage + "%");
		}

		@Override
		public void onFinished() {
			freeVersionBanner.setVisibility(View.VISIBLE);
			downloadProgressLayout.setVisibility(View.GONE);
		}
	}
}
