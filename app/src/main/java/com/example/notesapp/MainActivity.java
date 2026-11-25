package com.example.notesapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.notesapp.adapter.NotesAdapter;
import com.example.notesapp.model.Note;
import com.example.notesapp.storage.JsonNoteStorage;
import com.example.notesapp.storage.NoteStorage;
import com.example.notesapp.storage.SqliteNoteStorage;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    // ── Intent extra keys (shared across activities) ──────────────────────────
    public static final String EXTRA_NOTE_ID = "note_id";
    public static final String EXTRA_SOURCE  = "source";
    public static final String EXTRA_MODE    = "mode";
    public static final String EXTRA_TITLE   = "title";
    public static final String EXTRA_CONTENT = "content";

    public static final String MODE_ADD  = "add";
    public static final String MODE_EDIT = "edit";

    public static final String SOURCE_SQLITE = "sqlite";
    public static final String SOURCE_JSON   = "json";

    // ── SharedPreferences ─────────────────────────────────────────────────────
    private static final String PREFS_NAME  = "NotesAppPrefs";
    private static final String PREF_SOURCE = "selected_source";

    // ── UI ────────────────────────────────────────────────────────────────────
    private RecyclerView recyclerView;
    private NotesAdapter adapter;
    private RadioGroup radioGroupSource;

    // ── Storage ───────────────────────────────────────────────────────────────
    private SqliteNoteStorage sqliteStorage;
    private JsonNoteStorage   jsonStorage;

    // ── State ─────────────────────────────────────────────────────────────────
    private SharedPreferences prefs;
    private String currentSource;

    // ── Activity result launcher ──────────────────────────────────────────────
    private final ActivityResultLauncher<Intent> activityLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    refreshNotesList();
                }
            });

    // ─────────────────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Enable edge-to-edge explicitly (also enforced automatically on API 35+).
        // This makes the window render behind the system bars so we can control insets.
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Storage instances
        sqliteStorage = new SqliteNoteStorage(this);
        jsonStorage   = new JsonNoteStorage(this);

        // Preferences
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        currentSource = prefs.getString(PREF_SOURCE, SOURCE_SQLITE);

        // Views
        recyclerView      = findViewById(R.id.recycler_view_notes);
        radioGroupSource  = findViewById(R.id.radio_group_source);
        Button btnSync    = findViewById(R.id.btn_sync);
        FloatingActionButton fabAdd = findViewById(R.id.fab_add_note);

        setupRecyclerView();
        setupRadioGroup();
        btnSync.setOnClickListener(v -> performSync());
        fabAdd.setOnClickListener(v -> openAddNote());

        // WindowInsets: CoordinatorLayout + AppBarLayout handle the status bar top
        // inset automatically via fitsSystemWindows="true" set in XML.
        // We only need to handle the FAB bottom margin for the navigation bar.
        applyWindowInsets(fabAdd);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshNotesList();
    }

    // ── WindowInsets ──────────────────────────────────────────────────────────

    /**
     * Adjusts the FAB bottom/end margin so it stays above the navigation bar,
     * and adds extra RecyclerView bottom padding for the same reason.
     */
    private void applyWindowInsets(FloatingActionButton fab) {
        final int fabDefaultMarginPx =
                (int) (16 * getResources().getDisplayMetrics().density + 0.5f);

        ViewCompat.setOnApplyWindowInsetsListener(fab, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            android.view.ViewGroup.MarginLayoutParams lp =
                    (android.view.ViewGroup.MarginLayoutParams) v.getLayoutParams();
            lp.bottomMargin = fabDefaultMarginPx + bars.bottom;
            lp.rightMargin  = fabDefaultMarginPx + bars.right;
            v.setLayoutParams(lp);
            return insets; // don't consume — siblings may still need insets
        });

        // Extra bottom padding in RecyclerView so last item isn't hidden by nav bar / FAB
        ViewCompat.setOnApplyWindowInsetsListener(recyclerView, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            int bottomPx = (int) (88 * getResources().getDisplayMetrics().density + 0.5f);
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(),
                    v.getPaddingRight(), bottomPx + bars.bottom);
            return insets;
        });
    }

    // ── Setup helpers ─────────────────────────────────────────────────────────

    private void setupRecyclerView() {
        List<Note> notes = getActiveStorage().getAllNotes();
        adapter = new NotesAdapter(notes, this::openNoteDetail);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void setupRadioGroup() {
        radioGroupSource.check(
                SOURCE_SQLITE.equals(currentSource) ? R.id.radio_sqlite : R.id.radio_json
        );

        radioGroupSource.setOnCheckedChangeListener((group, checkedId) -> {
            currentSource = (checkedId == R.id.radio_sqlite) ? SOURCE_SQLITE : SOURCE_JSON;
            prefs.edit().putString(PREF_SOURCE, currentSource).apply();
            refreshNotesList();
        });
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    private void openNoteDetail(Note note) {
        Intent intent = new Intent(this, NoteDetailActivity.class);
        intent.putExtra(EXTRA_NOTE_ID, note.getId());
        intent.putExtra(EXTRA_SOURCE,  currentSource);
        activityLauncher.launch(intent);
    }

    private void openAddNote() {
        Intent intent = new Intent(this, AddEditNoteActivity.class);
        intent.putExtra(EXTRA_MODE,   MODE_ADD);
        intent.putExtra(EXTRA_SOURCE, currentSource);
        activityLauncher.launch(intent);
    }

    // ── Sync ──────────────────────────────────────────────────────────────────

    private void performSync() {
        List<Note> sqliteNotes = sqliteStorage.getAllNotes();
        if (sqliteNotes.isEmpty()) {
            Toast.makeText(this, R.string.toast_sync_empty, Toast.LENGTH_SHORT).show();
            return;
        }
        boolean success = jsonStorage.overwriteAll(sqliteNotes);
        if (success) {
            Toast.makeText(this,
                    getString(R.string.toast_sync_success, sqliteNotes.size()),
                    Toast.LENGTH_SHORT).show();
            refreshNotesList();
        } else {
            Toast.makeText(this, R.string.toast_operation_failed, Toast.LENGTH_SHORT).show();
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void refreshNotesList() {
        adapter.updateNotes(getActiveStorage().getAllNotes());
    }

    private NoteStorage getActiveStorage() {
        return SOURCE_SQLITE.equals(currentSource) ? sqliteStorage : jsonStorage;
    }
}
