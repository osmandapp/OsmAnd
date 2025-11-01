package net.osmand.plus.configmap;

import android.os.Handler;
import android.view.View;

import androidx.fragment.app.Fragment;

import java.time.Duration;
import java.util.function.Function;

import de.KnollFrank.lib.settingssearch.results.Setting;
import de.KnollFrank.lib.settingssearch.results.SettingHighlighter;
import de.KnollFrank.lib.settingssearch.results.ViewHighlighter;

public class ViewOfSettingHighlighter implements SettingHighlighter {

	private final Function<Setting, View> getViewOfSetting;
	private final Duration highlightDuration;

	public ViewOfSettingHighlighter(final Function<Setting, View> getViewOfSetting,
									final Duration highlightDuration) {
		this.getViewOfSetting = getViewOfSetting;
		this.highlightDuration = highlightDuration;
	}

	@Override
	public void highlightSetting(final Fragment settingsFragment, final Setting setting) {
		highlightView(getViewOfSetting.apply(setting));
	}

	private void highlightView(final View view) {
		// FK-TODO: too much post() because postDelayed() in _highlightView() is enough?
		new Handler().post(() -> _highlightView(view));
	}

	private void _highlightView(final View view) {
		// itemsContainer.scrollToPosition(itemPosition);
		view.postDelayed(
				() -> ViewHighlighter.highlightView(view, highlightDuration),
				200);
	}
}
