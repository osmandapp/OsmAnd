package net.osmand.plus.mapcontextmenu;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.util.Linkify;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.osmand.plus.IconsCache;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.render.RenderingIcons;

import java.util.LinkedList;

import static android.util.TypedValue.COMPLEX_UNIT_DIP;

public abstract class MenuBuilder {

	public static final float SHADOW_HEIGHT_TOP_DP = 16f;
	public static final float SHADOW_HEIGHT_BOTTOM_DP = 6f;

	protected OsmandApplication app;
	protected LinkedList<PlainMenuItem> plainMenuItems;
	private boolean firstRow;
	private boolean light;

	public class PlainMenuItem {
		private int iconId;
		private String text;

		public PlainMenuItem(int iconId, String text) {
			this.iconId = iconId;
			this.text = text;
		}

		public int getIconId() {
			return iconId;
		}

		public String getText() {
			return text;
		}
	}

	public MenuBuilder(OsmandApplication app) {
		this.app = app;
		plainMenuItems = new LinkedList<>();
		light = app.getSettings().isLightContent();
	}

	public void build(View view) {
		firstRow = true;
	}

	protected boolean isFirstRow() {
		return firstRow;
	}

	protected void rowBuilt() {
		firstRow = false;
	}

	protected void buildRow(final View view, Drawable icon, String text, int textColor) {
		LinearLayout ll = new LinearLayout(view.getContext());
		ll.setOrientation(LinearLayout.HORIZONTAL);
		LinearLayout.LayoutParams llParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		ll.setLayoutParams(llParams);

		// Icon
		LinearLayout llIcon = new LinearLayout(view.getContext());
		llIcon.setOrientation(LinearLayout.HORIZONTAL);
		llIcon.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(72f), isFirstRow() ? dpToPx(48f) - dpToPx(SHADOW_HEIGHT_BOTTOM_DP) : dpToPx(48f)));
		llIcon.setGravity(Gravity.CENTER_VERTICAL);
		ll.addView(llIcon);

		ImageView iconView = new ImageView(view.getContext());
		LinearLayout.LayoutParams llIconParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
		llIconParams.setMargins(dpToPx(16f), isFirstRow() ? dpToPx(12f) - dpToPx(SHADOW_HEIGHT_BOTTOM_DP / 2f) : dpToPx(12f), dpToPx(32f), dpToPx(12f));
		llIconParams.gravity = Gravity.CENTER_VERTICAL;
		iconView.setLayoutParams(llIconParams);
		iconView.setScaleType(ImageView.ScaleType.CENTER);
		iconView.setImageDrawable(icon);
		llIcon.addView(iconView);

		// Text
		LinearLayout llText = new LinearLayout(view.getContext());
		llText.setOrientation(LinearLayout.VERTICAL);
		ll.addView(llText);

		TextView textView = new TextView(view.getContext());
		LinearLayout.LayoutParams llTextParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		llTextParams.setMargins(0, isFirstRow() ? dpToPx(8f) - dpToPx(SHADOW_HEIGHT_BOTTOM_DP) : dpToPx(8f), 0, dpToPx(8f));
		textView.setLayoutParams(llTextParams);
		textView.setTextSize(16);
		textView.setTextColor(app.getResources().getColor(light ? R.color.ctx_menu_info_text_light : R.color.ctx_menu_info_text_dark));

		textView.setAutoLinkMask(Linkify.ALL);
		textView.setLinksClickable(true);
		textView.setText(text);
		if (textColor > 0) {
			textView.setTextColor(view.getResources().getColor(textColor));
		}

		LinearLayout.LayoutParams llTextViewParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		llTextViewParams.setMargins(0, 0, dpToPx(10f), 0);
		llTextViewParams.gravity = Gravity.CENTER_VERTICAL;
		llText.setLayoutParams(llTextViewParams);
		llText.addView(textView);

		((LinearLayout) view).addView(ll);

		View horizontalLine = new View(view.getContext());
		LinearLayout.LayoutParams llHorLineParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(1f));
		llHorLineParams.gravity = Gravity.BOTTOM;
		horizontalLine.setLayoutParams(llHorLineParams);

		horizontalLine.setBackgroundColor(app.getResources().getColor(light ? R.color.ctx_menu_info_divider_light : R.color.ctx_menu_info_divider_dark));

		((LinearLayout) view).addView(horizontalLine);

		rowBuilt();
	}

	public void addPlainMenuItem(int iconId, String text) {
		plainMenuItems.add(new PlainMenuItem(iconId, text));
	}

	public Drawable getRowIcon(int iconId) {
		IconsCache iconsCache = app.getIconsCache();
		boolean light = app.getSettings().isLightContent();
		return iconsCache.getIcon(iconId,
				light ? R.color.icon_color : R.color.icon_color_light);
	}

	public Drawable getRowIcon(Context ctx, String fileName) {
		Bitmap iconBitmap = RenderingIcons.getIcon(ctx, fileName, false);
		if (iconBitmap != null) {
			return new BitmapDrawable(ctx.getResources(), iconBitmap);
		} else {
			return null;
		}
	}

	public int dpToPx(float dp) {
		Resources r = app.getResources();
		return (int) TypedValue.applyDimension(
				COMPLEX_UNIT_DIP,
				dp,
				r.getDisplayMetrics()
		);
	}
}
