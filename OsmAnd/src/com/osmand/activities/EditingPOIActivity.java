package com.osmand.activities;

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
import java.util.List;

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
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.util.Xml;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.osmand.AmenityIndexRepository;
import com.osmand.Base64;
import com.osmand.LogUtil;
import com.osmand.OsmandSettings;
import com.osmand.R;
import com.osmand.ResourceManager;
import com.osmand.Version;
import com.osmand.data.Amenity;
import com.osmand.data.AmenityType;
import com.osmand.osm.Entity;
import com.osmand.osm.EntityInfo;
import com.osmand.osm.Node;
import com.osmand.osm.OSMSettings.OSMTagKey;
import com.osmand.osm.io.OsmBaseStorage;
import com.osmand.views.OsmandMapTileView;

public class EditingPOIActivity {
	
//	private final static String SITE_API = "http://api06.dev.openstreetmap.org/";
	private final static String SITE_API = "http://api.openstreetmap.org/"; //$NON-NLS-1$
	
	private static final String DELETE_ACTION = "delete";  //$NON-NLS-1$
	private static final String MODIFY_ACTION = "modify"; //$NON-NLS-1$
	private static final String CREATE_ACTION = "create";  //$NON-NLS-1$
	
	private Dialog dlg;
	private final Context ctx;
	private final OsmandMapTileView view;
	private AutoCompleteTextView typeText;
	private EditText nameText;
	private Button typeButton;
	private EditText openingHours;
	private EntityInfo entityInfo;
	private EditText commentText;
	private final static Log log = LogUtil.getLog(EditingPOIActivity.class);


	public EditingPOIActivity(final Context ctx, OsmandMapTileView view){
		this.ctx = ctx;
		this.view = view;
	}
	
	public void showEditDialog(long id){
		Node n = loadNode(id);
		if(n != null){
			dlg = new Dialog(ctx);
			dlg.setTitle(R.string.poi_edit_title);
			showDialog(n);
		}
	}
	
	public void showCreateDialog(double latitude, double longitude){
		dlg = new Dialog(ctx);
		Node n = new Node(latitude, longitude, -1);
		n.putTag(OSMTagKey.AMENITY.getValue(), ""); //$NON-NLS-1$
		n.putTag(OSMTagKey.OPENING_HOURS.getValue(), "Mo-Su 08:00-20:00"); //$NON-NLS-1$
		dlg.setTitle(R.string.poi_create_title);
		showDialog(n);
	}
	
	public void showDeleteDialog(long id){
		final Node n = loadNode(id);
		if(n == null){
			Toast.makeText(ctx, ctx.getResources().getString(R.string.poi_error_poi_not_found), Toast.LENGTH_LONG).show();
			return;
		}
		
		Builder builder = new AlertDialog.Builder(ctx);
		builder.setTitle(MessageFormat.format(this.view.getResources().getString(R.string.poi_remove_confirm_template), n.getTag(OSMTagKey.NAME)));
		final EditText comment = new EditText(ctx);
		comment.setText(R.string.poi_remove_title);
		builder.setView(comment);
		builder.setNegativeButton(R.string.default_buttons_cancel, null);
		builder.setPositiveButton(R.string.default_buttons_delete, new DialogInterface.OnClickListener(){
			@Override
			public void onClick(DialogInterface dialog, int which) {
				String c = comment.getText().toString();
				commitNode(DELETE_ACTION, n, entityInfo, c, new Runnable(){
					@Override
					public void run() {
						Toast.makeText(ctx, ctx.getResources().getString(R.string.poi_remove_success), Toast.LENGTH_LONG).show();
						if(view != null){
							view.refreshMap();
						}						
					}
				});
			}
		});
		builder.show();
	}
	
