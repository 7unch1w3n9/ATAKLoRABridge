package com.atakmap.android.LoRaBridge.Database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface GenericCotDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(GenericCotEntity e);

    @Query("SELECT COUNT(*) FROM generic_cot WHERE id = :id")
    int existsById(String id);

    @Query("SELECT * FROM generic_cot WHERE uid = :uid ORDER BY timeIso ASC")
    LiveData<List<GenericCotEntity>> getByUid(String uid);

    @Query("SELECT * FROM generic_cot ORDER BY timeIso DESC LIMIT 1")
    LiveData<GenericCotEntity> latest();

    @Query("DELETE FROM generic_cot")
    void deleteAll();
}
