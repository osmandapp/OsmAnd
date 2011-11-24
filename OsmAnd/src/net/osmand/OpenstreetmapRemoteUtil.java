package net.osmand;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.osmand.data.Amenity;
import net.osmand.osm.Entity;
import net.osmand.osm.Entity.EntityId;
import net.osmand.osm.Entity.EntityType;
import net.osmand.osm.EntityInfo;
import net.osmand.osm.MapUtils;
import net.osmand.osm.Node;
import net.osmand.osm.io.OsmBaseStorage;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;

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

import android.util.Xml;
import android.widget.Toast;

public class OpenstreetmapRemoteUtil implements OpenstreetmapUtil {
	
//	private final static String SITE_API = "http://api06.dev.openstreetmap.org/";
	private final static String SITE_API = "http://api.openstreetmap.org/"; //$NON-NLS-1$

	public static final Map<Action, String> stringAction = new HashMap<Action, String>();
	public static final Map<String, Action> actionString = new HashMap<String, Action>();
	static {
		stringAction.put(Action.CREATE, "create");
		stringAction.put(Action.MODIFY, "modify");
		stringAction.put(Action.DELETE, "delete");

		actionString.put("create", Action.CREATE);
		actionString.put("modify", Action.MODIFY);
		actionString.put("delete", Action.DELETE);
	};

	private static final long NO_CHANGESET_ID = -1;
	
	private final MapActivity ctx;
	private EntityInfo entityInfo;
	
	// reuse changeset
	private long changeSetId = NO_CHANGESET_ID;
	private long changeSetTimeStamp = NO_CHANGESET_ID;

	public final static Log log = LogUtil.getLog(OpenstreetmapRemoteUtil.class);

	public OpenstreetmapRemoteUtil(MapActivity uiContext){
		this.ctx = uiContext;
	}

	public EntityInfo getEntityInfo() {
		return entityInfo;
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
	
	public boolean commitNodeImpl(Action action, Node n, EntityInfo info, String comment){
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
				ser.startTag(null, stringAction.get(action));
				ser.attribute(null, "version", "0.6"); //$NON-NLS-1$ //$NON-NLS-2$
				ser.attribute(null, "generator", Version.APP_NAME); //$NON-NLS-1$
				writeNode(n, info, ser, changeSetId, OsmandSettings.getOsmandSettings(ctx).USER_NAME.get());
				ser.endTag(null, stringAction.get(action));
				ser.endTag(null, "osmChange"); //$NON-NLS-1$
				ser.endDocument();
			} catch (IOException e) {
				log.error("Unhandled exception", e); //$NON-NLS-1$
			}
			String res = sendRequest(SITE_API+"api/0.6/changeset/"+changeSetId + "/upload", "POST", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					writer.getBuffer().toString(), ctx.getString(R.string.commiting_node), true);
			log.debug(res+""); //$NON-NLS-1$
			if (res != null) {
				if (Action.CREATE == action) {
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
	
	private void showWarning(final String msg){
		ctx.getMapView().post(new Runnable(){
			@Override
			public void run() {
				Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show();
			}
		});
	}

}
