package net.osmand.plus.osmedit;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
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

import net.osmand.access.AccessibleToast;
import net.osmand.data.Amenity;
import net.osmand.osm.MapPoiTypes;
import net.osmand.osm.PoiCategory;
import net.osmand.osm.PoiType;
import net.osmand.osm.edit.EntityInfo;
import net.osmand.osm.edit.Node;
import net.osmand.osm.edit.OSMSettings;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.util.Algorithms;

import java.io.Serializable;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class EditPoiFragment extends Fragment {
	public static final String TAG = "EditPoiFragment";

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
	private OpenstreetmapUtil openstreetmapUtil;
	private TextInputLayout poiTypeTextInputLayout;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
//		poiTypes = ((OsmandApplication) activity.getApplication()).getPoiTypes();
//		allTranslatedSubTypes = poiTypes.getAllTranslatedNames();
		OsmandSettings settings = ((OsmandApplication) activity.getApplication()).getSettings();
//		editPoiData.isLocalEdit = true;
		OsmEditingPlugin plugin = (OsmEditingPlugin) OsmandPlugin.getPlugin(OsmEditingPlugin.class);
		if (settings.OFFLINE_EDITION.get() || !settings.isInternetConnectionAvailable(true)) {
			openstreetmapUtil = new OpenstreetmapLocalUtil(plugin, activity);
//			openstreetmapUtilToLoad = openstreetmapUtil;
		} else if (!settings.isInternetConnectionAvailable(true)) {
			openstreetmapUtil = new OpenstreetmapLocalUtil(plugin, activity);
//			openstreetmapUtilToLoad = new OpenstreetmapRemoteUtil(activity);
		} else {
			isLocalEdit = false;
			openstreetmapUtil = new OpenstreetmapRemoteUtil(activity);
//			openstreetmapUtilToLoad = openstreetmapUtil;
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
				if(poiTypeEditText.getText().length() == 0 && editPoiData.amenity.getType() != null){
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
				Log.v(TAG, "tags=" + editPoiData.tags);
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
				commitNode(action, node, openstreetmapUtil.getEntityInfo(),
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
						}, getActivity());

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

	public void commitNode(final OsmPoint.Action action,
						   final Node n,
						   final EntityInfo info,
						   final String comment,
						   final boolean closeChangeSet,
						   final Runnable successAction,
						   final Activity activity) {
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

	private void updateType(Amenity amenity) {
		// TODO implement
		Log.v(TAG, "updateType(" + "amenity=" + amenity + ")");
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
		private Set<TagsChangedListener> mListeners = new HashSet<>();
		public LinkedHashSet<Tag> tags;
		public Amenity amenity;

		public void notifyDatasetChanged(TagsChangedListener listenerToSkip) {
			Log.v(TAG, "notifyDatasetChanged(" + "listenerToSkip=" + listenerToSkip + ")" + mListeners);
			for (TagsChangedListener listener : mListeners) {
				if (listener != listenerToSkip) listener.onTagsChanged();
			}
		}

		public void addListener(TagsChangedListener listener) {
			mListeners.add(listener);
		}

		public void deleteListener(TagsChangedListener listener) {
			mListeners.remove(listener);
		}

		public interface TagsChangedListener {
			void onTagsChanged();
		}
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

	public static class PoiTypeDialogFragment extends DialogFragment {
		@NonNull
		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			MapPoiTypes poiTypes = ((OsmandApplication) getActivity().getApplication()).getPoiTypes();
			final Amenity amenity = (Amenity) getArguments().getSerializable(KEY_AMENITY);
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			final List<PoiCategory> categories = poiTypes.getCategories(false);
			String[] vals = new String[categories.size()];
			for (int i = 0; i < vals.length; i++) {
				vals[i] = categories.get(i).getTranslation();
			}
			builder.setItems(vals, new Dialog.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					PoiCategory aType = categories.get(which);
					if (aType != amenity.getType()) {
						amenity.setType(aType);
						amenity.setSubType(""); //$NON-NLS-1$
						((EditPoiFragment) getParentFragment()).updateType(amenity);
					}
					dismiss();
				}
			});
			return builder.create();
		}

		public static PoiTypeDialogFragment createInstance(Amenity amenity) {
			PoiTypeDialogFragment poiTypeDialogFragment = new PoiTypeDialogFragment();
			Bundle args = new Bundle();
			args.putSerializable(KEY_AMENITY, amenity);
			poiTypeDialogFragment.setArguments(args);
			return poiTypeDialogFragment;
		}
	}

	public static class PoiSubTypeDialogFragment extends DialogFragment {
		@NonNull
		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			MapPoiTypes poiTypes = ((OsmandApplication) getActivity().getApplication()).getPoiTypes();
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			final Amenity a = (Amenity) getArguments().getSerializable(KEY_AMENITY);
			final Map<String, PoiType> allTranslatedNames = poiTypes.getAllTranslatedNames(a.getType(), true);
			// (=^.^=)
			final String[] subCats = allTranslatedNames.keySet().toArray(new String[0]);
			builder.setItems(subCats, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					((EditPoiFragment) getParentFragment()).setSubCategory(subCats[which]);
					dismiss();
				}
			});
			return builder.create();
		}

		public static PoiSubTypeDialogFragment createInstance(Amenity amenity) {
			PoiSubTypeDialogFragment fragment = new PoiSubTypeDialogFragment();
			Bundle args = new Bundle();
			args.putSerializable(KEY_AMENITY, amenity);
			fragment.setArguments(args);
			return fragment;
		}
	}
}