package com.akingyin.rfidwgs.util;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.ObservableSource;
import io.reactivex.ObservableTransformer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import org.reactivestreams.Subscription;

/**
 * @ Description:
 * @ Author king
 * @ Date 2016/12/30 10:49
 * @ Version V1.0
 */

public class RxUtil {


  public static void unsubscribe(Subscription subscription) {
    if (subscription != null ) {
      subscription.cancel();
    }
  }



  /**
   * 简化线程转换(消费事件会在UI线程)
   * @param <T>
   * @return
   */
  public static <T> ObservableTransformer<T, T> IO_Main() {
    return new ObservableTransformer<T, T>() {
      @Override public ObservableSource<T> apply(Observable<T> upstream) {
        return upstream.subscribeOn(Schedulers.io()).unsubscribeOn(Schedulers.io())

               .observeOn(AndroidSchedulers.mainThread());
      }
    };
  }

  /**
   * 线程转换全是IO线程
   * @param <T>
   * @return
   */
  public static <T> ObservableTransformer<T, T> IO_IO() {
    return new ObservableTransformer<T, T>() {
      @Override public ObservableSource<T> apply(Observable<T> upstream) {
        return upstream.subscribeOn(Schedulers.io()).unsubscribeOn(Schedulers.io())
            .observeOn(Schedulers.io());
      }
    };
  }


  /**
   * 生成Observable
   *
   * @param <T>
   * @return
   */
  public static <T> Observable<T> createData(final T t) {
    return Observable.create(new ObservableOnSubscribe<T>() {
      @Override public void subscribe(ObservableEmitter<T> subscriber) throws Exception {
        try {
          subscriber.onNext(t);
          subscriber.onComplete();
        }catch (Exception e){
          e.printStackTrace();
          subscriber.onError(e.getCause());
        }
      }
    });
  }






  public  void   test(){
   Disposable disposable = Observable.just(1)
        .compose(RxUtil.<Integer>IO_Main())
        .map(new Function<Integer, String >() {
          @Override public String apply(Integer integer) throws Exception {
            return null;
          }
        }).subscribe(new Consumer<String>() {
      @Override public void accept(String s) throws Exception {

      }
    }, new Consumer<Throwable>() {
      @Override public void accept(Throwable throwable) throws Exception {

      }
    });
  }

}
