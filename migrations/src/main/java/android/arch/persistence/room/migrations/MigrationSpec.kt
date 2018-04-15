package android.arch.persistence.room.migrations

import android.arch.persistence.room.migrations.common.*
import android.arch.persistence.room.migrations.models.RoomColumn
import android.arch.persistence.room.migrations.models.RoomIndex
import android.arch.persistence.room.migrations.models.RoomModel
import com.squareup.javapoet.*
import java.util.*
import javax.lang.model.element.Modifier

/**
 * Created by sergeygolishnikov on 10/01/2018.
 */

private val roomMigrationType = ClassName.bestGuess("android.arch.persistence.room.migration.Migration")
private val databaseType = ClassName.bestGuess("android.arch.persistence.db.SupportSQLiteDatabase")
private val cursorType = ClassName.bestGuess("android.database.Cursor")
val nonNullAnnotationType = ClassName.bestGuess("android.support.annotation.NonNull")

fun databaseParameter(): ParameterSpec {
    return ParameterSpec
            .builder(databaseType, "database", Modifier.FINAL)
            .addAnnotation(nonNullAnnotationType)
            .build()
}


private fun String.quote(): String {
    return "`$this`"
}

private fun RoomColumn.nullable(): String {
    if (isNullable) return ""
    return " NOT NULL"
}

private fun RoomColumn.defaultValue(): String {
    if (isNullable) return ""
    if (defaultValue == "") return ""
    return " DEFAULT $defaultValue"
}

private fun RoomModel.columnNames(): String {
    return columns.joinToString { "\"${it.name}\"" }
}

private fun RoomModel.indexNames(): String {
    return indices.joinToString { "\"${it.name}\"" }
}

private fun RoomModel.quotedIndexNames(): String {
    return indices.joinToString { "'${it.name}'" }
}

private fun RoomModel.mapSize(): Int {
    return columns.size + indices.size
}

private fun RoomIndex.createIndex(tableName: String): String {
    if (isUnique) {
        return "CREATE UNIQUE INDEX IF NOT EXISTS ${name.quote()} ON ${tableName.quote()}(${columns.joinToString { it.quote() }});"
    }
    return "CREATE INDEX IF NOT EXISTS ${name.quote()} ON ${tableName.quote()}(${columns.joinToString { it.quote() }});"
}

class MigrationSpec(private val roomModel: RoomModel) {


    fun get(): TypeSpec {
        val specBuilder = TypeSpec.classBuilder("${roomModel.simpleName}Migration")
                .superclass(roomMigrationType)
                .addModifiers(Modifier.FINAL, Modifier.PUBLIC)
                .addField(map(String::class.java, String::class.java), "tableMeta", Modifier.PRIVATE, Modifier.FINAL)
                .addMethod(createConstructor())
                .addMethod(createTableMethod())
                .addMethod(migrateMethod())
                .addMethod(addColumnsMethod())

        if (roomModel.indices.isNotEmpty()) {
            specBuilder.addMethod(addIndexesMethod())
        }

        return specBuilder.build()
    }

    private fun createConstructor(): MethodSpec {
        val builder = MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC)
        builder.addParameter(TypeName.INT, "startVersion", Modifier.FINAL)
        builder.addParameter(TypeName.INT, "endVersion", Modifier.FINAL)
        builder.addStatement("super(startVersion, endVersion)")
        builder.addStatement("tableMeta = new \$T<>(${roomModel.mapSize()})", hashMapClassName)


        for (column in roomModel.columns) {
            val stringBuilder = StringBuilder()
            stringBuilder.append("ALTER TABLE ").append(roomModel.tableName.quote())
            stringBuilder.append(" ADD COLUMN ").append(column.name.quote()).append(" ")
            stringBuilder.append(column.sqlType)
            stringBuilder.append(column.nullable())
            stringBuilder.append(column.defaultValue())
            builder.addStatement("tableMeta.put(\$S, \$S)", column.name, stringBuilder.toString())
        }

        for (index in roomModel.indices) {
            builder.addStatement("tableMeta.put(\$S, \$S)", index.name, index.createIndex(roomModel.tableName))
        }

