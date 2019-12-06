package com.akingyin.rfidwgs.ext

import android.app.Application

/**
 * app单例
 * @ Description:
 * @author king
 * @ Date 2019/4/29 11:51
 * @version V1.0
 */
object Ext {

    lateinit var ctx: Application

    fun with(app: Application) {
        ctx = app
    }
}