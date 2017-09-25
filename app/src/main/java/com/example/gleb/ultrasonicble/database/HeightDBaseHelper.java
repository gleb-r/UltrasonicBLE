package com.example.gleb.ultrasonicble.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.gleb.ultrasonicble.database.DbSchema.HeightsTable;
import com.example.gleb.ultrasonicble.database.DbSchema.HeightsTable.Cols;

/**
 * Created by Gleb on 14.09.2017.
 */

public class HeightDBaseHelper extends SQLiteOpenHelper {
    public static final int VERSION = 1;
    public static final String DATABASE_NAME = "heightsBase.db";

    public HeightDBaseHelper(Context context) {
        super(context, DATABASE_NAME, null, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("create table " + HeightsTable.NAME +
                "(" + "_id integer primary key autoincrement, " +
                Cols.UUID + ", " +
                Cols.HEIGHT + ", " +
                Cols.SPEED + ", " +
                Cols.DATE + ")"
        );

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
