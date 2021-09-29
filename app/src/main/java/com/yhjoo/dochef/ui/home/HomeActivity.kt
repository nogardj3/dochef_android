package com.yhjoo.dochef.ui.home

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.view.*
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.input.getInputField
import com.afollestad.materialdialogs.input.input
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.canhub.cropper.CropImage
import com.canhub.cropper.CropImageView
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.yhjoo.dochef.App
import com.yhjoo.dochef.GlideApp
import com.yhjoo.dochef.R
import com.yhjoo.dochef.RECIPE
import com.yhjoo.dochef.data.model.Recipe
import com.yhjoo.dochef.data.model.UserDetail
import com.yhjoo.dochef.data.network.RetrofitBuilder
import com.yhjoo.dochef.data.network.RetrofitServices.*
import com.yhjoo.dochef.data.repository.*
import com.yhjoo.dochef.databinding.HomeActivityBinding
import com.yhjoo.dochef.ui.base.BaseActivity
import com.yhjoo.dochef.ui.follow.FollowListActivity
import com.yhjoo.dochef.ui.post.PostDetailActivity
import com.yhjoo.dochef.ui.recipe.RecipeDetailActivity
import com.yhjoo.dochef.ui.recipe.RecipeMyListActivity
import com.yhjoo.dochef.utils.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

class HomeActivity : BaseActivity() {
    companion object {
        const val CODE_PERMISSION = 22
        const val IMG_WIDTH = 360
        const val IMG_HEIGHT = 360
    }

    object UIMODE {
        const val OWNER = 0
        const val OTHERS = 1
    }

    object OPERATION {
        const val VIEW = 0
        const val REVISE = 1
    }

    private val binding: HomeActivityBinding by lazy {
        DataBindingUtil.setContentView(this, R.layout.home_activity)
    }
    private val homeViewModel: HomeViewModel by viewModels {
        HomeViewModelFactory(
            UserRepository(applicationContext),
            RecipeRepository(applicationContext),
            PostRepository(applicationContext),
            AccountRepository(applicationContext),
        )
    }
    private lateinit var recipeListAdapter: RecipeListAdapter
    private lateinit var postListAdapter: PostListAdapter

    private lateinit var storageReference: StorageReference

    private lateinit var reviseMenu: MenuItem
    private lateinit var okMenu: MenuItem

    private var currentMode: Int? = null
    private var currentOperation: Int? = null

    private lateinit var originUserInfo: UserDetail

