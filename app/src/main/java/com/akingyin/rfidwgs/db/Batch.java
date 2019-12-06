package com.akingyin.rfidwgs.db;

import java.io.Serializable;
import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.Property;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.Transient;

/**
 * @author king
 * @version V1.0
 * @ Description:
 * @ Date 2019/12/6 13:41
 */

@Entity(nameInDb = "tb_batch")
public class Batch  implements Serializable {
  private static final long serialVersionUID = 6397868603515993230L;


  @Transient
  public   int   todayTotal;

  @Transient
  public   int    exportedTotal;

  @Transient
  public   int    uploadedTotal;

  @Id(autoincrement = true)
  private   Long   id;

  @Property
  private   String     name;

  @Property
  private   long    createTime;

  @Property
  private   int    total;

  @Property
  private   String    uuid;

  @Generated(hash = 1009233592)
public Batch(Long id, String name, long createTime, int total, String uuid) {
    this.id = id;
    this.name = name;
    this.createTime = createTime;
    this.total = total;
    this.uuid = uuid;
}

@Generated(hash = 1365196433)
  public Batch() {
  }

  public Long getId() {
      return this.id;
  }

  public void setId(Long id) {
      this.id = id;
  }

  public String getName() {
      return this.name;
  }

  public void setName(String name) {
      this.name = name;
  }

  public long getCreateTime() {
      return this.createTime;
  }

  public void setCreateTime(long createTime) {
      this.createTime = createTime;
  }

  public int getTotal() {
      return this.total;
  }

  public void setTotal(int total) {
      this.total = total;
  }

public String getUuid() {
    return this.uuid;
}

public void setUuid(String uuid) {
    this.uuid = uuid;
}
}
