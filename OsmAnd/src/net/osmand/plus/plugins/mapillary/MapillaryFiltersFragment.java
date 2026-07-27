package net.osmand.plus.plugins.mapillary;


import android.app.DatePickerDialog;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.fragment.app.FragmentManager;

import net.osmand.map.TileSourceManager;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseFullScreenFragment;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.utils.InsetTarget;
import net.osmand.plus.utils.InsetTarget.Type;
import net.osmand.plus.utils.InsetTargetsCollection;
import net.osmand.plus.widgets.tools.SimpleTextWatcher;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.resources.ResourceManager;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.controls.DelayAutoCompleteTextView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class MapillaryFiltersFragment extends BaseFullScreenFragment {

    public static final String TAG = MapillaryFiltersFragment.class.getSimpleName();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        updateNightMode();
        MapActivity mapActivity = (MapActivity) requireActivity();
        ApplicationMode appMode = app.getSettings().getApplicationMode();
        MapillaryPlugin plugin = PluginsHelper.getPlugin(MapillaryPlugin.class);

        int backgroundColor = ColorUtilities.getActivityBgColor(mapActivity, nightMode);
        DateFormat dateFormat = SimpleDateFormat.getDateInstance(DateFormat.MEDIUM);
        int currentModeColor = appMode.getProfileColor(nightMode);

        View view = UiUtilities.getInflater(mapActivity, nightMode)
                .inflate(R.layout.fragment_mapillary_filters, container, false);

        boolean portrait = AndroidUiHelper.isOrientationPortrait(mapActivity);
        AndroidUiHelper.updateVisibility(view.findViewById(R.id.shadow_on_map), portrait);

        view.findViewById(R.id.mapillary_filters_linear_layout).setBackgroundColor(backgroundColor);

        View toggleRow = view.findViewById(R.id.toggle_row);
        boolean selected = plugin.SHOW_MAPILLARY.get();
        int toggleActionStringId = R.string.street_level_imagery;
        int toggleIconColor;
        int toggleIconId;
        if (selected) {
            toggleIconId = R.drawable.ic_action_view;
            toggleIconColor = currentModeColor;
        } else {
            toggleIconId = R.drawable.ic_action_hide;
            toggleIconColor = ColorUtilities.getDefaultIconColor(mapActivity, nightMode);
        }
        ((AppCompatTextView) toggleRow.findViewById(R.id.toggle_row_title)).setText(toggleActionStringId);
        Drawable drawable = getPaintedIcon(toggleIconId, toggleIconColor);
        ((AppCompatImageView) toggleRow.findViewById(R.id.toggle_row_icon)).setImageDrawable(drawable);
        CompoundButton toggle = toggleRow.findViewById(R.id.toggle_row_toggle);
        toggle.setOnCheckedChangeListener(null);
        toggle.setChecked(selected);
        toggle.setOnCheckedChangeListener((compoundButton, b) -> {
            plugin.SHOW_MAPILLARY.set(!plugin.SHOW_MAPILLARY.get());
            plugin.updateLayers(mapActivity, mapActivity);
            mapActivity.getDashboard().refreshContent(true);
        });
        toggleRow.setOnClickListener(v -> toggle.setChecked(!toggle.isChecked()));
        UiUtilities.setupCompoundButton(nightMode, currentModeColor, toggle);

        Button reloadTile = view.findViewById(R.id.button_reload_tile);
        reloadTile.setOnClickListener(v -> {
            ResourceManager manager = app.getResourceManager();
            manager.clearCacheAndTiles(TileSourceManager.getMapillaryVectorSource());
            mapActivity.refreshMap();
        });


        int colorRes = ColorUtilities.getDefaultIconColorId(nightMode);
        ((AppCompatImageView) view.findViewById(R.id.mapillary_filters_user_icon))
                .setImageDrawable(getIcon(R.drawable.ic_action_user, colorRes));
        ((AppCompatImageView) view.findViewById(R.id.mapillary_filters_date_icon))
                .setImageDrawable(getIcon(R.drawable.ic_action_data, colorRes));
        ((AppCompatImageView) view.findViewById(R.id.mapillary_filters_tile_cache_icon))
                .setImageDrawable(getIcon(R.drawable.ic_layer_top, colorRes));

        DelayAutoCompleteTextView textView =
                view.findViewById(R.id.auto_complete_text_view);
        textView.setAdapter(new MapillaryAutoCompleteAdapter(mapActivity, R.layout.auto_complete_suggestion));
        String selectedUsername = plugin.MAPILLARY_FILTER_USERNAME.get();
        if (!selectedUsername.isEmpty() && plugin.USE_MAPILLARY_FILTER.get()) {
            textView.setText(selectedUsername);
            textView.setSelection(selectedUsername.length());
        }
        textView.setOnItemClickListener((adapterView, v, i, l) -> {
            hideKeyboard();
            mapActivity.getDashboard().refreshContent(true);
        });
        textView.setOnEditorActionListener((tv, id, keyEvent) -> {
            if (id == EditorInfo.IME_ACTION_DONE) {
                hideKeyboard();
                mapActivity.getDashboard().refreshContent(true);
                return true;
            }
            return false;
        });
        textView.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                view.findViewById(R.id.warning_linear_layout).setVisibility(View.GONE);
                enableButtonApply(view);
            }
        });
        ImageView imageView = view.findViewById(R.id.warning_image_view);
        imageView.setImageDrawable(getPaintedIcon(R.drawable.ic_small_warning,
                getColor(R.color.color_warning)));


        EditText dateFromEt = view.findViewById(R.id.date_from_edit_text);
        EditText dateToEt = view.findViewById(R.id.date_to_edit_text);

        EditText dateFirstSplitEt = view.findViewById(R.id.date_first_split_edit_text);
        EditText dateSecondSplitEt = view.findViewById(R.id.date_second_split_edit_text);
        CompoundButton firstSplitToggle = view.findViewById(R.id.date_first_split_toggle);
        CompoundButton secondSplitToggle = view.findViewById(R.id.date_second_split_toggle);

        setupDateField(mapActivity, view, dateFormat, dateFromEt, plugin.MAPILLARY_FILTER_FROM_DATE, plugin);
        setupDateField(mapActivity, view, dateFormat, dateToEt, plugin.MAPILLARY_FILTER_TO_DATE, plugin);

        setupDateField(mapActivity, view, dateFormat, dateFirstSplitEt, plugin.MAPILLARY_FILTER_FIRST_SPLIT_TO_DATE, plugin);
        setupDateField(mapActivity, view, dateFormat, dateSecondSplitEt, plugin.MAPILLARY_FILTER_SECOND_SPLIT_TO_DATE, plugin);

        setupSplitToggle(mapActivity, view, firstSplitToggle, dateFirstSplitEt, plugin, dateFormat);
        setupSplitToggle(mapActivity, view, secondSplitToggle, dateSecondSplitEt, plugin, dateFormat);
        UiUtilities.setupCompoundButton(nightMode, currentModeColor, firstSplitToggle);
        UiUtilities.setupCompoundButton(nightMode, currentModeColor, secondSplitToggle);

        firstSplitToggle.setChecked(plugin.MAPILLARY_FILTER_SPLIT_FIRST.get());
        secondSplitToggle.setChecked(plugin.MAPILLARY_FILTER_SPLIT_SECOND.get());
        setEditTextEnabled(dateFirstSplitEt, firstSplitToggle.isChecked());
        setEditTextEnabled(dateSecondSplitEt, secondSplitToggle.isChecked());
        updateTimelineUi(view, plugin, dateFormat);

        View rowPano = view.findViewById(R.id.pano_row);
        CompoundButton pano = rowPano.findViewById(R.id.pano_row_toggle);
        pano.setOnCheckedChangeListener(null);
        pano.setChecked(plugin.MAPILLARY_FILTER_PANO.get());
        pano.setOnCheckedChangeListener((compoundButton, b) -> {
            plugin.MAPILLARY_FILTER_PANO.set(!plugin.MAPILLARY_FILTER_PANO.get());
            enableButtonApply(view);
            mapActivity.getDashboard().refreshContent(true);
        });
        rowPano.setOnClickListener(v -> pano.setChecked(!pano.isChecked()));
        UiUtilities.setupCompoundButton(nightMode, currentModeColor, pano);

        View rowAboveOverlay = view.findViewById(R.id.above_overlay_row);
        CompoundButton aboveOverlay = rowAboveOverlay.findViewById(R.id.above_overlay_row_toggle);
        aboveOverlay.setOnCheckedChangeListener(null);
        aboveOverlay.setChecked(plugin.MAPILLARY_SHOW_ABOVE_OVERLAY.get());
        aboveOverlay.setOnCheckedChangeListener((compoundButton, b) -> {
            plugin.MAPILLARY_SHOW_ABOVE_OVERLAY.set(!plugin.MAPILLARY_SHOW_ABOVE_OVERLAY.get());
            plugin.updateLayers(mapActivity, mapActivity);
            enableButtonApply(view);
            mapActivity.getDashboard().refreshContent(true);
        });
        rowAboveOverlay.setOnClickListener(v -> aboveOverlay.setChecked(!aboveOverlay.isChecked()));
        UiUtilities.setupCompoundButton(nightMode, currentModeColor, aboveOverlay);

        Button apply = view.findViewById(R.id.button_apply);
        disableButtonApply(view);
        apply.setOnClickListener(v -> {
            String username = textView.getText().toString();
            String dateFrom = dateFromEt.getText().toString();
            String dateTo = dateToEt.getText().toString();

            if (!plugin.MAPILLARY_FILTER_USERNAME.get().isEmpty() || !dateFrom.isEmpty() || !dateTo.isEmpty() || plugin.MAPILLARY_FILTER_PANO.get()) {
                plugin.USE_MAPILLARY_FILTER.set(true);
            }
            if (dateFrom.isEmpty()) {
                plugin.MAPILLARY_FILTER_FROM_DATE.set(0L);
            }
            if (dateTo.isEmpty()) {
                plugin.MAPILLARY_FILTER_TO_DATE.set(0L);
            }
            if (!username.isEmpty() && plugin.MAPILLARY_FILTER_USERNAME.get().isEmpty()) {
                view.findViewById(R.id.warning_linear_layout).setVisibility(View.VISIBLE);
            } else {
                mapActivity.getDashboard().hideDashboard();
            }

            changeButtonState(apply, .5f, false);
            plugin.updateLayers(mapActivity, mapActivity);
            hideKeyboard();
        });


        Button clear = view.findViewById(R.id.button_clear);
        clear.setOnClickListener(v -> {
            textView.setText("");
            dateFromEt.setText("");
            dateToEt.setText("");

            dateFirstSplitEt.setText("");
            dateSecondSplitEt.setText("");
            firstSplitToggle.setChecked(false);
            secondSplitToggle.setChecked(false);

            pano.setChecked(false);

            plugin.USE_MAPILLARY_FILTER.set(false);
            plugin.MAPILLARY_FILTER_USER_KEY.set("");
            plugin.MAPILLARY_FILTER_USERNAME.set("");
            plugin.MAPILLARY_FILTER_FROM_DATE.set(0L);
            plugin.MAPILLARY_FILTER_TO_DATE.set(0L);

            plugin.MAPILLARY_FILTER_MAIN_SPLIT_FROM_DATE.set(0L);
            plugin.MAPILLARY_FILTER_MAIN_SPLIT_TO_DATE.set(0L);
            plugin.MAPILLARY_FILTER_SPLIT_FIRST.set(false);
            plugin.MAPILLARY_FILTER_FIRST_SPLIT_FROM_DATE.set(0L);
            plugin.MAPILLARY_FILTER_FIRST_SPLIT_TO_DATE.set(0L);
            plugin.MAPILLARY_FILTER_SPLIT_SECOND.set(false);
            plugin.MAPILLARY_FILTER_SECOND_SPLIT_FROM_DATE.set(0L);
            plugin.MAPILLARY_FILTER_SECOND_SPLIT_TO_DATE.set(0L);

            plugin.MAPILLARY_FILTER_PANO.set(false);
            plugin.updateLayers(mapActivity, mapActivity);

            updateTimelineUi(view, plugin, dateFormat);

            hideKeyboard();
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
        changeButtonState(view.findViewById(R.id.button_apply), 1, true);
    }

    private void disableButtonApply(View view) {
        changeButtonState(view.findViewById(R.id.button_apply), .5f, false);
    }

    private void changeButtonState(Button button, float alpha, boolean enabled) {
        button.setAlpha(alpha);
        button.setEnabled(enabled);
    }

    private void setupDateField(@NonNull MapActivity mapActivity, @NonNull View parentView, @NonNull DateFormat dateFormat,
                                @NonNull EditText dateEditText, @NonNull CommonPreference<Long> datePreference, MapillaryPlugin plugin) {
        dateEditText.setOnClickListener(v -> {

            Calendar selectedDate = Calendar.getInstance();
            long savedDateMillis = datePreference.get();
            if (savedDateMillis != 0L) {
                selectedDate.setTimeInMillis(savedDateMillis);
            }

            DatePickerDialog.OnDateSetListener dialog = (picker, year, monthOfYear, dayOfMonth) -> {
                Calendar date = Calendar.getInstance();
                date.set(Calendar.YEAR, year);
                date.set(Calendar.MONTH, monthOfYear);
                date.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                dateEditText.setText(dateFormat.format(date.getTime()));
                datePreference.set(date.getTimeInMillis());

                boolean isValid = validateDatesAndSetDateRanges(parentView, plugin, dateEditText);

                if (isValid) {
                    enableButtonApply(parentView);
                    mapActivity.getDashboard().refreshContent(true);

                    updateTimelineUi(parentView, plugin, dateFormat);
                }

            };
            new DatePickerDialog(mapActivity, dialog,
                    selectedDate.get(Calendar.YEAR),
                    selectedDate.get(Calendar.MONTH),
                    selectedDate.get(Calendar.DAY_OF_MONTH)).show();
        });
        dateEditText.setCompoundDrawablesWithIntrinsicBounds(null, null, getContentIcon(R.drawable.ic_action_arrow_drop_down), null);

        long dateMillis = datePreference.get();
        if (dateMillis != 0) {
            dateEditText.setText(dateFormat.format(new Date(dateMillis)));
        }

    }

    private void setupSplitToggle(@NonNull MapActivity mapActivity, @NonNull View parentView, @NonNull CompoundButton toggle, EditText editText, @NonNull MapillaryPlugin plugin, DateFormat dateFormat) {
        toggle.setOnCheckedChangeListener((compoundButton, isChecked) -> {
            int id = compoundButton.getId();

            if (id == R.id.date_first_split_toggle) {
                plugin.MAPILLARY_FILTER_SPLIT_FIRST.set(isChecked);
            } else if (id == R.id.date_second_split_toggle) {
                plugin.MAPILLARY_FILTER_SPLIT_SECOND.set(isChecked);
            }

            setEditTextEnabled(editText, isChecked);

            boolean isValid = validateDatesAndSetDateRanges(parentView, plugin, editText);
            if (isValid) {
                enableButtonApply(parentView);
                mapActivity.getDashboard().refreshContent(true);

                updateTimelineUi(parentView, plugin, dateFormat);
            }
            //should be called?
            plugin.updateLayers(mapActivity, mapActivity);
        });
    }

    private void setEditTextEnabled(@NonNull EditText editText, boolean enabled) {
        editText.setEnabled(enabled);
        editText.setAlpha(enabled ? 1f : 0.5f);
    }

    private boolean validateDatesAndSetDateRanges(@NonNull View parentView,
                                                  @NonNull MapillaryPlugin plugin, EditText dateEditText) {

        long from = plugin.MAPILLARY_FILTER_FROM_DATE.get();
        long to = plugin.MAPILLARY_FILTER_TO_DATE.get();
        long split1 = plugin.MAPILLARY_FILTER_FIRST_SPLIT_TO_DATE.get();
        long split2 = plugin.MAPILLARY_FILTER_SECOND_SPLIT_TO_DATE.get();
        boolean isFirstSplitActive = plugin.MAPILLARY_FILTER_SPLIT_FIRST.get();
        boolean isSecondSplitActive = plugin.MAPILLARY_FILTER_SPLIT_SECOND.get();

        int validationTextId = 0;

        int id = dateEditText.getId();

        if (from != 0 && to != 0 && from > to) {
            validationTextId = R.string.mapillary_validation_text_from_to;// "From Date can not be after To Date";

            if (id == R.id.date_to_edit_text) {
                plugin.MAPILLARY_FILTER_TO_DATE.set(0L);
                plugin.MAPILLARY_FILTER_MAIN_SPLIT_TO_DATE.set(0L);
            } else {
                plugin.MAPILLARY_FILTER_FROM_DATE.set(0L);
                plugin.MAPILLARY_FILTER_MAIN_SPLIT_FROM_DATE.set(0L);
            }
        }

        if (isFirstSplitActive && (split1 != 0 && (split1 < from || (to != 0 && split1 > to)))) {
            //validationText = "Date must be within From/To range";
            validationTextId = R.string.mapillary_validation_text_split_in_from_to;

            if (id == R.id.date_first_split_edit_text) {
                plugin.MAPILLARY_FILTER_FIRST_SPLIT_FROM_DATE.set(0L);
                plugin.MAPILLARY_FILTER_FIRST_SPLIT_TO_DATE.set(0L);
            } else {
                plugin.MAPILLARY_FILTER_FROM_DATE.set(0L);
                plugin.MAPILLARY_FILTER_MAIN_SPLIT_FROM_DATE.set(0L);
                plugin.MAPILLARY_FILTER_TO_DATE.set(0L);
                plugin.MAPILLARY_FILTER_MAIN_SPLIT_TO_DATE.set(0L);
            }
        }

        if (isSecondSplitActive && (split2 != 0 && (split2 < from || (to != 0 && split2 > to)))) {
            //validationText = "Date must be within From/To range";
            validationTextId = R.string.mapillary_validation_text_split_in_from_to;

            if (id == R.id.date_second_split_edit_text) {
                plugin.MAPILLARY_FILTER_SECOND_SPLIT_FROM_DATE.set(0L);
                plugin.MAPILLARY_FILTER_SECOND_SPLIT_TO_DATE.set(0L);
            } else {
                plugin.MAPILLARY_FILTER_FROM_DATE.set(0L);
                plugin.MAPILLARY_FILTER_MAIN_SPLIT_FROM_DATE.set(0L);
                plugin.MAPILLARY_FILTER_TO_DATE.set(0L);
                plugin.MAPILLARY_FILTER_MAIN_SPLIT_TO_DATE.set(0L);
            }
        }

        if (isFirstSplitActive && isSecondSplitActive && split1 != 0 && split2 != 0 && split1 < split2) {
            //validationText = "Second split date must be after first split date"; mapillary_validation_text_second_split_after_first
            validationTextId = R.string.mapillary_validation_text_second_split_after_first;

            if (id == R.id.date_second_split_edit_text) {
                plugin.MAPILLARY_FILTER_SECOND_SPLIT_TO_DATE.set(0L);
            } else {
                plugin.MAPILLARY_FILTER_FIRST_SPLIT_TO_DATE.set(0L);
            }
        }

        app.showShortToastMessage(validationTextId);
        //dateEditText.setError(requireContext().getString(validationTextId));

        boolean isValid = (validationTextId == 0);

        if (isValid) {
            long oneDayMillis = 24 * 60 * 60 * 1000L;

            plugin.MAPILLARY_FILTER_MAIN_SPLIT_TO_DATE.set(to);
            plugin.MAPILLARY_FILTER_MAIN_SPLIT_FROM_DATE.set(from);

            if (plugin.MAPILLARY_FILTER_SPLIT_FIRST.get() && split1 != 0){
                plugin.MAPILLARY_FILTER_FIRST_SPLIT_FROM_DATE.set(from);
                plugin.MAPILLARY_FILTER_FIRST_SPLIT_TO_DATE.set(split1);
                plugin.MAPILLARY_FILTER_MAIN_SPLIT_FROM_DATE.set(split1 + oneDayMillis);
            }

            if (plugin.MAPILLARY_FILTER_SPLIT_SECOND.get() && split2 != 0) {
                plugin.MAPILLARY_FILTER_SECOND_SPLIT_FROM_DATE.set(from);
                plugin.MAPILLARY_FILTER_SECOND_SPLIT_TO_DATE.set(split2);

                if (plugin.MAPILLARY_FILTER_SPLIT_FIRST.get() && split1 != 0) {
                    plugin.MAPILLARY_FILTER_FIRST_SPLIT_FROM_DATE.set(split2 + oneDayMillis);
                } else {
                    plugin.MAPILLARY_FILTER_MAIN_SPLIT_FROM_DATE.set(split2 + oneDayMillis);
                }
            }

        } else {
            dateEditText.setText("");
        }

        return isValid;
    }

    private void updateTimelineUi(@NonNull View parentView, @NonNull MapillaryPlugin plugin, DateFormat dateFormat) {

        View progressYellow = parentView.findViewById(R.id.progress_yellow);
        View progressRed = parentView.findViewById(R.id.progress_red);

        AppCompatTextView labelFrom = parentView.findViewById(R.id.progress_label_from);
        AppCompatTextView labelTo = parentView.findViewById(R.id.progress_label_to);

        AppCompatTextView labelFirstSplit = parentView.findViewById(R.id.progress_label_first_split);
        AppCompatTextView labelSecondSplit = parentView.findViewById(R.id.progress_label_second_split);

        CharSequence textFromDate = getDateText(dateFormat, plugin.MAPILLARY_FILTER_FROM_DATE.get());
        if (textFromDate.length() > 0 ) {
            labelFrom.setText(textFromDate);
        } else {
            labelFrom.setText("Oldest");
        }

        CharSequence textToDate = getDateText(dateFormat, plugin.MAPILLARY_FILTER_TO_DATE.get());
        if (textToDate.length() > 0) {
            labelTo.setText(textToDate);
        } else {
            labelTo.setText("Now");
        }

        CharSequence textFirstSplit = getDateText(dateFormat, plugin.MAPILLARY_FILTER_FIRST_SPLIT_TO_DATE.get());
        if (plugin.MAPILLARY_FILTER_SPLIT_FIRST.get() && textFirstSplit.length() > 0) {
            labelFirstSplit.setText(textFirstSplit);
            setLayoutWeight(progressYellow, 1f);
            setLayoutWeight(labelFirstSplit, 4f);
        } else {
            setLayoutWeight(progressYellow, 0f);
            setLayoutWeight(labelFirstSplit, 0f);
        }

        CharSequence textSecondSplit = getDateText(dateFormat, plugin.MAPILLARY_FILTER_SECOND_SPLIT_TO_DATE.get());
        if (plugin.MAPILLARY_FILTER_SPLIT_SECOND.get() && textSecondSplit.length() > 0) {
            labelSecondSplit.setText(textSecondSplit);
            setLayoutWeight(progressRed, 1f);
            setLayoutWeight(labelSecondSplit, 4f);
        } else {
            setLayoutWeight(progressRed, 0f);
            setLayoutWeight(labelSecondSplit, 0f);
        }

    }

    private CharSequence getDateText(@NonNull DateFormat dateFormat, long dateMillis) {
        return dateMillis == 0L ? "" : dateFormat.format(new Date(dateMillis));
    }

    private void setLayoutWeight(@NonNull View progressView, float weight) {
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) progressView.getLayoutParams();
        params.weight = weight;
        progressView.setLayoutParams(params);
    }

    @Override
    public InsetTargetsCollection getInsetTargets() {
        InsetTargetsCollection collection = super.getInsetTargets();
        collection.replace(InsetTarget.createBottomContainer(R.id.mapillary_filters_linear_layout).landscapeLeftSided(true));
        collection.removeType(Type.ROOT_INSET);
        return collection;
    }

    public static void showInstance(@NonNull FragmentManager fragmentManager) {
        if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
            fragmentManager.beginTransaction()
                    .replace(R.id.content, new MapillaryFiltersFragment(), TAG)
                    .commitAllowingStateLoss();
        }
    }
}