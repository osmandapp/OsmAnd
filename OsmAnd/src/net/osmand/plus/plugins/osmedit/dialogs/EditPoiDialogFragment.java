package net.osmand.plus.plugins.osmedit.dialogs;

import static net.osmand.osm.edit.Entity.POI_TYPE_TAG;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import net.osmand.CallbackWithObject;
import net.osmand.PlatformUtil;
import net.osmand.data.LatLon;
import net.osmand.data.MapObject;
import net.osmand.osm.PoiCategory;
import net.osmand.osm.PoiType;
import net.osmand.osm.edit.Entity;
import net.osmand.osm.edit.EntityInfo;
import net.osmand.osm.edit.Node;
import net.osmand.osm.edit.OSMSettings.OSMTagKey;
import net.osmand.osm.edit.Way;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseFullScreenDialogFragment;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.osmedit.OsmEditingPlugin;
import net.osmand.plus.plugins.osmedit.asynctasks.CommitEntityTask;
import net.osmand.plus.plugins.osmedit.asynctasks.LoadEntityTask;
import net.osmand.plus.plugins.osmedit.data.EditPoiData;
import net.osmand.plus.plugins.osmedit.data.OpenstreetmapPoint;
import net.osmand.plus.plugins.osmedit.data.OsmPoint;
import net.osmand.plus.plugins.osmedit.data.OsmPoint.Action;
import net.osmand.plus.plugins.osmedit.helpers.OpenstreetmapLocalUtil;
import net.osmand.plus.plugins.osmedit.helpers.OpenstreetmapRemoteUtil;
import net.osmand.plus.plugins.osmedit.helpers.OpenstreetmapUtil;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.widgets.OsmandTextFieldBoxes;
import net.osmand.plus.widgets.tools.SimpleTextWatcher;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.io.Serializable;
import java.util.*;
import java.util.Map.Entry;

import studio.carbonylgroup.textfieldboxes.ExtendedEditText;

public class EditPoiDialogFragment extends BaseFullScreenDialogFragment {

	public static final String TAG = EditPoiDialogFragment.class.getSimpleName();
	private static final Log LOG = PlatformUtil.getLog(EditPoiDialogFragment.class);

	private static final String KEY_AMENITY_ENTITY = "key_amenity_entity";
	private static final String TAGS_LIST = "tags_list";
	private static final String IS_ADDING_POI = "is_adding_poi";
	private static final int ADVANCED_TAB = 1;

	public static final HashSet<String> BASIC_TAGS = new HashSet<String>();

	static {
		BASIC_TAGS.add(OSMTagKey.NAME.getValue());
		BASIC_TAGS.add(OSMTagKey.ADDR_STREET.getValue());
		BASIC_TAGS.add(OSMTagKey.ADDR_HOUSE_NUMBER.getValue());
		BASIC_TAGS.add(OSMTagKey.PHONE.getValue());
		BASIC_TAGS.add(OSMTagKey.WEBSITE.getValue());
		BASIC_TAGS.add(OSMTagKey.OPENING_HOURS.getValue());
	}

	private final OsmEditingPlugin plugin = PluginsHelper.requirePlugin(OsmEditingPlugin.class);
	private OpenstreetmapUtil openstreetmapUtil;

	private EditPoiData editPoiData;
	private ViewPager2 viewPager;
	private ExtendedEditText poiTypeEditText;

	private OnSaveButtonClickListener onSaveButtonClickListener;
	private OsmandTextFieldBoxes poiTypeTextInputLayout;
	private View view;

	public static final int AMENITY_TEXT_LENGTH = 255;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (plugin.OFFLINE_EDITION.get() || !settings.isInternetConnectionAvailable(true)) {
			openstreetmapUtil = plugin.getPoiModificationLocalUtil();
		} else {
			openstreetmapUtil = plugin.getPoiModificationRemoteUtil();
		}

