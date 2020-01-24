package net.osmand.plus.osmedit;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Map.Entry;

import net.osmand.AndroidUtils;
import net.osmand.CallbackWithObject;
import net.osmand.PlatformUtil;
import net.osmand.data.Amenity;
import net.osmand.data.LatLon;
import net.osmand.data.MapObject;
import net.osmand.osm.PoiCategory;
import net.osmand.osm.PoiType;
import net.osmand.osm.edit.Entity;
import net.osmand.osm.edit.EntityInfo;
import net.osmand.osm.edit.Node;
import net.osmand.osm.edit.OSMSettings;
import net.osmand.osm.edit.Way;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndDialogFragment;
import net.osmand.plus.osmedit.OsmPoint.Action;
import net.osmand.plus.osmedit.dialogs.PoiSubTypeDialogFragment;
import net.osmand.plus.osmedit.dialogs.PoiTypeDialogFragment;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class EditPoiDialogFragment extends BaseOsmAndDialogFragment {
	public static final String TAG = "EditPoiDialogFragment";
	private static final Log LOG = PlatformUtil.getLog(EditPoiDialogFragment.class);

	private static final String KEY_AMENITY_ENTITY = "key_amenity_entity";
	private static final String TAGS_LIST = "tags_list";
	private static final String IS_ADDING_POI = "is_adding_poi";

	public static final HashSet<String> BASIC_TAGS = new HashSet<String>() ;
	static {
		BASIC_TAGS.add(OSMSettings.OSMTagKey.NAME.getValue());
		BASIC_TAGS.add(OSMSettings.OSMTagKey.ADDR_STREET.getValue());
		BASIC_TAGS.add(OSMSettings.OSMTagKey.ADDR_HOUSE_NUMBER.getValue());
		BASIC_TAGS.add(OSMSettings.OSMTagKey.PHONE.getValue());
		BASIC_TAGS.add(OSMSettings.OSMTagKey.WEBSITE.getValue());
		BASIC_TAGS.add(OSMSettings.OSMTagKey.OPENING_HOURS.getValue());
	}

	private EditPoiData editPoiData;
	private ViewPager viewPager;
	private AutoCompleteTextView poiTypeEditText;

	private OpenstreetmapUtil mOpenstreetmapUtil;
	private TextInputLayout poiTypeTextInputLayout;
	private View view;

	public static final int AMENITY_TEXT_LENGTH= 255;

	@Override
	public void onAttach(Context activity) {
		super.onAttach(activity);
		OsmEditingPlugin plugin = OsmandPlugin.getPlugin(OsmEditingPlugin.class);
		if (getSettings().OFFLINE_EDITION.get()
				|| !getSettings().isInternetConnectionAvailable(true)) {
			mOpenstreetmapUtil = plugin.getPoiModificationLocalUtil();
		} else {
			mOpenstreetmapUtil = plugin.getPoiModificationRemoteUtil();
		}

		Entity entity = (Entity) getArguments().getSerializable(KEY_AMENITY_ENTITY);
		editPoiData = new EditPoiData(entity, getMyApplication());
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		view = inflater.inflate(R.layout.fragment_edit_poi, container, false);
		boolean isLightTheme = getSettings().OSMAND_THEME.get() == OsmandSettings.OSMAND_LIGHT_THEME;

		if (savedInstanceState != null) {
			@SuppressWarnings("unchecked")
			Map<String, String> mp = (Map<String, String>) savedInstanceState.getSerializable(TAGS_LIST);
			editPoiData.updateTags(mp);
		}

		boolean isAddingPoi = getArguments().getBoolean(IS_ADDING_POI);

		Toolbar toolbar = (Toolbar) view.findViewById(R.id.toolbar);
		toolbar.setTitle(isAddingPoi ? R.string.poi_create_title : R.string.poi_edit_title);
		toolbar.setNavigationIcon(R.drawable.ic_arrow_back);
		toolbar.setNavigationContentDescription(R.string.access_shared_string_navigate_up);
		toolbar.setNavigationOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dismissCheckForChanges();
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

		// tabLayout.setupWithViewPager(viewPager);
		// Hack due to bug in design support library v22.2.1
		// https://code.google.com/p/android/issues/detail?id=180462
		// TODO remove in new version
		if (Build.VERSION.SDK_INT >= 11) {
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
		} else {
			ViewTreeObserver vto = view.getViewTreeObserver();
			vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {

				@Override
				public void onGlobalLayout() {

					ViewTreeObserver obs = view.getViewTreeObserver();
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
						obs.removeGlobalOnLayoutListener(this);
					}

					if (getActivity() != null) {
						tabLayout.setupWithViewPager(viewPager);
					}
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

		final int colorId = isLightTheme ? R.color.active_color_primary_light : R.color.active_color_primary_dark;
		final int color = getResources().getColor(colorId);
		onlineDocumentationButton.setImageDrawable(getPaintedContentIcon(R.drawable.ic_action_help, color));
		final ImageButton poiTypeButton = (ImageButton) view.findViewById(R.id.poiTypeButton);
		poiTypeButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				PoiTypeDialogFragment fragment = PoiTypeDialogFragment.createInstance();
				fragment.setOnItemSelectListener(new PoiTypeDialogFragment.OnItemSelectListener() {
					@Override
					public void select(PoiCategory poiCategory) {
						setPoiCategory(poiCategory);
					}
				});
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
					if (!TextUtils.isEmpty(s)) {
						getEditPoiData().putTag(OSMSettings.OSMTagKey.NAME.getValue(),
								s.toString());
					} else {
						getEditPoiData().removeTag(OSMSettings.OSMTagKey.NAME.getValue());
					}
				}
			}
		});
		poiNameEditText.setText(editPoiData.getTag(OSMSettings.OSMTagKey.NAME.getValue()));
		poiNameEditText.requestFocus();
		AndroidUtils.showSoftKeyboard(poiNameEditText);
		poiTypeTextInputLayout = (TextInputLayout) view.findViewById(R.id.poiTypeTextInputLayout);
		poiTypeEditText = (AutoCompleteTextView) view.findViewById(R.id.poiTypeEditText);
		poiTypeEditText.setText(editPoiData.getPoiTypeString());
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
					getEditPoiData().updateTypeTag(s.toString(), true);
					if (!getMyApplication().isApplicationInitializing()) {
						PoiCategory category = editPoiData.getPoiCategory();
						if (category != null) {
							poiTypeTextInputLayout.setHint(category.getTranslation());
						}
					}
				}
			}
		});
		poiNameEditText.setOnEditorActionListener(mOnEditorActionListener);
		poiTypeEditText.setOnEditorActionListener(mOnEditorActionListener);
		poiTypeEditText.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(final View v, MotionEvent event) {
				final EditText editText = (EditText) v;
				final int DRAWABLE_RIGHT = 2;
				if (event.getAction() == MotionEvent.ACTION_UP) {
					if (event.getX() >= (editText.getRight()
							- editText.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width()
							- editText.getPaddingRight())) {
						PoiCategory category = editPoiData.getPoiCategory();
						if (category != null) {
							PoiSubTypeDialogFragment dialogFragment =
									PoiSubTypeDialogFragment.createInstance(category);
							dialogFragment.setOnItemSelectListener(new PoiSubTypeDialogFragment.OnItemSelectListener() {
								@Override
								public void select(String category) {
									setSubCategory(category);
								}
							});
							dialogFragment.show(getChildFragmentManager(), "PoiSubTypeDialogFragment");
						}

						return true;
					}
				}
				return false;
			}
		});

		if (!isAddingPoi && Entity.EntityType.valueOf(editPoiData.getEntity()) == Entity.EntityType.NODE) {
			Button deleteButton = (Button) view.findViewById(R.id.deleteButton);
			deleteButton.setVisibility(View.VISIBLE);
			deleteButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					DeletePoiHelper deletePoiHelper = new DeletePoiHelper(getMyActivity());
					deletePoiHelper.setCallback(new DeletePoiHelper.DeletePoiCallback() {
						@Override
						public void poiDeleted() {
							dismiss();
						}
					});
					deletePoiHelper.deletePoiWithDialog(getEditPoiData().getEntity());
				}
			});
		}

		Button saveButton = (Button) view.findViewById(R.id.saveButton);
		saveButton.setText(mOpenstreetmapUtil instanceof OpenstreetmapRemoteUtil
				? R.string.shared_string_upload : R.string.shared_string_save);
		saveButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				trySave();
			}
		});
		Button cancelButton = (Button) view.findViewById(R.id.cancelButton);
		cancelButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dismissCheckForChanges();
			}
		});
		setAdapterForPoiTypeEditText();
		setCancelable(false);
		return view;
	}

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		final Dialog dialog = super.onCreateDialog(savedInstanceState);
		dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
		return dialog;
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
						dismissCheckForChanges();
						return true;
					}
				}
				return false;
			}
		});
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putSerializable(TAGS_LIST, (Serializable) editPoiData.getTagValues());
		super.onSaveInstanceState(outState);
	}

	@Override
	public void show(FragmentManager manager, String tag) {
		if (manager.findFragmentByTag(TAG) == null) {
			super.show(manager, TAG);
		}
	}

	@Override
	public int show(FragmentTransaction transaction, String tag) {
		throw new UnsupportedOperationException("Please use show(FragmentManager manager, String tag)");
	}

	private void trySave() {
		String tagWithExceedingValue = isTextLengthInRange();
		if (!Algorithms.isEmpty(tagWithExceedingValue)){
			ValueExceedLimitDialogFragment f = new ValueExceedLimitDialogFragment();
			Bundle args = new Bundle();
			args.putString("tag", tagWithExceedingValue);
			f.setArguments(args);
			f.show(getChildFragmentManager(), "exceedDialog");
		} else if (TextUtils.isEmpty(poiTypeEditText.getText())) {
			HashSet<String> tagsCopy = new HashSet<>();
			tagsCopy.addAll(editPoiData.getTagValues().keySet());
			if (Algorithms.isEmpty(editPoiData.getTag(OSMSettings.OSMTagKey.ADDR_HOUSE_NUMBER.getValue()))) {
				SaveExtraValidationDialogFragment f = new SaveExtraValidationDialogFragment();
				Bundle args = new Bundle();
				args.putInt("message", R.string.save_poi_without_poi_type_message);
				f.setArguments(args);
				f.show(getChildFragmentManager(), "dialog");
				// poiTypeEditText.setError(getResources().getString(R.string.please_specify_poi_type));
			} else {
				save();
			}
		} else if (testTooManyCapitalLetters(editPoiData.getTag(OSMSettings.OSMTagKey.NAME.getValue()))) {
			SaveExtraValidationDialogFragment f = new SaveExtraValidationDialogFragment();
			Bundle args = new Bundle();
			args.putInt("message", R.string.save_poi_too_many_uppercase);
			f.setArguments(args);
			f.show(getChildFragmentManager(), "dialog");			
		} else if (editPoiData.getPoiCategory() == getMyApplication().getPoiTypes().getOtherPoiCategory()) {
			poiTypeEditText.setError(getResources().getString(R.string.please_specify_poi_type));
		} else if (editPoiData.getPoiTypeDefined() == null) {
			poiTypeEditText.setError(
				getResources().getString(R.string.please_specify_poi_type_only_from_list));
		} else {
			save();
		}
	}

	private String isTextLengthInRange() {
		for (Entry<String, String> s: editPoiData.getTagValues().entrySet()) {
			if (s.getValue().length() > AMENITY_TEXT_LENGTH) {
				return s.getKey();
			}
		}
		return "";
	}

	private boolean testTooManyCapitalLetters(String name) {
		if(name == null) {
			return false;
		}
		int capital = 0;
		int lower = 0;
		int nonalpha = 0;
		for(int i = 0; i < name.length(); i++) {
			char c = name.charAt(i);
			if(Character.isLetter(c) || Character.getType(c) == Character.LETTER_NUMBER) {
				if(Character.isUpperCase(c)) {
					capital ++;
				} else {
					lower ++;
				}
			} else {
				nonalpha ++;
			}
		}
		return capital > nonalpha && capital > lower;
	}

	private void save() {
		Entity original = editPoiData.getEntity();

		final boolean offlineEdit = mOpenstreetmapUtil instanceof OpenstreetmapLocalUtil;
		Entity entity;
		if (original instanceof Node) {
			entity = new Node(original.getLatitude(), original.getLongitude(), original.getId());
		} else if (original instanceof Way) {
			entity = new Way(original.getId(), ((Way) original).getNodeIds(), original.getLatitude(), original.getLongitude());
		} else {
			return;
		}

		Action action = entity.getId() < 0 ? Action.CREATE : Action.MODIFY;
		for (Map.Entry<String, String> tag : editPoiData.getTagValues().entrySet()) {
			if (!Algorithms.isEmpty(tag.getKey()) && !Algorithms.isEmpty(tag.getValue()) && 
					!tag.getKey().equals(EditPoiData.POI_TYPE_TAG)) {
				entity.putTagNoLC(tag.getKey(), tag.getValue());
			}
		}
		String poiTypeTag = editPoiData.getTagValues().get(EditPoiData.POI_TYPE_TAG);
		String comment = "";
		if (poiTypeTag != null) {
			final PoiType poiType = editPoiData.getAllTranslatedSubTypes().get(poiTypeTag.trim().toLowerCase());
			if (poiType != null) {
				entity.putTagNoLC(poiType.getEditOsmTag(), poiType.getEditOsmValue());
				entity.removeTag(EditPoiData.REMOVE_TAG_PREFIX + poiType.getEditOsmTag());
				if (poiType.getOsmTag2() != null) {
					entity.putTagNoLC(poiType.getOsmTag2(), poiType.getOsmValue2());
					entity.removeTag(EditPoiData.REMOVE_TAG_PREFIX + poiType.getOsmTag2());
				}
			} else if (!Algorithms.isEmpty(poiTypeTag)) {
				PoiCategory category = editPoiData.getPoiCategory();
				if (category != null) {
					entity.putTagNoLC(category.getDefaultTag(), poiTypeTag);
				}
			}
			if (offlineEdit && !Algorithms.isEmpty(poiTypeTag)) {
				entity.putTagNoLC(EditPoiData.POI_TYPE_TAG, poiTypeTag);
			}
			String actionString = action == Action.CREATE ? getString(R.string.default_changeset_add) : getString(R.string.default_changeset_edit);
			comment = actionString + " " + poiTypeTag;
		}
		commitEntity(action, entity, mOpenstreetmapUtil.getEntityInfo(entity.getId()), comment, false,
				new CallbackWithObject<Entity>() {

					@Override
					public boolean processResult(Entity result) {
						if (result != null) {
							OsmEditingPlugin plugin = OsmandPlugin.getPlugin(OsmEditingPlugin.class);
							if (plugin != null && offlineEdit) {
								List<OpenstreetmapPoint> points = plugin.getDBPOI().getOpenstreetmapPoints();
								if (getActivity() instanceof MapActivity && points.size() > 0) {
									OsmPoint point = points.get(points.size() - 1);
									MapActivity mapActivity = (MapActivity) getActivity();
									mapActivity.getContextMenu().showOrUpdate(
											new LatLon(point.getLatitude(), point.getLongitude()),
											plugin.getOsmEditsLayer(mapActivity).getObjectName(point), point);
								}
							}

							if (getActivity() instanceof MapActivity) {
								((MapActivity) getActivity()).getMapView().refreshMap(true);
							}
							dismiss();
						} else {
							OsmEditingPlugin plugin = OsmandPlugin.getPlugin(OsmEditingPlugin.class);
							mOpenstreetmapUtil = plugin.getPoiModificationLocalUtil();
							Button saveButton = (Button) view.findViewById(R.id.saveButton);
							saveButton.setText(mOpenstreetmapUtil instanceof OpenstreetmapRemoteUtil
									? R.string.shared_string_upload : R.string.shared_string_save);
						}

						return false;
					}
				}, getActivity(), mOpenstreetmapUtil, action == Action.MODIFY ? editPoiData.getChangedTags() : null);
	}

	private void dismissCheckForChanges() {
		if (editPoiData.hasChangesBeenMade()) {
			new AreYouSureDialogFragment().show(getChildFragmentManager(),
					"AreYouSureDialogFragment");
		} else {
			dismiss();
		}
	}

	public EditPoiData getEditPoiData() {
		return editPoiData;
	}

	public void setSubCategory(String subCategory) {
		poiTypeEditText.setText(subCategory);
	}

	public static void commitEntity(final Action action,
								  final Entity entity,
								  final EntityInfo info,
								  final String comment,
								  final boolean closeChangeSet,
								  final CallbackWithObject<Entity> postExecute,
								  final Activity activity,
								  final OpenstreetmapUtil openstreetmapUtil,
								  @Nullable final Set<String> changedTags) {
		if (info == null && Action.CREATE != action && openstreetmapUtil instanceof OpenstreetmapRemoteUtil) {
			Toast.makeText(activity, activity.getResources().getString(R.string.poi_error_info_not_loaded), Toast.LENGTH_LONG).show();
			return;
		}
		new AsyncTask<Void, Void, Entity>() {
			ProgressDialog progress;

			@Override
			protected void onPreExecute() {
				progress = ProgressDialog.show(activity, activity.getString(R.string.uploading), activity.getString(R.string.uploading_data));
				super.onPreExecute();
			}

			@Override
			protected Entity doInBackground(Void... params) {
				return openstreetmapUtil.commitEntityImpl(action, entity, info, comment, closeChangeSet, changedTags);
			}

			@Override
			protected void onPostExecute(Entity result) {
				progress.dismiss();
				if (postExecute != null) {
					postExecute.processResult(result);
				}
			}
		}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	public void setPoiCategory(PoiCategory type) {
		editPoiData.updateType(type);
		poiTypeEditText.setText(editPoiData.getPoiTypeString());
		setAdapterForPoiTypeEditText();
	}

	private void setAdapterForPoiTypeEditText() {
		final Map<String, PoiType> subCategories = new LinkedHashMap<>();
		PoiCategory ct = editPoiData.getPoiCategory();
		if (ct != null) {
			for (PoiType s : ct.getPoiTypes()) {
				if (!s.isReference() && !s.isNotEditableOsm() && s.getBaseLangType() == null) {
					addMapEntryAdapter(subCategories, s.getTranslation(), s);
					if(!s.getKeyName().contains("osmand")) {
						addMapEntryAdapter(subCategories, s.getKeyName().replace('_', ' '), s);
					}
					if(!Algorithms.isEmpty(s.getEditOsmValue())) {
						addMapEntryAdapter(subCategories, s.getEditOsmValue().replace('_', ' '), s);
					}
				}
			}
		}
		for (Map.Entry<String, PoiType> s : editPoiData.getAllTranslatedSubTypes().entrySet()) {
			if(!s.getKey().contains("osmand")) {
				addMapEntryAdapter(subCategories, s.getKey(), s.getValue());
			}
		}
		final ArrayAdapter<Object> adapter;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			adapter = new ArrayAdapter<>(getActivity(),
					R.layout.list_textview, subCategories.keySet().toArray());
		} else {
			TypedValue typedValue = new TypedValue();
			Resources.Theme theme = getActivity().getTheme();
			theme.resolveAttribute(android.R.attr.textColorSecondary, typedValue, true);
			final int textColor = typedValue.data;

			adapter = new ArrayAdapter<Object>(getActivity(),
					R.layout.list_textview, subCategories.keySet().toArray()) {
				@Override
				public View getView(int position, View convertView, ViewGroup parent) {
					final View view = super.getView(position, convertView, parent);
					((TextView) view.findViewById(R.id.textView)).setTextColor(textColor);
					return view;
				}
			};
		}
		adapter.sort(new Comparator<Object>() {
			@Override
			public int compare(Object lhs, Object rhs) {
				return lhs.toString().compareTo(rhs.toString());
			}
		});
		poiTypeEditText.setAdapter(adapter);
		poiTypeEditText.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				Object item = parent.getAdapter().getItem(position);
				poiTypeEditText.setText(item.toString());
				setAdapterForPoiTypeEditText();
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
			}
		});

	}

	private void addMapEntryAdapter(final Map<String, PoiType> subCategories, String key, PoiType v) {
		if (!subCategories.containsKey(key.toLowerCase())) {
			subCategories.put(Algorithms.capitalizeFirstLetterAndLowercase(key), v);
		}
	}

	public static EditPoiDialogFragment createAddPoiInstance(double latitude, double longitude,
															 OsmandApplication application) {
		Node node = new Node(latitude, longitude, -1);
		return createInstance(node, true);
	}

	public static EditPoiDialogFragment createInstance(Entity entity, boolean isAddingPoi) {
		EditPoiDialogFragment editPoiDialogFragment = new EditPoiDialogFragment();
		Bundle args = new Bundle();
		args.putSerializable(KEY_AMENITY_ENTITY, entity);
		args.putBoolean(IS_ADDING_POI, isAddingPoi);
		editPoiDialogFragment.setArguments(args);
		return editPoiDialogFragment;
	}

	public static EditPoiDialogFragment createInstance(Entity entity, boolean isAddingPoi, Map<String, String> tagList) {
		EditPoiDialogFragment editPoiDialogFragment = new EditPoiDialogFragment();
		Bundle args = new Bundle();
		args.putSerializable(KEY_AMENITY_ENTITY, entity);
		args.putBoolean(IS_ADDING_POI, isAddingPoi);
		args.putSerializable(TAGS_LIST, (Serializable) Collections.unmodifiableMap(tagList));
		editPoiDialogFragment.setArguments(args);
		return editPoiDialogFragment;
	}

	public static void showEditInstance(final MapObject mapObject,
										final AppCompatActivity activity) {
		final OsmandSettings settings = ((OsmandApplication) activity.getApplication())
				.getSettings();
		final OpenstreetmapUtil openstreetmapUtilToLoad;
		OsmEditingPlugin plugin = OsmandPlugin.getPlugin(OsmEditingPlugin.class);
		if (//settings.OFFLINE_EDITION.get() ||
				!settings.isInternetConnectionAvailable(true)) {
			openstreetmapUtilToLoad = plugin.getPoiModificationLocalUtil();
		} else {
			openstreetmapUtilToLoad = plugin.getPoiModificationRemoteUtil();
		}
		new AsyncTask<Void, Void, Entity>() {
			@Override
			protected Entity doInBackground(Void... params) {
				return openstreetmapUtilToLoad.loadEntity(mapObject);
			}

			protected void onPostExecute(Entity entity) {
				if (entity != null) {
					EditPoiDialogFragment fragment =
							EditPoiDialogFragment.createInstance(entity, false);
					fragment.show(activity.getSupportFragmentManager(), TAG);
				} else {
					Toast.makeText(activity,
							activity.getString(R.string.poi_cannot_be_found),
							Toast.LENGTH_LONG).show();
				}
			}
		}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	public static class MyAdapter extends FragmentPagerAdapter {
		private final Fragment[] fragments = new Fragment[]{new BasicEditPoiFragment(),
				new AdvancedEditPoiFragment()};
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

	public static class DeletePoiHelper {
		private final OpenstreetmapUtil openstreetmapUtil;
		private final AppCompatActivity activity;
		private DeletePoiCallback callback;

		public void setCallback(DeletePoiCallback callback) {
			this.callback = callback;
		}

		public DeletePoiHelper(AppCompatActivity activity) {
			this.activity = activity;
			OsmandSettings settings = ((OsmandApplication) activity.getApplication()).getSettings();
			OsmEditingPlugin plugin = OsmandPlugin.getPlugin(OsmEditingPlugin.class);
			if (settings.OFFLINE_EDITION.get() || !settings.isInternetConnectionAvailable(true)) {
				openstreetmapUtil = plugin.getPoiModificationLocalUtil();
			} else {
				openstreetmapUtil = plugin.getPoiModificationRemoteUtil();
			}
		}

		public void deletePoiWithDialog(Amenity amenity) {
			new AsyncTask<Amenity, Void, Entity>() {

				@Override
				protected Entity doInBackground(Amenity... params) {
					return openstreetmapUtil.loadEntity(params[0]);
				}

				@Override
				protected void onPostExecute(Entity entity) {
					deletePoiWithDialog(entity);
				}
			}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, amenity);
		}

		public void deletePoiWithDialog(final Entity entity) {
			if (entity == null) {
				Toast.makeText(activity, activity.getResources().getString(R.string.poi_cannot_be_found), Toast.LENGTH_LONG).show();
				return;
			}
			AlertDialog.Builder builder = new AlertDialog.Builder(activity);
			builder.setTitle(R.string.poi_remove_title);
			final EditText comment;
			final CheckBox closeChangesetCheckBox;
			final boolean isLocalEdit = openstreetmapUtil instanceof OpenstreetmapLocalUtil;
			if (isLocalEdit) {
				closeChangesetCheckBox = null;
				comment = null;
			} else {
				LinearLayout ll = new LinearLayout(activity);
				ll.setPadding(16, 2, 16, 0);
				ll.setOrientation(LinearLayout.VERTICAL);
				closeChangesetCheckBox = new CheckBox(activity);
				closeChangesetCheckBox.setText(R.string.close_changeset);
				ll.addView(closeChangesetCheckBox);
				comment = new EditText(activity);
				comment.setText(R.string.poi_remove_title);
				ll.addView(comment);
				builder.setView(ll);
			}
			builder.setNegativeButton(R.string.shared_string_cancel, null);
			builder.setPositiveButton(isLocalEdit ? R.string.shared_string_save : R.string.shared_string_delete, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					if (entity instanceof Node) {
						String c = comment == null ? null : comment.getText().toString();
						boolean closeChangeSet = closeChangesetCheckBox != null
								&& closeChangesetCheckBox.isChecked();
						deleteNode(entity, c, closeChangeSet);
					}
				}
			});
			builder.create().show();
		}

		private void deleteNode(final Entity entity, final String c, final boolean closeChangeSet) {
			final boolean isLocalEdit = openstreetmapUtil instanceof OpenstreetmapLocalUtil;
			commitEntity(Action.DELETE, entity, openstreetmapUtil.getEntityInfo(entity.getId()), c, closeChangeSet,
					new CallbackWithObject<Entity>() {

						@Override
						public boolean processResult(Entity result) {
							if (result != null) {
								if (callback != null) {
									callback.poiDeleted();
								}
								if (isLocalEdit) {
									Toast.makeText(activity, R.string.osm_changes_added_to_local_edits,
											Toast.LENGTH_LONG).show();
								} else {
									Toast.makeText(activity, R.string.poi_remove_success, Toast.LENGTH_LONG)
											.show();
								}
								if (activity instanceof MapActivity) {
									((MapActivity) activity).getMapView().refreshMap(true);
								}
							}
							return false;
						}
					}, activity, openstreetmapUtil, null);
		}

		public interface DeletePoiCallback {
			void poiDeleted();
		}
	}

	public static class SaveExtraValidationDialogFragment extends DialogFragment {
		@NonNull
		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			String msg = getString(R.string.save_poi_without_poi_type_message);
			int i = getArguments().getInt("message", 0);
			if(i != 0) {
				msg = getString(i);
			}
			builder.setTitle(getResources().getString(R.string.are_you_sure))
					.setMessage(msg)
					.setPositiveButton(R.string.shared_string_ok, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							((EditPoiDialogFragment) getParentFragment()).save();
						}
					})
					.setNegativeButton(R.string.shared_string_cancel, null);
			return builder.create();
		}
	}

	public static class AreYouSureDialogFragment extends DialogFragment {
		@NonNull
		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			builder.setTitle(getResources().getString(R.string.are_you_sure))
					.setMessage(getString(R.string.unsaved_changes_will_be_lost))
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

	public static class ValueExceedLimitDialogFragment extends DialogFragment {
		@NonNull
		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			String msg = getString(R.string.save_poi_value_exceed_length);
			String fieldTag = getArguments().getString("tag", "");
			if(!Algorithms.isEmpty(fieldTag)) {
				msg = String.format(msg, fieldTag);
			}
			builder.setTitle(String.format(getResources().getString(R.string.save_poi_value_exceed_length_title), fieldTag))
				.setMessage(msg)
				.setNegativeButton(R.string.shared_string_ok, null);
			return builder.create();
		}
	}

	private TextView.OnEditorActionListener mOnEditorActionListener =
			new TextView.OnEditorActionListener() {
				@Override
				public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
					boolean handled = false;
					if (actionId == EditorInfo.IME_ACTION_SEND) {
						trySave();
						handled = true;
					}
					return handled;
				}
			};

	public interface OnFragmentActivatedListener {
		void onFragmentActivated();
	}
}
