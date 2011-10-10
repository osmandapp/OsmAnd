package net.osmand.plus.activities;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.osmand.Base64;
import net.osmand.LogUtil;
import net.osmand.OsmAndFormatter;
import net.osmand.Version;
import net.osmand.data.Amenity;
import net.osmand.data.AmenityType;
import net.osmand.osm.Entity;
import net.osmand.osm.EntityInfo;
import net.osmand.osm.MapRenderingTypes;
import net.osmand.osm.MapUtils;
import net.osmand.osm.Node;
import net.osmand.osm.OpeningHoursParser;
import net.osmand.osm.Entity.EntityId;
import net.osmand.osm.Entity.EntityType;
import net.osmand.osm.OSMSettings.OSMTagKey;
import net.osmand.osm.OpeningHoursParser.BasicDayOpeningHourRule;
import net.osmand.osm.OpeningHoursParser.OpeningHoursRule;
import net.osmand.osm.io.OsmBaseStorage;
import net.osmand.plus.AmenityIndexRepositoryOdb;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;

import org.apache.commons.logging.Log;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.xml.sax.SAXException;
import org.xmlpull.v1.XmlSerializer;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.util.Xml;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class EditingPOIActivity implements DialogProvider {
	
//	private final static String SITE_API = "http://api06.dev.openstreetmap.org/";
	private final static String SITE_API = "http://api.openstreetmap.org/"; //$NON-NLS-1$
	
	private static final String DELETE_ACTION = "delete";  //$NON-NLS-1$
	private static final String MODIFY_ACTION = "modify"; //$NON-NLS-1$
	private static final String CREATE_ACTION = "create";  //$NON-NLS-1$

	private static final long NO_CHANGESET_ID = -1;
	
	private final MapActivity ctx;
	private AutoCompleteTextView typeText;
	private EditText nameText;
	private Button typeButton;
	private Button openHoursButton;
	private EditText openingHours;
	private EntityInfo entityInfo;
	private EditText commentText;

	// reuse changeset
	private long changeSetId = NO_CHANGESET_ID;
	private long changeSetTimeStamp = NO_CHANGESET_ID;

	private final static Log log = LogUtil.getLog(EditingPOIActivity.class);

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

	public EditingPOIActivity(MapActivity uiContext){
		this.ctx = uiContext;
	}
	
	public void showEditDialog(Amenity editA){
		Node n = loadNode(editA);
		if(n != null){
			showPOIDialog(DIALOG_EDIT_POI, n, editA.getType(), editA.getSubType());
		} else {
			Toast.makeText(ctx, ctx.getString(R.string.poi_error_poi_not_found), Toast.LENGTH_SHORT).show();
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
		ctx.showDialog(dialogID, dialogBundle);
	}
	
	public void showDeleteDialog(Amenity a){
		final Node n = loadNode(a);
		if(n == null){
			Toast.makeText(ctx, ctx.getResources().getString(R.string.poi_error_poi_not_found), Toast.LENGTH_LONG).show();
			return;
		}
		dialogBundle.putSerializable(KEY_AMENITY, a);
		dialogBundle.putSerializable(KEY_AMENITY_NODE, n);
		ctx.showDialog(DIALOG_DELETE_POI, dialogBundle);
	}
	
	private void prepareDeleteDialog(Dialog dlg, Bundle args) {
		Amenity a = (Amenity) args.getSerializable(KEY_AMENITY);
		dlg.setTitle(MessageFormat.format(this.ctx.getMapView().getResources().getString(R.string.poi_remove_confirm_template), 
				OsmAndFormatter.getPoiStringWithoutType(a, OsmandSettings.getOsmandSettings(ctx).usingEnglishNames())));
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
				commitNode(DELETE_ACTION, n, entityInfo, c, new Runnable(){
					@Override
					public void run() {
						Toast.makeText(ctx, ctx.getResources().getString(R.string.poi_remove_success), Toast.LENGTH_LONG).show();
						if(ctx.getMapView() != null){
							ctx.getMapView().refreshMap();
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
		updateType(a);
	}
	
	private Dialog createPOIDialog(final int dialogID, final Bundle args) {
		Dialog dlg = new Dialog(ctx);
		dlg.setContentView(R.layout.editing_poi);
		nameText = ((EditText)dlg.findViewById(R.id.Name));
		openingHours = ((EditText)dlg.findViewById(R.id.OpeningHours));
		typeText = ((AutoCompleteTextView)dlg.findViewById(R.id.Type));
		typeButton = ((Button)dlg.findViewById(R.id.TypeButton));
		openHoursButton = ((Button)dlg.findViewById(R.id.OpenHoursButton));
		typeText = ((AutoCompleteTextView)dlg.findViewById(R.id.Type));
		typeText.setThreshold(1);
		commentText = ((EditText)dlg.findViewById(R.id.Comment));
		
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
				ctx.showDialog(DIALOG_OPENING_HOURS, args);
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
				ctx.showDialog(DIALOG_POI_TYPES, args);
			}
		});
		
		((Button)dlg.findViewById(R.id.Cancel)).setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(View v) {
				ctx.dismissDialog(dialogID);
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
				String action = n.getId() == -1 ? CREATE_ACTION : MODIFY_ACTION;
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
				n.putTag(OSMTagKey.NAME.getValue(), nameText.getText().toString());
				if (openingHours.getText().toString().length() == 0) {
					n.removeTag(OSMTagKey.OPENING_HOURS.getValue());
				} else {
					n.putTag(OSMTagKey.OPENING_HOURS.getValue(), openingHours.getText().toString());
				}
				commitNode(action, n, entityInfo, commentText.getText().toString(), new Runnable() {
					@Override
					public void run() {
						Toast.makeText(ctx, MessageFormat.format(ctx.getResources().getString(R.string.poi_action_succeded_template), msg),
								Toast.LENGTH_LONG).show();
						if (ctx.getMapView() != null) {
							ctx.getMapView().refreshMap();
						}
						ctx.dismissDialog(dialogID);
					}
				});
			}
		});
		
		return dlg;
	}

	private void showSubCategory(final Bundle args) {
		final Amenity a = (Amenity) args.getSerializable(KEY_AMENITY);
		if(typeText.getText().length() == 0 && a.getType() != null){
			ctx.showDialog(DIALOG_SUB_CATEGORIES,args);
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
	
	private void showWarning(final String msg){
		ctx.getMapView().post(new Runnable(){
			@Override
			public void run() {
				Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show();
			}
		});
	}
	

	private Dialog createOpenHoursDlg(){
		List<OpeningHoursRule> time = OpeningHoursParser.parseOpenedHours(openingHours.getText().toString());
		if(time == null){
			Toast.makeText(ctx, ctx.getString(R.string.opening_hours_not_supported), Toast.LENGTH_LONG).show();
			return null;
		}
		
		List<BasicDayOpeningHourRule> simple = null;
		if(time != null){
			simple = new ArrayList<BasicDayOpeningHourRule>();
			for(OpeningHoursRule r : time){
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
				openingHours.setText(OpeningHoursParser.toStringOpenedHours(v.getTime()));
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
	
	
	
	protected String sendRequsetThroughHttpClient(String url, String requestMethod, String requestBody, String userOperation, boolean doAuthenticate) {
		StringBuilder responseBody = new StringBuilder();
		try {

			HttpParams params = new BasicHttpParams();
			HttpConnectionParams.setConnectionTimeout(params, 15000);
			DefaultHttpClient httpclient = new DefaultHttpClient(params);
			if (doAuthenticate) {
				UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(OsmandSettings.getOsmandSettings(ctx).USER_NAME.get() + ":" //$NON-NLS-1$
						+ OsmandSettings.getOsmandSettings(ctx).USER_PASSWORD.get());
				httpclient.getCredentialsProvider().setCredentials(new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT), credentials);
			}
			HttpRequestBase method = null;
			if (requestMethod.equals("GET")) { //$NON-NLS-1$
				method = new HttpGet(url);
			} else if (requestMethod.equals("POST")) { //$NON-NLS-1$
				method = new HttpPost(url);
			} else if (requestMethod.equals("PUT")) { //$NON-NLS-1$
				method = new HttpPut(url);
			} else if (requestMethod.equals("DELETE")) { //$NON-NLS-1$
				method = new HttpDelete(url);
				
			} else {
				throw new IllegalArgumentException(requestMethod + " is invalid method"); //$NON-NLS-1$
			}
			if (requestMethod.equals("PUT") || requestMethod.equals("POST") || requestMethod.equals("DELETE")) { //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				// TODO add when needed
//				connection.setDoOutput(true);
//				connection.setRequestProperty("Content-type", "text/xml");
//				OutputStream out = connection.getOutputStream();
//				if (requestBody != null) {
//					BufferedWriter bwr = new BufferedWriter(new OutputStreamWriter(out, "UTF-8"));
//					bwr.write(requestBody);
//					bwr.flush();
//				}
//				out.close();
			}

			HttpResponse response = httpclient.execute(method);
			if(response.getStatusLine() == null || 
					response.getStatusLine().getStatusCode() != 200){
				
				String msg;
				if(response.getStatusLine() != null){
					msg = userOperation + " " +ctx.getString(R.string.failed_op); //$NON-NLS-1$
				} else {
					msg = userOperation + " " + ctx.getString(R.string.failed_op) + response.getStatusLine().getStatusCode() + " : " + //$NON-NLS-1$//$NON-NLS-2$
							response.getStatusLine().getReasonPhrase();
				}
				log.error(msg);
				showWarning(msg);
			} else {
				InputStream is = response.getEntity().getContent();
				if (is != null) {
					BufferedReader in = new BufferedReader(new InputStreamReader(is, "UTF-8")); //$NON-NLS-1$
					String s;
					while ((s = in.readLine()) != null) {
						responseBody.append(s);
						responseBody.append("\n"); //$NON-NLS-1$
					}
					is.close();
				}
				httpclient.getConnectionManager().shutdown();
				return responseBody.toString();
			}
		} catch (MalformedURLException e) {
			log.error(userOperation + " failed", e); //$NON-NLS-1$
			showWarning(MessageFormat.format(ctx.getResources().getString(R.string.poi_error_unexpected_template), userOperation));
		} catch (IOException e) {
			log.error(userOperation + " failed", e); //$NON-NLS-1$
			showWarning(MessageFormat.format(ctx.getResources().getString(R.string.poi_error_unexpected_template), userOperation));
		}
		return null; 
		
	}
	private String sendRequest(String url, String requestMethod, String requestBody, String userOperation, boolean doAuthenticate) {
		log.info("Sending request " + url); //$NON-NLS-1$
//		if(true){
//			return sendRequsetThroughHttpClient(url, requestMethod, requestBody, userOperation, doAuthenticate);
//		}
		
		try {
			HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
			
			connection.setConnectTimeout(15000);
			connection.setRequestMethod(requestMethod);
			StringBuilder responseBody = new StringBuilder();
			if (doAuthenticate) {
				String token = OsmandSettings.getOsmandSettings(ctx).USER_NAME.get() + ":" + OsmandSettings.getOsmandSettings(ctx).USER_PASSWORD.get(); //$NON-NLS-1$
				connection.addRequestProperty("Authorization", "Basic " + Base64.encode(token.getBytes("UTF-8"))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
			connection.setDoInput(true);
			if (requestMethod.equals("PUT") || requestMethod.equals("POST") || requestMethod.equals("DELETE")) { //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				connection.setDoOutput(true);
				connection.setRequestProperty("Content-type", "text/xml"); //$NON-NLS-1$ //$NON-NLS-2$
				OutputStream out = connection.getOutputStream();
				if (requestBody != null) {
					BufferedWriter bwr = new BufferedWriter(new OutputStreamWriter(out, "UTF-8"), 1024); //$NON-NLS-1$
					bwr.write(requestBody);
					bwr.flush();
				}
				out.close();
			}
			connection.connect();
			if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
				String msg = userOperation + " " + ctx.getString(R.string.failed_op) + " : " +  connection.getResponseMessage();  //$NON-NLS-1$//$NON-NLS-2$
				log.error(msg);
				showWarning(msg);
			} else {
				log.info("Response : " + connection.getResponseMessage()); //$NON-NLS-1$
				// populate return fields.
				responseBody.setLength(0);
				InputStream i = connection.getInputStream();
				if (i != null) {
					BufferedReader in = new BufferedReader(new InputStreamReader(i, "UTF-8"), 256); //$NON-NLS-1$
					String s;
					boolean f = true;
					while ((s = in.readLine()) != null) {
						if(!f){
							responseBody.append("\n"); //$NON-NLS-1$
						} else {
							f = false;
						}
						responseBody.append(s);
					}
				}
				return responseBody.toString();
			}
		} catch (NullPointerException e) {
			// that's tricky case why NPE is thrown to fix that problem httpClient could be used
			String msg = ctx.getString(R.string.auth_failed);
			log.error(msg , e);
			showWarning(msg);
		} catch (MalformedURLException e) {
			log.error(userOperation + " " + ctx.getString(R.string.failed_op) , e); //$NON-NLS-1$
			showWarning(MessageFormat.format(ctx.getResources().getString(R.string.poi_error_unexpected_template), userOperation));
		} catch (IOException e) {
			log.error(userOperation + " " + ctx.getString(R.string.failed_op) , e); //$NON-NLS-1$
			showWarning(MessageFormat.format(ctx.getResources().getString(R.string.poi_error_io_error_template), userOperation));
		}

		return null;
	}
	
	public long openChangeSet(String comment) {
		long id = -1;
		StringWriter writer = new StringWriter(256);
		XmlSerializer ser = Xml.newSerializer();
		try {
			ser.setOutput(writer);
			ser.startDocument("UTF-8", true); //$NON-NLS-1$
			ser.startTag(null, "osm"); //$NON-NLS-1$
			ser.startTag(null, "changeset"); //$NON-NLS-1$

			ser.startTag(null, "tag"); //$NON-NLS-1$
			ser.attribute(null, "k", "comment"); //$NON-NLS-1$ //$NON-NLS-2$
			ser.attribute(null, "v", comment); //$NON-NLS-1$
			ser.endTag(null, "tag"); //$NON-NLS-1$

			ser.startTag(null, "tag"); //$NON-NLS-1$
			ser.attribute(null, "k", "created_by"); //$NON-NLS-1$ //$NON-NLS-2$
			ser.attribute(null, "v", Version.APP_NAME_VERSION); //$NON-NLS-1$
			ser.endTag(null, "tag"); //$NON-NLS-1$
			ser.endTag(null, "changeset"); //$NON-NLS-1$
			ser.endTag(null, "osm"); //$NON-NLS-1$
			ser.endDocument();
			writer.close();
		} catch (IOException e) {
			log.error("Unhandled exception", e); //$NON-NLS-1$
		}
		String response = sendRequest(SITE_API + "api/0.6/changeset/create/", "PUT", writer.getBuffer().toString(), ctx.getString(R.string.opening_changeset), true); //$NON-NLS-1$ //$NON-NLS-2$
		if (response != null && response.length() > 0) {
			id = Long.parseLong(response);
		}

		return id;
	}
	
	public void closeChangeSet(long id){
		String response = sendRequest(SITE_API+"api/0.6/changeset/"+id+"/close", "PUT", "", ctx.getString(R.string.closing_changeset), true); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		log.info("Response : " + response); //$NON-NLS-1$
	}
	
	private void writeNode(Node n, EntityInfo i, XmlSerializer ser, long changeSetId, String user) throws IllegalArgumentException, IllegalStateException, IOException{
		ser.startTag(null, "node"); //$NON-NLS-1$
		ser.attribute(null, "id", n.getId()+""); //$NON-NLS-1$ //$NON-NLS-2$
		ser.attribute(null, "lat", n.getLatitude()+""); //$NON-NLS-1$ //$NON-NLS-2$
		ser.attribute(null, "lon", n.getLongitude()+""); //$NON-NLS-1$ //$NON-NLS-2$
		if (i != null) {
			// ser.attribute(null, "timestamp", i.getETimestamp());
			// ser.attribute(null, "uid", i.getUid());
			// ser.attribute(null, "user", i.getUser());
			ser.attribute(null, "visible", i.getVisible()); //$NON-NLS-1$
			ser.attribute(null, "version", i.getVersion()); //$NON-NLS-1$
		}
		ser.attribute(null, "changeset", changeSetId+""); //$NON-NLS-1$ //$NON-NLS-2$
		
		for(String k : n.getTagKeySet()){
			String val = n.getTag(k);
			ser.startTag(null, "tag"); //$NON-NLS-1$
			ser.attribute(null, "k", k); //$NON-NLS-1$
			ser.attribute(null, "v", val); //$NON-NLS-1$
			ser.endTag(null, "tag"); //$NON-NLS-1$
		}
		ser.endTag(null, "node"); //$NON-NLS-1$
	}
	
	private void updateNodeInIndexes(String action, Node n) {
		final OsmandApplication app = ctx.getMyApplication();
		final AmenityIndexRepositoryOdb repo = app.getResourceManager().getUpdatablePoiDb();
		ctx.getMapView().post(new Runnable() {
			
			@Override
			public void run() {
				if (repo == null) {
					Toast.makeText(app, app.getString(R.string.update_poi_no_offline_poi_index), Toast.LENGTH_LONG).show();
					return;
				} else {
					Toast.makeText(app, app.getString(R.string.update_poi_does_not_change_indexes), Toast.LENGTH_LONG).show();
				}
			}
		});
		
		// delete all amenities with same id
		if (DELETE_ACTION.equals(action) || MODIFY_ACTION.equals(action)) {
			repo.deleteAmenities(n.getId() << 1);
			repo.clearCache();
		}
		// add amenities
		if (!DELETE_ACTION.equals(action)) {
			List<Amenity> ams = Amenity.parseAmenities(n, new ArrayList<Amenity>());
			for (Amenity a : ams) {
				repo.addAmenity(a);
				repo.clearCache();
			}
		}

	}
	
	
	public void commitNode(final String action, final Node n, final EntityInfo info, final String comment, final Runnable successAction) {
		if (info == null && !CREATE_ACTION.equals(action)) {
			Toast.makeText(ctx, ctx.getResources().getString(R.string.poi_error_info_not_loaded), Toast.LENGTH_LONG).show();
			return;
		}
		final ProgressDialog progress = ProgressDialog.show(ctx, ctx.getString(R.string.uploading), ctx.getString(R.string.uploading_data));
		new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					if (commitNodeImpl(action, n, info, comment)) {
						ctx.getMapView().post(successAction);
					}
				} finally {
					progress.dismiss();
				}

			}
		}, "EditingPoi").start(); //$NON-NLS-1$
	}

	private boolean isNewChangesetRequired() {
		// first commit
		if (changeSetId == NO_CHANGESET_ID){
			return true;
		}

		long now = System.currentTimeMillis();
		// changeset is idle for more than 30 minutes (1 hour according specification)
		if (now - changeSetTimeStamp > 30 * 60 * 1000) {
			return true;
		}

		return false;
	}
	
	public boolean commitNodeImpl(String action, Node n, EntityInfo info, String comment){
		if (isNewChangesetRequired()){
			changeSetId = openChangeSet(comment);
			changeSetTimeStamp = System.currentTimeMillis();
		}
		if(changeSetId < 0){
			return false;
		}
		try {
			StringWriter writer = new StringWriter(256);
			XmlSerializer ser = Xml.newSerializer();
			try {
				ser.setOutput(writer);
				ser.startDocument("UTF-8", true); //$NON-NLS-1$
				ser.startTag(null, "osmChange"); //$NON-NLS-1$
				ser.attribute(null, "version", "0.6");  //$NON-NLS-1$ //$NON-NLS-2$
				ser.attribute(null, "generator", Version.APP_NAME); //$NON-NLS-1$
				ser.startTag(null, action);
				ser.attribute(null, "version", "0.6"); //$NON-NLS-1$ //$NON-NLS-2$
				ser.attribute(null, "generator", Version.APP_NAME); //$NON-NLS-1$
				writeNode(n, info, ser, changeSetId, OsmandSettings.getOsmandSettings(ctx).USER_NAME.get());
				ser.endTag(null, action);
				ser.endTag(null, "osmChange"); //$NON-NLS-1$
				ser.endDocument();
			} catch (IOException e) {
				log.error("Unhandled exception", e); //$NON-NLS-1$
			}
			String res = sendRequest(SITE_API+"api/0.6/changeset/"+changeSetId + "/upload", "POST", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					writer.getBuffer().toString(), ctx.getString(R.string.commiting_node), true);
			log.debug(res+""); //$NON-NLS-1$
			if (res != null) {
				if (CREATE_ACTION.equals(action)) {
					long newId = n.getId();
					int i = res.indexOf("new_id=\""); //$NON-NLS-1$
					if (i > 0) {
						i = i + "new_id=\"".length(); //$NON-NLS-1$
						int end = res.indexOf("\"", i); //$NON-NLS-1$
						if (end > 0) {
							newId = Long.parseLong(res.substring(i, end));
							Node newN = new Node(n.getLatitude(), n.getLongitude(), newId);
							for (String t : n.getTagKeySet()) {
								newN.putTag(t, n.getTag(t));
							}
							n = newN;
						}
					}
				}
				updateNodeInIndexes(action, n);
				changeSetTimeStamp = System.currentTimeMillis();
				return true;
			}
			return false;
		} finally {
			// reuse changeset, do not close
			//closeChangeSet(changeSetId);
		}
	}
	
	public Node loadNode(Amenity n) {
		if(n.getId() % 2 == 1){
			// that's way id
			return null;
		}
		long nodeId = n.getId() >> 1;
		try {
			String res = sendRequest(SITE_API+"api/0.6/node/"+nodeId, "GET", null, ctx.getString(R.string.loading_poi_obj) + nodeId, false); //$NON-NLS-1$ //$NON-NLS-2$
			if(res != null){
				OsmBaseStorage st = new OsmBaseStorage();
				st.parseOSM(new ByteArrayInputStream(res.getBytes("UTF-8")), null, null, true); //$NON-NLS-1$
				EntityId id = new Entity.EntityId(EntityType.NODE, nodeId);
				Node entity = (Node) st.getRegisteredEntities().get(id);
				entityInfo = st.getRegisteredEntityInfo().get(id);
				// check whether this is node (because id of node could be the same as relation) 
				if(entity != null && MapUtils.getDistance(entity.getLatLon(), n.getLocation()) < 50){
					return entity;
				}
				return null;
			}
			
		} catch (IOException e) {
			log.error("Loading node failed " + nodeId, e); //$NON-NLS-1$
			Toast.makeText(ctx, ctx.getResources().getString(R.string.error_io_error), Toast.LENGTH_LONG).show();
		} catch (SAXException e) {
			log.error("Loading node failed " + nodeId, e); //$NON-NLS-1$
			Toast.makeText(ctx, ctx.getResources().getString(R.string.error_io_error), Toast.LENGTH_LONG).show();
		}
		return null;
	}
	
	@Override
	public Dialog onCreateDialog(int id, Bundle args) {
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
	public void onPrepareDialog(int id, Dialog dialog, Bundle args) {
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

