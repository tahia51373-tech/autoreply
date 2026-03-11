package com.autoreply

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Notification
import android.content.Intent
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import androidx.core.app.NotificationCompat

class AutoReplyService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private val processedNotifications = mutableSetOf<String>()
    private var isReplying = false  // 防止重复回复

    override fun onServiceConnected() {
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED or
                        AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                        AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS or
                   AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                   AccessibilityServiceInfo.FLAG_REQUEST_TOUCH_EXPLORATION_MODE
            notificationTimeout = 100
        }
        serviceInfo = info
        startForeground(1, createNotification())
        Toast.makeText(this, "AutoReply 服务已启动", Toast.LENGTH_SHORT).show()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        
        if (event.eventType == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) {
            handleNotification(event)
        }
    }

    private fun handleNotification(event: AccessibilityEvent) {
        if (isReplying) return  // 正在回复中，忽略新通知
        
        val packageName = event.packageName?.toString() ?: return
        
        // 只处理微信
        if (packageName != "com.tencent.mm") return

        // 获取通知内容
        val text = event.text.joinToString(" ") { it?.toString() ?: "" }
        if (text.isEmpty()) return

        // 避免重复处理同一条通知
        val notificationKey = "${packageName}:${text.hashCode()}"
        if (processedNotifications.contains(notificationKey)) return
        processedNotifications.add(notificationKey)
        
        // 清理旧记录（保留最近 50 条）
        if (processedNotifications.size > 50) {
            processedNotifications.clear()
        }

        // 延迟一点处理，确保通知内容完整
        handler.postDelayed({
            checkAndAutoReply(text)
        }, 1500)
    }

    private fun checkAndAutoReply(messageText: String) {
        val rules = MainActivity.getRules(this)
        
        for (rule in rules) {
            if (messageText.contains(rule.keyword, ignoreCase = true)) {
                // 匹配到规则，开始自动回复
                Toast.makeText(this, "匹配：${rule.keyword} → 自动回复", Toast.LENGTH_SHORT).show()
                
                // 执行自动回复流程
                autoReplyToWeChat(rule.reply)
                break
            }
        }
    }

    private fun autoReplyToWeChat(replyText: String) {
        isReplying = true
        
        handler.post {
            try {
                // 步骤 1: 打开微信
                openWeChat()
                
                // 步骤 2: 等待微信打开
                handler.postDelayed({
                    // 步骤 3: 找输入框并输入文字
                    val inputSuccess = inputText(replyText)
                    
                    if (inputSuccess) {
                        // 步骤 4: 等待输入完成
                        handler.postDelayed({
                            // 步骤 5: 点击发送
                            val sendSuccess = clickSendButton()
                            
                            if (sendSuccess) {
                                Toast.makeText(this@AutoReplyService, "自动回复成功！", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(this@AutoReplyService, "发送失败，请手动发送", Toast.LENGTH_SHORT).show()
                            }
                            
                            // 步骤 6: 返回桌面
                            handler.postDelayed({
                                returnToHome()
                                isReplying = false
                            }, 500)
                        }, 1000)
                    } else {
                        Toast.makeText(this@AutoReplyService, "未找到输入框", Toast.LENGTH_SHORT).show()
                        isReplying = false
                    }
                }, 2000)
                
            } catch (e: Exception) {
                Toast.makeText(this, "自动回复出错：${e.message}", Toast.LENGTH_SHORT).show()
                isReplying = false
            }
        }
    }

    private fun openWeChat() {
        val intent = packageManager.getLaunchIntentForPackage("com.tencent.mm")
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            Toast.makeText(this, "正在打开微信...", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "未找到微信", Toast.LENGTH_SHORT).show()
        }
    }

    private fun inputText(text: String): Boolean {
        try {
            val rootNode = rootInActiveWindow ?: return false
            
            // 查找可编辑的输入框
            val editText = findEditableNode(rootNode)
            
            if (editText != null) {
                // 点击输入框，弹出输入法
                editText.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                
                // 等待输入法弹出
                Thread.sleep(500)
                
                // 输入文字
                val arguments = Bundle().apply {
                    putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
                }
                editText.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
                
                Toast.makeText(this, "已输入：$text", Toast.LENGTH_SHORT).show()
                return true
            }
            
            return false
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    private fun findEditableNode(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        // 检查当前节点是否可编辑
        if (node.isEditable) {
            return node
        }
        
        // 递归查找子节点
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = findEditableNode(child)
            if (result != null) {
                return result
            }
        }
        
        return null
    }

    private fun clickSendButton(): Boolean {
        try {
            val rootNode = rootInActiveWindow ?: return false
            
            // 方案 1: 找文字为"发送"的按钮
            val sendNodes = rootNode.findAccessibilityNodeInfosByText("发送")
            if (sendNodes.isNotEmpty()) {
                for (node in sendNodes) {
                    if (node.isClickable) {
                        node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                        return true
                    }
                }
            }
            
            // 方案 2: 找右下角的可点击区域（备用方案）
            val screenW = resources.displayMetrics.widthPixels
            val screenH = resources.displayMetrics.heightPixels
            val sendButton = findSendButtonByPosition(rootNode, screenW, screenH)
            
            if (sendButton != null) {
                sendButton.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                return true
            }
            
            // 方案 3: 尝试按回车发送（很多 APP 支持）
            performGlobalAction(GLOBAL_ACTION_BACK)
            
            return false
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    private fun findSendButtonByPosition(node: AccessibilityNodeInfo, screenW: Int, screenH: Int): AccessibilityNodeInfo? {
        val rect = Rect()
        node.getBoundsInScreen(rect)
        
        // 检查当前节点是否在右下角区域
        if (node.isClickable && 
            rect.centerX() > screenW * 0.7 && 
            rect.centerY() > screenH * 0.7 &&
            rect.width() < screenW * 0.3 &&  // 按钮不会太大
            rect.height() < screenH * 0.2) {
            return node
        }
        
        // 递归查找子节点
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = findSendButtonByPosition(child, screenW, screenH)
            if (result != null) {
                return result
            }
        }
        
        return null
    }

    private fun returnToHome() {
        performGlobalAction(GLOBAL_ACTION_HOME)
    }

    private fun createNotification(): Notification {
        val channelId = "AutoReplyChannel"
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                channelId,
                "AutoReply 服务",
                android.app.NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
            manager.createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("AutoReply 正在运行")
            .setContentText("自动回复服务已启动")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onInterrupt() {
        // 服务中断时的处理
    }

    override fun onDestroy() {
        super.onDestroy()
        Toast.makeText(this, "AutoReply 服务已停止", Toast.LENGTH_SHORT).show()
    }
}
