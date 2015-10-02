package net.osmand.plus.download.newimplementation;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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

		DownloadsUiHelper.initFreeVersionBanner(view,
				getMyActivity().getMyApplication().getSettings(), getResources());
		mProgressListener = new DownloadsUiHelper.MapDownloadListener(view, getResources()){
			@Override
			public void onFinished() {
				super.onFinished();
				DownloadsUiHelper.initFreeVersionBanner(view,
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


}
