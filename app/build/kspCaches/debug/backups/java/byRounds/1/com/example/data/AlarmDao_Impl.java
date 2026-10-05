package com.example.data;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class AlarmDao_Impl implements AlarmDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<AlarmEntity> __insertionAdapterOfAlarmEntity;

  private final EntityDeletionOrUpdateAdapter<AlarmEntity> __deletionAdapterOfAlarmEntity;

  private final EntityDeletionOrUpdateAdapter<AlarmEntity> __updateAdapterOfAlarmEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeleteAlarmById;

  public AlarmDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfAlarmEntity = new EntityInsertionAdapter<AlarmEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `alarms` (`id`,`hour`,`minute`,`name`,`isEnabled`,`repeatDays`,`soundName`,`soundUri`,`snoozeMinutes`,`lastTriggeredEpoch`,`skipIfCalendarEvent`,`smartSkipLocation`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final AlarmEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getHour());
        statement.bindLong(3, entity.getMinute());
        statement.bindString(4, entity.getName());
        final int _tmp = entity.isEnabled() ? 1 : 0;
        statement.bindLong(5, _tmp);
        statement.bindLong(6, entity.getRepeatDays());
        statement.bindString(7, entity.getSoundName());
        statement.bindString(8, entity.getSoundUri());
        statement.bindLong(9, entity.getSnoozeMinutes());
        statement.bindLong(10, entity.getLastTriggeredEpoch());
        final int _tmp_1 = entity.getSkipIfCalendarEvent() ? 1 : 0;
        statement.bindLong(11, _tmp_1);
        final int _tmp_2 = entity.getSmartSkipLocation() ? 1 : 0;
        statement.bindLong(12, _tmp_2);
      }
    };
    this.__deletionAdapterOfAlarmEntity = new EntityDeletionOrUpdateAdapter<AlarmEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `alarms` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final AlarmEntity entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__updateAdapterOfAlarmEntity = new EntityDeletionOrUpdateAdapter<AlarmEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `alarms` SET `id` = ?,`hour` = ?,`minute` = ?,`name` = ?,`isEnabled` = ?,`repeatDays` = ?,`soundName` = ?,`soundUri` = ?,`snoozeMinutes` = ?,`lastTriggeredEpoch` = ?,`skipIfCalendarEvent` = ?,`smartSkipLocation` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final AlarmEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getHour());
        statement.bindLong(3, entity.getMinute());
        statement.bindString(4, entity.getName());
        final int _tmp = entity.isEnabled() ? 1 : 0;
        statement.bindLong(5, _tmp);
        statement.bindLong(6, entity.getRepeatDays());
        statement.bindString(7, entity.getSoundName());
        statement.bindString(8, entity.getSoundUri());
        statement.bindLong(9, entity.getSnoozeMinutes());
        statement.bindLong(10, entity.getLastTriggeredEpoch());
        final int _tmp_1 = entity.getSkipIfCalendarEvent() ? 1 : 0;
        statement.bindLong(11, _tmp_1);
        final int _tmp_2 = entity.getSmartSkipLocation() ? 1 : 0;
        statement.bindLong(12, _tmp_2);
        statement.bindLong(13, entity.getId());
      }
    };
    this.__preparedStmtOfDeleteAlarmById = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM alarms WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insertAlarm(final AlarmEntity alarm, final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfAlarmEntity.insertAndReturnId(alarm);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteAlarm(final AlarmEntity alarm, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfAlarmEntity.handle(alarm);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateAlarm(final AlarmEntity alarm, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfAlarmEntity.handle(alarm);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteAlarmById(final long id, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteAlarmById.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, id);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteAlarmById.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<AlarmEntity>> getAllAlarms() {
    final String _sql = "SELECT * FROM alarms ORDER BY hour ASC, minute ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"alarms"}, new Callable<List<AlarmEntity>>() {
      @Override
      @NonNull
      public List<AlarmEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfHour = CursorUtil.getColumnIndexOrThrow(_cursor, "hour");
          final int _cursorIndexOfMinute = CursorUtil.getColumnIndexOrThrow(_cursor, "minute");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfIsEnabled = CursorUtil.getColumnIndexOrThrow(_cursor, "isEnabled");
          final int _cursorIndexOfRepeatDays = CursorUtil.getColumnIndexOrThrow(_cursor, "repeatDays");
          final int _cursorIndexOfSoundName = CursorUtil.getColumnIndexOrThrow(_cursor, "soundName");
          final int _cursorIndexOfSoundUri = CursorUtil.getColumnIndexOrThrow(_cursor, "soundUri");
          final int _cursorIndexOfSnoozeMinutes = CursorUtil.getColumnIndexOrThrow(_cursor, "snoozeMinutes");
          final int _cursorIndexOfLastTriggeredEpoch = CursorUtil.getColumnIndexOrThrow(_cursor, "lastTriggeredEpoch");
          final int _cursorIndexOfSkipIfCalendarEvent = CursorUtil.getColumnIndexOrThrow(_cursor, "skipIfCalendarEvent");
          final int _cursorIndexOfSmartSkipLocation = CursorUtil.getColumnIndexOrThrow(_cursor, "smartSkipLocation");
          final List<AlarmEntity> _result = new ArrayList<AlarmEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final AlarmEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final int _tmpHour;
            _tmpHour = _cursor.getInt(_cursorIndexOfHour);
            final int _tmpMinute;
            _tmpMinute = _cursor.getInt(_cursorIndexOfMinute);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final boolean _tmpIsEnabled;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsEnabled);
            _tmpIsEnabled = _tmp != 0;
            final int _tmpRepeatDays;
            _tmpRepeatDays = _cursor.getInt(_cursorIndexOfRepeatDays);
            final String _tmpSoundName;
            _tmpSoundName = _cursor.getString(_cursorIndexOfSoundName);
            final String _tmpSoundUri;
            _tmpSoundUri = _cursor.getString(_cursorIndexOfSoundUri);
            final int _tmpSnoozeMinutes;
            _tmpSnoozeMinutes = _cursor.getInt(_cursorIndexOfSnoozeMinutes);
            final long _tmpLastTriggeredEpoch;
            _tmpLastTriggeredEpoch = _cursor.getLong(_cursorIndexOfLastTriggeredEpoch);
            final boolean _tmpSkipIfCalendarEvent;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfSkipIfCalendarEvent);
            _tmpSkipIfCalendarEvent = _tmp_1 != 0;
            final boolean _tmpSmartSkipLocation;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfSmartSkipLocation);
            _tmpSmartSkipLocation = _tmp_2 != 0;
            _item = new AlarmEntity(_tmpId,_tmpHour,_tmpMinute,_tmpName,_tmpIsEnabled,_tmpRepeatDays,_tmpSoundName,_tmpSoundUri,_tmpSnoozeMinutes,_tmpLastTriggeredEpoch,_tmpSkipIfCalendarEvent,_tmpSmartSkipLocation);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getEnabledAlarms(final Continuation<? super List<AlarmEntity>> $completion) {
    final String _sql = "SELECT * FROM alarms WHERE isEnabled = 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<AlarmEntity>>() {
      @Override
      @NonNull
      public List<AlarmEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfHour = CursorUtil.getColumnIndexOrThrow(_cursor, "hour");
          final int _cursorIndexOfMinute = CursorUtil.getColumnIndexOrThrow(_cursor, "minute");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfIsEnabled = CursorUtil.getColumnIndexOrThrow(_cursor, "isEnabled");
          final int _cursorIndexOfRepeatDays = CursorUtil.getColumnIndexOrThrow(_cursor, "repeatDays");
          final int _cursorIndexOfSoundName = CursorUtil.getColumnIndexOrThrow(_cursor, "soundName");
          final int _cursorIndexOfSoundUri = CursorUtil.getColumnIndexOrThrow(_cursor, "soundUri");
          final int _cursorIndexOfSnoozeMinutes = CursorUtil.getColumnIndexOrThrow(_cursor, "snoozeMinutes");
          final int _cursorIndexOfLastTriggeredEpoch = CursorUtil.getColumnIndexOrThrow(_cursor, "lastTriggeredEpoch");
          final int _cursorIndexOfSkipIfCalendarEvent = CursorUtil.getColumnIndexOrThrow(_cursor, "skipIfCalendarEvent");
          final int _cursorIndexOfSmartSkipLocation = CursorUtil.getColumnIndexOrThrow(_cursor, "smartSkipLocation");
          final List<AlarmEntity> _result = new ArrayList<AlarmEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final AlarmEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final int _tmpHour;
            _tmpHour = _cursor.getInt(_cursorIndexOfHour);
            final int _tmpMinute;
            _tmpMinute = _cursor.getInt(_cursorIndexOfMinute);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final boolean _tmpIsEnabled;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsEnabled);
            _tmpIsEnabled = _tmp != 0;
            final int _tmpRepeatDays;
            _tmpRepeatDays = _cursor.getInt(_cursorIndexOfRepeatDays);
            final String _tmpSoundName;
            _tmpSoundName = _cursor.getString(_cursorIndexOfSoundName);
            final String _tmpSoundUri;
            _tmpSoundUri = _cursor.getString(_cursorIndexOfSoundUri);
            final int _tmpSnoozeMinutes;
            _tmpSnoozeMinutes = _cursor.getInt(_cursorIndexOfSnoozeMinutes);
            final long _tmpLastTriggeredEpoch;
            _tmpLastTriggeredEpoch = _cursor.getLong(_cursorIndexOfLastTriggeredEpoch);
            final boolean _tmpSkipIfCalendarEvent;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfSkipIfCalendarEvent);
            _tmpSkipIfCalendarEvent = _tmp_1 != 0;
            final boolean _tmpSmartSkipLocation;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfSmartSkipLocation);
            _tmpSmartSkipLocation = _tmp_2 != 0;
            _item = new AlarmEntity(_tmpId,_tmpHour,_tmpMinute,_tmpName,_tmpIsEnabled,_tmpRepeatDays,_tmpSoundName,_tmpSoundUri,_tmpSnoozeMinutes,_tmpLastTriggeredEpoch,_tmpSkipIfCalendarEvent,_tmpSmartSkipLocation);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getAlarmById(final long id, final Continuation<? super AlarmEntity> $completion) {
    final String _sql = "SELECT * FROM alarms WHERE id = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<AlarmEntity>() {
      @Override
      @Nullable
      public AlarmEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfHour = CursorUtil.getColumnIndexOrThrow(_cursor, "hour");
          final int _cursorIndexOfMinute = CursorUtil.getColumnIndexOrThrow(_cursor, "minute");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfIsEnabled = CursorUtil.getColumnIndexOrThrow(_cursor, "isEnabled");
          final int _cursorIndexOfRepeatDays = CursorUtil.getColumnIndexOrThrow(_cursor, "repeatDays");
          final int _cursorIndexOfSoundName = CursorUtil.getColumnIndexOrThrow(_cursor, "soundName");
          final int _cursorIndexOfSoundUri = CursorUtil.getColumnIndexOrThrow(_cursor, "soundUri");
          final int _cursorIndexOfSnoozeMinutes = CursorUtil.getColumnIndexOrThrow(_cursor, "snoozeMinutes");
          final int _cursorIndexOfLastTriggeredEpoch = CursorUtil.getColumnIndexOrThrow(_cursor, "lastTriggeredEpoch");
          final int _cursorIndexOfSkipIfCalendarEvent = CursorUtil.getColumnIndexOrThrow(_cursor, "skipIfCalendarEvent");
          final int _cursorIndexOfSmartSkipLocation = CursorUtil.getColumnIndexOrThrow(_cursor, "smartSkipLocation");
          final AlarmEntity _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final int _tmpHour;
            _tmpHour = _cursor.getInt(_cursorIndexOfHour);
            final int _tmpMinute;
            _tmpMinute = _cursor.getInt(_cursorIndexOfMinute);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final boolean _tmpIsEnabled;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsEnabled);
            _tmpIsEnabled = _tmp != 0;
            final int _tmpRepeatDays;
            _tmpRepeatDays = _cursor.getInt(_cursorIndexOfRepeatDays);
            final String _tmpSoundName;
            _tmpSoundName = _cursor.getString(_cursorIndexOfSoundName);
            final String _tmpSoundUri;
            _tmpSoundUri = _cursor.getString(_cursorIndexOfSoundUri);
            final int _tmpSnoozeMinutes;
            _tmpSnoozeMinutes = _cursor.getInt(_cursorIndexOfSnoozeMinutes);
            final long _tmpLastTriggeredEpoch;
            _tmpLastTriggeredEpoch = _cursor.getLong(_cursorIndexOfLastTriggeredEpoch);
            final boolean _tmpSkipIfCalendarEvent;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfSkipIfCalendarEvent);
            _tmpSkipIfCalendarEvent = _tmp_1 != 0;
            final boolean _tmpSmartSkipLocation;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfSmartSkipLocation);
            _tmpSmartSkipLocation = _tmp_2 != 0;
            _result = new AlarmEntity(_tmpId,_tmpHour,_tmpMinute,_tmpName,_tmpIsEnabled,_tmpRepeatDays,_tmpSoundName,_tmpSoundUri,_tmpSnoozeMinutes,_tmpLastTriggeredEpoch,_tmpSkipIfCalendarEvent,_tmpSmartSkipLocation);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
