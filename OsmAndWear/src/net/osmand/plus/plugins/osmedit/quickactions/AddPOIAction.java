package net.osmand.plus.plugins.osmedit.quickactions;

import static net.osmand.osm.edit.Entity.POI_TYPE_TAG;
import static net.osmand.plus.plugins.osmedit.fragments.AdvancedEditPoiFragment.addPoiToStringSet;
import static net.osmand.plus.quickaction.QuickActionIds.ADD_POI_ACTION_ID;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;

import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import net.osmand.data.LatLon;
import net.osmand.osm.AbstractPoiType;
import net.osmand.osm.MapPoiTypes;
import net.osmand.osm.PoiCategory;
import net.osmand.osm.PoiType;
import net.osmand.osm.edit.Entity;
import net.osmand.osm.edit.Node;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.osmedit.OsmEditingPlugin;
import net.osmand.plus.plugins.osmedit.data.EditPoiData;
import net.osmand.plus.plugins.osmedit.data.OpenstreetmapPoint;
import net.osmand.plus.plugins.osmedit.data.OsmPoint;
import net.osmand.plus.plugins.osmedit.data.OsmPoint.Action;
import net.osmand.plus.plugins.osmedit.dialogs.EditPoiDialogFragment;
import net.osmand.plus.plugins.osmedit.dialogs.PoiSubTypeDialogFragment;
import net.osmand.plus.plugins.osmedit.helpers.OpenstreetmapLocalUtil;
import net.osmand.plus.plugins.osmedit.helpers.OpenstreetmapUtil;
import net.osmand.plus.poi.PoiFilterUtils;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionType;
import net.osmand.plus.render.RenderingIcons;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.widgets.OsmandTextFieldBoxes;
import net.osmand.plus.widgets.tools.SimpleTextWatcher;
import net.osmand.util.Algorithms;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import studio.carbonylgroup.textfieldboxes.ExtendedEditText;

public class AddPOIAction extends QuickAction {

	public static final QuickActionType TYPE = new QuickActionType(ADD_POI_ACTION_ID,
			"osmpoi.add", AddPOIAction.class).
			nameRes(R.string.poi).iconRes(R.drawable.ic_action_plus_dark).
			category(QuickActionType.MY_PLACES).nameActionRes(R.string.shared_string_add);
	public static final String KEY_TAG = "key_tag";
	public static final String KEY_DIALOG = "dialog";

	private transient EditText title;
	private transient String prevType = "";
	private transient MapPoiTypes poiTypes;
	private transient Map<String, PoiType> allTranslatedNames;

	public AddPOIAction() {
		super(TYPE);
	}

	public AddPOIAction(QuickAction quickAction) {
		super(quickAction);
	}

	private MapPoiTypes getPoiTypes(Context context) {
		if (poiTypes == null) {
			OsmandApplication application = (OsmandApplication) (context).getApplicationContext();
			poiTypes = application.getPoiTypes();
		}
		return poiTypes;
	}

	private Map<String, PoiType> getAllTranslatedNames(Context context) {
		if (allTranslatedNames == null) {
			allTranslatedNames = getPoiTypes(context).getAllTranslatedNames(true);
		}
		return allTranslatedNames;
	}

	@Override
	public int getIconRes(Context context) {
		PoiType poiType = getPoiType(context);
		String iconName = PoiFilterUtils.getPoiTypeIconName(poiType);
		if (!Algorithms.isEmpty(iconName)) {
			return RenderingIcons.getBigIconResourceId(iconName);
		}
		PoiCategory poiCategory = getCategory(context);
		String categoryIconName = PoiFilterUtils.getPoiTypeIconName(poiCategory);
		return Algorithms.isEmpty(categoryIconName)
				? getIconRes()
				: RenderingIcons.getBigIconResourceId(categoryIconName);
	}

	@Nullable
	private PoiType getPoiType(@NonNull Context context) {
		String poiTypeTranslation = getPoiTypeTranslation();
		return poiTypeTranslation == null ? null : getAllTranslatedNames(context).get(poiTypeTranslation.toLowerCase());
	}

