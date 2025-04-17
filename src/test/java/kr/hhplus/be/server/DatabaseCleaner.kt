package kr.hhplus.be.server

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import javax.sql.DataSource

@Component
class MySqlDatabaseCleaner(
    @Autowired private val dataSource: DataSource
) {

    fun clean() {
        val connection = dataSource.connection
        val metadata = connection.metaData

        val dbName = connection.catalog

        connection.createStatement().use { stmt ->
            stmt.execute("SET FOREIGN_KEY_CHECKS = 0")
        }

        val tableNames = mutableListOf<String>()
        metadata.getTables(dbName, null, "%", arrayOf("TABLE")).use { rs ->
            while (rs.next()) {
                tableNames.add(rs.getString("TABLE_NAME"))
            }
        }

        connection.createStatement().use { stmt ->
            tableNames.forEach { table ->
                stmt.execute("TRUNCATE TABLE `$table`")
            }
        }

        connection.createStatement().use { stmt ->
            stmt.execute("SET FOREIGN_KEY_CHECKS = 1")
        }

        connection.close() // JDBC 연결 닫기
    }
}
