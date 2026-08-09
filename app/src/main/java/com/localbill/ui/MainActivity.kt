package com.localbill.ui
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.localbill.App
import com.localbill.R
import com.localbill.util.C
import com.localbill.util.Prefs
import com.localbill.util.Theme
import com.localbill.util.UiKit

class MainActivity : Activity() {

    private lateinit var contentFrame: FrameLayout
    private val pages = ArrayList<View>()
    private val tabs = ArrayList<View>()
    private var currentTab = 0
    private var unlocked = false

    private lateinit var homePage: HomePage
    private lateinit var billsPage: BillsPage
    private lateinit var statsPage: StatsPage
    private lateinit var minePage: MinePage

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(if (Prefs.darkMode) R.style.Theme_LocalBill_Dark else R.style.Theme_LocalBill)
        super.onCreate(savedInstanceState)
        currentTab = savedInstanceState?.getInt(STATE_TAB, 0) ?: 0
        buildUi()
        checkPasswordLock()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(STATE_TAB, currentTab)
    }

    companion object {
        private const val STATE_TAB = "state_tab"
    }

    override fun onResume() {
        super.onResume()
        refreshAll()
    }

    fun refreshAll() {
        homePage.refresh()
        billsPage.refresh()
        statsPage.refresh()
        minePage.refresh()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Theme.pageBg(this@MainActivity))
        }

        contentFrame = FrameLayout(this)
        root.addView(contentFrame, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        ))

        homePage = HomePage(this)
        billsPage = BillsPage(this)
        statsPage = StatsPage(this)
        minePage = MinePage(this)
        pages.add(homePage)
        pages.add(billsPage)
        pages.add(statsPage)
        pages.add(minePage)
        pages.forEach { contentFrame.addView(it, FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT) }

        root.addView(buildBottomBar(), LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, Theme.dp(this, 62)))

        setContentView(root)
        switchTab(currentTab)
    }

    private fun buildBottomBar(): LinearLayout {
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setBackgroundColor(Theme.surface(this@MainActivity))
            elevation = Theme.dp(this@MainActivity, 6).toFloat()
        }
        bar.addView(tabItem(0, R.drawable.ic_tab_home, getString(R.string.tab_home)), weightParams())
        bar.addView(tabItem(1, R.drawable.ic_tab_bills, getString(R.string.tab_bills)), weightParams())
        bar.addView(tabItem(2, R.drawable.ic_tab_stats, getString(R.string.tab_stats)), weightParams())
        bar.addView(tabItem(3, R.drawable.ic_tab_mine, getString(R.string.tab_mine)), weightParams())
        return bar
    }

    private fun weightParams(): LinearLayout.LayoutParams {
        val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
        return lp
    }

    private fun tabItem(index: Int, iconRes: Int, label: String): View {
        val v = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(0, Theme.dp(this@MainActivity, 6), 0, Theme.dp(this@MainActivity, 4))
        }
        val iv = ImageView(this)
        iv.setImageResource(iconRes)
        iv.layoutParams = LinearLayout.LayoutParams(Theme.dp(this, 24), Theme.dp(this, 24))
        val tv = TextView(this).apply {
            text = label
            textSize = 11f
            setPadding(0, Theme.dp(this@MainActivity, 2), 0, 0)
        }
        v.addView(iv)
        v.addView(tv)
        v.tag = index
        v.setOnClickListener { switchTab(index) }
        tabs.add(v)
        return v
    }

    private fun switchTab(index: Int) {
        currentTab = index
        for (i in pages.indices) {
            pages[i].visibility = if (i == index) View.VISIBLE else View.GONE
        }
        for (i in tabs.indices) {
            val v = tabs[i]
            val iv = (v as LinearLayout).getChildAt(0) as ImageView
            val tv = v.getChildAt(1) as TextView
            val active = i == index
            iv.setColorFilter(if (active) C.PRIMARY else Theme.lightText(this))
            tv.setTextColor(if (active) C.PRIMARY else Theme.lightText(this))
            tv.setTypeface(tv.typeface, if (active) Typeface.BOLD else Typeface.NORMAL)
        }
    }

    fun openRecord(billId: Long?) {
        val intent = Intent(this, RecordActivity::class.java)
        if (billId != null) intent.putExtra(RecordActivity.EXTRA_ID, billId)
        startActivity(intent)
    }

    /* ---------------- 密码锁 ---------------- */
    private fun checkPasswordLock() {
        if (unlocked || !Prefs.passwordEnabled || Prefs.password.isEmpty()) return
        val input = android.widget.EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
            hint = "请输入密码"
        }
        val dialog = android.app.AlertDialog.Builder(this)
            .setTitle("已开启密码锁")
            .setView(input)
            .setPositiveButton("确定") { _, _ ->
                if (input.text.toString() == Prefs.password) {
                    unlocked = true
                } else {
                    Toast.makeText(this, R.string.toast_password_wrong, Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .setCancelable(false)
            .create()
        dialog.show()
        dialog.setOnShowListener {
            val b = dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)
            b.setTextColor(C.PRIMARY)
        }
    }
}
