package net.osmand.plus.quickaction.actions;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.support.design.widget.TextInputLayout;
import android.support.v7.widget.SwitchCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
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
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import net.osmand.CallbackWithObject;
import net.osmand.data.LatLon;
import net.osmand.osm.AbstractPoiType;
import net.osmand.osm.MapPoiTypes;
import net.osmand.osm.PoiCategory;
import net.osmand.osm.PoiType;
import net.osmand.osm.edit.Entity;
import net.osmand.osm.edit.Node;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.osmedit.EditPoiData;
import net.osmand.plus.osmedit.EditPoiDialogFragment;
import net.osmand.plus.osmedit.OpenstreetmapLocalUtil;
import net.osmand.plus.osmedit.OpenstreetmapPoint;
import net.osmand.plus.osmedit.OpenstreetmapUtil;
import net.osmand.plus.osmedit.OsmEditingPlugin;
import net.osmand.plus.osmedit.OsmPoint;
import net.osmand.plus.osmedit.dialogs.PoiSubTypeDialogFragment;
import net.osmand.plus.quickaction.CreateEditActionDialog;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.render.RenderingIcons;
import net.osmand.util.Algorithms;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static net.osmand.plus.osmedit.AdvancedEditPoiFragment.addPoiToStringSet;
import static net.osmand.plus.osmedit.EditPoiData.POI_TYPE_TAG;

