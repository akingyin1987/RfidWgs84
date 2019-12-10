package com.akingyin.rfidwgs.util

import com.akingyin.rfidwgs.config.AppFileConfig
import com.akingyin.rfidwgs.db.Batch
import com.akingyin.rfidwgs.db.LatLngRfid
import com.akingyin.rfidwgs.ext.currentTimeMillis
import com.blankj.utilcode.util.TimeUtils
import jxl.CellView
import jxl.Workbook
import jxl.format.Colour
import jxl.format.UnderlineStyle
import jxl.write.Label
import jxl.write.WritableCellFormat
import jxl.write.WritableFont
import java.io.File
import java.util.*


/**
 * @ Description:
 * @author king
 * @ Date 2019/12/7 15:48
 * @version V1.0
 */
object ExcelUtil {

   suspend fun   onExportExcel(latLngVos: List<LatLngRfid>,batch: Batch,callBack:(result:Boolean,error:String?) ->Unit  ){
        try {
            val  exportFile = File(AppFileConfig.APP_FILE_ROOT+File.separator+TimeUtils.date2String(Date(),"yyyyMMddHHmmss")+".xls")
            exportFile.parentFile?.mkdirs()
            //创建excel文件
            val mExcelWorkbook = Workbook.createWorkbook(exportFile)
            //工作表
            val wsheet =  mExcelWorkbook.createSheet("标签-"+batch.name,0)

            // 设置Excel字体
            val wfont = WritableFont(WritableFont.ARIAL, 16, WritableFont.BOLD, false, UnderlineStyle.NO_UNDERLINE, Colour.BLACK)
            val cv = CellView()
            cv.isAutosize = true
            val titleFormat = WritableCellFormat(wfont)
            val title = arrayOf("标签","经度","维度","时间")

            val  label = Label(0,0,"批次：${batch.name} 创建时间：${TimeUtils.millis2String(batch.createTime)}  导出总数：${latLngVos.size}  导出时间：${TimeUtils.millis2String(currentTimeMillis)}",titleFormat)
            wsheet.addCell(label)
            wsheet.setColumnView(0, cv)



            val cv1 = CellView()
            cv1.isAutosize = true
            // 设置Excel表头
            for (k in title.indices) {
                val excelTitle = Label(k, 1, title[k], titleFormat)
                wsheet.addCell(excelTitle)
                wsheet.setColumnView(k, cv1)
            }
            wsheet.setColumnView(0,50)

            latLngVos.forEachIndexed { index, latLngVo ->
                val content1 = Label(0, index+2, latLngVo.rfid)
                val content2 = Label(1, index+2,latLngVo.wgsLat.toString())
                val content3 = Label(2, index+2,latLngVo.wgsLng.toString())
                val content4 = Label(3, index+2,TimeUtils.millis2String(latLngVo.operationTime))
                wsheet.addCell(content1)
                wsheet.addCell(content2)
                wsheet.addCell(content3)
                wsheet.addCell(content4)

            }
            wsheet.mergeCells(0,0,4,0)
           // wsheet.mergedCells
            mExcelWorkbook.write() // 写入文件
            mExcelWorkbook.close()
//            latLngVos.forEach {
//                it.exportTime = currentTimeMillis
//            }
//            BatichDbUtil.getLatlngRfidDao().saveInTx(latLngVos)
            callBack(true,null)
        }catch (e : Exception){
            e.printStackTrace()
            callBack.invoke(false,e.message)
        }

    }
}