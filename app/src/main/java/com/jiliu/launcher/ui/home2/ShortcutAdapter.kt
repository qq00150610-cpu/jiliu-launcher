package com.jiliu.launcher.ui.home2

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.jiliu.launcher.databinding.ItemShortcutBinding

data class ShortcutItem(
    val id: String,
    val title: String,
    val iconRes: String,
    val packageName: String? = null,
    val action: String? = null
)

class ShortcutAdapter(
    private val shortcuts: List<ShortcutItem>,
    private val onShortcutClick: (ShortcutItem) -> Unit
) : RecyclerView.Adapter<ShortcutAdapter.ShortcutViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShortcutViewHolder {
        val binding = ItemShortcutBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ShortcutViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ShortcutViewHolder, position: Int) {
        holder.bind(shortcuts[position])
    }

    override fun getItemCount(): Int = shortcuts.size

    inner class ShortcutViewHolder(
        private val binding: ItemShortcutBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(shortcut: ShortcutItem) {
            binding.shortcutTitle.text = shortcut.title
            
            binding.root.setOnClickListener {
                onShortcutClick(shortcut)
            }
        }
    }
}
