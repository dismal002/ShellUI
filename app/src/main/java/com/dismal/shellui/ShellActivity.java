package com.dismal.shellui;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

// import com.android.colorpicker.ColorPickerDialog;
// import com.android.colorpicker.ColorPickerSwatch;

public class ShellActivity extends AppCompatActivity {

    private String outputFilePath;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        String action = intent.getStringExtra("action");
        outputFilePath = intent.getStringExtra("output");

        if (action == null) {
            finish();
            return;
        }

        switch (action) {
            case "toast":
                showToast(intent);
                break;
            case "dialog":
                showDialog(intent);
                break;
            case "input":
                showInputDialog(intent);
                break;
            case "form":
                showFormDialog(intent);
                break;
            case "date":
                showDatePicker(intent);
                break;
            case "time":
                showTimePicker(intent);
                break;
            case "color":
                showColorPicker(intent);
                break;
            case "loading":
                showLoading(intent);
                break;
            case "hide":
                hideLoading();
                break;
            default:
                finish();
        }
    }

    private void showLoading(Intent intent) {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        String title = intent.getStringExtra("title");
        String message = intent.getStringExtra("message");
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle(title != null ? title : "Please Wait..");
        progressDialog.setMessage(message != null ? message : "Loading...");
        progressDialog.setCancelable(false);
        progressDialog.show();
    }

    private void hideLoading() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        finish();
    }

    private void showToast(Intent intent) {
        String message = intent.getStringExtra("message");
        Toast.makeText(this, message != null ? message : "", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void showDialog(Intent intent) {
        String title = intent.getStringExtra("title");
        String message = intent.getStringExtra("message");
        String buttons = intent.getStringExtra("buttons");
        String mode = intent.getStringExtra("mode"); // list, radio, checkbox
        String itemsStr = intent.getStringExtra("items");

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);
        if (message != null)
            builder.setMessage(message);

        if (itemsStr != null) {
            String[] items = itemsStr.split(",");
            if ("radio".equals(mode)) {
                builder.setSingleChoiceItems(items, -1, (dialog, which) -> {
                    writeResult(items[which]);
                    dialog.dismiss();
                    finish();
                });
            } else if ("checkbox".equals(mode)) {
                boolean[] checked = new boolean[items.length];
                builder.setMultiChoiceItems(items, checked, (dialog, which, isChecked) -> checked[which] = isChecked);
                builder.setPositiveButton("OK", (dialog, which) -> {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < items.length; i++) {
                        if (checked[i]) {
                            if (sb.length() > 0)
                                sb.append(",");
                            sb.append(items[i]);
                        }
                    }
                    writeResult(sb.toString());
                    finish();
                });
            } else {
                builder.setItems(items, (dialog, which) -> {
                    writeResult(items[which]);
                    finish();
                });
            }
        }

        if (buttons != null && itemsStr == null) {
            String[] btnArr = buttons.split(",");
            if (btnArr.length > 0)
                builder.setPositiveButton(btnArr[0], (dialog, which) -> {
                    writeResult(btnArr[0]);
                    finish();
                });
            if (btnArr.length > 1)
                builder.setNegativeButton(btnArr[1], (dialog, which) -> {
                    writeResult(btnArr[1]);
                    finish();
                });
            if (btnArr.length > 2)
                builder.setNeutralButton(btnArr[2], (dialog, which) -> {
                    writeResult(btnArr[2]);
                    finish();
                });
        } else if (itemsStr == null) {
            builder.setPositiveButton("OK", (dialog, which) -> finish());
        }

        builder.setOnCancelListener(dialog -> finish());
        builder.show();
    }

    private void showInputDialog(Intent intent) {
        String title = intent.getStringExtra("title");
        String message = intent.getStringExtra("message");
        String hint = intent.getStringExtra("hint");

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);
        builder.setMessage(message);

        final EditText input = new EditText(this);
        input.setHint(hint);
        builder.setView(input);

        builder.setPositiveButton("OK", (dialog, which) -> {
            writeResult(input.getText().toString());
            finish();
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> finish());
        builder.setOnCancelListener(dialog -> finish());
        builder.show();
    }

    private void showFormDialog(Intent intent) {
        String title = intent.getStringExtra("title");
        String schema = intent.getStringExtra("schema");

        if (schema == null) {
            finish();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title != null ? title : "Form");

        ScrollView scrollView = new ScrollView(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 40, 40, 40);
        scrollView.addView(layout);

        String[] fields = schema.split(";");
        List<FormField> formFields = new ArrayList<>();

        for (String field : fields) {
            String[] parts = field.split(":");
            if (parts.length < 2)
                continue;
            String label = parts[0];
            String type = parts[1];
            String options = parts.length > 2 ? parts[2] : "";

            TextView tv = new TextView(this);
            tv.setText(label);
            layout.addView(tv);

            if ("text".equals(type)) {
                EditText et = new EditText(this);
                layout.addView(et);
                formFields.add(new FormField(label, et));
            } else if ("radio".equals(type)) {
                RadioGroup rg = new RadioGroup(this);
                String[] opts = options.split(",");
                for (String opt : opts) {
                    RadioButton rb = new RadioButton(this);
                    rb.setText(opt);
                    rg.addView(rb);
                }
                layout.addView(rg);
                formFields.add(new FormField(label, rg));
            } else if ("checkbox".equals(type)) {
                CheckBox cb = new CheckBox(this);
                cb.setText(options.isEmpty() ? label : options);
                layout.addView(cb);
                formFields.add(new FormField(label, cb));
            } else if ("spinner".equals(type)) {
                Spinner spinner = new Spinner(this);
                String[] opts = options.split(",");
                ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, opts);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinner.setAdapter(adapter);
                layout.addView(spinner);
                formFields.add(new FormField(label, spinner));
            }
        }

        builder.setView(scrollView);
        builder.setPositiveButton("Submit", (dialog, which) -> {
            StringBuilder sb = new StringBuilder();
            for (FormField ff : formFields) {
                sb.append(ff.label).append(":").append(ff.getValue()).append("\n");
            }
            writeResult(sb.toString().trim());
            finish();
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> finish());
        builder.setOnCancelListener(dialog -> finish());
        builder.show();
    }

    private void showDatePicker(Intent intent) {
        final Calendar c = Calendar.getInstance();
        int year = intent.getIntExtra("year", c.get(Calendar.YEAR));
        int month = intent.getIntExtra("month", c.get(Calendar.MONTH));
        int day = intent.getIntExtra("day", c.get(Calendar.DAY_OF_MONTH));

        DatePickerDialog dpd = new DatePickerDialog(this, (view, y, m, d) -> {
            writeResult(String.format(Locale.US, "%04d-%02d-%02d", y, m + 1, d));
            finish();
        }, year, month, day);
        dpd.setTitle(intent.getStringExtra("title"));
        dpd.setOnCancelListener(dialog -> finish());
        dpd.show();
    }

    private void showTimePicker(Intent intent) {
        final Calendar c = Calendar.getInstance();
        int hour = intent.getIntExtra("hour", c.get(Calendar.HOUR_OF_DAY));
        int minute = intent.getIntExtra("minute", c.get(Calendar.MINUTE));

        TimePickerDialog tpd = new TimePickerDialog(this, (view, h, m) -> {
            writeResult(String.format(Locale.US, "%02d:%02d", h, m));
            finish();
        }, hour, minute, true);
        tpd.setTitle(intent.getStringExtra("title"));
        tpd.setOnCancelListener(dialog -> finish());
        tpd.show();
    }

    private void showColorPicker(Intent intent) {
        // Color picker logic removed as :aosp-colorpicker module is missing
        Toast.makeText(this, "Color picker not available", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void writeResult(String result) {
        android.util.Log.i("ShellUI_Result", result);
        if (outputFilePath == null)
            return;
        try (FileOutputStream fos = new FileOutputStream(new File(outputFilePath))) {
            fos.write(result.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String join(String delimiter, List<String> list) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            sb.append(list.get(i));
            if (i < list.size() - 1)
                sb.append(delimiter);
        }
        return sb.toString();
    }

    private static class FormField {
        String label;
        Object view;

        FormField(String label, Object view) {
            this.label = label;
            this.view = view;
        }

        String getValue() {
            if (view instanceof EditText)
                return ((EditText) view).getText().toString();
            if (view instanceof RadioGroup) {
                int id = ((RadioGroup) view).getCheckedRadioButtonId();
                if (id == -1)
                    return "";
                RadioButton rb = ((RadioGroup) view).findViewById(id);
                return rb.getText().toString();
            }
            if (view instanceof CheckBox)
                return String.valueOf(((CheckBox) view).isChecked());
            if (view instanceof Spinner)
                return ((Spinner) view).getSelectedItem().toString();
            return "";
        }
    }
}
