package com.akingyin.rfidwgs.ui

import android.app.Activity
import java.util.*
/**
 * @ Description:
 * @author king
 * @ Date 2018/9/3 15:51
 * @version V1.0
 */
class  AppManager  private  constructor(){ //私有的主构造器


    companion object{ ////被companion object包裹的语句都是private的

        @Volatile
        private var instance: AppManager? = null

        @Synchronized
        fun getInstance(): AppManager?{
            if (instance == null){
                instance = AppManager()
            }
            return instance
        }
    }

     var activityStack: Stack<Activity>? = null











    /**
     * 添加Activity到堆栈
     */
    fun addActivity(activity: Activity) {
        if (activityStack == null) {
            activityStack = Stack()
        }
        activityStack!!.add(activity)
    }

    /**
     * 获取当前Activity（堆栈中最后一个压入的）
     */
    fun currentActivity(): Activity {
        return activityStack!!.lastElement()
    }

    /**
     * 结束当前Activity（堆栈中最后一个压入的）
     */
    fun finishActivity() {
        val activity = activityStack!!.lastElement()
        finishActivity(activity)
    }

    /**
     * 结束指定的Activity
     */
    fun finishActivity(activity: Activity?) {
        if (activity != null) {
            activityStack!!.remove(activity)
        }
    }

    /**
     * 结束指定类名的Activity
     */
    fun finishActivity(cls: Class<*>) {
        for (activity in activityStack!!) {
            if (activity.javaClass == cls) {
                finishActivity(activity)
            }
        }
    }

    /**
     * 结束所有Activity
     */
    fun finishAllActivity() {
        var i = 0
        val size = activityStack!!.size
        while (i < size) {
            if (null != activityStack!!.get(i)) {

                activityStack!!.get(i).finish()
            }
            i++
        }
        activityStack!!.clear()
    }

    /**
     * 退出应用程序
     */
    fun AppExit() {
        try {
            finishAllActivity()
            android.os.Process.killProcess(android.os.Process.myPid())

            System.exit(0)
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }
}