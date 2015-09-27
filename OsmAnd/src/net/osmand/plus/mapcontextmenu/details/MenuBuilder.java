package net.osmand.plus.mapcontextmenu.details;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;

import net.osmand.plus.IconsCache;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.render.RenderingIcons;

import java.util.LinkedList;

public abstract class MenuBuilder {

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

	public static final float SHADOW_HEIGHT_TOP_DP = 16f;
	public static final float SHADOW_HEIGHT_BOTTOM_DP = 6f;

	protected OsmandApplication app;
	protected LinkedList<PlainMenuItem> plainMenuItems;
	private boolean firstRow;

	public MenuBuilder(OsmandApplication app) {
		this.app = app;
		plainMenuItems = new LinkedList<>();
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
}
