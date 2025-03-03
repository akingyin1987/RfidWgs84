package com.akingyin.rfidwgs.ui

import android.content.Intent
import android.widget.Toast
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.afollestad.materialdialogs.MaterialDialog
import com.akingyin.rfidwgs.R
import com.akingyin.rfidwgs.databinding.ActivityBatchListBinding
import com.akingyin.rfidwgs.db.Batch
import com.akingyin.rfidwgs.db.dao.BatichDbUtil
import com.akingyin.rfidwgs.ext.currentTimeMillis
import com.akingyin.rfidwgs.ui.adapter.BatchListAdapter
import com.akingyin.rfidwgs.util.DialogUtil
import com.bleqpp.BleQppNfcCameraServer
import com.zlcdgroup.nfcsdk.ConStatus

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*


/**
 * 批次管理
 * @ Description:
 * @author king
 * @ Date 2019/12/6 14:06
 * @version V1.0
 */
class BatchListActivity : BaseActivity() {

    private lateinit var  batchListAdapter: BatchListAdapter


    private lateinit var  viewBind: ActivityBatchListBinding


    override val useViewBind: Boolean
        get() = true

    override fun getLayoutId()= R.layout.activity_batch_list


    override fun initViewBind() {
        super.initViewBind()
        viewBind = ActivityBatchListBinding.inflate(layoutInflater)
        setContentView(viewBind.root)
    }

    override fun initView() {

        setToolBar(viewBind.toolbar,"批次管理")
        batchListAdapter = BatchListAdapter()
        viewBind.recycler.layoutManager = LinearLayoutManager(this)
        viewBind.recycler.itemAnimator = DefaultItemAnimator()
        viewBind.recycler.adapter = batchListAdapter

        batchListAdapter.setOnItemClickListener { _, _, position ->
            batchListAdapter.getItem(position).apply {

                startActivity(Intent(this@BatchListActivity,LatlngRfidEditActivity::class.java).apply {

                    putExtra("batchId",id)
                })
            }
        }

        batchListAdapter.setOnItemLongClickListener { _, _, position ->
            onShowDeleteBatch(batchListAdapter.getItem(position),position)
            return@setOnItemLongClickListener true
        }
        viewBind.fabLoc.setOnClickListener {
            onShowAddBatch()
        }
    }

    private fun   onShowAddBatch(){
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

   private  fun   onShowDeleteBatch(batch: Batch,postion:Int){
        DialogUtil.showConfigDialog(this,"确定要删除当前批次！"){
            if(it){
                BatichDbUtil.onDeleteBatch(batch)
                showMsg("删除成功")
                batchListAdapter.removeAt(postion)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        println("列表取消注册->")
    }

    override fun onResume() {
        super.onResume()
        println("列表开始注册--->")
        flushData()

    }

    private fun   flushData(){

        GlobalScope.launch(Dispatchers.Main) {

            batchListAdapter.setNewInstance(getBatchList().toMutableList())
        }
    }

    private  suspend fun   getBatchList():List<Batch>{
       return withContext(IO){
            BatichDbUtil.findAllBatich()
        }

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


    private  var   mExitTime = 0L
    override fun onBackPressed() {
        if(currentTimeMillis - mExitTime > 2000){
            Toast.makeText(this,"再按一次退出程序",Toast.LENGTH_SHORT).show()
            mExitTime = currentTimeMillis
            return
        }
        println("ble=$isSupportBle:$BLE_NFC_CARD")
        if(isSupportBle && BLE_NFC_CARD == 1){
            BleQppNfcCameraServer.getInstance(this).connectDestroy()
        }
        AppManager.getInstance()?.AppExit()
        finish()
        super.onBackPressed()
    }

    override fun onDestroy() {

        super.onDestroy()
    }
}