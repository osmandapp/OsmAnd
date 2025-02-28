package net.osmand.plus.configmap;

import android.os.Handler;
import android.view.View;

import androidx.fragment.app.Fragment;

import org.threeten.bp.Duration;

import java.util.Map;

import de.KnollFrank.lib.settingssearch.results.PositionOfSettingProvider;
import de.KnollFrank.lib.settingssearch.results.Setting;
import de.KnollFrank.lib.settingssearch.results.SettingHighlighter;
import de.KnollFrank.lib.settingssearch.results.ViewHighlighter;

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
		highlightItem(positionOfSettingProvider.getPositionOfSetting(setting).orElseThrow());
	}

	private void highlightItem(final int itemPosition) {
		new Handler().post(() -> _highlightItem(itemPosition));
	}

	private void _highlightItem(final int itemPosition) {
		// itemsContainer.scrollToPosition(itemPosition);
		final View view = views.get(itemPosition);
		if (view != null) {
			view.postDelayed(
					() -> ViewHighlighter.highlightView(view, highlightDuration),
					200);
		}
	}
}
