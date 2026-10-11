package com.generated.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

    private static final String PREFS_NAME = "secret_notes_prefs";
    private static final String KEY_SECRET_ACTIVE = "secret_offset_active";
    private static final String KEY_SECRET_OFFSET = "secret_offset_value";

    private NoteDatabaseHelper dbHelper;
    private final List<Note> noteList = new ArrayList<Note>();
    private NotesAdapter adapter;
    private ListView lvNotes;
    private TextView tvEmpty;

    // Secret config stored in SharedPreferences
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        dbHelper = new NoteDatabaseHelper(this);

        lvNotes = (ListView) findViewById(R.id.lvNotes);
        tvEmpty = (TextView) findViewById(R.id.tvEmpty);
        ImageButton fabAdd = (ImageButton) findViewById(R.id.fabAdd);
        TextView tvAppTitle = (TextView) findViewById(R.id.tvAppTitle);
        TextView tvSecretHint = (TextView) findViewById(R.id.tvSecretHint);

        adapter = new NotesAdapter();
        lvNotes.setAdapter(adapter);

        // Click note to edit
        lvNotes.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Note note = noteList.get(position);
                openNoteEditorDialog(note);
            }
        });

        // Long click note to delete
        lvNotes.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                final Note note = noteList.get(position);
                showDeleteDialog(note);
                return true;
            }
        });

        // FAB to add new note
        fabAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openNoteEditorDialog(null);
            }
        });

        // Hidden Setting Trigger 1: Long Press on the App Title
        tvAppTitle.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                openSecretSettingsDialog();
                return true;
            }
        });

        // Hidden Setting Trigger 2: Tap the hint icon
        tvSecretHint.setOnClickListener(new View.OnClickListener() {
            private int clickCount = 0;
            private long lastClickTime = 0;

            @Override
            public void onClick(View v) {
                long now = System.currentTimeMillis();
                if (now - lastClickTime < 600) {
                    clickCount++;
                } else {
                    clickCount = 1;
                }
                lastClickTime = now;

                if (clickCount >= 2) {
                    clickCount = 0;
                    openSecretSettingsDialog();
                } else {
                    Toast.makeText(MainActivity.this, "לחץ לחיצה ארוכה על כותרת האפליקציה או הקש שוב להגדרות סודיות", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Load default sample if first launch
        loadNotes();
        if (noteList.isEmpty()) {
            addSampleNote();
            loadNotes();
        }
    }

    private void addSampleNote() {
        String now = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date());
        dbHelper.insertNote(
                "חשבון קניות לדוגמה",
                "קניות לסופר:\nתפוחים: 5*5=\nחלב ולחם: 12+8=\nסך הכל לפני הנחה",
                now
        );
    }

    private void loadNotes() {
        noteList.clear();
        noteList.addAll(dbHelper.getAllNotes());
        adapter.notifyDataSetChanged();
        if (noteList.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
        } else {
            tvEmpty.setVisibility(View.GONE);
        }
    }

    private boolean isSecretActive() {
        return prefs.getBoolean(KEY_SECRET_ACTIVE, true);
    }

    private double getSecretOffset() {
        try {
            String val = prefs.getString(KEY_SECRET_OFFSET, "10");
            return Double.parseDouble(val);
        } catch (Exception e) {
            return 10.0;
        }
    }

    /**
     * Dialog to write/edit a note with built-in arithmetic solver!
     */
    private void openNoteEditorDialog(final Note existingNote) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_note, null);
        builder.setView(dialogView);

        final EditText etTitle = (EditText) dialogView.findViewById(R.id.etTitle);
        final EditText etContent = (EditText) dialogView.findViewById(R.id.etContent);
        final EditText etMathInput = (EditText) dialogView.findViewById(R.id.etMathInput);
        Button btnQuickCalc = (Button) dialogView.findViewById(R.id.btnQuickCalc);
        Button btnSolveInText = (Button) dialogView.findViewById(R.id.btnSolveInText);

        if (existingNote != null) {
            etTitle.setText(existingNote.getTitle());
            etContent.setText(existingNote.getContent());
        }

        // Quick calculator bar in the note
        btnQuickCalc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String mathExp = etMathInput.getText().toString().trim();
                if (mathExp.isEmpty()) {
                    Toast.makeText(MainActivity.this, "הזן ביטוי לחישוב (למשל 5*5)", Toast.LENGTH_SHORT).show();
                    return;
                }
                try {
                    boolean secretOn = isSecretActive();
                    double offset = getSecretOffset();
                    double res = MathEvaluator.evaluateExpression(mathExp, secretOn, offset);
                    String resultFormatted = MathEvaluator.formatResult(res);

                    // Insert calculation into note content
                    String current = etContent.getText().toString();
                    String line = mathExp + " = " + resultFormatted;
                    if (!current.isEmpty() && !current.endsWith("\n")) {
                        current += "\n";
                    }
                    current += line;
                    etContent.setText(current);
                    etContent.setSelection(etContent.getText().length());
                    etMathInput.setText("");

                    Toast.makeText(MainActivity.this, mathExp + " = " + resultFormatted, Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "שגיאה בחישוב הביטוי", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Solve math occurrences written directly inside the note text
        btnSolveInText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String original = etContent.getText().toString();
                boolean secretOn = isSecretActive();
                double offset = getSecretOffset();

                String solved = MathEvaluator.solveExpressionsInText(original, secretOn, offset);
                if (!solved.equals(original)) {
                    etContent.setText(solved);
                    etContent.setSelection(etContent.getText().length());
                    Toast.makeText(MainActivity.this, R.string.calc_done, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, R.string.calc_none_found, Toast.LENGTH_LONG).show();
                }
            }
        });

        builder.setTitle(existingNote == null ? R.string.new_note : R.string.app_name);
        builder.setPositiveButton(R.string.btn_save, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String title = etTitle.getText().toString().trim();
                String content = etContent.getText().toString().trim();
                if (title.isEmpty() && content.isEmpty()) {
                    return;
                }
                if (title.isEmpty()) {
                    title = content.length() > 20 ? content.substring(0, 20) + "..." : content;
                }
                String date = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date());

                if (existingNote == null) {
                    dbHelper.insertNote(title, content, date);
                } else {
                    dbHelper.updateNote(existingNote.getId(), title, content, date);
                }
                loadNotes();
            }
        });

        builder.setNegativeButton(R.string.btn_cancel, null);
        builder.show();
    }

    /**
     * Secret dialog where the user can set how much is automatically added to calculations.
     */
    private void openSecretSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.secret_title);

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_secret_settings, null);
        builder.setView(dialogView);

        final Switch swSecretActive = (Switch) dialogView.findViewById(R.id.swSecretActive);
        final EditText etSecretOffset = (EditText) dialogView.findViewById(R.id.etSecretOffset);
        final TextView tvPreview = (TextView) dialogView.findViewById(R.id.tvPreview);

        boolean currentActive = prefs.getBoolean(KEY_SECRET_ACTIVE, true);
        String currentOffset = prefs.getString(KEY_SECRET_OFFSET, "10");

        swSecretActive.setChecked(currentActive);
        etSecretOffset.setText(currentOffset);

        final Runnable updatePreview = new Runnable() {
            @Override
            public void run() {
                try {
                    boolean active = swSecretActive.isChecked();
                    String raw = etSecretOffset.getText().toString().trim();
                    double off = raw.isEmpty() ? 0 : Double.parseDouble(raw);
                    double base = 25.0; // 5 * 5
                    double result = active ? (base + off) : base;
                    String previewStr = "דוגמה: 5 * 5 = " + MathEvaluator.formatResult(result);
                    if (active && off != 0) {
                        previewStr += " (הוספה: " + (off > 0 ? "+" : "") + MathEvaluator.formatResult(off) + ")";
                    }
                    tvPreview.setText(previewStr);
                } catch (Exception e) {
                    tvPreview.setText("שגיאה בערך ההיסט");
                }
            }
        };

        updatePreview.run();

        etSecretOffset.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updatePreview.run();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        swSecretActive.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                updatePreview.run();
            }
        });

        builder.setPositiveButton(R.string.btn_save, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                boolean active = swSecretActive.isChecked();
                String offStr = etSecretOffset.getText().toString().trim();
                if (offStr.isEmpty()) {
                    offStr = "0";
                }
                prefs.edit()
                        .putBoolean(KEY_SECRET_ACTIVE, active)
                        .putString(KEY_SECRET_OFFSET, offStr)
                        .apply();

                Toast.makeText(MainActivity.this, R.string.secret_saved, Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton(R.string.btn_cancel, null);
        builder.show();
    }

    private void showDeleteDialog(final Note note) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_confirm_title)
                .setMessage(R.string.delete_confirm_msg)
                .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dbHelper.deleteNote(note.getId());
                        loadNotes();
                    }
                })
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }

    private class NotesAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return noteList.size();
        }

        @Override
        public Object getItem(int position) {
            return noteList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return noteList.get(position).getId();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = LayoutInflater.from(MainActivity.this).inflate(R.layout.item_note, parent, false);
                holder = new ViewHolder();
                holder.title = (TextView) convertView.findViewById(R.id.itemTitle);
                holder.date = (TextView) convertView.findViewById(R.id.itemDate);
                holder.snippet = (TextView) convertView.findViewById(R.id.itemSnippet);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            Note note = noteList.get(position);
            holder.title.setText(note.getTitle());
            holder.date.setText(note.getUpdatedAt());
            holder.snippet.setText(note.getContent());

            return convertView;
        }

        class ViewHolder {
            TextView title;
            TextView date;
            TextView snippet;
        }
    }
}
