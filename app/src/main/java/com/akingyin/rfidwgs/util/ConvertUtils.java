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


}
