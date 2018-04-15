package android.arch.persistence.room.migrations

import com.squareup.javapoet.TypeName
import javax.lang.model.element.Element
import javax.lang.model.type.TypeMirror

/**
 * Created by sergeygolishnikov on 09/01/2018.
 */

fun Element.toSqlType(): String? {
    return asType().toSqlType()
}

fun TypeMirror.toSqlType(): String? {
    val type = TypeName.get(this).box()
    when(type.toString()) {
        "java.lang.String" -> return "TEXT"

        "java.lang.Double",
        "java.lang.Float" -> return "REAL"

        "java.lang.Integer",
        "java.lang.Boolean",
        "java.lang.Long" -> return "INTEGER"
    }
    return null
}

fun toSqlDefaultValue(sqlType: String): String {
    return when(sqlType) {
        "INTEGER" -> "0"
        "REAL" -> return "0.0"
        else -> "''"
    }
}