	@Override
	public void execute(@NonNull MapActivity mapActivity) {
		OsmandApplication app = mapActivity.getMyApplication();
		OsmandSettings settings = app.getSettings();
		OsmEditingPlugin plugin = PluginsHelper.getPlugin(OsmEditingPlugin.class);
		if (plugin == null) return;

		LatLon latLon = getMapLocation(mapActivity);

		Node node = new Node(latLon.getLatitude(), latLon.getLongitude(), -1);
		node.replaceTags(getTagsFromParams());
		EditPoiData editPoiData = new EditPoiData(node, mapActivity.getMyApplication());
		if (Boolean.parseBoolean(getParams().get(KEY_DIALOG)) || editPoiData.hasEmptyValue()) {
			Entity newEntity = editPoiData.getEntity();
			EditPoiDialogFragment editPoiDialogFragment =
					EditPoiDialogFragment.createInstance(newEntity, true, getTagsFromParams());
			editPoiDialogFragment.show(mapActivity.getSupportFragmentManager(),
					EditPoiDialogFragment.TAG);
		} else {
			OpenstreetmapUtil mOpenstreetmapUtil;
			if (plugin.OFFLINE_EDITION.get() || !settings.isInternetConnectionAvailable(true)) {
				mOpenstreetmapUtil = plugin.getPoiModificationLocalUtil();
			} else {
				mOpenstreetmapUtil = plugin.getPoiModificationRemoteUtil();
			}

			boolean offlineEdit = mOpenstreetmapUtil instanceof OpenstreetmapLocalUtil;
			Node newNode = new Node(node.getLatitude(), node.getLongitude(), node.getId());
			Action action = newNode.getId() < 0 ? OsmPoint.Action.CREATE : OsmPoint.Action.MODIFY;
			for (Map.Entry<String, String> tag : editPoiData.getTagValues().entrySet()) {
				if (tag.getKey().equals(POI_TYPE_TAG)) {
					PoiType poiType = editPoiData.getAllTranslatedSubTypes().get(tag.getValue().trim().toLowerCase());
					if (poiType != null) {
						newNode.putTagNoLC(poiType.getEditOsmTag(), poiType.getEditOsmValue());
						if (poiType.getOsmTag2() != null) {
							newNode.putTagNoLC(poiType.getOsmTag2(), poiType.getOsmValue2());
						}
						if (poiType.getEditOsmTag2() != null) {
							newNode.putTagNoLC(poiType.getEditOsmTag2(), poiType.getEditOsmValue2());
						}
					} else if (!Algorithms.isEmpty(tag.getValue())) {
						PoiCategory category = editPoiData.getPoiCategory();
						if (category != null) {
							newNode.putTagNoLC(category.getDefaultTag(), tag.getValue());
						}
					}
					if (offlineEdit && !Algorithms.isEmpty(tag.getValue())) {
						newNode.putTagNoLC(tag.getKey(), tag.getValue());
					}
				} else if (!Algorithms.isEmpty(tag.getKey()) && !Algorithms.isEmpty(tag.getValue())) {
					newNode.putTagNoLC(tag.getKey(), tag.getValue());
				}
			}
			EditPoiDialogFragment.commitEntity(action, newNode, mOpenstreetmapUtil.getEntityInfo(newNode.getId()), "", false,
					result -> {
						if (result != null) {
							OsmEditingPlugin plugin1 = PluginsHelper.getPlugin(OsmEditingPlugin.class);
							if (plugin1 != null && offlineEdit) {
								List<OpenstreetmapPoint> points = plugin1.getDBPOI().getOpenstreetmapPoints();
								if (points.size() > 0) {
									OsmPoint point = points.get(points.size() - 1);
									mapActivity.getContextMenu().showOrUpdate(
											new LatLon(point.getLatitude(), point.getLongitude()),
											plugin1.getOsmEditsLayer(mapActivity).getObjectName(point), point);
								}
							}
							mapActivity.getMapView().refreshMap(true);
						}
						return false;
					}, mapActivity, mOpenstreetmapUtil, null);

		}
	}

	@Override
	public void setAutoGeneratedTitle(EditText title) {
		this.title = title;
	}

