package com.example.gleb.ultrasonicble.database;

import android.database.Cursor;
import android.database.CursorWrapper;

import com.example.gleb.ultrasonicble.UltraHeight;
import com.example.gleb.ultrasonicble.database.DbSchema.HeightsTable.Cols;

import java.util.Date;
import java.util.UUID;

/**
 * Created by Gleb on 14.09.2017.
 */

public class HeightCursorWrapper extends CursorWrapper {

    public HeightCursorWrapper (Cursor cursor) {
        super(cursor);
    }


    public UltraHeight getUltraHeight() {
        String uuidString = getString(getColumnIndex(Cols.UUID));
        float height = getFloat(getColumnIndex(Cols.HEIGHT));
        float speed = getFloat(getColumnIndex(Cols.SPEED));
        long date = getLong(getColumnIndex(Cols.DATE));
        UltraHeight ultraHeight = new UltraHeight(
                UUID.fromString(uuidString),
                height,
                speed,
                new Date(date)
        );
        return ultraHeight;
    }


}
