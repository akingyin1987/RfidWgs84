package com.akingyin.rfidwgs.util;

import android.content.Context;
import android.content.DialogInterface;
import android.text.TextUtils;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.StackingBehavior;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import java.util.concurrent.TimeUnit;


/**
 * Created by Administrator on 2017/12/29.
 */

public class TaskShowDialog {

    // --------------进度条显示----------------
    MaterialDialog loadingDialog;

    boolean    isAutomatic = false;
    boolean isLoading() {
        return null != loadingDialog && loadingDialog.isShowing();
    }
    Context context;

    private   DialogCallBack<Boolean>  callBack;

    public DialogCallBack<Boolean> getCallBack() {
        return callBack;
    }

    public TaskShowDialog setCallBack(DialogCallBack<Boolean> callBack) {
        this.callBack = callBack;
        return this;
    }

    Disposable mDisposable;
    private    long   currentTime;
    private   String  msg;
    public void showLoadDialog(Context context,int  max, String message) {
        isAutomatic = false;
        this.context = context;
        this.msg = message;
        try {
            if (isLoading()) {
                return;
            }
            if (null == loadingDialog) {

                loadingDialog = new MaterialDialog.Builder(context)
                        .content(message)
                        .progress(false,0)
                        .stackingBehavior(StackingBehavior.ADAPTIVE).build();

            }
            currentTime = System.currentTimeMillis();
            loadingDialog.setMaxProgress(max);
            if (!TextUtils.isEmpty(message)) {
                loadingDialog.setContent(message+" ,耗时("+((System.currentTimeMillis()-currentTime)/1000)+"s"+")");
            } else {
                loadingDialog.setContent("处理中...耗时("+(System.currentTimeMillis()-currentTime)/1000+"s"+")");
            }
            loadingDialog.setCancelable(true);
            loadingDialog.setCanceledOnTouchOutside(false);
            loadingDialog.show();
            loadingDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    if(null != mDisposable && !mDisposable.isDisposed()){
                        mDisposable.dispose();
                    }
                    if(!isAutomatic && null != callBack){
                        callBack.call(true);
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();

        }

       mDisposable =  Observable.interval(1,1, TimeUnit.SECONDS)
                .compose(RxUtil.<Long>IO_Main())
                .subscribe(new Consumer<Long>() {
                    @Override
                    public void accept(Long aLong) throws Exception {
                        if (!TextUtils.isEmpty(msg)) {
                            loadingDialog.setContent(msg+" ,耗时("+((System.currentTimeMillis()-currentTime)/1000)+"s"+")");
                        } else {
                            loadingDialog.setContent("处理中...耗时("+(System.currentTimeMillis()-currentTime)/1000+"s"+")");
                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {

                    }
                });

    }



    public void hideDialog() {
        isAutomatic = true;
        if(null != mDisposable && !mDisposable.isDisposed()){
            mDisposable.dispose();
        }
        if (isLoading()) {
            loadingDialog.dismiss();
        }
        loadingDialog = null;
    }

    public void flushDialog(String message,int  progress) {
        this.msg = message;
        if (isLoading()) {
            loadingDialog.setProgress(progress);

            if (!TextUtils.isEmpty(msg)) {
                loadingDialog.setContent(msg+" .耗时("+((System.currentTimeMillis()-currentTime)/1000)+"s"+")");
            } else {
                loadingDialog.setContent("处理中...耗时("+(System.currentTimeMillis()-currentTime)/1000+"s"+")");
            }
        }
    }
}
