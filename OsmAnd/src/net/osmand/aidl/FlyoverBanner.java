package net.osmand.aidl;

import android.graphics.Bitmap;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.settings.enums.ThemeUsageContext;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;

/**
 * While a flyover is recorded the map belongs to the recorder. The map area shows the frames being recorded, a bar
 * under it the progress and a cancel button; the rest of the app stays usable (the map itself takes no touches).
 * Added again if MapActivity was recreated meanwhile.
 */
final class FlyoverBanner {

	private static final String BAR_TAG = "flyover_bar";
	private static final String TEXT_TAG = "flyover_bar_text";
	private static final String PREVIEW_TAG = "flyover_preview";

	private final OsmandApplication app;
	private final Runnable onCancel;

	FlyoverBanner(@NonNull OsmandApplication app, @NonNull Runnable onCancel) {
		this.app = app;
		this.onCancel = onCancel;
	}

	/** On the UI thread. */
	void show(int percent) {
		MapActivity activity = getActivity();
		ViewGroup main = activity != null ? getMainLayout(activity) : null;
		if (main == null) {
			return;
		}
		View bar = main.findViewWithTag(BAR_TAG);
		if (bar == null) {
			bar = createBar(activity);
			main.addView(bar);
			ViewCompat.requestApplyInsets(bar);
		}
		TextView text = bar.findViewWithTag(TEXT_TAG);
		text.setText(app.getString(R.string.flyover_recording, percent + "%"));
	}

	/** The last recorded frame in place of the map, on the UI thread. */
	void showFrame(@NonNull Bitmap frame) {
		MapActivity activity = getActivity();
		ViewGroup mapArea = activity != null ? activity.findViewById(R.id.map_view_with_layers) : null;
		if (mapArea == null) {
			return;
		}
		ImageView preview = mapArea.findViewWithTag(PREVIEW_TAG);
		if (preview == null) {
			preview = new ImageView(activity);
			preview.setTag(PREVIEW_TAG);
			preview.setScaleType(ImageView.ScaleType.FIT_CENTER);
			preview.setBackgroundColor(0xFF202124);
			mapArea.addView(preview, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
					ViewGroup.LayoutParams.MATCH_PARENT));
		}
		preview.setImageBitmap(frame);
	}

	/** On the UI thread. */
	void hide() {
		MapActivity activity = getActivity();
		if (activity == null) {
			return;
		}
		ViewGroup main = getMainLayout(activity);
		View bar = main != null ? main.findViewWithTag(BAR_TAG) : null;
		if (bar != null) {
			main.removeView(bar);
		}
		ViewGroup mapArea = activity.findViewById(R.id.map_view_with_layers);
		View preview = mapArea != null ? mapArea.findViewWithTag(PREVIEW_TAG) : null;
		if (preview != null) {
			mapArea.removeView(preview);
		}
	}

	@Nullable
	private MapActivity getActivity() {
		MapActivity activity = app.getOsmandMap().getMapView().getMapActivity();
		return activity != null && !activity.isFinishing() ? activity : null;
	}

	/** The vertical layout of main.xml with the map area: the bar goes under the map and lifts it. */
	@Nullable
	private static ViewGroup getMainLayout(@NonNull MapActivity activity) {
		ViewGroup drawer = activity.findViewById(R.id.drawer_layout);
		View first = drawer != null && drawer.getChildCount() > 0 ? drawer.getChildAt(0) : null;
		return first instanceof LinearLayout ? (ViewGroup) first : null;
	}

	@NonNull
	private View createBar(@NonNull MapActivity activity) {
		boolean night = app.getDaynightHelper().isNightMode(ThemeUsageContext.MAP);
		LinearLayout bar = new LinearLayout(activity);
		bar.setTag(BAR_TAG);
		bar.setOrientation(LinearLayout.HORIZONTAL);
		bar.setGravity(Gravity.CENTER_VERTICAL);
		int pad = AndroidUtils.dpToPx(activity, 16);
		bar.setPadding(pad, pad / 4, pad / 2, pad / 4);
		bar.setBackgroundColor(ColorUtilities.getListBgColor(activity, night));

		TextView title = new TextView(activity);
		title.setTag(TEXT_TAG);
		title.setTextSize(16);
		title.setTextColor(ColorUtilities.getPrimaryTextColor(activity, night));
		bar.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

		Button cancel = new Button(activity, null, android.R.attr.borderlessButtonStyle);
		cancel.setText(R.string.shared_string_cancel);
		cancel.setTextColor(ColorUtilities.getActiveColor(activity, night));
		cancel.setOnClickListener(v -> {
			v.setEnabled(false);
			onCancel.run();
		});
		bar.addView(cancel);
		bar.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.WRAP_CONTENT));
		// the map is drawn edge to edge: the bar stays above the system navigation bar
		int top = bar.getPaddingTop(), bottom = bar.getPaddingBottom();
		ViewCompat.setOnApplyWindowInsetsListener(bar, (v, insets) -> {
			int inset = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout()).bottom;
			v.setPadding(v.getPaddingLeft(), top, v.getPaddingRight(), bottom + inset);
			return insets;
		});
		return bar;
	}
}
