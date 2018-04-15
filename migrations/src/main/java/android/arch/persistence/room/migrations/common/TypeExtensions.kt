package android.arch.persistence.room.migrations.common

import android.arch.persistence.room.Ignore
import android.arch.persistence.room.migrations.ProcessorException
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import java.util.*
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement
import javax.lang.model.type.TypeMirror

/**
 * Created by sergeygolishnikov on 31/10/2017.
 */

val hashMapClassName = ClassName.get(HashMap::class.java)
val listClassName = ClassName.get(List::class.java)
val arrayListClassName = ClassName.get(ArrayList::class.java)
val arraysClassName = ClassName.get(Arrays::class.java)

@Throws(ProcessorException::class)
fun Element.asTypeElement(): TypeElement {
    if (this is TypeElement) return this
    try {
        return this.asType().asTypeElement()
    } catch (e: Throwable) {
        throw ProcessorException("Can't convert ${this.simpleName}: ${this.asType()} to TypeElement").setElement(this)
    }
}

fun TypeMirror.asTypeElement(): TypeElement {
    return MoreTypes.asTypeElement(this)
}


fun AnnotationMirror.asElement() : Element {
    return annotationType.asElement()
}



fun Element.isIgnore(): Boolean {
    return getAnnotation(Ignore::class.java) != null
}

fun Element.name(): String {
    return simpleName.toString()
}

fun Element.isNonNull(): Boolean {
    for (annotation in annotationMirrors) {
        if (annotation.asElement().asType().toString() == "android.support.annotation.NonNull")
            return true
        if (annotation.asElement().asType().toString() == "org.jetbrains.annotations.NotNull")
            return true
    }

    return asType().kind.isPrimitive
}

fun map(key: Class<*>, value: Class<*>): TypeName {
    val mapClass = ClassName.get(Map::class.java)
    return ParameterizedTypeName.get(mapClass, ClassName.get(key), ClassName.get(value))
}

fun list(parameterized: Class<*>): ParameterizedTypeName {
    return ParameterizedTypeName.get(listClassName, ClassName.get(parameterized))
}

fun iterator(parameterized: Class<*>): ParameterizedTypeName {
    return ParameterizedTypeName.get(ClassName.get(Iterator::class.java), ClassName.get(parameterized))
}