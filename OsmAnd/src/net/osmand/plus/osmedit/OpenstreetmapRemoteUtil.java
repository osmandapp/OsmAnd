package net.osmand.plus.osmedit;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.text.MessageFormat;
import java.util.LinkedHashMap;
import java.util.Map;

import net.osmand.PlatformUtil;
import net.osmand.data.Amenity;
import net.osmand.osm.PoiType;
import net.osmand.osm.edit.Entity;
import net.osmand.osm.edit.Entity.EntityId;
import net.osmand.osm.edit.Entity.EntityType;
import net.osmand.osm.edit.EntityInfo;
import net.osmand.osm.edit.Node;
import net.osmand.osm.io.Base64;
import net.osmand.osm.io.NetworkUtils;
import net.osmand.osm.io.OsmBaseStorage;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.util.MapUtils;

import org.apache.commons.logging.Log;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import android.util.Xml;
import android.widget.Toast;

public class OpenstreetmapRemoteUtil implements OpenstreetmapUtil {

	private static final long NO_CHANGESET_ID = -1;

	private final OsmandApplication ctx;
	private EntityInfo entityInfo;
	private EntityId entityInfoId;

	// reuse changeset
	private long changeSetId = NO_CHANGESET_ID;
	private long changeSetTimeStamp = NO_CHANGESET_ID;

	public final static Log log = PlatformUtil.getLog(OpenstreetmapRemoteUtil.class);

	private OsmandSettings settings;


	public OpenstreetmapRemoteUtil(OsmandApplication app) {
		this.ctx = app;
		settings = ctx.getSettings();
	}

	@Override
	public EntityInfo getEntityInfo(long id) {
		if(entityInfoId != null && entityInfoId.getId().longValue() == id) {
			return entityInfo;
		}
		return null;
	}

	private static String getSiteApi() {
		final int deviceApiVersion = android.os.Build.VERSION.SDK_INT;

		String RETURN_API;

		if (deviceApiVersion >= android.os.Build.VERSION_CODES.GINGERBREAD) {
			RETURN_API = "https://api.openstreetmap.org/";
		} else {
			RETURN_API = "http://api.openstreetmap.org/";
		}

		// RETURN_API = "http://api06.dev.openstreetmap.org/";

		return RETURN_API;
	}

	private final static String URL_TO_UPLOAD_GPX = getSiteApi() + "api/0.6/gpx/create";

	public String uploadGPXFile(String tagstring, String description, String visibility, File f) {
		String url = URL_TO_UPLOAD_GPX;
		Map<String, String> additionalData = new LinkedHashMap<String, String>();
		additionalData.put("description", description);
		additionalData.put("tags", tagstring);
		additionalData.put("visibility", visibility);
		return NetworkUtils.uploadFile(url, f, settings.USER_NAME.get() + ":" + settings.USER_PASSWORD.get(), "file",
				true, additionalData);
	}

