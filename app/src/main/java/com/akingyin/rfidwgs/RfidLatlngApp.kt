package com.akingyin.rfidwgs

import android.app.Application
import android.content.Context
import com.akingyin.rfidwgs.config.AppFileConfig
import com.akingyin.rfidwgs.db.dao.DbCore
import com.akingyin.rfidwgs.ext.Ext
import com.tencent.bugly.crashreport.CrashReport


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
        CrashReport.enableBugly(true)
        CrashReport.initCrashReport(this, "900021733", true)
        CrashReport.setUserSceneTag(this, 261841)

//        if(BuildConfig.DEBUG){
//            showDebugDBAddressLogToast(this)
//        }

        AppFileConfig.APP_FILE_ROOT = AppFileConfig.getAppFileRoot(this).absolutePath
    }

    override fun onTerminate() {
        super.onTerminate()
    }

    fun showDebugDBAddressLogToast(context: Context) {
   //     if (BuildConfig.DEBUG) {
//            try {
//                val debugDB = Class.forName("com.amitshekhar.DebugDB")
//                val getAddressLog: Method = debugDB.getMethod("getAddressLog")
//                val value: Any = getAddressLog.invoke(null)
//                Toast.makeText(context, value as String, Toast.LENGTH_LONG).show()
//            } catch (ignore: Exception) {
//            }
//        }
    }
}