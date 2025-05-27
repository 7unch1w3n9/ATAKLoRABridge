package com.atakmap.android.LoRaBridge.Database;

import android.database.Cursor;
import androidx.lifecycle.LiveData;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

@SuppressWarnings({"unchecked", "deprecation"})
public final class ChatMessageDao_Impl implements ChatMessageDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<ChatMessageEntity> __insertionAdapterOfChatMessageEntity;

  public ChatMessageDao_Impl(RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfChatMessageEntity = new EntityInsertionAdapter<ChatMessageEntity>(__db) {
      @Override
      public String createQuery() {
        return "INSERT OR ABORT INTO `chat_messages` (`id`,`senderUid`,`senderCallsign`,`receiverUid`,`receiverCallsign`,`message`,`sentTime`) VALUES (nullif(?, 0),?,?,?,?,?,?)";
      }

      @Override
      public void bind(SupportSQLiteStatement stmt, ChatMessageEntity value) {
        stmt.bindLong(1, value.id);
        if (value.senderUid == null) {
          stmt.bindNull(2);
        } else {
          stmt.bindString(2, value.senderUid);
        }
        if (value.senderCallsign == null) {
          stmt.bindNull(3);
        } else {
          stmt.bindString(3, value.senderCallsign);
        }
        if (value.receiverUid == null) {
          stmt.bindNull(4);
        } else {
          stmt.bindString(4, value.receiverUid);
        }
        if (value.receiverCallsign == null) {
          stmt.bindNull(5);
        } else {
          stmt.bindString(5, value.receiverCallsign);
        }
        if (value.message == null) {
          stmt.bindNull(6);
        } else {
          stmt.bindString(6, value.message);
        }
        stmt.bindLong(7, value.sentTime);
      }
    };
  }

  @Override
  public void insert(final ChatMessageEntity message) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __insertionAdapterOfChatMessageEntity.insert(message);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public LiveData<List<ChatMessageEntity>> getMessagesForContact(final String contactUid) {
    final String _sql = "SELECT * FROM chat_messages WHERE senderUid = ? OR receiverUid = ? ORDER BY sentTime ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    if (contactUid == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, contactUid);
    }
    _argIndex = 2;
    if (contactUid == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, contactUid);
    }
    return __db.getInvalidationTracker().createLiveData(new String[]{"chat_messages"}, false, new Callable<List<ChatMessageEntity>>() {
      @Override
      public List<ChatMessageEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfSenderUid = CursorUtil.getColumnIndexOrThrow(_cursor, "senderUid");
          final int _cursorIndexOfSenderCallsign = CursorUtil.getColumnIndexOrThrow(_cursor, "senderCallsign");
          final int _cursorIndexOfReceiverUid = CursorUtil.getColumnIndexOrThrow(_cursor, "receiverUid");
          final int _cursorIndexOfReceiverCallsign = CursorUtil.getColumnIndexOrThrow(_cursor, "receiverCallsign");
          final int _cursorIndexOfMessage = CursorUtil.getColumnIndexOrThrow(_cursor, "message");
          final int _cursorIndexOfSentTime = CursorUtil.getColumnIndexOrThrow(_cursor, "sentTime");
          final List<ChatMessageEntity> _result = new ArrayList<ChatMessageEntity>(_cursor.getCount());
          while(_cursor.moveToNext()) {
            final ChatMessageEntity _item;
            final String _tmpSenderUid;
            if (_cursor.isNull(_cursorIndexOfSenderUid)) {
              _tmpSenderUid = null;
            } else {
              _tmpSenderUid = _cursor.getString(_cursorIndexOfSenderUid);
            }
            final String _tmpSenderCallsign;
            if (_cursor.isNull(_cursorIndexOfSenderCallsign)) {
              _tmpSenderCallsign = null;
            } else {
              _tmpSenderCallsign = _cursor.getString(_cursorIndexOfSenderCallsign);
            }
            final String _tmpReceiverUid;
            if (_cursor.isNull(_cursorIndexOfReceiverUid)) {
              _tmpReceiverUid = null;
            } else {
              _tmpReceiverUid = _cursor.getString(_cursorIndexOfReceiverUid);
            }
            final String _tmpReceiverCallsign;
            if (_cursor.isNull(_cursorIndexOfReceiverCallsign)) {
              _tmpReceiverCallsign = null;
            } else {
              _tmpReceiverCallsign = _cursor.getString(_cursorIndexOfReceiverCallsign);
            }
            final String _tmpMessage;
            if (_cursor.isNull(_cursorIndexOfMessage)) {
              _tmpMessage = null;
            } else {
              _tmpMessage = _cursor.getString(_cursorIndexOfMessage);
            }
            final long _tmpSentTime;
            _tmpSentTime = _cursor.getLong(_cursorIndexOfSentTime);
            _item = new ChatMessageEntity(_tmpSenderUid,_tmpSenderCallsign,_tmpReceiverUid,_tmpReceiverCallsign,_tmpMessage,_tmpSentTime);
            _item.id = _cursor.getInt(_cursorIndexOfId);
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
  public LiveData<List<ChatMessageEntity>> getAllMessages() {
    final String _sql = "SELECT * FROM chat_messages ORDER BY sentTime ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return __db.getInvalidationTracker().createLiveData(new String[]{"chat_messages"}, false, new Callable<List<ChatMessageEntity>>() {
      @Override
      public List<ChatMessageEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfSenderUid = CursorUtil.getColumnIndexOrThrow(_cursor, "senderUid");
          final int _cursorIndexOfSenderCallsign = CursorUtil.getColumnIndexOrThrow(_cursor, "senderCallsign");
          final int _cursorIndexOfReceiverUid = CursorUtil.getColumnIndexOrThrow(_cursor, "receiverUid");
          final int _cursorIndexOfReceiverCallsign = CursorUtil.getColumnIndexOrThrow(_cursor, "receiverCallsign");
          final int _cursorIndexOfMessage = CursorUtil.getColumnIndexOrThrow(_cursor, "message");
          final int _cursorIndexOfSentTime = CursorUtil.getColumnIndexOrThrow(_cursor, "sentTime");
          final List<ChatMessageEntity> _result = new ArrayList<ChatMessageEntity>(_cursor.getCount());
          while(_cursor.moveToNext()) {
            final ChatMessageEntity _item;
            final String _tmpSenderUid;
            if (_cursor.isNull(_cursorIndexOfSenderUid)) {
              _tmpSenderUid = null;
            } else {
              _tmpSenderUid = _cursor.getString(_cursorIndexOfSenderUid);
            }
            final String _tmpSenderCallsign;
            if (_cursor.isNull(_cursorIndexOfSenderCallsign)) {
              _tmpSenderCallsign = null;
            } else {
              _tmpSenderCallsign = _cursor.getString(_cursorIndexOfSenderCallsign);
            }
            final String _tmpReceiverUid;
            if (_cursor.isNull(_cursorIndexOfReceiverUid)) {
              _tmpReceiverUid = null;
            } else {
              _tmpReceiverUid = _cursor.getString(_cursorIndexOfReceiverUid);
            }
            final String _tmpReceiverCallsign;
            if (_cursor.isNull(_cursorIndexOfReceiverCallsign)) {
              _tmpReceiverCallsign = null;
            } else {
              _tmpReceiverCallsign = _cursor.getString(_cursorIndexOfReceiverCallsign);
            }
            final String _tmpMessage;
            if (_cursor.isNull(_cursorIndexOfMessage)) {
              _tmpMessage = null;
            } else {
              _tmpMessage = _cursor.getString(_cursorIndexOfMessage);
            }
            final long _tmpSentTime;
            _tmpSentTime = _cursor.getLong(_cursorIndexOfSentTime);
            _item = new ChatMessageEntity(_tmpSenderUid,_tmpSenderCallsign,_tmpReceiverUid,_tmpReceiverCallsign,_tmpMessage,_tmpSentTime);
            _item.id = _cursor.getInt(_cursorIndexOfId);
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

  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
