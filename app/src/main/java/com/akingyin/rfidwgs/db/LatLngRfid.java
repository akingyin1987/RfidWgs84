package com.akingyin.rfidwgs.db;

import java.io.Serializable;
import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.Property;
import org.greenrobot.greendao.annotation.Generated;

/**
 * @author king
 * @version V1.0
 * @ Description:
 * @ Date 2019/12/6 13:36
 */

@Entity(nameInDb = "tb_latlng_rfid")
public class LatLngRfid implements Serializable {
  private static final long serialVersionUID = 5654481107274340232L;

  @Id(autoincrement = true)
  private   Long   id;

  /**
   * 批次ID
   */
  @Property
  private   Long    batchId;


  @Property
  private   double    wgsLat;


  @Property
  private   double    wgsLng;


  @Property
  private   String    rfid;

  /**
   * 操作时间
   */

  @Property
  private   long    operationTime;

  /**
   * 导出时间
   */
  @Property
  private   long    exportTime;

  /**
   * 上传时间
   */
  @Property
  private   long    uploadTime;

  @Generated(hash = 1739820074)
  public LatLngRfid(Long id, Long batchId, double wgsLat, double wgsLng,
          String rfid, long operationTime, long exportTime, long uploadTime) {
      this.id = id;
      this.batchId = batchId;
      this.wgsLat = wgsLat;
      this.wgsLng = wgsLng;
      this.rfid = rfid;
      this.operationTime = operationTime;
      this.exportTime = exportTime;
      this.uploadTime = uploadTime;
  }

  @Generated(hash = 1676580530)
  public LatLngRfid() {
  }

  public Long getId() {
      return this.id;
  }

  public void setId(Long id) {
      this.id = id;
  }

  public Long getBatchId() {
      return this.batchId;
  }

  public void setBatchId(Long batchId) {
      this.batchId = batchId;
  }

  public double getWgsLat() {
      return this.wgsLat;
  }

  public void setWgsLat(double wgsLat) {
      this.wgsLat = wgsLat;
  }

  public double getWgsLng() {
      return this.wgsLng;
  }

  public void setWgsLng(double wgsLng) {
      this.wgsLng = wgsLng;
  }

  public String getRfid() {
      return this.rfid;
  }

  public void setRfid(String rfid) {
      this.rfid = rfid;
  }

  public long getOperationTime() {
      return this.operationTime;
  }

  public void setOperationTime(long operationTime) {
      this.operationTime = operationTime;
  }

  public long getExportTime() {
      return this.exportTime;
  }

  public void setExportTime(long exportTime) {
      this.exportTime = exportTime;
  }

  public long getUploadTime() {
      return this.uploadTime;
  }

  public void setUploadTime(long uploadTime) {
      this.uploadTime = uploadTime;
  }

}