        return builder.build()
    }

    private fun createTableMethod(): MethodSpec {
        val columns = roomModel.columns.joinToString(", ", transform = ::column)
        val builder = MethodSpec.methodBuilder("createTable").addModifiers(Modifier.FINAL, Modifier.PUBLIC)
                .addParameter(databaseParameter())
                .addStatement("database.execSQL(\"DROP TABLE IF EXISTS `${roomModel.tableName}`;\")")
                .addStatement("database.execSQL(\"CREATE TABLE IF NOT EXISTS `${roomModel.tableName}`($columns);\")")

        return builder.build()
    }

    private fun migrateMethod(): MethodSpec {
        val builder = MethodSpec.methodBuilder("migrate").addModifiers(Modifier.FINAL, Modifier.PUBLIC)
                .addAnnotation(Override::class.java)
                .addParameter(databaseParameter())
                .addCode(existsTable())

        return builder.build()
    }

    private fun existsTable() = CodeBlock.builder().apply {
        val existsQuery = "SELECT name FROM sqlite_master WHERE type='table' AND name='${roomModel.tableName}';"
        addStatement("\$T cursor = database.query(\"$existsQuery\")", cursorType)
        beginControlFlow("try")

        beginControlFlow("if (cursor.moveToFirst())")
        addStatement("addColumns(database)")
        nextControlFlow("else")
        addStatement("createTable(database)")
        endControlFlow()

        nextControlFlow("finally")
        addStatement("cursor.close()")
        endControlFlow()
        if (roomModel.indices.isNotEmpty()) addStatement("addIndexes(database)")
    }.build()

    private fun addColumnsMethod(): MethodSpec {
        val builder = MethodSpec.methodBuilder("addColumns")
                .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                .addParameter(databaseParameter())
                .addStatement("\$T tableInfo = database.query(\"pragma table_info(`\$N`)\")", cursorType, roomModel.tableName)
                .addCode(addColumnsCodeBlock())
        return builder.build()
    }

    private fun addColumnsCodeBlock(): CodeBlock {
        val builder = CodeBlock.builder()

        builder.addStatement("\$T columns = new \$T<>(\$T.asList(\$L))",
                list(String::class.java),
                arrayListClassName,
                arraysClassName,
                roomModel.columnNames())


        builder.addStatement("\$T currentColumns = new \$T<>()", list(String::class.java), arrayListClassName)
        builder.beginControlFlow("try")
        builder.beginControlFlow("while(tableInfo.moveToNext())")
        builder.addStatement("currentColumns.add(tableInfo.getString(1))")
        builder.endControlFlow()
        builder.nextControlFlow("finally")
        builder.addStatement("tableInfo.close()")
        builder.endControlFlow()

        builder.addStatement("\$T iterator = columns.iterator()", iterator(String::class.java))

        builder.beginControlFlow("while(iterator.hasNext())")
        builder.addStatement("if (currentColumns.remove(iterator.next())) iterator.remove()")
        builder.endControlFlow()

        builder.beginControlFlow("for(String column : columns)")
        builder.addStatement("database.execSQL(tableMeta.get(column))")
        builder.endControlFlow()

        builder.beginControlFlow("if (!currentColumns.isEmpty())")
        val random = Random().nextInt(19999999)
        val columns = roomModel.columns.joinToString(", ", transform = ::column)
        val names = roomModel.columns.joinToString(", ", transform = { it.name.quote() })
        val nullableNames = roomModel.columns.joinToString(", ", transform = ::nullableColumn)
        builder.addStatement("database.execSQL(\"CREATE TABLE `${roomModel.tableName}_$random`($columns);\")")
        builder.addStatement("database.execSQL(\"INSERT INTO `${roomModel.tableName}_$random`($names) SELECT $nullableNames FROM ${roomModel.tableName.quote()};\")")
        builder.addStatement("database.execSQL(\"DROP TABLE ${roomModel.tableName.quote()}\")")
        builder.addStatement("database.execSQL(\"ALTER TABLE `${roomModel.tableName}_$random` RENAME TO ${roomModel.tableName.quote()}\")")
        builder.endControlFlow()

        return builder.build()
    }

    private fun nullableColumn(column: RoomColumn) = buildString {
        if (!column.isNullable && !column.isPrimary) append("ifnull(")
        append(column.name.quote())
        if (!column.isNullable && !column.isPrimary) append(", ${column.defaultValue})")
    }


    private fun column(column: RoomColumn) = buildString {
        append(column.name.quote())
        append(" ${column.sqlType}")
        if (column.isPrimary) {
            append(" PRIMARY KEY")
        }

        if (column.isAutoGenerate) {
            append(" AUTOINCREMENT")
        }
        append(column.nullable())
        if (!column.isPrimary) {
            append(column.defaultValue())
        }
    }

    private fun addIndexesMethod(): MethodSpec {
        val builder = MethodSpec.methodBuilder("addIndexes")
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addParameter(databaseParameter())

        builder.addStatement("\$T tableInfo = database.query(\"SELECT tbl_name, name FROM sqlite_master WHERE type = 'index' AND tbl_name = '\$N'\")", cursorType, roomModel.tableName)

        builder.addStatement("\$T indices = new \$T<>(\$T.asList(\$L))",
                list(String::class.java),
                arrayListClassName,
                arraysClassName,
                roomModel.indexNames())

        builder.addComment("check indices that already was added to database, remove them from indices list")
        builder.addStatement("\$T currentIndices = new \$T<>()", list(String::class.java), arrayListClassName)
        builder.beginControlFlow("try")
        builder.beginControlFlow("while(tableInfo.moveToNext())")
        builder.addStatement("currentIndices.add(tableInfo.getString(1))")
        builder.endControlFlow()
        builder.nextControlFlow("finally")
        builder.addStatement("tableInfo.close()")
        builder.endControlFlow()

        builder.addStatement("\$T iterator = indices.iterator()", iterator(String::class.java))

        builder.beginControlFlow("while(iterator.hasNext())")
        builder.addStatement("if (currentIndices.remove(iterator.next())) iterator.remove()")
        builder.endControlFlow()

        builder.addComment("add new indices to database for current table")
        builder.beginControlFlow("if (!indices.isEmpty())")
        roomModel.indices
                .filter { it.isUnique }
                .forEach {
                    builder.addComment("delete all duplicates from \$N for [\$N] ", roomModel.tableName.quote(), it.columns.joinToString())
                    builder.addStatement("database.execSQL(\"DELETE FROM \$N WHERE rowid NOT IN (SELECT MIN(rowId) FROM \$N GROUP BY \$N)\")", roomModel.tableName.quote(), roomModel.tableName.quote(), it.columns.joinToString())
                }
        builder.beginControlFlow("for (String index: indices)")
        builder.addStatement("database.execSQL(tableMeta.get(index))")
        builder.endControlFlow()
        builder.endControlFlow()

        builder.beginControlFlow("for (String index: currentIndices)")
        builder.addStatement("if (index.startsWith(\"sqlite_autoindex\")) continue")
        builder.addStatement("database.execSQL(String.format(\"DROP INDEX IF EXISTS %s\", index))")
        builder.endControlFlow()

        return builder.build()
    }
}