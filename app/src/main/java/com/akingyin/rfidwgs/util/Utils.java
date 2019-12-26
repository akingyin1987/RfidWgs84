package com.akingyin.rfidwgs.util;

/**
 * @author king
 * @version V1.0
 * @ Description:
 * @ Date 2019/12/20 15:44
 */
public class Utils {


  /**
   * Degree to Degree minute second. 十进制度转度分秒(xxx° ==> xxx°xxx′xxx″)
   */
  public static String getDegreeString(double radian) {
    int degree = 0;
    double minute = 0;
    double second = 0;

    try {
      double d = Math.toDegrees(radian);
      degree = (int)d;
      minute = (d - degree) * 60;
      second = (minute - (int)minute) * 60;
    } catch (Exception e) {
      e.printStackTrace();
    }
    return String.format("%1$s ° %2$s ′ %3$.4f ″", degree, (int)minute, second);
  }


}
