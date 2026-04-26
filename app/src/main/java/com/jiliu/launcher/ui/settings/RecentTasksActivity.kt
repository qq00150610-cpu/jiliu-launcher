package com.jiliu.launcher.ui.settings

import android.app.ActivityManager
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.jiliu.launcher.databinding.ActivityRecentTasksBinding
import com.jiliu.launcher.databinding.ItemRecentTaskBinding

class RecentTasksActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRecentTasksBinding
    private lateinit var taskAdapter: RecentTaskAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecentTasksBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        loadRecentTasks()
    }

    private fun setupUI() {
        binding.btnClose.setOnClickListener {
            finish()
        }

        binding.btnClearAll.setOnClickListener {
            clearAllTasks()
        }

        taskAdapter = RecentTaskAdapter()

        binding.taskRecycler.apply {
            layoutManager = LinearLayoutManager(this@RecentTasksActivity)
            adapter = taskAdapter
        }
    }

    private fun loadRecentTasks() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            
            try {
                // Get recent tasks (deprecated but still works for our use case)
                val recentTasks = activityManager.appTasks
                
                val tasks = recentTasks.mapNotNull { task ->
                    try {
                        val taskInfo = task.taskInfo
                        RecentTaskInfo(
                            packageName = taskInfo.baseIntent?.component?.packageName ?: "",
                            taskId = taskInfo.id
                        )
                    } catch (e: Exception) {
                        null
                    }
                }.filter { it.packageName.isNotBlank() }
                
                taskAdapter.submitList(tasks)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        binding.tvEmpty.visibility = if (taskAdapter.itemCount == 0) View.VISIBLE else View.GONE
    }

    private fun clearAllTasks() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            
            try {
                val recentTasks = activityManager.appTasks
                for (task in recentTasks) {
                    try {
                        task.setExcludeFromRecents(true)
                        task.finishAndRemoveTask()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                
                loadRecentTasks()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    data class RecentTaskInfo(
        val packageName: String,
        val taskId: Int
    )

    inner class RecentTaskAdapter : androidx.recyclerview.widget.ListAdapter<RecentTaskInfo, RecentTaskAdapter.TaskViewHolder>(
        object : androidx.recyclerview.widget.DiffUtil.ItemCallback<RecentTaskInfo>() {
            override fun areItemsTheSame(oldItem: RecentTaskInfo, newItem: RecentTaskInfo) =
                oldItem.taskId == newItem.taskId

            override fun areContentsTheSame(oldItem: RecentTaskInfo, newItem: RecentTaskInfo) =
                oldItem == newItem
        }
    ) {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
            val binding = ItemRecentTaskBinding.inflate(
                layoutInflater,
                parent,
                false
            )
            return TaskViewHolder(binding)
        }

        override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
            holder.bind(getItem(position))
        }

        inner class TaskViewHolder(private val binding: ItemRecentTaskBinding) :
            androidx.recyclerview.widget.RecyclerView.ViewHolder(binding.root) {

            fun bind(task: RecentTaskInfo) {
                try {
                    val pm = packageManager
                    val appInfo = pm.getApplicationInfo(task.packageName, 0)
                    binding.taskIcon.setImageDrawable(appInfo.loadIcon(pm))
                    binding.taskName.text = pm.getApplicationLabel(appInfo)
                    binding.taskPackage.text = task.packageName
                } catch (e: Exception) {
                    binding.taskName.text = task.packageName
                    binding.taskPackage.text = ""
                }

                binding.btnRemove.setOnClickListener {
                    removeTask(task)
                }
            }
        }
    }

    private fun removeTask(task: RecentTasksActivity.RecentTaskInfo) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            
            try {
                val recentTasks = activityManager.appTasks
                for (t in recentTasks) {
                    if (t.taskInfo.id == task.taskId) {
                        t.finishAndRemoveTask()
                        break
                    }
                }
                
                loadRecentTasks()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
