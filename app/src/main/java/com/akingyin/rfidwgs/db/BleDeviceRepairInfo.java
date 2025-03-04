package com.akingyin.rfidwgs.db;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.Property;


import java.io.Serializable;
import org.greenrobot.greendao.annotation.Generated;

/**
 * @author: aking <a href="mailto:akingyin@163.com">Contact me.</a>
 * @since: 2025/3/4 10:15
 * @version: 1.0
 */

@Entity(nameInDb ="tb_ble_device_repair_info" )
public class BleDeviceRepairInfo implements Serializable {

    private static final long serialVersionUID = -3238077154577062932L;

    @Id(autoincrement = true)
    private   Long   id;


    @Property
    private   Long   batchId;

    @Property
    private   String   bleDeviceAddress;

    @Property
    private   Integer  repairStatus;

    @Property
    private   String   repairTime;

    @Property
    private   Long   createTime;

    @Property
    private   Long   updateTime;


    @Generated(hash = 21019631)
    public BleDeviceRepairInfo(Long id, Long batchId, String bleDeviceAddress,
            Integer repairStatus, String repairTime, Long createTime,
            Long updateTime) {
        this.id = id;
        this.batchId = batchId;
        this.bleDeviceAddress = bleDeviceAddress;
        this.repairStatus = repairStatus;
        this.repairTime = repairTime;
        this.createTime = createTime;
        this.updateTime = updateTime;
    }

    @Generated(hash = 1819982147)
    public BleDeviceRepairInfo() {
    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getBatchId() {
        return batchId;
    }

    public void setBatchId(Long batchId) {
        this.batchId = batchId;
    }

    public String getBleDeviceAddress() {
        return bleDeviceAddress;
    }

    public void setBleDeviceAddress(String bleDeviceAddress) {
        this.bleDeviceAddress = bleDeviceAddress;
    }

    public Integer getRepairStatus() {
        return repairStatus;
    }

    public void setRepairStatus(Integer repairStatus) {
        this.repairStatus = repairStatus;
    }

    public String getRepairTime() {
        return repairTime;
    }

    public void setRepairTime(String repairTime) {
        this.repairTime = repairTime;
    }

    public Long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Long createTime) {
        this.createTime = createTime;
    }

    public Long getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Long updateTime) {
        this.updateTime = updateTime;
    }
}
