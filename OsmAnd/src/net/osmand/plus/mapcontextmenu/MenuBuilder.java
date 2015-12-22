package net.osmand.plus.mapcontextmenu;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.ClipboardManager;
import android.text.util.Linkify;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import net.osmand.plus.IconsCache;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.render.RenderingIcons;

import java.util.LinkedList;

import static android.util.TypedValue.COMPLEX_UNIT_DIP;

public class MenuBuilder {

	public static final float SHADOW_HEIGHT_TOP_DP = 16f;

	protected OsmandApplication app;
	protected LinkedList<PlainMenuItem> plainMenuItems;
	private boolean firstRow;
	protected boolean light;

	public class PlainMenuItem {
		private int iconId;
		private String text;
		private boolean needLinks;
		private boolean url;

		public PlainMenuItem(int iconId, String text, boolean needLinks, boolean url) {
			this.iconId = iconId;
			this.text = text;
			this.needLinks = needLinks;
			this.url = url;
		}

		public int getIconId() {
			return iconId;
		}

		public String getText() {
			return text;
		}

		public boolean isNeedLinks() {
			return needLinks;
		}

		public boolean isUrl() {
			return url;
		}
	}

	public MenuBuilder(OsmandApplication app) {
		this.app = app;
		plainMenuItems = new LinkedList<>();
	}

	public void setLight(boolean light) {
		this.light = light;
	}

	public void build(View view) {
		firstRow = true;
		if (needBuildPlainMenuItems()) {
			buildPlainMenuItems(view);
		}
		buildInternal(view);
		buildAfter(view);
	}

	protected void buildPlainMenuItems(View view) {
		for (PlainMenuItem item : plainMenuItems) {
			buildRow(view, item.getIconId(), item.getText(), 0, item.isNeedLinks(), 0, item.isUrl());
		}
	}

	protected boolean needBuildPlainMenuItems() {
		return true;
	}

	protected void buildInternal(View view) {
	}

	protected void buildAfter(View view) {
		buildRowDivider(view, false);
	}

	protected boolean isFirstRow() {
		return firstRow;
	}

	protected void rowBuilt() {
		firstRow = false;
	}

	protected View buildRow(View view, int iconId, String text, int textColor, boolean needLinks, int textLinesLimit, boolean isUrl) {
		return buildRow(view, getRowIcon(iconId), text, textColor, needLinks, textLinesLimit, isUrl);
	}

