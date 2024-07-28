package com.xoliu.flowmessenger.invoke_strategy

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import com.xoliu.aptprocessor.annotations.MethodInvoker


/***
 * 实现订阅者的基类
 * 在此规定三种Handler，利用Handler切换线程
 * @param null
 * @return
 * @author xoliu
 * @create 24-7-25
 **/

abstract class MethodInvokeStrategy : MethodInvoker {


    var handlerThread: HandlerThread = HandlerThread("defaultThread")

    var mainHandler: Handler? = null

    var workHandler: Handler? = null

    constructor(){
        handlerThread.start()
        mainHandler = Handler(Looper.getMainLooper())
        workHandler = Handler(handlerThread.looper)
    }


}
