package net.osmand.plus.settings.fragments.search;

import androidx.preference.Preference;

import java.util.Optional;

// FK-TODO: rename
@FunctionalInterface
public interface InfoProvider {

	// FK-TODO: rename method
	Optional<Info> getInfo(Preference preference);
}
