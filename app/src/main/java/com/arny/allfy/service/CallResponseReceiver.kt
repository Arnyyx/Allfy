package com.arny.allfy.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.arny.allfy.MainActivity
import com.arny.allfy.utils.Response
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class CallResponseReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
//        val callerId = intent.getStringExtra("callerId") ?: return
//        val calleeId = intent.getStringExtra("calleeId") ?: return
//        val callId = intent.getStringExtra("callId") ?: return
//        val conversationId = listOf(callerId, calleeId).sorted().joinToString("_")
//
//        val scope = CoroutineScope(Dispatchers.Main)
//        when (intent.action) {
//            "ACCEPT_CALL" -> {
//                scope.launch {
//                    context.sendBroadcast(Intent("STOP_CALL_EFFECTS"))
//                    acceptCallUseCase(conversationId, callId).collect { response ->
//                        if (response is Response.Success) {
//                            val callIntent = Intent(context, MainActivity::class.java).apply {
//                                action = "START_CALL"
//                                putExtra("callerId", callerId)
//                                putExtra("calleeId", calleeId)
//                                putExtra("callId", callId)
//                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
//                            }
//                            context.startActivity(callIntent)
//                        }
//                    }
//                }
//            }
//
//            "REJECT_CALL" -> {
//                scope.launch {
//                    context.sendBroadcast(Intent("STOP_CALL_EFFECTS"))
//                    rejectCallUseCase(conversationId, callId).collect { response ->
//                        if (response is Response.Success) {
//
//                        }
//                    }
//                }
//            }
//        }
    }
}