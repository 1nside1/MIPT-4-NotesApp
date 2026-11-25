package com.example.notesapp;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ScrollView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.notesapp.model.Note;
import com.example.notesapp.storage.JsonNoteStorage;
import com.example.notesapp.storage.NoteStorage;
import com.example.notesapp.storage.SqliteNoteStorage;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class AddEditNoteActivity extends AppCompatActivity {

    private TextInputLayout   tilTitle;
    private TextInputLayout   tilContent;
    private TextInputEditText etTitle;
    private TextInputEditText etContent;

    private NoteStorage storage;
    private String mode;
    private String source;
    private long noteId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Enable edge-to-edge so we can control exactly where content sits.
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_note);

        // ── Toolbar setup ─────────────────────────────────────────────────────
        // setSupportActionBar hands menu management to the ActionBar framework:
        //   • onCreateOptionsMenu inflates the Save button into the toolbar
        //   • onOptionsItemSelected handles both android.R.id.home (back) and action_save
        // toolbar.setNavigationOnClickListener is set as a direct safety-net listener.
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        // Direct navigation listener — fires even if the ActionBar callback is missed.
        toolbar.setNavigationOnClickListener(v -> finish());

        // ── Intent data ───────────────────────────────────────────────────────
        mode   = getIntent().getStringExtra(MainActivity.EXTRA_MODE);
        source = getIntent().getStringExtra(MainActivity.EXTRA_SOURCE);
        noteId = getIntent().getLongExtra(MainActivity.EXTRA_NOTE_ID, -1);

        storage = MainActivity.SOURCE_SQLITE.equals(source)
                ? new SqliteNoteStorage(this)
                : new JsonNoteStorage(this);

        // ── Views ─────────────────────────────────────────────────────────────
        tilTitle   = findViewById(R.id.til_title);
        tilContent = findViewById(R.id.til_content);
        etTitle    = findViewById(R.id.et_title);
        etContent  = findViewById(R.id.et_content);

        // ── Mode-specific setup ───────────────────────────────────────────────
        if (MainActivity.MODE_EDIT.equals(mode)) {
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(R.string.title_edit_note);
            }
            // Only populate from Intent on initial creation; on configuration change (e.g. rotation),
            // savedInstanceState preserves the user's edits automatically.
            if (savedInstanceState == null) {
                etTitle.setText(getIntent().getStringExtra(MainActivity.EXTRA_TITLE));
                etContent.setText(getIntent().getStringExtra(MainActivity.EXTRA_CONTENT));
            }
        } else {
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(R.string.title_add_note);
            }
        }

        // ── WindowInsets ──────────────────────────────────────────────────────
        applyWindowInsets();
    }

    // ── WindowInsets ──────────────────────────────────────────────────────────

    /**
     * Applies system bar insets:
     *  • Status bar → top padding on AppBarLayout  (Toolbar stays below status bar)
     *  • Nav bar    → bottom padding on ScrollView (fields not hidden behind nav bar)
     *
     * LinearLayout root doesn't auto-dispatch fitsSystemWindows to children,
     * so we set listeners directly on each target view.
     */
    private void applyWindowInsets() {
        AppBarLayout appBarLayout  = findViewById(R.id.app_bar_layout);
        ScrollView   scrollView    = findViewById(R.id.scroll_view_content);

        // Status bar → AppBarLayout top padding
        ViewCompat.setOnApplyWindowInsetsListener(appBarLayout, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, bars.top, 0, 0);
            return insets; // don't consume — ScrollView sibling still needs bottom inset
        });

        // Nav bar → ScrollView bottom padding (soft keyboard also handled via adjustResize)
        ViewCompat.setOnApplyWindowInsetsListener(scrollView, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(),
                    v.getPaddingRight(), bars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
    }

    // ── Options menu (Save button in Toolbar) ─────────────────────────────────

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_add_edit, menu);
        return true;
    }

    /**
     * Handles both the Up/Back arrow (android.R.id.home) and the Save menu item.
     * This is the primary handler when setSupportActionBar is used.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
            return true;
        }
        if (id == R.id.action_save) {
            saveNote();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // ── Save logic ────────────────────────────────────────────────────────────

    private void saveNote() {
        String title   = etTitle.getText()   != null ? etTitle.getText().toString().trim()   : "";
        String content = etContent.getText() != null ? etContent.getText().toString().trim() : "";

        boolean valid = true;

        if (title.isEmpty()) {
            tilTitle.setError(getString(R.string.error_empty_title));
            valid = false;
        } else {
            tilTitle.setError(null);
        }

        if (content.isEmpty()) {
            tilContent.setError(getString(R.string.error_empty_content));
            valid = false;
        } else {
            tilContent.setError(null);
        }

        if (!valid) {
            Toast.makeText(this, R.string.toast_validation_error, Toast.LENGTH_SHORT).show();
            return;
        }

        Note note = new Note(title, content);
        boolean success;

        if (MainActivity.MODE_EDIT.equals(mode)) {
            note.setId(noteId);
            success = storage.updateNote(note);
            if (success) {
                Toast.makeText(this, R.string.toast_note_updated, Toast.LENGTH_SHORT).show();
            }
        } else {
            long newId = storage.addNote(note);
            success = newId != -1;
            if (success) {
                Toast.makeText(this, R.string.toast_note_saved, Toast.LENGTH_SHORT).show();
            }
        }

        if (success) {
            setResult(RESULT_OK);
            finish();
        } else {
            Toast.makeText(this, R.string.toast_operation_failed, Toast.LENGTH_SHORT).show();
        }
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
