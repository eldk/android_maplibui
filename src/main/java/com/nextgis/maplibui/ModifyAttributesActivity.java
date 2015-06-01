/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * *****************************************************************************
 * Copyright (c) 2012-2015. NextGIS, info@nextgis.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.nextgis.maplibui;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import com.nextgis.maplib.api.GpsEventListener;
import com.nextgis.maplib.api.IGISApplication;
import com.nextgis.maplib.datasource.Field;
import com.nextgis.maplib.datasource.GeoGeometry;
import com.nextgis.maplib.datasource.GeoMultiPoint;
import com.nextgis.maplib.datasource.GeoPoint;
import com.nextgis.maplib.location.GpsEventSource;
import com.nextgis.maplib.map.MapBase;
import com.nextgis.maplib.map.VectorLayer;
import com.nextgis.maplib.util.GeoConstants;
import com.nextgis.maplib.util.LocationUtil;
import com.nextgis.maplibui.util.SettingsConstantsUI;

import java.io.IOException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.nextgis.maplib.util.Constants.*;
import static com.nextgis.maplibui.util.ConstantsUI.*;


/**
 * Activity to add or modify vector layer attributes
 */
public class ModifyAttributesActivity
        extends AppCompatActivity
        implements GpsEventListener
{
    protected TextView          mLatView;
    protected TextView          mLongView;
    protected TextView          mAltView;
    protected TextView          mAccView;
    protected Location          mLocation;
    protected VectorLayer       mLayer;
    protected long              mFeatureId;
    protected Map<String, View> mFields;
    protected GeoGeometry       mGeometry;

    protected final static int DATE     = 0;
    protected final static int TIME     = 1;
    protected final static int DATETIME = 2;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_standard_attributes);

        Toolbar toolbar = (Toolbar) findViewById(R.id.main_toolbar);
        toolbar.getBackground().setAlpha(255);
        setSupportActionBar(toolbar);


        LinearLayout layout = (LinearLayout) findViewById(R.id.controls_list);

        ActionBar bar = getSupportActionBar();
        if (null != bar) {
            bar.setHomeButtonEnabled(true);
            bar.setDisplayHomeAsUpEnabled(true);
        }

        final IGISApplication app = (IGISApplication) getApplication();
        //create and fill controls
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            short layerId = extras.getShort(KEY_LAYER_ID);
            mFeatureId = extras.getLong(KEY_FEATURE_ID);
            mGeometry = (GeoGeometry) extras.getSerializable(KEY_GEOMETRY);

            MapBase map = app.getMap();
            mLayer = (VectorLayer) map.getLayerById(layerId);
            if (null != mLayer) {
                createAndFillControls(layout);
            }
        }

        if (null == mGeometry && mFeatureId == NOT_FOUND) {
            mLatView = (TextView) findViewById(R.id.latitude_view);
            mLongView = (TextView) findViewById(R.id.longitude_view);
            mAltView = (TextView) findViewById(R.id.altitude_view);
            mAccView = (TextView) findViewById(R.id.accuracy_view);

            ImageButton refreshLocation = (ImageButton) findViewById(R.id.refresh);
            refreshLocation.setOnClickListener(
                    new View.OnClickListener()
                    {
                        @Override
                        public void onClick(View view)
                        {
                            if (null != app) {
                                GpsEventSource gpsEventSource = app.getGpsEventSource();
                                Location location = gpsEventSource.getLastKnownLocation();
                                setLocationText(location);
                            }
                        }
                    });
        } else {
            //hide location panel
            ViewGroup rootView = (ViewGroup) findViewById(R.id.root_view);
            rootView.removeView(findViewById(R.id.location_panel));
        }
    }


    protected void createAndFillControls(LinearLayout layout)
    {
        Cursor cursor = null;

        if (mFeatureId != NOT_FOUND) {
            cursor = mLayer.query(null, FIELD_ID + " = " + mFeatureId, null, null);
            if (!cursor.moveToFirst()) {
                cursor = null;
            }
        }

        float lineHeight = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 1, getResources().getDisplayMetrics());

        mFields = new HashMap<>();
        List<Field> fields = mLayer.getFields();

        for (Field field : fields) {
            //create static text with alias
            TextView aliasText = getAliasTextView(field);
            layout.addView(aliasText);

            try {

                //create control
                switch (field.getType()) {

                    case GeoConstants.FTString:
                        EditText editStringView = getStringEditView(field, cursor);
                        layout.addView(editStringView);
                        mFields.put(field.getName(), editStringView);
                        break;

                    case GeoConstants.FTInteger:
                        EditText editIntView = getIntEditView(field, cursor);
                        layout.addView(editIntView);
                        mFields.put(field.getName(), editIntView);
                        break;

                    case GeoConstants.FTReal:
                        EditText editRealView = getRealEditView(field, cursor);
                        layout.addView(editRealView);
                        mFields.put(field.getName(), editRealView);
                        break;

                    case GeoConstants.FTDateTime:
                        TextView dateEdit = getDateEditView(field, cursor);
                        layout.addView(dateEdit);
                        mFields.put(field.getName(), dateEdit);

                        // add grey line here
                        View greyLine = new View(this);
                        layout.addView(greyLine);
                        ViewGroup.LayoutParams params = greyLine.getLayoutParams();
                        params.height = (int) lineHeight;
                        greyLine.setLayoutParams(params);
                        greyLine.setBackgroundResource(android.R.color.darker_gray);
                        break;

                    case GeoConstants.FTIntegerList:
                    case GeoConstants.FTBinary:
                    case GeoConstants.FTStringList:
                    case GeoConstants.FTRealList:
                    default:
                        //TODO: add support for this types
                        break;
                }

            } catch (CursorIndexOutOfBoundsException e) {
                e.printStackTrace();
            }
        }

        if (null != cursor) {
            cursor.close();
        }
    }


    protected TextView getAliasTextView(Field field)
    {
        TextView aliasText = new TextView(this);
        aliasText.setText(field.getAlias());
        aliasText.setEllipsize(TextUtils.TruncateAt.END);
        aliasText.setTextAppearance(this, android.R.style.TextAppearance_Medium);
        aliasText.setTextColor(getResources().getColor(android.R.color.darker_gray));

        return aliasText;
    }


    protected EditText getStringEditView(
            Field field,
            Cursor cursor)
    {
        EditText stringEdit = new EditText(this);
        //stringEdit.setSingleLine(true);

        if (null != cursor) {
            int nIndex = cursor.getColumnIndex(field.getName());

            if (nIndex >= 0) {
                String stringVal = cursor.getString(nIndex);
                stringEdit.setText(stringVal);
            }
        }

        return stringEdit;
    }


    protected EditText getIntEditView(
            Field field,
            Cursor cursor)
    {
        EditText intEdit = new EditText(this);
        intEdit.setSingleLine(true);
        intEdit.setInputType(InputType.TYPE_CLASS_NUMBER);

        if (null != cursor) {
            int nIndex = cursor.getColumnIndex(field.getName());

            if (nIndex >= 0) {
                int intVal = cursor.getInt(nIndex);
                intEdit.setText("" + intVal);
            }
        }

        return intEdit;
    }


    protected EditText getRealEditView(
            Field field,
            Cursor cursor)
    {
        EditText realEdit = new EditText(this);
        realEdit.setSingleLine(true);
        realEdit.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);

        if (null != cursor) {
            int nIndex = cursor.getColumnIndex(field.getName());

            if (nIndex >= 0) {
                float realVal = cursor.getFloat(nIndex);
                realEdit.setText("" + realVal);
            }
        }

        return realEdit;
    }


    protected TextView getDateEditView(
            Field field,
            Cursor cursor)
    {
        //TextView dateEdit = new TextView(this);
        TextView dateEdit =
                (TextView) getLayoutInflater().inflate(R.layout.spinner_datepicker, null);

        dateEdit.setSingleLine(true);
        dateEdit.setFocusable(false);
        dateEdit.setOnClickListener(getDateUpdateWatcher(dateEdit, DATETIME));

        SimpleDateFormat dateText = (SimpleDateFormat) DateFormat.getDateTimeInstance();
        String pattern = dateText.toLocalizedPattern();
        dateEdit.setHint(pattern);

        if (null != cursor) {
            int nIndex = cursor.getColumnIndex(field.getName());
            if (nIndex >= 0) {
                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(cursor.getLong(nIndex));
                dateEdit.setText(dateText.format(calendar.getTime()));
            }
        }

        return dateEdit;
    }


    @Override
    protected void onPause()
    {
        if (null != findViewById(R.id.location_panel)) {
            IGISApplication app = (IGISApplication) getApplication();
            if (null != app) {
                GpsEventSource gpsEventSource = app.getGpsEventSource();
                gpsEventSource.removeListener(this);
            }
        }
        super.onPause();
    }


    @Override
    protected void onResume()
    {
        if (null != findViewById(R.id.location_panel)) {
            IGISApplication app = (IGISApplication) getApplication();
            if (null != app) {
                GpsEventSource gpsEventSource = app.getGpsEventSource();
                gpsEventSource.addListener(this);
                setLocationText(gpsEventSource.getLastKnownLocation());
            }
        }
        super.onResume();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.edit_attributes, menu);

        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();

        if (id == R.id.menu_cancel || id == android.R.id.home) {
            finish();
            return true;

        } else if (id == R.id.menu_settings) {
            final IGISApplication app = (IGISApplication) getApplication();
            app.showSettings();
            return true;

        } else if (id == R.id.menu_apply) {
            onMenuApplyClicked();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    protected void onMenuApplyClicked()
    {
        saveFeature();
        finish();
    }


    protected void saveFeature()
    {
        //create new row or modify existing
        List<Field> fields = mLayer.getFields();
        ContentValues values = new ContentValues();

        for (Field field : fields) {
            putFieldValue(values, field);
        }

        putGeometry(values);


        IGISApplication app = (IGISApplication) getApplication();

        if (null == app) {
            throw new IllegalArgumentException("Not a IGISApplication");
        }

        Uri uri = Uri.parse(
                "content://" + app.getAuthority() + "/" + mLayer.getPath().getName());

        if (mFeatureId == NOT_FOUND) {

            int nUniqId = mLayer.getUniqId();
            if (nUniqId < 1000) {
                nUniqId = 1000;// + mLayer.getCount();
            }

            values.put(FIELD_ID, nUniqId);

            if (getContentResolver().insert(uri, values) == null) {
                Toast.makeText(this, getText(R.string.error_db_insert), Toast.LENGTH_SHORT)
                        .show();
            }

        } else {
            Uri updateUri = ContentUris.withAppendedId(uri, mFeatureId);

            if (getContentResolver().update(updateUri, values, null, null) == 0) {
                Toast.makeText(this, getText(R.string.error_db_update), Toast.LENGTH_SHORT)
                        .show();
            }
        }
    }


    protected void putFieldValue(ContentValues values, Field field)
    {
        TextView textView = (TextView) mFields.get(field.getName());
        String stringVal = textView.getText().toString();

        if (field.getType() == GeoConstants.FTDateTime) {
            SimpleDateFormat dateFormat =
                    (SimpleDateFormat) DateFormat.getDateTimeInstance();

            try {
                Date date = dateFormat.parse(stringVal);
                values.put(field.getName(), date.getTime());
            } catch (ParseException e) {
                Log.d(TAG, "Date parse error, " + e.getLocalizedMessage());
            }

        } else {
            values.put(field.getName(), stringVal);
        }
    }


    protected boolean putGeometry(ContentValues values)
    {
        GeoGeometry geometry = null;

        if (null != mGeometry) {
            geometry = mGeometry;

        } else if (NOT_FOUND == mFeatureId) {

            if (null == mLocation) {
                Toast.makeText(
                        this, getText(R.string.error_no_location), Toast.LENGTH_SHORT).show();
                return false;
            }

            //create point geometry
            GeoPoint pt;

            switch (mLayer.getGeometryType()) {

                case GeoConstants.GTPoint:
                    pt = new GeoPoint(mLocation.getLongitude(), mLocation.getLatitude());
                    pt.setCRS(GeoConstants.CRS_WGS84);
                    pt.project(GeoConstants.CRS_WEB_MERCATOR);

                    geometry = pt;
                    break;

                case GeoConstants.GTMultiPoint:
                    pt = new GeoPoint(mLocation.getLongitude(), mLocation.getLatitude());
                    pt.setCRS(GeoConstants.CRS_WGS84);
                    pt.project(GeoConstants.CRS_WEB_MERCATOR);

                    GeoMultiPoint mpt = new GeoMultiPoint();
                    mpt.add(pt);
                    geometry = mpt;
                    break;
            }
        }

        if (null != geometry) {
            try {
                values.put(FIELD_GEOM, geometry.toBlob());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return true;
    }


    protected void setLocationText(Location location)
    {
        if (null == location || null == mLatView || null == mLongView || null == mAccView ||
            null == mAltView) {

            mLatView.setText(
                    getString(R.string.latitude_caption_short) + ": " + getString(R.string.n_a));
            mLongView.setText(
                    getString(R.string.longitude_caption_short) + ": " + getString(R.string.n_a));
            mAltView.setText(
                    getString(R.string.altitude_caption_short) + ": " + getString(R.string.n_a));
            mAccView.setText(
                    getString(R.string.accuracy_caption_short) + ": " + getString(R.string.n_a));

            return;
        }

        mLocation = location;

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        int nFormat = prefs.getInt(
                SettingsConstantsUI.KEY_PREF_COORD_FORMAT + "_int", Location.FORMAT_SECONDS);
        DecimalFormat df = new DecimalFormat("0.0");

        mLatView.setText(
                getString(R.string.latitude_caption_short) + ": " +
                LocationUtil.formatLatitude(location.getLatitude(), nFormat, getResources()));

        mLongView.setText(
                getString(R.string.longitude_caption_short) + ": " +
                LocationUtil.formatLongitude(location.getLongitude(), nFormat, getResources()));

        double altitude = location.getAltitude();
        mAltView.setText(
                getString(R.string.altitude_caption_short) + ": " + df.format(altitude) + " " +
                getString(R.string.unit_meter));

        float accuracy = location.getAccuracy();
        mAccView.setText(
                getString(R.string.accuracy_caption_short) + ": " + df.format(accuracy) + " " +
                getString(R.string.unit_meter));
    }


    @Override
    public void onLocationChanged(Location location)
    {

    }


    @Override
    public void onGpsStatusChanged(int event)
    {

    }


    protected View.OnClickListener getDateUpdateWatcher(
            final TextView dateEdit,
            final int pickerType)
    {
        View.OnClickListener out = new View.OnClickListener()
        {
            protected Calendar calendar = Calendar.getInstance();

            DatePickerDialog.OnDateSetListener date = new DatePickerDialog.OnDateSetListener()
            {

                @Override
                public void onDateSet(
                        DatePicker view,
                        int year,
                        int monthOfYear,
                        int dayOfMonth)
                {
                    calendar.set(Calendar.YEAR, year);
                    calendar.set(Calendar.MONTH, monthOfYear);
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                    updateLabel();
                }

            };

            TimePickerDialog.OnTimeSetListener time = new TimePickerDialog.OnTimeSetListener()
            {
                @Override
                public void onTimeSet(
                        TimePicker view,
                        int hourOfDay,
                        int minute)
                {
                    calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    calendar.set(Calendar.MINUTE, minute);

                    updateLabel();
                }
            };


            protected void updateLabel()
            {
                SimpleDateFormat sdf;
                if (pickerType == DATE) {
                    sdf = (SimpleDateFormat) DateFormat.getDateInstance();
                } else if (pickerType == TIME) {
                    sdf = (SimpleDateFormat) DateFormat.getTimeInstance();
                } else if (pickerType == DATETIME) {
                    sdf = (SimpleDateFormat) DateFormat.getDateTimeInstance();
                } else {
                    sdf = (SimpleDateFormat) DateFormat.getDateTimeInstance();
                }
                dateEdit.setText(sdf.format(calendar.getTime()));
            }


            @Override
            public void onClick(View view)
            {
                if (pickerType == DATE) {
                    calendar.setTime(new Date());
                    new DatePickerDialog(
                            ModifyAttributesActivity.this, date, calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)).show();
                } else if (pickerType == TIME) {
                    calendar.setTime(new Date());
                    new TimePickerDialog(
                            ModifyAttributesActivity.this, time, calendar.get(Calendar.HOUR_OF_DAY),
                            calendar.get(Calendar.MINUTE),
                            android.text.format.DateFormat.is24HourFormat(
                                    ModifyAttributesActivity.this)).show();
                } else if (pickerType == DATETIME) {
                    calendar.setTime(new Date());
                    final SimpleDateFormat sdf =
                            (SimpleDateFormat) DateFormat.getDateTimeInstance();

                    AlertDialog.Builder builder =
                            new AlertDialog.Builder(ModifyAttributesActivity.this);

                    builder.setTitle(sdf.format(calendar.getTime()));
                    builder.setNegativeButton(
                            R.string.ok, new DialogInterface.OnClickListener()
                            {
                                @Override
                                public void onClick(
                                        DialogInterface dialog,
                                        int which)
                                {
                                    updateLabel();
                                    dialog.cancel();
                                }
                            });

                    final AlertDialog alert = builder.create();

                    View datetimePickerLayout =
                            getLayoutInflater().inflate(R.layout.layout_datetimepicker, null);
                    alert.setView(datetimePickerLayout);

                    DatePicker dt = (DatePicker) datetimePickerLayout.findViewById(R.id.datePicker);
                    dt.init(
                            calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH),
                            new DatePicker.OnDateChangedListener()
                            {

                                @Override
                                public void onDateChanged(
                                        DatePicker view,
                                        int year,
                                        int monthOfYear,
                                        int dayOfMonth)
                                {
                                    calendar.set(Calendar.YEAR, year);
                                    calendar.set(Calendar.MONTH, monthOfYear);
                                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                                    alert.setTitle(sdf.format(calendar.getTime()));

                                    updateLabel();
                                }
                            });

                    TimePicker tp = (TimePicker) datetimePickerLayout.findViewById(R.id.timePicker);
                    tp.setIs24HourView(
                            android.text.format.DateFormat.is24HourFormat(
                                    ModifyAttributesActivity.this));
                    tp.setOnTimeChangedListener(
                            new TimePicker.OnTimeChangedListener()
                            {
                                @Override
                                public void onTimeChanged(
                                        TimePicker view,
                                        int hourOfDay,
                                        int minute)
                                {
                                    calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                                    calendar.set(Calendar.MINUTE, minute);

                                    alert.setTitle(sdf.format(calendar.getTime()));

                                    updateLabel();
                                }
                            });

                    alert.show();
                }
            }
        };

        return out;
    }
}
