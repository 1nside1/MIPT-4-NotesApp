package com.example.notesapp.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class NotesDatabaseHelper extends SQLiteOpenHelper {

    public static final String DB_NAME = "notes.db";
    public static final int DB_VERSION = 1;
    public static final String TABLE_NOTES = "notes";
    public static final String COL_ID = "id";
    public static final String COL_TITLE = "title";
    public static final String COL_CONTENT = "content";

    private static NotesDatabaseHelper instance;

    public static synchronized NotesDatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new NotesDatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    private static final String CREATE_TABLE =
            "CREATE TABLE " + TABLE_NOTES + " (" +
            COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COL_TITLE + " TEXT NOT NULL, " +
            COL_CONTENT + " TEXT NOT NULL)";

    public NotesDatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NOTES);
        onCreate(db);
    }
}
