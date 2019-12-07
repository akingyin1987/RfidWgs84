package com.akingyin.rfidwgs.db.dao

import com.akingyin.rfidwgs.db.Batch
import com.akingyin.rfidwgs.db.LatLngRfid
import java.util.*

/**
 * @ Description:
 * @author king
 * @ Date 2019/12/6 14:24
 * @version V1.0
 */
object BatichDbUtil {


    fun   getBatichDao():BatchDao{
        return  DbCore.getDaoSession().batchDao
    }

    fun  getLatlngRfidDao():LatLngRfidDao{
        return  DbCore.getDaoSession().latLngRfidDao
    }

    fun   findAllBatich():List<Batch>{
        return  DbCore.getDaoSession().batchDao.queryBuilder().orderDesc(BatchDao.Properties.CreateTime).list()
                .map {
                    it.uploadedTotal = getLatlngRfidDao().queryBuilder().where(LatLngRfidDao.Properties.BatchId.eq(it.id),LatLngRfidDao.Properties.OperationTime.gt(getTodayStartTime())).buildCount().count().toInt()
                    it.exportedTotal = getLatlngRfidDao().queryBuilder().where(LatLngRfidDao.Properties.BatchId.eq(it.id),LatLngRfidDao.Properties.ExportTime.gt(0)).buildCount().count().toInt()
                    it.uploadedTotal = getLatlngRfidDao().queryBuilder().where(LatLngRfidDao.Properties.BatchId.eq(it.id),LatLngRfidDao.Properties.UploadTime.gt(0)).buildCount().count().toInt()
                    it
                }
    }


    fun   onDelectBatch(batch: Batch){
        getLatlngRfidDao().queryBuilder().where(LatLngRfidDao.Properties.BatchId.eq(batch.id)).buildDelete().executeDeleteWithoutDetachingEntities()
        getBatichDao().delete(batch)
    }


    fun   getTodayStartTime():Long {
      return  Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.MILLISECOND,0)
            set(Calendar.MINUTE,0)
            set(Calendar.HOUR,0)
            set(Calendar.SECOND,0)
        }.timeInMillis
    }

    fun    onValRfid(rfid:String):Boolean{
       return getLatlngRfidDao().queryBuilder().where(LatLngRfidDao.Properties.Rfid.eq(rfid)).limit(1).list().size>0
    }

    fun   findRfidEntityByRfid(rfid: String):LatLngRfid?{
        return getLatlngRfidDao().queryBuilder().where(LatLngRfidDao.Properties.Rfid.eq(rfid)).limit(1).list().let {
            if(it.size>0){
              return@let it[0]
            }
            return@let null

        }

    }
}