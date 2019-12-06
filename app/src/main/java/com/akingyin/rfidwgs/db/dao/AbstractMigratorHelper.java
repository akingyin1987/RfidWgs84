/*
 * Copyright (c) 2017. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package com.akingyin.rfidwgs.db.dao;

import android.database.sqlite.SQLiteDatabase;

/**
 * @ Description:
 *  数据库升级
 *
 * @ Author king
 * @ Date 2016/9/26 12:05
 * @ Version V1.0
 */

public abstract class AbstractMigratorHelper {

  public abstract void onUpgrade(SQLiteDatabase db);
}
