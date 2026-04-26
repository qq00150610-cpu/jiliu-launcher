package com.jiliu.launcher.ui.home2

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.jiliu.launcher.databinding.ItemShortcutBinding

class ShortcutAdapter(
    private val shortcuts: List<Home2Fragment.ShortcutItem>,
    private val onShortcutClick: (Home2Fragment.ShortcutItem) -> Unit
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

        fun bind(shortcut: Home2Fragment.ShortcutItem) {
            binding.shortcutTitle.text = shortcut.title
            // Set icon based on shortcut.iconRes
            // binding.shortcutIcon.setImageResource(getIconResource(shortcut.iconRes))
            
            binding.root.setOnClickListener {
                onShortcutClick(shortcut)
            }
        }

        private fun getIconResource(iconRes: String): Int {
            // Map icon resource names to actual resources
            return 0
        }
    }
}
