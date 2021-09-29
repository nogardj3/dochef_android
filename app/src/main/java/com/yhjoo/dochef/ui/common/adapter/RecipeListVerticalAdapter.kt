package com.yhjoo.dochef.ui.common.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.yhjoo.dochef.R
import com.yhjoo.dochef.data.model.Recipe
import com.yhjoo.dochef.databinding.MainMyrecipeItemBinding
import com.yhjoo.dochef.databinding.MainRecipesItemBinding
import com.yhjoo.dochef.databinding.RecipemylistItemBinding
import com.yhjoo.dochef.databinding.RecipethemeItemBinding
import com.yhjoo.dochef.utils.ImageLoaderUtil
import com.yhjoo.dochef.utils.OtherUtil
import com.yhjoo.dochef.utils.ValidateUtil

class RecipeListVerticalAdapter(
    private val layoutType: Int,
    private val activeUserID: String?,
    private val itemClickListener: ((Recipe) -> Unit)?,
    private val childClickListener: ((Recipe) -> Unit)?
) :
    ListAdapter<Recipe, RecyclerView.ViewHolder>(RecipeListComparator()) {
    companion object {
        object LayoutType {
            const val MAIN_RECIPES = 0
            const val MAIN_MYRECIPE = 1
            const val MYLIST = 2
            const val THEME = 3
        }
    }

    lateinit var context: Context

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        context = parent.context

        return when (layoutType) {
            LayoutType.MAIN_RECIPES -> MainRecipesViewHolder(
                DataBindingUtil.inflate(
                    LayoutInflater.from(context),
                    R.layout.main_recipes_item,
                    parent,
                    false
                )
            )
            LayoutType.MAIN_MYRECIPE -> MainMyRecipeViewHolder(
                DataBindingUtil.inflate(
                    LayoutInflater.from(context),
                    R.layout.main_myrecipe_item,
                    parent,
                    false
                )
            )
            LayoutType.MYLIST -> RecipeMyListViewHolder(
                DataBindingUtil.inflate(
                    LayoutInflater.from(context),
                    R.layout.recipemylist_item,
                    parent,
                    false
                )
            )
            else -> RecipeThemeViewHolder(
                DataBindingUtil.inflate(
                    LayoutInflater.from(context),
                    R.layout.recipetheme_item,
                    parent,
                    false
                )
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is MainRecipesViewHolder -> holder.bind(getItem(position))
            is MainMyRecipeViewHolder -> holder.bind(getItem(position))
            is RecipeMyListViewHolder -> holder.bind(getItem(position))
            is RecipeThemeViewHolder -> holder.bind(getItem(position))
        }
    }

    inner class MainRecipesViewHolder(val binding: MainRecipesItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(recipe: Recipe) {
            binding.apply {
                root.setOnClickListener {
                    itemClickListener!!(recipe)
                }

                ImageLoaderUtil.loadRecipeImage(
                    context,
                    recipe.recipeImg,
                    mainRecipesRecipeImg
                )
                mainRecipesTitle.text = recipe.recipeName
                mainRecipesNickname.text =
                    String.format(
                        context.resources.getString(R.string.format_usernickname),
                        recipe.nickname
                    )
                mainRecipesDate.text = OtherUtil.millisToText(recipe.datetime)
                mainRecipesRating.text = String.format("%.1f", recipe.rating)
                mainRecipesView.text = recipe.viewCount.toString()

                mainRecipesNew.isVisible = ValidateUtil.checkNew(recipe.datetime)
                mainRecipesIsmine.isVisible = activeUserID == recipe.userID
            }
        }
    }

    inner class MainMyRecipeViewHolder(val binding: MainMyrecipeItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(recipe: Recipe) {
            binding.apply {
                binding.root.setOnClickListener {
                    itemClickListener!!(recipe)
                }

                ImageLoaderUtil.loadRecipeImage(
                    context,
                    recipe.recipeImg,
                    mainMyrecipeRecipeimg
                )
                mainMyrecipeTitle.text = recipe.recipeName
                mainMyrecipeNickname.text =
                    String.format(
                        context.resources.getString(R.string.format_usernickname),
                        recipe.nickname
                    )
                mainMyrecipeDate.text = OtherUtil.millisToText(recipe.datetime)
                mainMyrecipeRating.text = String.format("%.1f", recipe.rating)
                mainMyrecipeView.text = recipe.viewCount.toString()
                mainMyrecipeIslikerecipe.isVisible = activeUserID != recipe.userID
            }
        }
    }

    inner class RecipeMyListViewHolder(val binding: RecipemylistItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(recipe: Recipe) {
            binding.apply {
                binding.root.setOnClickListener {
                    itemClickListener!!(recipe)
                }

                ImageLoaderUtil.loadRecipeImage(
                    context,
                    recipe.recipeImg,
                    recipemylistRecipeimg
                )
                recipemylistRecipetitle.text = recipe.recipeName
                recipemylistNickname.text =
                    String.format(
                        context.resources.getString(R.string.format_usernickname),
                        recipe.nickname
                    )
                recipemylistDate.text = OtherUtil.millisToText(recipe.datetime)
                recipemylistRating.text = String.format("%.1f", recipe.rating)
                recipemylistView.text = recipe.viewCount.toString()

                recipemylistIslikerecipe.isVisible = activeUserID != recipe.userID
                recipemylistIslikerecipe.setOnClickListener {
                    childClickListener!!(recipe)
                }
            }
        }
    }

    inner class RecipeThemeViewHolder(val binding: RecipethemeItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(recipe: Recipe) {
            binding.apply {
                binding.root.setOnClickListener {
                    itemClickListener!!(recipe)
                }

                ImageLoaderUtil.loadRecipeImage(
                    context, recipe.recipeImg, recipethemeImg
                )
                recipethemeTitle.text = recipe.recipeName
                recipethemeNickname.text = String.format(
                    context.resources.getString(R.string.format_usernickname),
                    recipe.nickname
                )
                recipethemeRating.text = String.format("%.1f", recipe.rating)
            }
        }
    }

    class RecipeListComparator : DiffUtil.ItemCallback<Recipe>() {
        override fun areItemsTheSame(
            oldItem: Recipe,
            newItem: Recipe
        ): Boolean {
            return oldItem.recipeID == newItem.recipeID
        }

        override fun areContentsTheSame(
            oldItem: Recipe,
            newItem: Recipe
        ): Boolean {
            return oldItem == newItem
        }
    }
}