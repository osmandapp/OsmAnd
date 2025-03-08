package net.osmand.plus.configmap;

import android.os.Handler;
import android.view.View;

import androidx.fragment.app.Fragment;

import org.threeten.bp.Duration;

import java.util.function.Function;

import de.KnollFrank.lib.settingssearch.results.PositionOfSettingProvider;
import de.KnollFrank.lib.settingssearch.results.Setting;
import de.KnollFrank.lib.settingssearch.results.SettingHighlighter;

public class ViewHighlighter implements SettingHighlighter {

	private final Function<Integer, View> getViewAtPosition;
	private final PositionOfSettingProvider positionOfSettingProvider;
	private final Duration highlightDuration;

	public ViewHighlighter(final Function<Integer, View> getViewAtPosition,
						   final PositionOfSettingProvider positionOfSettingProvider,
						   final Duration highlightDuration) {
		this.getViewAtPosition = getViewAtPosition;
		this.positionOfSettingProvider = positionOfSettingProvider;
		this.highlightDuration = highlightDuration;
	}

	@Override
	public void highlightSetting(final Fragment settingsFragment, final Setting setting) {
		highlightItem(positionOfSettingProvider.getPositionOfSetting(setting).orElseThrow());
	}

	private void highlightItem(final int itemPosition) {
		new Handler().post(() -> _highlightItem(itemPosition));
	}

	private void _highlightItem(final int itemPosition) {
		// itemsContainer.scrollToPosition(itemPosition);
		final View view = getViewAtPosition.apply(itemPosition);
		if (view != null) {
			view.postDelayed(
					() -> de.KnollFrank.lib.settingssearch.results.ViewHighlighter.highlightView(view, highlightDuration),
					200);
		}
	}
}
