package net.osmand.plus.utils;

import android.util.Pair;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class AndroidDbUtils {

	@NonNull
	public static String createDbInsertQuery(@NonNull String tableName, @NonNull Set<String> rowKeys) {
		StringBuilder keys = new StringBuilder();
		StringBuilder values = new StringBuilder();
		String split = ", ";
		Iterator<String> iterator = rowKeys.iterator();
		while (iterator.hasNext()) {
			keys.append(iterator.next());
			values.append("?");
			if (iterator.hasNext()) {
				keys.append(split);
				values.append(split);
			}
		}
		return "INSERT INTO " + tableName + " (" + keys + ") VALUES (" + values + ")";
	}

	@NonNull
	public static Pair<String, Object[]> createDbUpdateQuery(@NonNull String tableName,
	                                                         @NonNull Map<String, Object> columnsToUpdate,
	                                                         @NonNull Map<String, Object> columnsToSearch) {
		List<Object> values = new ArrayList<>();
		String updateQuery = getRowsQuery(columnsToUpdate, values, ", ");
		String whereQuery = getRowsQuery(columnsToSearch, values, " AND ");

		String query = "UPDATE " + tableName + " SET " + updateQuery + " WHERE " + whereQuery;
		return new Pair<>(query, values.toArray());
	}

	@NonNull
	private static String getRowsQuery(@NonNull Map<String, Object> map, @NonNull List<Object> values, @NonNull String separator) {
		StringBuilder builder = new StringBuilder();
		Iterator<Entry<String, Object>> iterator = map.entrySet().iterator();
		while (iterator.hasNext()) {
			Entry<String, Object> entry = iterator.next();
			builder.append(entry.getKey());
			builder.append(" = ?");

			if (iterator.hasNext()) {
				builder.append(separator);
			}
			values.add(entry.getValue());
		}
		return builder.toString();
	}
}
