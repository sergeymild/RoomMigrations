## Declaring Dependencies

###### Installation
Add it in your root build.gradle at the end of repositories:
```
allprojects {
  repositories {
    maven { url 'https://jitpack.io' }
  }
}

```
Add this dependencies to your project's `build.gradle`.
```
compile 'com.github.sergeymild.ioc:core:1.1'
kapt 'com.github.sergeymild.ioc:processor:1.1'
```

Migrations find all models declared in `@Database` annotation and for all of these it will generate `ModelClassNameMigration` class

```
@Database(entities = [Post::class], version = 2, exportSchema = false)
abstract class RoomDataSource : RoomDatabase() {}
```

Migrations  will generate `PostMigration` class. That you'll need pass to:

```
return Room.databaseBuilder(applicationContext, RoomDataSource::class.java, "database.db")
            .addMigrations(PostMigration(1, 2))
            .build()
```

If you need migrate on more time with same model, just add `MigrationClass` one more time but with different version.

```
return Room.databaseBuilder(applicationContext, RoomDataSource::class.java, "database.db")
            .addMigrations(PostMigration(3, 4))
            .build()
```

That's it. Migration automatically add new `columns`, remove deleted from model `columns`, add new `indices` and remove deleted from model `indices`.