package com.yhjoo.dochef.ui.notification

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.yhjoo.dochef.App
import com.yhjoo.dochef.R
import com.yhjoo.dochef.data.entity.NotificationEntity
import com.yhjoo.dochef.databinding.NotificationActivityBinding
import com.yhjoo.dochef.ui.base.BaseActivity
import com.yhjoo.dochef.ui.home.HomeActivity
import com.yhjoo.dochef.ui.post.PostDetailActivity
import com.yhjoo.dochef.ui.recipe.RecipeDetailActivity
import com.yhjoo.dochef.utils.OtherUtil
import kotlinx.coroutines.*

class NotificationActivity : BaseActivity() {
    // TODO
    // 1. setRead Flow
    private val binding: NotificationActivityBinding by lazy {
        DataBindingUtil.setContentView(this, R.layout.notification_activity)
    }
    private val notificationViewModel: NotificationViewModel by viewModels {
        NotificationViewModelFactory((application as App).notificationRepository)
    }

    private lateinit var notificationListAdapter: NotificationListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setSupportActionBar(binding.notificationToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.apply {
            lifecycleOwner = this@NotificationActivity

            notificationListAdapter = NotificationListAdapter {
                itemClicked(it)
            }

            notificationRecycler.apply {
                layoutManager = LinearLayoutManager(this@NotificationActivity)
                adapter = notificationListAdapter
            }

            notificationViewModel.allnotifications
                .observe(this@NotificationActivity, {
                    notificationEmpty.isVisible = it.isEmpty()
                    notificationListAdapter.submitList(it) {
                        binding.notificationRecycler.scrollToPosition(0)
                    }
                })
        }
    }

    private fun itemClicked(notificationItem: NotificationEntity) {
        CoroutineScope(Dispatchers.IO).launch {
            OtherUtil.log(notificationItem.toString())

            if (App.isServerAlive) {
                notificationViewModel.setRead(notificationItem.id!!)
                val intent = when (notificationItem.type) {
                    resources.getInteger(R.integer.NOTIFICATION_TYPE_1),
                    resources.getInteger(R.integer.NOTIFICATION_TYPE_2) -> {
                        Intent(this@NotificationActivity, RecipeDetailActivity::class.java)
                            .putExtra("recipeID", notificationItem.intentData.toInt())
                    }
                    resources.getInteger(R.integer.NOTIFICATION_TYPE_3) -> {
                        Intent(this@NotificationActivity, PostDetailActivity::class.java)
                            .putExtra("postID", notificationItem.intentData.toInt())
                    }
                    else -> {
                        Intent(this@NotificationActivity, HomeActivity::class.java)
                            .putExtra("postID", notificationItem.intentData)
                    }
                }
                startActivity(intent)
            }
        }
    }
}