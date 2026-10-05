package app.metroclock.data

import kotlinx.coroutines.flow.Flow

class AlarmRepository(private val alarmDao: AlarmDao) {
  val allAlarms: Flow<List<AlarmEntity>> = alarmDao.getAllAlarms()

  suspend fun getAlarmById(id: Long): AlarmEntity? = alarmDao.getAlarmById(id)

  suspend fun getEnabledAlarms(): List<AlarmEntity> = alarmDao.getEnabledAlarms()

  suspend fun insertAlarm(alarm: AlarmEntity): Long = alarmDao.insertAlarm(alarm)

  suspend fun updateAlarm(alarm: AlarmEntity) = alarmDao.updateAlarm(alarm)

  suspend fun deleteAlarm(alarm: AlarmEntity) = alarmDao.deleteAlarm(alarm)

  suspend fun deleteAlarmById(id: Long) = alarmDao.deleteAlarmById(id)
}
