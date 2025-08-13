package net.osmand.shared.util

object DbUtils {

	fun createDbInsertQuery(tableName: String, rowKeys: Set<String>): String {
		val keys = StringBuilder()
		val values = StringBuilder()
		val split = ", "
		val iterator = rowKeys.iterator()

		while (iterator.hasNext()) {
			keys.append(iterator.next())
			values.append("?")
			if (iterator.hasNext()) {
				keys.append(split)
				values.append(split)
			}
		}

		return "INSERT INTO $tableName ($keys) VALUES ($values)"
	}

	fun createDbUpdateQuery(
		tableName: String,
		columnsToUpdate: Map<String, Any?>,
		columnsToSearch: Map<String, Any?>
	): Pair<String, Array<Any?>> {
		val values = mutableListOf<Any?>()
		val updateQuery = getRowsQuery(columnsToUpdate, values, ", ")
		val whereQuery = getRowsQuery(columnsToSearch, values, " AND ")

		val query = "UPDATE $tableName SET $updateQuery WHERE $whereQuery"
		return Pair(query, values.toTypedArray())
	}

	private fun getRowsQuery(map: Map<String, Any?>, values: MutableList<Any?>, separator: String): String {
		val builder = StringBuilder()
		val iterator = map.entries.iterator()

		while (iterator.hasNext()) {
			val entry = iterator.next()
			builder.append(entry.key)
			builder.append(" = ?")

			if (iterator.hasNext()) {
				builder.append(separator)
			}
			values.add(entry.value)
		}

		return builder.toString()
	}
}
