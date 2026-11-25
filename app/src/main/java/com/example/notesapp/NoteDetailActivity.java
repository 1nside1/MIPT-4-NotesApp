package com.example.notesapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
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
import com.google.android.material.button.MaterialButton;

public class NoteDetailActivity extends AppCompatActivity {

    private long noteId;
    private String source;
    private Note currentNote;
    private NoteStorage storage;

    private TextView tvTitle;
    private TextView tvContent;

    private final ActivityResultLauncher<Intent> editLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    if (loadNote()) {
                        setResult(RESULT_OK);
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Enable edge-to-edge so we can control exactly where content sits.
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_detail);

        // ── Toolbar setup ─────────────────────────────────────────────────────
        // setSupportActionBar delegates menu inflation and navigation to the
        // ActionBar framework. The back-arrow click is routed to onSupportNavigateUp.
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        // Also set a direct listener on the toolbar navigation icon as a safety net
        // (some edge cases on API 35+ where onSupportNavigateUp is not triggered).
        toolbar.setNavigationOnClickListener(v -> finish());

        // ── Intent data ───────────────────────────────────────────────────────
        noteId = getIntent().getLongExtra(MainActivity.EXTRA_NOTE_ID, -1);
        source = getIntent().getStringExtra(MainActivity.EXTRA_SOURCE);

        storage = MainActivity.SOURCE_SQLITE.equals(source)
                ? new SqliteNoteStorage(this)
                : new JsonNoteStorage(this);

        // ── Views ─────────────────────────────────────────────────────────────
        tvTitle   = findViewById(R.id.tv_detail_title);
        tvContent = findViewById(R.id.tv_detail_content);
        MaterialButton btnEdit   = findViewById(R.id.btn_edit);
        MaterialButton btnDelete = findViewById(R.id.btn_delete);

        if (!loadNote()) {
            return;
        }

        btnEdit.setOnClickListener(v -> openEditNote());
        btnDelete.setOnClickListener(v -> showDeleteConfirmDialog());

        // ── WindowInsets ──────────────────────────────────────────────────────
        applyWindowInsets();
    }

    // ── WindowInsets ──────────────────────────────────────────────────────────

    /**
     * Applies system bar insets:
     *  • Status bar  → top padding on AppBarLayout  (Toolbar stays below status bar)
     *  • Nav bar     → bottom padding on buttons row (buttons stay above nav bar)
     *
     * LinearLayout root doesn't auto-dispatch fitsSystemWindows to children,
     * so we set listeners directly on each target view.
     */
    private void applyWindowInsets() {
        AppBarLayout appBarLayout = findViewById(R.id.app_bar_layout);
        LinearLayout layoutButtons = findViewById(R.id.layout_buttons);

        // Status bar → AppBarLayout top padding
        // We do NOT return CONSUMED so the insets continue to reach sibling views.
        ViewCompat.setOnApplyWindowInsetsListener(appBarLayout, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, bars.top, 0, 0);
            return insets;
        });

        // Nav bar → buttons container bottom padding (keeps buttons above nav bar)
        ViewCompat.setOnApplyWindowInsetsListener(layoutButtons, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            int paddingPx = (int) (12 * getResources().getDisplayMetrics().density + 0.5f);
            v.setPadding(paddingPx, paddingPx, paddingPx, paddingPx + bars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private boolean loadNote() {
        if (noteId == -1) {
            Toast.makeText(this, R.string.toast_note_not_found, Toast.LENGTH_SHORT).show();
            finish();
            return false;
        }
        currentNote = storage.getNoteById(noteId);
        if (currentNote != null) {
            tvTitle.setText(currentNote.getTitle());
            tvContent.setText(currentNote.getContent());
            return true;
        } else {
            Toast.makeText(this, R.string.toast_note_not_found, Toast.LENGTH_SHORT).show();
            finish();
            return false;
        }
    }

    private void openEditNote() {
        if (currentNote == null) return;
        Intent intent = new Intent(this, AddEditNoteActivity.class);
        intent.putExtra(MainActivity.EXTRA_MODE,    MainActivity.MODE_EDIT);
        intent.putExtra(MainActivity.EXTRA_SOURCE,  source);
        intent.putExtra(MainActivity.EXTRA_NOTE_ID, noteId);
        intent.putExtra(MainActivity.EXTRA_TITLE,   currentNote.getTitle());
        intent.putExtra(MainActivity.EXTRA_CONTENT, currentNote.getContent());
        editLauncher.launch(intent);
    }

    private void showDeleteConfirmDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_delete_title)
                .setMessage(R.string.dialog_delete_message)
                .setPositiveButton(R.string.dialog_yes, (dialog, which) -> {
                    boolean deleted = storage.deleteNote(noteId);
                    if (deleted) {
                        Toast.makeText(this, R.string.toast_note_deleted, Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                    } else {
                        Toast.makeText(this, R.string.toast_operation_failed, Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(R.string.dialog_no, null)
                .show();
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        // android.R.id.home = back/up arrow on the ActionBar
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
