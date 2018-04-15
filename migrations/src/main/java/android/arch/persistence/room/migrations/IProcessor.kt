package android.arch.persistence.room.migrations

import android.arch.persistence.room.Database
import android.arch.persistence.room.migrations.models.RoomModel
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.TypeSpec
import java.io.IOException
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.RoundEnvironment
import javax.annotation.processing.SupportedAnnotationTypes
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement
import javax.lang.model.type.MirroredTypesException
import javax.lang.model.type.TypeMirror
import javax.tools.Diagnostic

@SupportedAnnotationTypes("android.arch.persistence.room.Entity")
open class IProcessor : AbstractProcessor() {

    override fun process(annotations: Set<TypeElement>, roundEnv: RoundEnvironment): Boolean {
        try {
            return newParse(roundEnv)
        } catch (e: ProcessorException) {
            processingEnv.messager.printMessage(Diagnostic.Kind.ERROR, e.message, e.element)
        } catch (e: Throwable) {
            throw RuntimeException(e)
        }

        return false
    }

    private fun newParse(roundEnv: RoundEnvironment): Boolean {
        val entities = roundEnv.getElementsAnnotatedWith(Database::class.java)
        val database: Element = entities.firstOrNull() ?: return false
        val databaseAnnotation = database.getAnnotation(Database::class.java)
        val models = mutableListOf<RoomModel>()
        try {
            databaseAnnotation.entities
        } catch (e: MirroredTypesException) {
            createRoomModels(e.typeMirrors, models)
        }


        createMigrations(models)

        return true
    }


    private fun createRoomModels(models: List<TypeMirror>, roomModels: MutableList<RoomModel>) {
        roomModels.addAll(RoomModelParser(models).parse())
    }

    private fun createMigrations(roomModels: MutableList<RoomModel>) {
        for (roomModel in roomModels) {
            writeClassFile(roomModel.packageName, MigrationSpec(roomModel).get())
        }
    }


    @Throws(ProcessorException::class)
    private fun writeClassFile(packageName: String,
                               classSpec: TypeSpec) {
        try {
            val javaFile = JavaFile.builder(packageName, classSpec).skipJavaLangImports(true).build()
            javaFile.writeTo(processingEnv.filer)
        } catch (e: IOException) {
            e.printStackTrace()
            throw ProcessorException("Can't generate code file %s.%s.", packageName, classSpec.name)
        }

    }
}
