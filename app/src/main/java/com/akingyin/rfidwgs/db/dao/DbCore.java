/*
 * Copyright (c) 2017. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package com.akingyin.rfidwgs.db.dao;

import android.content.Context;
import java.lang.ref.WeakReference;
import org.greenrobot.greendao.database.Database;
import org.greenrobot.greendao.query.QueryBuilder;


public class DbCore {
  public static final boolean ENCRYPTED = false;
  private static final String DEFAULT_DB_NAME = "rfid.db";
  private static  final String  SECRET_KEY="";
  private  volatile static DaoMaster daoMaster;
  private  volatile static DaoSession daoSession;

  private static WeakReference<Context> mContext;
  private static String DB_NAME;

  public static void init(Context context) {
    init(context, DEFAULT_DB_NAME);
  }

  public static void init(Context context, String dbName) {

    if (context == null) {
      throw new IllegalArgumentException("context can't be null");
    }
    mContext = new WeakReference<>(context.getApplicationContext());
    DB_NAME = dbName;

  }

  public static DaoMaster getDaoMaster() {
    if (daoMaster == null ) {
      synchronized (DaoMaster.class) {
        if (daoMaster == null) {
          if(null != mContext && null != mContext.get()){
            DaoMaster.OpenHelper helper = new UpgradeHelper(mContext.get(),ENCRYPTED ? "encrypted-"+DB_NAME : DB_NAME);
            Database db = helper.getWritableDb();
            daoMaster = new DaoMaster(db);
          }
        }
      }



    }
    return daoMaster;
  }




  public static DaoSession getDaoSession() {
    if (daoSession == null) {
      synchronized (DaoSession.class){
        if(null == daoSession){
          if (daoMaster == null) {
            daoMaster = getDaoMaster();
          }
          daoSession = daoMaster.newSession();
        }
      }

    }
    return daoSession;
  }

  public static void enableQueryBuilderLog(){

    QueryBuilder.LOG_SQL = true;
    QueryBuilder.LOG_VALUES = true;
  }
}
