package me.liuwj.ktorm.support.sqlite

import me.liuwj.ktorm.database.Database
import me.liuwj.ktorm.database.SqlDialect
import me.liuwj.ktorm.database.prepareStatement
import me.liuwj.ktorm.database.useConnection
import me.liuwj.ktorm.expression.SqlExpression
import me.liuwj.ktorm.expression.SqlFormatter
import me.liuwj.ktorm.schema.Column
import me.liuwj.ktorm.schema.IntSqlType
import me.liuwj.ktorm.schema.SqlType
import java.sql.PreparedStatement

/**
 * [SqlDialect] implementation for SQLite databases on plaforms with limited JDBC functionality. In particular
 * Android and Robovm do not support calling prepareStatement with flags.
 */
open class SimpleSQLiteDialect : SQLiteDialect() {

    /**
     * Call prepareStatement without requesting generated keys. Only the primary key will be auto-generated, we will
     * pick that up with [generatedKey]
     */
    override fun <T> prepareStatement(expression: SqlExpression, autoGeneratedKeys: Boolean, func: (PreparedStatement) -> T): T {
        return expression.prepareStatement(false, func)
    }

    /**
     * Every SQLite database table has a unique ROWID. This maps to an integer primary key if present. We get to this
     * with an SQLite specific select statement. Although this function has a generic type, the only auto-generated
     * primary key type supported is Int.
     */
    override fun <T : Any> generatedKey(statement: PreparedStatement, primaryKey: Column<T>?): T {
        return Database.global.useConnection { connection ->
            val resultSet = connection.prepareStatement("select last_insert_rowid();").executeQuery()
            val sqlType = primaryKey?.sqlType ?: IntSqlType

            if(resultSet.next()) {
                @Suppress("UNCHECKED_CAST")
                sqlType.getResult(resultSet, 1) as T
            } else {
                error("No generated ROWID returned from query.")
            }
        }
    }
}