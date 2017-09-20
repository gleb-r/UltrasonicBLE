package com.example.gleb.ultrasonicble.database;

/**
 * Created by Gleb on 14.09.2017.
 */

public class DbSchema {

    public static final class HeightsTable {

        public static final String NAME = "heights";

        public static final class Cols {
            public static final String UUID = "uuid";
            public static final String HEIGHT = "height";
            public static final String DATE = "date";
        }
    }
}
