package net.osmand.plus.flyover;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
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
		}
		// again with every update: MapActivity hands the window's insets to the map whenever they change
		setMapBottomInset(activity, false);
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
			preview = new PreviewView(activity);
			preview.setTag(PREVIEW_TAG);
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
			setMapBottomInset(activity, true);
		}
		ViewGroup mapArea = activity.findViewById(R.id.map_view_with_layers);
		View preview = mapArea != null ? mapArea.findViewWithTag(PREVIEW_TAG) : null;
		if (preview != null) {
			mapArea.removeView(preview);
		}
	}

	/**
	 * The bar takes the place of the system navigation bar under the map: the map's buttons get the window insets
	 * without its bottom part, else they keep a gap for it above our bar. The real insets come back with hide().
	 */
	private static void setMapBottomInset(@NonNull MapActivity activity, boolean keep) {
		View root = activity.findViewById(R.id.drawer_layout);
		WindowInsetsCompat insets = root != null ? ViewCompat.getRootWindowInsets(root) : null;
		if (insets == null) {
			return;
		}
		if (keep) {
			// MapActivity hands the window's own insets to the map again
			ViewCompat.requestApplyInsets(root);
			return;
		}
		int types = WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout();
		Insets bars = insets.getInsets(types);
		activity.getMapLayers().setWindowInsets(new WindowInsetsCompat.Builder(insets)
				.setInsets(types, Insets.of(bars.left, bars.top, bars.right, 0))
				.build());
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
		View root = activity.findViewById(R.id.drawer_layout);
		WindowInsetsCompat insets = root != null ? ViewCompat.getRootWindowInsets(root) : null;
		if (insets != null) {
			int inset = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout()).bottom;
			bar.setPadding(bar.getPaddingLeft(), bar.getPaddingTop(), bar.getPaddingRight(), bar.getPaddingBottom() + inset);
		}
		return bar;
	}

	/** The recorded frame fitted into the map area; two fingers zoom it in up to 3x, one finger moves it, a double tap fits it again. */
	private static class PreviewView extends ImageView {

		private final Matrix matrix = new Matrix();
		private final ScaleGestureDetector scaleDetector;
		private final GestureDetector gestureDetector;
		private float scale = 1;
		private float dx;
		private float dy;

		PreviewView(@NonNull Context context) {
			super(context);
			setScaleType(ScaleType.MATRIX);
			scaleDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
				@Override
				public boolean onScale(@NonNull ScaleGestureDetector detector) {
					float next = Math.max(1, Math.min(3, scale * detector.getScaleFactor()));
					float f = next / scale;
					// the point under the fingers stays there
					dx = detector.getFocusX() - (detector.getFocusX() - dx) * f;
					dy = detector.getFocusY() - (detector.getFocusY() - dy) * f;
					scale = next;
					update();
					return true;
				}
			});
			gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
				@Override
				public boolean onScroll(@Nullable MotionEvent e1, @NonNull MotionEvent e2, float distanceX, float distanceY) {
					dx -= distanceX;
					dy -= distanceY;
					update();
					return true;
				}

				@Override
				public boolean onDoubleTap(@NonNull MotionEvent e) {
					scale = 1;
					dx = dy = 0;
					update();
					return true;
				}
			});
		}

		@Override
		public void setImageBitmap(Bitmap bm) {
			super.setImageBitmap(bm);
			update();
		}

		@Override
		protected void onSizeChanged(int w, int h, int oldw, int oldh) {
			super.onSizeChanged(w, h, oldw, oldh);
			update();
		}

		@Override
		@SuppressLint("ClickableViewAccessibility")
		public boolean onTouchEvent(MotionEvent event) {
			scaleDetector.onTouchEvent(event);
			gestureDetector.onTouchEvent(event);
			return true;
		}

		private void update() {
			Drawable d = getDrawable();
			int w = getWidth(), h = getHeight();
			if (d == null || w == 0 || h == 0) {
				return;
			}
			float fit = Math.min((float) w / d.getIntrinsicWidth(), (float) h / d.getIntrinsicHeight());
			float iw = d.getIntrinsicWidth() * fit * scale, ih = d.getIntrinsicHeight() * fit * scale;
			// centred while smaller than the view, otherwise kept covering it
			float baseX = (w - iw) / 2, baseY = (h - ih) / 2;
			dx = iw <= w ? 0 : Math.max(baseX, Math.min(-baseX, dx));
			dy = ih <= h ? 0 : Math.max(baseY, Math.min(-baseY, dy));
			matrix.setScale(fit * scale, fit * scale);
			matrix.postTranslate(baseX + dx, baseY + dy);
			setImageMatrix(matrix);
		}
	}
}
