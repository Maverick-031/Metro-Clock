package com.example.data;

import android.database.Cursor;
import androidx.annotation.NonNull;
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
public final class WorldCityDao_Impl implements WorldCityDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<WorldCityEntity> __insertionAdapterOfWorldCityEntity;

  private final EntityDeletionOrUpdateAdapter<WorldCityEntity> __deletionAdapterOfWorldCityEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeleteCityById;

  public WorldCityDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfWorldCityEntity = new EntityInsertionAdapter<WorldCityEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `world_cities` (`id`,`cityName`,`countryName`,`timeZoneId`) VALUES (nullif(?, 0),?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final WorldCityEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getCityName());
        statement.bindString(3, entity.getCountryName());
        statement.bindString(4, entity.getTimeZoneId());
      }
    };
    this.__deletionAdapterOfWorldCityEntity = new EntityDeletionOrUpdateAdapter<WorldCityEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `world_cities` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final WorldCityEntity entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__preparedStmtOfDeleteCityById = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM world_cities WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insertCity(final WorldCityEntity city,
      final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfWorldCityEntity.insertAndReturnId(city);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteCity(final WorldCityEntity city,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfWorldCityEntity.handle(city);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteCityById(final long id, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteCityById.acquire();
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
          __preparedStmtOfDeleteCityById.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<WorldCityEntity>> getAllCities() {
    final String _sql = "SELECT * FROM world_cities ORDER BY id ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"world_cities"}, new Callable<List<WorldCityEntity>>() {
      @Override
      @NonNull
      public List<WorldCityEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfCityName = CursorUtil.getColumnIndexOrThrow(_cursor, "cityName");
          final int _cursorIndexOfCountryName = CursorUtil.getColumnIndexOrThrow(_cursor, "countryName");
          final int _cursorIndexOfTimeZoneId = CursorUtil.getColumnIndexOrThrow(_cursor, "timeZoneId");
          final List<WorldCityEntity> _result = new ArrayList<WorldCityEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final WorldCityEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpCityName;
            _tmpCityName = _cursor.getString(_cursorIndexOfCityName);
            final String _tmpCountryName;
            _tmpCountryName = _cursor.getString(_cursorIndexOfCountryName);
            final String _tmpTimeZoneId;
            _tmpTimeZoneId = _cursor.getString(_cursorIndexOfTimeZoneId);
            _item = new WorldCityEntity(_tmpId,_tmpCityName,_tmpCountryName,_tmpTimeZoneId);
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

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
