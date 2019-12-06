package com.akingyin.rfidwgs.config

import android.content.Context
import android.os.Build
import android.os.Environment
import java.io.File


/**
 * @ Description:
 * @author king
 * @ Date 2019/11/25 15:08
 * @version V1.0
 */
object AppFileConfig {

    /**
     * 应用文件根目录
     */
    var APP_FILE_ROOT = ""

    fun getAppFileRoot(context: Context): File {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            File(context.getExternalFilesDir(null)
                    ?.getAbsoluteFile().toString() + "/rfid/")
        } else {
            File(Environment.getExternalStorageDirectory()
                    .getAbsoluteFile().toString() + "/rfid/")
        }
    }

    fun getSdCardRoot(context: Context): File {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            File(context.getExternalFilesDir(null)
                    ?.getAbsoluteFile().toString() )
        } else {
            File(Environment.getExternalStorageDirectory()
                    .getAbsoluteFile().toString() )
        }
    }
}