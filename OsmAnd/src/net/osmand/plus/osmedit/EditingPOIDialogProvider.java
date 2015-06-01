package net.osmand.plus.osmedit;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;
import net.osmand.access.AccessibleToast;
import net.osmand.data.Amenity;
import net.osmand.osm.MapPoiTypes;
import net.osmand.osm.MapRenderingTypes;
import net.osmand.osm.PoiCategory;
import net.osmand.osm.PoiType;
import net.osmand.osm.edit.EntityInfo;
import net.osmand.osm.edit.EntityParser;
import net.osmand.osm.edit.Node;
import net.osmand.osm.edit.OSMSettings.OSMTagKey;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.DialogProvider;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.OpeningHoursView;
import net.osmand.util.OpeningHoursParser;
import net.osmand.util.OpeningHoursParser.BasicOpeningHourRule;
import net.osmand.util.OpeningHoursParser.OpeningHours;
import net.osmand.util.OpeningHoursParser.OpeningHoursRule;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class EditingPOIDialogProvider implements DialogProvider {
	
	private final Activity activity;
	private final OsmEditingPlugin plugin;
	private OpenstreetmapUtil openstreetmapUtil;
	private OpenstreetmapUtil openstreetmapUtilToLoad;
	private AutoCompleteTextView typeText;
	private EditText nameText;
	private Button typeButton;
	private TextView osmTagValue;
	private Button openHoursButton;
	private EditText openingHours;
	private EditText commentText;
	private EditText websiteText;
	private EditText phoneText;
	private EditText streetNameText;
	private EditText hnoText;
	private CheckBox closeChange;

//	private final static Log log = LogUtil.getLog(EditingPOIActivity.class);

	/* dialog stuff */
	public static final int DIALOG_PLUGIN = 600;
	private static final int DIALOG_CREATE_POI = DIALOG_PLUGIN + 0;
	private static final int DIALOG_EDIT_POI = DIALOG_PLUGIN + 1;
	protected static final int DIALOG_SUB_CATEGORIES = DIALOG_PLUGIN + 2; 
	protected static final int DIALOG_POI_TYPES = DIALOG_PLUGIN + 3;
	private static final int DIALOG_DELETE_POI = DIALOG_PLUGIN + 4;
	private static final int DIALOG_OPENING_HOURS = DIALOG_PLUGIN + 5;

	private static final String KEY_AMENITY_NODE = "amenity_node";
	private static final String KEY_AMENITY = "amenity";

	private static Bundle dialogBundle = new Bundle();
	private OsmandSettings settings;
	private MapPoiTypes poiTypes;
	private boolean isLocalEdit;
	private Map<String, PoiType> allTranslatedSubTypes;
	

	public EditingPOIDialogProvider(MapActivity uiContext, OsmEditingPlugin plugin) {
		this.activity = uiContext;
		this.plugin = plugin;
		
	}

	private void prepareProvider() {
		poiTypes = ((OsmandApplication) activity.getApplication()).getPoiTypes();
		allTranslatedSubTypes = poiTypes.getAllTranslatedNames(false);
		settings = ((OsmandApplication) activity.getApplication()).getSettings();
		isLocalEdit = true;
		if (settings.OFFLINE_EDITION.get() || !settings.isInternetConnectionAvailable(true)) {
			this.openstreetmapUtil = new OpenstreetmapLocalUtil(plugin, activity);
			this.openstreetmapUtilToLoad = openstreetmapUtil;
		} else if(!settings.isInternetConnectionAvailable(true)) {
			this.openstreetmapUtil = new OpenstreetmapLocalUtil(plugin, activity);
			this.openstreetmapUtilToLoad = new OpenstreetmapRemoteUtil(activity);
		} else {
			isLocalEdit = false;
			this.openstreetmapUtil = new OpenstreetmapRemoteUtil(activity);
			this.openstreetmapUtilToLoad = openstreetmapUtil;	
		}
	}
	
	public void showEditDialog(final Amenity editA){
		prepareProvider();
		new AsyncTask<Void, Void, Node>() {

			@Override
			protected Node doInBackground(Void... params) {
				return openstreetmapUtilToLoad.loadNode(editA);
			}
			
			protected void onPostExecute(Node n) {
				if(n != null){
					showPOIDialog(DIALOG_EDIT_POI, n, editA.getType(), editA.getSubType());
				} else {
					AccessibleToast.makeText(activity, activity.getString(R.string.poi_error_poi_not_found), Toast.LENGTH_SHORT).show();
				}
			};
			
		}.execute(new Void[0]);
	}
	
	public void showCreateDialog(double latitude, double longitude){
		prepareProvider();
		Node n = new Node(latitude, longitude, -1);
		n.putTag(OSMTagKey.OPENING_HOURS.getValue(), ""); //$NON-NLS-1$
		showPOIDialog(DIALOG_CREATE_POI, n, poiTypes.getOtherPoiCategory(), "");
	}

	private void showPOIDialog(int dialogID, Node n, PoiCategory type, String subType) {
		Amenity a = EntityParser.parseAmenity(n, type, subType, null, MapRenderingTypes.getDefault());
		dialogBundle.putSerializable(KEY_AMENITY, a);
		dialogBundle.putSerializable(KEY_AMENITY_NODE, n);
		createPOIDialog(dialogID, dialogBundle).show();
	}
	
	public void showDeleteDialog(final Amenity a){
		prepareProvider();
		new AsyncTask<Void, Void, Node>() {
			protected Node doInBackground(Void[] params) {
				return openstreetmapUtil.loadNode(a);
			};
			
			protected void onPostExecute(Node n) {
				if(n == null){
					AccessibleToast.makeText(activity, activity.getResources().getString(R.string.poi_error_poi_not_found), Toast.LENGTH_LONG).show();
					return;
				}
				dialogBundle.putSerializable(KEY_AMENITY, a);
				dialogBundle.putSerializable(KEY_AMENITY_NODE, n);
				activity.showDialog(DIALOG_DELETE_POI); //TODO from android 2.0 use showDialog(id,bundle)
			};
		}.execute(new Void[0]);
	}
	
	private void prepareDeleteDialog(Dialog dlg, Bundle args) {
		Amenity a = (Amenity) args.getSerializable(KEY_AMENITY);
		dlg.setTitle(MessageFormat.format(this.activity.getResources().getString(R.string.poi_remove_confirm_template), 
				OsmAndFormatter.getPoiStringWithoutType(a, settings.MAP_PREFERRED_LOCALE.get())));
	}
	
	private Dialog createDeleteDialog(final Bundle args) {
		Builder builder = new AlertDialog.Builder(activity);
		builder.setTitle(R.string.poi_remove_title);
		LinearLayout ll = new LinearLayout(activity);
		ll.setPadding(4, 2, 4, 0);
		ll.setOrientation(LinearLayout.VERTICAL);
		final EditText comment = new EditText(activity);
		comment.setText(R.string.poi_remove_title);
		ll.addView(comment);
		final CheckBox closeChangeset ;
		if (!isLocalEdit) {
			closeChangeset = new CheckBox(activity);
			closeChangeset.setText(R.string.close_changeset);
			ll.addView(closeChangeset);
		} else {
			closeChangeset = null;
		}
		builder.setView(ll);
		builder.setNegativeButton(R.string.shared_string_cancel, null);
		builder.setPositiveButton(R.string.shared_string_delete, new DialogInterface.OnClickListener(){
			@Override
			public void onClick(DialogInterface dialog, int which) {
				Node n = (Node) args.getSerializable(KEY_AMENITY_NODE);
				String c = comment.getText().toString();
				commitNode(OsmPoint.Action.DELETE, n, openstreetmapUtil.getEntityInfo(), c,
						closeChangeset == null ? false : closeChangeset.isSelected(), new Runnable() {
					@Override
					public void run() {
						if (isLocalEdit) {
							AccessibleToast.makeText(
									activity,R.string.osm_changes_added_to_local_edits,
									Toast.LENGTH_LONG).show();
						} else {
							AccessibleToast.makeText(activity, R.string.poi_remove_success, Toast.LENGTH_LONG).show();
						}
						if(activity instanceof MapActivity){
							((MapActivity) activity).getMapView().refreshMap(true);
						}						
					}
				});
			}
		});
		return builder.create();
	}

	private void preparePOIDialog(View dlg, Bundle args) {
		Amenity a = (Amenity) args.getSerializable(KEY_AMENITY);
		Node n = (Node) args.getSerializable(KEY_AMENITY_NODE);
		EditText nameText = ((EditText)dlg.findViewById(R.id.Name));
		nameText.setText(a.getName());
		EditText openingHours = ((EditText)dlg.findViewById(R.id.OpeningHours));
		openingHours.setText(a.getOpeningHours());
		EditText streetName = ((EditText)dlg.findViewById(R.id.StreetName));
		streetName.setText(n.getTag(OSMTagKey.ADDR_STREET));
		EditText houseNumber = ((EditText)dlg.findViewById(R.id.HouseNumber));
		houseNumber.setText(n.getTag(OSMTagKey.ADDR_HOUSE_NUMBER));
		EditText phoneText = ((EditText)dlg.findViewById(R.id.Phone));
		phoneText.setText(a.getPhone());
		EditText websiteText = ((EditText)dlg.findViewById(R.id.Website));
		websiteText.setText(a.getSite());
		final TableLayout layout = ((TableLayout)dlg.findViewById(R.id.advancedModeTable));
		layout.setVisibility(View.GONE);
		updateType(a);
	}
	
	private void addTagValueRow(final Node n, final TableLayout layout, String tg, String vl) {
		final TableRow newTagRow = new TableRow(activity);				            
        TableRow.LayoutParams tlp = new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT);          
        tlp.leftMargin = 5;
        tlp.gravity = Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM;
        newTagRow.setLayoutParams(tlp);

        final AutoCompleteTextView tag = new AutoCompleteTextView(activity);
        final AutoCompleteTextView value = new AutoCompleteTextView(activity);				            
        final Button delete = new Button(activity);
        
        tag.setDropDownWidth(ViewGroup.LayoutParams.MATCH_PARENT);
        value.setDropDownWidth(ViewGroup.LayoutParams.MATCH_PARENT);
        tag.setLayoutParams(tlp);
        if(tg != null) {
        	tag.setText(tg);
        } else {
        	tag.setHint("Tag");
        }

        final Set<String> tagKeys = new TreeSet<String>();
		for (OSMTagKey t : OSMTagKey.values()) {
			if ((t != OSMTagKey.NAME) && (t != OSMTagKey.OPENING_HOURS) && (t != OSMTagKey.PHONE)
					&& (t != OSMTagKey.WEBSITE)) {
				tagKeys.add(t.getValue());
			}
		}
		ArrayAdapter<Object> adapter = new ArrayAdapter<Object>(activity, R.layout.list_textview, tagKeys.toArray());
        tag.setAdapter(adapter);
        tag.setThreshold(1);
        tag.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Builder builder = new AlertDialog.Builder(activity);
				final String[] tags = tagKeys.toArray(new String[tagKeys.size()]);
				builder.setItems(tags, new Dialog.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						tag.setText(tags[which]);
					}
					
				});		
				builder.create();
				builder.show();
			}
		});			            
        tlp = new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.FILL_PARENT);
        tlp.leftMargin = 5;
        tlp.gravity = Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM;
        value.setLayoutParams(tlp);
        if(vl != null) {
        	value.setText(vl);
        } else {
        	value.setHint("Value");
        }
        Set<String> subCategories = new LinkedHashSet<String>();
        // could be osm values
