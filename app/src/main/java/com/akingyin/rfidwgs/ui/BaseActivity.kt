package com.akingyin.rfidwgs.ui

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.*
import android.os.Bundle
import android.widget.Toast
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.akingyin.rfidwgs.util.ConvertUtils


/**
 * @ Description:
 * @author king
 * @ Date 2019/12/6 14:51
 * @version V1.0
 */
abstract class BaseActivity : AppCompatActivity() {

    var mAdapter: NfcAdapter? = null
    var mPendingIntent: PendingIntent? = null
    var mFilters: Array<IntentFilter>? = null
    var mTechLists: Array<Array<String>>? = null
    var mf: MifareClassic? = null
    var tagFromIntent: Tag? = null
    var openNfc = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(getLayoutId())
        mAdapter = NfcAdapter.getDefaultAdapter(this)


        if(null == mAdapter){
            showMsg("当前终端不支持NFC")
        }else{
            mPendingIntent = PendingIntent.getActivity(this,0, Intent(this,javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),0)
            val ndef = IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED)
            try {
                ndef.addDataType("*/*")
                mFilters = arrayOf(ndef)
                mTechLists = arrayOf(
                        arrayOf(MifareClassic::class.java.name),
                        arrayOf(NfcA::class.java.name),
                        arrayOf(NfcB::class.java.name),
                        arrayOf(NfcF::class.java.name),
                        arrayOf(NfcV::class.java.name))

            } catch (e: IntentFilter.MalformedMimeTypeException) {
                // TODO Auto-generated catch block
                e.printStackTrace()
            }
        }
        initView()
    }


    /**
     * 加载布局
     */
    @LayoutRes
    abstract    fun    getLayoutId():Int

    /**
     * 初始化View
     */
    abstract    fun    initView()

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        if (null != intent && NfcAdapter.ACTION_TECH_DISCOVERED == intent.action) {
            tagFromIntent = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
            tagFromIntent?.let {
                handTag(ConvertUtils.bytes2HexStrReverse(it.id),null)
            }

        }
    }


    abstract   fun    handTag( rfid:String,block0: String?)
    override fun onPause() {
        super.onPause()
        if(null != mAdapter){
            mAdapter!!.disableForegroundDispatch(this)
        }
    }

    override fun onResume() {
        super.onResume()
        if(null != mAdapter){
            mAdapter!!.enableForegroundDispatch(this,mPendingIntent,mFilters,mTechLists)
        }
    }

    override fun onStart() {
        super.onStart()
    }

    override fun onStop() {
        super.onStop()
    }

    protected fun setToolBar(toolbar: Toolbar, title: String) {
        toolbar.title = title
        setSupportActionBar(toolbar)
        supportActionBar?.let {
            it.setDisplayHomeAsUpEnabled(true)
            it.setDisplayShowHomeEnabled(true)
            toolbar.setNavigationOnClickListener {
                onBackPressed()
            }
        }


    }


    fun   showMsg(msg :String){
        Toast.makeText(this,msg,Toast.LENGTH_SHORT).show()
    }

}