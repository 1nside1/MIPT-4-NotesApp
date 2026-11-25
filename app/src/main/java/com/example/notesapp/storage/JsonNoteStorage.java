package com.example.notesapp.storage;

import android.content.Context;
import android.util.Log;

import com.example.notesapp.model.Note;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class JsonNoteStorage implements NoteStorage {

    private static final String TAG = "JsonNoteStorage";
    private static final String FILE_NAME = "notes_secondary.json";
    private static final String KEY_ID = "id";
    private static final String KEY_TITLE = "title";
    private static final String KEY_CONTENT = "content";

    private final File file;
    private final Object lock = new Object();

    public JsonNoteStorage(Context context) {
        file = new File(context.getFilesDir(), FILE_NAME);
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    private List<Note> readAll() {
        synchronized (lock) {
            List<Note> notes = new ArrayList<>();
            if (!file.exists()) return notes;
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                JSONArray array = new JSONArray(sb.toString());
                for (int i = 0; i < array.length(); i++) {
                    JSONObject obj = array.getJSONObject(i);
                    notes.add(new Note(
                            obj.getLong(KEY_ID),
                            obj.getString(KEY_TITLE),
                            obj.getString(KEY_CONTENT)
                    ));
                }
            } catch (IOException | JSONException e) {
                Log.e(TAG, "Error reading JSON file", e);
            }
            return notes;
        }
    }

    private boolean writeAll(List<Note> notes) {
        synchronized (lock) {
            File tempFile = new File(file.getParentFile(), FILE_NAME + ".tmp");
            try {
                JSONArray array = new JSONArray();
                for (Note note : notes) {
                    JSONObject obj = new JSONObject();
                    obj.put(KEY_ID, note.getId());
                    obj.put(KEY_TITLE, note.getTitle());
                    obj.put(KEY_CONTENT, note.getContent());
                    array.put(obj);
                }
                try (FileWriter writer = new FileWriter(tempFile, false)) {
                    writer.write(array.toString());
                    writer.flush();
                }
                if (tempFile.exists()) {
                    if (file.exists() && !file.delete()) {
                        Log.w(TAG, "Failed to delete existing JSON file prior to rename");
                    }
                    if (!tempFile.renameTo(file)) {
                        Log.e(TAG, "Failed to atomically rename temp file to target JSON file");
                        return false;
                    }
                }
                return true;
            } catch (IOException | JSONException e) {
                Log.e(TAG, "Error writing JSON file", e);
                if (tempFile.exists()) {
                    tempFile.delete();
                }
                return false;
            }
        }
    }

    /** Generates next id = max existing id + 1 (mimics SQLite auto-increment). */
    private long generateId(List<Note> notes) {
        long maxId = 0;
        for (Note note : notes) {
            if (note.getId() > maxId) maxId = note.getId();
        }
        return maxId + 1;
    }

    // ── NoteStorage implementation ────────────────────────────────────────────

    @Override
    public long addNote(Note note) {
        synchronized (lock) {
            List<Note> notes = readAll();
            long id = generateId(notes);
            note.setId(id);
            notes.add(note);
            if (writeAll(notes)) {
                return id;
            }
            return -1;
        }
    }

    @Override
    public boolean updateNote(Note note) {
        synchronized (lock) {
            List<Note> notes = readAll();
            for (int i = 0; i < notes.size(); i++) {
                if (notes.get(i).getId() == note.getId()) {
                    notes.set(i, note);
                    return writeAll(notes);
                }
            }
            return false;
        }
    }

    @Override
    public boolean deleteNote(long id) {
        synchronized (lock) {
            List<Note> notes = readAll();
            for (int i = 0; i < notes.size(); i++) {
                if (notes.get(i).getId() == id) {
                    notes.remove(i);
                    return writeAll(notes);
                }
            }
            return false;
        }
    }

    @Override
    public List<Note> getAllNotes() {
        return readAll();
    }

    @Override
    public Note getNoteById(long id) {
        for (Note note : readAll()) {
            if (note.getId() == id) return note;
        }
        return null;
    }

    /**
     * Full overwrite — replaces the entire JSON file with the given list.
     * Called by the Sync operation in MainActivity.
     */
    public boolean overwriteAll(List<Note> notes) {
        return writeAll(notes);
    }
}
