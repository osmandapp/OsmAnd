package net.osmand.plus.osmedit;

import android.util.Xml;
import android.widget.Toast;
import com.github.scribejava.core.model.Response;
import gnu.trove.list.array.TLongArrayList;
import net.osmand.NativeLibrary;
import net.osmand.PlatformUtil;
import net.osmand.data.Amenity;
import net.osmand.data.Building;
import net.osmand.data.LatLon;
import net.osmand.data.MapObject;
import net.osmand.osm.PoiCategory;
import net.osmand.osm.PoiType;
import net.osmand.osm.edit.Entity;
import net.osmand.osm.edit.Entity.EntityId;
import net.osmand.osm.edit.Entity.EntityType;
import net.osmand.osm.edit.EntityInfo;
import net.osmand.osm.edit.Node;
import net.osmand.osm.edit.Way;
import net.osmand.osm.io.Base64;
import net.osmand.osm.io.NetworkUtils;
import net.osmand.osm.io.OsmBaseStorage;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.osmedit.oauth.OsmOAuthAuthorizationAdapter;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.util.MapUtils;
import org.apache.commons.logging.Log;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static net.osmand.osm.edit.Entity.POI_TYPE_TAG;

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
		if (entityInfoId != null && entityInfoId.getId().longValue() == id) {
			return entityInfo;
		}
		return null;
	}

	private String getSiteApi() {
		return settings.getOsmUrl();
	}

	public String uploadGPXFile(String tagstring, String description, String visibility, File f) {
		OsmOAuthAuthorizationAdapter adapter = new OsmOAuthAuthorizationAdapter(ctx);
		String url = getSiteApi() + "api/0.6/gpx/create";
		Map<String, String> additionalData = new LinkedHashMap<String, String>();
		additionalData.put("description", description);
		additionalData.put("tags", tagstring);
		additionalData.put("visibility", visibility);
		return NetworkUtils.uploadFile(url, f,
				settings.OSM_USER_NAME.get() + ":" + settings.OSM_USER_PASSWORD.get(),
				adapter.getClient(),
				"file",
				true, additionalData);
	}

	private String sendRequest(String url, String requestMethod, String requestBody, String userOperation,
			boolean doAuthenticate) {
		log.info("Sending request " + url); //$NON-NLS-1$
		try {
			OsmOAuthAuthorizationAdapter client = new OsmOAuthAuthorizationAdapter(ctx);
			if (doAuthenticate) {
				if (client.isValidToken()) {
					Response response = client.performRequest(url, requestMethod, requestBody);
					return response.getBody();
				} else {
					return performBasicAuthRequest(url, requestMethod, requestBody, userOperation);
				}
			} else {
				Response response = client.performRequestWithoutAuth(url, requestMethod, requestBody);
				return response.getBody();
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
		} catch (InterruptedException e) {
			log.error(userOperation + " " + ctx.getString(R.string.failed_op), e); //$NON-NLS-1$
			showWarning(MessageFormat.format(ctx.getResources().getString(R.string.shared_string_action_template)
					+ ": " + ctx.getResources().getString(R.string.shared_string_unexpected_error), userOperation));
		} catch (Exception e) {
			log.error(userOperation + " " + ctx.getString(R.string.failed_op), e); //$NON-NLS-1$
			showWarning(MessageFormat.format(ctx.getResources().getString(R.string.shared_string_action_template)
					+ ": " + ctx.getResources().getString(R.string.shared_string_unexpected_error), userOperation));
		}

		return null;
	}

	private String performBasicAuthRequest(String url, String requestMethod, String requestBody, String userOperation) throws IOException {
		HttpURLConnection connection = NetworkUtils.getHttpURLConnection(url);
		connection.setConnectTimeout(15000);
		connection.setRequestMethod(requestMethod);
		connection.setRequestProperty("User-Agent", Version.getFullVersion(ctx)); //$NON-NLS-1$
		StringBuilder responseBody = new StringBuilder();
		String token = settings.OSM_USER_NAME.get() + ":" + settings.OSM_USER_PASSWORD.get(); //$NON-NLS-1$
		connection.addRequestProperty("Authorization", "Basic " + Base64.encode(token.getBytes("UTF-8"))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
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
		try {
			if (response != null && response.length() > 0) {
				log.debug(response);
				id = Long.parseLong(response);
			}
		} catch (Exception e) {
			log.error(e);
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

		writeTags(n, ser);
		ser.endTag(null, "node"); //$NON-NLS-1$
	}

	private void writeWay(Way way, EntityInfo i, XmlSerializer ser, long changeSetId, String user)
			throws IllegalArgumentException, IllegalStateException, IOException {
		ser.startTag(null, "way"); //$NON-NLS-1$
		ser.attribute(null, "id", way.getId() + ""); //$NON-NLS-1$ //$NON-NLS-2$
		if (i != null) {
			ser.attribute(null, "visible", i.getVisible()); //$NON-NLS-1$
			ser.attribute(null, "version", i.getVersion()); //$NON-NLS-1$
		}
		ser.attribute(null, "changeset", changeSetId + ""); //$NON-NLS-1$ //$NON-NLS-2$

		writeNodesIds(way, ser);
		writeTags(way, ser);
		ser.endTag(null, "way"); //$NON-NLS-1$
	}

	private void writeTags(Entity entity, XmlSerializer ser)
			throws IllegalArgumentException, IllegalStateException, IOException {
		for (String k : entity.getTagKeySet()) {
			String val = entity.getTag(k);
			if (val.length() == 0 || k.length() == 0 || POI_TYPE_TAG.equals(k) ||
					k.startsWith(Entity.REMOVE_TAG_PREFIX) || k.contains(Entity.REMOVE_TAG_PREFIX))
				continue;
			ser.startTag(null, "tag"); //$NON-NLS-1$
			ser.attribute(null, "k", k); //$NON-NLS-1$
			ser.attribute(null, "v", val); //$NON-NLS-1$
			ser.endTag(null, "tag"); //$NON-NLS-1$
		}
	}

	private void writeNodesIds(Way way, XmlSerializer ser)
			throws IllegalArgumentException, IllegalStateException, IOException {
		for (int i = 0; i < way.getNodeIds().size(); i++) {
			long nodeId = way.getNodeIds().get(i);
			if (nodeId != 0) {
				ser.startTag(null, "nd"); //$NON-NLS-1$
				ser.attribute(null, "ref", String.valueOf(nodeId)); //$NON-NLS-1$
				ser.endTag(null, "nd"); //$NON-NLS-1$
			}
		}
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
	public Entity commitEntityImpl(OsmPoint.Action action, final Entity n, EntityInfo info, String comment,
							   boolean closeChangeSet, Set<String> changedTags) {
		if (isNewChangesetRequired()) {
			changeSetId = openChangeSet(comment);
			changeSetTimeStamp = System.currentTimeMillis();
		}
		if (changeSetId < 0) {
			return null;
		}

		try {
			Entity newE = n;
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
				if (n instanceof Node) {
					writeNode((Node) n, info, ser, changeSetId, settings.OSM_USER_NAME.get());
				} else if (n instanceof Way) {
					writeWay((Way) n, info, ser, changeSetId, settings.OSM_USER_NAME.get());
				}
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
							if (n instanceof Node) {
								newE = new Node((Node) n, newId);
							} else if (n instanceof Way) {
								newE = new Way(newId, ((Way) n).getNodeIds(), n.getLatitude(), n.getLongitude());
							}
						}
					}
				}
				changeSetTimeStamp = System.currentTimeMillis();
				return newE;
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

	public EntityInfo loadEntity(Entity n) {
		long entityId = n.getId(); // >> 1;
		boolean isWay = false;
		if (n instanceof Way) { // check if entity is a way
			isWay = true;
		}
		try {
			String api = isWay ? "api/0.6/way/" : "api/0.6/node/";
			String res = sendRequest(getSiteApi() + api + entityId, "GET", null,
					ctx.getString(R.string.loading_poi_obj) + entityId, false); //$NON-NLS-1$ //$NON-NLS-2$
			if (res != null) {
				OsmBaseStorage st = new OsmBaseStorage();
				st.setConvertTagsToLC(false);
				st.parseOSM(new ByteArrayInputStream(res.getBytes("UTF-8")), null, null, true); //$NON-NLS-1$
				EntityId id = new Entity.EntityId(isWay ? EntityType.WAY : EntityType.NODE, entityId);
				Entity entity = st.getRegisteredEntities().get(id);
				// merge non existing tags
				Map<String, String> updatedTags = new HashMap<>();
				for (String tagKey : entity.getTagKeySet()) {
					if (tagKey != null && !deletedTag(n, tagKey)) {
						addIfNotNull(tagKey, entity.getTag(tagKey), updatedTags);
					}
				}
				if (n.getChangedTags() != null) {
					for (String tagKey : n.getChangedTags()) {
						if (tagKey != null) {
							addIfNotNull(tagKey, n.getTag(tagKey), updatedTags);
						}
					}
				}
				n.replaceTags(updatedTags);
				if (isWay) {
					Way foundWay = (Way) entity;
					Way currentWay = (Way) n;
					TLongArrayList nodeIds = foundWay.getNodeIds();
					if (nodeIds != null) {
						for (int i = 0; i < nodeIds.size(); i++) {
							long nodeId = nodeIds.get(i);
							if (nodeId != 0) {
								currentWay.addNode(nodeId);
							}
						}
					}
				} else if (MapUtils.getDistance(n.getLatLon(), entity.getLatLon()) < 10 || 
					  MapUtils.getDistance(n.getLatLon(), entity.getLatLon()) > 10000) {
					// avoid shifting due to round error and avoid moving to more than 10 km
					n.setLatitude(entity.getLatitude());
					n.setLongitude(entity.getLongitude());
				}
				entityInfo = st.getRegisteredEntityInfo().get(id);
				entityInfoId = id;
				return entityInfo;
			}

		} catch (IOException | XmlPullParserException e) {
			log.error("Loading entity failed " + entityId, e); //$NON-NLS-1$
			Toast.makeText(ctx, ctx.getResources().getString(R.string.shared_string_io_error),
					Toast.LENGTH_LONG).show();
		}
		return null;
	}

	private void addIfNotNull(String key, String value, Map<String, String> tags) {
		if (value != null) {
			tags.put(key, value);
		}
	}

	private boolean deletedTag(Entity entity, String tag) {
		return entity.getTagKeySet().contains(Entity.REMOVE_TAG_PREFIX + tag);
	}

	@Override
	public Entity loadEntity(MapObject object) {
		Long objectId = object.getId();
		if (!(objectId != null && objectId > 0 && (objectId % 2 == MapObject.AMENITY_ID_RIGHT_SHIFT
				|| (objectId >> MapObject.NON_AMENITY_ID_RIGHT_SHIFT) < Integer.MAX_VALUE))) {
			return null;
		}
		boolean isWay = objectId % 2 == MapObject.WAY_MODULO_REMAINDER;// check if mapObject is a way
		long entityId;
		if (object instanceof Amenity) {
			entityId = objectId >> MapObject.AMENITY_ID_RIGHT_SHIFT;
		} else {
			entityId = objectId >> MapObject.NON_AMENITY_ID_RIGHT_SHIFT;
		}
		try {
			String api = isWay ? "api/0.6/way/" : "api/0.6/node/";
			String res = sendRequest(getSiteApi() + api + entityId, "GET", null,
					ctx.getString(R.string.loading_poi_obj) + entityId, false); //$NON-NLS-1$ //$NON-NLS-2$
			if (res != null) {
				OsmBaseStorage st = new OsmBaseStorage();
				st.setConvertTagsToLC(false);
				st.parseOSM(new ByteArrayInputStream(res.getBytes("UTF-8")), null, null, true); //$NON-NLS-1$
				EntityId id = new Entity.EntityId(isWay ? EntityType.WAY : EntityType.NODE, entityId);
				Entity entity = (Entity) st.getRegisteredEntities().get(id);
				entityInfo = st.getRegisteredEntityInfo().get(id);
				entityInfoId = id;
				if (entity != null) {
					if (!isWay && entity instanceof Node) {
						// check whether this is node (because id of node could be the same as relation)
						if (object instanceof NativeLibrary.RenderedObject && object.getLocation() == null) {
							object.setLocation(((NativeLibrary.RenderedObject) object).getLabelLatLon());
						}
						if (MapUtils.getDistance(entity.getLatLon(), object.getLocation()) < 50) {
							if (object instanceof Amenity) {
								return replaceEditOsmTags((Amenity) object, entity);
							} else {
								return entity;
							}
						}
					} else if (isWay && entity instanceof Way) {
						LatLon loc = object.getLocation();
						if (loc == null) {
							if (object instanceof NativeLibrary.RenderedObject) {
								loc = ((NativeLibrary.RenderedObject) object).getLabelLatLon();
							} else if (object instanceof Building) {
								loc = ((Building) object).getLatLon2();
							}
						}
						if (loc == null) {
							return null;
						}
						entity.setLatitude(loc.getLatitude());
						entity.setLongitude(loc.getLongitude());
						if (object instanceof Amenity) {
							return replaceEditOsmTags((Amenity) object, entity);
						} else {
							return entity;
						}
					}
				}
				return null;
			}

		} catch (Exception e) {
			log.error("Loading entity failed " + entityId, e); //$NON-NLS-1$
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

	private Entity replaceEditOsmTags(Amenity amenity, Entity entity) {
		PoiCategory type = amenity.getType();
		String subType = amenity.getSubType();
		if (type != null && subType != null) {
			PoiType poiType = type.getPoiTypeByKeyName(subType);
			if (poiType != null && poiType.getEditOsmValue().equals(entity.getTag(poiType.getEditOsmTag()))) {
				entity.removeTag(poiType.getEditOsmTag());
				entity.putTagNoLC(POI_TYPE_TAG, poiType.getTranslation());
			} else {
				for (PoiType pt : type.getPoiTypes()) {
					if (pt.getEditOsmValue().equals(entity.getTag(pt.getEditOsmTag()))) {
						entity.removeTag(pt.getEditOsmTag());
						entity.putTagNoLC(POI_TYPE_TAG, pt.getTranslation());
					}
				}
			}
		}
		return entity;
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
