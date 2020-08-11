package room.persistence.arch.android.migration

import android.app.Activity
import android.os.Bundle
import androidx.room.Database
import androidx.room.Entity
import androidx.room.RoomDatabase


@Database(entities = [
    Post::class], version = 1, exportSchema = false)
abstract class RoomDataSource : RoomDatabase() {
}

@Entity
class Post(
        var title: String,
        var url: String
)

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }
}
