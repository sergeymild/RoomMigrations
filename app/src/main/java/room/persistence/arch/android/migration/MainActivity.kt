package room.persistence.arch.android.migration

import android.app.Activity
import android.os.Bundle
import androidx.room.*


@Database(
        entities = [AccountEntity::class,
            ChatRoomEntity::class,
            ChatParticipantEntity::class,
            ChatRoomToParticipantEntity::class,
            ChatMessageEntity::class,
            ContactEntity::class],
        version = 1
)
@TypeConverters(ChatTypeConverters::class)
abstract class RoomDataSource : RoomDatabase() {
}

@Entity(tableName = "rooms")
data class ChatRoomEntity(
        @PrimaryKey
        val chatId: String,
        val type: String,
        val chatRoomName: String,
        val lastReadId: Long,
        val isHistoryLoaded: Boolean
)

@Entity(tableName = "chat_participants")
data class ChatParticipantEntity(
        @PrimaryKey
        val participantId: Long,
        val name: String,
        val surname: String,
        val avatarSrc: String
)

@Entity(
        tableName = "room_participants",
        primaryKeys = ["chatId", "participantId"],
        foreignKeys = [
            ForeignKey(
                    entity = ChatRoomEntity::class,
                    parentColumns = ["chatId"],
                    childColumns = ["chatId"]
            ),
            ForeignKey(
                    entity = ChatParticipantEntity::class,
                    parentColumns = ["participantId"],
                    childColumns = ["participantId"]
            )
        ]
)
data class ChatRoomToParticipantEntity(
        val chatId: String,
        val participantId: Long
)

@Entity(tableName = "chat_messages", indices = [Index("localId"), Index("chatId")])
data class ChatMessageEntity(
        @PrimaryKey
        val messageId: Long,
        val localId: String?,
        val userId: Long,
        val chatId: String,
        val type: String,
        val text: String,
        val sentAt: Long,
        val status: MessageStatus
)

enum class MessageStatus {
    SENT, IN_PROGRESS, RETRYING, ERROR
}

class ChatTypeConverters {

    @TypeConverter
    fun fromMessageStatus(value: MessageStatus?): String? {
        if (value == null) {
            return null
        }

        return value.name
    }

    @TypeConverter
    fun toMessageStatus(value: String?): MessageStatus? {
        if (value == null) {
            return null
        }

        return MessageStatus.valueOf(value)
    }
}

@Entity(tableName = "contacts")
data class ContactEntity(
        @PrimaryKey
        val id: Long,
        val email: String,
        val name: String,
        val surname: String,
        val avatarSrc: String,
        val company: String,
        val position: String,
        val about: String,
        val phone: String,
        val createdAt: Long
)

@Entity(tableName = "profile")
data class AccountEntity(
        @PrimaryKey val id: Long,
        @ColumnInfo val email: String,
        @ColumnInfo val name: String,
        @ColumnInfo val surname: String,
        @ColumnInfo val position: String,
        @ColumnInfo val about: String,
        @ColumnInfo val createdAt: Long,
        @ColumnInfo val avatarSrc: String,
        @ColumnInfo val jabberPassword: String,
        @ColumnInfo val jabberServer: String
) {

}

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }
}
