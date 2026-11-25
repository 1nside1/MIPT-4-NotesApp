package com.example.notesapp.storage;

import com.example.notesapp.model.Note;

import java.util.List;

public interface NoteStorage {
    long addNote(Note note);
    boolean updateNote(Note note);
    boolean deleteNote(long id);
    List<Note> getAllNotes();
    Note getNoteById(long id);
}
