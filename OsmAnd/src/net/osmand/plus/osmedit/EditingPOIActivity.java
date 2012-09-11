package net.osmand.plus.osmedit;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import net.osmand.OsmAndFormatter;
import net.osmand.access.AccessibleToast;
import net.osmand.data.Amenity;
import net.osmand.data.AmenityType;
import net.osmand.osm.EntityInfo;
import net.osmand.osm.MapRenderingTypes;
import net.osmand.osm.Node;
import net.osmand.osm.OSMSettings.OSMTagKey;
import net.osmand.osm.OpeningHoursParser;
import net.osmand.osm.OpeningHoursParser.BasicDayOpeningHourRule;
import net.osmand.osm.OpeningHoursParser.OpeningHours;
import net.osmand.osm.OpeningHoursParser.OpeningHoursRule;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.DialogProvider;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.OpeningHoursView;
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
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

public class EditingPOIActivity implements DialogProvider {
	
	private final MapActivity ctx;
	private final OpenstreetmapUtil openstreetmapUtil;
	private final OpenstreetmapUtil openstreetmapUtilToLoad;
	private AutoCompleteTextView typeText;
	private EditText nameText;
	private Button typeButton;
	private Button openHoursButton;
	private EditText openingHours;
	private EditText commentText;
	private EditText websiteText;
	private EditText phoneText;

//	private final static Log log = LogUtil.getLog(EditingPOIActivity.class);

	/* dialog stuff */
	private static final int DIALOG_CREATE_POI = 200;
	private static final int DIALOG_EDIT_POI = 201;
	protected static final int DIALOG_SUB_CATEGORIES = 202;
	protected static final int DIALOG_POI_TYPES = 203;
	private static final int DIALOG_DELETE_POI = 204;
	private static final int DIALOG_OPENING_HOURS = 205;

	private static final String KEY_AMENITY_NODE = "amenity_node";
	private static final String KEY_AMENITY = "amenity";

	private Bundle dialogBundle = new Bundle();
	private OsmandSettings settings;

	public EditingPOIActivity(MapActivity uiContext){
		this.ctx = uiContext;

		settings = ((OsmandApplication) uiContext.getApplication()).getSettings();
		if (settings.OFFLINE_EDITION.get() || !settings.isInternetConnectionAvailable(true)) {
			this.openstreetmapUtil = new OpenstreetmapLocalUtil(ctx);
			if (settings.isInternetConnectionAvailable(true)) {
				this.openstreetmapUtilToLoad = new OpenstreetmapRemoteUtil(ctx, ctx.getMapView());
			} else {
				this.openstreetmapUtilToLoad = openstreetmapUtil;
			}
		} else {
			this.openstreetmapUtil = new OpenstreetmapRemoteUtil(ctx, ctx.getMapView());
			this.openstreetmapUtilToLoad= openstreetmapUtil;
		}
	}
	
	public void showEditDialog(Amenity editA){
		Node n = openstreetmapUtilToLoad.loadNode(editA);
		if(n != null){
			showPOIDialog(DIALOG_EDIT_POI, n, editA.getType(), editA.getSubType());
		} else {
			AccessibleToast.makeText(ctx, ctx.getString(R.string.poi_error_poi_not_found), Toast.LENGTH_SHORT).show();
		}
	}
	
	public void showCreateDialog(double latitude, double longitude){
		Node n = new Node(latitude, longitude, -1);
		n.putTag(OSMTagKey.OPENING_HOURS.getValue(), ""); //$NON-NLS-1$
		showPOIDialog(DIALOG_CREATE_POI, n, AmenityType.OTHER, "");
	}

	private void showPOIDialog(int dialogID, Node n, AmenityType type, String subType) {
		Amenity a = new Amenity(n, type, subType);
		dialogBundle.putSerializable(KEY_AMENITY, a);
		dialogBundle.putSerializable(KEY_AMENITY_NODE, n);
		ctx.showDialog(dialogID);
	}
	
	public void showDeleteDialog(Amenity a){
		final Node n = openstreetmapUtil.loadNode(a);
		if(n == null){
			AccessibleToast.makeText(ctx, ctx.getResources().getString(R.string.poi_error_poi_not_found), Toast.LENGTH_LONG).show();
			return;
		}
		dialogBundle.putSerializable(KEY_AMENITY, a);
		dialogBundle.putSerializable(KEY_AMENITY_NODE, n);
		ctx.showDialog(DIALOG_DELETE_POI); //TODO from android 2.0 use showDialog(id,bundle)
	}
	
	private void prepareDeleteDialog(Dialog dlg, Bundle args) {
		Amenity a = (Amenity) args.getSerializable(KEY_AMENITY);
		dlg.setTitle(MessageFormat.format(this.ctx.getMapView().getResources().getString(R.string.poi_remove_confirm_template), 
				OsmAndFormatter.getPoiStringWithoutType(a, settings.usingEnglishNames())));
	}
	
