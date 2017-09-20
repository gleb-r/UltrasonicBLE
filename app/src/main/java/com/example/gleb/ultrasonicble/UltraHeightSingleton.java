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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
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
        values.put(Cols.DATE, ultraHeight.getDate().getTime());
        return values;
    }


    private UltraHeightSingleton(Context context) {
        mContext = context.getApplicationContext();
        mDatabase = new HeightDBaseHelper(mContext).getWritableDatabase();
    }


    public List<UltraHeight> getData() {
        List<UltraHeight> data = new ArrayList<>();
        try (HeightCursorWrapper cursor = queryUltraHeights(null, null)) {
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                data.add(cursor.getHeight());
                cursor.moveToNext();
            }
            return data;
        }
    }

    public List<UltraHeight> getLastData (int numOfLastItems) {
        List<UltraHeight> ultraHeights = new ArrayList<>();

        String whereClause = "_id > ((select max(_id) from " + HeightsTable.NAME + ")-?)";
        String[] whereArgs =  new String[]{String.valueOf(numOfLastItems)};

        try (HeightCursorWrapper cursor = queryUltraHeights(whereClause,whereArgs)) {
            cursor.moveToFirst();
            Log.i(TAG,"cursor size= " +cursor.getCount());
            while (!cursor.isAfterLast()) {
                ultraHeights.add(cursor.getHeight());
                cursor.moveToNext();
            }
            return ultraHeights;
        }
    }

    public UltraHeight getHeight(UUID id) {
        try (HeightCursorWrapper cursor = queryUltraHeights
                (Cols.UUID + "=?", new String[]{id.toString()})) {
            if (cursor.getCount() == 0) {
                return null;
            }
            cursor.moveToFirst();
            return cursor.getHeight();
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
//        if (whereArgs!=null && whereArgs.length!=0) {
//            Log.i(TAG, "whereClause = " + whereClause + " whereArgs=" + whereArgs[0] );
//        }

        return new HeightCursorWrapper(cursor);
    }





    public void addItem(float height) {
        UltraHeight ultraHeight = new UltraHeight(height, Calendar.getInstance().getTime());
        ContentValues values = getContentValues(ultraHeight);
        mDatabase.insert(HeightsTable.NAME, null, values);
    }

    public void updateHeight(UltraHeight ultraHeight) {
        String uuidString = ultraHeight.getUuid().toString();
        ContentValues values = getContentValues(ultraHeight);
        mDatabase.update
                (HeightsTable.NAME,values,Cols.UUID + "=?", new String[]{uuidString});
    }





}
