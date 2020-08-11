package android.arch.persistence.room.migrations

import android.arch.persistence.room.migrations.common.asTypeElement
import android.arch.persistence.room.migrations.common.isIgnore
import android.arch.persistence.room.migrations.common.isNonNull
import android.arch.persistence.room.migrations.common.name
import android.arch.persistence.room.migrations.models.RoomColumn
import android.arch.persistence.room.migrations.models.RoomIndex
import android.arch.persistence.room.migrations.models.RoomModel
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement
import androidx.room.*
import javax.lang.model.element.VariableElement
import javax.lang.model.type.MirroredTypesException
import javax.lang.model.type.TypeMirror
import javax.lang.model.util.ElementFilter

/**
 * Created by sergeygolishnikov on 09/01/2018.
 */
class RoomModelParser(val models: List<TypeMirror>) {

    fun parse(): List<RoomModel> {
        val roomModels = mutableListOf<RoomModel>()
        for (typeModel in models) {
            val model = typeModel.asTypeElement()
            val roomModel = parseEntityAnnotation(model)
            val columns = parseColumns(model, roomModel)
            roomModel.columns = columns
            roomModels.add(roomModel)
        }

        return roomModels
    }

    private fun parseEntityAnnotation(model: TypeElement): RoomModel {
        val roomModel = RoomModel()
        val entityAnnotation = model.getAnnotation(Entity::class.java)

        roomModel.tableName = model.name()
        roomModel.element = model
        if (!entityAnnotation.tableName.isBlank()) {
            roomModel.tableName = entityAnnotation.tableName
        }

        for (index in entityAnnotation.indices) {
            val roomIndex = RoomIndex()
            roomIndex.name = "index_${roomModel.tableName}_${index.value.joinToString("_")}"
            if (index.name != "") roomIndex.name = index.name
            roomIndex.isUnique = index.unique
            roomIndex.columns = index.value.toList()
            roomModel.indices.add(roomIndex)
        }

        return roomModel
    }

    private fun parseColumns(model: TypeElement, roomModel: RoomModel, prefix: String = ""): List<RoomColumn> {
        val columns = mutableListOf<RoomColumn>()
        val fields = ElementFilter.fieldsIn(model.enclosedElements)
        for (field in fields) {
            if (field.isIgnore()) continue
            val embedded = field.getAnnotation(Embedded::class.java)
            if (embedded != null) {
                val embeddedColumns = parseColumns(field.asTypeElement(), roomModel, embedded.prefix)
                columns.addAll(embeddedColumns)
                continue
            }


            val column = parseColumnAnnotation(field, roomModel)
            column.name = "$prefix${column.name}"
            columns.add(column)
        }
        return columns
    }

    private fun parseColumnAnnotation(field: VariableElement, roomModel: RoomModel): RoomColumn {
        val column = RoomColumn()
        val columnInfoAnnotation: ColumnInfo? = field.getAnnotation(ColumnInfo::class.java)
        column.name = field.name()
        if (columnInfoAnnotation != null && columnInfoAnnotation.name != "[field-name]") {
            column.name = columnInfoAnnotation.name
        }

        if (columnInfoAnnotation?.index == true) {
            val index = RoomIndex()
            index.name = "index_${roomModel.tableName}_${column.name}"
            index.columns = listOf(column.name)
            roomModel.indices.add(index)
        }

        column.isNullable = !field.isNonNull()
        column.sqlType = toSqlType(field, roomModel)
        column.defaultValue = toSqlDefaultValue(column.sqlType)
        field.getAnnotation(PrimaryKey::class.java)?.let {
            column.isPrimary = true
            column.isAutoGenerate = it.autoGenerate
        }

        return column
    }

    private fun toSqlType(field: VariableElement, roomModel: RoomModel): String {
        var type = field.toSqlType()
        if (type == null) {
            type = tryFindConverterType(field, roomModel)
        }
        return type
    }

    private fun tryFindConverterType(field: VariableElement, roomModel: RoomModel): String {
        //return "TEXT"
        val annotation = roomModel.element.getAnnotation(TypeConverters::class.java)
                ?: throw ProcessorException("Can't detect field type for ${field.asType()} in ${roomModel.element}").setElement(field)

        try {
            annotation.value
        } catch (e: MirroredTypesException) {
            val types = e.typeMirrors
            var method: ExecutableElement? = null
            for (converter in types) {
                method = ElementFilter.methodsIn(converter.asTypeElement().enclosedElements)
                        .filter { it.parameters.size == 1 }
                        .firstOrNull { it.parameters[0].asType().toString() == field.asType().toString() }
                if (method != null) {
                    break
                }
            }
            if (method == null) {
                throw ProcessorException("Can't detect field type for ${field.asType()} in ${roomModel.element}").setElement(field)
            }

            return method.returnType.toSqlType()
                ?: throw ProcessorException("Can't detect field type for ${field.asType()} in ${roomModel.element}").setElement(field)
        }

        throw ProcessorException("Can't detect field type for ${field.asType()} in ${roomModel.element}").setElement(field)
    }
}