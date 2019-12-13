package com.akingyin.rfidwgs.ui

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.*
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.widget.Toast
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.akingyin.rfidwgs.ext.spGetInt
import com.akingyin.rfidwgs.util.ConvertUtils
import com.bleqpp.BleQppNfcCameraServer
import com.zlcdgroup.nfcsdk.ConStatus
import com.zlcdgroup.nfcsdk.RfidConnectorInterface
import com.zlcdgroup.nfcsdk.RfidInterface
import java.lang.ref.WeakReference


/**
 * @ Description:
 * @author king
 * @ Date 2019/12/6 14:51
 * @version V1.0
 */
abstract class BaseActivity : AppCompatActivity(), RfidConnectorInterface {

    var mAdapter: NfcAdapter? = null
    var mPendingIntent: PendingIntent? = null
    var mFilters: Array<IntentFilter>? = null
    var mTechLists: Array<Array<String>>? = null
    var mf: MifareClassic? = null
    var tagFromIntent: Tag? = null
    var openNfc = 0
    var isSupportBle = true
    var  BLE_NFC_CARD  = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainHandler = MyHandler(this)
        setContentView(getLayoutId())
        mAdapter = NfcAdapter.getDefaultAdapter(this)

        if(!packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)){
            isSupportBle = false
        }
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
        BLE_NFC_CARD = spGetInt("ble_cardread")
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
        if(isSupportBle && BLE_NFC_CARD == 1){
            BleQppNfcCameraServer.getInstance(this).unregistered(this)
        }
    }

    override fun onResume() {
        super.onResume()
        if(null != mAdapter){
            mAdapter!!.enableForegroundDispatch(this,mPendingIntent,mFilters,mTechLists)
        }
        if(isSupportBle && BLE_NFC_CARD == 1){
            BleQppNfcCameraServer.getInstance(this).onregistered(this)
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


    class MyHandler(activity: BaseActivity) : Handler(Looper.getMainLooper()) {
        private val mActivity: WeakReference<BaseActivity> = WeakReference(activity)

        override fun handleMessage(msg: Message) {
            if (mActivity.get() == null) {
                return
            }
            val activity = mActivity.get()
            when (msg.what) {
                0-> {
                    activity?.handleConnectStatus(msg.obj as ConStatus)
                }
                1->{
                    activity?.handTag(msg.obj.toString(),null)
                }

                2->{
                    activity?.handleElectricity(msg.arg1)
                }
                else -> {
                }
            }
        }
    }

    override fun onNewRfid(data: ByteArray?, p1: RfidInterface?) {
        data?.let {
            mainHandler.sendMessage(mainHandler.obtainMessage().apply {
                what=1
                obj = ConvertUtils.bytes2HexStrReverse(it)
            })

        }
    }

    override fun onConnectStatus(conSttus: ConStatus?) {
        conSttus?.let {
            mainHandler.sendMessage(mainHandler.obtainMessage().apply {
                what=0
                obj = it
            })
        }

    }

    abstract   fun    handleConnectStatus(conSttus: ConStatus)

    abstract   fun    handleElectricity(elect: Int)

    override fun onElectricity(elect: Int) {
        mainHandler.sendMessage(mainHandler.obtainMessage().apply {
            what=2
            arg1 = elect
        })
    }

    lateinit var  mainHandler  :MyHandler
    override fun onDestroy() {
        mainHandler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}