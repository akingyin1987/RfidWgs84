package com.akingyin.rfidwgs.ui

import android.content.Intent
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.afollestad.materialdialogs.MaterialDialog
import com.akingyin.rfidwgs.R
import com.akingyin.rfidwgs.db.Batch
import com.akingyin.rfidwgs.db.dao.BatichDbUtil
import com.akingyin.rfidwgs.ui.adapter.BatchListAdapter
import com.akingyin.rfidwgs.util.DialogUtil
import com.zlcdgroup.nfcsdk.ConStatus
import kotlinx.android.synthetic.main.activity_batch_list.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*


/**
 * 批次管理
 * @ Description:
 * @author king
 * @ Date 2019/12/6 14:06
 * @version V1.0
 */
class BatchListActivity : BaseActivity() {

    lateinit var  batchListAdapter: BatchListAdapter

    override fun getLayoutId()= R.layout.activity_batch_list

    override fun initView() {

        setToolBar(toolbar,"批次管理")
        batchListAdapter = BatchListAdapter()
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.itemAnimator = DefaultItemAnimator()
        recycler.adapter = batchListAdapter

        batchListAdapter.setOnItemClickListener { _, _, position ->
            batchListAdapter.getItem(position)?.apply {

                startActivity(Intent(this@BatchListActivity,LatlngRfidEditActivity::class.java).apply {

                    putExtra("batchId",id)
                })
            }
        }

        batchListAdapter.setOnItemLongClickListener { _, _, position ->
            onShowDelectBatch(batchListAdapter.getItem(position)!!,position)
            return@setOnItemLongClickListener true
        }
        fab_loc.setOnClickListener {
            onShowAddBatch()
        }
    }

    fun   onShowAddBatch(){
        DialogUtil.showEditDialog(this,"添加批次",""){
            if(it.isNullOrEmpty()){
                showMsg("输入内容不可以为空！")
                return@showEditDialog
            }
            val   batch = Batch().apply {
                name = it
                createTime = System.currentTimeMillis()
                uuid = UUID.randomUUID().toString().replace("-","")
            }
            BatichDbUtil.getBatichDao().saveInTx(batch)
            batchListAdapter.addData(batch)
        }
    }

   private  fun   onShowDelectBatch(batch: Batch,postion:Int){
        DialogUtil.showConfigDialog(this,"确定要删除当前批次！"){
            if(it){
                BatichDbUtil.onDelectBatch(batch)
                showMsg("删除成功")
                batchListAdapter.remove(postion)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        flushData()

    }

    private fun   flushData(){
        GlobalScope.launch(Dispatchers.Main) {
            batchListAdapter.setNewData(getBatchList())
        }
    }

    suspend fun   getBatchList():List<Batch>{
        return BatichDbUtil.findAllBatich()
    }


    private   var   dialog:MaterialDialog ?=null
    override fun handTag(rfid: String, block0: String?) {
        dialog?.dismiss()
        BatichDbUtil.findRfidEntityByRfid(rfid)?.let {
           dialog = MaterialDialog.Builder(this).autoDismiss(true).title("操作")
                    .content("已查到当前标签：${rfid},请选择以下操作！")
                    .neutralText("取消")
                   // .negativeText("删除")
                    .positiveText("删除")
                    .onPositive { _, _ ->
                        BatichDbUtil.getLatlngRfidDao().delete(it)
                        BatichDbUtil.getLatlngRfidDao().detach(it)
                        flushData()
                    }
                    .show()
        }?:showMsg("当前标签未在终端数据中查询到！")

    }

    override fun handleConnectStatus(conSttus: ConStatus) {
    }

    override fun handleElectricity(elect: Int) {
    }
}