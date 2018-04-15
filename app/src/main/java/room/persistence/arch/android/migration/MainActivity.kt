package room.persistence.arch.android.migration

import android.arch.persistence.room.Database
import android.arch.persistence.room.Entity
import android.arch.persistence.room.RoomDatabase
import android.support.v7.app.AppCompatActivity
import android.os.Bundle


@Database(entities = [
    Post::class], version = 1, exportSchema = false)
abstract class RoomDataSource : RoomDatabase() {
}

@Entity
class Post(
        var title: String,
        var url: String
)

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }
}
