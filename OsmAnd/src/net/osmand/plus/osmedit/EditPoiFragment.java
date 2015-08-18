package net.osmand.plus.osmedit;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import net.osmand.data.Amenity;
import net.osmand.osm.edit.Node;
import net.osmand.plus.R;

import java.io.Serializable;
import java.util.LinkedHashSet;

public class EditPoiFragment extends Fragment {
	public static final String TAG = "EditPoiFragment";

	private static final String KEY_AMENITY_NODE = "amenity_node";
	private static final String KEY_AMENITY = "amenity";
	private static final String TAGS_LIST = "tags_list";

	private final EditPoiData editPoiData = new EditPoiData();
	private ViewPager viewPager;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
//		poiTypes = ((OsmandApplication) activity.getApplication()).getPoiTypes();
//		allTranslatedSubTypes = poiTypes.getAllTranslatedNames();
//		OsmandSettings settings = ((OsmandApplication) activity.getApplication()).getSettings();
//		editPoiData.isLocalEdit = true;
//		if (settings.OFFLINE_EDITION.get() || !settings.isInternetConnectionAvailable(true)) {
//			openstreetmapUtil = new OpenstreetmapLocalUtil(plugin, activity);
//			openstreetmapUtilToLoad = openstreetmapUtil;
//		} else if(!settings.isInternetConnectionAvailable(true)) {
//			openstreetmapUtil = new OpenstreetmapLocalUtil(plugin, activity);
//			openstreetmapUtilToLoad = new OpenstreetmapRemoteUtil(activity);
//		} else {
//			editPoiData.isLocalEdit = false;
//			openstreetmapUtil = new OpenstreetmapRemoteUtil(activity);
//			openstreetmapUtilToLoad = openstreetmapUtil;
//		}

//		editPoiData.node = (Node) getArguments().getSerializable(KEY_AMENITY_NODE);
//		editPoiData.tags = new LinkedHashSet<>();
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		if (savedInstanceState != null) {
			editPoiData.tags = (LinkedHashSet<Tag>) savedInstanceState.getSerializable(TAGS_LIST);
		} else {
			editPoiData.tags = new LinkedHashSet<>();
		}

		View view = inflater.inflate(R.layout.fragment_edit_poi, container, false);

		Toolbar toolbar = (Toolbar) view.findViewById(R.id.toolbar);
		toolbar.setTitle(R.string.poi_create_title);
		toolbar.setNavigationIcon(R.drawable.abc_ic_ab_back_mtrl_am_alpha);
		toolbar.setNavigationOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
				fragmentManager.beginTransaction().remove(EditPoiFragment.this).commit();
				fragmentManager.popBackStack();
			}
		});

		viewPager = (ViewPager) view.findViewById(R.id.viewpager);
		MyAdapter pagerAdapter = new MyAdapter(getChildFragmentManager());
		viewPager.setAdapter(pagerAdapter);

		final TabLayout tabLayout = (TabLayout) view.findViewById(R.id.tab_layout);
		tabLayout.setTabMode(TabLayout.MODE_SCROLLABLE);

		// Hack due to bug in design support library v22.2.1
		// https://code.google.com/p/android/issues/detail?id=180462
		// TODO remove in new version
		if (ViewCompat.isLaidOut(tabLayout)) {
			tabLayout.setupWithViewPager(viewPager);
		} else {
			tabLayout.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
				@Override
				public void onLayoutChange(View v, int left, int top, int right, int bottom,
										   int oldLeft, int oldTop, int oldRight, int oldBottom) {
					tabLayout.setupWithViewPager(viewPager);
					tabLayout.removeOnLayoutChangeListener(this);
				}
			});
		}

		ImageButton onlineDocumentationButton =
				(ImageButton) view.findViewById(R.id.onlineDocumentationButton);
		onlineDocumentationButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				getActivity().startActivity(new Intent(Intent.ACTION_VIEW,
						Uri.parse("https://wiki.openstreetmap.org/wiki/Map_Features")));
			}
		});

		return view;
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putSerializable(TAGS_LIST, editPoiData.tags);
		super.onSaveInstanceState(outState);
	}

	public void addOnPageChangeListener(ViewPager.OnPageChangeListener listener) {
		viewPager.addOnPageChangeListener(listener);
	}

	public EditPoiFragment createInstance(Node node, Amenity amenity) {
		EditPoiFragment editPoiFragment = new EditPoiFragment();
		Bundle args = new Bundle();
		args.putSerializable(KEY_AMENITY_NODE, node);
		args.putSerializable(KEY_AMENITY, amenity);
		editPoiFragment.setArguments(args);
		return editPoiFragment;
	}

	public EditPoiData getEditPoiData() {
		return editPoiData;
	}

	public void send() {
		// TODO implement saving
	}

	public static class MyAdapter extends FragmentPagerAdapter {
		public MyAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public int getCount() {
			return 2;
		}

		@Override
		public Fragment getItem(int position) {
			switch (position) {
				case 0:
					return new NormalDataFragment();
				case 1:
					return new AdvancedDataFragment();
			}
			throw new IllegalArgumentException("Unexpected position");
		}

		@Override
		public CharSequence getPageTitle(int position) {
			// TODO replace with string resources
			switch (position) {
				case 0:
					return "Normal";
				case 1:
					return "Advanced";
			}
			throw new IllegalArgumentException("Unexpected position");
		}
	}

	public static class EditPoiData {
//		public boolean isLocalEdit;
//		public Node node;
		public LinkedHashSet<Tag> tags;
	}

	public static class Tag implements Serializable {
		public String tag;
		public String value;

		public Tag(String tag, String value) {
			this.tag = tag;
			this.value = value;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			Tag tag1 = (Tag) o;

			return tag.equals(tag1.tag);

		}

		@Override
		public int hashCode() {
			return tag.hashCode();
		}

		@Override
		public String toString() {
			return "Tag{" +
					"tag='" + tag + '\'' +
					", value='" + value + '\'' +
					'}';
		}
	}
}
