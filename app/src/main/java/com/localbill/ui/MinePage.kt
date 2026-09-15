package com.localbill.ui
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
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

    private var pickerWrap: LinearLayout? = null
    private var tvCurrentTheme: TextView? = null
    private var tvAddCustom: TextView? = null

    private fun showThemeDialog() {
        val box = UiKit.vertical(host).apply {
            setPadding(Theme.dp(host, 18), Theme.dp(host, 12), Theme.dp(host, 18), Theme.dp(host, 4))
        }
        box.addView(UiKit.text(host, "深色模式", 15f, Theme.mainText(host)))
        val darkSwitch = android.widget.Switch(host).apply {
            isChecked = Prefs.darkMode
        }
        // 选中/切换时保持界面不退出，关闭对话框时统一生效（recreate）
        darkSwitch.setOnCheckedChangeListener { _, checked ->
            Prefs.darkMode = checked
        }
        box.addView(darkSwitch, LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
            setMargins(0, Theme.dp(host, 4), 0, Theme.dp(host, 10))
        })
        box.addView(UiKit.text(host, "主题色", 15f, Theme.mainText(host)))
        pickerWrap = UiKit.vertical(host)
        box.addView(pickerWrap, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
            setMargins(0, Theme.dp(host, 6), 0, 0)
        })
        rebuildColorPicker()

        val addBtn = UiKit.text(host, addCustomLabel(), 14f, Theme.primary(host), bold = true).apply {
            setPadding(Theme.dp(host, 4), Theme.dp(host, 10), 0, Theme.dp(host, 4))
        }
        addBtn.setOnClickListener { showCustomColorDialog() }
        tvAddCustom = addBtn
        box.addView(addBtn)

        android.app.AlertDialog.Builder(host)
            .setTitle("主题设置")
            .setView(box)
            .setPositiveButton("完成", null)
            .create()
            .apply {
                setOnDismissListener { host.recreate() }
                show()
            }
    }

    /** 重建色板区：内置色 + 自定义色（多排）+ 提示 + 当前选中 */
    private fun rebuildColorPicker() {
        val wrap = pickerWrap ?: return
        wrap.removeAllViews()
        val colors = intArrayOf(*C.THEME_COLORS, *Prefs.customThemeColors.toIntArray())
        val names = listOf("蓝色", "绿色", "紫色", "橙色", "青色", "粉色", "靛蓝", "柠檬", "鼠尾草") +
            Prefs.customThemeColors.mapIndexed { i, c ->
                Prefs.customThemeColorNames.getOrElse(i) { "" }.ifBlank { hex(c) }
            }
        wrap.addView(
            ColorPickerView(
                host,
                Theme.primary(host),
                colors,
                onSelected = { color -> pickThemeColor(color) },
                onLongPress = { color -> tryDeleteCustomColor(color) },
                names = names,
                columns = 4
            ),
            LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        )
        wrap.addView(UiKit.text(host, "长按自定义颜色可删除", 11f, Theme.lightText(host)).apply {
            setPadding(Theme.dp(host, 4), Theme.dp(host, 6), 0, 0)
        })
        tvCurrentTheme = UiKit.text(host, "当前主题色：${currentThemeLabel()}", 12f, Theme.subText(host)).apply {
            setPadding(Theme.dp(host, 4), Theme.dp(host, 6), 0, 0)
        }
        wrap.addView(tvCurrentTheme)
    }

    private fun pickThemeColor(color: Int) {
        when (color) {
            C.ANT_GREEN -> Prefs.themeColor = "green"
            C.ANT_PURPLE -> Prefs.themeColor = "purple"
            C.ANT_ORANGE -> Prefs.themeColor = "orange"
            C.CYAN_PRIMARY -> Prefs.themeColor = "cyan"
            C.PINK_PRIMARY -> Prefs.themeColor = "pink"
            C.INDIGO_PRIMARY -> Prefs.themeColor = "indigo"
            C.LIME_PRIMARY -> Prefs.themeColor = "lime"
            C.SAGE_PRIMARY -> Prefs.themeColor = "sage"
            else -> {
                Prefs.customThemeColor = color
                Prefs.themeColor = "custom"
            }
        }
        tvCurrentTheme?.text = "当前主题色：${currentThemeLabel()}"
        tvAddCustom?.text = addCustomLabel()
    }

    /** 自定义颜色按钮文字：未选中自定义色时显示新增，否则显示当前自定义色 */
    private fun addCustomLabel(): String {
        if (Prefs.themeColor != "custom") return "＋ 自定义颜色（RGB）"
        val idx = Prefs.customThemeColors.indexOf(Prefs.customThemeColor)
        if (idx >= 0) {
            val name = Prefs.customThemeColorNames.getOrElse(idx) { "" }
            if (name.isNotBlank()) return "自定义颜色：$name"
        }
        return "自定义颜色：#${hex(Prefs.customThemeColor)}"
    }

    /** 当前主题色的显示名（内置色 → 中文名，自定义 → 名称/十六进制） */
    private fun currentThemeLabel(): String {
        val color = when (Prefs.themeColor) {
            "green" -> C.ANT_GREEN
            "purple" -> C.ANT_PURPLE
            "orange" -> C.ANT_ORANGE
            "cyan" -> C.CYAN_PRIMARY
            "pink" -> C.PINK_PRIMARY
            "indigo" -> C.INDIGO_PRIMARY
            "lime" -> C.LIME_PRIMARY
            "custom" -> Prefs.customThemeColor
            else -> C.ANT_BLUE
        }
        if (Prefs.themeColor == "custom") {
            val idx = Prefs.customThemeColors.indexOf(color)
            if (idx >= 0) {
                val name = Prefs.customThemeColorNames.getOrElse(idx) { "" }
                if (name.isNotBlank()) return name
            }
            return "自定义 #${hex(color)}"
        }
        return colorLabel(color)
    }

    private fun colorLabel(color: Int): String = when (color) {
        C.ANT_BLUE -> "蓝色"
        C.ANT_GREEN -> "绿色"
        C.ANT_PURPLE -> "紫色"
        C.ANT_ORANGE -> "橙色"
        C.CYAN_PRIMARY -> "青色"
        C.PINK_PRIMARY -> "粉色"
        C.INDIGO_PRIMARY -> "靛蓝"
        C.LIME_PRIMARY -> "柠檬"
        C.SAGE_PRIMARY -> "鼠尾草"
        else -> "自定义 #${hex(color)}"
    }

    private fun hex(color: Int): String {
        val r = color shr 16 and 0xFF
        val g = color shr 8 and 0xFF
        val b = color and 0xFF
        return String.format("%02X%02X%02X", r, g, b)
    }

    /** 长按自定义颜色 → 确认删除 */
    private fun tryDeleteCustomColor(color: Int) {
        val idx = Prefs.customThemeColors.indexOf(color)
        if (idx < 0) return
        android.app.AlertDialog.Builder(host)
            .setTitle("删除自定义颜色？")
            .setPositiveButton("删除") { _, _ ->
                Prefs.customThemeColors = Prefs.customThemeColors.filterIndexed { i, _ -> i != idx }
                Prefs.customThemeColorNames = Prefs.customThemeColorNames.filterIndexed { i, _ -> i != idx }
                if (Prefs.themeColor == "custom" && Prefs.customThemeColor == color) {
                    Prefs.themeColor = "blue"
                }
                rebuildColorPicker()
                tvAddCustom?.text = addCustomLabel()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    /** 添加/重命名自定义主题色（RGB + 名称） */
    private fun showCustomColorDialog() {
        val current = if (Prefs.themeColor == "custom") Prefs.customThemeColor else Theme.primary(host)
        val etName = EditText(host).apply {
            hint = "颜色名称（可选）"
            textSize = 14f
            setTextColor(Theme.mainText(host))
        }
        val etR = EditText(host).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            hint = "0-255"
            setText(((current shr 16) and 0xFF).toString())
            textSize = 14f
            setTextColor(Theme.mainText(host))
        }
        val etG = EditText(host).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            hint = "0-255"
            setText(((current shr 8) and 0xFF).toString())
            textSize = 14f
            setTextColor(Theme.mainText(host))
        }
        val etB = EditText(host).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            hint = "0-255"
            setText((current and 0xFF).toString())
            textSize = 14f
            setTextColor(Theme.mainText(host))
        }

        val preview = UiKit.circle(host, current, 36)
        fun updatePreview() {
            val r = etR.text.toString().toIntOrNull() ?: return
            val g = etG.text.toString().toIntOrNull() ?: return
            val b = etB.text.toString().toIntOrNull() ?: return
            if (r !in 0..255 || g !in 0..255 || b !in 0..255) return
            (preview.background as? GradientDrawable)?.setColor(Color.rgb(r, g, b))
        }
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) = updatePreview()
        }
        etR.addTextChangedListener(watcher)
        etG.addTextChangedListener(watcher)
        etB.addTextChangedListener(watcher)

        val box = UiKit.vertical(host).apply {
            setPadding(Theme.dp(host, 18), Theme.dp(host, 12), Theme.dp(host, 18), Theme.dp(host, 4))
        }
        box.addView(preview, LinearLayout.LayoutParams(Theme.dp(host, 36), Theme.dp(host, 36)).apply {
            gravity = Gravity.CENTER_HORIZONTAL
            bottomMargin = Theme.dp(host, 12)
        })
        box.addView(colorInputRow("名称", etName))
        box.addView(colorInputRow("R", etR))
        box.addView(colorInputRow("G", etG))
        box.addView(colorInputRow("B", etB))

        android.app.AlertDialog.Builder(host)
            .setTitle("自定义主题色（RGB）")
            .setView(box)
            .setPositiveButton("添加") { _, _ ->
                val r = etR.text.toString().toIntOrNull()
                val g = etG.text.toString().toIntOrNull()
                val b = etB.text.toString().toIntOrNull()
                if (r == null || g == null || b == null || r !in 0..255 || g !in 0..255 || b !in 0..255) {
                    Toast.makeText(host, "请输入 0-255 的 RGB 值", Toast.LENGTH_SHORT).show()
                } else {
                    addCustomColor(etName.text.toString(), Color.rgb(r, g, b))
                }
            }
            .setNegativeButton("取消", null)
            .create()
            .apply {
                show()
                getButton(android.app.AlertDialog.BUTTON_POSITIVE)?.setTextColor(Theme.primary(host))
            }
    }

    private fun colorInputRow(label: String, et: EditText): View {
        val row = UiKit.horizontal(host).apply {
            gravity = Gravity.CENTER_VERTICAL
        }
        row.addView(
            UiKit.text(host, label, 14f, Theme.mainText(host), bold = true),
            LinearLayout.LayoutParams(Theme.dp(host, 36), WRAP_CONTENT)
        )
        row.addView(et, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
        return row
    }

    /** 保存自定义颜色（含名称），并刷新色板 */
    private fun addCustomColor(name: String, color: Int) {
        val idx = Prefs.customThemeColors.indexOf(color)
        if (idx >= 0) {
            // 已存在则更新名称
            val names = Prefs.customThemeColorNames.toMutableList()
            names[idx] = name.trim()
            Prefs.customThemeColorNames = names
        } else {
            Prefs.customThemeColors = Prefs.customThemeColors + color
            Prefs.customThemeColorNames = Prefs.customThemeColorNames + name.trim()
        }
        Prefs.customThemeColor = color
        Prefs.themeColor = "custom"
        rebuildColorPicker()
        tvAddCustom?.text = addCustomLabel()
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

