package net.osmand.plus.configmap;

import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.Log;
import android.view.View;

import androidx.annotation.ColorInt;
import androidx.fragment.app.Fragment;

import org.threeten.bp.Duration;

import java.util.Map;
import java.util.OptionalInt;

import de.KnollFrank.lib.settingssearch.common.Attributes;
import de.KnollFrank.lib.settingssearch.results.PositionOfSettingProvider;
import de.KnollFrank.lib.settingssearch.results.Setting;
import de.KnollFrank.lib.settingssearch.results.SettingHighlighter;

public class ItemOfLinearLayoutHighlighter implements SettingHighlighter {

	private final Map<Integer, View> views;
	private final PositionOfSettingProvider positionOfSettingProvider;
	private final Duration highlightDuration;

	public ItemOfLinearLayoutHighlighter(final Map<Integer, View> views,
										 final PositionOfSettingProvider positionOfSettingProvider,
										 final Duration highlightDuration) {
		this.views = views;
		this.positionOfSettingProvider = positionOfSettingProvider;
		this.highlightDuration = highlightDuration;
	}

	@Override
	public void highlightSetting(final Fragment settingsFragment, final Setting setting) {
		highlightItem(positionOfSettingProvider.getPositionOfSetting(setting));
	}

	private void highlightItem(final OptionalInt itemPosition) {
		itemPosition.ifPresentOrElse(
				this::highlightItem,
				() -> Log.e("doHighlight", "Setting not found on given screen"));
	}

	private void highlightItem(final int itemPosition) {
		new Handler().post(() -> _highlightItem(itemPosition));
	}

	private void _highlightItem(final int itemPosition) {
		// itemsContainer.scrollToPosition(itemPosition);
		final View view = views.get(itemPosition);
		if (view != null) {
			view.postDelayed(
					() -> {
						final Drawable oldBackground = view.getBackground();
						final @ColorInt int color = Attributes.getColorFromAttr(view.getContext(), android.R.attr.textColorPrimary);
						view.setBackgroundColor(color & 0xffffff | 0x33000000);
						new Handler().postDelayed(
								() -> view.setBackgroundDrawable(oldBackground),
								highlightDuration.toMillis());
					},
					200);
		}
	}
}
