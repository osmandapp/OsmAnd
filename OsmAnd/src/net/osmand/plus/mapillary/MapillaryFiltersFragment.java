package net.osmand.plus.mapillary;


import android.app.DatePickerDialog;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ContextThemeWrapper;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.content.ContextCompat;

import net.osmand.map.TileSourceManager;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.resources.ResourceManager;
import net.osmand.plus.views.controls.DelayAutoCompleteTextView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class MapillaryFiltersFragment extends BaseOsmAndFragment {

    public static final String TAG = "MAPILLARY_FILTERS_FRAGMENT";

    public MapillaryFiltersFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final MapActivity mapActivity = (MapActivity) getActivity();
        final OsmandSettings settings = getSettings();
        final MapillaryPlugin plugin = OsmandPlugin.getPlugin(MapillaryPlugin.class);

        final boolean nightMode = getMyApplication().getDaynightHelper().isNightModeForMapControls();
        final int themeRes = nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;
        final int backgroundColor = ContextCompat.getColor(getActivity(),
                nightMode ? R.color.activity_background_color_dark : R.color.activity_background_color_light);
        final DateFormat dateFormat = SimpleDateFormat.getDateInstance(DateFormat.MEDIUM);
        final int currentModeColor = getMyApplication().getSettings().getApplicationMode().getProfileColor(nightMode);

        final View view = View.inflate(new ContextThemeWrapper(getContext(), themeRes), R.layout.fragment_mapillary_filters, null);
        view.findViewById(R.id.mapillary_filters_linear_layout).setBackgroundColor(backgroundColor);


        final View toggleRow = view.findViewById(R.id.toggle_row);
        final boolean selected = settings.SHOW_MAPILLARY.get();
        final int toggleActionStringId = selected ? R.string.shared_string_on : R.string.shared_string_off;
        int toggleIconColor;
        int toggleIconId;
        if (selected) {
            toggleIconId = R.drawable.ic_action_view;
            toggleIconColor = currentModeColor;
        } else {
            toggleIconId = R.drawable.ic_action_hide;
            toggleIconColor = ContextCompat.getColor(getContext(), nightMode ? R.color.icon_color_default_dark : R.color.icon_color_default_light);
        }
        ((AppCompatTextView) toggleRow.findViewById(R.id.toggle_row_title)).setText(toggleActionStringId);
        final Drawable drawable = getPaintedContentIcon(toggleIconId, toggleIconColor);
        ((AppCompatImageView) toggleRow.findViewById(R.id.toggle_row_icon)).setImageDrawable(drawable);
        final CompoundButton toggle = (CompoundButton) toggleRow.findViewById(R.id.toggle_row_toggle);
		toggle.setOnCheckedChangeListener(null);
        toggle.setChecked(selected);
        toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                settings.SHOW_MAPILLARY.set(!settings.SHOW_MAPILLARY.get());
                plugin.updateLayers(mapActivity.getMapView(), mapActivity);
                mapActivity.getDashboard().refreshContent(true);
            }
        });
        toggleRow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggle.setChecked(!toggle.isChecked());
            }
        });
        UiUtilities.setupCompoundButton(nightMode, currentModeColor, toggle);


        final Button reloadTile = (Button) view.findViewById(R.id.button_reload_tile);
        reloadTile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ResourceManager manager = getMyApplication().getResourceManager();
                manager.clearCacheAndTiles(TileSourceManager.getMapillaryVectorSource());
                manager.clearCacheAndTiles(TileSourceManager.getMapillaryRasterSource());
                mapActivity.refreshMap();
            }
        });


        final int colorRes = nightMode ? R.color.icon_color_default_dark : R.color.icon_color_default_light;
        ((AppCompatImageView) view.findViewById(R.id.mapillary_filters_user_icon)).setImageDrawable(getIcon(R.drawable.ic_action_user, colorRes));
        ((AppCompatImageView) view.findViewById(R.id.mapillary_filters_date_icon)).setImageDrawable(getIcon(R.drawable.ic_action_data, colorRes));
        ((AppCompatImageView) view.findViewById(R.id.mapillary_filters_tile_cache_icon)).setImageDrawable(getIcon(R.drawable.ic_layer_top, colorRes));

        final DelayAutoCompleteTextView textView = (DelayAutoCompleteTextView) view.findViewById(R.id.auto_complete_text_view);
        textView.setAdapter(new MapillaryAutoCompleteAdapter(getContext(), R.layout.auto_complete_suggestion, getMyApplication()));
        String selectedUsername = settings.MAPILLARY_FILTER_USERNAME.get();
        if (!selectedUsername.isEmpty() && settings.USE_MAPILLARY_FILTER.get()) {
            textView.setText(selectedUsername);
            textView.setSelection(selectedUsername.length());
        }
        textView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                hideKeyboard();
                mapActivity.getDashboard().refreshContent(true);
            }
        });
        textView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == EditorInfo.IME_ACTION_DONE) {
                    hideKeyboard();
                    mapActivity.getDashboard().refreshContent(true);
                    return true;
                }
                return false;
            }
        });
        textView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                view.findViewById(R.id.warning_linear_layout).setVisibility(View.GONE);
                enableButtonApply(view);
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
        ImageView imageView = (ImageView) view.findViewById(R.id.warning_image_view);
        imageView.setImageDrawable(getPaintedContentIcon(R.drawable.ic_small_warning,
                getResources().getColor(R.color.color_warning)));


        final EditText dateFromEt = (EditText) view.findViewById(R.id.date_from_edit_text);
        final DatePickerDialog.OnDateSetListener dateFromDialog = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker v, int year, int monthOfYear, int dayOfMonth) {
                Calendar from = Calendar.getInstance();
                from.set(Calendar.YEAR, year);
                from.set(Calendar.MONTH, monthOfYear);
                from.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                dateFromEt.setText(dateFormat.format(from.getTime()));
                settings.MAPILLARY_FILTER_FROM_DATE.set(from.getTimeInMillis());
                enableButtonApply(view);
                mapActivity.getDashboard().refreshContent(true);
            }
        };
        dateFromEt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Calendar now = Calendar.getInstance();
                new DatePickerDialog(mapActivity, dateFromDialog,
                        now.get(Calendar.YEAR),
                        now.get(Calendar.MONTH),
                        now.get(Calendar.DAY_OF_MONTH)).show();
            }
        });
        dateFromEt.setCompoundDrawablesWithIntrinsicBounds(null, null, getContentIcon(R.drawable.ic_action_arrow_drop_down), null);


        final EditText dateToEt = (EditText) view.findViewById(R.id.date_to_edit_text);
        final DatePickerDialog.OnDateSetListener dateToDialog = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker v, int year, int monthOfYear, int dayOfMonth) {
                Calendar to = Calendar.getInstance();
                to.set(Calendar.YEAR, year);
                to.set(Calendar.MONTH, monthOfYear);
                to.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                dateToEt.setText(dateFormat.format(to.getTime()));
                settings.MAPILLARY_FILTER_TO_DATE.set(to.getTimeInMillis());
                enableButtonApply(view);
                mapActivity.getDashboard().refreshContent(true);
            }
        };
        dateToEt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Calendar now = Calendar.getInstance();
                new DatePickerDialog(mapActivity, dateToDialog,
                        now.get(Calendar.YEAR),
                        now.get(Calendar.MONTH),
                        now.get(Calendar.DAY_OF_MONTH)).show();
            }
        });
        dateToEt.setCompoundDrawablesWithIntrinsicBounds(null, null, getContentIcon(R.drawable.ic_action_arrow_drop_down), null);

        if (settings.USE_MAPILLARY_FILTER.get()) {
            long to = settings.MAPILLARY_FILTER_TO_DATE.get();
            if (to != 0) {
                dateToEt.setText(dateFormat.format(new Date(to)));
            }
            long from = settings.MAPILLARY_FILTER_FROM_DATE.get();
            if (from != 0) {
                dateFromEt.setText(dateFormat.format(new Date(from)));
            }
        }

        final View rowPano = view.findViewById(R.id.pano_row);
        final CompoundButton pano = (CompoundButton) rowPano.findViewById(R.id.pano_row_toggle);
        pano.setOnCheckedChangeListener(null);
        pano.setChecked(settings.MAPILLARY_FILTER_PANO.get());
        pano.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                settings.MAPILLARY_FILTER_PANO.set(!settings.MAPILLARY_FILTER_PANO.get());
                enableButtonApply(view);
                mapActivity.getDashboard().refreshContent(true);
            }
        });
        rowPano.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pano.setChecked(!pano.isChecked());
            }
        });
        UiUtilities.setupCompoundButton(nightMode, currentModeColor, pano);


        final Button apply = (Button) view.findViewById(R.id.button_apply);
        disableButtonApply(view);
        apply.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = textView.getText().toString();
                String dateFrom = dateFromEt.getText().toString();
                String dateTo = dateToEt.getText().toString();

                if (!settings.MAPILLARY_FILTER_USERNAME.get().isEmpty() || !dateFrom.isEmpty() || !dateTo.isEmpty() || settings.MAPILLARY_FILTER_PANO.get()) {
                    settings.USE_MAPILLARY_FILTER.set(true);
                }
                if (dateFrom.isEmpty()) {
                    settings.MAPILLARY_FILTER_FROM_DATE.set(0L);
                }
                if (dateTo.isEmpty()) {
                    settings.MAPILLARY_FILTER_TO_DATE.set(0L);
                }
                if (!username.isEmpty() && settings.MAPILLARY_FILTER_USERNAME.get().isEmpty()) {
                    view.findViewById(R.id.warning_linear_layout).setVisibility(View.VISIBLE);
                } else {
                    mapActivity.getDashboard().hideDashboard();
                }

                changeButtonState(apply, .5f, false);
                plugin.updateLayers(mapActivity.getMapView(), mapActivity);
                hideKeyboard();
            }
        });


        final Button clear = (Button) view.findViewById(R.id.button_clear);
        clear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                textView.setText("");
                dateFromEt.setText("");
                dateToEt.setText("");
                pano.setChecked(false);

                settings.USE_MAPILLARY_FILTER.set(false);
                settings.MAPILLARY_FILTER_USER_KEY.set("");
                settings.MAPILLARY_FILTER_USERNAME.set("");
                settings.MAPILLARY_FILTER_FROM_DATE.set(0L);
                settings.MAPILLARY_FILTER_TO_DATE.set(0L);
                settings.MAPILLARY_FILTER_PANO.set(false);

                plugin.updateLayers(mapActivity.getMapView(), mapActivity);
                hideKeyboard();
            }
        });

        return view;
    }

    private void hideKeyboard() {
        View currentFocus = getActivity().getCurrentFocus();
        if (currentFocus != null) {
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
        }
    }

    private void enableButtonApply(View view) {
        changeButtonState((Button) view.findViewById(R.id.button_apply), 1, true);
    }

    private void disableButtonApply(View view) {
        changeButtonState((Button) view.findViewById(R.id.button_apply), .5f, false);
    }

    private void changeButtonState(Button button, float alpha, boolean enabled) {
        button.setAlpha(alpha);
        button.setEnabled(enabled);
    }
}
