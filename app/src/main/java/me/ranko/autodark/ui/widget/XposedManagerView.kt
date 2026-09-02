package me.ranko.autodark.ui.widget

import android.annotation.SuppressLint
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import java.time.LocalTime
import me.ranko.autodark.BuildConfig
import me.ranko.autodark.R
import me.ranko.autodark.Utils.DarkTimeUtil
import me.ranko.autodark.Utils.ViewUtil
import me.ranko.autodark.ui.ManagerAppListAdapter

@SuppressLint("InflateParams")
class XposedManagerView(
    context: Activity,
    container: ViewGroup
) : DefaultLifecycleObserver {

    private val root: View = LayoutInflater.from(context)
        .inflate(R.layout.widget_xposed_manager, container, false)
    private lateinit var time: TextView
    private val recyclerView: RecyclerView
    private var adapter: ManagerAppListAdapter? = null
    private var activity: Activity? = context
    private var ticker: BroadcastReceiver? = null

    init {
        recyclerView = root.findViewById(R.id.recyclerView)
        adapter = ManagerAppListAdapter(context)
        adaptManagerView()

        val screenWidth = context.resources.displayMetrics.widthPixels
        val containerRoot = container.rootView
        containerRoot.addOnLayoutChangeListener(object : View.OnLayoutChangeListener {
            override fun onLayoutChange(
                v: View,
                left: Int,
                top: Int,
                right: Int,
                bottom: Int,
                oldLeft: Int,
                oldTop: Int,
                oldRight: Int,
                oldBottom: Int
            ) {
                containerRoot.removeOnLayoutChangeListener(this)
                val containerHeight = container.measuredHeight
                val containerWidth = container.measuredWidth
                if (containerWidth <= 0 || screenWidth <= 0) return

                val outScale = screenWidth / containerWidth.toFloat()
                val scaledHeight = (containerHeight * outScale).toInt()
                root.measure(
                    View.MeasureSpec.makeMeasureSpec(screenWidth, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(scaledHeight, View.MeasureSpec.EXACTLY)
                )
                root.layout(0, 0, screenWidth, scaledHeight)

                val scale = containerWidth / screenWidth.toFloat()
                root.scaleX = scale
                root.scaleY = scale
                root.pivotX = 0f
                root.pivotY = 0f
                container.addView(root, root.measuredWidth, root.measuredHeight)
                recyclerView.adapter = adapter
            }
        })
        (activity as LifecycleOwner).lifecycle.addObserver(this)
    }

    private fun adaptManagerView() {
        val statusBar: View = root.findViewById(R.id.statusBar)
        val toolbar: View = statusBar.findViewById(R.id.toolbar)
        time = statusBar.findViewById(R.id.lock_time)
        val id: TextView = toolbar.findViewById(R.id.id)
        id.text = BuildConfig.APPLICATION_ID

        val navIcon: ImageView = toolbar.findViewById(R.id.navIcon)
        val menu: ImageView = toolbar.findViewById(R.id.menu)
        navIcon.setImageDrawable(getDrawable(context = requireActivity(), res = R.drawable.ic_arrow_back))
        menu.setImageDrawable(getDrawable(context = requireActivity(), res = R.drawable.ic_more))
    }

    override fun onResume(owner: LifecycleOwner) {
        val currentActivity = activity ?: return
        if (ticker != null) return

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
        }
        ticker = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                updateTime()
            }
        }
        ContextCompat.registerReceiver(
            currentActivity,
            ticker,
            filter,
            ContextCompat.RECEIVER_EXPORTED
        )
        updateTime()
    }

    override fun onPause(owner: LifecycleOwner) {
        val currentActivity = activity ?: return
        val currentTicker = ticker ?: return
        currentActivity.unregisterReceiver(currentTicker)
        ticker = null
    }

    override fun onDestroy(owner: LifecycleOwner) {
        destroy()
    }

    private fun updateTime() {
        time.text = DarkTimeUtil.getDisplayFormattedString(LocalTime.now())
    }

    private fun requireActivity(): Activity = checkNotNull(activity)

    fun destroy() {
        val currentActivity = activity
        val currentTicker = ticker
        if (currentActivity != null && currentTicker != null) {
            currentActivity.unregisterReceiver(currentTicker)
            (currentActivity as LifecycleOwner).lifecycle.removeObserver(this)
        }
        ticker = null
        activity = null

        // Destroy static DUMMY_ICON in the adapter.
        recyclerView.adapter = null
        adapter = null
    }

    companion object {
        private fun getDrawable(context: Context, @DrawableRes res: Int): Drawable {
            val color = ViewUtil.getAttrColor(context, com.google.android.material.R.attr.colorOnSurface)
            val drawable = requireNotNull(ContextCompat.getDrawable(context, res))
            drawable.setTint(color)
            drawable.setTintMode(PorterDuff.Mode.SRC_IN)
            return drawable
        }
    }
}