	protected View buildRow(final View view, Drawable icon, final String text, int textColor, boolean needLinks, int textLinesLimit, boolean isUrl) {

		if (!isFirstRow()) {
			buildRowDivider(view, false);
		}

		LinearLayout ll = new LinearLayout(view.getContext());
		ll.setOrientation(LinearLayout.HORIZONTAL);
		LinearLayout.LayoutParams llParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		ll.setLayoutParams(llParams);
		ll.setBackgroundResource(resolveAttribute(view.getContext(), android.R.attr.selectableItemBackground));
		ll.setOnLongClickListener(new View.OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				copyToClipboard(text, view.getContext());
				return true;
			}
		});

		// Icon
		LinearLayout llIcon = new LinearLayout(view.getContext());
		llIcon.setOrientation(LinearLayout.HORIZONTAL);
		llIcon.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(72f), dpToPx(48f)));
		llIcon.setGravity(Gravity.CENTER_VERTICAL);
		ll.addView(llIcon);

		ImageView iconView = new ImageView(view.getContext());
		LinearLayout.LayoutParams llIconParams = new LinearLayout.LayoutParams(dpToPx(24f), dpToPx(24f));
		llIconParams.setMargins(dpToPx(16f), dpToPx(12f), dpToPx(32f), dpToPx(12f));
		llIconParams.gravity = Gravity.CENTER_VERTICAL;
		iconView.setLayoutParams(llIconParams);
		iconView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
		iconView.setImageDrawable(icon);
		llIcon.addView(iconView);

		// Text
		LinearLayout llText = new LinearLayout(view.getContext());
		llText.setOrientation(LinearLayout.VERTICAL);
		ll.addView(llText);

		TextView textView = new TextView(view.getContext());
		LinearLayout.LayoutParams llTextParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		llTextParams.setMargins(0, dpToPx(8f), 0, dpToPx(8f));
		textView.setLayoutParams(llTextParams);
		textView.setTextSize(16);
		textView.setTextColor(app.getResources().getColor(light ? R.color.ctx_menu_info_text_light : R.color.ctx_menu_info_text_dark));

		if (isUrl) {
			textView.setTextColor(textView.getLinkTextColors());
		} else if (needLinks) {
			textView.setAutoLinkMask(Linkify.ALL);
			textView.setLinksClickable(true);
		}
		if (textLinesLimit > 0) {
			textView.setMinLines(1);
			textView.setMaxLines(textLinesLimit);
		}
		textView.setText(text);
		if (textColor > 0) {
			textView.setTextColor(view.getResources().getColor(textColor));
		}

		LinearLayout.LayoutParams llTextViewParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		llTextViewParams.setMargins(0, 0, dpToPx(10f), 0);
		llTextViewParams.gravity = Gravity.CENTER_VERTICAL;
		llText.setLayoutParams(llTextViewParams);
		llText.addView(textView);

		if (isUrl) {
			ll.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					Intent intent = new Intent(Intent.ACTION_VIEW);
					intent.setData(Uri.parse(text));
					v.getContext().startActivity(intent);
				}
			});
		}

		((LinearLayout) view).addView(ll);

		rowBuilt();

		return ll;
	}

	protected void copyToClipboard(String text, Context ctx) {
		((ClipboardManager) app.getSystemService(Activity.CLIPBOARD_SERVICE)).setText(text);
		Toast.makeText(ctx,
				ctx.getResources().getString(R.string.copied_to_clipboard) + ":\n" + text,
				Toast.LENGTH_SHORT).show();
	}

	protected void buildButtonRow(final View view, Drawable buttonIcon, String text, OnClickListener onClickListener) {
		LinearLayout ll = new LinearLayout(view.getContext());
		ll.setOrientation(LinearLayout.HORIZONTAL);
		LinearLayout.LayoutParams llParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		ll.setLayoutParams(llParams);
		ll.setBackgroundResource(resolveAttribute(view.getContext(), android.R.attr.selectableItemBackground));

		// Empty
		LinearLayout llIcon = new LinearLayout(view.getContext());
		llIcon.setOrientation(LinearLayout.HORIZONTAL);
		llIcon.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(62f), dpToPx(58f)));
		llIcon.setGravity(Gravity.CENTER_VERTICAL);
		ll.addView(llIcon);


		// Button
		LinearLayout llButton = new LinearLayout(view.getContext());
		llButton.setOrientation(LinearLayout.VERTICAL);
		ll.addView(llButton);

		Button buttonView = new Button(view.getContext());
		LinearLayout.LayoutParams llBtnParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		buttonView.setLayoutParams(llBtnParams);
		buttonView.setPadding(dpToPx(10f), 0, dpToPx(10f), 0);
		buttonView.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
		//buttonView.setTextSize(view.getResources().getDimension(resolveAttribute(view.getContext(), R.dimen.default_desc_text_size)));
		buttonView.setTextColor(view.getResources().getColor(resolveAttribute(view.getContext(), R.attr.contextMenuButtonColor)));
		buttonView.setText(text);

		if (buttonIcon != null) {
			buttonView.setCompoundDrawablesWithIntrinsicBounds(buttonIcon, null, null, null);
			buttonView.setCompoundDrawablePadding(dpToPx(8f));
		}
		llButton.addView(buttonView);

		((LinearLayout) view).addView(ll);

		ll.setOnClickListener(onClickListener);

		rowBuilt();
	}

	protected void buildRowDivider(View view, boolean matchWidth) {
		View horizontalLine = new View(view.getContext());
		LinearLayout.LayoutParams llHorLineParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(1f));
		llHorLineParams.gravity = Gravity.BOTTOM;
		if (!matchWidth) {
			llHorLineParams.setMargins(dpToPx(72f), 0, 0, 0);
		}
		horizontalLine.setLayoutParams(llHorLineParams);
		horizontalLine.setBackgroundColor(app.getResources().getColor(light ? R.color.ctx_menu_info_divider_light : R.color.ctx_menu_info_divider_dark));
		((LinearLayout) view).addView(horizontalLine);
	}

	public boolean hasCustomAddressLine() {
		return false;
	}

	public void buildCustomAddressLine(LinearLayout ll) {
	}

	public void addPlainMenuItem(int iconId, String text, boolean needLinks, boolean isUrl) {
		plainMenuItems.add(new PlainMenuItem(iconId, text, needLinks, isUrl));
	}

	public void clearPlainMenuItems() {
		plainMenuItems.clear();
	}

	public Drawable getRowIcon(int iconId) {
		IconsCache iconsCache = app.getIconsCache();
		return iconsCache.getIcon(iconId,
				light ? R.color.icon_color : R.color.icon_color_light);
	}

	public Drawable getRowIcon(Context ctx, String fileName) {
		Drawable d = RenderingIcons.getBigIcon(ctx, fileName);
		if (d != null) {
			d.setColorFilter(app.getResources()
					.getColor(light ? R.color.icon_color : R.color.icon_color_light), PorterDuff.Mode.SRC_IN);
			return d;
		} else {
			return null;
		}
	}

	public int resolveAttribute(Context ctx, int attribute) {
		TypedValue outValue = new TypedValue();
		ctx.getTheme().resolveAttribute(attribute, outValue, true);
		return outValue.resourceId;
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
