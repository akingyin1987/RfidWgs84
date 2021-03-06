package com.akingyin.rfidwgs.db.dao;

import android.database.Cursor;
import android.database.sqlite.SQLiteStatement;

import org.greenrobot.greendao.AbstractDao;
import org.greenrobot.greendao.Property;
import org.greenrobot.greendao.internal.DaoConfig;
import org.greenrobot.greendao.database.Database;
import org.greenrobot.greendao.database.DatabaseStatement;

import com.akingyin.rfidwgs.db.LatLngRfid;

// THIS CODE IS GENERATED BY greenDAO, DO NOT EDIT.
/** 
 * DAO for table "tb_latlng_rfid".
*/
public class LatLngRfidDao extends AbstractDao<LatLngRfid, Long> {

    public static final String TABLENAME = "tb_latlng_rfid";

    /**
     * Properties of entity LatLngRfid.<br/>
     * Can be used for QueryBuilder and for referencing column names.
     */
    public static class Properties {
        public final static Property Id = new Property(0, Long.class, "id", true, "_id");
        public final static Property BatchId = new Property(1, Long.class, "batchId", false, "BATCH_ID");
        public final static Property WgsLat = new Property(2, double.class, "wgsLat", false, "WGS_LAT");
        public final static Property WgsLng = new Property(3, double.class, "wgsLng", false, "WGS_LNG");
        public final static Property Rfid = new Property(4, String.class, "rfid", false, "RFID");
        public final static Property OperationTime = new Property(5, long.class, "operationTime", false, "OPERATION_TIME");
        public final static Property ExportTime = new Property(6, long.class, "exportTime", false, "EXPORT_TIME");
        public final static Property UploadTime = new Property(7, long.class, "uploadTime", false, "UPLOAD_TIME");
    }


    public LatLngRfidDao(DaoConfig config) {
        super(config);
    }
    
    public LatLngRfidDao(DaoConfig config, DaoSession daoSession) {
        super(config, daoSession);
    }

    /** Creates the underlying database table. */
    public static void createTable(Database db, boolean ifNotExists) {
        String constraint = ifNotExists? "IF NOT EXISTS ": "";
        db.execSQL("CREATE TABLE " + constraint + "\"tb_latlng_rfid\" (" + //
                "\"_id\" INTEGER PRIMARY KEY AUTOINCREMENT ," + // 0: id
                "\"BATCH_ID\" INTEGER," + // 1: batchId
                "\"WGS_LAT\" REAL NOT NULL ," + // 2: wgsLat
                "\"WGS_LNG\" REAL NOT NULL ," + // 3: wgsLng
                "\"RFID\" TEXT," + // 4: rfid
                "\"OPERATION_TIME\" INTEGER NOT NULL ," + // 5: operationTime
                "\"EXPORT_TIME\" INTEGER NOT NULL ," + // 6: exportTime
                "\"UPLOAD_TIME\" INTEGER NOT NULL );"); // 7: uploadTime
    }

    /** Drops the underlying database table. */
    public static void dropTable(Database db, boolean ifExists) {
        String sql = "DROP TABLE " + (ifExists ? "IF EXISTS " : "") + "\"tb_latlng_rfid\"";
        db.execSQL(sql);
    }

    @Override
    protected final void bindValues(DatabaseStatement stmt, LatLngRfid entity) {
        stmt.clearBindings();
 
        Long id = entity.getId();
        if (id != null) {
            stmt.bindLong(1, id);
        }
 
        Long batchId = entity.getBatchId();
        if (batchId != null) {
            stmt.bindLong(2, batchId);
        }
        stmt.bindDouble(3, entity.getWgsLat());
        stmt.bindDouble(4, entity.getWgsLng());
 
        String rfid = entity.getRfid();
        if (rfid != null) {
            stmt.bindString(5, rfid);
        }
        stmt.bindLong(6, entity.getOperationTime());
        stmt.bindLong(7, entity.getExportTime());
        stmt.bindLong(8, entity.getUploadTime());
    }

    @Override
    protected final void bindValues(SQLiteStatement stmt, LatLngRfid entity) {
        stmt.clearBindings();
 
        Long id = entity.getId();
        if (id != null) {
            stmt.bindLong(1, id);
        }
 
        Long batchId = entity.getBatchId();
        if (batchId != null) {
            stmt.bindLong(2, batchId);
        }
        stmt.bindDouble(3, entity.getWgsLat());
        stmt.bindDouble(4, entity.getWgsLng());
 
        String rfid = entity.getRfid();
        if (rfid != null) {
            stmt.bindString(5, rfid);
        }
        stmt.bindLong(6, entity.getOperationTime());
        stmt.bindLong(7, entity.getExportTime());
        stmt.bindLong(8, entity.getUploadTime());
    }

    @Override
    public Long readKey(Cursor cursor, int offset) {
        return cursor.isNull(offset + 0) ? null : cursor.getLong(offset + 0);
    }    

    @Override
    public LatLngRfid readEntity(Cursor cursor, int offset) {
        LatLngRfid entity = new LatLngRfid( //
            cursor.isNull(offset + 0) ? null : cursor.getLong(offset + 0), // id
            cursor.isNull(offset + 1) ? null : cursor.getLong(offset + 1), // batchId
            cursor.getDouble(offset + 2), // wgsLat
            cursor.getDouble(offset + 3), // wgsLng
            cursor.isNull(offset + 4) ? null : cursor.getString(offset + 4), // rfid
            cursor.getLong(offset + 5), // operationTime
            cursor.getLong(offset + 6), // exportTime
            cursor.getLong(offset + 7) // uploadTime
        );
        return entity;
    }
     
    @Override
    public void readEntity(Cursor cursor, LatLngRfid entity, int offset) {
        entity.setId(cursor.isNull(offset + 0) ? null : cursor.getLong(offset + 0));
        entity.setBatchId(cursor.isNull(offset + 1) ? null : cursor.getLong(offset + 1));
        entity.setWgsLat(cursor.getDouble(offset + 2));
        entity.setWgsLng(cursor.getDouble(offset + 3));
        entity.setRfid(cursor.isNull(offset + 4) ? null : cursor.getString(offset + 4));
        entity.setOperationTime(cursor.getLong(offset + 5));
        entity.setExportTime(cursor.getLong(offset + 6));
        entity.setUploadTime(cursor.getLong(offset + 7));
     }
    
    @Override
    protected final Long updateKeyAfterInsert(LatLngRfid entity, long rowId) {
        entity.setId(rowId);
        return rowId;
    }
    
    @Override
    public Long getKey(LatLngRfid entity) {
        if(entity != null) {
            return entity.getId();
        } else {
            return null;
        }
    }

    @Override
    public boolean hasKey(LatLngRfid entity) {
        return entity.getId() != null;
    }

    @Override
    protected final boolean isEntityUpdateable() {
        return true;
    }
    
}
