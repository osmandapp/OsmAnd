package net.osmand.aidl;

import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;

/**
 * While a flyover is recorded the map belongs to the recorder: a banner over the map shows the progress and a cancel
 * button, and the map under it takes no touches. Added again if MapActivity was recreated meanwhile.
 */
final class FlyoverBanner {

	private static final String TAG = "flyover_banner";
	private static final String TEXT_TAG = "flyover_banner_text";

	private final OsmandApplication app;
	private final Runnable onCancel;

	FlyoverBanner(@NonNull OsmandApplication app, @NonNull Runnable onCancel) {
		this.app = app;
		this.onCancel = onCancel;
	}

	/** On the UI thread. */
	void show(int percent) {
		ViewGroup root = getRoot();
		if (root == null) {
			return;
		}
		View banner = root.findViewWithTag(TAG);
		if (banner == null) {
			banner = create(root);
		}
		TextView text = banner.findViewWithTag(TEXT_TAG);
		text.setText(app.getString(R.string.flyover_recording, percent + "%"));
	}

	/** On the UI thread. */
	void hide() {
		ViewGroup root = getRoot();
		View banner = root != null ? root.findViewWithTag(TAG) : null;
		if (banner != null) {
			root.removeView(banner);
		}
	}

	@Nullable
	private ViewGroup getRoot() {
		MapActivity activity = app.getOsmandMap().getMapView().getMapActivity();
		return activity != null && !activity.isFinishing() ? activity.findViewById(android.R.id.content) : null;
	}

	@NonNull
	private View create(@NonNull ViewGroup root) {
		MapActivity activity = app.getOsmandMap().getMapView().getMapActivity();
		boolean night = app.getDaynightHelper().isNightMode(net.osmand.plus.settings.enums.ThemeUsageContext.MAP);
		FrameLayout cover = new FrameLayout(activity);
		cover.setTag(TAG);
		// the map under the banner takes no touches: they would move the camera of the video
		cover.setClickable(true);
		cover.setFocusable(true);

		LinearLayout card = new LinearLayout(activity);
		card.setOrientation(LinearLayout.HORIZONTAL);
		card.setGravity(Gravity.CENTER_VERTICAL);
		int pad = AndroidUtils.dpToPx(activity, 16);
		card.setPadding(pad, pad / 2, pad / 2, pad / 2);
		card.setBackgroundResource(night ? R.drawable.bg_card_dark : R.drawable.bg_card_light);

		TextView title = new TextView(activity);
		title.setTag(TEXT_TAG);
		title.setTextSize(16);
		title.setTextColor(ColorUtilities.getPrimaryTextColor(activity, night));
		card.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

		Button cancel = new Button(activity, null, android.R.attr.borderlessButtonStyle);
		cancel.setText(R.string.shared_string_cancel);
		cancel.setTextColor(ColorUtilities.getActiveColor(activity, night));
		cancel.setOnClickListener(v -> {
			v.setEnabled(false);
			onCancel.run();
		});
		card.addView(cancel);

		FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM);
		params.setMargins(pad, pad, pad, pad * 2);
		cover.addView(card, params);
		root.addView(cover, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.MATCH_PARENT));
		return cover;
	}
}
