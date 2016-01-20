package net.osmand.plus.base;

import android.graphics.drawable.Drawable;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.IdRes;
import android.support.v4.app.Fragment;
import android.view.View;
import android.widget.ImageView;

import net.osmand.plus.IconsCache;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.activities.OsmandActionBarActivity;

public class BaseOsmAndFragment extends Fragment {
	private IconsCache iconsCache;

	protected OsmandApplication getMyApplication() {
		return (OsmandApplication) getActivity().getApplication();
	}

	protected OsmandActionBarActivity getMyActivity() {
		return (OsmandActionBarActivity) getActivity();
	}

	protected IconsCache getIconsCache() {
		if (iconsCache == null) {
			iconsCache = getMyApplication().getIconsCache();
		}
		return iconsCache;
	}

	protected Drawable getPaintedContentIcon(@DrawableRes int id, @ColorInt int color){
		return getIconsCache().getPaintedContentIcon(id, color);
	}

	protected Drawable getIcon(@DrawableRes int id, @ColorRes int colorId){
		return getIconsCache().getIcon(id, colorId);
	}

	protected Drawable getContentIcon(@DrawableRes int id){
		return getIconsCache().getContentIcon(id);
	}

	protected void setThemedDrawable(View parent, @IdRes int viewId, @DrawableRes int iconId) {
		((ImageView) parent.findViewById(viewId)).setImageDrawable(getContentIcon(iconId));
	}

	protected void setThemedDrawable(View view, @DrawableRes int iconId) {
		((ImageView) view).setImageDrawable(getContentIcon(iconId));
	}

	protected OsmandSettings getSettings() {
		return getMyApplication().getSettings();
	}
}