		Entity entity = AndroidUtils.getSerializable(getArguments(), KEY_AMENITY_ENTITY, Entity.class);
		editPoiData = new EditPoiData(entity, app);
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
			@Nullable Bundle savedInstanceState) {
		updateNightMode();
		view = themedInflater.inflate(R.layout.fragment_edit_poi, container, false);
		if (savedInstanceState != null) {
			Map<String, String> map = (Map<String, String>) AndroidUtils.getSerializable(savedInstanceState, TAGS_LIST, LinkedHashMap.class);
			editPoiData.updateTags(map);
		}

		boolean isAddingPoi = getArguments().getBoolean(IS_ADDING_POI);

		Toolbar toolbar = view.findViewById(R.id.toolbar);
		toolbar.setTitle(isAddingPoi ? R.string.poi_create_title : R.string.poi_edit_title);
		Drawable icBack = app.getUIUtilities().getIcon(AndroidUtils.getNavigationIconResId(getContext()));
		toolbar.setNavigationIcon(icBack);
		toolbar.setNavigationContentDescription(R.string.access_shared_string_navigate_up);
		toolbar.setNavigationOnClickListener(v -> dismissCheckForChanges());

		viewPager = view.findViewById(R.id.viewpager);
		String basicTitle = getResources().getString(R.string.tab_title_basic);
		String extendedTitle = getResources().getString(R.string.tab_title_advanced);
		TabLayout tabLayout = view.findViewById(R.id.tab_layout);
		PoiInfoPagerAdapter pagerAdapter = new PoiInfoPagerAdapter(this, new String[] {basicTitle, extendedTitle});
		viewPager.setAdapter(pagerAdapter);
		viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
			@Override
			public void onPageScrolled(int position, float positionOffset,
					int positionOffsetPixels) {
				super.onPageScrolled(position, positionOffset, positionOffsetPixels);
			}

			@Override
			public void onPageSelected(int position) {
				Fragment pageFragment = pagerAdapter.createFragment(position);
				((OnFragmentActivatedListener) pageFragment).onFragmentActivated();
				if (pageFragment instanceof OnSaveButtonClickListener) {
					onSaveButtonClickListener = (OnSaveButtonClickListener) pageFragment;
				} else {
					onSaveButtonClickListener = null;
				}
			}

			@Override
			public void onPageScrollStateChanged(int state) {
				super.onPageScrollStateChanged(state);
			}
		});


		tabLayout.setTabMode(TabLayout.MODE_SCROLLABLE);

		// tabLayout.setupWithViewPager(viewPager);
		// Hack due to bug in design support library v22.2.1
		// https://code.google.com/p/android/issues/detail?id=180462
		// TODO remove in new version
		if (ViewCompat.isLaidOut(tabLayout)) {
			TabLayoutMediator mediator = new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> tab.setText(pagerAdapter.getPageTitle(position)));
			mediator.attach();
		} else {
			tabLayout.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
				@Override
				public void onLayoutChange(View v, int left, int top, int right, int bottom,
						int oldLeft, int oldTop, int oldRight, int oldBottom) {
					TabLayoutMediator mediator = new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> tab.setText(pagerAdapter.getPageTitle(position)));
					mediator.attach();
					tabLayout.removeOnLayoutChangeListener(this);
				}
			});
		}

		ImageButton onlineDocumentationButton = view.findViewById(R.id.onlineDocumentationButton);
		onlineDocumentationButton.setOnClickListener(v -> {
			Activity activity = getActivity();
			if (activity != null) {
				Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://wiki.openstreetmap.org/wiki/Map_Features"));
				AndroidUtils.startActivityIfSafe(activity, intent);
			}
		});

		int activeColor = ColorUtilities.getActiveColor(getContext(), nightMode);
		onlineDocumentationButton.setImageDrawable(getPaintedContentIcon(R.drawable.ic_action_help, activeColor));
		ImageButton poiTypeButton = view.findViewById(R.id.poiTypeButton);
		poiTypeButton.setOnClickListener(v -> {
			PoiTypeDialogFragment fragment = PoiTypeDialogFragment.createInstance();
			fragment.setOnItemSelectListener(this::setPoiCategory);
			fragment.show(getChildFragmentManager(), "PoiTypeDialogFragment");
		});

		ExtendedEditText poiNameEditText = view.findViewById(R.id.poiNameEditText);
		AndroidUtils.setTextHorizontalGravity(poiNameEditText, Gravity.START);
		poiNameEditText.addTextChangedListener(new SimpleTextWatcher() {
			@Override
			public void afterTextChanged(Editable s) {
				if (!getEditPoiData().isInEdit()) {
					if (!TextUtils.isEmpty(s)) {
						getEditPoiData().putTag(OSMTagKey.NAME.getValue(),
								s.toString());
					} else {
						getEditPoiData().removeTag(OSMTagKey.NAME.getValue());
					}
				}
			}
		});
		poiNameEditText.setText(editPoiData.getTag(OSMTagKey.NAME.getValue()));
		poiNameEditText.requestFocus();
		AndroidUtils.showSoftKeyboard(getActivity(), poiNameEditText);
		poiTypeTextInputLayout = view.findViewById(R.id.poiTypeTextInputLayout);
		poiTypeEditText = view.findViewById(R.id.poiTypeEditText);
		AndroidUtils.setTextHorizontalGravity(poiTypeEditText, Gravity.START);
		poiTypeEditText.setText(editPoiData.getPoiTypeString());
		poiTypeEditText.addTextChangedListener(new SimpleTextWatcher() {
			@Override
			public void afterTextChanged(Editable s) {
				if (!getEditPoiData().isInEdit()) {
					getEditPoiData().updateTypeTag(s.toString(), true);
					if (!app.isApplicationInitializing()) {
						PoiCategory category = editPoiData.getPoiCategory();
						if (category != null) {
							poiTypeTextInputLayout.setLabelText(category.getTranslation());
						}
					}
				}
			}
		});
		poiNameEditText.setOnEditorActionListener(mOnEditorActionListener);
		poiTypeEditText.setOnEditorActionListener(mOnEditorActionListener);

		AppCompatImageButton expandButton = poiTypeTextInputLayout.getEndIconImageButton();
		expandButton.setColorFilter(R.color.gpx_chart_red);
		expandButton.setOnClickListener(v -> {
			PoiCategory category = editPoiData.getPoiCategory();
			if (category != null) {
				PoiSubTypeDialogFragment.showInstance(getChildFragmentManager(), category, this::setSubCategory);
			}
		});

		if (!isAddingPoi && Entity.EntityType.valueOf(editPoiData.getEntity()) == Entity.EntityType.NODE) {
			Button deleteButton = view.findViewById(R.id.deleteButton);
			deleteButton.setVisibility(View.VISIBLE);
			deleteButton.setOnClickListener(v -> {
				DeletePoiHelper deletePoiHelper = new DeletePoiHelper((AppCompatActivity) getActivity());
				deletePoiHelper.setCallback(this::dismiss);
				deletePoiHelper.deletePoiWithDialog(getEditPoiData().getEntity());
			});
		}

		Button saveButton = view.findViewById(R.id.saveButton);
		saveButton.setText(openstreetmapUtil instanceof OpenstreetmapRemoteUtil
				? R.string.shared_string_upload : R.string.shared_string_save);
		saveButton.setOnClickListener(v -> trySave());
		Button cancelButton = view.findViewById(R.id.cancelButton);
		cancelButton.setOnClickListener(v -> dismissCheckForChanges());
		setAdapterForPoiTypeEditText();
		setCancelable(false);
		if (editPoiData.hasEmptyValue()) {
			viewPager.setCurrentItem(ADVANCED_TAB);
		}
		editPoiData.setupInitPoint();

		AppBarLayout appBarLayout = view.findViewById(R.id.app_bar);
		appBarLayout.addOnOffsetChangedListener((layout, verticalOffset) -> {
			Rect mReact = new Rect();
			view.getHitRect(mReact);

			boolean clearFocus = poiNameEditText.getLocalVisibleRect(mReact);
			poiNameEditText.setFocusable(clearFocus);
			poiNameEditText.setFocusableInTouchMode(clearFocus);
			poiNameEditText.setClickable(clearFocus);

			poiTypeEditText.setFocusable(clearFocus);
			poiTypeEditText.setFocusableInTouchMode(clearFocus);
			poiTypeEditText.setClickable(clearFocus);
		});

		return view;
	}

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		Dialog dialog = super.onCreateDialog(savedInstanceState);
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
	public void onSaveInstanceState(@NonNull Bundle outState) {
		outState.putSerializable(TAGS_LIST, (Serializable) new LinkedHashMap<>(editPoiData.getTagValues()));
		super.onSaveInstanceState(outState);
	}

	@Override
	public void show(FragmentManager manager, String tag) {
		if (!manager.isStateSaved() && manager.findFragmentByTag(TAG) == null) {
			super.show(manager, TAG);
		}
	}

	@Override
	public int show(@NonNull FragmentTransaction transaction, String tag) {
		throw new UnsupportedOperationException("Please use show(FragmentManager manager, String tag)");
	}

	public void trySave() {
		if (onSaveButtonClickListener != null) {
			onSaveButtonClickListener.onSaveButtonClick();
		}
		String tagWithExceedingValue = isTextLengthInRange();
		if (!Algorithms.isEmpty(tagWithExceedingValue)) {
			ValueExceedLimitDialogFragment.showInstance(getChildFragmentManager(), tagWithExceedingValue);
		} else if (TextUtils.isEmpty(poiTypeEditText.getText())) {
			if (Algorithms.isEmpty(editPoiData.getTag(OSMTagKey.ADDR_HOUSE_NUMBER.getValue()))) {
				int messageId = R.string.save_poi_without_poi_type_message;
				SaveExtraValidationDialogFragment.showInstance(getChildFragmentManager(), messageId);
			} else {
				save();
			}
		} else if (testTooManyCapitalLetters(editPoiData.getTag(OSMTagKey.NAME.getValue()))) {
			int messageId = R.string.save_poi_too_many_uppercase;
			SaveExtraValidationDialogFragment.showInstance(getChildFragmentManager(), messageId);
		} else if (editPoiData.getPoiCategory() == app.getPoiTypes().getOtherPoiCategory()) {
			poiTypeEditText.setError(getString(R.string.please_specify_poi_type));
		} else if (editPoiData.getPoiTypeDefined() == null) {
			poiTypeEditText.setError(getString(R.string.please_specify_poi_type_only_from_list));
		} else {
			save();
		}
	}

	private String isTextLengthInRange() {
		for (Entry<String, String> s : editPoiData.getTagValues().entrySet()) {
			if (!Algorithms.isEmpty(s.getValue()) && s.getValue().length() > AMENITY_TEXT_LENGTH) {
				return s.getKey();
			}
		}
		return "";
	}

	private boolean testTooManyCapitalLetters(String name) {
		if (name == null) {
			return false;
		}
		int capital = 0;
		int lower = 0;
		int nonalpha = 0;
		for (int i = 0; i < name.length(); i++) {
			char c = name.charAt(i);
			if (Character.isLetter(c) || Character.getType(c) == Character.LETTER_NUMBER) {
				if (Character.isUpperCase(c)) {
					capital++;
				} else {
					lower++;
				}
			} else {
				nonalpha++;
			}
		}
		return capital > nonalpha && capital > lower;
	}

	public void save() {
		Entity original = editPoiData.getEntity();
		boolean offlineEdit = openstreetmapUtil instanceof OpenstreetmapLocalUtil;
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
					!tag.getKey().equals(POI_TYPE_TAG)) {
				entity.putTagNoLC(tag.getKey(), tag.getValue());
			}
		}
		String poiTypeTag = editPoiData.getTagValues().get(POI_TYPE_TAG);
		String comment = "";
		if (poiTypeTag != null) {
			PoiType poiType = editPoiData.getAllTranslatedSubTypes().get(poiTypeTag.trim().toLowerCase());
			if (poiType != null) {
				entity.putTagNoLC(poiType.getEditOsmTag(), poiType.getEditOsmValue());
				entity.removeTag(Entity.REMOVE_TAG_PREFIX + poiType.getEditOsmTag());
				if (poiType.getOsmTag2() != null) {
					entity.putTagNoLC(poiType.getOsmTag2(), poiType.getOsmValue2());
					entity.removeTag(Entity.REMOVE_TAG_PREFIX + poiType.getOsmTag2());
				}
				if (poiType.getEditOsmTag2() != null) {
					entity.putTagNoLC(poiType.getEditOsmTag2(), poiType.getEditOsmValue2());
					entity.removeTag(Entity.REMOVE_TAG_PREFIX + poiType.getEditOsmTag2());
				}
			} else if (!Algorithms.isEmpty(poiTypeTag)) {
				PoiCategory category = editPoiData.getPoiCategory();
				if (category != null) {
					entity.putTagNoLC(category.getDefaultTag(), poiTypeTag);
				}
			}
			if (offlineEdit && !Algorithms.isEmpty(poiTypeTag)) {
				entity.putTagNoLC(POI_TYPE_TAG, poiTypeTag);
			}
			String actionString = action == Action.CREATE ? getString(R.string.default_changeset_add) : getString(R.string.default_changeset_edit);
			comment = actionString + " " + poiTypeTag;
		}
		commitEntity(action, entity, openstreetmapUtil.getEntityInfo(entity.getId()), comment, false,
				result -> {
					if (result != null) {
						if (offlineEdit) {
							List<OpenstreetmapPoint> points = plugin.getDBPOI().getOpenstreetmapPoints();
							if (getActivity() instanceof MapActivity && points.size() > 0) {
								OsmPoint point = points.get(points.size() - 1);
								MapActivity mapActivity = (MapActivity) getActivity();
								mapActivity.getContextMenu().showOrUpdate(
										new LatLon(point.getLatitude(), point.getLongitude()),
										plugin.getOsmEditsLayer(mapActivity).getObjectName(point), point);
								mapActivity.getMapLayers().getContextMenuLayer().updateContextMenu();
							}
						}

						if (getActivity() instanceof MapActivity) {
							((MapActivity) getActivity()).getMapView().refreshMap(true);
						}
						dismissAllowingStateLoss();
					} else {
						openstreetmapUtil = plugin.getPoiModificationLocalUtil();
						Button saveButton = view.findViewById(R.id.saveButton);
						saveButton.setText(openstreetmapUtil instanceof OpenstreetmapRemoteUtil
								? R.string.shared_string_upload : R.string.shared_string_save);
					}

					return false;
				}, getActivity(), openstreetmapUtil, action == Action.MODIFY ? editPoiData.getChangedTags() : null);
	}

	private void dismissCheckForChanges() {
		if (editPoiData.hasChanges()) {
			FragmentManager fragmentManager = getFragmentManager();
			if (fragmentManager != null) {
				AreYouSureBottomSheetDialogFragment.showInstance(fragmentManager, this);
			}
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

	public static void commitEntity(@NonNull Action action,
			@NonNull Entity entity,
			@Nullable EntityInfo info,
			@Nullable String comment,
			boolean closeChangeSet,
			@Nullable CallbackWithObject<Entity> callback,
			@NonNull FragmentActivity activity,
			@NonNull OpenstreetmapUtil osmUtil,
			@Nullable Set<String> changedTags) {
		if (info == null && Action.CREATE != action && osmUtil instanceof OpenstreetmapRemoteUtil) {
			AndroidUtils.getApp(activity).showToastMessage(R.string.poi_error_info_not_loaded);
			return;
		}
		CommitEntityTask task = new CommitEntityTask(activity, osmUtil, entity, action, info,
				comment, closeChangeSet, changedTags, callback);
		task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	public void setPoiCategory(PoiCategory type) {
		editPoiData.updateType(type);
		poiTypeEditText.setText(editPoiData.getPoiTypeString());
		setAdapterForPoiTypeEditText();
	}

	private void setAdapterForPoiTypeEditText() {
		Map<String, PoiType> subCategories = new LinkedHashMap<>();
		PoiCategory ct = editPoiData.getPoiCategory();
		if (ct != null) {
			for (PoiType s : ct.getPoiTypes()) {
				if (!s.isReference() && !s.isNotEditableOsm() && s.getBaseLangType() == null) {
					addMapEntryAdapter(subCategories, s.getTranslation(), s);
					if (!s.getKeyName().contains("osmand")) {
						addMapEntryAdapter(subCategories, s.getKeyName().replace('_', ' '), s);
					}
					if (!Algorithms.isEmpty(s.getEditOsmValue())) {
						addMapEntryAdapter(subCategories, s.getEditOsmValue().replace('_', ' '), s);
					}
				}
			}
		}
		for (Map.Entry<String, PoiType> s : editPoiData.getAllTranslatedSubTypes().entrySet()) {
			if (!s.getKey().contains("osmand")) {
				addMapEntryAdapter(subCategories, s.getKey(), s.getValue());
			}
		}
		ArrayAdapter<Object> adapter = new ArrayAdapter<>(getActivity(),
				R.layout.list_textview, subCategories.keySet().toArray());
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

	private void addMapEntryAdapter(Map<String, PoiType> subCategories, String key, PoiType v) {
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

	public static EditPoiDialogFragment createInstance(Entity entity, boolean isAddingPoi,
			Map<String, String> tagList) {
		EditPoiDialogFragment editPoiDialogFragment = new EditPoiDialogFragment();
		Bundle args = new Bundle();
		args.putSerializable(KEY_AMENITY_ENTITY, entity);
		args.putBoolean(IS_ADDING_POI, isAddingPoi);
		args.putSerializable(TAGS_LIST, (Serializable) Collections.unmodifiableMap(tagList));
		editPoiDialogFragment.setArguments(args);
		return editPoiDialogFragment;
	}

	public static void showEditInstance(@NonNull FragmentActivity activity,
			@NonNull MapObject mapObject) {
		OsmandApplication app = AndroidUtils.getApp(activity);
		LoadEntityTask task = new LoadEntityTask(app, mapObject, entity -> {
			if (entity != null) {
				EditPoiDialogFragment fragment = createInstance(entity, false);
				fragment.show(activity.getSupportFragmentManager(), TAG);
			} else {
				app.showToastMessage(R.string.poi_cannot_be_found);
			}
			return false;
		});
		task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	private final TextView.OnEditorActionListener mOnEditorActionListener =
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

	public interface OnSaveButtonClickListener {
		void onSaveButtonClick();
	}
}
