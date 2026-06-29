/*
 * OsmAnd, OsmAnd BV <info@osmand.net>, 2010–present
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.osmand.plus.plugins.driverbreak;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Result of hiking/cycling route validation from driver_break_route_validator.c.
 */
public class ValidationResult {

	private final double forbiddenFraction;
	private final double priorityFraction;
	private final int maxMtbScale;
	private final int maxMtbScaleUphill;
	private final List<String> warnings;

	public ValidationResult(double forbiddenFraction, double priorityFraction,
			int maxMtbScale, int maxMtbScaleUphill, @NonNull List<String> warnings) {
		this.forbiddenFraction = forbiddenFraction;
		this.priorityFraction = priorityFraction;
		this.maxMtbScale = maxMtbScale;
		this.maxMtbScaleUphill = maxMtbScaleUphill;
		this.warnings = new ArrayList<>(warnings);
	}

	public double getForbiddenFraction() {
		return forbiddenFraction;
	}

	public double getPriorityFraction() {
		return priorityFraction;
	}

	public int getMaxMtbScale() {
		return maxMtbScale;
	}

	public int getMaxMtbScaleUphill() {
		return maxMtbScaleUphill;
	}

	@NonNull
	public List<String> getWarnings() {
		return Collections.unmodifiableList(warnings);
	}
}
