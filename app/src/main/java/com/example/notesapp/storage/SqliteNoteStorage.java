package com.example.notesapp.storage;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.example.notesapp.db.NotesDatabaseHelper;
import com.example.notesapp.model.Note;

import java.util.ArrayList;
import java.util.List;

public class SqliteNoteStorage implements NoteStorage {

    private static final String TAG = "SqliteNoteStorage";
    private final NotesDatabaseHelper dbHelper;

    public SqliteNoteStorage(Context context) {
        dbHelper = NotesDatabaseHelper.getInstance(context);
    }

    @Override
    public long addNote(Note note) {
        try {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(NotesDatabaseHelper.COL_TITLE, note.getTitle());
            values.put(NotesDatabaseHelper.COL_CONTENT, note.getContent());
            return db.insert(NotesDatabaseHelper.TABLE_NOTES, null, values);
        } catch (Exception e) {
            Log.e(TAG, "Error adding note to SQLite", e);
            return -1;
        }
    }

    @Override
    public boolean updateNote(Note note) {
        try {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(NotesDatabaseHelper.COL_TITLE, note.getTitle());
            values.put(NotesDatabaseHelper.COL_CONTENT, note.getContent());
            int rows = db.update(
                    NotesDatabaseHelper.TABLE_NOTES,
                    values,
                    NotesDatabaseHelper.COL_ID + "=?",
                    new String[]{String.valueOf(note.getId())}
            );
            return rows > 0;
        } catch (Exception e) {
            Log.e(TAG, "Error updating note in SQLite", e);
            return false;
        }
    }

    @Override
    public boolean deleteNote(long id) {
        try {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            int rows = db.delete(
                    NotesDatabaseHelper.TABLE_NOTES,
                    NotesDatabaseHelper.COL_ID + "=?",
                    new String[]{String.valueOf(id)}
            );
            return rows > 0;
        } catch (Exception e) {
            Log.e(TAG, "Error deleting note from SQLite", e);
            return false;
        }
    }

    @Override
    public List<Note> getAllNotes() {
        List<Note> notes = new ArrayList<>();
        try {
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            try (Cursor cursor = db.query(
                    NotesDatabaseHelper.TABLE_NOTES,
                    null, null, null, null, null,
                    NotesDatabaseHelper.COL_ID + " ASC"
            )) {
                if (cursor != null && cursor.moveToFirst()) {
                    int idCol = cursor.getColumnIndexOrThrow(NotesDatabaseHelper.COL_ID);
                    int titleCol = cursor.getColumnIndexOrThrow(NotesDatabaseHelper.COL_TITLE);
                    int contentCol = cursor.getColumnIndexOrThrow(NotesDatabaseHelper.COL_CONTENT);
                    do {
                        long id = cursor.getLong(idCol);
                        String title = cursor.getString(titleCol);
                        String content = cursor.getString(contentCol);
                        notes.add(new Note(id, title, content));
                    } while (cursor.moveToNext());
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error querying all notes from SQLite", e);
        }
        return notes;
    }

    @Override
    public Note getNoteById(long id) {
        try {
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            try (Cursor cursor = db.query(
                    NotesDatabaseHelper.TABLE_NOTES,
                    null,
                    NotesDatabaseHelper.COL_ID + "=?",
                    new String[]{String.valueOf(id)},
                    null, null, null
            )) {
                if (cursor != null && cursor.moveToFirst()) {
                    int titleCol = cursor.getColumnIndexOrThrow(NotesDatabaseHelper.COL_TITLE);
                    int contentCol = cursor.getColumnIndexOrThrow(NotesDatabaseHelper.COL_CONTENT);
                    String title = cursor.getString(titleCol);
                    String content = cursor.getString(contentCol);
                    return new Note(id, title, content);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error querying note by id from SQLite: " + id, e);
        }
        return null;
    }
}
