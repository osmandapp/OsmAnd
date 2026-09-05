package net.osmand.plus.auto.views;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;

import androidx.annotation.AnyRes;
import androidx.annotation.AttrRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class AndroidAutoMapPlaceholderView extends FrameLayout {

	private final View container;
	private final ImageView icon;
	private final TextView title;
	private final TextView desc;

	public AndroidAutoMapPlaceholderView(@NonNull Context context) {
		this(context, null);
	}

	public AndroidAutoMapPlaceholderView(@NonNull Context context, @Nullable AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public AndroidAutoMapPlaceholderView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
		this(context, attrs, defStyleAttr, 0);
	}

	public AndroidAutoMapPlaceholderView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);

		boolean nightMode = getMyApplication().getDaynightHelper().isNightMode();
		inflate(UiUtilities.getThemedContext(context, nightMode), R.layout.android_auto_map_placeholder, this);

		container = findViewById(R.id.container);
		icon = findViewById(R.id.icon);
		title = findViewById(R.id.title);
		desc = findViewById(R.id.desc);
	}

	public void updateNightMode(boolean nightMode) {
		Context themedContext = UiUtilities.getThemedContext(getContext(), nightMode);
		container.setBackgroundResource(resolveAttr(themedContext, R.attr.mapBackground));
		icon.setImageDrawable(getIcon(resolveAttr(themedContext, R.attr.ic_action_android_auto_logo_colored)));
		title.setTextColor(ColorUtilities.getPrimaryTextColor(themedContext, nightMode));
		desc.setTextColor(ColorUtilities.getSecondaryTextColor(themedContext, nightMode));
	}

	@NonNull
	private Drawable getIcon(@DrawableRes int drawableId) {
		return getMyApplication().getUIUtilities().getIcon(drawableId);
	}

	@AnyRes
	private int resolveAttr(@NonNull Context context, @AttrRes int attrId) {
		return AndroidUtils.resolveAttribute(context, attrId);
	}

	@NonNull
	private OsmandApplication getMyApplication() {
		return ((OsmandApplication) getContext().getApplicationContext());
	}
}