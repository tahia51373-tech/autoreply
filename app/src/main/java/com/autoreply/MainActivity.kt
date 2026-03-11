package com.autoreply

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: RuleAdapter
    private val rules = mutableListOf<ReplyRule>()
    private lateinit var serviceStatusText: TextView
    private lateinit var toggleButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.recyclerView)
        serviceStatusText = findViewById(R.id.serviceStatusText)
        toggleButton = findViewById(R.id.toggleButton)
        val addButton: FloatingActionButton = findViewById(R.id.addButton)

        // 加载规则
        loadRules()

        // 设置 RecyclerView
        adapter = RuleAdapter(rules) { rule -> deleteRule(rule) }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // 添加规则按钮
        addButton.setOnClickListener {
            showAddRuleDialog()
        }

        // 切换服务按钮
        toggleButton.setOnClickListener {
            if (isServiceEnabled()) {
                showAccessibilitySettings()
            } else {
                showAccessibilitySettings()
            }
        }

        updateServiceStatus()
    }

    override fun onResume() {
        super.onResume()
        updateServiceStatus()
    }

    private fun loadRules() {
        rules.clear()
        val file = File(filesDir, "rules.txt")
        if (file.exists()) {
            file.readLines().forEach { line ->
                val parts = line.split(":::", limit = 2)
                if (parts.size == 2) {
                    rules.add(ReplyRule(parts[0], parts[1]))
                }
            }
        }
    }

    private fun saveRules() {
        val file = File(filesDir, "rules.txt")
        file.writeText(rules.joinToString("\n") { "${it.keyword}:::${it.reply}" })
    }

    private fun deleteRule(rule: ReplyRule) {
        rules.remove(rule)
        saveRules()
        adapter.notifyDataSetChanged()
        Toast.makeText(this, "规则已删除", Toast.LENGTH_SHORT).show()
    }

    private fun showAddRuleDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_rule, null)
        val keywordEdit = dialogView.findViewById<EditText>(R.id.keywordEdit)
        val replyEdit = dialogView.findViewById<EditText>(R.id.replyEdit)

        AlertDialog.Builder(this)
            .setTitle("添加回复规则")
            .setView(dialogView)
            .setPositiveButton("保存") { _, _ ->
                val keyword = keywordEdit.text.toString().trim()
                val reply = replyEdit.text.toString().trim()
                if (keyword.isNotEmpty() && reply.isNotEmpty()) {
                    rules.add(ReplyRule(keyword, reply))
                    saveRules()
                    adapter.notifyDataSetChanged()
                    Toast.makeText(this, "规则已添加", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "请填写关键词和回复内容", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun isServiceEnabled(): Boolean {
        val am = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        return enabledServices.any {
            it.resolveInfo.serviceInfo.packageName == packageName
        }
    }

    private fun updateServiceStatus() {
        val enabled = isServiceEnabled()
        serviceStatusText.text = if (enabled) {
            getString(R.string.service_enabled)
        } else {
            getString(R.string.service_disabled)
        }
        toggleButton.text = if (enabled) "前往设置" else "开启服务"
    }

    private fun showAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivity(intent)
        Toast.makeText(this, "请找到 AutoReply 并开启服务", Toast.LENGTH_LONG).show()
    }

    // 静态方法，供服务调用
    companion object {
        fun getRules(context: Context): List<ReplyRule> {
            val rules = mutableListOf<ReplyRule>()
            val file = File(context.filesDir, "rules.txt")
            if (file.exists()) {
                file.readLines().forEach { line ->
                    val parts = line.split(":::", limit = 2)
                    if (parts.size == 2) {
                        rules.add(ReplyRule(parts[0], parts[1]))
                    }
                }
            }
            return rules
        }
    }
}

data class ReplyRule(val keyword: String, val reply: String)
