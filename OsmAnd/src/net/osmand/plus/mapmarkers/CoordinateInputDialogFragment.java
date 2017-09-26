package net.osmand.plus.mapmarkers;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.TextView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;

public class CoordinateInputDialogFragment extends DialogFragment {

	public static final String TAG = "CoordinateInputDialogFragment";

	private boolean lightTheme;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		OsmandApplication app = getMyApplication();
		lightTheme = app.getSettings().OSMAND_THEME.get() == OsmandSettings.OSMAND_LIGHT_THEME;
		int themeId = lightTheme ? R.style.OsmandLightTheme : R.style.OsmandDarkTheme;
		setStyle(STYLE_NO_FRAME, themeId);
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		final View mainView = inflater.inflate(R.layout.fragment_coordinate_input_dialog, container);
		final MapActivity mapActivity = getMapActivity();

		Toolbar toolbar = (Toolbar) mainView.findViewById(R.id.coordinate_input_toolbar);
		if (!lightTheme) {
			toolbar.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.actionbar_dark_color));
		}

		toolbar.setNavigationIcon(getMyApplication().getIconsCache().getIcon(R.drawable.ic_arrow_back));
		toolbar.setNavigationOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				dismiss();
			}
		});
		final View optionsButton = mainView.findViewById(R.id.options_button);
		optionsButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {

			}
		});

		final EditText latitudeEditText = (EditText) mainView.findViewById(R.id.latitude_edit_text);
		final EditText longitudeEditText = (EditText) mainView.findViewById(R.id.longitude_edit_text);
		final EditText nameEditText = (EditText) mainView.findViewById(R.id.name_edit_text);

		View.OnTouchListener editTextOnTouchListener = new View.OnTouchListener() {
			@Override
			public boolean onTouch(View view, MotionEvent motionEvent) {
				EditText editText = null;
				switch (view.getId()) {
					case R.id.latitude_edit_text:
						editText = latitudeEditText;
						break;
					case R.id.longitude_edit_text:
						editText = longitudeEditText;
						break;
					case R.id.name_edit_text:
						editText = nameEditText;
						break;
				}
				if (editText != null) {
					editText.requestFocus();
				}
				return true;
			}
		};

		latitudeEditText.setOnTouchListener(editTextOnTouchListener);
		longitudeEditText.setOnTouchListener(editTextOnTouchListener);
		nameEditText.setOnTouchListener(editTextOnTouchListener);

		String[] keyboardItems = new String[] { "1", "2", "3",
				"4", "5", "6",
				"7", "8", "9",
				getString(R.string.shared_string_delete), "0", getString(R.string.shared_string_clear) };
		GridView keyboardGrid = (GridView) mainView.findViewById(R.id.keyboard_grid_view);
		KeyboardAdapter keyboardAdapter = new KeyboardAdapter(mapActivity, keyboardItems);
		keyboardGrid.setAdapter(keyboardAdapter);

		return mainView;
	}

	private MapActivity getMapActivity() {
		return (MapActivity) getActivity();
	}

	private OsmandApplication getMyApplication() {
		return (OsmandApplication) getActivity().getApplication();
	}

	public static boolean showInstance(@NonNull MapActivity mapActivity) {
		try {
			if (mapActivity.isActivityDestroyed()) {
				return false;
			}
			CoordinateInputDialogFragment fragment = new CoordinateInputDialogFragment();
			fragment.show(mapActivity.getSupportFragmentManager(), TAG);
			return true;
		} catch (RuntimeException e) {
			return false;
		}
	}

	private class KeyboardAdapter extends ArrayAdapter<String> {

		KeyboardAdapter(@NonNull Context context, String[] items) {
			super(context, 0, items);
		}

		@NonNull
		@Override
		public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
			if (convertView == null) {
				convertView = LayoutInflater.from(getContext()).inflate(R.layout.input_coordinate_keyboard_item, parent, false);
			}

			((TextView) convertView.findViewById(R.id.keyboard_item)).setText(getItem(position));

			return convertView;
		}
	}

}
