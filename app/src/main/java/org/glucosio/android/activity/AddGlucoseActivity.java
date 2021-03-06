/*
 * Copyright (C) 2016 Glucosio Foundation
 *
 * This file is part of Glucosio.
 *
 * Glucosio is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Glucosio is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Glucosio.  If not, see <http://www.gnu.org/licenses/>.
 *
 *
 */

package org.glucosio.android.activity;

import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.TextView;

import com.wdullaer.materialdatetimepicker.time.RadialPickerLayout;

import org.glucosio.android.GlucosioApplication;
import org.glucosio.android.R;
import org.glucosio.android.analytics.Analytics;
import org.glucosio.android.db.GlucoseReading;
import org.glucosio.android.presenter.AddGlucosePresenter;
import org.glucosio.android.tools.FormatDateTime;
import org.glucosio.android.tools.LabelledSpinner;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Calendar;


public class AddGlucoseActivity extends AddReadingActivity {

    private TextView readingTextView;
    private EditText typeCustomEditText;
    private EditText notesEditText;
    private LabelledSpinner readingTypeSpinner;
    private boolean isCustomType = false;

    static final int CUSTOM_TYPE_SPINNER_VALUE = 11;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_glucose);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);

        if (toolbar != null) {
            setSupportActionBar(toolbar);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setElevation(2);
        }

        this.retrieveExtra();

        AddGlucosePresenter presenter = new AddGlucosePresenter(this);
        setPresenter(presenter);
        presenter.setReadingTimeNow();

        readingTypeSpinner = (LabelledSpinner) findViewById(R.id.glucose_add_reading_type);
        readingTypeSpinner.setItemsArray(R.array.dialog_add_measured_list);
        readingTextView = (TextView) findViewById(R.id.glucose_add_concentration);
        typeCustomEditText = (EditText) findViewById(R.id.glucose_type_custom);
        notesEditText = (EditText) findViewById(R.id.glucose_add_notes);

        this.createDateTimeViewAndListener();
        this.createFANViewAndListener();

        readingTypeSpinner.setOnItemChosenListener(new LabelledSpinner.OnItemChosenListener() {
            @Override
            public void onItemChosen(View labelledSpinner, AdapterView<?> adapterView, View itemView, int position, long id) {
                // If other is selected
                if (position == CUSTOM_TYPE_SPINNER_VALUE) {
                    typeCustomEditText.setVisibility(View.VISIBLE);
                    isCustomType = true;
                } else {
                    if (typeCustomEditText.getVisibility() == View.VISIBLE) {
                        typeCustomEditText.setVisibility(View.GONE);
                        isCustomType = false;
                    }
                }
            }

            @Override
            public void onNothingChosen(View labelledSpinner, AdapterView<?> adapterView) {

            }
        });

        TextView unitM = (TextView) findViewById(R.id.glucose_add_unit_measurement);

        if (presenter.getUnitMeasuerement().equals("mg/dL")) {
            unitM.setText("mg/dL");
        } else {
            unitM.setText("mmol/L");
        }

        // If an id is passed, open the activity in edit mode
        Calendar cal = Calendar.getInstance();
        FormatDateTime dateTime = new FormatDateTime(getApplicationContext());
        if (this.isEditing()){
            setTitle(R.string.title_activity_add_glucose_edit);
            GlucoseReading readingToEdit = presenter.getGlucoseReadingById(this.getEditId());
            readingTextView.setText(readingToEdit.getReading()+"");
            notesEditText.setText(readingToEdit.getNotes());
            cal.setTime(readingToEdit.getCreated());
            this.getAddDateTextView().setText(dateTime.getDate(cal));
            this.getAddTimeTextView().setText(dateTime.getTime(cal));
            presenter.updateReadingSplitDateTime(readingToEdit.getCreated());
            // retrieve spinner reading to set the registered one
            String measuredTypeText = readingToEdit.getReading_type();
            Integer measuredId = presenter.retrieveSpinnerID(measuredTypeText, Arrays.asList(getResources().getStringArray(R.array.dialog_add_measured_list)));
            if (measuredId == null) { // if nothing, it a custom type
                this.isCustomType = true;
                readingTypeSpinner.setSelection(CUSTOM_TYPE_SPINNER_VALUE);
            } else {
                readingTypeSpinner.setSelection(measuredId);
            }
            if(this.isCustomType) {
                typeCustomEditText.setText(measuredTypeText);
            }
        } else {
            this.getAddDateTextView().setText(dateTime.getDate(cal));
            this.getAddTimeTextView().setText(dateTime.getTime(cal));
            presenter.updateSpinnerTypeTime();
        }

        this.getDoneFAB().postDelayed(this.getFabAnimationRunnable(), 600);
    }

    private void addAnalyticsEvent() {
        Analytics analytics = ((GlucosioApplication) getApplication()).getAnalytics();
        analytics.reportAction("FreeStyle Libre", "New reading added");
    }

    @Override
    protected void dialogOnAddButtonPressed() {
        AddGlucosePresenter presenter = (AddGlucosePresenter) getPresenter();
        String readingType;
        if (isCustomType) {
            readingType = typeCustomEditText.getText().toString();
        } else {
            readingType = readingTypeSpinner.getSpinner().getSelectedItem().toString();
        }

        if (this.isEditing()) {
            presenter.dialogOnAddButtonPressed(this.getAddTimeTextView().getText().toString(),
                    this.getAddDateTextView().getText().toString(), readingTextView.getText().toString(),
                    readingType, notesEditText.getText().toString(), this.getEditId());
        } else {
            presenter.dialogOnAddButtonPressed(this.getAddTimeTextView().getText().toString(),
                    this.getAddDateTextView().getText().toString(), readingTextView.getText().toString(),
                    readingType, notesEditText.getText().toString());
        }
    }

    public void showErrorMessage() {
        View rootLayout = findViewById(android.R.id.content);
        Snackbar.make(rootLayout, getString(R.string.dialog_error2), Snackbar.LENGTH_SHORT).show();
    }


    public void showDuplicateErrorMessage() {
        View rootLayout = findViewById(android.R.id.content);
        Snackbar.make(rootLayout, getString(R.string.dialog_error_duplicate), Snackbar.LENGTH_SHORT).show();
    }

    public void updateSpinnerTypeTime(int selection) {
        readingTypeSpinner.setSelection(selection);
    }

    private void updateSpinnerTypeHour(int hour) {
        AddGlucosePresenter presenter = (AddGlucosePresenter) getPresenter();
        readingTypeSpinner.setSelection(presenter.hourToSpinnerType(hour));
    }

    @Override
    public void onTimeSet(RadialPickerLayout view, int hourOfDay, int minute, int seconds) {
        super.onTimeSet(view, hourOfDay, minute, seconds);
        DecimalFormat df = new DecimalFormat("00");
        updateSpinnerTypeHour(Integer.parseInt(df.format(hourOfDay)));
    }
}
