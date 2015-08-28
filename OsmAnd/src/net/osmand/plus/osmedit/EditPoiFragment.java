package net.osmand.plus.osmedit;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import net.osmand.PlatformUtil;
import net.osmand.access.AccessibleToast;
import net.osmand.data.Amenity;
import net.osmand.osm.PoiType;
import net.osmand.osm.edit.EntityInfo;
import net.osmand.osm.edit.Node;
import net.osmand.osm.edit.OSMSettings;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.osmedit.data.EditPoiData;
import net.osmand.plus.osmedit.data.Tag;
import net.osmand.plus.osmedit.dialogs.DeletePoiDialogFragment;
import net.osmand.plus.osmedit.dialogs.PoiSubTypeDialogFragment;
import net.osmand.plus.osmedit.dialogs.PoiTypeDialogFragment;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.text.MessageFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;

public class EditPoiFragment extends Fragment {
	public static final String TAG = "EditPoiFragment";
	private static final Log LOG = PlatformUtil.getLog(EditPoiFragment.class);

	private static final String KEY_AMENITY_NODE = "amenity_node";
	private static final String KEY_AMENITY = "amenity";
	private static final String TAGS_LIST = "tags_list";

	private final EditPoiData editPoiData = new EditPoiData();
	private ViewPager viewPager;
	private boolean isLocalEdit;
	private boolean mIsUserInput = true;
	private EditText poiNameEditText;
	private EditPoiData.TagsChangedListener mTagsChangedListener;
	private AutoCompleteTextView poiTypeEditText;
	private Node node;
	private Map<String, PoiType> allTranslatedSubTypes;
	public static final String POI_TYPE_TAG = "poi_type_tag";
	private OpenstreetmapUtil mOpenstreetmapUtil;
	private TextInputLayout poiTypeTextInputLayout;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
//		poiTypes = ((OsmandApplication) activity.getApplication()).getPoiTypes();
//		allTranslatedSubTypes = poiTypes.getAllTranslatedNames();
		OsmandSettings settings = ((OsmandApplication) activity.getApplication()).getSettings();
//		editPoiData.isLocalEdit = true;
		OsmEditingPlugin plugin = OsmandPlugin.getPlugin(OsmEditingPlugin.class);
		if (settings.OFFLINE_EDITION.get() || !settings.isInternetConnectionAvailable(true)) {
			mOpenstreetmapUtil = new OpenstreetmapLocalUtil(plugin, activity);
//			openstreetmapUtilToLoad = mOpenstreetmapUtil;
		} else if (!settings.isInternetConnectionAvailable(true)) {
			mOpenstreetmapUtil = new OpenstreetmapLocalUtil(plugin, activity);
//			openstreetmapUtilToLoad = new OpenstreetmapRemoteUtil(activity);
		} else {
			isLocalEdit = false;
			mOpenstreetmapUtil = new OpenstreetmapRemoteUtil(activity);
//			openstreetmapUtilToLoad = mOpenstreetmapUtil;
		}

