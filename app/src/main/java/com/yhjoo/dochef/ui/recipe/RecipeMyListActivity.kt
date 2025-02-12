package com.yhjoo.dochef.ui.recipe

import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import com.afollestad.materialdialogs.MaterialDialog
import com.yhjoo.dochef.Constants
import com.yhjoo.dochef.R
import com.yhjoo.dochef.data.model.Recipe
import com.yhjoo.dochef.databinding.RecipemylistActivityBinding
import com.yhjoo.dochef.ui.base.BaseActivity
import com.yhjoo.dochef.utils.*
import dagger.hilt.android.AndroidEntryPoint
import java.util.*

@AndroidEntryPoint
class RecipeMyListActivity : BaseActivity() {
    private val binding: RecipemylistActivityBinding by lazy {
        DataBindingUtil.setContentView(this, R.layout.recipemylist_activity)
    }
    private val recipeListViewModel: RecipeMyListViewModel by viewModels()
    private lateinit var recipeMyListAdapter: RecipeMyListAdapter

    private lateinit var addMenu: MenuItem

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setSupportActionBar(binding.recipelistToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.apply {
            lifecycleOwner = this@RecipeMyListActivity

            recipeMyListAdapter = RecipeMyListAdapter(this@RecipeMyListActivity)
            recipelistRecycler.adapter = recipeMyListAdapter
        }

        recipeListViewModel.allRecipeList.observe(this@RecipeMyListActivity, {
            recipeMyListAdapter.submitList(it) {
                binding.recipemylistEmpty.isVisible = it.isEmpty()
                binding.recipelistRecycler.scrollToPosition(0)
            }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_recipe_add, menu)
        addMenu = menu.findItem(R.id.menu_recipe_add)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == R.id.menu_recipe_add) {
            startActivity(Intent(this, RecipeMakeActivity::class.java))
            true
        } else
            super.onOptionsItemSelected(item)
    }

    fun showFavoriteDialog(item: Recipe){
        OtherUtil.log("asdfasdfasdf")
        MaterialDialog(this@RecipeMyListActivity).show {
            message(text = "즐겨찾기를 해제 하시겠습니까?")
            positiveButton(text = "확인") {
                recipeListViewModel.disLikeRecipe(
                    item.recipeID,
                    recipeListViewModel.activeUserId
                )
            }
            negativeButton(text = "취소")
        }
    }

    fun goDetail(item: Recipe) {
        startActivity(
            Intent(this@RecipeMyListActivity, RecipeDetailActivity::class.java)
                .putExtra(Constants.INTENTNAME.RECIPE_ID, item.recipeID)
        )
    }
}