	private Dialog createDeleteDialog(final Bundle args) {
		Builder builder = new AlertDialog.Builder(ctx);
		builder.setTitle(R.string.poi_remove_title);
		final EditText comment = new EditText(ctx);
		comment.setText(R.string.poi_remove_title);
		builder.setView(comment);
		builder.setNegativeButton(R.string.default_buttons_cancel, null);
		builder.setPositiveButton(R.string.default_buttons_delete, new DialogInterface.OnClickListener(){
			@Override
			public void onClick(DialogInterface dialog, int which) {
				Node n = (Node) args.getSerializable(KEY_AMENITY_NODE);
				String c = comment.getText().toString();
				commitNode(OsmPoint.Action.DELETE, n, openstreetmapUtil.getEntityInfo(), c, new Runnable(){
					@Override
					public void run() {
						AccessibleToast.makeText(ctx, ctx.getResources().getString(R.string.poi_remove_success), Toast.LENGTH_LONG).show();
						if(ctx.getMapView() != null){
							ctx.getMapView().refreshMap(true);
						}						
					}
				});
			}
		});
		return builder.create();
	}

	private void preparePOIDialog(Dialog dlg, Bundle args, int title) {
		Amenity a = (Amenity) args.getSerializable(KEY_AMENITY);
		dlg.setTitle(title);
		EditText nameText = ((EditText)dlg.findViewById(R.id.Name));
		nameText.setText(a.getName());
		EditText openingHours = ((EditText)dlg.findViewById(R.id.OpeningHours));
		openingHours.setText(a.getOpeningHours());
		EditText phoneText = ((EditText)dlg.findViewById(R.id.Phone));
		phoneText.setText(a.getPhone());
		EditText websiteText = ((EditText)dlg.findViewById(R.id.Website));
		websiteText.setText(a.getSite());
		final TableLayout layout = ((TableLayout)dlg.findViewById(R.id.advancedModeTable));
		layout.setVisibility(View.GONE);
		updateType(a);
	}
	
	private void addTagValueRow(final Node n, final TableLayout layout, String tg, String vl) {
		final TableRow newTagRow = new TableRow(ctx);				            
        TableRow.LayoutParams tlp = new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT);          
        tlp.leftMargin = 5;
        newTagRow.setLayoutParams(tlp);

        final AutoCompleteTextView tag = new AutoCompleteTextView(ctx);
        final AutoCompleteTextView value = new AutoCompleteTextView(ctx);				            
        final Button delete = new Button(ctx);
        
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
		ArrayAdapter<Object> adapter = new ArrayAdapter<Object>(ctx, R.layout.list_textview, tagKeys.toArray());
        tag.setAdapter(adapter);
        tag.setThreshold(1);
        tag.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Builder builder = new AlertDialog.Builder(ctx);
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
        tlp.rightMargin = 5;
        tlp.width = 80;
        value.setLayoutParams(tlp);
        if(vl != null) {
        	value.setText(vl);
        } else {
        	value.setHint("Value");
        }
        Set<String> subCategories = MapRenderingTypes.getDefault().getAmenityNameToType().keySet();
        ArrayAdapter<Object> valueAdapter = new ArrayAdapter<Object>(ctx, R.layout.list_textview, subCategories.toArray());
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
        tlp.gravity = Gravity.CENTER;
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
	
