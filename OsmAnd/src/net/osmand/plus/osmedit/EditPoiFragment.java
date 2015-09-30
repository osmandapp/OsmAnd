package net.osmand.plus.osmedit;

import android.annotation.TargetApi;
import android.app.Activity;
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
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
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
import net.osmand.plus.osmedit.dialogs.DeletePoiDialogFragment;
import net.osmand.plus.osmedit.dialogs.PoiSubTypeDialogFragment;
import net.osmand.plus.osmedit.dialogs.PoiTypeDialogFragment;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.io.Serializable;
import java.text.MessageFormat;
import java.util.LinkedHashMap;
import java.util.Map;

public class EditPoiFragment extends DialogFragment {
	public static final String TAG = "EditPoiFragment";
	private static final Log LOG = PlatformUtil.getLog(EditPoiFragment.class);

	private static final String KEY_AMENITY_NODE = "key_amenity_node";
	private static final String KEY_AMENITY = "key_amenity";
	private static final String TAGS_LIST = "tags_list";

	private EditPoiData editPoiData;
	private ViewPager viewPager;
	private boolean isLocalEdit;
	private AutoCompleteTextView poiTypeEditText;
	private Node node;
	private Map<String, PoiType> allTranslatedSubTypes;

	private OpenstreetmapUtil mOpenstreetmapUtil;
	private TextInputLayout poiTypeTextInputLayout;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		OsmandSettings settings = getMyApplication().getSettings();
		OsmEditingPlugin plugin = OsmandPlugin.getPlugin(OsmEditingPlugin.class);
		if (settings.OFFLINE_EDITION.get() || !settings.isInternetConnectionAvailable(true)) {
			mOpenstreetmapUtil = new OpenstreetmapLocalUtil(plugin, activity);
		} else if (!settings.isInternetConnectionAvailable(true)) {
			mOpenstreetmapUtil = new OpenstreetmapLocalUtil(plugin, activity);
		} else {
			isLocalEdit = false;
			mOpenstreetmapUtil = new OpenstreetmapRemoteUtil(activity);
		}

		node = (Node) getArguments().getSerializable(KEY_AMENITY_NODE);
		allTranslatedSubTypes = getMyApplication().getPoiTypes().getAllTranslatedNames();

		Amenity amenity = (Amenity) getArguments().getSerializable(KEY_AMENITY);
		editPoiData = new EditPoiData(amenity, node, allTranslatedSubTypes);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		boolean isLightTheme = ((OsmandApplication) getActivity().getApplication())
				.getSettings().OSMAND_THEME.get() == OsmandSettings.OSMAND_LIGHT_THEME;
		int themeId = isLightTheme ? R.style.OsmandLightTheme : R.style.OsmandDarkTheme;
		setStyle(STYLE_NO_FRAME, themeId);
		getActivity().getWindow()
				.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		final View view = inflater.inflate(R.layout.fragment_edit_poi, container, false);
		final OsmandSettings settings = getMyApplication().getSettings();
		boolean isLightTheme = settings.OSMAND_THEME.get() == OsmandSettings.OSMAND_LIGHT_THEME;

		if (savedInstanceState != null) {
			Map<String, String> mp = (Map<String, String>) savedInstanceState.getSerializable(TAGS_LIST);
			editPoiData.updateTags(mp);
		}

