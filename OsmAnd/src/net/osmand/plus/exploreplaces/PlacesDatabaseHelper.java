package net.osmand.plus.exploreplaces;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import net.osmand.wiki.WikiCoreHelper;
import net.osmand.wiki.WikiCoreHelper.OsmandApiFeatureData;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class PlacesDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "places.db";

    private static final long DATA_EXPIRATION_TIME = TimeUnit.DAYS.toMillis(30); // 1 month

    private static final int DATABASE_VERSION = 2; // Incremented version for schema changes
    private static final String TABLE_PLACES = "places";
    private static final String COLUMN_ZOOM = "zoom";
    private static final String COLUMN_TILE_X = "tileX";
    private static final String COLUMN_TILE_Y = "tileY";
    private static final String COLUMN_LANG = "lang";
    private static final String COLUMN_DATA = "data";
    private static final String COLUMN_TIMESTAMP = "timestamp";

    private static final String CREATE_TABLE_PLACES = "CREATE TABLE " + TABLE_PLACES + "("
            + COLUMN_ZOOM + " INTEGER,"
            + COLUMN_TILE_X + " INTEGER,"
            + COLUMN_TILE_Y + " INTEGER,"
            + COLUMN_LANG + " TEXT,"
            + COLUMN_DATA + " TEXT,"
            + COLUMN_TIMESTAMP + " INTEGER,"
            + "PRIMARY KEY (" + COLUMN_ZOOM + ", " + COLUMN_TILE_X + ", " + COLUMN_TILE_Y + ", " + COLUMN_LANG + ")"
            + ")";

    private Gson gson = new Gson();

    public PlacesDatabaseHelper(Context context) {
        super(context, context.getCacheDir() + File.separator + DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public SQLiteDatabase getWritableDatabase() {
        return super.getWritableDatabase();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_PLACES);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PLACES);
        onCreate(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_PLACES);
            onCreate(db);
        }
    }

    public void insertPlaces(int zoom, int tileX, int tileY, String lang, List<? extends OsmandApiFeatureData> places) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_ZOOM, zoom);
        values.put(COLUMN_TILE_X, tileX);
        values.put(COLUMN_TILE_Y, tileY);
        values.put(COLUMN_LANG, lang);
        values.put(COLUMN_DATA, gson.toJson(places));
        values.put(COLUMN_TIMESTAMP, System.currentTimeMillis());
        db.insertWithOnConflict(TABLE_PLACES, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public List<OsmandApiFeatureData> getPlaces(int zoom, int tileX, int tileY, String lang) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_PLACES, new String[]{COLUMN_DATA, COLUMN_TIMESTAMP},
                COLUMN_ZOOM + "=? AND " + COLUMN_TILE_X + "=? AND " + COLUMN_TILE_Y + "=? AND " + COLUMN_LANG + "=?",
                new String[]{String.valueOf(zoom), String.valueOf(tileX), String.valueOf(tileY), lang},
                null, null, null);

        List<OsmandApiFeatureData> places = new ArrayList<>();
        if (cursor.moveToFirst()) {
            int c = cursor.getColumnIndex(COLUMN_DATA);
            String json = cursor.getString(c);
            int t = cursor.getColumnIndex(COLUMN_TIMESTAMP);
            long timestamp = cursor.getLong(t);
            places = gson.fromJson(json, new TypeToken<List<WikiCoreHelper.OsmandApiFeatureData>>(){}.getType());
        }
        cursor.close();
        return places;
    }

    public boolean isDataExpired(int zoom, int tileX, int tileY, String lang) {
        SQLiteDatabase db = this.getReadableDatabase();
        try (Cursor cursor = db.query(TABLE_PLACES, new String[] {COLUMN_TIMESTAMP},
                COLUMN_ZOOM + "=? AND " + COLUMN_TILE_X + "=? AND " + COLUMN_TILE_Y + "=? AND " + COLUMN_LANG + "=?",
                new String[] {String.valueOf(zoom), String.valueOf(tileX), String.valueOf(tileY), lang},
                null, null, null)) {

            if (cursor.moveToFirst()) {
                int tc = cursor.getColumnIndex(COLUMN_TIMESTAMP);
                long timestamp = cursor.getLong(tc);
                long currentTime = System.currentTimeMillis();
                return (currentTime - timestamp) > DATA_EXPIRATION_TIME; // 1 month expiration
            }
            return true; // Data is expired if it doesn't exist
        }
    }
}