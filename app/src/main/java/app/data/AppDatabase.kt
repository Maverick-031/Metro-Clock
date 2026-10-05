package app.metroclock.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [AlarmEntity::class, WorldCityEntity::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
  abstract fun alarmDao(): AlarmDao
  abstract fun worldCityDao(): WorldCityDao

  companion object {
    @Volatile
    private var INSTANCE: AppDatabase? = null

    fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
      return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
          context.applicationContext,
          AppDatabase::class.java,
          "metro_clock_database"
        )
          .fallbackToDestructiveMigration()
          .addCallback(DatabaseCallback(scope))
          .build()
        INSTANCE = instance
        instance
      }
    }

    private class DatabaseCallback(
      private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
      override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        INSTANCE?.let { database ->
          scope.launch(Dispatchers.IO) {
            populateInitialData(database.alarmDao(), database.worldCityDao())
          }
        }
      }

      suspend fun populateInitialData(alarmDao: AlarmDao, cityDao: WorldCityDao) {
        // Pre-populate realistic alarms shown in Windows Phone screenshots
        alarmDao.insertAlarm(
          AlarmEntity(
            id = 1,
            hour = 13,
            minute = 56,
            name = "Alarm",
            isEnabled = false,
            repeatDays = 127, // every day
            soundName = "Alarm Classic",
            snoozeMinutes = 10
          )
        )
        alarmDao.insertAlarm(
          AlarmEntity(
            id = 2,
            hour = 14,
            minute = 0,
            name = "Alarm",
            isEnabled = false,
            repeatDays = 0, // only once
            soundName = "Alarm Classic",
            snoozeMinutes = 10
          )
        )
        alarmDao.insertAlarm(
          AlarmEntity(
            id = 3,
            hour = 15,
            minute = 45,
            name = "Alarm",
            isEnabled = false,
            repeatDays = 0, // only once
            soundName = "Alarm Classic",
            snoozeMinutes = 10
          )
        )

        // Pre-populate initial World Clock cities
        cityDao.insertCity(
          WorldCityEntity(
            id = 1,
            cityName = "London",
            countryName = "United Kingdom",
            timeZoneId = "Europe/London"
          )
        )
        cityDao.insertCity(
          WorldCityEntity(
            id = 2,
            cityName = "New York",
            countryName = "United States",
            timeZoneId = "America/New_York"
          )
        )
        cityDao.insertCity(
          WorldCityEntity(
            id = 3,
            cityName = "Tokyo",
            countryName = "Japan",
            timeZoneId = "Asia/Tokyo"
          )
        )
      }
    }
  }
}
