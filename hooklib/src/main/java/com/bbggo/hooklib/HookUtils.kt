package com.bbggo.hooklib

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Message
import com.bbggo.hooklib.proxy.ProxyActivity
import java.lang.reflect.Field
import java.lang.reflect.Proxy

/**
 * @Description:
 * @Author: wangyuebin
 * @Date: 2021/9/23 4:52 下午
 */
class HookUtils {
    companion object {

        private const val INTENT_KEY = "target_intent"

        fun hookInit(context: Context) {
            hookAms(context)
            hookHandler()
        }

        @SuppressLint("PrivateApi", "DiscouragedPrivateApi")
        @JvmStatic
        private fun hookAms(context: Context) {
            try {
                val singletonField: Field
                val iActivityManagerClass: Class<*>
                // 1，获取Instrumentation中调用startActivity(,intent,)方法的对象
                when {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                        // 10.0以上是ActivityTaskManager中的IActivityTaskManagerSingleton
                        val activityTaskManagerClass = Class.forName("android.app.ActivityTaskManager")
                        singletonField =
                            activityTaskManagerClass.getDeclaredField("IActivityTaskManagerSingleton")
                        iActivityManagerClass = Class.forName("android.app.IActivityTaskManager")
                    }
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                        // 8.0,9.0在ActivityManager类中IActivityManagerSingleton
                        val activityManagerClass: Class<*> = ActivityManager::class.java
                        singletonField =
                            activityManagerClass.getDeclaredField("IActivityManagerSingleton")
                        iActivityManagerClass = Class.forName("android.app.IActivityManager")
                    }
                    else -> {
                        // 8.0以下在ActivityManagerNative类中 gDefault
                        val activityManagerNative = Class.forName("android.app.ActivityManagerNative")
                        singletonField = activityManagerNative.getDeclaredField("gDefault")
                        iActivityManagerClass = Class.forName("android.app.IActivityManager")
                    }
                }
                singletonField.isAccessible = true
                val singleton = singletonField[null]

                // 2，获取Singleton中的mInstance，也就是要代理的对象
                val singletonClass = Class.forName("android.util.Singleton")
                val mInstanceField = singletonClass.getDeclaredField("mInstance")
                mInstanceField.isAccessible = true
                val getMethod = singletonClass.getDeclaredMethod("get")
                val mInstance = getMethod.invoke(singleton) ?: return
                // 3，对IActivityManager进行动态代理
                val proxyInstance = Proxy.newProxyInstance(
                    Thread.currentThread().contextClassLoader, arrayOf(iActivityManagerClass)
                ) { proxy, method, args ->
                    if (method.name == "startActivity") {
                        var pos = 0
                        for (i in args.indices) {
                            if (args[i] is Intent) {
                                pos = i
                                break
                            }
                        }
                        val originIntent = args[pos] as Intent
                        val proxyIntent = Intent(originIntent)
                        proxyIntent.setClass(context, ProxyActivity::class.java)
                        proxyIntent.putExtra(INTENT_KEY, originIntent)
                        args[pos] = proxyIntent
                    }
                    if (args == null) return@newProxyInstance Unit
                    method.invoke(mInstance, *args)
                }

                // 4，把代理赋值给IActivityManager类型的mInstance对象
                mInstanceField[singleton] = proxyInstance
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        @SuppressLint("PrivateApi", "DiscouragedPrivateApi")
        @JvmStatic
        private fun hookHandler() {
            try {
                val activityThreadClz = Class.forName("android.app.ActivityThread")
                val sCurrentActivityThread =
                    activityThreadClz.getDeclaredField("sCurrentActivityThread")
                sCurrentActivityThread.isAccessible = true
                val activityThread = sCurrentActivityThread[null]
                val mHField = activityThreadClz.getDeclaredField("mH")
                mHField.isAccessible = true
                val mH = mHField[activityThread]
                val handlerSuperClz = mH.javaClass.superclass
                val mCallbackField = handlerSuperClz.getDeclaredField("mCallback")
                mCallbackField.isAccessible = true
                mCallbackField[mH] = HookCallBack()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * hook其实就是把系统原有的替换为自定义的
     */
    private class HookCallBack : Handler.Callback {
        override fun handleMessage(msg: Message): Boolean {
            when (msg.what) {
                100 -> {
                    try {
                        val intentField = msg.obj.javaClass.getDeclaredField("intent")
                        intentField.isAccessible = true
                        val proxyIntent = intentField[msg.obj] as Intent
                        val targetIntent =
                            proxyIntent.getParcelableExtra<Intent>(INTENT_KEY)
                        if (targetIntent != null) {
                            intentField[msg.obj] = targetIntent
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                159 -> {
                    try {
                        val mActivityCallbacksField =
                            msg.obj.javaClass.getDeclaredField("mActivityCallbacks")
                        mActivityCallbacksField.isAccessible = true
                        val mActivityCallbacks = mActivityCallbacksField[msg.obj] as List<*>
                        var i = 0
                        while (i < mActivityCallbacks.size) {
                            if (mActivityCallbacks[i]!!.javaClass.name
                                == "android.app.servertransaction.LaunchActivityItem"
                            ) {
                                val launchActivityItem = mActivityCallbacks[i]!!
                                val mIntentField =
                                    launchActivityItem.javaClass.getDeclaredField("mIntent")
                                mIntentField.isAccessible = true
                                val intent = mIntentField[launchActivityItem] as Intent
                                // 获取插件的
                                val proxyIntent =
                                    intent.getParcelableExtra<Intent>(INTENT_KEY)
                                //替换
                                if (proxyIntent != null) {
                                    mIntentField[launchActivityItem] = proxyIntent
                                }
                            }
                            i++
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                else -> {
                }
            }
            return false
        }
    }
}