package com.akingyin.rfidwgs.ui.adapter


import com.akingyin.rfidwgs.R
import com.akingyin.rfidwgs.db.Batch
import com.blankj.utilcode.util.TimeUtils
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder

import java.text.MessageFormat

/**
 * @ Description:
 * @author king
 * @ Date 2019/12/6 14:06
 * @version V1.0
 */
class BatchListAdapter  : BaseQuickAdapter<Batch, BaseViewHolder>( R.layout.item_batch) {

    override fun convert(helper: BaseViewHolder, item: Batch) {
        item.apply {
            helper.apply {
                setText(R.id.tv_sort,(adapterPosition+1).toString())
                setText(R.id.tv_bacth_name,name)
                setText(R.id.tv_bacth_time,TimeUtils.millis2String(createTime))
                setText(R.id.tv_bacth_total,MessageFormat.format("总数：{0}  今日：{1}  已导：{2}  已传：{3}",total,todayTotal,exportedTotal,uploadedTotal))
            }
        }
    }
}