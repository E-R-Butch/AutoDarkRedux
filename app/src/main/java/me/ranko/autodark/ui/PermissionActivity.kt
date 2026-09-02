package me.ranko.autodark.ui

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewTreeObserver
import android.view.animation.AnimationUtils
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.snackbar.Snackbar
import me.ranko.autodark.AutoDarkApplication
import me.ranko.autodark.R
import me.ranko.autodark.Utils.CircularAnimationUtil
import me.ranko.autodark.core.LoadStatus
import me.ranko.autodark.core.ShizukuApi
import me.ranko.autodark.core.ShizukuStatus
import me.ranko.autodark.databinding.ActivityPermissionBinding
import me.ranko.autodark.databinding.ContentPermissionShizukuCardBinding

/**
 * Activity that shows an instruction for granting [Manifest.permission.WRITE_SECURE_SETTINGS].
 */
class PermissionActivity : BaseListActivity(), ViewTreeObserver.OnGlobalLayoutListener {
    private lateinit var binding: ActivityPermissionBinding
    private var shizukuBinding: ContentPermissionShizukuCardBinding? = null

    /** Coordinates that circle animation starts from. */
    private var coordinate: IntArray? = null

    private var shizukuDialog: AlertDialog? = null

    private val viewModel: PermissionViewModel by lazy(LazyThreadSafetyMode.NONE) {
        ViewModelProvider(this, PermissionViewModel.Companion.Factory(application))[
            PermissionViewModel::class.java
        ]
    }

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        coordinate = intent.getIntArrayExtra(ARG_COORDINATE)
        if (coordinate != null) {
            overridePendingTransition(R.anim.do_not_move, R.anim.do_not_move)
        }

        binding = ActivityPermissionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        lifecycle.addObserver(viewModel)
        super.onCreate(savedInstanceState)
        viewModel.registerPermissionPre11(this)

        initShizukuCard()
        observePermissionState()

        viewModel.permissionResult.observe(this) { result ->
            if (result) {
                finish()
            } else {
                Snackbar.make(binding.coordRoot, R.string.permission_failed, Snackbar.LENGTH_SHORT).show()
            }
        }

        if (savedInstanceState == null && coordinate != null) {
            val viewTreeObserver = binding.coordRoot.viewTreeObserver
            if (viewTreeObserver.isAlive) {
                viewTreeObserver.addOnGlobalLayoutListener(this)
            }
        } else {
            showRootView()
        }
    }

    private fun observePermissionState() {
        viewModel.sudoJobStatus.observe(this) { status ->
            updateProgress(binding.content.btnRoot, binding.content.progressRoot, status)
        }
        viewModel.adbJobStatus.observe(this) { status ->
            updateProgress(binding.content.btnAdb, binding.content.progressAdb, status)
        }
        viewModel.shizukuJobStatus.observe(this) { status ->
            shizukuBinding?.let { updateProgress(it.btnShizuku, it.progressShizuku, status) }
        }
    }

    private fun updateProgress(button: TextView, progress: ProgressBar, status: Int?) {
        val running = status == LoadStatus.START
        button.visibility = if (running) View.GONE else View.VISIBLE
        progress.visibility = if (running) View.VISIBLE else View.GONE
    }

    override fun getRootView(): View = binding.coordRoot

    override fun getListView(): View = binding.content.permissionRoot

    override fun getAppbar(): View = binding.appbarPermission

    /** Called when the Shizuku grant button is clicked. */
    private fun onShizukuClick(@Suppress("UNUSED_PARAMETER") view: View?) {
        when (viewModel.status.value ?: ShizukuStatus.DEAD) {
            ShizukuStatus.DEAD -> showShizukuDeadDialog()
            ShizukuStatus.NOT_INSTALL -> {
                Snackbar.make(
                    binding.coordRoot,
                    R.string.shizuku_not_install,
                    Snackbar.LENGTH_SHORT
                ).show()
            }
            ShizukuStatus.UNAUTHORIZED -> viewModel.requestPermission()
            ShizukuStatus.AVAILABLE -> viewModel.grantWithShizuku()
        }
    }

    private fun showShizukuDeadDialog() {
        if (shizukuDialog == null) {
            shizukuDialog = ShizukuApi.buildShizukuDeadDialog(this)
        }
        shizukuDialog?.show()
    }

    override fun onGlobalLayout() {
        binding.coordRoot.viewTreeObserver.removeOnGlobalLayoutListener(this)
        val animator = CircularAnimationUtil.buildAnimator(coordinate!!, binding.coordRoot)
        showRootView()
        animator.duration = resources.getInteger(android.R.integer.config_mediumAnimTime).toLong()
        animator.start()
    }

    private fun initShizukuCard() {
        val inflated = binding.content.stubShizukuFirst.inflate()
        shizukuBinding = ContentPermissionShizukuCardBinding.bind(inflated)
        val card = requireNotNull(shizukuBinding)
        val isSui = AutoDarkApplication.isSui
        card.root.setTitle(if (isSui) R.string.sui_title else R.string.shizuku_title)
        card.root.iconColor = if (isSui) ShizukuApi.SUI_COLOR else ShizukuApi.SHIZUKU_COLOR
        card.root.description = getString(R.string.shizuku_description, card.root.title)
        card.btnShizuku.setOnClickListener(::onShizukuClick)

        binding.content.btnAdb.setOnClickListener { viewModel.onAdbCheck() }
        binding.content.btnRoot.setOnClickListener { viewModel.grantWithRoot() }
        binding.content.btnSend.setOnClickListener(PermissionViewModel.shareAdbCommand)

        if (AutoDarkApplication.isSui) {
            // Remove the legacy root permission card when Sui is available.
            binding.content.getRoot().removeView(binding.content.root)
        }

        if (ShizukuApi.isShizukuInstalled(this)) {
            val rotate = AnimationUtils.loadAnimation(this, R.anim.rotate_infinite)
            card.root.titleIcon.startAnimation(rotate)
        }

        // Keep the unified Shizuku route as the primary UI. The fallback code remains available.
        binding.content.adb.visibility = View.GONE
        binding.content.root.visibility = View.GONE
    }

    private fun showRootView() {
        binding.coordRoot.visibility = View.VISIBLE
    }

    override fun onDestroy() {
        shizukuBinding?.root?.titleIcon?.clearAnimation()
        shizukuDialog?.dismiss()
        super.onDestroy()
    }

    companion object {
        private const val ARG_COORDINATE = "ARG_COORDINATE"

        /** Launch this activity for requesting permission from user. */
        fun startWithAnimationForResult(
            startView: View,
            launcher: ActivityResultLauncher<Intent>
        ) {
            val intent = Intent(startView.context, PermissionActivity::class.java)
            intent.putExtra(ARG_COORDINATE, CircularAnimationUtil.getViewCenterCoordinate(startView))
            launcher.launch(intent)
        }
    }
}