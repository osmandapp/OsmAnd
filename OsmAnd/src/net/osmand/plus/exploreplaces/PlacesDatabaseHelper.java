package net.osmand.plus.exploreplaces;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Pair;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import net.osmand.util.Algorithms;
import net.osmand.wiki.WikiCoreHelper.OsmandApiFeatureData;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    private final Gson gson = new Gson();

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

    public void insertPlaces(int zoom, int tileX, int tileY,
            Map<String, List<OsmandApiFeatureData>> placesByLang) {
        SQLiteDatabase db = getWritableDatabase();
        try {
            db.beginTransaction();
            for (Map.Entry<String, List<OsmandApiFeatureData>> entry : placesByLang.entrySet()) {
                String lang = entry.getKey();
                List<OsmandApiFeatureData> places = entry.getValue();

                ContentValues values = new ContentValues();
                values.put(COLUMN_ZOOM, zoom);
                values.put(COLUMN_TILE_X, tileX);
                values.put(COLUMN_TILE_Y, tileY);
                values.put(COLUMN_LANG, lang);
                values.put(COLUMN_DATA, gson.toJson(places));
                values.put(COLUMN_TIMESTAMP, System.currentTimeMillis());
                db.insertWithOnConflict(TABLE_PLACES, null, values, SQLiteDatabase.CONFLICT_REPLACE);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    @NonNull
    public List<OsmandApiFeatureData> getPlaces(int zoom, int tileX, int tileY, @NonNull List<String> languages) {
        SQLiteDatabase db = getReadableDatabase();
        Pair<String, String[]> pair = getSelectionWithArgs(zoom, tileX, tileY, languages);
        Cursor cursor = db.query(TABLE_PLACES, new String[] {COLUMN_DATA, COLUMN_TIMESTAMP},
                pair.first, pair.second, null, null, null);

        List<OsmandApiFeatureData> places = new ArrayList<>();
        if (cursor.moveToFirst()) {
            int c = cursor.getColumnIndex(COLUMN_DATA);
            int t = cursor.getColumnIndex(COLUMN_TIMESTAMP);
            do {
                String json = cursor.getString(c);
                long timestamp = cursor.getLong(t);
                List<OsmandApiFeatureData> parsed = gson.fromJson(json,
                        new TypeToken<List<OsmandApiFeatureData>>() {}.getType());
                if (parsed != null) {
                    places.addAll(parsed);
                }
            } while (cursor.moveToNext());
        }
        cursor.close();
        return places;
    }

    public boolean isDataExpired(int zoom, int tileX, int tileY, @NonNull List<String> languages) {
        SQLiteDatabase db = getReadableDatabase();
        boolean filterByLang = !Algorithms.isEmpty(languages);
        Pair<String, String[]> pair = getSelectionWithArgs(zoom, tileX, tileY, languages);

        try (Cursor cursor = db.query(TABLE_PLACES, new String[] {COLUMN_LANG, COLUMN_TIMESTAMP},
                pair.first, pair.second, null, null, null)) {
            if (cursor.moveToFirst()) {
                Set<String> foundLangs = new HashSet<>();
                long currentTime = System.currentTimeMillis();
                do {
                    int timestampIndex = cursor.getColumnIndex(COLUMN_TIMESTAMP);
                    long timestamp = cursor.getLong(timestampIndex);
                    if ((currentTime - timestamp) > DATA_EXPIRATION_TIME) {
                        return true; // 1 month expiration
                    }
                    int langIndex = cursor.getColumnIndex(COLUMN_LANG);
                    String lang = cursor.getString(langIndex);
                    foundLangs.add(lang);
                } while (cursor.moveToNext());

                return filterByLang && foundLangs.size() < languages.size();
            }
            return true; // Data is expired if it doesn't exist
        }
    }

    @NonNull
    private Pair<String, String[]> getSelectionWithArgs(int zoom, int tileX, int tileY, @NonNull List<String> languages) {
        List<String> list = new ArrayList<>();
        list.add(String.valueOf(zoom));
        list.add(String.valueOf(tileX));
        list.add(String.valueOf(tileY));

        StringBuilder builder = new StringBuilder();
        builder.append(COLUMN_ZOOM).append("=? AND ")
                .append(COLUMN_TILE_X).append("=? AND ")
                .append(COLUMN_TILE_Y).append("=?");

        if (!Algorithms.isEmpty(languages)) {
            StringBuilder placeholders = new StringBuilder();
            for (int i = 0; i < languages.size(); i++) {
                placeholders.append("?");
                if (i < languages.size() - 1) {
                    placeholders.append(",");
                }
            }
            builder.append(" AND ").append(COLUMN_LANG).append(" IN (").append(placeholders).append(")");
            list.addAll(languages);
        }
        return Pair.create(builder.toString(), list.toArray(new String[0]));
    }
}