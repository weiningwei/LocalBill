package com.localbill.ui
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT

import android.app.Activity
import android.content.Context
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.EditText
import android.widget.GridView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import com.localbill.App
import com.localbill.R
import com.localbill.model.Account
import com.localbill.model.Category
import com.localbill.model.Kinds
import com.localbill.util.C
import com.localbill.util.CatIcon
import com.localbill.util.DateUtil
import com.localbill.util.Money
import com.localbill.util.Prefs
import com.localbill.util.Theme
import com.localbill.util.UiKit
import java.util.Calendar
import java.util.Locale

class RecordActivity : Activity() {
    private val ctx: Context get() = this@RecordActivity

    companion object {
        const val EXTRA_ID = "bill_id"
        const val EXTRA_AMOUNT_CENTS = "amount_cents" // Long
        const val EXTRA_REMARK = "remark"             // String（商家名）
        const val EXTRA_DAY = "day"                   // Int YYYYMMDD（可选）
        const val EXTRA_TIME = "time"                 // Int 当日秒数（可选）
        const val EXTRA_CATEGORY = "category"         // String 一级分类名（可选）
    }

    private var editId: Long = -1
    private var kind = Kinds.EXPENSE

    private lateinit var tvAmount: TextView
    private lateinit var topGrid: GridView
    private lateinit var subGrid: GridView
    private lateinit var subRow: LinearLayout
    private lateinit var tvAccount: TextView
    private lateinit var tvDate: TextView
    private lateinit var tvTime: TextView
    private lateinit var etRemark: EditText
    private lateinit var keypadBox: LinearLayout

    private var topCats: List<Category> = emptyList()
    private var subCats: List<Category> = emptyList()
    private var selectedTop: Category? = null
    private var selectedSub: Category? = null
    private var accounts: List<Account> = emptyList()
    private var selectedAccount: Account? = null
    private var day = DateUtil.today()
    private var time = DateUtil.timeNow()

    private var amountStr = ""
    private var topAdapter: CatAdapter? = null
    private var subAdapter: CatAdapter? = null
    private val kindButtons = ArrayList<TextView>()

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(Theme.themeStyle())
        super.onCreate(savedInstanceState)
        editId = intent.getLongExtra(EXTRA_ID, -1)

        val ledger = App.db.activeLedger()
        accounts = App.db.accounts(ledger.id)
        val remembered = Prefs.rememberedRecordDay
        day = if (editId > 0) {
            App.db.billById(editId)?.day ?: DateUtil.today()
        } else if (remembered != 0 && remembered > 0) {
            remembered
        } else DateUtil.today()

        if (editId > 0) {
            val bill = App.db.billById(editId)
            if (bill != null) {
                kind = bill.kind
                day = bill.day
                time = bill.time
                selectedAccount = accounts.firstOrNull { it.id == bill.accountId }
                val cat = App.db.categoryById(bill.categoryId)
                if (cat != null) {
                    if (cat.parent == 0L) selectedTop = cat else {
                        selectedTop = App.db.categoryById(cat.parent)
                        selectedSub = cat
                    }
                }
                amountStr = Money.format(bill.amount).replace(",", "")
                etRemark = EditText(ctx)
                etRemark.setText(bill.remark)
            }
        } else if (intent.hasExtra(EXTRA_AMOUNT_CENTS)) {
            // 自动记账预填：金额/备注/日期时间/分类
            val cents = intent.getLongExtra(EXTRA_AMOUNT_CENTS, 0L)
            if (cents > 0) amountStr = Money.format(cents).replace(",", "")
            if (intent.hasExtra(EXTRA_DAY)) day = intent.getIntExtra(EXTRA_DAY, day)
            if (intent.hasExtra(EXTRA_TIME)) time = intent.getIntExtra(EXTRA_TIME, time)
            val remark = intent.getStringExtra(EXTRA_REMARK).orEmpty()
            etRemark = EditText(ctx)
            etRemark.setText(remark)
            val catName = intent.getStringExtra(EXTRA_CATEGORY)
            if (!catName.isNullOrEmpty()) {
                selectedTop = App.db.categoryByName(catName, Kinds.EXPENSE)
                selectedSub = null
            }
        }
        if (selectedAccount == null) selectedAccount = accounts.firstOrNull()

