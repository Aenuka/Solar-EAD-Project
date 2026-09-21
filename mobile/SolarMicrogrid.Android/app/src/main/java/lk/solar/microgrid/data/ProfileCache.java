package lk.solar.microgrid.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import org.json.JSONException;
import org.json.JSONObject;

/** Device-private SQLite cache of the last server-confirmed profile. Never stores passwords/tokens. */
public final class ProfileCache extends SQLiteOpenHelper {
    public ProfileCache(Context context) { super(context, "solar_profile.db", null, 1); }
    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE profile_cache (nic TEXT PRIMARY KEY, payload TEXT NOT NULL, fetched_at INTEGER NOT NULL)");
    }
    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        throw new IllegalStateException("Add an explicit migration before increasing the database version.");
    }
    public void save(Profile profile) {
        ContentValues values = new ContentValues();
        values.put("nic", profile.nic);
        values.put("payload", profile.json.toString());
        values.put("fetched_at", System.currentTimeMillis());
        getWritableDatabase().insertWithOnConflict("profile_cache", null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }
    public Snapshot read(String nic) throws JSONException {
        try (Cursor cursor = getReadableDatabase().query("profile_cache", new String[]{"payload", "fetched_at"}, "nic = ?", new String[]{nic}, null, null, null)) {
            if (!cursor.moveToFirst()) return null;
            return new Snapshot(new Profile(new JSONObject(cursor.getString(0))), true, cursor.getLong(1));
        }
    }
    public void clear() { getWritableDatabase().delete("profile_cache", null, null); }
    public static final class Snapshot {
        public final Profile profile;
        public final boolean cached;
        public final long fetchedAt;
        public Snapshot(Profile profile, boolean cached, long fetchedAt) { this.profile = profile; this.cached = cached; this.fetchedAt = fetchedAt; }
    }
}
