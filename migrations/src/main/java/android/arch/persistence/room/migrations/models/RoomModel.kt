package android.arch.persistence.room.migrations.models

import android.arch.persistence.room.migrations.common.asTypeElement
import com.squareup.javapoet.ClassName
import javax.lang.model.element.Element

/**
 * Created by sergeygolishnikov on 09/01/2018.
 */

class RoomModel {
    lateinit var tableName: String
    lateinit var element: Element
    var simpleName: String = ""
        get() = element.simpleName.toString()
    var packageName: String = ""
        get() = ClassName.get(element.asTypeElement()).packageName()
    var columns = emptyList<RoomColumn>()
    var indices = mutableListOf<RoomIndex>()

    override fun toString(): String {
        return "RoomModel(tableName='$tableName')"
    }
}