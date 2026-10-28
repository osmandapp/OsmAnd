package net.osmand.plus.dashboard;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.dashboard.DashboardOnMap.DashboardType;

import java.util.Stack;

public class DashboardVisibilityStack {

	private final Stack<DashboardType> stack = new Stack<>();

	public void add(@NonNull DashboardType type) {
		stack.add(type);
	}

	@Nullable
	public DashboardType getCurrent() {
		return stack.empty() ? null : stack.lastElement();
	}

	@Nullable
	public DashboardType getPrevious() {
		int index = stack.size() - 2;
		return index >= 0 ? stack.get(index) : null;
	}

	@Nullable
	public DashboardType pop() {
		return !stack.empty() ? stack.pop() : null;
	}

	public void clear() {
		stack.clear();
	}
}