		Toolbar toolbar = (Toolbar) view.findViewById(R.id.toolbar);
		toolbar.setTitle(R.string.poi_create_title);
		toolbar.setNavigationIcon(R.drawable.abc_ic_ab_back_mtrl_am_alpha);
		toolbar.setNavigationOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dismiss();
			}
		});

		viewPager = (ViewPager) view.findViewById(R.id.viewpager);
		String basicTitle = getResources().getString(R.string.tab_title_basic);
		String extendedTitle = getResources().getString(R.string.tab_title_advanced);
		final MyAdapter pagerAdapter = new MyAdapter(getChildFragmentManager(), basicTitle, extendedTitle);
		viewPager.setAdapter(pagerAdapter);
		viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
			@Override
			public void onPageScrolled(int i, float v, int i1) {

			}

			@Override
			public void onPageSelected(int i) {
				((OnFragmentActivatedListener) pagerAdapter.getItem(i)).onFragmentActivated();
			}

			@Override
			public void onPageScrollStateChanged(int i) {

			}
		});

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
		onlineDocumentationButton.setImageDrawable(
				getMyApplication().getIconsCache()
						.getPaintedContentIcon(R.drawable.ic_action_help,
								getResources().getColor(
										isLightTheme ? R.color.inactive_item_orange
												: R.color.dash_search_icon_dark)));
		final ImageButton poiTypeButton = (ImageButton) view.findViewById(R.id.poiTypeButton);
		poiTypeButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				DialogFragment fragment = PoiTypeDialogFragment.createInstance(editPoiData.amenity);
				fragment.show(getChildFragmentManager(), "PoiTypeDialogFragment");
			}
		});

		EditText poiNameEditText = (EditText) view.findViewById(R.id.poiNameEditText);
		poiNameEditText.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}

			@Override
			public void afterTextChanged(Editable s) {
				if (!getEditPoiData().isInEdit()) {
					getEditPoiData().putTag(OSMSettings.OSMTagKey.NAME.getValue(), s.toString());
				}
			}
		});
		poiNameEditText.setText(node.getTag(OSMSettings.OSMTagKey.NAME));
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
				if (!getEditPoiData().isInEdit()) {
					getEditPoiData().putTag(EditPoiData.POI_TYPE_TAG, s.toString());
				}
			}
		});
		poiNameEditText.setOnEditorActionListener(mOnEditorActionListener);
		poiTypeEditText.setOnEditorActionListener(mOnEditorActionListener);

		Button saveButton = (Button) view.findViewById(R.id.saveButton);
		int saveButtonTextId = isLocalEdit ? R.string.shared_string_save :
				R.string.default_buttons_commit;
		saveButton.setText(saveButtonTextId);
		saveButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				save();

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
		setAdapterForPoiTypeEditText();
		setCancelable(false);
		return view;
	}

	private void save() {
		if (TextUtils.isEmpty(poiTypeEditText.getText())) {
			poiTypeEditText.setError(getResources().getString(R.string.please_specify_poi_type));
			return;
		}
		OsmPoint.Action action = node.getId() == -1 ? OsmPoint.Action.CREATE : OsmPoint.Action.MODIFY;
		for (Map.Entry<String, String> tag : editPoiData.getTagValues().entrySet()) {
			if (tag.getKey().equals(EditPoiData.POI_TYPE_TAG)) {
				final PoiType poiType = allTranslatedSubTypes.get(tag.getValue().trim().toLowerCase());
				if (poiType != null) {
					node.putTag(poiType.getOsmTag(), poiType.getOsmValue());
					if (poiType.getOsmTag2() != null) {
						node.putTag(poiType.getOsmTag2(), poiType.getOsmValue2());
					}
				} else {
					node.putTag(editPoiData.amenity.getType().getDefaultTag(), tag.getValue());
				}
//					} else if (tag.tag.equals(OSMSettings.OSMTagKey.DESCRIPTION.getValue())) {
//						description = tag.value;
			} else {
				if (tag.getKey().length() > 0) {
					node.putTag(tag.getKey(), tag.getValue());
				} else {
					node.removeTag(tag.getKey());
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
						dismiss();
					}
				}, getActivity(), mOpenstreetmapUtil);
	}

	@Override
	public void onResume() {
		super.onResume();
		getDialog().setOnKeyListener(new DialogInterface.OnKeyListener() {
			@Override
			public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
				if (keyCode == android.view.KeyEvent.KEYCODE_BACK) {
					if (event.getAction() == KeyEvent.ACTION_DOWN) {
						return true;
					} else {
						new AreYouSureDialogFrgament().show(getChildFragmentManager(),
								"AreYouSureDialogFrgament");
						return true;
					}
				} else {
					return false;
				}
			}
		});
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putSerializable(TAGS_LIST, (Serializable) editPoiData.getTagValues());
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
				return openstreetmapUtil.commitNodeImpl(action, n, info, comment, closeChangeSet);
			}

			@Override
			protected void onPostExecute(Node result) {
				progress.dismiss();
				if (result != null) {
					successAction.run();
				}
			}
		}.execute();
	}

	public void updateType(Amenity amenity) {
		poiTypeEditText.setText(amenity.getSubType());
		poiTypeTextInputLayout.setHint(amenity.getType().getTranslation());
		setAdapterForPoiTypeEditText();
		poiTypeEditText.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(final View v, MotionEvent event) {
				final EditText editText = (EditText) v;
				final int DRAWABLE_RIGHT = 2;
				if (event.getAction() == MotionEvent.ACTION_UP) {
					if (event.getX() >= (editText.getRight()
							- editText.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width()
							- editText.getPaddingRight())) {
						if (editPoiData.amenity.getType() != null) {
							DialogFragment dialogFragment =
									PoiSubTypeDialogFragment.createInstance(editPoiData.amenity);
							dialogFragment.show(getChildFragmentManager(), "PoiSubTypeDialogFragment");
						}

						return true;
					}
				}
				return false;
			}
		});
	}

	private void setAdapterForPoiTypeEditText() {
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
				LOG.debug("item=" + item);
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

	private OsmandApplication getMyApplication() {
		return (OsmandApplication) getActivity().getApplication();
	}

	public static void showEditInstance(final Amenity amenity,
										final AppCompatActivity activity) {
		final OsmandSettings settings = ((OsmandApplication) activity.getApplication())
				.getSettings();
		final OpenstreetmapUtil openstreetmapUtilToLoad;
		if (settings.OFFLINE_EDITION.get() || !settings.isInternetConnectionAvailable(true)) {
			OsmEditingPlugin plugin = OsmandPlugin.getPlugin(OsmEditingPlugin.class);
			openstreetmapUtilToLoad = new OpenstreetmapLocalUtil(plugin, activity);
		} else if (!settings.isInternetConnectionAvailable(true)) {
			openstreetmapUtilToLoad = new OpenstreetmapRemoteUtil(activity);
		} else {
			openstreetmapUtilToLoad = new OpenstreetmapRemoteUtil(activity);
		}
		new AsyncTask<Void, Void, Node>() {
			@Override
			protected Node doInBackground(Void... params) {
				return openstreetmapUtilToLoad.loadNode(amenity);
			}

			protected void onPostExecute(Node n) {
				if (n != null) {
					EditPoiFragment fragment =
							EditPoiFragment.createInstance(n, amenity);
					fragment.show(activity.getSupportFragmentManager(), TAG);
				} else {
					AccessibleToast.makeText(activity,
							activity.getString(R.string.poi_error_poi_not_found),
							Toast.LENGTH_SHORT).show();
				}
			}
		}.execute();
	}

	public static class MyAdapter extends FragmentPagerAdapter {
		private final Fragment[] fragments = new Fragment[]{new BasicDataFragment(),
				new AdvancedDataFragment()};
		private final String[] titles;

		public MyAdapter(FragmentManager fm, String basicTitle, String extendedTitle) {
			super(fm);
			titles = new String[]{basicTitle, extendedTitle};
		}

		@Override
		public int getCount() {
			return 2;
		}

		@Override
		public Fragment getItem(int position) {
			return fragments[position];
		}

		@Override
		public CharSequence getPageTitle(int position) {
			return titles[position];
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
		}

		protected void onPostExecute(Node n) {
			if (n == null) {
				AccessibleToast.makeText(activity, activity.getResources().getString(R.string.poi_error_poi_not_found), Toast.LENGTH_LONG).show();
				return;
			}
			DeletePoiDialogFragment.createInstance(n).show(activity.getSupportFragmentManager(),
					"DeletePoiDialogFragment");
		}
	}

	public static class AreYouSureDialogFrgament extends DialogFragment {
		@NonNull
		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			builder.setTitle(getResources().getString(R.string.are_you_sure))
					.setMessage(getResources().getString(R.string.unsaved_changes_will_be_lost))
					.setPositiveButton(R.string.shared_string_ok, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							((DialogFragment) getParentFragment()).dismiss();
						}
					})
					.setNegativeButton(R.string.shared_string_cancel, null);
			return builder.create();
		}
	}

	private TextView.OnEditorActionListener mOnEditorActionListener =
			new TextView.OnEditorActionListener() {
				@Override
				public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
					boolean handled = false;
					if (actionId == EditorInfo.IME_ACTION_SEND) {
						save();
						handled = true;
					}
					return handled;
				}
			};
	public interface OnFragmentActivatedListener {
		void onFragmentActivated();
	}
}