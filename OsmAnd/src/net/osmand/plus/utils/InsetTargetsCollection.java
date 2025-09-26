package net.osmand.plus.utils;

import androidx.annotation.NonNull;

import net.osmand.plus.utils.InsetTarget.Type;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class InsetTargetsCollection {

	private final Map<Type, List<InsetTarget>> targets = new EnumMap<>(InsetTarget.Type.class);

	public void add(@NonNull InsetTarget target) {
		targets.computeIfAbsent(target.getType(), k -> new ArrayList<>()).add(target);
	}

	public void replace(@NonNull InsetTarget target) {
		List<InsetTarget> list = new ArrayList<>();
		list.add(target);
		targets.put(target.getType(), list);
	}

	public void removeType(@NonNull InsetTarget.Type type) {
		targets.remove(type);
	}

	public List<InsetTarget> getAll() {
		return targets.values().stream()
				.flatMap(Collection::stream)
				.collect(Collectors.toList());
	}

	public List<InsetTarget> getByType(@NonNull InsetTarget.Type type) {
		return targets.getOrDefault(type, Collections.emptyList());
	}
}