	private void showDialog(final Node n){
		final Amenity a = new Amenity(n);
		dlg.setContentView(R.layout.editing_poi);
		nameText =((EditText)dlg.findViewById(R.id.Name));
		nameText.setText(a.getName());
		typeText = ((AutoCompleteTextView)dlg.findViewById(R.id.Type));
		typeButton = ((Button)dlg.findViewById(R.id.TypeButton));
		openingHours = ((EditText)dlg.findViewById(R.id.OpeningHours));
		openingHours.setText(a.getOpeningHours());
		typeText = ((AutoCompleteTextView)dlg.findViewById(R.id.Type));
		typeText.setThreshold(1);
		commentText = ((EditText)dlg.findViewById(R.id.Comment));
		updateType(a);
		
		
		typeButton.setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(View v) {
				Builder builder = new AlertDialog.Builder(ctx);
				String[] vals = new String[AmenityType.values().length];
				for(int i=0; i<vals.length; i++){
					vals[i] = AmenityType.toPublicString(AmenityType.values()[i]); 
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
					}
				});
				builder.show();
			}
		});
		
		((Button)dlg.findViewById(R.id.Cancel)).setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(View v) {
				dlg.dismiss();
			}
		});
		((Button)dlg.findViewById(R.id.Commit)).setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(View v) {
				Resources resources = v.getResources();
				final String msg = n.getId() == -1 ? resources.getString(R.string.poi_action_add) : resources
						.getString(R.string.poi_action_change);
				String action = n.getId() == -1 ? CREATE_ACTION : MODIFY_ACTION;
				n.putTag(a.convertToAmenityTag(), typeText.getText().toString());
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
						if (view != null) {
							view.refreshMap();
						}
						dlg.dismiss();

					}
				});
			}
		});
		
		
		dlg.show();
	}
	
	private void updateType(Amenity a){
		typeText.setText(a.getSubType());
		typeButton.setText(AmenityType.toPublicString(a.getType()));
		
		List<String> subCategories = AmenityType.getSubCategories(a.getType());
		ArrayAdapter<Object> adapter = new ArrayAdapter<Object>(ctx, R.layout.list_textview, subCategories.toArray());
		typeText.setAdapter(adapter);
	}
	
	private void showWarning(final String msg){
		view.post(new Runnable(){
			@Override
			public void run() {
				Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show();
			}
			
		});
	}
	
	
	
	
	protected String sendRequsetThroughHttpClient(String url, String requestMethod, String requestBody, String userOperation, boolean doAuthenticate) {
		StringBuilder responseBody = new StringBuilder();
		try {

			HttpParams params = new BasicHttpParams();
			HttpConnectionParams.setConnectionTimeout(params, 15000);
			DefaultHttpClient httpclient = new DefaultHttpClient(params);

			if (doAuthenticate) {
				UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(OsmandSettings.getUserName(ctx) + ":" //$NON-NLS-1$
						+ OsmandSettings.getUserPassword(ctx));
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
				// TODO
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
				String token = OsmandSettings.getUserName(ctx) + ":" + OsmandSettings.getUserPassword(ctx); //$NON-NLS-1$
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
	
	public long openChangeSet(String comment,double lat, double lon) {
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
	
	private void updateNodeInIndexes(String action, Node n){
		List<AmenityIndexRepository> repos = ResourceManager.getResourceManager().searchRepositories(n.getLatitude(), n.getLongitude());
		if(DELETE_ACTION.equals(action)){
			for(AmenityIndexRepository r: repos){
				r.deleteAmenity(n.getId());
				r.clearCache();
			}
		} else {
			boolean changed = MODIFY_ACTION.equals(action);
			Amenity a = new Amenity(n);
			for(AmenityIndexRepository r: repos){
				if(changed){
					r.updateAmenity(a.getId(), n.getLatitude(), n.getLongitude(), a.getName(), a.getEnName(), a.getType(), a.getSubType(), a.getOpeningHours());
				} else {
					r.addAmenity(a.getId(), n.getLatitude(), n.getLongitude(), a.getName(), a.getEnName(), a.getType(), a.getSubType(), a.getOpeningHours());
				}
				r.clearCache();
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
						view.post(successAction);
					}
				} finally {
					progress.dismiss();
				}

			}
		}, "EditingPoi").start(); //$NON-NLS-1$
	}
	
	public boolean commitNodeImpl(String action, Node n, EntityInfo info, String comment){
		long changeSetId = openChangeSet(comment, n.getLatitude(), n.getLongitude());
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
				writeNode(n, info, ser, changeSetId, OsmandSettings.getUserName(ctx));
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
				return true;
			}
			return false;
		} finally {
			closeChangeSet(changeSetId);
		}
	}
	
	public Node loadNode(long id) {
		try {
			String res = sendRequest(SITE_API+"api/0.6/node/"+id, "GET", null, ctx.getString(R.string.loading_poi_obj) + id, false); //$NON-NLS-1$ //$NON-NLS-2$
			if(res != null){
				OsmBaseStorage st = new OsmBaseStorage();
				st.parseOSM(new ByteArrayInputStream(res.getBytes("UTF-8")), null, null, true); //$NON-NLS-1$
				Entity entity = st.getRegisteredEntities().get(id);
				entityInfo = st.getRegisteredEntityInfo().get(id);
				if(entity instanceof Node){
					return (Node) entity;
				}
			}
			
		} catch (IOException e) {
			log.error("Loading node failed" + id, e); //$NON-NLS-1$
			Toast.makeText(ctx, ctx.getResources().getString(R.string.error_io_error), Toast.LENGTH_LONG).show();
		} catch (SAXException e) {
			log.error("Loading node failed" + id, e); //$NON-NLS-1$
			Toast.makeText(ctx, ctx.getResources().getString(R.string.error_io_error), Toast.LENGTH_LONG).show();
		}
		return null;
	}
	
	
}

