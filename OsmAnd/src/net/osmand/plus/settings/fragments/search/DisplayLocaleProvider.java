package net.osmand.plus.settings.fragments.search;

import android.content.res.Resources;

import net.osmand.plus.R;

import java.util.Locale;

import de.KnollFrank.lib.settingssearch.common.Locales;
import de.KnollFrank.lib.settingssearch.common.LocalesReader;
import de.KnollFrank.lib.settingssearch.db.preference.pojo.converters.LocaleListConverter;

public class DisplayLocaleProvider {

	public static Locale getDisplayLocale(final Resources resources) {
		return Locales.getDisplayLocale(
				LocalesReader.readLocales(resources, R.xml.locales_config),
				new LocaleListConverter().convertForward(resources.getConfiguration().getLocales()));
	}
}