	private Dialog createPOIDialog(final int dialogID, final Bundle args) {
		final Dialog dlg = new Dialog(ctx);
		dlg.setContentView(R.layout.editing_poi);
		nameText = ((EditText)dlg.findViewById(R.id.Name));
		openingHours = ((EditText)dlg.findViewById(R.id.OpeningHours));
		typeText = ((AutoCompleteTextView)dlg.findViewById(R.id.Type));
		typeButton = ((Button)dlg.findViewById(R.id.TypeButton));
		openHoursButton = ((Button)dlg.findViewById(R.id.OpenHoursButton));
		typeText = ((AutoCompleteTextView)dlg.findViewById(R.id.Type));
		typeText.setThreshold(1);
		commentText = ((EditText)dlg.findViewById(R.id.Comment));
		phoneText = ((EditText)dlg.findViewById(R.id.Phone));
		websiteText = ((EditText)dlg.findViewById(R.id.Website));
		
		
		TextView linkToOsmDoc = (TextView) dlg.findViewById(R.id.LinkToOsmDoc);
		linkToOsmDoc.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				ctx.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://wiki.openstreetmap.org/wiki/Map_Features")));
			}
		});
		linkToOsmDoc.setMovementMethod(LinkMovementMethod.getInstance());
		
		
		// DO NOT show on focus with empty text predefined list of subcategories - problems when rotating
		typeText.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				showSubCategory(args);
			}
		});
		typeText.setOnLongClickListener(new View.OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				showSubCategory(args);
				return true;
			}
		});
		openHoursButton.setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(View v) {
				ctx.showDialog(DIALOG_OPENING_HOURS);
			}
		});
		typeText.addTextChangedListener(new TextWatcher(){

			@Override
			public void afterTextChanged(Editable s) {
				Amenity a = (Amenity) args.getSerializable(KEY_AMENITY);
				String str = s.toString();
				a.setSubType(str);
				AmenityType t = MapRenderingTypes.getDefault().getAmenityNameToType().get(str);
				if(t != null && a.getType() != t){
					a.setType(t);
					typeButton.setText(OsmAndFormatter.toPublicString(t, ctx));
					updateSubTypesAdapter(t);
				}
				
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
				ctx.showDialog(DIALOG_POI_TYPES);
			}
		});
		
		final Button advancedModeButton = ((Button)dlg.findViewById(R.id.advancedMode));
		advancedModeButton.setOnClickListener(new View.OnClickListener() {	
			@Override
			public void onClick(View v) {
				final Node n = (Node) args.getSerializable(KEY_AMENITY_NODE);
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
				Amenity a = (Amenity) args.getSerializable(KEY_AMENITY);
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
		
		((Button)dlg.findViewById(R.id.Cancel)).setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(View v) {
				//we must do remove, because there are two dialogs EDIT,CREATE using same variables!!
				ctx.removeDialog(dialogID);
			}
		});
		((Button)dlg.findViewById(R.id.Commit)).setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(View v) {
				final Amenity a = (Amenity) args.getSerializable(KEY_AMENITY);
				final Node n = (Node) args.getSerializable(KEY_AMENITY_NODE);
				Resources resources = v.getResources();
				final String msg = n.getId() == -1 ? resources.getString(R.string.poi_action_add) : resources
						.getString(R.string.poi_action_change);
				OsmPoint.Action action = n.getId() == -1 ? OsmPoint.Action.CREATE : OsmPoint.Action.MODIFY;
				Map<AmenityType, Map<String, String>> typeNameToTagVal = MapRenderingTypes.getDefault().getAmenityTypeNameToTagVal();
				AmenityType type = a.getType();
				String tag = type.getDefaultTag();
				String subType = typeText.getText().toString();
				String val = subType;
				if (typeNameToTagVal.containsKey(type)) {
					Map<String, String> map = typeNameToTagVal.get(type);
					if (map.containsKey(subType)) {
						String res = map.get(subType);
						if (res != null) {
							int i = res.indexOf(' ');
							if (i != -1) {
								tag = res.substring(0, i);
								val = res.substring(i + 1);
							} else {
								tag = res;
							}
						}
					}
				}
				n.putTag(tag, val);
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
				}
				String phone = phoneText.getText().toString();
				if (phone.length() > 0 ){
					n.putTag(OSMTagKey.PHONE.getValue(),phone);
				}
				commitNode(action, n, openstreetmapUtil.getEntityInfo(), commentText.getText().toString(), new Runnable() {
					@Override
					public void run() {
						AccessibleToast.makeText(ctx, MessageFormat.format(ctx.getResources().getString(R.string.poi_action_succeded_template), msg),
								Toast.LENGTH_LONG).show();
						if (ctx.getMapView() != null) {
							ctx.getMapView().refreshMap(true);
						}
						ctx.removeDialog(dialogID);
					}
				});
			}
		});
		
		return dlg;
	}

	private void showSubCategory(final Bundle args) {
		final Amenity a = (Amenity) args.getSerializable(KEY_AMENITY);
		if(typeText.getText().length() == 0 && a.getType() != null){
			ctx.showDialog(DIALOG_SUB_CATEGORIES);
		}
	}

	private void updateSubTypesAdapter(AmenityType t){
		
		Set<String> subCategories = new LinkedHashSet<String>(AmenityType.getSubCategories(t, MapRenderingTypes.getDefault()));
		for(String s : MapRenderingTypes.getDefault().getAmenityNameToType().keySet()){
			if(!subCategories.contains(s)){
				subCategories.add(s);
			}
		}
		ArrayAdapter<Object> adapter = new ArrayAdapter<Object>(ctx, R.layout.list_textview, subCategories.toArray());
		typeText.setAdapter(adapter);
	}
	
	private void updateType(Amenity a){
		typeText.setText(a.getSubType());
		typeButton.setText(OsmAndFormatter.toPublicString(a.getType(), ctx));
		updateSubTypesAdapter(a.getType());
	}
	

	private Dialog createOpenHoursDlg(){
		OpeningHours time = OpeningHoursParser.parseOpenedHours(openingHours.getText().toString());
		if(time == null){
			AccessibleToast.makeText(ctx, ctx.getString(R.string.opening_hours_not_supported), Toast.LENGTH_LONG).show();
			return null;
		}
		
		List<BasicDayOpeningHourRule> simple = null;
		if(time != null){
			simple = new ArrayList<BasicDayOpeningHourRule>();
			for(OpeningHoursRule r : time.getRules()){
				if(r instanceof BasicDayOpeningHourRule){
					simple.add((BasicDayOpeningHourRule) r);
				} else {
					time = null;
					break;
				}
			}
		}
		
		Builder builder = new AlertDialog.Builder(ctx);
		final OpeningHoursView v = new OpeningHoursView(ctx);
		builder.setView(v.createOpeningHoursEditView(simple));
		builder.setPositiveButton(ctx.getString(R.string.default_buttons_apply), new DialogInterface.OnClickListener(){
			@Override
			public void onClick(DialogInterface dialog, int which) {
				OpeningHours oh = new OpeningHours((ArrayList<OpeningHoursRule>) v.getTime());
				openingHours.setText(oh.toString());
				ctx.removeDialog(DIALOG_OPENING_HOURS);
			}
		});
		builder.setNegativeButton(ctx.getString(R.string.default_buttons_cancel), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				ctx.removeDialog(DIALOG_OPENING_HOURS);
			}
		});
		return builder.create();
	}
	
	
	public void commitNode(final OsmPoint.Action action, final Node n, final EntityInfo info, final String comment, final Runnable successAction) {
		if (info == null && OsmPoint.Action.CREATE != action) {
			AccessibleToast.makeText(ctx, ctx.getResources().getString(R.string.poi_error_info_not_loaded), Toast.LENGTH_LONG).show();
			return;
		}
		new AsyncTask<Void, Void, Node>() {
			ProgressDialog progress;
			@Override
			protected void onPreExecute() {
				progress = ProgressDialog.show(ctx, ctx.getString(R.string.uploading), ctx.getString(R.string.uploading_data));
				super.onPreExecute();
			}
			@Override
			protected Node doInBackground(Void... params) {
				Node node = null;
				if ((node = openstreetmapUtil.commitNodeImpl(action, n, info, comment)) != null) {
					openstreetmapUtil.updateNodeInIndexes(ctx, action, node, n);
				}
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
			case DIALOG_CREATE_POI:
			case DIALOG_EDIT_POI:
				return createPOIDialog(id,args);
			case DIALOG_DELETE_POI:
				return createDeleteDialog(args);
			case DIALOG_SUB_CATEGORIES: {
				Builder builder = new AlertDialog.Builder(ctx);
				final Amenity a = (Amenity) args.getSerializable(KEY_AMENITY);
				final String[] subCats = AmenityType.getSubCategories(a.getType(), MapRenderingTypes.getDefault()).toArray(new String[0]);
				builder.setItems(subCats, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						typeText.setText(subCats[which]);
						a.setSubType(subCats[which]);
						ctx.removeDialog(DIALOG_SUB_CATEGORIES);
					}
				});
				builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
					@Override
					public void onCancel(DialogInterface dialog) {
						ctx.removeDialog(DIALOG_SUB_CATEGORIES);
					}
				});
				return builder.create();
			}
			case DIALOG_POI_TYPES: {
				final Amenity a = (Amenity) args.getSerializable(KEY_AMENITY);
				Builder builder = new AlertDialog.Builder(ctx);
				String[] vals = new String[AmenityType.values().length];
				for(int i=0; i<vals.length; i++){
					vals[i] = OsmAndFormatter.toPublicString(AmenityType.values()[i], ctx); 
				}
				builder.setItems(vals, new Dialog.OnClickListener(){
					@Override
					public void onClick(DialogInterface dialog, int which) {
						AmenityType aType = AmenityType.values()[which];
						if(aType != a.getType()){
							a.setType(aType);
							a.setSubType(""); //$NON-NLS-1$
							updateType(a);
						}
						ctx.removeDialog(DIALOG_POI_TYPES);
					}
				});
				builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
					@Override
					public void onCancel(DialogInterface dialog) {
						ctx.removeDialog(DIALOG_POI_TYPES);
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
		Bundle args = dialogBundle;
		switch (id) {
			case DIALOG_CREATE_POI:
				preparePOIDialog(dialog,args,R.string.poi_create_title);
				break;
			case DIALOG_EDIT_POI:
				preparePOIDialog(dialog,args,R.string.poi_edit_title);
				break;
			case DIALOG_DELETE_POI:
				prepareDeleteDialog(dialog,args);
				break;
		}
	}

}

