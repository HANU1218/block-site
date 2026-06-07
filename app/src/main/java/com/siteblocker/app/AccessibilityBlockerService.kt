package com.siteblocker.app

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class AccessibilityBlockerService : AccessibilityService() {

    private var overlayView: View? = null
    private var windowManager: WindowManager? = null
    private var lastBlockedUrl = ""

    private val messages = listOf(
        "Every time you resist, you grow stronger 💪",
        "Your future self is watching. Make them proud 🌟",
        "180° change starts with this one moment 🔄",
        "Discipline today = Freedom tomorrow 🦅",
        "You are bigger than this distraction ⚡",
        "Champions choose discomfort over distraction 🏆",
        "This moment defines you. Walk away 🚶",
        "Your goals don't care about your distractions 🎯",
        "Redirect this energy into something that builds you 🔥",
        "You blocked this for a reason. Trust yourself ✅"
    )

    private val browsers = setOf(
        "com.android.chrome", "com.brave.browser", "org.mozilla.firefox",
        "com.opera.browser", "com.microsoft.emmx", "com.sec.android.app.sbrowser",
        "com.UCMobile.intl", "com.kiwibrowser.browser", "mark.via.gp",
        "com.duckduckgo.mobile.android", "com.vivaldi.browser",
        "com.google.android.apps.chrome", "org.mozilla.fenix"
    )

    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_SCREEN_OFF) removeOverlay()
        }
    }

    override fun onServiceConnected() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        registerReceiver(screenOffReceiver, IntentFilter(Intent.ACTION_SCREEN_OFF))
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val pkg = event.packageName?.toString() ?: return
        if (!browsers.contains(pkg)) {
            removeOverlay()
            lastBlockedUrl = ""
            return
        }
        val url = extractUrl() ?: return
        if (url == lastBlockedUrl) return
        lastBlockedUrl = url
        if (KeywordManager.shouldBlock(this, url)) showBlockScreen()
    }

    private fun extractUrl(): String? {
        val root = rootInActiveWindow ?: return null
        return findUrl(root)
    }

    private fun findUrl(node: AccessibilityNodeInfo?): String? {
        if (node == null) return null
        val text = node.text?.toString()
        if (!text.isNullOrBlank() && isUrl(text)) return text
        for (i in 0 until node.childCount) {
            val result = findUrl(node.getChild(i))
            if (result != null) return result
        }
        return null
    }

    private fun isUrl(text: String): Boolean {
        val t = text.trim().lowercase()
        return t.startsWith("http://") || t.startsWith("https://") ||
               t.startsWith("www.") ||
               (t.contains(".") && !t.contains(" ") && t.length > 4)
    }

    private fun showBlockScreen() {
        removeOverlay()

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            android.graphics.PixelFormat.OPAQUE
        )

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.gravity = Gravity.CENTER
        layout.setBackgroundColor(Color.parseColor("#0D1B2A"))
        layout.setPadding(80, 80, 80, 80)

        val icon = TextView(this)
        icon.text = "🔒"
        icon.textSize = 64f
        icon.gravity = Gravity.CENTER

        val title = TextView(this)
        title.text = "Site Blocked"
        title.textSize = 28f
        title.setTextColor(Color.WHITE)
        title.gravity = Gravity.CENTER
        title.typeface = Typeface.DEFAULT_BOLD
        title.setPadding(0, 24, 0, 32)

        val msg = TextView(this)
        msg.text = messages.random()
        msg.textSize = 18f
        msg.setTextColor(Color.parseColor("#90CAF9"))
        msg.gravity = Gravity.CENTER
        msg.setPadding(0, 0, 0, 64)
        msg.lineSpacingMultiplier = 1.4f

        val btn = Button(this)
        btn.text = "← Go Back"
        btn.textSize = 16f
        btn.setTextColor(Color.WHITE)
        btn.setBackgroundColor(Color.parseColor("#1565C0"))
        btn.setPadding(60, 24, 60, 24)
        btn.setOnClickListener {
            removeOverlay()
            performGlobalAction(GLOBAL_ACTION_BACK)
        }

        layout.addView(icon)
        layout.addView(title)
        layout.addView(msg)
        layout.addView(btn)

        overlayView = layout
        windowManager?.addView(layout, params)
    }

    private fun removeOverlay() {
        overlayView?.let {
            try { windowManager?.removeView(it) } catch (e: Exception) {}
            overlayView = null
        }
    }

    override fun onInterrupt() { removeOverlay() }

    override fun onDestroy() {
        super.onDestroy()
        removeOverlay()
        try { unregisterReceiver(screenOffReceiver) } catch (e: Exception) {}
    }
}
