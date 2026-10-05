package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.TimeZone

@Entity(tableName = "world_cities")
data class WorldCityEntity(
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0,
  val cityName: String,
  val countryName: String,
  val timeZoneId: String
) {
  fun getFormattedTime(): String {
    return try {
      val zone = ZoneId.of(timeZoneId)
      val zdt = ZonedDateTime.now(zone)
      val formatter = DateTimeFormatter.ofPattern("h:mm a", Locale.US)
      zdt.format(formatter)
    } catch (e: Exception) {
      "12:00 PM"
    }
  }

  fun getTimeDifference(): String {
    return try {
      val localZone = ZoneId.systemDefault()
      val targetZone = ZoneId.of(timeZoneId)
      val now = Instant.now()
      val localOffsetSec = localZone.rules.getOffset(now).totalSeconds
      val targetOffsetSec = targetZone.rules.getOffset(now).totalSeconds
      val diffHours = (targetOffsetSec - localOffsetSec) / 3600
      val diffMins = ((targetOffsetSec - localOffsetSec) % 3600) / 60

      val sign = if (diffHours > 0 || (diffHours == 0 && diffMins > 0)) "+" else if (diffHours < 0 || diffMins < 0) "-" else ""
      val absHours = Math.abs(diffHours)
      val absMins = Math.abs(diffMins)

      if (absHours == 0 && absMins == 0) {
        "Same time"
      } else if (absMins > 0) {
        "$sign$absHours hrs $absMins mins"
      } else {
        val hrStr = if (absHours == 1) "hr" else "hours"
        "$sign$absHours $hrStr"
      }
    } catch (e: Exception) {
      ""
    }
  }
}

@Dao
interface WorldCityDao {
  @Query("SELECT * FROM world_cities ORDER BY id ASC")
  fun getAllCities(): Flow<List<WorldCityEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertCity(city: WorldCityEntity): Long

  @Delete
  suspend fun deleteCity(city: WorldCityEntity)

  @Query("DELETE FROM world_cities WHERE id = :id")
  suspend fun deleteCityById(id: Long)
}
