package com.akingyin.rfidwgs

import android.app.Application
import com.akingyin.rfidwgs.config.AppFileConfig
import com.akingyin.rfidwgs.db.dao.DbCore
import com.akingyin.rfidwgs.ext.Ext

/**
 * @ Description:
 * @author king
 * @ Date 2019/12/6 15:31
 * @version V1.0
 */
class RfidLatlngApp  : Application() {

    override fun onCreate() {
        super.onCreate()
        Ext.with(this)
        DbCore.enableQueryBuilderLog()
        DbCore.init(this)
        AppFileConfig.APP_FILE_ROOT = AppFileConfig.getAppFileRoot(this).absolutePath
    }

    override fun onTerminate() {
        super.onTerminate()
    }
}