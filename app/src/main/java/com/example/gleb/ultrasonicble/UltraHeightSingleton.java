package com.example.gleb.ultrasonicble;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.example.gleb.ultrasonicble.database.DbSchema.HeightsTable;
import com.example.gleb.ultrasonicble.database.DbSchema.HeightsTable.Cols;
import com.example.gleb.ultrasonicble.database.HeightCursorWrapper;
import com.example.gleb.ultrasonicble.database.HeightDBaseHelper;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Created by Gleb on 13.09.2017.
 */

public class UltraHeightSingleton {

    public static final String TAG = UltraHeightSingleton.class.getName();

    private static UltraHeightSingleton instance;

    //private List<UltraHeight> data;

    private Context mContext;
    private SQLiteDatabase mDatabase;


    public static UltraHeightSingleton get(Context context) {
        if (instance == null) {
            instance = new UltraHeightSingleton(context);
        }
        return instance;
    }

    protected static ContentValues getContentValues(UltraHeight ultraHeight) {
        ContentValues values = new ContentValues();
        values.put(Cols.UUID, ultraHeight.getUuid().toString());
        values.put(Cols.HEIGHT, ultraHeight.getHeight());
        values.put(Cols.SPEED, ultraHeight.getSpeed());
        values.put(Cols.DATE, ultraHeight.getDate().getTime());
        return values;
    }


    private UltraHeightSingleton(Context context) {
        mContext = context.getApplicationContext();
        mDatabase = new HeightDBaseHelper(mContext).getWritableDatabase();
    }


    public List<UltraHeight> getData() {
        return getDataFromDB(null, null);
    }

    public List<UltraHeight> getDataOnDate(int year, int month, int day) {

        String whereClause = "date((" +
                Cols.DATE +
                "/ 1000), 'unixepoch') =?";
        String[] whereArgs = new String[]{
                String.format(Locale.US, "%d-%02d-%02d", year, month, day)
        };
        return getDataFromDB(whereClause, whereArgs);
    }

    public List<UltraHeight> getDataFromDB(String whereClause, String[] whereArgs) {
        List<UltraHeight> data = new ArrayList<>();
        try (HeightCursorWrapper cursor = queryUltraHeights(whereClause, whereArgs)) {
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                data.add(cursor.getUltraHeight());
                cursor.moveToNext();
            }
            return data;
        }
    }

    public List<UltraHeight> getLastData(int numOfLastItems) {

        String whereClause = "_id > ((select max(_id) from " + HeightsTable.NAME + ")-?)";
        String[] whereArgs = new String[]{
                String.valueOf(numOfLastItems)
        };
        return getDataFromDB(whereClause, whereArgs);

    }

    public UltraHeight getHeight(UUID id) {
        try (HeightCursorWrapper cursor = queryUltraHeights
                (Cols.UUID + "=?", new String[]{id.toString()})) {
            if (cursor.getCount() == 0) {
                return null;
            }
            cursor.moveToFirst();
            return cursor.getUltraHeight();
        }
    }


    private HeightCursorWrapper queryUltraHeights(String whereClause, String[] whereArgs) {

        Cursor cursor = mDatabase.query(
                HeightsTable.NAME,
                null, //all columns
                whereClause,
                whereArgs,
                null,
                null,
                null
        );
        return new HeightCursorWrapper(cursor);
    }


    public void addItem(float height, float speed) {
        UltraHeight ultraHeight = new UltraHeight(height, speed, Calendar.getInstance().getTime());
        ContentValues values = getContentValues(ultraHeight);
        mDatabase.insert(HeightsTable.NAME, null, values);
    }

    public void updateHeight(UltraHeight ultraHeight) {
        String uuidString = ultraHeight.getUuid().toString();
        ContentValues values = getContentValues(ultraHeight);
        mDatabase.update
                (HeightsTable.NAME, values, Cols.UUID + "=?", new String[]{uuidString});
    }

    /**
     * Возвращает кольчество записей в БД
     *
     * @return
     */
    public int getItemsCount() {
        String selectionStr = "SELECT count() FROM " + HeightsTable.NAME;
        try (Cursor cursor = mDatabase.rawQuery(selectionStr, null)) {
            if (cursor.getCount() == 0) {
                return 0;
            } else {
                cursor.moveToFirst();
                return cursor.getInt(0);
            }
        }
    }

    public Calendar[] getActiveDays() {
        Calendar[] active_days;
        String queryString = "SELECT distinct date((" +
                Cols.DATE +
                "/ 1000), 'unixepoch') FROM " +
                HeightsTable.NAME;
        try (Cursor cursor = mDatabase.rawQuery(queryString, null)) {
            if (cursor.getCount() == 0) {
                return null;
            } else {
                active_days = new Calendar[cursor.getCount()];
                cursor.moveToFirst();
                for (int i = 0; i < cursor.getCount(); i++) {

                    // TODO Вероятое появление ошибки при выдачи даты в другом формате
                    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                    String dateStr = cursor.getString(0);
                    Date date = null;
                    try {
                        date = dateFormat.parse(dateStr);
                    } catch (ParseException e) {
                        Log.e(TAG, "Can't parse date from DB " + e);
                        return null;
                    }
                    if (date != null) {
                        Calendar day = Calendar.getInstance();
                        day.setTime(date);
                        active_days[i] = day;
                    }
                    cursor.moveToNext();
                }
                return active_days;
            }
        }
    }
}
