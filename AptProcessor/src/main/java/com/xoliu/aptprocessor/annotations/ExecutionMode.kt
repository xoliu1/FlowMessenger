package com.xoliu.aptprocessor.annotations

/***
 * 线程模式的枚举类定义
 * 同步、主线程、后台线程
 * @param null
 * @return
 * @author xoliu
 * @create 24-7-25
 **/
enum class ExecutionMode() {
    INSTANT,
    MAIN_THREAD,
    BACKGROUND_THREAD
}
