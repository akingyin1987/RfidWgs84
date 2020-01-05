package com.akingyin.rfidwgs.util;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.InputType;
import android.text.Spanned;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import java.util.List;

/**
 * Created by Administrator on 2017/8/29.
 */

public class DialogUtil {

  public static boolean checkNULL(Context context) {
    if (null == context) {
      return false;
    }
    if (context instanceof Activity) {
      if (((Activity) context).isFinishing()) {
        return false;
      }
    }
    return true;
  }


  public static void showConfigNotAutoDismissDialog(Context context, String message,
      final DialogCallBack<Boolean> callBack) {
    new MaterialDialog.Builder(context).title("提示")
        .content(message)
        .positiveText("确定")
        .negativeText("取消")
        .autoDismiss(false)
        .canceledOnTouchOutside(false)
        .cancelable(false)
        .onPositive(new MaterialDialog.SingleButtonCallback() {
          @Override
          public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
            dialog.dismiss();
            if (null != callBack) {
              callBack.call(true);
            }
          }
        })
        .onNegative(new MaterialDialog.SingleButtonCallback() {
          @Override
          public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
            dialog.dismiss();
            if (null != callBack) {
              callBack.call(false);
            }
          }
        })
        .show();
  }

  /**
   * 确认圣诞框
   */
  public static MaterialDialog showConfigDialog(Context context, String message,
      final DialogCallBack<Boolean> callBack) {
   return new MaterialDialog.Builder(context).title("提示")
        .content(message)
        .autoDismiss(true)
        .positiveText("确定")
        .negativeText("取消")

        .onPositive(new MaterialDialog.SingleButtonCallback() {
          @Override
          public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {

            if (null != callBack) {
              callBack.call(true);
            }
          }
        })
        .onNegative(new MaterialDialog.SingleButtonCallback() {
          @Override
          public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
            if (null != callBack) {
              callBack.call(false);
            }
          }
        })
        .show();
  }

  /**
   * 确认对话框
   */

  public static void showConfigDialog(@NonNull Context context, String message, String leftName,
      String rihthName, boolean cancelable, final DialogCallBack<Boolean> callBack) {
    new MaterialDialog.Builder(context).cancelable(cancelable)
        .content(message)
        .positiveText(rihthName)
        .negativeText(leftName)
        .onPositive(new MaterialDialog.SingleButtonCallback() {
          @Override
          public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
            if (null != callBack) {
              callBack.call(true);
            }
          }
        })
        .onNegative(new MaterialDialog.SingleButtonCallback() {
          @Override
          public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
            if (null != callBack) {
              callBack.call(false);
            }
          }
        })
        .show();
  }

  /**
   * html显示
   */

  public static void showConfigDialog(Context context, Spanned message,
      final DialogCallBack<Boolean> callBack) {
    new MaterialDialog.Builder(context).title("提示")
        .content(message)
        .positiveText("确定")
        .onPositive(new MaterialDialog.SingleButtonCallback() {
          @Override
          public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
            if (null != callBack) {
              callBack.call(true);
            }
          }
        })
        .negativeText("取消")
        .onNegative(new MaterialDialog.SingleButtonCallback() {
          @Override
          public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
            if (null != callBack) {
              callBack.call(false);
            }
          }
        })
        .show();
  }

  /**
   * html显示对话框
   */
  public static void showConfigDialog(@NonNull Context context, Spanned message, String leftName,
      String rihthName, boolean cancelable, final DialogCallBack<Boolean> callBack) {
    new MaterialDialog.Builder(context).cancelable(cancelable)
        .content(message)
        .positiveText(rihthName)
        .negativeText(leftName)
        .onPositive(new MaterialDialog.SingleButtonCallback() {
          @Override
          public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
            if (null != callBack) {
              callBack.call(true);
            }
          }
        })
        .onNegative(new MaterialDialog.SingleButtonCallback() {
          @Override
          public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
            if (null != callBack) {
              callBack.call(false);
            }
          }
        })
        .show();
  }





  /**
   * 显示信息
   */
  public static void showInfoDialog(@NonNull Context context, String message) {
    new MaterialDialog.Builder(context)

        .content(message).autoDismiss(true).canceledOnTouchOutside(true).show();
  }

  /**
   * 显示提示信息
   * @param context
   * @param title
   * @param message
   */
  public static void showInfoDialog(@NonNull Context context, String title, String message) {
    new MaterialDialog.Builder(context).title(title)
        .content(message)
        .autoDismiss(true)
        .canceledOnTouchOutside(true)
        .show();
  }

  /**
   * 输入对话框
   * @param context
   * @param title
   * @param message
   * @param callBack
   */
  public  static  void   showEditDialog(@NonNull Context context, String title, String message,int minLength,int maxLength,
      final AppCallBack<String> callBack){
    new MaterialDialog.Builder(context)
        .title(title)
        .inputRange(minLength,maxLength)
        .inputType(InputType.TYPE_CLASS_TEXT )
        .input("请输入",message, new MaterialDialog.InputCallback() {
          @Override
          public void onInput(MaterialDialog dialog, CharSequence input) {
            dialog.dismiss();
            if(null != callBack){
              callBack.call(input.toString());
            }
          }
        }).show();
  }

  public  static  void   showEditDialog(@NonNull Context context, String title, String message,
      final AppCallBack<String> callBack){
     new MaterialDialog.Builder(context)
        .title(title)
        .inputRange(0,100)
        .inputType(InputType.TYPE_CLASS_TEXT )

        .input("请输入",message,true, new MaterialDialog.InputCallback() {
          @Override
          public void onInput(MaterialDialog dialog, CharSequence input) {
            dialog.dismiss();
            if(null != callBack){
              callBack.call(input.toString());
            }
          }
        }).show();
  }

  /**
   * 列表显示对话框
   * @param context
   * @param title
   * @param items
   */
  public  static   void   showListInfoDialog(@NonNull Context context,String  title,List<String>  items){
       new MaterialDialog.Builder(context)
           .title(title)
           .items(items)
           .build().show();
  }

  /**
   * 等待圣诞框
   */
  public static MaterialDialog showLoadDialog(@NonNull Context context, String message,
      final DialogCallBack<Boolean> callBack) {

    MaterialDialog materialDialog = new MaterialDialog.Builder(context)

        .content(TextUtils.isEmpty(message) ? "请稍候..." : message)
        .progress(true, 0)
        .dismissListener(new DialogInterface.OnDismissListener() {
          @Override public void onDismiss(DialogInterface dialog) {
            if (null != callBack) {
              callBack.call(false);
            }
          }
        })
        .cancelListener(new DialogInterface.OnCancelListener() {
          @Override public void onCancel(DialogInterface dialog) {
            if (null != callBack) {
              callBack.call(true);
            }
          }
        })
        .show();
    return materialDialog;
  }

  public static void hideLoadDialog(Dialog materialDialog) {
    try {
      if (null != materialDialog && materialDialog.isShowing()) {
        materialDialog.dismiss();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
