import com.intellij.database.model.DasTable
import com.intellij.database.util.DasUtil

data class DbContextActionProvider(val dasTables: List<DasTable>) {
    /**
     * Retrieves the columns of the specified tables.
     *
     * @param tables A list of table names to retrieve the columns from.
     * @return A list of column names from the specified tables.
     */
    fun getTableColumns(tables: List<String>): List<String> {
        return dasTables.mapNotNull { tableName ->
            if (tables.contains(tableName.name)) {
                val columns = DasUtil.getColumns(tableName).map {
                    "${it.name}: ${it.dataType.typeName}"
                }.joinToString(", ")

                "TableName: ${tableName.name}, Columns: $columns"
            } else {
                null
            }
        }
    }
}