//		for (String s : poiTypes.getAllTranslatedNames().keySet()) {
//			if (!subCategories.contains(s)) {
//				subCategories.add(s);
//			}
//		} ;
        ArrayAdapter<Object> valueAdapter = new ArrayAdapter<Object>(activity, R.layout.list_textview, subCategories.toArray());
        value.setThreshold(1);
        value.setAdapter(valueAdapter);
		value.addTextChangedListener(new TextWatcher() {
			@Override
			public void afterTextChanged(Editable s) {
				if ((newTagRow != null) && (tag != null) && (value != null) && (tag.getText() != null)
						&& (value.getText() != null) && (!tag.getText().equals("")) && (!value.getText().equals(""))) {
					n.putTag(tag.getText().toString(), value.getText().toString());
				}
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}
		});
        tlp = new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT);
        tlp.gravity = Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM;
        tlp.rightMargin = 5;
        delete.setLayoutParams(tlp);
        delete.setText("X");
        delete.setOnClickListener(new View.OnClickListener() {
        	@Override
        	public void onClick(View v) {
        		layout.removeView(newTagRow);
        		layout.invalidate();
        		n.removeTag(tag.getText().toString());
        	}
        });      
        newTagRow.addView(tag);
        newTagRow.addView(value);
        newTagRow.addView(delete);			            
        layout.addView(newTagRow);
        layout.invalidate();
	}
	
	private Builder createPOIDialog(final int dialogID, Bundle args) {
		final View view = activity.getLayoutInflater().inflate(R.layout.editing_poi, null);
		final Builder dlg = new Builder(activity);
		dlg.setView(view);
		switch (dialogID) {
			case DIALOG_CREATE_POI:
				dlg.setTitle(R.string.poi_create_title);
				break;
			case DIALOG_EDIT_POI:
				dlg.setTitle(R.string.poi_edit_title);
				break;
		}

		//dlg.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);;
		
		nameText = ((EditText)view.findViewById(R.id.Name));
		openingHours = ((EditText)view.findViewById(R.id.OpeningHours));
		typeText = ((AutoCompleteTextView)view.findViewById(R.id.Type));
		typeButton = ((Button)view.findViewById(R.id.TypeButton));
		osmTagValue = ((TextView) view.findViewById(R.id.OsmTagValue));
		openHoursButton = ((Button)view.findViewById(R.id.OpenHoursButton));
		typeText = ((AutoCompleteTextView)view.findViewById(R.id.Type));
		typeText.setThreshold(1);
		commentText = ((EditText)view.findViewById(R.id.Comment));
		phoneText = ((EditText)view.findViewById(R.id.Phone));
		hnoText = ((EditText)view.findViewById(R.id.HouseNumber));
		streetNameText = ((EditText)view.findViewById(R.id.StreetName));
		websiteText = ((EditText)view.findViewById(R.id.Website));
		closeChange = ((CheckBox) view.findViewById(R.id.CloseChangeset));
		closeChange.setVisibility(isLocalEdit ? View.GONE : View.VISIBLE);


		TextView linkToOsmDoc = (TextView) view.findViewById(R.id.LinkToOsmDoc);
		linkToOsmDoc.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://wiki.openstreetmap.org/wiki/Map_Features")));
			}
		});
		linkToOsmDoc.setMovementMethod(LinkMovementMethod.getInstance());

		final Amenity a = (Amenity) args.getSerializable(KEY_AMENITY);
		final Node n = (Node) args.getSerializable(KEY_AMENITY_NODE);
		dlg.setNegativeButton(R.string.shared_string_cancel, null);
		dlg.setPositiveButton(
				isLocalEdit ? R.string.shared_string_save :
					R.string.default_buttons_commit, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				Resources resources = view.getResources();
				final String msg = n.getId() == -1 ? resources.getString(R.string.poi_action_add) : resources
						.getString(R.string.poi_action_change);
				OsmPoint.Action action = n.getId() == -1 ? OsmPoint.Action.CREATE : OsmPoint.Action.MODIFY;
				String subType = typeText.getText().toString();
				if (allTranslatedSubTypes.get(subType.trim()) != null) {
					PoiType pt = allTranslatedSubTypes.get(subType);
					n.putTag(pt.getOsmTag(), pt.getOsmValue());
					if (pt.getOsmTag2() != null) {
						n.putTag(pt.getOsmTag2(), pt.getOsmValue2());
					}
				} else {
					n.putTag(a.getType().getDefaultTag(), subType);
				}
				String name = nameText.getText().toString();
				if(name.length() > 0) {
					n.putTag(OSMTagKey.NAME.getValue(), name);
				}
				if (openingHours.getText().toString().length() == 0) {
					n.removeTag(OSMTagKey.OPENING_HOURS.getValue());
				} else {
					n.putTag(OSMTagKey.OPENING_HOURS.getValue(), openingHours.getText().toString());
				}
				String website = websiteText.getText().toString();
				if (website.length() > 0 ){
					n.putTag(OSMTagKey.WEBSITE.getValue(),website);
				} else {
					n.removeTag(OSMTagKey.WEBSITE.getValue());
				}
				String phone = phoneText.getText().toString();
				if (phone.length() > 0 ){
					n.putTag(OSMTagKey.PHONE.getValue(),phone);
				} else {
					n.removeTag(OSMTagKey.PHONE.getValue());
				}
				String str = streetNameText.getText().toString();
				if (str .length() > 0 ){
					n.putTag(OSMTagKey.ADDR_STREET.getValue(),str);
				} else {
					n.removeTag(OSMTagKey.ADDR_STREET.getValue());
				}
				String hno = hnoText.getText().toString();
				if (hno .length() > 0 ){
					n.putTag(OSMTagKey.ADDR_HOUSE_NUMBER.getValue(),hno);
				} else {
					n.removeTag(OSMTagKey.ADDR_HOUSE_NUMBER.getValue());
				}
				commitNode(action, n, openstreetmapUtil.getEntityInfo(), commentText.getText().toString(), closeChange.isSelected(),
						new Runnable() {
							@Override
									public void run() {
										if (isLocalEdit) {
											AccessibleToast.makeText(
													activity,R.string.osm_changes_added_to_local_edits,
													Toast.LENGTH_LONG).show();
										} else {
											AccessibleToast.makeText(
													activity,
													MessageFormat.format(
															activity.getResources().getString(
																	R.string.poi_action_succeded_template), msg),
													Toast.LENGTH_LONG).show();
										}
										if (activity instanceof MapActivity) {
											((MapActivity) activity).getMapView().refreshMap(true);
										}
										activity.removeDialog(dialogID);
									}
						});
			}
		});
		preparePOIDialog(view, args);
		attachListeners(view, a, n);
		updateOsmTagValue(a);
		return dlg;
	}
	
	private void updateOsmTagValue(final Amenity a) {
		String subType = typeText.getText().toString();
		String s = "OSM ";
		if (allTranslatedSubTypes.get(subType.trim()) != null) {
			PoiType pt = allTranslatedSubTypes.get(subType);
			s = pt.getOsmTag() + "=" + pt.getOsmValue();
			if (pt.getOsmTag2() != null) {
				s += " " + pt.getOsmTag2() + "=" + pt.getOsmValue2();
			}
		} else {
			s += a.getType().getDefaultTag() + "=" + subType;
		}
		osmTagValue.setText(s);
	}

	private void attachListeners(final View dlg, final Amenity a, final Node n) {
		// DO NOT show on focus with empty text predefined list of subcategories - problems when rotating
		typeText.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				showSubCategory(a);
			}
		});
		typeText.setOnLongClickListener(new View.OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				showSubCategory(a);
				return true;
			}
		});
		openHoursButton.setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(View v) {
				activity.showDialog(DIALOG_OPENING_HOURS);
			}
		});
		typeText.addTextChangedListener(new TextWatcher(){

			@Override
			public void afterTextChanged(Editable s) {
				String str = s.toString();
				a.setSubType(str);
				PoiType st = allTranslatedSubTypes.get(str);
				if(st != null && a.getType() != st.getCategory() && st.getCategory() != null){
					a.setType(st.getCategory());
					typeButton.setText(st.getCategory().getTranslation());
					updateSubTypesAdapter(st.getCategory());
				}
				updateOsmTagValue(a);
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}
		});

		typeButton.setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(View v) {
				activity.showDialog(DIALOG_POI_TYPES);
			}
		});
		
		final Button advancedModeButton = ((Button)dlg.findViewById(R.id.advancedMode));
		advancedModeButton.setOnClickListener(new View.OnClickListener() {	
			@Override
			public void onClick(View v) {
				final TableLayout layout = ((TableLayout) dlg.findViewById(R.id.advancedModeTable));
				TableLayout.LayoutParams tlParams = new TableLayout.LayoutParams(TableLayout.LayoutParams.FILL_PARENT,
						TableLayout.LayoutParams.WRAP_CONTENT);
				layout.setLayoutParams(tlParams);
				layout.setColumnStretchable(1, true);
				layout.setVisibility((layout.getVisibility() == View.VISIBLE) ? View.GONE : View.VISIBLE);
				Button addTag = (Button) dlg.findViewById(R.id.addTag);
				addTag.setVisibility((layout.getVisibility() == View.VISIBLE) ? View.VISIBLE : View.GONE);
				if (layout.getVisibility() == View.VISIBLE) {
					addTag.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							addTagValueRow(n, layout, null, null);
						}
					});
				}
				while (layout.getChildCount() > 0) {
					layout.removeViewAt(0);
				}
				layout.requestLayout();
				for (String tg : n.getTagKeySet()) {
					if (!tg.equals(OSMTagKey.NAME.getValue()) && !tg.equals(OSMTagKey.OPENING_HOURS.getValue())
							&& !tg.equals(OSMTagKey.PHONE.getValue()) && !tg.equals(OSMTagKey.WEBSITE.getValue())) {
						if(a == null || a.getType() == null || !a.getType().getDefaultTag().equals(tg)) {
							addTagValueRow(n, layout, tg, n.getTag(tg));
						}
					}
				}
			}
		});
		
	}

	private void showSubCategory(Amenity a) {
		if(typeText.getText().length() == 0 && a.getType() != null){
			activity.showDialog(DIALOG_SUB_CATEGORIES);
		}
	}

	private void updateSubTypesAdapter(PoiCategory poiCategory) {
		final Map<String, PoiType> subCategories = getSubCategoriesMap(poiCategory);
		final ArrayAdapter<Object> adapter = new ArrayAdapter<Object>(activity, R.layout.list_textview, subCategories.keySet().toArray());
		typeText.setAdapter(adapter);
		typeText.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				Object item = parent.getAdapter().getItem(position);
				if(subCategories.containsKey(item)) {
					String kn = subCategories.get(item).getKeyName();
					typeText.setText(kn);
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
			}
		});
	}

	private Map<String, PoiType> getSubCategoriesMap(PoiCategory poiCategory) {
		Map<String, PoiType> subCategories = new LinkedHashMap<>(poiTypes.getAllTranslatedNames(poiCategory, false));
		for (Map.Entry<String, PoiType> s : allTranslatedSubTypes.entrySet()) {
			if (!subCategories.containsKey(s.getKey())) {
				subCategories.put(s.getKey(), s.getValue());
			}
		}
		ArrayAdapter<Object> adapter = new ArrayAdapter<Object>(activity, R.layout.list_textview, 
				subCategories.keySet().toArray());
		typeText.setAdapter(adapter);
		return subCategories;
	}
	
	private void updateType(Amenity a){
		typeText.setText(a.getSubType());
		typeButton.setText(a.getType().getTranslation());
		updateSubTypesAdapter(a.getType());
		updateOsmTagValue(a);
	}
	

	private Dialog createOpenHoursDlg(){
		OpeningHours time = OpeningHoursParser.parseOpenedHours(openingHours.getText().toString());
		if(time == null){
			AccessibleToast.makeText(activity, activity.getString(R.string.opening_hours_not_supported), Toast.LENGTH_LONG).show();
			return null;
		}
		
		List<BasicOpeningHourRule> simple = null;
		if(time != null){
			simple = new ArrayList<BasicOpeningHourRule>();
			for(OpeningHoursRule r : time.getRules()){
				if(r instanceof BasicOpeningHourRule){
					simple.add((BasicOpeningHourRule) r);
				} else {
					time = null;
					break;
				}
			}
		}
		
		Builder builder = new AlertDialog.Builder(activity);
		final OpeningHoursView v = new OpeningHoursView(activity);
		builder.setView(v.createOpeningHoursEditView(simple));
		builder.setPositiveButton(activity.getString(R.string.shared_string_apply), new DialogInterface.OnClickListener(){
			@Override
			public void onClick(DialogInterface dialog, int which) {
				OpeningHours oh = new OpeningHours((ArrayList<OpeningHoursRule>) v.getTime());
				openingHours.setText(oh.toString());
				activity.removeDialog(DIALOG_OPENING_HOURS);
			}
		});
		builder.setNegativeButton(activity.getString(R.string.shared_string_cancel), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				activity.removeDialog(DIALOG_OPENING_HOURS);
			}
		});
		return builder.create();
	}
	
	
	public void commitNode(final OsmPoint.Action action, final Node n, final EntityInfo info, final String comment,
			final boolean closeChangeSet,
			final Runnable successAction) {
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
			};
		}.execute();
	}


	@Override
	public Dialog onCreateDialog(int id) {
		Bundle args = dialogBundle;
		switch (id) {
		case DIALOG_DELETE_POI:
			return createDeleteDialog(args);
		case DIALOG_SUB_CATEGORIES: {
			Builder builder = new AlertDialog.Builder(activity);
			final Amenity a = (Amenity) args.getSerializable(KEY_AMENITY);
			final Map<String, PoiType> allTranslatedNames = poiTypes.getAllTranslatedNames(a.getType(), true);
			final String[] subCats = allTranslatedNames.keySet().toArray(new String[0]);
			builder.setItems(subCats, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					PoiType poiType = allTranslatedNames.get(subCats[which]);
					typeText.setText(subCats[which]);
					activity.removeDialog(DIALOG_SUB_CATEGORIES);
				}
			});
			builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
				@Override
				public void onCancel(DialogInterface dialog) {
					activity.removeDialog(DIALOG_SUB_CATEGORIES);
				}
			});
			return builder.create();
		}
		case DIALOG_POI_TYPES: {
			final Amenity a = (Amenity) args.getSerializable(KEY_AMENITY);
			Builder builder = new AlertDialog.Builder(activity);
			final List<PoiCategory> categories = poiTypes.getCategories();
			String[] vals = new String[categories.size()];
			for (int i = 0; i < vals.length; i++) {
				vals[i] = categories.get(i).getTranslation();
			}
			builder.setItems(vals, new Dialog.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					PoiCategory aType = categories.get(which);
					if (aType != a.getType()) {
						a.setType(aType);
						a.setSubType(""); //$NON-NLS-1$
						updateType(a);
					}
					activity.removeDialog(DIALOG_POI_TYPES);
				}
			});
			builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
				@Override
				public void onCancel(DialogInterface dialog) {
					activity.removeDialog(DIALOG_POI_TYPES);
				}
			});
			return builder.create();
		}
		case DIALOG_OPENING_HOURS: {
			return createOpenHoursDlg();
		}
		}
		return null;
	}

	@Override
	public void onPrepareDialog(int id, Dialog dialog) {
		prepareProvider();
		Bundle args = dialogBundle;
		switch (id) {
			case DIALOG_DELETE_POI:
				prepareDeleteDialog(dialog,args);
				break;
		}
	}

}

