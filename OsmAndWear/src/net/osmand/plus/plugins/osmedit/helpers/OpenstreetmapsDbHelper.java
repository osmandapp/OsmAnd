package net.osmand.plus.plugins.osmedit.helpers;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import net.osmand.plus.backup.BackupUtils;
import net.osmand.plus.utils.AndroidDbUtils;
import net.osmand.PlatformUtil;
import net.osmand.osm.edit.Entity;
import net.osmand.osm.edit.Node;
import net.osmand.osm.edit.Way;
import net.osmand.plus.plugins.osmedit.data.OpenstreetmapPoint;
import net.osmand.plus.plugins.osmedit.data.OsmPoint;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;


public class OpenstreetmapsDbHelper extends SQLiteOpenHelper {

	private static final Log log = PlatformUtil.getLog(OpenstreetmapsDbHelper.class);

	private static final int DATABASE_VERSION = 6;
	public static final String OPENSTREETMAP_DB_NAME = "openstreetmap"; //$NON-NLS-1$
	private static final String OPENSTREETMAP_TABLE_NAME = "openstreetmaptable"; //$NON-NLS-1$
	private static final String OPENSTREETMAP_COL_ID = "id"; //$NON-NLS-1$
	private static final String OPENSTREETMAP_COL_LAT= "lat"; //$NON-NLS-1$
	private static final String OPENSTREETMAP_COL_LON= "lon"; //$NON-NLS-1$
	private static final String OPENSTREETMAP_COL_TAGS = "tags"; //$NON-NLS-1$
	private static final String OPENSTREETMAP_COL_ACTION = "action"; //$NON-NLS-1$
	private static final String OPENSTREETMAP_COL_COMMENT = "comment"; //$NON-NLS-1$
	private static final String OPENSTREETMAP_COL_CHANGED_TAGS = "changed_tags";
	private static final String OPENSTREETMAP_COL_ENTITY_TYPE = "entity_type";

	private static final String OPENSTREETMAP_TABLE_CREATE = "CREATE TABLE " + OPENSTREETMAP_TABLE_NAME + " (" + //$NON-NLS-1$ //$NON-NLS-2$
			OPENSTREETMAP_COL_ID + " bigint,"+
			OPENSTREETMAP_COL_LAT + " double," + OPENSTREETMAP_COL_LON + " double," +
			OPENSTREETMAP_COL_TAGS + " VARCHAR(2048)," +
			OPENSTREETMAP_COL_ACTION + " TEXT, " + OPENSTREETMAP_COL_COMMENT + " TEXT," +
			" " + OPENSTREETMAP_COL_CHANGED_TAGS + " TEXT, " + OPENSTREETMAP_COL_ENTITY_TYPE + " TEXT);";
	List<OpenstreetmapPoint> cache;

	private static final String OPENSTREETMAP_DB_LAST_MODIFIED_NAME = "openstreetmap";

	private final Context context;

