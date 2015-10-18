package net.osmand.plus.download.ui;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.SearchView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.download.DownloadActivity;

public class SearchDialogFragment extends DialogFragment {

	public static final String TAG = "SearchDialogFragment";
	private static final String SEARCH_TEXT_DLG_KEY = "search_text_dlg_key";
	private String searchText;
	SearchView search;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		boolean isLightTheme = getMyApplication().getSettings().OSMAND_THEME.get() == OsmandSettings.OSMAND_LIGHT_THEME;
		int themeId = isLightTheme ? R.style.OsmandLightTheme : R.style.OsmandDarkTheme;
		setStyle(STYLE_NO_FRAME, themeId);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		final View view = inflater.inflate(R.layout.maps_in_category_fragment, container, false);

		if (savedInstanceState != null) {
			searchText = savedInstanceState.getString(SEARCH_TEXT_DLG_KEY);
		}
		if (searchText == null) {
			searchText = getArguments().getString(SEARCH_TEXT_DLG_KEY);
		}
		if (searchText == null)
			searchText = "";

		Toolbar toolbar = (Toolbar) view.findViewById(R.id.toolbar);
		toolbar.setNavigationIcon(getMyApplication().getIconsCache().getIcon(R.drawable.abc_ic_ab_back_mtrl_am_alpha));
		toolbar.setNavigationOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dismiss();
			}
		});

		search = new SearchView(getActivity());
		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
				LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		params.setMargins(0, 0, 0, 0);
		search.setLayoutParams(params);
		toolbar.addView(search);

		search.setOnCloseListener(new SearchView.OnCloseListener() {
			@Override
			public boolean onClose() {
				if (search.getQuery().length() == 0) {
					dismiss();
					return true;
				}
				return false;
			}
		});
		
		search.setOnQueryTextFocusChangeListener(new View.OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
			}
		});

		search.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
			@Override
			public boolean onQueryTextSubmit(String query) {
				return false;
			}

			@Override
			public boolean onQueryTextChange(String newText) {
				Fragment f = getChildFragmentManager().findFragmentByTag(SearchItemsFragment.TAG);
				if (f != null) {
					((SearchItemsFragment) f).updateSearchText(newText);
					return true;
				}
				return false;
			}
		});

		Fragment fragment = getChildFragmentManager().findFragmentById(R.id.fragmentContainer);
		if (fragment == null) {
			getChildFragmentManager().beginTransaction().add(R.id.fragmentContainer,
					SearchItemsFragment.createInstance(searchText), SearchItemsFragment.TAG).commit();
		}

		getDownloadActivity().registerFreeVersionBanner(view);
		return view;
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putString(SEARCH_TEXT_DLG_KEY, searchText);
		super.onSaveInstanceState(outState);
	}

	@Override
	public void onResume() {
		super.onResume();
		search.setIconified(false);
	}

	private OsmandApplication getMyApplication() {
		return (OsmandApplication) getActivity().getApplication();
	}

	private DownloadActivity getDownloadActivity() {
		return (DownloadActivity) getActivity();
	}

	public void newDownloadIndexes() {
		Fragment f = getChildFragmentManager().findFragmentByTag(SearchItemsFragment.TAG);
		if (f != null) {
			((SearchItemsFragment) f).onCategorizationFinished();
		}
	}

	public static SearchDialogFragment createInstance(String searchText) {
		Bundle bundle = new Bundle();
		bundle.putString(SEARCH_TEXT_DLG_KEY, searchText);
		SearchDialogFragment fragment = new SearchDialogFragment();
		fragment.setArguments(bundle);
		return fragment;
	}
}
