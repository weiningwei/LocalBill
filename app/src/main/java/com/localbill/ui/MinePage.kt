package com.localbill.ui
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT

import android.content.Context
import android.content.Intent
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import com.localbill.App
import com.localbill.R
import com.localbill.util.C
import com.localbill.util.Money
import com.localbill.util.Prefs
import com.localbill.util.Theme
import com.localbill.util.UiKit

class MinePage(private val host: MainActivity) : LinearLayout(host) {

    private val tvLedger = UiKit.text(host, "", 17f, Theme.mainText(host), bold = true)
    private val tvAssets = UiKit.text(host, "", 15f, Theme.mainText(host))

    init {
        orientation = LinearLayout.VERTICAL
        setBackgroundColor(Theme.pageBg(host))
        buildUi()
        refresh()
    }

    private fun buildUi() {
        val scroll = ScrollView(host)
        scroll.isFillViewport = true
        val body = UiKit.vertical(host)

        // 顶部账本卡片
        val card = UiKit.horizontal(host).apply {
            gravity = Gravity.CENTER_VERTICAL
            background = UiKit.rounded(host, Theme.surface(host), 14)
            setPadding(Theme.dp(host, 16), Theme.dp(host, 14), Theme.dp(host, 10), Theme.dp(host, 14))
        }
        val cardLp = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        cardLp.setMargins(Theme.dp(host, 24), Theme.dp(host, 10), Theme.dp(host, 24), Theme.dp(host, 6))
        val left = UiKit.vertical(host)
        left.addView(UiKit.text(host, "当前账本", 12f, Theme.lightText(host)))
        left.addView(tvLedger)
        left.addView(UiKit.text(host, "资产合计", 12f, Theme.lightText(host)).apply {
            setPadding(0, Theme.dp(host, 6), 0, 0)
        })
        left.addView(tvAssets)
        card.addView(left, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
        val btn = UiKit.text(host, "切换", 14f, Theme.primary(host), bold = true)
        btn.setPadding(Theme.dp(host, 12), Theme.dp(host, 8), Theme.dp(host, 12), Theme.dp(host, 8))
        btn.setOnClickListener { open(LedgerManageActivity::class.java) }
        card.addView(btn)
        body.addView(card, cardLp)

        body.addView(row("分类管理", "管理账目分类与图标") { open(CategoryManageActivity::class.java) })
        body.addView(row("账户管理", "现金、银行卡、支付宝等") { open(AccountManageActivity::class.java) })
        body.addView(row("账本管理", "多账本独立记账") { open(LedgerManageActivity::class.java) })
        body.addView(row("主题", "外观与主题色") { showThemeDialog() })
        body.addView(rowWithSwitch("密码锁", "进入应用需要密码", { Prefs.passwordEnabled }, { v -> togglePassword(v) }))
        body.addView(rowWithSwitch("自动记账", "微信/支付宝支付成功自动填写", { Prefs.importEnabled }, { v -> toggleImport(v) }))
        body.addView(row("无障碍服务设置", "需在系统设置中开启后才能自动识别", {
            host.startActivity(Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }))
        body.addView(row("数据管理", "备份、恢复、导出账单") { open(BackupActivity::class.java) })
        body.addView(row("回收站", "已删除的记录") { open(RecycleBinActivity::class.java) })
        body.addView(row("关于", "版本与说明") { showAbout() })

        scroll.addView(body, ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        addView(scroll, LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f))
    }

    private fun open(cls: Class<*>) {
        host.startActivity(Intent(host, cls))
    }

    private fun row(title: String, subtitle: String, onClick: () -> Unit): View {
        val r = UiKit.horizontal(host).apply {
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(Theme.surface(host))
            setPadding(Theme.dp(host, 16), Theme.dp(host, 14), Theme.dp(host, 14), Theme.dp(host, 14))
            isClickable = true
            isFocusable = true
        }
        val lp = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        lp.setMargins(Theme.dp(host, 24), Theme.dp(host, 3), Theme.dp(host, 24), Theme.dp(host, 3))
        r.addView(rowLeft(title, subtitle), LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
        val chevron = TextView(host).apply {
            text = "›"
            textSize = 20f
            setTextColor(Theme.lightText(host))
        }
        r.addView(chevron)
        r.setOnClickListener { onClick() }
        return r.apply { layoutParams = lp }
    }

    private fun rowWithSwitch(
        title: String,
        subtitle: String,
        checkedSupplier: () -> Boolean,
        onChanged: (Boolean) -> Unit
    ): View {
        val r = UiKit.horizontal(host).apply {
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(Theme.surface(host))
            setPadding(Theme.dp(host, 16), Theme.dp(host, 8), Theme.dp(host, 14), Theme.dp(host, 8))
        }
        val lp = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        lp.setMargins(Theme.dp(host, 24), Theme.dp(host, 3), Theme.dp(host, 24), Theme.dp(host, 3))
        r.addView(rowLeft(title, subtitle), LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
        val sw = Switch(host).apply {
            isChecked = checkedSupplier()
        }
        sw.setOnCheckedChangeListener { _, checked -> onChanged(checked) }
        r.addView(sw)
        return r.apply { layoutParams = lp }
    }

    private fun rowLeft(title: String, subtitle: String): LinearLayout {
        val c = UiKit.vertical(host)
        c.gravity = Gravity.CENTER_VERTICAL
        c.addView(UiKit.text(host, title, 15f, Theme.mainText(host)))
        c.addView(UiKit.text(host, subtitle, 12f, Theme.lightText(host)))
        return c
    }

    private fun showThemeDialog() {
        val box = UiKit.vertical(host).apply {
            setPadding(Theme.dp(host, 18), Theme.dp(host, 12), Theme.dp(host, 18), Theme.dp(host, 4))
        }
        box.addView(UiKit.text(host, "深色模式", 15f, Theme.mainText(host)))
        val darkSwitch = android.widget.Switch(host).apply {
            isChecked = Prefs.darkMode
        }
        darkSwitch.setOnCheckedChangeListener { _, checked ->
            Prefs.darkMode = checked
            host.recreate()
        }
        box.addView(darkSwitch, LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
            setMargins(0, Theme.dp(host, 4), 0, Theme.dp(host, 10))
        })
        box.addView(UiKit.text(host, "主题色", 15f, Theme.mainText(host)))
        box.addView(
            ColorPickerView(host, Theme.primary(host), themeColors) { color -> pickThemeColor(color) },
            LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                setMargins(0, Theme.dp(host, 6), 0, 0)
            }
        )
        android.app.AlertDialog.Builder(host)
            .setTitle("主题设置")
            .setView(box)
            .setPositiveButton("完成", null)
            .show()
    }

    private val themeColors = intArrayOf(C.ANT_BLUE, C.ANT_GREEN, C.ANT_PURPLE, C.ANT_ORANGE)

    private fun pickThemeColor(color: Int) {
        Prefs.themeColor = when (color) {
            C.ANT_GREEN -> "green"
            C.ANT_PURPLE -> "purple"
            C.ANT_ORANGE -> "orange"
            else -> "blue"
        }
        host.recreate()
    }

    private fun togglePassword(checked: Boolean) {
        if (checked) {
            showSetPassword()
        } else {
            android.app.AlertDialog.Builder(host)
                .setTitle("关闭密码锁？")
                .setPositiveButton("关闭") { _, _ -> Prefs.passwordEnabled = false; host.recreate() }
                .setNegativeButton("取消", null)
                .show()
        }
    }

    private fun toggleImport(checked: Boolean) {
        if (checked) {
            Prefs.importEnabled = true
            Toast.makeText(host, "请到系统设置中开启「本地账」的无障碍服务", Toast.LENGTH_LONG).show()
            host.startActivity(Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS))
        } else {
            Prefs.importEnabled = false
            Prefs.lastImportSignature = ""
            Prefs.lastImportAtMillis = 0
        }
    }

    private fun showSetPassword() {
        val name = android.widget.EditText(host).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            hint = "请设置 4-8 位数字密码"
        }
        val confirm = android.widget.EditText(host).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            hint = "再次输入确认"
        }
        val box = UiKit.vertical(host)
        box.addView(name)
        box.addView(confirm, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
            topMargin = Theme.dp(host, 8)
        })
        val dialog = android.app.AlertDialog.Builder(host)
            .setTitle("设置密码锁")
            .setView(box)
            .setPositiveButton("确定") { _, _ ->
                val p1 = name.text.toString()
                val p2 = confirm.text.toString()
                if (p1.length < 4 || p1.length > 8) {
                    Toast.makeText(host, "密码长度需为 4-8 位", Toast.LENGTH_SHORT).show()
                } else if (p1 != p2) {
                    Toast.makeText(host, "两次输入不一致", Toast.LENGTH_SHORT).show()
                } else {
                    Prefs.password = p1
                    Prefs.passwordEnabled = true
                    Toast.makeText(host, R.string.toast_password_set, Toast.LENGTH_SHORT).show()
                    host.recreate()
                }
            }
            .setNegativeButton("取消", null)
            .create()
        dialog.show()
        dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)?.setTextColor(Theme.primary(host))
    }

    private fun showAbout() {
        android.app.AlertDialog.Builder(host)
            .setTitle("本地账 LocalBill")
            .setMessage(host.getString(R.string.about_text))
            .setPositiveButton("知道了", null)
            .show()
    }

    fun refresh() {
        val ledger = App.db.activeLedger()
        tvLedger.text = ledger.name
        tvAssets.text = Money.format(App.db.totalAssets(ledger.id))
    }
}