	@SuppressLint("ClickableViewAccessibility")
	@Override
	public void drawUI(@NonNull ViewGroup parent, @NonNull MapActivity mapActivity) {
		View view = LayoutInflater.from(parent.getContext())
				.inflate(R.layout.quick_action_add_poi_layout, parent, false);

		OsmandApplication application = mapActivity.getMyApplication();
		boolean isLightTheme = application.getSettings().isLightContent();
		boolean isLayoutRtl = AndroidUtils.isLayoutRtl(mapActivity);
		Drawable deleteDrawable = application.getUIUtilities().getIcon(R.drawable.ic_action_remove_dark, isLightTheme);

		LinearLayout editTagsLineaLayout = view.findViewById(R.id.editTagsList);

		TagAdapterLinearLayoutHack mAdapter = new TagAdapterLinearLayoutHack(editTagsLineaLayout, getTagsFromParams(), deleteDrawable);
		// It is possible to not restart initialization every time, and probably move initialization to appInit
		HashSet<String> tagKeys = new HashSet<>();
		HashSet<String> valueKeys = new HashSet<>();
		for (AbstractPoiType abstractPoiType : getAllTranslatedNames(application).values()) {
			addPoiToStringSet(abstractPoiType, tagKeys, valueKeys);
		}
		addPoiToStringSet(getPoiTypes(mapActivity).getOtherMapCategory(), tagKeys, valueKeys);
		tagKeys.addAll(EditPoiDialogFragment.BASIC_TAGS);
		mAdapter.setTagData(tagKeys.toArray(new String[0]));
		mAdapter.setValueData(valueKeys.toArray(new String[0]));
		Button addTagButton = view.findViewById(R.id.addTagButton);
		addTagButton.setOnClickListener(v -> {
			for (int i = 0; i < editTagsLineaLayout.getChildCount(); i++) {
				View item = editTagsLineaLayout.getChildAt(i);
				if (((EditText) item.findViewById(R.id.tagEditText)).getText().toString().isEmpty() &&
						((EditText) item.findViewById(R.id.valueEditText)).getText().toString().isEmpty())
					return;
			}
			mAdapter.addTagView("", "");
		});

		mAdapter.updateViews();

		TextInputLayout poiTypeTextInputLayout = view.findViewById(R.id.poiTypeTextInputLayout);
		AutoCompleteTextView poiTypeEditText = view.findViewById(R.id.poiTypeEditText);
		SwitchCompat showDialog = view.findViewById(R.id.saveButton);
		showDialog.setChecked(Boolean.parseBoolean(getParams().get(KEY_DIALOG)));

		String text = getTagsFromParams().get(POI_TYPE_TAG);
		poiTypeEditText.addTextChangedListener(new SimpleTextWatcher() {
			@Override
			public void afterTextChanged(Editable s) {
				String tp = s.toString();
				putTagIntoParams(POI_TYPE_TAG, tp);
				PoiCategory category = getCategory(application);

				if (category != null) {
					poiTypeTextInputLayout.setHint(category.getTranslation());
				}

				String add = application.getString(R.string.shared_string_add);

				if (title != null) {

					if (prevType.equals(title.getText().toString())
							|| title.getText().toString().equals(mapActivity.getString(getNameRes()))
							|| title.getText().toString().equals((add + " "))) {

						if (!tp.isEmpty()) {

							title.setText(add + " " + tp);
							prevType = title.getText().toString();
						}
					}
				}
			}
		});
		poiTypeEditText.setText(text != null ? text : "");
		poiTypeEditText.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				EditText editText = (EditText) v;
				if (event.getAction() == MotionEvent.ACTION_UP) {
					final int DRAWABLE_END = 2;
					int expandBtnWidth = AndroidUtils.getCompoundDrawables(editText)[DRAWABLE_END].getBounds().width();

					boolean expandButtonPressed;
					if (isLayoutRtl) {
						expandButtonPressed = event.getX() <= (editText.getLeft() + expandBtnWidth
								+ editText.getPaddingLeft());
					} else {
						expandButtonPressed = event.getX() >= (editText.getRight() - expandBtnWidth
								- editText.getPaddingRight());
					}

					if (expandButtonPressed) {
						PoiCategory category = getCategory(mapActivity);
						PoiCategory tempPoiCategory = (category != null) ? category : getPoiTypes(mapActivity).getOtherPoiCategory();
						PoiSubTypeDialogFragment f =
								PoiSubTypeDialogFragment.createInstance(tempPoiCategory);
						f.setOnItemSelectListener(new PoiSubTypeDialogFragment.OnItemSelectListener() {
							@Override
							public void select(String category) {
								poiTypeEditText.setText(category);
							}
						});
						f.show(mapActivity.getSupportFragmentManager(), "PoiSubTypeDialogFragment");
						return true;
					}
				}
				return false;
			}
		});

		setUpAdapterForPoiTypeEditText(mapActivity, getAllTranslatedNames(mapActivity), poiTypeEditText);

		ImageButton onlineDocumentationButton = view.findViewById(R.id.onlineDocumentationButton);
		onlineDocumentationButton.setOnClickListener(v -> {
			String url = application.getString(R.string.url_osm_wiki_map_features);
			Uri uri = Uri.parse(url);
			Intent intent = new Intent(Intent.ACTION_VIEW, uri);
			AndroidUtils.startActivityIfSafe(mapActivity, intent);
		});

		int activeColor = ColorUtilities.getActiveColor(mapActivity, !isLightTheme);
		onlineDocumentationButton.setImageDrawable(mapActivity.getMyApplication().getUIUtilities().getPaintedIcon(R.drawable.ic_action_help, activeColor));

		parent.addView(view);
	}

	private void setUpAdapterForPoiTypeEditText(MapActivity activity,
	                                            Map<String, PoiType> allTranslatedNames,
	                                            AutoCompleteTextView poiTypeEditText) {
		Map<String, PoiType> subCategories = new LinkedHashMap<>();
		for (Map.Entry<String, PoiType> s : allTranslatedNames.entrySet()) {
			addMapEntryAdapter(subCategories, s.getKey(), s.getValue());
		}
		ArrayAdapter<Object> adapter;
		adapter = new ArrayAdapter<>(activity, R.layout.list_textview, subCategories.keySet().toArray());
		adapter.sort((lhs, rhs) -> lhs.toString().compareTo(rhs.toString()));
		poiTypeEditText.setAdapter(adapter);
		poiTypeEditText.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				Object item = parent.getAdapter().getItem(position);
				poiTypeEditText.setText(item.toString());
				setUpAdapterForPoiTypeEditText(activity, allTranslatedNames, poiTypeEditText);
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
			}
		});
	}

	@Nullable
	private PoiCategory getCategory(@NonNull Context context) {
		PoiType poiType = getPoiType(context);
		return poiType != null ? poiType.getCategory() : null;
	}

	private void addMapEntryAdapter(Map<String, PoiType> subCategories, String key, PoiType v) {
		if (!subCategories.containsKey(key.toLowerCase())) {
			subCategories.put(Algorithms.capitalizeFirstLetterAndLowercase(key), v);
		}
	}

	private class TagAdapterLinearLayoutHack {
		private final LinearLayout linearLayout;
		private final Map<String, String> tagsData;
		private final ArrayAdapter<String> tagAdapter;
		private final ArrayAdapter<String> valueAdapter;
		private final Drawable deleteDrawable;

		public TagAdapterLinearLayoutHack(LinearLayout linearLayout,
		                                  Map<String, String> tagsData,
		                                  Drawable deleteDrawable) {
			this.linearLayout = linearLayout;
			this.tagsData = tagsData;
			this.deleteDrawable = deleteDrawable;

			tagAdapter = new ArrayAdapter<>(linearLayout.getContext(), R.layout.list_textview);
			valueAdapter = new ArrayAdapter<>(linearLayout.getContext(), R.layout.list_textview);
		}

		public void updateViews() {
			linearLayout.removeAllViews();
			List<Map.Entry<String, String>> entries = new ArrayList<>(tagsData.entrySet());
			for (Map.Entry<String, String> tag : entries) {
				if (POI_TYPE_TAG.equals(tag.getKey())) {
					continue;
				}
				addTagView(tag.getKey(), tag.getValue());
			}
		}

		public void addTagView(String tg, String vl) {
			View convertView = LayoutInflater.from(linearLayout.getContext())
					.inflate(R.layout.list_item_poi_tag, null, false);

			OsmandTextFieldBoxes tagFB = convertView.findViewById(R.id.tag_fb);
			tagFB.setClearButton(deleteDrawable);
			tagFB.hideClearButton();

			OsmandTextFieldBoxes valueFB = convertView.findViewById(R.id.value_fb);
			valueFB.setClearButton(deleteDrawable);
			valueFB.hideClearButton();

			ExtendedEditText tagEditText = convertView.findViewById(R.id.tagEditText);
			View deleteButton = convertView.findViewById(R.id.delete_button);
			String[] previousTag = {tg};
			deleteButton.setOnClickListener(v -> {
				linearLayout.removeView(convertView);
				tagsData.remove(tagEditText.getText().toString());
				setTagsIntoParams(tagsData);
			});
			ExtendedEditText valueEditText = convertView.findViewById(R.id.valueEditText);
			tagEditText.setText(tg);
			tagEditText.setAdapter(tagAdapter);
			tagEditText.setThreshold(1);
			tagEditText.setOnFocusChangeListener((v, hasFocus) -> {
				if (!hasFocus) {
					tagFB.hideClearButton();
					String s = tagEditText.getText().toString();
					tagsData.remove(previousTag[0]);
					tagsData.put(s, valueEditText.getText().toString());
					previousTag[0] = s;
					setTagsIntoParams(tagsData);
				} else {
					tagFB.showClearButton();
					tagAdapter.getFilter().filter(tagEditText.getText());
				}
			});

			valueEditText.setText(vl);
			valueEditText.addTextChangedListener(new SimpleTextWatcher() {
				@Override
				public void afterTextChanged(Editable s) {
					tagsData.put(tagEditText.getText().toString(), s.toString());
					setTagsIntoParams(tagsData);
				}
			});

			valueEditText.setOnFocusChangeListener((v, hasFocus) -> {
				if (hasFocus) {
					valueFB.showClearButton();
					valueAdapter.getFilter().filter(valueEditText.getText());
				} else {
					valueFB.hideClearButton();
				}
			});

			linearLayout.addView(convertView);
			tagEditText.requestFocus();
		}

		public void setTagData(String[] tags) {
			tagAdapter.clear();
			for (String s : tags) {
				tagAdapter.add(s);
			}
			tagAdapter.sort(String.CASE_INSENSITIVE_ORDER);
			tagAdapter.notifyDataSetChanged();
		}

		public void setValueData(String[] values) {
			valueAdapter.clear();
			for (String s : values) {
				valueAdapter.add(s);
			}
			valueAdapter.sort(String.CASE_INSENSITIVE_ORDER);
			valueAdapter.notifyDataSetChanged();
		}
	}

	@Override
	public boolean fillParams(@NonNull View root, @NonNull MapActivity mapActivity) {
		getParams().put(KEY_DIALOG, Boolean.toString(((SwitchCompat) root.findViewById(R.id.saveButton)).isChecked()));
		return !getParams().isEmpty() && (getParams().get(KEY_TAG) != null || !getTagsFromParams().isEmpty());
	}

	private Map<String, String> getTagsFromParams() {
		Map<String, String> quickActions = null;
		if (getParams().get(KEY_TAG) != null) {
			String json = getParams().get(KEY_TAG);
			Type type = new TypeToken<LinkedHashMap<String, String>>() {
			}.getType();
			quickActions = new Gson().fromJson(json, type);
		}
		return quickActions != null ? quickActions : new LinkedHashMap<>();
	}

	private void setTagsIntoParams(Map<String, String> tags) {
		if (!tags.containsKey(POI_TYPE_TAG)) {
			Map<String, String> additionalTags = new HashMap<>(tags);
			tags.clear();
			tags.put(POI_TYPE_TAG, getPoiTypeTranslation());
			tags.putAll(additionalTags);
		}
		getParams().put(KEY_TAG, new Gson().toJson(tags));
	}

	private void putTagIntoParams(String tag, String value) {
		Map<String, String> tagsFromParams = getTagsFromParams();
		tagsFromParams.put(tag, value);
		setTagsIntoParams(tagsFromParams);
	}

	@Nullable
	private String getPoiTypeTranslation() {
		return getTagsFromParams().get(POI_TYPE_TAG);
	}
}