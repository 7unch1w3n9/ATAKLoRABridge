package com.atakmap.android.LoRaBridge.Database;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;

@Entity(tableName = "generic_cot")
public class GenericCotEntity {


    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id")
    public String id;

    @ColumnInfo(name = "uid")
    public String uid;

    @ColumnInfo(name = "type")
    public String type;


    @ColumnInfo(name = "timeIso")
    public String timeIso;

    @ColumnInfo(name = "origin")
    public String origin;

    @ColumnInfo(name = "cotRawXml")
    public String cotRawXml;

    @ColumnInfo(name = "exiBytes", typeAffinity = ColumnInfo.BLOB)
    public byte[] exiBytes;

    public GenericCotEntity(@NonNull String id,
                            String uid,
                            String type,
                            String timeIso,
                            String origin,
                            String cotRawXml,
                            byte[] exiBytes) {
        this.id = id;
        this.uid = uid;
        this.type = type;
        this.timeIso = timeIso;
        this.origin = origin;
        this.cotRawXml = cotRawXml;
        this.exiBytes = exiBytes;
    }

    public String getType() {
        return type;
    }
}
