package android.arch.persistence.room.migrations.models

/**
 * Created by sergeygolishnikov on 09/01/2018.
 */

class RoomIndex {
    lateinit var name: String
    var columns = emptyList<String>()
    var isUnique = false
}

class RoomColumn {
    lateinit var name: String
    var isNullable: Boolean = false
    var isPrimary: Boolean = false
    var isAutoGenerate: Boolean = false
    var defaultValue: String = ""
    lateinit var sqlType: String
    override fun toString(): String {
        return "RoomColumn(name='$name', isNullable=$isNullable, isPrimary=$isPrimary, isAutoGenerate=$isAutoGenerate, sqlType='$sqlType')"
    }
}