		node = (Node) getArguments().getSerializable(KEY_AMENITY_NODE);
		allTranslatedSubTypes = ((OsmandApplication) activity.getApplication()).getPoiTypes()
				.getAllTranslatedNames();
		// TODO implement normal name
		editPoiData.amenity = (Amenity) getArguments().getSerializable(KEY_AMENITY);
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
		String basicTitle = getResources().getString(R.string.basic_tab_title);
		String extendedTitle = getResources().getString(R.string.extended_tab_title);
		MyAdapter pagerAdapter = new MyAdapter(getChildFragmentManager(), basicTitle, extendedTitle);
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
		ImageButton poiTypeButton = (ImageButton) view.findViewById(R.id.poiTypeButton);
		poiTypeButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				DialogFragment fragment = PoiTypeDialogFragment.createInstance(editPoiData.amenity);
				fragment.show(getChildFragmentManager(), "PoiTypeDialogFragment");
			}
		});

		poiNameEditText = (EditText) view.findViewById(R.id.poiNameEditText);
		poiNameEditText.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}

			@Override
			public void afterTextChanged(Editable s) {
				final Tag tag = new Tag(OSMSettings.OSMTagKey.NAME.getValue(), s.toString());
				if (mIsUserInput) {
					getEditPoiData().tags.remove(tag);
					getEditPoiData().tags.add(tag);
					getEditPoiData().notifyDatasetChanged(mTagsChangedListener);
				}
			}
		});
		poiTypeTextInputLayout = (TextInputLayout) view.findViewById(R.id.poiTypeTextInputLayout);
		poiTypeEditText = (AutoCompleteTextView) view.findViewById(R.id.poiTypeEditText);
		poiTypeEditText.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}

			@Override
			public void afterTextChanged(Editable s) {
				final Tag tag = new Tag(POI_TYPE_TAG, s.toString());
				if (mIsUserInput) {
					getEditPoiData().tags.remove(tag);
					getEditPoiData().tags.add(tag);
					getEditPoiData().notifyDatasetChanged(mTagsChangedListener);
				}
			}
		});
		poiTypeEditText.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (poiTypeEditText.getText().length() == 0 && editPoiData.amenity.getType() != null) {
					DialogFragment dialogFragment =
							PoiSubTypeDialogFragment.createInstance(editPoiData.amenity);
					dialogFragment.show(getChildFragmentManager(), "PoiSubTypeDialogFragment");
				}
			}
		});

		Button saveButton = (Button) view.findViewById(R.id.saveButton);
		int saveButtonTextId = isLocalEdit ? R.string.shared_string_save :
				R.string.default_buttons_commit;
		saveButton.setText(saveButtonTextId);
		saveButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO implement saving
				OsmPoint.Action action = node.getId() == -1 ? OsmPoint.Action.CREATE : OsmPoint.Action.MODIFY;
				String description = "";
				for (Tag tag : editPoiData.tags) {
					if (tag.tag.equals(POI_TYPE_TAG)) {
						if (allTranslatedSubTypes.get(tag.value) != null) {
							PoiType pt = allTranslatedSubTypes.get(tag.value);
							node.putTag(pt.getOsmTag(), pt.getOsmValue());
							if (pt.getOsmTag2() != null) {
								node.putTag(pt.getOsmTag2(), pt.getOsmValue2());
							}
						} else {
							node.putTag(editPoiData.amenity.getType().getDefaultTag(), tag.value);
						}
//					} else if (tag.tag.equals(OSMSettings.OSMTagKey.DESCRIPTION.getValue())) {
//						description = tag.value;
					} else {
						if (tag.value.length() > 0) {
							node.putTag(tag.tag, tag.value);
						} else {
							node.removeTag(tag.tag);
						}
					}
				}
				commitNode(action, node, mOpenstreetmapUtil.getEntityInfo(),
						"",
						false,//closeChange.isSelected(),
						new Runnable() {
							@Override
							public void run() {
								if (isLocalEdit) {
									AccessibleToast.makeText(
											getActivity(),
											R.string.osm_changes_added_to_local_edits,
											Toast.LENGTH_LONG).show();
								} else {
									final String message = node.getId() == -1 ?
											getResources().getString(R.string.poi_action_add)
											: getResources().getString(R.string.poi_action_change);

									AccessibleToast.makeText(
											getActivity(),
											MessageFormat.format(
													getResources().getString(
															R.string.poi_action_succeded_template), message),
											Toast.LENGTH_LONG).show();
								}
								if (getActivity() instanceof MapActivity) {
									((MapActivity) getActivity()).getMapView().refreshMap(true);
								}
								FragmentManager fragmentManager =
										getActivity().getSupportFragmentManager();
								fragmentManager.beginTransaction().remove(EditPoiFragment.this)
										.commit();
								fragmentManager.popBackStack();
							}
						}, getActivity(), mOpenstreetmapUtil);

			}
		});
		Button cancelButton = (Button) view.findViewById(R.id.cancelButton);
		cancelButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
				fragmentManager.beginTransaction().remove(EditPoiFragment.this).commit();
				fragmentManager.popBackStack();
			}
		});
		updateType(editPoiData.amenity);
		return view;
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putSerializable(TAGS_LIST, editPoiData.tags);
		super.onSaveInstanceState(outState);
	}

	public static EditPoiFragment createAddPoiInstance(double latitude, double longitude,
													   OsmandApplication application) {
		Node node = new Node(latitude, longitude, -1);
		Amenity amenity;
		amenity = new Amenity();
		amenity.setType(application.getPoiTypes().getOtherPoiCategory());
		amenity.setSubType("");
		amenity.setAdditionalInfo(OSMSettings.OSMTagKey.OPENING_HOURS.getValue(), "");
		return createInstance(node, amenity);
	}

	public static EditPoiFragment createInstance(Node node, Amenity amenity) {
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

	public void setSubCategory(String subCategory) {
		poiTypeEditText.setText(subCategory);
	}

	// TODO: 8/28/15 Move to some king of helper class
	public static void commitNode(final OsmPoint.Action action,
								  final Node n,
								  final EntityInfo info,
								  final String comment,
								  final boolean closeChangeSet,
								  final Runnable successAction,
								  final Activity activity,
								  final OpenstreetmapUtil openstreetmapUtil) {
		if (info == null && OsmPoint.Action.CREATE != action) {
			AccessibleToast.makeText(activity, activity.getResources().getString(R.string.poi_error_info_not_loaded), Toast.LENGTH_LONG).show();
			return;
		}
		new AsyncTask<Void, Void, Node>() {
			ProgressDialog progress;

			@Override
			protected void onPreExecute() {
				progress = ProgressDialog.show(activity, activity.getString(R.string.uploading), activity.getString(R.string.uploading_data));
				super.onPreExecute();
			}

			@Override
			protected Node doInBackground(Void... params) {
				Node node = openstreetmapUtil.commitNodeImpl(action, n, info, comment, closeChangeSet);
				return node;
			}

			@Override
			protected void onPostExecute(Node result) {
				progress.dismiss();
				if (result != null) {
					successAction.run();
				}
			}

			;
		}.execute();
	}

	public void updateType(Amenity amenity) {
		mIsUserInput = false;
		poiTypeEditText.setText(amenity.getSubType());
		mIsUserInput = true;
		poiTypeTextInputLayout.setHint(amenity.getType().getTranslation());

		final Map<String, PoiType> subCategories = new LinkedHashMap<>();
		for (Map.Entry<String, PoiType> s : allTranslatedSubTypes.entrySet()) {
			if (!subCategories.containsKey(s.getKey())) {
				subCategories.put(Algorithms.capitalizeFirstLetterAndLowercase(s.getKey()), s.getValue());
			}
		}

		final ArrayAdapter<Object> adapter = new ArrayAdapter<>(getActivity(),
				R.layout.list_textview, subCategories.keySet().toArray());
		poiTypeEditText.setAdapter(adapter);
		poiTypeEditText.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				Object item = parent.getAdapter().getItem(position);
				if (subCategories.containsKey(item)) {
					String keyName = subCategories.get(item).getKeyName();
					poiTypeEditText.setText(keyName);
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
			}
		});
	}

	public static void showEditInstance(final Amenity amenity, OsmandSettings settings,
										final MapActivity mapActivity) {
		final OpenstreetmapUtil openstreetmapUtilToLoad;
		if (settings.OFFLINE_EDITION.get() || !settings.isInternetConnectionAvailable(true)) {
			OsmEditingPlugin plugin = OsmandPlugin.getPlugin(OsmEditingPlugin.class);
			openstreetmapUtilToLoad = new OpenstreetmapLocalUtil(plugin, mapActivity);
		} else if (!settings.isInternetConnectionAvailable(true)) {
			openstreetmapUtilToLoad = new OpenstreetmapRemoteUtil(mapActivity);
		} else {
			openstreetmapUtilToLoad = new OpenstreetmapRemoteUtil(mapActivity);
		}
		new AsyncTask<Void, Void, Node>() {

			@Override
			protected Node doInBackground(Void... params) {
				return openstreetmapUtilToLoad.loadNode((Amenity) amenity);
			}

			protected void onPostExecute(Node n) {
				if (n != null) {
					EditPoiFragment fragment =
							EditPoiFragment.createInstance(n, (Amenity) amenity);
					mapActivity.getSupportFragmentManager().beginTransaction()
							.add(fragment, "EditPoiFragment").commit();
				} else {
					AccessibleToast.makeText(mapActivity,
							mapActivity.getString(R.string.poi_error_poi_not_found),
							Toast.LENGTH_SHORT).show();
				}
			}

			;

		}.execute(new Void[0]);
	}

	public static class MyAdapter extends FragmentPagerAdapter {
		private final String basicTitle;
		private final String extendedTitle;

		public MyAdapter(FragmentManager fm, String basicTitle, String extendedTitle) {
			super(fm);
			this.basicTitle = basicTitle;
			this.extendedTitle = extendedTitle;
		}

		@Override
		public int getCount() {
			return 2;
		}

		@Override
		public Fragment getItem(int position) {
			switch (position) {
				case 0:
					return new BasicDataFragment();
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
					return basicTitle;
				case 1:
					return extendedTitle;
			}
			throw new IllegalArgumentException("Unexpected position");
		}
	}

	// TODO: 8/28/15 Move to helper
	public static class ShowDeleteDialogAsyncTask extends AsyncTask<Amenity, Void, Node> {
		private final OpenstreetmapUtil openstreetmapUtil;
		private final AppCompatActivity activity;
		public ShowDeleteDialogAsyncTask(AppCompatActivity activity) {
			this.activity = activity;
			OsmandSettings settings = ((OsmandApplication) activity.getApplication()).getSettings();
			OsmEditingPlugin plugin = OsmandPlugin.getPlugin(OsmEditingPlugin.class);
			if (settings.OFFLINE_EDITION.get() || !settings.isInternetConnectionAvailable(true)) {
				openstreetmapUtil = new OpenstreetmapLocalUtil(plugin, activity);
			} else if (!settings.isInternetConnectionAvailable(true)) {
				openstreetmapUtil = new OpenstreetmapLocalUtil(plugin, activity);
			} else {
				openstreetmapUtil = new OpenstreetmapRemoteUtil(activity);
			}
		}

		protected Node doInBackground(Amenity[] params) {
			return openstreetmapUtil.loadNode(params[0]);
		};

		protected void onPostExecute(Node n) {
		if(n == null){
			AccessibleToast.makeText(activity, activity.getResources().getString(R.string.poi_error_poi_not_found), Toast.LENGTH_LONG).show();
			return;
		}
		DeletePoiDialogFragment.createInstance(n).show(activity.getSupportFragmentManager(),
				"DeletePoiDialogFragment");
	};
}
}