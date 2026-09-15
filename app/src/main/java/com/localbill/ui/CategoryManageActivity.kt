package com.localbill.ui
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT

import android.app.Activity
import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
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
        setTheme(Theme.themeStyle())
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

        val addBtn = UiKit.text(ctx, "+ 添加分类", 16f, Theme.primary(ctx), bold = true, gravity = Gravity.CENTER)
        addBtn.background = UiKit.rounded(ctx, Theme.primaryBg(ctx), 24)
        addBtn.setPadding(0, Theme.dp(ctx, 13), 0, Theme.dp(ctx, 13))
        val addLp = LinearLayout.LayoutParams(MATCH_PARENT, Theme.dp(ctx, 50))
        addLp.setMargins(Theme.dp(ctx, 16), Theme.dp(ctx, 6), Theme.dp(ctx, 16), Theme.dp(ctx, 16))
        addBtn.setOnClickListener { addDialog() }
        root.addView(addBtn, addLp)

        UiKit.fitSystemBars(root)
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
            tv.setTextColor(if (active) Theme.primary(ctx) else Theme.subText(ctx))
            tv.setTypeface(tv.typeface, if (active) Typeface.BOLD else Typeface.NORMAL)
        }
    }

    private fun reload() {
        items = if (parentId > 0) App.db.subCategories(parentId) else App.db.topCategories(kind)
        listView.adapter = CatListAdapter()
    }

    /**
     * 分类配色区块：色板统一为主题色系的深浅变体。
     * 图标底色已统一跟随主题色，此处颜色仅用于统计图表的扇区/排行区分。
     */
    private fun colorBlock(box: LinearLayout, initial: Int): ColorPickerView {
        val palette = Theme.palette(ctx)
        val picker = ColorPickerView(
            ctx,
            if (palette.contains(initial)) initial else palette[(palette.size - 1) / 2],
            palette,
            columns = 4
        )
        box.addView(UiKit.text(ctx, "统计图表颜色", 13f, Theme.subText(ctx)).apply {
            setPadding(0, Theme.dp(ctx, 12), 0, 0)
        })
        box.addView(picker, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
            topMargin = Theme.dp(ctx, 6)
        })
        box.addView(UiKit.text(ctx, "图标底色统一跟随主题色，此处颜色仅供统计图表区分", 11f, Theme.lightText(ctx)).apply {
            setPadding(0, Theme.dp(ctx, 6), 0, 0)
        })
        return picker
    }

    private fun addDialog() {
        val name = EditText(ctx).apply {
            setTextSize(14f)
            hint = "分类名称"
            setTextColor(Theme.mainText(ctx))
            setHintTextColor(Theme.lightText(ctx))
        }
        val palette = Theme.palette(ctx)
        val box = UiKit.vertical(ctx)
        box.addView(name)
        val picker = colorBlock(box, palette[items.size % palette.size])
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
        val box = UiKit.vertical(ctx)
        box.addView(name)
        val picker = colorBlock(box, cat.color)
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
            lp.setMargins(Theme.dp(this@CategoryManageActivity, 24), Theme.dp(this@CategoryManageActivity, 2),
                Theme.dp(this@CategoryManageActivity, 24), Theme.dp(this@CategoryManageActivity, 2))

            val dot = UiKit.catIcon(this@CategoryManageActivity, cat, 34)
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

/** 色板选择器（多排排列，选中项带边框） */
class ColorPickerView(
    context: Context,
    initial: Int,
    colors: IntArray = C.CATEGORY_COLORS,
    private val onSelected: ((Int) -> Unit)? = null,
    private val onLongPress: ((Int) -> Unit)? = null,
    names: List<String>? = null,
    columns: Int = 5
) : LinearLayout(context) {

    var selected: Int = initial
        private set

    private val dots = ArrayList<View>()
    private val colors = colors

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        val colCount = columns.coerceAtLeast(1)
        val hasNames = names != null
        val itemW = Theme.dp(context, if (hasNames) 44 else 26)
        val dotSize = Theme.dp(context, 26)

        for (rowIndices in colors.indices.chunked(colCount)) {
            val row = LinearLayout(context).apply {
                orientation = HORIZONTAL
                gravity = Gravity.CENTER
            }
            for (i in rowIndices) {
                val color = colors[i]
                val dot = UiKit.circle(context, color, 26)
                dots.add(dot)
                val item: View
                if (hasNames) {
                    // 每个色点下方带名称（对齐），名称可为空串
                    val col = LinearLayout(context).apply {
                        orientation = VERTICAL
                        gravity = Gravity.CENTER_HORIZONTAL
                    }
                    col.addView(dot)
                    col.addView(TextView(context).apply {
                        text = names!!.getOrElse(i) { "" }
                        textSize = 10f
                        setTextColor(Theme.subText(context))
                        gravity = Gravity.CENTER
                    }, LayoutParams(Theme.dp(context, 44), WRAP_CONTENT).apply {
                        topMargin = Theme.dp(context, 2)
                    })
                    item = col
                } else {
                    item = dot
                }
                // 无名称时高度必须给定，否则 WRAP_CONTENT 会被父容器撑满（圆点被拉成椭圆）
                val lp = LayoutParams(itemW, if (hasNames) WRAP_CONTENT else dotSize)
                lp.setMargins(Theme.dp(context, 6), Theme.dp(context, 4), Theme.dp(context, 6), Theme.dp(context, 4))
                row.addView(item, lp)
                item.setOnClickListener {
                    selected = color
                    updateSelection()
                    onSelected?.invoke(color)
                }
                item.setOnLongClickListener {
                    onLongPress?.invoke(color)
                    true
                }
            }
            addView(row, LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        }
        updateSelection()
    }

    /** 选中态只用描边标记：色板是同一色系的深浅变体，靠透明度淡化会看不出彼此差异 */
    private fun updateSelection() {
        for ((i, dot) in dots.withIndex()) {
            val isSel = colors[i] == selected
            val d = dot.background as? GradientDrawable
            d?.setStroke(
                if (isSel) Theme.dp(context, 2) else 0,
                if (isSel) Theme.mainText(context) else 0
            )
        }
    }
}