	private String sendRequest(String url, String requestMethod, String requestBody, String userOperation,
			boolean doAuthenticate) {
		log.info("Sending request " + url); //$NON-NLS-1$
		try {
			HttpURLConnection connection = NetworkUtils.getHttpURLConnection(url);

			connection.setConnectTimeout(15000);
			connection.setRequestMethod(requestMethod);
			connection.setRequestProperty("User-Agent", Version.getFullVersion(ctx)); //$NON-NLS-1$
			StringBuilder responseBody = new StringBuilder();
			if (doAuthenticate) {
				String token = settings.USER_NAME.get() + ":" + settings.USER_PASSWORD.get(); //$NON-NLS-1$
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
				String msg = userOperation
						+ " " + ctx.getString(R.string.failed_op) + " : " + connection.getResponseMessage(); //$NON-NLS-1$//$NON-NLS-2$
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
						if (!f) {
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
			log.error(msg, e);
			showWarning(msg);
		} catch (MalformedURLException e) {
			log.error(userOperation + " " + ctx.getString(R.string.failed_op), e); //$NON-NLS-1$
			showWarning(MessageFormat.format(ctx.getResources().getString(R.string.shared_string_action_template)
					+ ": " + ctx.getResources().getString(R.string.shared_string_unexpected_error), userOperation));
		} catch (IOException e) {
			log.error(userOperation + " " + ctx.getString(R.string.failed_op), e); //$NON-NLS-1$
			showWarning(MessageFormat.format(ctx.getResources().getString(R.string.shared_string_action_template)
					+ ": " + ctx.getResources().getString(R.string.shared_string_io_error), userOperation));
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

			if(comment != null) {
				ser.startTag(null, "tag"); //$NON-NLS-1$
				ser.attribute(null, "k", "comment"); //$NON-NLS-1$ //$NON-NLS-2$
				ser.attribute(null, "v", comment); //$NON-NLS-1$
				ser.endTag(null, "tag"); //$NON-NLS-1$
			}

			ser.startTag(null, "tag"); //$NON-NLS-1$
			ser.attribute(null, "k", "created_by"); //$NON-NLS-1$ //$NON-NLS-2$
			ser.attribute(null, "v", Version.getFullVersion(ctx)); //$NON-NLS-1$
			ser.endTag(null, "tag"); //$NON-NLS-1$
			ser.endTag(null, "changeset"); //$NON-NLS-1$
			ser.endTag(null, "osm"); //$NON-NLS-1$
			ser.endDocument();
			writer.close();
		} catch (IOException e) {
			log.error("Unhandled exception", e); //$NON-NLS-1$
		}
		String response = sendRequest(
				getSiteApi() + "api/0.6/changeset/create/", "PUT", writer.getBuffer().toString(), ctx.getString(R.string.opening_changeset), true); //$NON-NLS-1$ //$NON-NLS-2$
		if (response != null && response.length() > 0) {
			id = Long.parseLong(response);
		}

		return id;
	}

	private void writeNode(Node n, EntityInfo i, XmlSerializer ser, long changeSetId, String user)
			throws IllegalArgumentException, IllegalStateException, IOException {
		ser.startTag(null, "node"); //$NON-NLS-1$
		ser.attribute(null, "id", n.getId() + ""); //$NON-NLS-1$ //$NON-NLS-2$
		ser.attribute(null, "lat", n.getLatitude() + ""); //$NON-NLS-1$ //$NON-NLS-2$
		ser.attribute(null, "lon", n.getLongitude() + ""); //$NON-NLS-1$ //$NON-NLS-2$
		if (i != null) {
			// ser.attribute(null, "timestamp", i.getETimestamp());
			// ser.attribute(null, "uid", i.getUid());
			// ser.attribute(null, "user", i.getUser());
			ser.attribute(null, "visible", i.getVisible()); //$NON-NLS-1$
			ser.attribute(null, "version", i.getVersion()); //$NON-NLS-1$
		}
		ser.attribute(null, "changeset", changeSetId + ""); //$NON-NLS-1$ //$NON-NLS-2$

		for (String k : n.getTagKeySet()) {
			String val = n.getTag(k);
			if (val.length() == 0 || k.length() == 0 || EditPoiData.POI_TYPE_TAG.equals(k) ||
					k.startsWith(EditPoiData.REMOVE_TAG_PREFIX) || k.contains(EditPoiData.REMOVE_TAG_PREFIX))
				continue;
			ser.startTag(null, "tag"); //$NON-NLS-1$
			ser.attribute(null, "k", k); //$NON-NLS-1$
			ser.attribute(null, "v", val); //$NON-NLS-1$
			ser.endTag(null, "tag"); //$NON-NLS-1$
		}
		ser.endTag(null, "node"); //$NON-NLS-1$
	}

	private boolean isNewChangesetRequired() {
		// first commit
		if (changeSetId == NO_CHANGESET_ID) {
			return true;
		}

		long now = System.currentTimeMillis();
		// changeset is idle for more than 30 minutes (1 hour according specification)
		if (now - changeSetTimeStamp > 30 * 60 * 1000) {
			return true;
		}

		return false;
	}

	@Override
	public Node commitNodeImpl(OsmPoint.Action action, final Node n, EntityInfo info, String comment,
			boolean closeChangeSet) {
		if (isNewChangesetRequired()) {
			changeSetId = openChangeSet(comment);
			changeSetTimeStamp = System.currentTimeMillis();
		}
		if (changeSetId < 0) {
			return null;
		}

		try {
			Node newN = n;
			StringWriter writer = new StringWriter(256);
			XmlSerializer ser = Xml.newSerializer();
			try {
				ser.setOutput(writer);
				ser.startDocument("UTF-8", true); //$NON-NLS-1$
				ser.startTag(null, "osmChange"); //$NON-NLS-1$
				ser.attribute(null, "version", "0.6"); //$NON-NLS-1$ //$NON-NLS-2$
				ser.attribute(null, "generator", Version.getAppName(ctx)); //$NON-NLS-1$
				ser.startTag(null, OsmPoint.stringAction.get(action));
				ser.attribute(null, "version", "0.6"); //$NON-NLS-1$ //$NON-NLS-2$
				ser.attribute(null, "generator", Version.getAppName(ctx)); //$NON-NLS-1$
				writeNode(n, info, ser, changeSetId, settings.USER_NAME.get());
				ser.endTag(null, OsmPoint.stringAction.get(action));
				ser.endTag(null, "osmChange"); //$NON-NLS-1$
				ser.endDocument();
			} catch (IOException e) {
				log.error("Unhandled exception", e); //$NON-NLS-1$
			}
			String res = sendRequest(getSiteApi() + "api/0.6/changeset/" + changeSetId + "/upload", "POST", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					writer.getBuffer().toString(), ctx.getString(R.string.commiting_node), true);
			log.debug(res + ""); //$NON-NLS-1$
			if (res != null) {
				if (OsmPoint.Action.CREATE == action) {
					long newId = n.getId();
					int i = res.indexOf("new_id=\""); //$NON-NLS-1$
					if (i > 0) {
						i = i + "new_id=\"".length(); //$NON-NLS-1$
						int end = res.indexOf('\"', i); //$NON-NLS-1$
						if (end > 0) {
							newId = Long.parseLong(res.substring(i, end)); // << 1;
							newN = new Node(n, newId);
						}
					}
				}
				changeSetTimeStamp = System.currentTimeMillis();
				return newN;
			}
			return null;
		} finally {
			if (closeChangeSet) {
				closeChangeSet();
			}
		}
	}

	@Override
	public void closeChangeSet() {
		if (changeSetId != NO_CHANGESET_ID) {
			String response = sendRequest(
					getSiteApi() + "api/0.6/changeset/" + changeSetId + "/close", "PUT", "", ctx.getString(R.string.closing_changeset), true); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			log.info("Response : " + response); //$NON-NLS-1$
			changeSetId = NO_CHANGESET_ID;
		}

	}

	public EntityInfo loadNode(Node n) {
		long nodeId = n.getId(); // >> 1;
		try {
			String res = sendRequest(
					getSiteApi() + "api/0.6/node/" + nodeId, "GET", null, ctx.getString(R.string.loading_poi_obj) + nodeId, false); //$NON-NLS-1$ //$NON-NLS-2$
			if (res != null) {
				OsmBaseStorage st = new OsmBaseStorage();
				st.setConvertTagsToLC(false);
				st.parseOSM(new ByteArrayInputStream(res.getBytes("UTF-8")), null, null, true); //$NON-NLS-1$
				EntityId id = new Entity.EntityId(EntityType.NODE, nodeId);
				Node entity = (Node) st.getRegisteredEntities().get(id);
				// merge non existing tags
				for (String rtag : entity.getTagKeySet()) {
					if (!n.getTagKeySet().contains(rtag)) {
						n.putTagNoLC(rtag, entity.getTag(rtag));
					}
				}
				if(MapUtils.getDistance(n.getLatLon(), entity.getLatLon()) < 10) {
					// avoid shifting due to round error
					n.setLatitude(entity.getLatitude());
					n.setLongitude(entity.getLongitude());
				}
				entityInfo = st.getRegisteredEntityInfo().get(id);
				entityInfoId = id;
				return entityInfo;
			}

		} catch (IOException | XmlPullParserException e) {
			log.error("Loading node failed " + nodeId, e); //$NON-NLS-1$
			Toast.makeText(ctx, ctx.getResources().getString(R.string.shared_string_io_error),
					Toast.LENGTH_LONG).show();
		}
		return null;
	}

	@Override
	public Node loadNode(Amenity n) {
		if (n.getId() % 2 == 1) {
			// that's way id
			return null;
		}
		long nodeId = n.getId() >> 1;
		try {
			String res = sendRequest(
					getSiteApi() + "api/0.6/node/" + nodeId, "GET", null, ctx.getString(R.string.loading_poi_obj) + nodeId, false); //$NON-NLS-1$ //$NON-NLS-2$
			if (res != null) {
				OsmBaseStorage st = new OsmBaseStorage();
				st.setConvertTagsToLC(false);
				st.parseOSM(new ByteArrayInputStream(res.getBytes("UTF-8")), null, null, true); //$NON-NLS-1$
				EntityId id = new Entity.EntityId(EntityType.NODE, nodeId);
				Node entity = (Node) st.getRegisteredEntities().get(id);
				entityInfo = st.getRegisteredEntityInfo().get(id);
				entityInfoId = id;
				// check whether this is node (because id of node could be the same as relation)
				if (entity != null && MapUtils.getDistance(entity.getLatLon(), n.getLocation()) < 50) {
					PoiType poiType = n.getType().getPoiTypeByKeyName(n.getSubType());
					if(poiType.getOsmValue().equals(entity.getTag(poiType.getOsmTag()))) {
						entity.removeTag(poiType.getOsmTag());
						entity.putTagNoLC(EditPoiData.POI_TYPE_TAG, poiType.getTranslation());
					} else {
						// later we could try to determine tags
					}
					return entity;
				}
				return null;
			}

		} catch (Exception e) {
			log.error("Loading node failed " + nodeId, e); //$NON-NLS-1$
			ctx.runInUIThread(new Runnable() {

				@Override
				public void run() {
					Toast.makeText(ctx, ctx.getResources().getString(R.string.shared_string_io_error),
							Toast.LENGTH_LONG).show();
				}
			});
		}
		return null;
	}

	private void showWarning(final String msg) {
		ctx.runInUIThread(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show();
			}
		});
	}

}
