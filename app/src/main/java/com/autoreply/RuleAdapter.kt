package com.autoreply

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class RuleAdapter(
    private val rules: List<ReplyRule>,
    private val onDelete: (ReplyRule) -> Unit
) : RecyclerView.Adapter<RuleAdapter.RuleViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RuleViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_2, parent, false)
        return RuleViewHolder(view)
    }

    override fun onBindViewHolder(holder: RuleViewHolder, position: Int) {
        val rule = rules[position]
        holder.keywordText.text = "关键词：${rule.keyword}"
        holder.replyText.text = "回复：${rule.reply}"
        
        // 添加删除按钮
        if (holder.deleteButton == null) {
            val deleteBtn = ImageButton(holder.itemView.context).apply {
                setImageResource(android.R.drawable.ic_menu_delete)
                setOnClickListener {
                    onDelete(rule)
                }
            }
            // 简单实现：长按删除
            holder.itemView.setOnLongClickListener {
                onDelete(rule)
                true
            }
        }
    }

    override fun getItemCount() = rules.size

    class RuleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val keywordText: TextView = itemView.findViewById(android.R.id.text1)
        val replyText: TextView = itemView.findViewById(android.R.id.text2)
        var deleteButton: ImageButton? = null
    }
}
