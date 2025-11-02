package com.atakmap.android.LoRaBridge.Database;

import android.content.Context;

import androidx.lifecycle.LiveData;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GenericCotRepository {
    private final GenericCotDao dao;
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    public GenericCotRepository(Context ctx) {
        ChatDatabase db = ChatDatabase.getDatabase(ctx);
        this.dao = db.genericCotDao();
    }

    /** 原子去重插入：依赖 @Insert IGNORE 的返回值 */
    public boolean insertIfAbsent(GenericCotEntity e) {
        final long[] res = {-1};
        final Object lock = new Object();
        executor.execute(() -> {
            long rowId = dao.insert(e);
            synchronized (lock) {
                res[0] = rowId;
                lock.notify();
            }
        });

        synchronized (lock) {
            try { lock.wait(50); } catch (InterruptedException ignored) {}
        }
        return res[0] != -1;
    }

    public LiveData<List<GenericCotEntity>> getByUid(String uid) {
        return dao.getByUid(uid);
    }

    public LiveData<GenericCotEntity> latest() {
        return dao.latest();
    }

    public void deleteAll() {
        executor.execute(dao::deleteAll);
    }
}