        buildUi()
        loadCategories()
        refreshSelection()
    }

    private fun buildUi() {
        val root = UiKit.vertical(ctx).apply { setBackgroundColor(Theme.pageBg(ctx)) }

        // 顶栏
        val topBar = UiKit.horizontal(ctx).apply { gravity = Gravity.CENTER_VERTICAL }
        val topLp = LinearLayout.LayoutParams(MATCH_PARENT, Theme.dp(ctx, 52))
        val close = UiKit.text(ctx, "取消", 16f, Theme.subText(ctx))
        close.setPadding(Theme.dp(ctx, 16), Theme.dp(ctx, 12), Theme.dp(ctx, 12), Theme.dp(ctx, 12))
        close.setOnClickListener { finish() }
        topBar.addView(close)
        val title = UiKit.text(ctx, if (editId > 0) "编辑记录" else "记一笔", 17f, Theme.mainText(ctx), bold = true, gravity = Gravity.CENTER)
        topBar.addView(title, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
        val save = UiKit.text(ctx, "保存", 16f, Theme.primary(ctx), bold = true)
        save.setPadding(Theme.dp(ctx, 12), Theme.dp(ctx, 12), Theme.dp(ctx, 16), Theme.dp(ctx, 12))
        save.setOnClickListener { saveBill() }
        topBar.addView(save)
        root.addView(topBar, topLp)

        // 收支切换
        val seg = UiKit.horizontal(ctx).apply {
            gravity = Gravity.CENTER
            setPadding(Theme.dp(ctx, 14), Theme.dp(ctx, 4), Theme.dp(ctx, 14), Theme.dp(ctx, 4))
        }
        val segWrap = LinearLayout(ctx).apply {
            background = UiKit.rounded(ctx, Theme.surface(ctx), 22)
        }
        seg.addView(kindButton("支出", Kinds.EXPENSE), segWeight(1))
        seg.addView(kindButton("收入", Kinds.INCOME), segWeight(1))
        segWrap.addView(seg, LinearLayout.LayoutParams(0, Theme.dp(ctx, 44), 1f).apply {
            setMargins(Theme.dp(ctx, 4), Theme.dp(ctx, 4), Theme.dp(ctx, 4), Theme.dp(ctx, 4))
        })
        root.addView(segWrap, LinearLayout.LayoutParams(MATCH_PARENT, Theme.dp(ctx, 52)))

        // 金额
        val amountWrap = UiKit.horizontal(ctx).apply {
            gravity = Gravity.CENTER
            setPadding(0, Theme.dp(ctx, 8), 0, Theme.dp(ctx, 4))
        }
        amountWrap.addView(UiKit.text(ctx, "¥", 30f, Theme.mainText(ctx), bold = true))
        tvAmount = UiKit.text(ctx, "0.00", 36f, Theme.mainText(ctx), bold = true)
        amountWrap.addView(tvAmount)
        // 点击金额区域时，让键盘重新作用于金额
        amountWrap.setOnClickListener { clearRemarkFocus() }
        root.addView(amountWrap, LinearLayout.LayoutParams(MATCH_PARENT, Theme.dp(ctx, 62)))

        // 分类网格
        val catScroll = android.widget.ScrollView(ctx)
        val catBox = UiKit.vertical(ctx)
        topGrid = GridView(ctx).apply {
            numColumns = 5
            horizontalSpacing = Theme.dp(ctx, 8)
            verticalSpacing = Theme.dp(ctx, 6)
            stretchMode = GridView.STRETCH_COLUMN_WIDTH
        }
        catBox.addView(topGrid, ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        subRow = UiKit.vertical(ctx)
        subGrid = GridView(ctx).apply {
            numColumns = 5
            horizontalSpacing = Theme.dp(ctx, 8)
            verticalSpacing = Theme.dp(ctx, 6)
            stretchMode = GridView.STRETCH_COLUMN_WIDTH
        }
        subRow.addView(subGrid, ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        catBox.addView(subRow)
        catScroll.addView(catBox, ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        root.addView(catScroll, LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f))

        // 账户/日期/备注
        val infoRow = UiKit.horizontal(ctx).apply {
            gravity = Gravity.CENTER_VERTICAL
            background = UiKit.rounded(ctx, Theme.surface(ctx), 12)
            setPadding(Theme.dp(ctx, 14), Theme.dp(ctx, 8), Theme.dp(ctx, 10), Theme.dp(ctx, 8))
        }
        val infoLp = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        infoLp.setMargins(Theme.dp(ctx, 24), Theme.dp(ctx, 6), Theme.dp(ctx, 24), Theme.dp(ctx, 4))

        tvAccount = UiKit.text(ctx, "", 14f, Theme.mainText(ctx))
        tvDate = UiKit.text(ctx, "", 14f, Theme.mainText(ctx))
        tvTime = UiKit.text(ctx, "", 14f, Theme.mainText(ctx))
        tvAccount.setOnClickListener { pickAccount() }
        tvDate.setOnClickListener { pickDate() }
        tvTime.setOnClickListener { pickTime() }
        val iconWrap = UiKit.horizontal(ctx).apply { gravity = Gravity.CENTER_VERTICAL }
        val icon = ImageView(ctx)
        icon.setImageResource(R.drawable.ic_calendar)
        icon.setColorFilter(Theme.subText(ctx))
        iconWrap.addView(icon, LinearLayout.LayoutParams(Theme.dp(ctx, 18), Theme.dp(ctx, 18)))

        infoRow.addView(UiKit.text(ctx, "账户", 14f, Theme.subText(ctx)))
        infoRow.addView(tvAccount, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply {
            setMargins(Theme.dp(ctx, 8), 0, 0, 0)
        })
        infoRow.addView(iconWrap)
        infoRow.addView(tvDate, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply {
            setMargins(Theme.dp(ctx, 6), 0, Theme.dp(ctx, 8), 0)
        })
        infoRow.addView(tvTime, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply {
            setMargins(Theme.dp(ctx, 6), 0, Theme.dp(ctx, 8), 0)
        })
        root.addView(infoRow, infoLp)

        val remarkRow = UiKit.horizontal(ctx).apply {
            gravity = Gravity.CENTER_VERTICAL
            background = UiKit.rounded(ctx, Theme.surface(ctx), 12)
            setPadding(Theme.dp(ctx, 14), Theme.dp(ctx, 6), Theme.dp(ctx, 10), Theme.dp(ctx, 6))
        }
        val remarkLp = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        remarkLp.setMargins(Theme.dp(ctx, 24), 0, Theme.dp(ctx, 24), Theme.dp(ctx, 4))
        remarkRow.addView(UiKit.text(ctx, "备注", 14f, Theme.subText(ctx)))
        if (!::etRemark.isInitialized) etRemark = EditText(ctx)
        etRemark.setTextSize(14f)
        etRemark.setHintTextColor(Theme.lightText(ctx))
        etRemark.setTextColor(Theme.mainText(ctx))
        etRemark.hint = "选填"
        remarkRow.addView(etRemark, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply {
            setMargins(Theme.dp(ctx, 10), 0, 0, 0)
        })
        root.addView(remarkRow, remarkLp)

        // 键盘
        val keypadBox = UiKit.vertical(ctx)
        val keys = listOf(
            listOf("7", "8", "9", "⌫"),
            listOf("4", "5", "6", "."),
            listOf("1", "2", "3", "00")
        )
        for (rowKeys in keys) {
            val row = UiKit.horizontal(ctx)
            rowKeys.forEach { k -> row.addView(keyButton(k), keyParams()) }
            keypadBox.addView(row, LinearLayout.LayoutParams(MATCH_PARENT, Theme.dp(ctx, 50)))
        }
        val lastRow = UiKit.horizontal(ctx)
        lastRow.addView(keyButton("0"), keyParams())
        val saveBtn = UiKit.text(ctx, "记一笔", 17f, Theme.primary(ctx), bold = true, gravity = Gravity.CENTER)
        saveBtn.background = UiKit.rounded(ctx, Theme.primaryBg(ctx), 10)
        saveBtn.setPadding(0, Theme.dp(ctx, 12), 0, Theme.dp(ctx, 12))
        saveBtn.setOnClickListener { saveBill() }
        lastRow.addView(saveBtn, LinearLayout.LayoutParams(0, MATCH_PARENT, 3f))
        keypadBox.addView(lastRow, LinearLayout.LayoutParams(MATCH_PARENT, Theme.dp(ctx, 52)))
        root.addView(keypadBox)
        this.keypadBox = keypadBox

        // 备注聚焦时隐藏数字键盘，交由系统软键盘输入；失焦后恢复
        etRemark.setOnFocusChangeListener { _, hasFocus ->
            keypadBox.visibility = if (hasFocus) View.GONE else View.VISIBLE
        }

        // Android 15+ 不再由 adjustResize 自动压缩窗口，需手动处理系统栏 insets：
        // 顶部避开状态栏；备注聚焦（数字键盘隐藏）时底部让出输入法高度，
        // 数字键盘显示时键盘延伸至导航栏后面，仅按键内容让出手势条
        root.setOnApplyWindowInsetsListener { v, insets ->
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                val sys = insets.getInsets(android.view.WindowInsets.Type.systemBars())
                val ime = insets.getInsets(android.view.WindowInsets.Type.ime())
                v.setPadding(0, sys.top, 0,
                    if (keypadBox.visibility == View.GONE) ime.bottom.coerceAtLeast(sys.bottom) else 0)
                keypadBox.setPadding(0, 0, 0,
                    if (keypadBox.visibility == View.GONE) 0 else sys.bottom)
            } else {
                v.setPadding(0, insets.systemWindowInsetTop, 0,
                    if (keypadBox.visibility == View.GONE) insets.systemWindowInsetBottom else 0)
            }
            insets
        }

        setContentView(root)
        updateAmountText()
    }

    private fun kindButton(label: String, k: Int): TextView {
        val tv = TextView(ctx).apply {
            text = label
            textSize = 15f
            gravity = Gravity.CENTER
        }
        tv.tag = k
        tv.setOnClickListener {
            kind = k
            clearRemarkFocus()
            updateKind()
            loadCategories()
        }
        kindButtons.add(tv)
        return tv
    }

    private fun segWeight(w: Int): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(0, MATCH_PARENT, w.toFloat())

    private fun keyButton(label: String): TextView {
        val tv = TextView(ctx).apply {
            text = label
            textSize = if (label == "⌫") 20f else 20f
            gravity = Gravity.CENTER
            setTextColor(Theme.mainText(ctx))
        }
        tv.setOnClickListener { onKey(label) }
        return tv
    }

    private fun keyParams(): LinearLayout.LayoutParams {
        val lp = LinearLayout.LayoutParams(0, MATCH_PARENT, 1f)
        return lp
    }

    private fun onKey(key: String) {
        // 备注获得焦点时，键盘输入应作用于备注而不是金额
        if (::etRemark.isInitialized && etRemark.hasFocus()) {
            onRemarkKey(key)
            return
        }
        when (key) {
            "⌫" -> {
                if (amountStr.isNotEmpty()) amountStr = amountStr.dropLast(1)
            }
            "." -> {
                if (amountStr.isEmpty()) amountStr = "0."
                else if ('.' !in amountStr) amountStr += "."
            }
            "00" -> {
                if (amountStr.isNotEmpty() && amountStr != "0") amountStr += "00"
            }
            else -> {
                // 最多两位小数
                val dotIdx = amountStr.indexOf('.')
                if (dotIdx >= 0 && amountStr.length - dotIdx > 2) return
                if (amountStr.replace(".", "").length >= 9) return
                if (amountStr == "0" && key != "0") amountStr = key
                else if (amountStr == "0") { /* 保持 0 */ }
                else amountStr += key
            }
        }
        updateAmountText()
    }

    private fun updateAmountText() {
        val cents = Money.parseToCents(amountStr)
        tvAmount.text = if (cents == null) (if (amountStr.isEmpty()) "0.00" else amountStr) else Money.format(cents)
    }

    /** 数字键盘作用于备注输入框时的按键处理 */
    private fun onRemarkKey(key: String) {
        val et = etRemark
        val start = et.selectionStart.coerceAtLeast(0)
        val end = et.selectionEnd.coerceAtLeast(start)
        when (key) {
            "⌫" -> {
                if (start == end) {
                    if (start > 0) {
                        // 按完整代码点删除，避免代理对（emoji/生僻汉字）被删半截
                        var toDelete = start - 1
                        if (Character.isLowSurrogate(et.text[toDelete]) &&
                            toDelete > 0 && Character.isHighSurrogate(et.text[toDelete - 1])) {
                            toDelete -= 1
                        }
                        et.text.delete(toDelete, start)
                    }
                } else {
                    et.text.delete(start, end)
                }
            }
            else -> {
                et.text.replace(start, end, key)
            }
        }
    }

    /** 让数字键盘重新作用于金额输入 */
    private fun clearRemarkFocus() {
        if (::etRemark.isInitialized && etRemark.hasFocus()) etRemark.clearFocus()
    }

    private fun updateKind() {
        for (tv in kindButtons) {
            val active = tv.tag as Int == kind
            tv.setTypeface(tv.typeface, if (active) Typeface.BOLD else Typeface.NORMAL)
            tv.setTextColor(if (active) Theme.primary(ctx) else Theme.subText(ctx))
        }
    }

    private fun loadCategories() {
        topCats = App.db.topCategories(kind)
        if (topCats.isEmpty()) {
            selectedTop = null
            selectedSub = null
        } else {
            selectedTop = selectedTop?.takeIf { it.kind == kind && it.parent == 0L } ?: topCats.first()
            selectedSub = selectedSub?.takeIf {
                it.kind == kind && selectedTop?.id == it.parent
            } ?: subCategoriesOf(selectedTop!!).firstOrNull()
        }
        topAdapter = CatAdapter(topCats, true)
        topGrid.adapter = topAdapter
        refreshSubGrid()
        updateKind()
    }

    private fun subCategoriesOf(top: Category): List<Category> = App.db.subCategories(top.id)

    private fun refreshSubGrid() {
        subCats = selectedTop?.let { subCategoriesOf(it) } ?: emptyList()
        subRow.visibility = if (subCats.isEmpty()) View.GONE else View.VISIBLE
        if (subCats.isNotEmpty()) {
            subAdapter = CatAdapter(subCats, false)
            subGrid.adapter = subAdapter
        }
    }

    private fun pickAccount() {
        val names = accounts.map { it.name }.toTypedArray()
        android.app.AlertDialog.Builder(ctx)
            .setTitle("选择账户")
            .setItems(names) { _, which ->
                selectedAccount = accounts[which]
                refreshSelection()
            }
            .show()
    }

    private fun pickDate() {
        val cal = DateUtil.calOf(day)
        var selY = cal.get(Calendar.YEAR)
        var selM = cal.get(Calendar.MONTH) + 1
        var selD = cal.get(Calendar.DAY_OF_MONTH)

        val yearCol = PickerColumn(ctx)
        val monthCol = PickerColumn(ctx)
        val dayCol = PickerColumn(ctx)

        fun lastDay(y: Int, m: Int): Int = DateUtil.lastDayOfMonth(y * 100 + m) % 100

        val years = (selY - 8..selY + 8).toList()
        val months = (1..12).toList()

        fun refreshDayCol() {
            val max = lastDay(selY, selM)
            if (selD > max) selD = max
            dayCol.setData((1..max).map { "${it}日" }, selD - 1)
        }

        yearCol.setData(years.map { "${it}年" }, years.indexOf(selY))
        monthCol.setData(months.map { "${it}月" }, selM - 1)
        refreshDayCol()

        yearCol.listView.setOnItemClickListener { _, _, p, _ ->
            selY = years[p]
            yearCol.select(p)
            refreshDayCol()
        }
        monthCol.listView.setOnItemClickListener { _, _, p, _ ->
            selM = months[p]
            monthCol.select(p)
            refreshDayCol()
        }
        dayCol.listView.setOnItemClickListener { _, _, p, _ ->
            selD = p + 1
            dayCol.select(p)
        }

        val body = UiKit.horizontal(ctx).apply { gravity = Gravity.CENTER }
        val colH = Theme.dp(ctx, 280)
        body.addView(yearCol.listView, LinearLayout.LayoutParams(0, colH, 1f))
        body.addView(monthCol.listView, LinearLayout.LayoutParams(0, colH, 1f))
        body.addView(dayCol.listView, LinearLayout.LayoutParams(0, colH, 1f))

        android.app.AlertDialog.Builder(ctx)
            .setTitle("选择日期")
            .setView(body)
            .setPositiveButton("确定") { _, _ ->
                day = selY * 10000 + selM * 100 + selD
                refreshSelection()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun pickTime() {
        var selH = time / 3600
        var selM = (time % 3600) / 60

        val hourCol = PickerColumn(ctx)
        val minCol = PickerColumn(ctx)
        hourCol.setData((0..23).map { String.format(Locale.CHINA, "%02d时", it) }, selH)
        minCol.setData((0..59).map { String.format(Locale.CHINA, "%02d分", it) }, selM)
        hourCol.listView.setOnItemClickListener { _, _, p, _ -> hourCol.select(p); selH = p }
        minCol.listView.setOnItemClickListener { _, _, p, _ -> minCol.select(p); selM = p }

        val body = UiKit.horizontal(ctx).apply { gravity = Gravity.CENTER }
        val colH = Theme.dp(ctx, 280)
        body.addView(hourCol.listView, LinearLayout.LayoutParams(0, colH, 1f))
        body.addView(minCol.listView, LinearLayout.LayoutParams(0, colH, 1f))

        android.app.AlertDialog.Builder(ctx)
            .setTitle("选择时间")
            .setView(body)
            .setPositiveButton("确定") { _, _ ->
                time = selH * 3600 + selM * 60
                refreshSelection()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    /** 列表式选择列：显示一组中文选项，点选后高亮 */
    private inner class PickerColumn(context: Context) {
        val listView = ListView(context)
        private var items: List<String> = emptyList()
        private var selected: Int = 0
        private val adapter = object : BaseAdapter() {
            override fun getCount(): Int = items.size
            override fun getItem(p: Int): Any = items[p]
            override fun getItemId(p: Int): Long = p.toLong()
            override fun getView(p: Int, cv: View?, parent: ViewGroup?): View {
                val tv = (cv as? TextView) ?: UiKit.text(context, "", 14f,
                    Theme.mainText(context), gravity = Gravity.CENTER).apply {
                    setPadding(0, Theme.dp(context, 12), 0, Theme.dp(context, 12))
                }
                tv.text = items[p]
                val sel = p == selected
                tv.setTextColor(if (sel) Theme.primary(context) else Theme.mainText(context))
                tv.setTypeface(tv.typeface, if (sel) Typeface.BOLD else Typeface.NORMAL)
                return tv
            }
        }

        init {
            listView.adapter = adapter
            listView.divider = null
            listView.setSelector(android.R.color.transparent)
        }

        fun setData(newItems: List<String>, newSelected: Int) {
            items = newItems
            selected = newSelected
            adapter.notifyDataSetChanged()
            listView.post { listView.setSelection(newSelected) }
        }

        fun select(p: Int) {
            selected = p
            adapter.notifyDataSetChanged()
        }
    }

    private fun refreshSelection() {
        tvAccount.text = selectedAccount?.name ?: "无账户"
        tvDate.text = DateUtil.dayTitle(day)
        tvTime.text = DateUtil.timeText(time)
        topAdapter?.notifyDataSetChanged()
        subAdapter?.notifyDataSetChanged()
    }

    private fun saveBill() {
        val cents = Money.parseToCents(amountStr) ?: 0L
        if (cents <= 0) {
            Toast.makeText(ctx, R.string.toast_require_amount, Toast.LENGTH_SHORT).show()
            return
        }
        val catId = selectedSub?.id ?: selectedTop?.id ?: -1L
        if (catId <= 0) {
            Toast.makeText(ctx, R.string.toast_require_category, Toast.LENGTH_SHORT).show()
            return
        }
        val accountId = selectedAccount?.id ?: accounts.firstOrNull()?.id ?: 0L
        val ledgerId = App.db.activeLedger().id
        val remark = etRemark.text.toString().trim()

        if (editId > 0) {
            App.db.updateBill(editId, accountId, catId, kind, cents, remark, day, time)
        } else {
            App.db.addBill(ledgerId, accountId, catId, kind, cents, remark, day, time)
        }
        Prefs.rememberRecordDay(day)
        Toast.makeText(ctx, R.string.toast_saved, Toast.LENGTH_SHORT).show()
        finish()
    }

    /* ---------------- 分类网格适配器 ---------------- */

    inner class CatAdapter(private val items: List<Category>, private val isTop: Boolean) : BaseAdapter() {
        override fun getCount(): Int = items.size
        override fun getItem(pos: Int): Any = items[pos]
        override fun getItemId(pos: Int): Long = items[pos].id

        override fun getView(pos: Int, convertView: View?, parent: ViewGroup?): View {
            val cat = items[pos]
            val selected = (if (isTop) selectedTop else selectedSub)?.id == cat.id
            val box = UiKit.vertical(this@RecordActivity).apply {
                gravity = Gravity.CENTER
                setPadding(Theme.dp(this@RecordActivity, 2), Theme.dp(this@RecordActivity, 4),
                    Theme.dp(this@RecordActivity, 2), Theme.dp(this@RecordActivity, 4))
                background = UiKit.rounded(this@RecordActivity,
                    if (selected) lightTint(cat.color) else 0x00000000, 10)
            }
            val circle = UiKit.horizontal(this@RecordActivity).apply {
                gravity = Gravity.CENTER
                background = UiKit.rounded(this@RecordActivity, cat.color, 20)
            }
            val lp = LinearLayout.LayoutParams(Theme.dp(this@RecordActivity, 40), Theme.dp(this@RecordActivity, 40))
            val icon = ImageView(this@RecordActivity).apply {
                setImageResource(CatIcon.of(cat))
            }
            circle.addView(icon, LinearLayout.LayoutParams(Theme.dp(this@RecordActivity, 24), Theme.dp(this@RecordActivity, 24)))
            box.addView(circle, lp)
            val name = UiKit.text(this@RecordActivity, cat.name, 12f,
                if (selected) cat.color else Theme.mainText(this@RecordActivity))
            box.addView(name, LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                topMargin = Theme.dp(this@RecordActivity, 3)
            })
            box.setOnClickListener {
                clearRemarkFocus()
                if (isTop) {
                    selectedTop = cat
                    selectedSub = null
                    refreshSubGrid()
                    topAdapter?.notifyDataSetChanged()
                } else {
                    selectedSub = cat
                    subAdapter?.notifyDataSetChanged()
                }
            }
            return box
        }

        private fun lightTint(color: Int): Int {
            val a = 0x20
            return (a shl 24) or (color and 0xFFFFFF)
        }
    }
}

