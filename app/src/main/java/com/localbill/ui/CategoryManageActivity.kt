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
import android.widget.AdapterView
import android.widget.BaseAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import com.localbill.App
import com.localbill.R
import com.localbill.model.Category
import com.localbill.model.Kinds
import com.localbill.util.C
import com.localbill.util.Theme
import com.localbill.util.UiKit

class CategoryManageActivity : Activity() {
    private val ctx: Context get() = this@CategoryManageActivity

    companion object {
        const val EXTRA_PARENT = "parent_id"
    }

    private var parentId = 0L
    private var kind = Kinds.EXPENSE
    private var items: List<Category> = emptyList()
    private lateinit var listView: ListView
    private val kindButtons = ArrayList<TextView>()

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(if (com.localbill.util.Prefs.darkMode) R.style.Theme_LocalBill_Dark else R.style.Theme_LocalBill)
        super.onCreate(savedInstanceState)
        parentId = intent.getLongExtra(EXTRA_PARENT, 0L)
        if (parentId > 0) {
            kind = App.db.categoryById(parentId)?.kind ?: Kinds.EXPENSE
        }
        buildUi()
        reload()
    }

    private fun buildUi() {
        val root = UiKit.vertical(ctx).apply { setBackgroundColor(Theme.pageBg(ctx)) }

        // 顶栏
        val topBar = UiKit.horizontal(ctx).apply { gravity = Gravity.CENTER_VERTICAL }
        val back = ImageView(ctx)
        back.setImageResource(R.drawable.ic_back)
        back.setColorFilter(Theme.subText(ctx))
        back.setPadding(Theme.dp(ctx, 16), Theme.dp(ctx, 14), Theme.dp(ctx, 14), Theme.dp(ctx, 14))
        back.setOnClickListener { finish() }
        topBar.addView(back, LinearLayout.LayoutParams(Theme.dp(ctx, 48), Theme.dp(ctx, 48)))
        topBar.addView(
            UiKit.text(ctx, if (parentId > 0) "子分类" else "分类管理", 17f,
                Theme.mainText(ctx), bold = true, gravity = Gravity.CENTER),
            LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
        topBar.addView(View(ctx), LinearLayout.LayoutParams(Theme.dp(ctx, 48), 0))
        root.addView(topBar, LinearLayout.LayoutParams(MATCH_PARENT, Theme.dp(ctx, 52)))

        // 仅一级分类显示收支切换
        if (parentId == 0L) {
            val seg = UiKit.horizontal(ctx).apply {
                gravity = Gravity.CENTER
                setPadding(Theme.dp(ctx, 14), Theme.dp(ctx, 4), Theme.dp(ctx, 14), Theme.dp(ctx, 4))
            }
            seg.addView(kindButton("支出", Kinds.EXPENSE), kindWeight(1))
            seg.addView(kindButton("收入", Kinds.INCOME), kindWeight(1))
            root.addView(seg, LinearLayout.LayoutParams(MATCH_PARENT, Theme.dp(ctx, 46)))
            updateKind()
        }

        listView = ListView(ctx)
        listView.divider = null
        listView.setSelector(android.R.color.transparent)
        listView.onItemClickListener = AdapterView.OnItemClickListener { _, _, pos, _ ->
            val cat = items[pos]
            if (parentId == 0L) {
                val subs = App.db.subCategories(cat.id)
                if (subs.isNotEmpty()) {
                    startActivity(android.content.Intent(ctx, CategoryManageActivity::class.java)
                        .putExtra(EXTRA_PARENT, cat.id))
                } else {
                    editDialog(cat)
                }
            } else {
                editDialog(cat)
            }
        }
        listView.onItemLongClickListener = AdapterView.OnItemLongClickListener { _, _, pos, _ ->
            deleteConfirm(items[pos])
            true
        }
        root.addView(listView, LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f))

        val addBtn = UiKit.text(ctx, "+ 添加分类", 16f, C.PRIMARY, bold = true, gravity = Gravity.CENTER)
        addBtn.background = UiKit.rounded(ctx, C.PRIMARY_BG, 24)
        addBtn.setPadding(0, Theme.dp(ctx, 13), 0, Theme.dp(ctx, 13))
        val addLp = LinearLayout.LayoutParams(MATCH_PARENT, Theme.dp(ctx, 50))
        addLp.setMargins(Theme.dp(ctx, 16), Theme.dp(ctx, 6), Theme.dp(ctx, 16), Theme.dp(ctx, 16))
        addBtn.setOnClickListener { addDialog() }
        root.addView(addBtn, addLp)

        setContentView(root)
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
            updateKind()
            reload()
        }
        kindButtons.add(tv)
        return tv
    }

    private fun kindWeight(w: Int): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(0, Theme.dp(ctx, 38), w.toFloat())

    private fun updateKind() {
        for (tv in kindButtons) {
            val active = tv.tag as Int == kind
            tv.setTextColor(if (active) C.PRIMARY else Theme.subText(ctx))
            tv.setTypeface(tv.typeface, if (active) Typeface.BOLD else Typeface.NORMAL)
        }
    }

    private fun reload() {
        items = if (parentId > 0) App.db.subCategories(parentId) else App.db.topCategories(kind)
        listView.adapter = CatListAdapter()
    }

    private fun addDialog() {
        val name = EditText(ctx).apply {
            setTextSize(14f)
            hint = "分类名称"
            setTextColor(Theme.mainText(ctx))
            setHintTextColor(Theme.lightText(ctx))
        }
        val picker = ColorPickerView(ctx, C.CATEGORY_COLORS[items.size % C.CATEGORY_COLORS.size])
        val box = UiKit.vertical(ctx)
        box.addView(name)
        box.addView(picker, LinearLayout.LayoutParams(MATCH_PARENT, Theme.dp(ctx, 40)).apply {
            topMargin = Theme.dp(ctx, 10)
        })
        val dialog = android.app.AlertDialog.Builder(ctx)
            .setTitle("添加分类")
            .setView(box)
            .setPositiveButton("确定") { _, _ ->
                val n = name.text.toString().trim()
                if (n.isEmpty()) {
                    Toast.makeText(ctx, "请输入名称", Toast.LENGTH_SHORT).show()
                } else {
                    App.db.addCategory(parentId, n, kind, picker.selected)
                    reload()
                }
            }
            .setNegativeButton("取消", null)
            .create()
        dialog.show()
    }

    private fun editDialog(cat: Category) {
        val name = EditText(ctx).apply {
            setTextSize(14f)
            setText(cat.name)
            setTextColor(Theme.mainText(ctx))
        }
        val picker = ColorPickerView(ctx, cat.color)
        val box = UiKit.vertical(ctx)
        box.addView(name)
        box.addView(picker, LinearLayout.LayoutParams(MATCH_PARENT, Theme.dp(ctx, 40)).apply {
            topMargin = Theme.dp(ctx, 10)
        })
        val dialog = android.app.AlertDialog.Builder(ctx)
            .setTitle("编辑分类")
            .setView(box)
            .setPositiveButton("确定") { _, _ ->
                val n = name.text.toString().trim()
                if (n.isEmpty()) {
                    Toast.makeText(ctx, "请输入名称", Toast.LENGTH_SHORT).show()
                } else {
                    App.db.updateCategory(cat.id, n, picker.selected)
                    reload()
                }
            }
            .setNegativeButton("取消", null)
            .create()
        dialog.show()
    }

    private fun deleteConfirm(cat: Category) {
        android.app.AlertDialog.Builder(ctx)
            .setTitle("删除分类「${cat.name}」？")
            .setMessage(if (cat.parent == 0L) "将同时删除其子分类；系统预设分类不可删除。" else "仅自定义分类可删除。")
            .setPositiveButton("删除") { _, _ ->
                val ok = App.db.deleteCategory(cat.id)
                if (!ok) {
                    Toast.makeText(ctx,
                        if (cat.isSystem) "系统预设分类不可删除" else "该分类下已有账目，不可删除",
                        Toast.LENGTH_SHORT).show()
                }
                reload()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    inner class CatListAdapter : BaseAdapter() {
        override fun getCount(): Int = items.size
        override fun getItem(pos: Int): Any = items[pos]
        override fun getItemId(pos: Int): Long = items[pos].id

        override fun getView(pos: Int, convertView: View?, parent: ViewGroup?): View {
            val cat = items[pos]
            val row = UiKit.horizontal(this@CategoryManageActivity).apply {
                gravity = Gravity.CENTER_VERTICAL
                setBackgroundColor(Theme.surface(this@CategoryManageActivity))
            }
            val lp = LinearLayout.LayoutParams(MATCH_PARENT, Theme.dp(this@CategoryManageActivity, 56))
            lp.setMargins(Theme.dp(this@CategoryManageActivity, 12), Theme.dp(this@CategoryManageActivity, 2),
                Theme.dp(this@CategoryManageActivity, 12), Theme.dp(this@CategoryManageActivity, 2))

            val dot = UiKit.circle(this@CategoryManageActivity, cat.color, 34)
            val dotWrap = UiKit.horizontal(this@CategoryManageActivity).apply {
                gravity = Gravity.CENTER
                setPadding(Theme.dp(this@CategoryManageActivity, 6), 0, Theme.dp(this@CategoryManageActivity, 8), 0)
            }
            dotWrap.addView(dot)
            row.addView(dotWrap)

            val center = UiKit.vertical(this@CategoryManageActivity)
            center.gravity = Gravity.CENTER_VERTICAL
            center.addView(UiKit.text(this@CategoryManageActivity, cat.name, 15f, Theme.mainText(this@CategoryManageActivity)))
            if (parentId == 0L) {
                val subCnt = App.db.subCategories(cat.id).size
                if (subCnt > 0) {
                    center.addView(UiKit.text(this@CategoryManageActivity, "$subCnt 个子分类", 12f, Theme.lightText(this@CategoryManageActivity)))
                }
            }
            row.addView(center, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))

            row.addView(UiKit.text(this@CategoryManageActivity, "›", 20f, Theme.lightText(this@CategoryManageActivity)))
            return row.apply { layoutParams = lp }
        }
    }
}

/** 横向色板选择器 */
class ColorPickerView(context: Context, initial: Int) : LinearLayout(context) {

    var selected: Int = initial
        private set

    private val dots = ArrayList<View>()
    private val colors = C.CATEGORY_COLORS

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER
        for (color in colors) {
            val dot = UiKit.circle(context, color, 26)
            dots.add(dot)
            val lp = LayoutParams(Theme.dp(context, 26), Theme.dp(context, 26))
            lp.setMargins(Theme.dp(context, 6), 0, Theme.dp(context, 6), 0)
            addView(dot, lp)
            dot.setOnClickListener {
                selected = color
                updateAlpha()
            }
        }
        updateAlpha()
    }

    private fun updateAlpha() {
        for ((i, dot) in dots.withIndex()) {
            dot.alpha = if (colors[i] == selected) 1f else 0.3f
        }
    }
}