public class AddPOIAction extends QuickAction {
	public static final int TYPE = 13;
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
			final OsmandApplication application = (OsmandApplication) (context).getApplicationContext();
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
		PoiCategory category = getCategory(getAllTranslatedNames(context));
			if (category != null) {
				category.getIconKeyName();
				String res = category.getIconKeyName();
				if (res != null && RenderingIcons.containsBigIcon(res)) {
					return RenderingIcons.getBigIconResourceId(res);
				}
			}
		return super.getIconRes();
	}

	@Override
	public void execute(final MapActivity activity) {

		LatLon latLon = activity.getMapView()
				.getCurrentRotatedTileBox()
				.getCenterLatLon();

		OsmEditingPlugin plugin = OsmandPlugin.getPlugin(OsmEditingPlugin.class);
		if (plugin == null) return;
		Node node = new Node(latLon.getLatitude(), latLon.getLongitude(), -1);
		node.replaceTags(getTagsFromParams());
		EditPoiData editPoiData = new EditPoiData(node, activity.getMyApplication());
		if (Boolean.valueOf(getParams().get(KEY_DIALOG))) {
			Entity newEntity = editPoiData.getEntity();
			EditPoiDialogFragment editPoiDialogFragment =
					EditPoiDialogFragment.createInstance(newEntity, true, getTagsFromParams());
			editPoiDialogFragment.show(activity.getSupportFragmentManager(),
					EditPoiDialogFragment.TAG);
		} else {
			OpenstreetmapUtil mOpenstreetmapUtil;
			if (activity.getMyApplication().getSettings().OFFLINE_EDITION.get()
					|| !activity.getMyApplication().getSettings().isInternetConnectionAvailable(true)) {
				mOpenstreetmapUtil = plugin.getPoiModificationLocalUtil();
			} else {
				mOpenstreetmapUtil = plugin.getPoiModificationRemoteUtil();
			}

			final boolean offlineEdit = mOpenstreetmapUtil instanceof OpenstreetmapLocalUtil;
			Node newNode = new Node(node.getLatitude(), node.getLongitude(), node.getId());
			OsmPoint.Action action = newNode.getId() < 0 ? OsmPoint.Action.CREATE : OsmPoint.Action.MODIFY;
			for (Map.Entry<String, String> tag : editPoiData.getTagValues().entrySet()) {
				if (tag.getKey().equals(EditPoiData.POI_TYPE_TAG)) {
					final PoiType poiType = editPoiData.getAllTranslatedSubTypes().get(tag.getValue().trim().toLowerCase());
					if (poiType != null) {
						newNode.putTagNoLC(poiType.getEditOsmTag(), poiType.getEditOsmValue());
						if (poiType.getOsmTag2() != null) {
							newNode.putTagNoLC(poiType.getOsmTag2(), poiType.getOsmValue2());
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
					new CallbackWithObject<Entity>() {

						@Override
						public boolean processResult(Entity result) {
							if (result != null) {
								OsmEditingPlugin plugin = OsmandPlugin.getPlugin(OsmEditingPlugin.class);
								if (plugin != null && offlineEdit) {
									List<OpenstreetmapPoint> points = plugin.getDBPOI().getOpenstreetmapPoints();
									if (activity instanceof MapActivity && points.size() > 0) {
										OsmPoint point = points.get(points.size() - 1);
										activity.getContextMenu().showOrUpdate(
												new LatLon(point.getLatitude(), point.getLongitude()),
												plugin.getOsmEditsLayer(activity).getObjectName(point), point);
									}
								}

								if (activity instanceof MapActivity) {
									activity.getMapView().refreshMap(true);
								}
							} else {
//                                    OsmEditingPlugin plugin = OsmandPlugin.getPlugin(OsmEditingPlugin.class);
//                                    mOpenstreetmapUtil = plugin.getPoiModificationLocalUtil();
//                                    Button saveButton = (Button) view.findViewById(R.id.saveButton);
//                                    saveButton.setText(mOpenstreetmapUtil instanceof OpenstreetmapRemoteUtil
//                                            ? R.string.shared_string_upload : R.string.shared_string_save);
							}

							return false;
						}
					}, activity, mOpenstreetmapUtil, null);

		}
	}

	@Override
	public void setAutoGeneratedTitle(EditText title) {
		this.title = title;
	}

	@Override
	public void drawUI(final ViewGroup parent, final MapActivity activity) {
		final View view = LayoutInflater.from(parent.getContext())
				.inflate(R.layout.quick_action_add_poi_layout, parent, false);

		final OsmandApplication application = activity.getMyApplication();
		boolean isLightTheme = application.getSettings().isLightContent();
		Drawable deleteDrawable = application.getUIUtilities().getIcon(R.drawable.ic_action_remove_dark, isLightTheme);

		final LinearLayout editTagsLineaLayout =
				(LinearLayout) view.findViewById(R.id.editTagsList);

		final TagAdapterLinearLayoutHack mAdapter = new TagAdapterLinearLayoutHack(editTagsLineaLayout, getTagsFromParams(), deleteDrawable);
		// It is possible to not restart initialization every time, and probably move initialization to appInit
		HashSet<String> tagKeys = new HashSet<>();
		HashSet<String> valueKeys = new HashSet<>();
		for (AbstractPoiType abstractPoiType : getAllTranslatedNames(application).values()) {
			addPoiToStringSet(abstractPoiType, tagKeys, valueKeys);
		}
		addPoiToStringSet(getPoiTypes(activity).getOtherMapCategory(), tagKeys, valueKeys);
		tagKeys.addAll(EditPoiDialogFragment.BASIC_TAGS);
		mAdapter.setTagData(tagKeys.toArray(new String[tagKeys.size()]));
		mAdapter.setValueData(valueKeys.toArray(new String[valueKeys.size()]));
		Button addTagButton = (Button) view.findViewById(R.id.addTagButton);
		addTagButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				for (int i = 0; i < editTagsLineaLayout.getChildCount(); i++) {
					View item = editTagsLineaLayout.getChildAt(i);
					if (((EditText) item.findViewById(R.id.tagEditText)).getText().toString().isEmpty() &&
							((EditText) item.findViewById(R.id.valueEditText)).getText().toString().isEmpty())
						return;
				}
				mAdapter.addTagView("", "");
			}
		});

		mAdapter.updateViews();

		final TextInputLayout poiTypeTextInputLayout = (TextInputLayout) view.findViewById(R.id.poiTypeTextInputLayout);
		final AutoCompleteTextView poiTypeEditText = (AutoCompleteTextView) view.findViewById(R.id.poiTypeEditText);
		final SwitchCompat showDialog = (SwitchCompat) view.findViewById(R.id.saveButton);
//            showDialog.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
//                @Override
//                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//                    getParams().put(KEY_DIALOG, Boolean.toString(isChecked));
//                }
//            });
		showDialog.setChecked(Boolean.valueOf(getParams().get(KEY_DIALOG)));

		final String text = getTagsFromParams().get(POI_TYPE_TAG);
		poiTypeEditText.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}

			@Override
			public void afterTextChanged(Editable s) {
				String tp = s.toString();
				putTagIntoParams(POI_TYPE_TAG, tp);
				PoiCategory category = getCategory(getAllTranslatedNames(application));

				if (category != null) {
					poiTypeTextInputLayout.setHint(category.getTranslation());
				}

				String add = application.getString(R.string.shared_string_add);

				if (title != null) {

					if (prevType.equals(title.getText().toString())
							|| title.getText().toString().equals(activity.getString(getNameRes()))
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
			public boolean onTouch(final View v, MotionEvent event) {
				final EditText editText = (EditText) v;
				final int DRAWABLE_RIGHT = 2;
				if (event.getAction() == MotionEvent.ACTION_UP) {
					if (event.getX() >= (editText.getRight()
							- editText.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width()
							- editText.getPaddingRight())) {
						PoiCategory category = getCategory(getAllTranslatedNames(activity));
						PoiCategory tempPoiCategory = (category != null) ? category : getPoiTypes(activity).getOtherPoiCategory();
						PoiSubTypeDialogFragment f =
								PoiSubTypeDialogFragment.createInstance(tempPoiCategory);
						f.setOnItemSelectListener(new PoiSubTypeDialogFragment.OnItemSelectListener() {
							@Override
							public void select(String category) {
								poiTypeEditText.setText(category);
							}
						});

						CreateEditActionDialog parentFragment = (CreateEditActionDialog) activity.getSupportFragmentManager().findFragmentByTag(CreateEditActionDialog.TAG);
						f.show(activity.getSupportFragmentManager(), "PoiSubTypeDialogFragment");

						return true;
					}
				}
				return false;
			}
		});

		setUpAdapterForPoiTypeEditText(activity, getAllTranslatedNames(activity), poiTypeEditText);

		ImageButton onlineDocumentationButton =
				(ImageButton) view.findViewById(R.id.onlineDocumentationButton);
		onlineDocumentationButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				activity.startActivity(new Intent(Intent.ACTION_VIEW,
						Uri.parse("https://wiki.openstreetmap.org/wiki/Map_Features")));
			}
		});

		final int colorId = isLightTheme ? R.color.active_color_primary_light : R.color.active_color_primary_dark;
		final int color = activity.getResources().getColor(colorId);
		onlineDocumentationButton.setImageDrawable(activity.getMyApplication().getUIUtilities().getPaintedIcon(R.drawable.ic_action_help, color));
