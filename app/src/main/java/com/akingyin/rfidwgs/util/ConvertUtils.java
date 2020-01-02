package com.akingyin.rfidwgs.util;

/**
 * @author king
 * @version V1.0
 * @ Description:
 * @ Date 2019/12/6 15:00
 */
public class ConvertUtils {
  /**
   * 倒序排列
   * @param src
   * @return
   */
  public  static  String bytes2HexStrReverse(byte[]  src){
    StringBuilder stringBuilder = new StringBuilder("");
    if (src == null || src.length <= 0) {
      return null;
    }
    for (int i = src.length-1; i >=0; i--) {
      int v = src[i] & 0xFF;
      String hv = Integer.toHexString(v);
      if (hv.length() < 2) {
        stringBuilder.append(0);
      }
      stringBuilder.append(hv);
    }
    return stringBuilder.toString().toUpperCase();
  }
  /**
   * 将经纬度转换为度分秒格式
   * @param du 116.418847
   * @return 116°25'7.85"
   */
  public static String changeToDFM(double du) {
    int du1 = (int) du;
    double tp = (du - du1) * 60;
    int fen = (int) tp;
    String miao = String.format("%.2f", Math.abs(((tp - fen) * 60)));
    return du1 + "°" + Math.abs(fen) + "'" + miao + "\"";
  }

  /**
   * 度分秒转经纬度
   * @param dms 116°25'7.85"
   * @return 116.418847
   */
  public static double changeToDu(String dms) {
    if (dms == null) {
      return 0;
    }
    try {
      dms = dms.replace(" ", "");
      String[] str2 = dms.split("°");
      if (str2.length < 2) {
        return 0;
      }
      int d = Integer.parseInt(str2[0]);
      String[] str3 = str2[1].split("\'");
      if (str3.length < 2) {
        return 0;
      }
      int f = Integer.parseInt(str3[0]);
      String str4 = str3[1].substring(0, str3[1].length() - 1);
      double m = Double.parseDouble(str4);

      double fen = f + (m / 60);
      double du = (fen / 60) + Math.abs(d);
      if (d < 0) {
        du = -du;
      }
      return du;
    } catch (Exception e) {
      e.printStackTrace();
    }
    return 0;
  }



}
