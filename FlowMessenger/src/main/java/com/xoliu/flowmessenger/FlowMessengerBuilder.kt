package com.xoliu.flowmessenger

import com.xoliu.aptprocessor.annotations.MethodInvoker
import com.xoliu.flowmessengers.MsgHub.FlowMessenger

class FlowMessengerBuilder {
    var methodInvoker: MethodInvoker? = null

    fun setMethodHandle(aptInvoke: MethodInvoker): FlowMessengerBuilder {
        this.methodInvoker = aptInvoke
        return this
    }

    fun build(): FlowMessenger {
        return FlowMessenger.getInstance(this)
    }
}