    private var imageUri: Uri? = null
    private var imageUrl: String? = null
    private var targetUserID: String? = null
    private var activeUserID: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setSupportActionBar(binding.homeToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        storageReference = FirebaseStorage.getInstance().reference

        targetUserID = DatastoreUtil.getUserBrief(this).userID
        if (intent.getStringExtra("userID") == null || intent.getStringExtra("userID") == targetUserID) {
            currentMode = UIMODE.OWNER
            activeUserID = targetUserID
        } else {
            currentMode = UIMODE.OTHERS
            activeUserID = intent.getStringExtra("userID")
        }

        binding.apply {
            lifecycleOwner = this@HomeActivity

            recipeListAdapter = RecipeListAdapter(
                activeUserID
            ) { item ->
                Intent(this@HomeActivity, RecipeDetailActivity::class.java)
                    .putExtra("recipeID", item.recipeID).apply {
                        startActivity(this)
                    }
            }

            postListAdapter = PostListAdapter(
                { // 다른 사람 HomeActivity로 가기
//                        item ->
//                    Intent(this@HomeActivity, HomeActivity::class.java)
//                        .putExtra("postID", item.postID).apply {
//                            startActivity(intent)
//                        }
                },
                { item ->
                    Intent(this@HomeActivity, PostDetailActivity::class.java)
                        .putExtra("postID", item.postID).apply {
                            startActivity(intent)
                        }
                }
            )

            homeRecipeRecycler.apply {
                layoutManager =
                    LinearLayoutManager(this@HomeActivity, LinearLayoutManager.HORIZONTAL, false)
                adapter = recipeListAdapter
            }

            homePostRecycler.apply {
                layoutManager =
                    object : LinearLayoutManager(this@HomeActivity) {
                        override fun canScrollHorizontally(): Boolean {
                            return false
                        }

                        override fun canScrollVertically(): Boolean {
                            return false
                        }
                    }
                adapter = postListAdapter
            }

            homeViewModel.targetUserId.value = targetUserID
            homeViewModel.activeUserId.value = activeUserID

            homeViewModel.targetUserId.observe(this@HomeActivity, {
                homeViewModel.requestActiveUserDetail()
                homeViewModel.requestRecipeList()
                homeViewModel.requestPostListById()
            })

            homeViewModel.userDetail.observe(this@HomeActivity, { item ->
                originUserInfo = item
                setUserInfo(item)
            })

            homeViewModel.allRecipes.observe(this@HomeActivity, {
                recipeListAdapter.submitList(it) {}
            })

            homeViewModel.allPosts.observe(this@HomeActivity, {
                postListAdapter.submitList(it) {}
            })

            homeViewModel.updateComplete.observe(this@HomeActivity, { result->
                if (result) {
                    App.showToast("업데이트 되었습니다.")
                    currentOperation = OPERATION.VIEW
                    reviseMenu.isVisible = true
                    okMenu.isVisible = false
                    binding.homeRevisegroup.isVisible = true
                    imageUri = null
                    progressOFF()

                    homeViewModel.requestActiveUserDetail()
                }
            })

            homeViewModel.nicknameValid.observe(this@HomeActivity, { result->
                if (result.first) {
                    binding.homeNickname.text = result.second
                } else App.showToast("이미 존재합니다.")
            })
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (currentMode == UIMODE.OWNER) {
            menuInflater.inflate(R.menu.menu_home_owner, menu)
            reviseMenu = menu.findItem(R.id.menu_home_owner_revise)
            okMenu = menu.findItem(R.id.menu_home_owner_revise_ok)
        }

        return super.onCreateOptionsMenu(menu)
    }

    override fun onBackPressed() {
        if (currentOperation == OPERATION.REVISE) {
            MaterialDialog(this).show {
                positiveButton(text = "확인") {
                    currentOperation = OPERATION.VIEW
                    reviseMenu.isVisible = true
                    okMenu.isVisible = false
                    binding.apply {
                        homeRevisegroup.isVisible = false
                        ImageLoaderUtil.loadUserImage(
                            this@HomeActivity,
                            originUserInfo.userImg,
                            homeUserimg
                        )
                        homeNickname.text = originUserInfo.nickname
                        homeProfiletext.text = originUserInfo.profileText
                    }
                }
                negativeButton(text = "취소")
            }
        } else super.onBackPressed()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_home_owner_revise -> {
                currentOperation = OPERATION.REVISE
                reviseMenu.isVisible = false
                okMenu.isVisible = true
                binding.homeRevisegroup.isVisible = true
                true
            }
            R.id.menu_home_owner_revise_ok -> {
                updateProfile()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            val result = CropImage.getActivityResult(data)
            if (resultCode == RESULT_OK) {
                imageUri = result!!.originalUri
                OtherUtil.log(imageUri.toString())
                GlideApp.with(this)
                    .load(imageUri)
                    .circleCrop()
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .into(binding.homeUserimg)
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                val error = result!!.error
                error!!.printStackTrace()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CODE_PERMISSION) {
            for (result in grantResults) if (result == PackageManager.PERMISSION_DENIED) {
                App.showToast("권한 거부")
                return
            }
            CropImage.activity(imageUri)
                .setGuidelines(CropImageView.Guidelines.ON)
                .setAspectRatio(1, 1)
                .setCropShape(CropImageView.CropShape.OVAL)
                .setRequestedSize(IMG_WIDTH, IMG_HEIGHT)
                .setMaxCropResultSize(IMG_WIDTH, IMG_HEIGHT)
                .setOutputUri(imageUri)
                .start(this)
        }
    }

    private fun setUserInfo(userDetail: UserDetail) {
        binding.apply {
            ImageLoaderUtil.loadUserImage(
                this@HomeActivity,
                userDetail.userImg,
                homeUserimg
            )
            homeToolbar.title = userDetail.nickname
            homeNickname.text = userDetail.nickname
            homeProfiletext.text = userDetail.profileText
            homeRecipecount.text = userDetail.recipeCount.toString()
            homeFollowercount.text = userDetail.followerCount.toString()
            homeFollowingcount.text = userDetail.followingCount.toString()
            homeFollowBtn.isVisible = currentMode == UIMODE.OTHERS

            homeUserimgRevise.setOnClickListener { reviseProfileImage() }
            homeNicknameRevise.setOnClickListener { clickReviseNickname() }
            homeProfiletextRevise.setOnClickListener { clickReviseContents() }

            homeRecipewrapper.setOnClickListener {
                val intent = Intent(this@HomeActivity, RecipeMyListActivity::class.java)
                    .putExtra("userID", userDetail.userID)
                startActivity(intent)
            }
            homeFollowerwrapper.setOnClickListener {
                val intent = Intent(this@HomeActivity, FollowListActivity::class.java)
                    .putExtra("MODE", FollowListActivity.UIMODE.FOLLOWER)
                    .putExtra("userID", userDetail.userID)
                startActivity(intent)
            }
            homeFollowingwrapper.setOnClickListener {
                val intent = Intent(this@HomeActivity, FollowListActivity::class.java)
                    .putExtra("MODE", FollowListActivity.UIMODE.FOLLOWING)
                    .putExtra("userID", userDetail.userID)
                startActivity(intent)
            }
        }
    }

    private fun reviseProfileImage() {
        val permissions = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)

        if (OtherUtil.checkPermission(this, permissions)) {
            CropImage.activity()
                .setGuidelines(CropImageView.Guidelines.ON)
                .setAspectRatio(1, 1)
                .setOutputUri(imageUri)
                .setCropShape(CropImageView.CropShape.OVAL)
                .setRequestedSize(IMG_WIDTH, IMG_HEIGHT)
                .setMaxCropResultSize(IMG_WIDTH, IMG_HEIGHT)
                .start(this)
        } else ActivityCompat.requestPermissions(this, permissions, CODE_PERMISSION)
    }

    private fun clickReviseNickname() {
        MaterialDialog(this).show {
            noAutoDismiss()
            title(text = "닉네임 변경")
            input(hint = "닉네임", prefill = binding.homeNickname.text.toString())
            positiveButton(text = "확인", click = {
                when {
                    it.getInputField().text == null ->
                        App.showToast("닉네임을 입력 해 주세요.")
                    it.getInputField().text.length > 12 ->
                        App.showToast("12자 이하 입력 해 주세요.")
                    else ->
                        homeViewModel.checkNickname(it.getInputField().text.toString())
                }
            })
            negativeButton(text = "취소")
        }
    }

    private fun clickReviseContents() {
        MaterialDialog(this).show {
            noAutoDismiss()
            title(text = "프로필 변경")
            input(
                hint = "닉네임",
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE,
                prefill = binding.homeProfiletext.text.toString()
            )
            positiveButton(text = "확인", click = {
                when {
                    it.getInputField().text == null ->
                        App.showToast("프로필을 입력 해 주세요.")
                    it.getInputField().text.length > 60 ->
                        App.showToast("60자 이하 입력 해 주세요.")
                    it.getInputField().lineCount > 4 ->
                        App.showToast("4줄 이하 입력 해 주세요.")
                    else -> {
                        binding.homeProfiletext.text = it.getInputField().text
                        it.dismiss()
                    }
                }
            })
            negativeButton(text = "취소")
        }
    }

    private fun updateProfile() {
        progressON(this)
        if (imageUri != null) {
            imageUrl = String.format(
                getString(R.string.format_upload_file),
                activeUserID, System.currentTimeMillis().toString()
            )
            val ref = storageReference.child(getString(R.string.storage_path_profile) + imageUrl)
            ref.putFile(imageUri!!)
                .addOnSuccessListener { updateToServer() }
        } else {
            imageUrl = originUserInfo.userImg
            updateToServer()
        }
    }

    private fun updateToServer() {
        homeViewModel.updateUser(
            imageUrl!!,
            binding.homeNickname.text.toString(),
            binding.homeProfiletext.text.toString()
        )
    }
}