//            poiTypeEditText.setCompoundDrawables(null, null, activity.getMyApplication().getIconsCache().getPaintedIcon(R.drawable.ic_action_arrow_drop_down, color), null);

//            Button addTypeButton = (Button) view.findViewById(R.id.addTypeButton);
//            addTypeButton.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    PoiSubTypeDialogFragment f = PoiSubTypeDialogFragment.createInstance(poiTypes.getOtherPoiCategory());
//                    f.setOnItemSelectListener(new PoiSubTypeDialogFragment.OnItemSelectListener() {
//                        @Override
//                        public void select(String category) {
//                            putTagIntoParams(POI_TYPE_TAG, category);
//                        }
//                    });
//
//                    CreateEditActionDialog parentFragment = (CreateEditActionDialog) activity.getSupportFragmentManager().findFragmentByTag(CreateEditActionDialog.TAG);
//                    f.show(parentFragment.getChildFragmentManager(), "PoiSubTypeDialogFragment");
//                }
//            });

		parent.addView(view);
	}

	private void setUpAdapterForPoiTypeEditText(final MapActivity activity, final Map<String, PoiType> allTranslatedNames, final AutoCompleteTextView poiTypeEditText) {
		final Map<String, PoiType> subCategories = new LinkedHashMap<>();
//            PoiCategory ct = editPoiData.getPoiCategory();
//            if (ct != null) {
//                for (PoiType s : ct.getPoiTypes()) {
//                    if (!s.isReference() && !s.isNotEditableOsm() && s.getBaseLangType() == null) {
//                        addMapEntryAdapter(subCategories, s.getTranslation(), s);
//                        if(!s.getKeyName().contains("osmand")) {
//                            addMapEntryAdapter(subCategories, s.getKeyName().replace('_', ' '), s);
//                        }
//                    }
//                }
//            }
		for (Map.Entry<String, PoiType> s : allTranslatedNames.entrySet()) {
			addMapEntryAdapter(subCategories, s.getKey(), s.getValue());
		}
		final ArrayAdapter<Object> adapter;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			adapter = new ArrayAdapter<>(activity,
					R.layout.list_textview, subCategories.keySet().toArray());
		} else {
			TypedValue typedValue = new TypedValue();
			Resources.Theme theme = activity.getTheme();
			theme.resolveAttribute(android.R.attr.textColorSecondary, typedValue, true);
			final int textColor = typedValue.data;

			adapter = new ArrayAdapter<Object>(activity,
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
				setUpAdapterForPoiTypeEditText(activity, allTranslatedNames, poiTypeEditText);
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
			}
		});
	}

	private PoiCategory getCategory(Map<String, PoiType> allTranslatedNames) {
		String tp = getTagsFromParams().get(POI_TYPE_TAG);
		if (tp == null) return null;
		PoiType pt = allTranslatedNames.get(tp.toLowerCase());
		if (pt != null) {
			return pt.getCategory();
		} else
			return null;
	}

	private void addMapEntryAdapter(final Map<String, PoiType> subCategories, String key, PoiType v) {
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
				if (tag.getKey().equals(POI_TYPE_TAG)
						/*|| tag.getKey().equals(OSMSettings.OSMTagKey.NAME.getValue())*/)
					continue;
				addTagView(tag.getKey(), tag.getValue());
			}
		}

		public void addTagView(String tg, String vl) {
			View convertView = LayoutInflater.from(linearLayout.getContext())
					.inflate(R.layout.poi_tag_list_item, null, false);
			final AutoCompleteTextView tagEditText =
					(AutoCompleteTextView) convertView.findViewById(R.id.tagEditText);
			ImageButton deleteItemImageButton =
					(ImageButton) convertView.findViewById(R.id.deleteItemImageButton);
			deleteItemImageButton.setImageDrawable(deleteDrawable);
			final String[] previousTag = new String[]{tg};
			deleteItemImageButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					linearLayout.removeView((View) v.getParent());
					tagsData.remove(tagEditText.getText().toString());
					setTagsIntoParams(tagsData);
				}
			});
			final AutoCompleteTextView valueEditText =
					(AutoCompleteTextView) convertView.findViewById(R.id.valueEditText);
			tagEditText.setText(tg);
			tagEditText.setAdapter(tagAdapter);
			tagEditText.setThreshold(1);
			tagEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
				@Override
				public void onFocusChange(View v, boolean hasFocus) {
					if (!hasFocus) {
						String s = tagEditText.getText().toString();
						tagsData.remove(previousTag[0]);
						tagsData.put(s.toString(), valueEditText.getText().toString());
						previousTag[0] = s.toString();
						setTagsIntoParams(tagsData);
					} else {
						tagAdapter.getFilter().filter(tagEditText.getText());
					}
				}
			});

			valueEditText.setText(vl);
			valueEditText.addTextChangedListener(new TextWatcher() {
				@Override
				public void beforeTextChanged(CharSequence s, int start, int count, int after) {
				}

				@Override
				public void onTextChanged(CharSequence s, int start, int before, int count) {
				}

				@Override
				public void afterTextChanged(Editable s) {
					tagsData.put(tagEditText.getText().toString(), s.toString());
					setTagsIntoParams(tagsData);
				}
			});

			initAutocompleteTextView(valueEditText, valueAdapter);

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

	private static void initAutocompleteTextView(final AutoCompleteTextView textView,
												 final ArrayAdapter<String> adapter) {

		textView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (hasFocus) {
					adapter.getFilter().filter(textView.getText());
				}
			}
		});
	}

	@Override
	public boolean fillParams(View root, MapActivity activity) {
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
		return quickActions != null ? quickActions : new LinkedHashMap<String, String>();
	}

	private void setTagsIntoParams(Map<String, String> tags) {
		if (!tags.containsKey(POI_TYPE_TAG)) {
			Map<String, String> additionalTags = new HashMap<>(tags);
			tags.clear();
			tags.put(POI_TYPE_TAG, getTagsFromParams().get(POI_TYPE_TAG));
			tags.putAll(additionalTags);
		}
		getParams().put(KEY_TAG, new Gson().toJson(tags));
	}

	private void putTagIntoParams(String tag, String value) {
		Map<String, String> tagsFromParams = getTagsFromParams();
		tagsFromParams.put(tag, value);
		setTagsIntoParams(tagsFromParams);
	}
}
