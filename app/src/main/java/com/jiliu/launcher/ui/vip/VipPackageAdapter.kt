package com.jiliu.launcher.ui.vip

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.jiliu.launcher.databinding.ItemVipPackageBinding
import com.jiliu.launcher.model.VipPackage

/**
 * VIP套餐列表适配器
 */
class VipPackageAdapter(
    private val onPackageClick: (VipPackage) -> Unit
) : ListAdapter<VipPackage, VipPackageAdapter.PackageViewHolder>(PackageDiffCallback()) {

    private var selectedPackage: VipPackage? = null

    fun setSelectedPackage(packageInfo: VipPackage?) {
        val oldSelected = selectedPackage
        selectedPackage = packageInfo
        
        // 刷新旧选中项和新选中项
        currentList.forEachIndexed { index, item ->
            if (item.id == oldSelected?.id || item.id == packageInfo?.id) {
                notifyItemChanged(index)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PackageViewHolder {
        val binding = ItemVipPackageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PackageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PackageViewHolder, position: Int) {
        val packageInfo = getItem(position)
        holder.bind(packageInfo, packageInfo.id == selectedPackage?.id)
    }

    inner class PackageViewHolder(
        private val binding: ItemVipPackageBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(packageInfo: VipPackage, isSelected: Boolean) {
            binding.apply {
                // 套餐名称
                tvPackageName.text = packageInfo.name
                
                // 套餐描述
                tvDescription.text = packageInfo.description
                
                // 现价
                tvCurrentPrice.text = "¥${String.format("%.2f", packageInfo.currentPrice)}"
                
                // 原价（划线）
                tvOriginalPrice.text = "¥${String.format("%.2f", packageInfo.originalPrice)}"
                tvOriginalPrice.paintFlags = tvOriginalPrice.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                
                // 折扣标签
                if (packageInfo.hasDiscount) {
                    tvDiscount.visibility = View.VISIBLE
                    tvDiscount.text = packageInfo.discount
                } else {
                    tvDiscount.visibility = View.GONE
                }
                
                // 时长
                tvDuration.text = "${packageInfo.duration}天"
                
                // 功能列表
                tvFeatures.text = packageInfo.features.joinToString(" | ")
                
                // 选中状态
                if (isSelected) {
                    cardPackage.strokeWidth = 4
                    cardPackage.strokeColor = root.context.getColor(com.jiliu.launcher.R.color.vip_gold)
                    cardPackage.setCardBackgroundColor(root.context.getColor(com.jiliu.launcher.R.color.vip_card_selected))
                    ivSelected.visibility = View.VISIBLE
                } else {
                    cardPackage.strokeWidth = 2
                    cardPackage.strokeColor = root.context.getColor(com.jiliu.launcher.R.color.card_border)
                    cardPackage.setCardBackgroundColor(root.context.getColor(com.jiliu.launcher.R.color.card_background))
                    ivSelected.visibility = View.GONE
                }
                
                // 点击事件
                root.setOnClickListener {
                    onPackageClick(packageInfo)
                }
            }
        }
    }

    class PackageDiffCallback : DiffUtil.ItemCallback<VipPackage>() {
        override fun areItemsTheSame(oldItem: VipPackage, newItem: VipPackage): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: VipPackage, newItem: VipPackage): Boolean {
            return oldItem == newItem
        }
    }
}