	public OpenstreetmapsDbHelper(@NonNull Context context) {
		super(context, OPENSTREETMAP_DB_NAME, null, DATABASE_VERSION);
		this.context = context;
	}
	
	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(OPENSTREETMAP_TABLE_CREATE);
	}
	
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		boolean upgraded = false;
		if (newVersion == 4) {
			db.execSQL("DROP TABLE IF EXISTS " + OPENSTREETMAP_TABLE_NAME);
			db.execSQL(OPENSTREETMAP_TABLE_CREATE);
			upgraded = true;
		}
		if (oldVersion < 5) {
			db.execSQL("ALTER TABLE " + OPENSTREETMAP_TABLE_NAME + " ADD " + OPENSTREETMAP_COL_CHANGED_TAGS + " TEXT");
			upgraded = true;
		}
		if (oldVersion < 6) {
			db.execSQL("ALTER TABLE " + OPENSTREETMAP_TABLE_NAME + " ADD " + OPENSTREETMAP_COL_ENTITY_TYPE + " TEXT");
			db.execSQL("UPDATE " + OPENSTREETMAP_TABLE_NAME + " SET " + OPENSTREETMAP_COL_ENTITY_TYPE + " = ? " +
					"WHERE " + OPENSTREETMAP_COL_ENTITY_TYPE + " IS NULL", new String[]{Entity.EntityType.NODE.toString()});
			upgraded = true;
		}
		if (upgraded) {
			updateLastModifiedTime();
		}
	}

	public long getLastModifiedTime() {
		long lastModifiedTime = BackupUtils.getLastModifiedTime(context, OPENSTREETMAP_DB_LAST_MODIFIED_NAME);
		if (lastModifiedTime == 0) {
			File dbFile = context.getDatabasePath(OPENSTREETMAP_DB_NAME);
			lastModifiedTime = dbFile.exists() ? dbFile.lastModified() : 0;
			BackupUtils.setLastModifiedTime(context, OPENSTREETMAP_DB_LAST_MODIFIED_NAME, lastModifiedTime);
		}
		return lastModifiedTime;
	}

	public void setLastModifiedTime(long lastModifiedTime) {
		BackupUtils.setLastModifiedTime(context, OPENSTREETMAP_DB_LAST_MODIFIED_NAME, lastModifiedTime);
	}

	private void updateLastModifiedTime() {
		BackupUtils.setLastModifiedTime(context, OPENSTREETMAP_DB_LAST_MODIFIED_NAME);
	}

	@NonNull
	public List<OpenstreetmapPoint> getOpenstreetmapPoints() {
		if (cache == null) {
			return checkOpenstreetmapPoints();
		}
		return cache;
	}
	
	public boolean addOpenstreetmap(OpenstreetmapPoint p) {
		SQLiteDatabase db = getWritableDatabase();
		if (db != null) {
			StringBuilder tags = new StringBuilder();
			Entity entity = p.getEntity();
			Iterator<Entry<String, String>> eit = entity.getTags().entrySet().iterator();
			while(eit.hasNext()) {
				Entry<String, String> e = eit.next();
				if(Algorithms.isEmpty(e.getKey()) || Algorithms.isEmpty(e.getValue())) {
					continue;
				}
				tags.append(e.getKey()).append("$$$").append(e.getValue());
				if(eit.hasNext()) {
					tags.append("$$$");
				}
			}
			Set<String> chTags = p.getEntity().getChangedTags();
			StringBuilder changedTags = new StringBuilder();
			if (chTags != null) {
				Iterator<String> iterator = chTags.iterator();
				while (iterator.hasNext()) {
					changedTags.append(iterator.next());
					if (iterator.hasNext()) {
						changedTags.append("$$$");
					}
				}
			}
			db.execSQL("DELETE FROM " + OPENSTREETMAP_TABLE_NAME +
					" WHERE " + OPENSTREETMAP_COL_ID + " = ?", new Object[]{p.getId()});

			Map<String, Object> rowsMap = new HashMap<>();
			rowsMap.put(OPENSTREETMAP_COL_ID, p.getId());
			rowsMap.put(OPENSTREETMAP_COL_LAT, p.getLatitude());
			rowsMap.put(OPENSTREETMAP_COL_LON, p.getLongitude());
			rowsMap.put(OPENSTREETMAP_COL_TAGS, tags.toString());
			rowsMap.put(OPENSTREETMAP_COL_ACTION, OsmPoint.stringAction.get(p.getAction()));
			rowsMap.put(OPENSTREETMAP_COL_COMMENT, p.getComment());
			rowsMap.put(OPENSTREETMAP_COL_CHANGED_TAGS, chTags == null ? null : changedTags.toString());
			rowsMap.put(OPENSTREETMAP_COL_ENTITY_TYPE, Entity.EntityType.valueOf(entity));

			db.execSQL(AndroidDbUtils.createDbInsertQuery(OPENSTREETMAP_TABLE_NAME, rowsMap.keySet()),
					rowsMap.values().toArray());

			db.close();
			checkOpenstreetmapPoints();
			updateLastModifiedTime();
			return true;
		}
		return false;
	}
	
	public boolean deletePOI(OpenstreetmapPoint p) {
		SQLiteDatabase db = getWritableDatabase();
		if (db != null) {
			db.execSQL("DELETE FROM " + OPENSTREETMAP_TABLE_NAME +
					" WHERE " + OPENSTREETMAP_COL_ID + " = ?", new Object[] { p.getId() }); //$NON-NLS-1$ //$NON-NLS-2$
			db.close();
			checkOpenstreetmapPoints();
			updateLastModifiedTime();
			return true;
		}
		return false;
	}

	@NonNull
	private List<OpenstreetmapPoint> checkOpenstreetmapPoints() {
		SQLiteDatabase db = getReadableDatabase();
		List<OpenstreetmapPoint> points = new ArrayList<>();
		if (db != null) {
			Cursor query = db.rawQuery("SELECT " +
					OPENSTREETMAP_COL_ID + ", " +
					OPENSTREETMAP_COL_LAT + "," +
					OPENSTREETMAP_COL_LON + "," +
					OPENSTREETMAP_COL_ACTION + "," +
					OPENSTREETMAP_COL_COMMENT + "," +
					OPENSTREETMAP_COL_TAGS + "," +
					OPENSTREETMAP_COL_CHANGED_TAGS + "," +
					OPENSTREETMAP_COL_ENTITY_TYPE +
					" FROM " + OPENSTREETMAP_TABLE_NAME, null);
			if (query.moveToFirst()) {
				do {
					OpenstreetmapPoint p = new OpenstreetmapPoint();
					Entity.EntityType entityType = parseEntityType(query.getString(7));
					Entity entity = null;
					if (entityType == Entity.EntityType.NODE) {
						entity = new Node(query.getDouble(1),
								query.getDouble(2),
								query.getLong(0));
					} else if (entityType == Entity.EntityType.WAY) {
						entity = new Way(query.getLong(0), null,
								query.getDouble(1),
								query.getDouble(2));
					}
					if (entity != null) {
						String tags = query.getString(5);
						String[] split = tags.split("\\$\\$\\$");
						for (int i = 0; i < split.length - 1; i += 2) {
							entity.putTagNoLC(split[i].trim(), split[i + 1].trim());
						}
						String changedTags = query.getString(6);
						if (changedTags != null) {
							entity.setChangedTags(new HashSet<>(Arrays.asList(changedTags.split("\\$\\$\\$"))));
						}
						p.setEntity(entity);
						p.setAction(query.getString(3));
						p.setComment(query.getString(4));
						points.add(p);
					}

				} while (query.moveToNext());
			}
			query.close();
		}
		cache = points;
		return points;
	}

	@Nullable
	private Entity.EntityType parseEntityType(@Nullable String entityType) {
		if (entityType == null) {
			return null;
		}
		try {
			return Entity.EntityType.valueOf(entityType);
		} catch (IllegalArgumentException e) {
			log.error(e);
			return null;
		}
	}

	public long getMinID() {
		SQLiteDatabase db = getReadableDatabase();
		long minID = 0;
		if (db != null) {
			Cursor query = db.rawQuery("SELECT MIN(" + OPENSTREETMAP_COL_ID + ") FROM " + OPENSTREETMAP_TABLE_NAME, null); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			if (query.moveToFirst()) {
				minID = query.getLong(0);
			}
			query.close();
		}
		return minID;
	}
}