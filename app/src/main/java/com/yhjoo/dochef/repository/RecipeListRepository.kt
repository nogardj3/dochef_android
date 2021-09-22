package com.yhjoo.dochef.repository

import android.content.Context
import androidx.annotation.WorkerThread
import com.yhjoo.dochef.App
import com.yhjoo.dochef.R
import com.yhjoo.dochef.db.DataGenerator
import com.yhjoo.dochef.model.Recipe
import com.yhjoo.dochef.model.RecipeDetail
import com.yhjoo.dochef.utilities.RetrofitBuilder
import com.yhjoo.dochef.utilities.RetrofitServices
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.Response
import java.util.*

class RecipeListRepository(
    private val context: Context
) {
    companion object {
        object SEARCHBY {
            const val ALL = 0
            const val USERID = 1
            const val INGREDIENT = 2
            const val RECIPENAME = 3
            const val TAG = 4
        }
    }

    private val recipeClient =
        RetrofitBuilder.create(context, RetrofitServices.RecipeService::class.java)

    @WorkerThread
    suspend fun getRecipeDetail(recipeId: Int): Flow<Response<RecipeDetail>> {
        return flow {
            if (App.isServerAlive)
                emit(recipeClient.getRecipeDetail(recipeId))
            else
                emit(
                    Response.success(
                        DataGenerator.make(
                            context.resources,
                            context.resources.getInteger(R.integer.DATA_TYPE_RECIPE_DETAIL)
                        )
                    )
                )
        }
    }

    @WorkerThread
    suspend fun getRecipeList(
        searchby: Int,
        sort: String,
        searchValue: String?
    ): Flow<Response<ArrayList<Recipe>>> {
        return flow {
            if (App.isServerAlive) {
                when (searchby) {
                    SEARCHBY.USERID -> emit(recipeClient.getRecipeByUserID(searchValue!!, sort))
                    SEARCHBY.INGREDIENT -> emit(
                        recipeClient.getRecipeByIngredient(
                            searchValue!!,
                            sort
                        )
                    )
                    SEARCHBY.RECIPENAME -> emit(recipeClient.getRecipeByName(searchValue!!, sort))
                    SEARCHBY.TAG -> emit(recipeClient.getRecipeByTag(searchValue!!, sort))
                    else -> emit(recipeClient.getRecipes(sort))
                }
            } else
                emit(
                    Response.success(
                        DataGenerator.make(
                            context.resources,
                            context.resources.getInteger(R.integer.DATE_TYPE_RECIPE)
                        )
                    )
                )
        }
    }
}