package com.localbill.ui
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.localbill.App
import com.localbill.R
import com.localbill.model.Account
import com.localbill.model.Bill
import com.localbill.model.Category
import com.localbill.model.Ledger
import com.localbill.util.C
import com.localbill.util.DateUtil
import com.localbill.util.Theme
import com.localbill.util.UiKit
import org.json.JSONArray
import org.json.JSONObject

class BackupActivity : Activity() {
    private val ctx: Context get() = this@BackupActivity

    companion object {
        const val EXTRA_MODE = "mode"
        const val MODE_RESTORE = 1
        private const val REQ_EXPORT = 1001
        private const val REQ_IMPORT = 1002
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(if (com.localbill.util.Prefs.darkMode) R.style.Theme_LocalBill_Dark else R.style.Theme_LocalBill)
        super.onCreate(savedInstanceState)
        buildUi()
        if (intent.getIntExtra(EXTRA_MODE, 0) == MODE_RESTORE) {
            importBackup()
        }
    }

    private fun buildUi() {
        val root = UiKit.vertical(ctx).apply { setBackgroundColor(Theme.pageBg(ctx)) }

        val topBar = UiKit.horizontal(ctx).apply { gravity = Gravity.CENTER_VERTICAL }
        val back = ImageView(ctx)
        back.setImageResource(R.drawable.ic_back)
        back.setColorFilter(Theme.subText(ctx))
        back.setPadding(Theme.dp(ctx, 16), Theme.dp(ctx, 14), Theme.dp(ctx, 14), Theme.dp(ctx, 14))
        back.setOnClickListener { finish() }
        topBar.addView(back, LinearLayout.LayoutParams(Theme.dp(ctx, 48), Theme.dp(ctx, 48)))
        topBar.addView(UiKit.text(ctx, "数据备份", 17f, Theme.mainText(ctx), bold = true, gravity = Gravity.CENTER),
            LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
        topBar.addView(View(ctx), LinearLayout.LayoutParams(Theme.dp(ctx, 48), 0))
        root.addView(topBar, LinearLayout.LayoutParams(MATCH_PARENT, Theme.dp(ctx, 52)))

        val export = bigBtn("导出备份", "将全部账目数据导出为 JSON 文件保存到本地", C.PRIMARY) { exportBackup() }
        val restore = bigBtn("恢复备份", "从之前导出的 JSON 文件恢复数据（将覆盖当前数据）", C.ANT_GREEN) { importBackup() }

        val tip = UiKit.text(ctx, "所有数据仅保存在本机，不经过任何网络。",
            12f, Theme.lightText(ctx), gravity = Gravity.CENTER)
        tip.setPadding(0, Theme.dp(ctx, 24), 0, 0)

        root.addView(export)
        root.addView(restore)
        root.addView(tip)
        setContentView(root)
    }

    private fun bigBtn(title: String, subtitle: String, color: Int, onClick: () -> Unit): LinearLayout {
        val row = UiKit.horizontal(ctx).apply {
            gravity = Gravity.CENTER_VERTICAL
            background = UiKit.rounded(ctx, Theme.surface(ctx), 14)
            setPadding(Theme.dp(ctx, 16), Theme.dp(ctx, 16), Theme.dp(ctx, 16), Theme.dp(ctx, 16))
            isClickable = true
            isFocusable = true
        }
        val lp = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        lp.setMargins(Theme.dp(ctx, 12), Theme.dp(ctx, 4), Theme.dp(ctx, 12), Theme.dp(ctx, 4))
        val dot = UiKit.circle(ctx, color, 12)
        row.addView(dot)
        val c = UiKit.vertical(ctx).apply {
            setPadding(Theme.dp(ctx, 12), 0, 0, 0)
        }
        c.addView(UiKit.text(ctx, title, 16f, Theme.mainText(ctx), bold = true))
        c.addView(UiKit.text(ctx, subtitle, 12f, Theme.lightText(ctx)).apply {
            setPadding(0, Theme.dp(ctx, 3), 0, 0)
        })
        row.addView(c, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
        row.addView(UiKit.text(ctx, "›", 22f, Theme.lightText(ctx)))
        row.setOnClickListener { onClick() }
        return row.apply { layoutParams = lp }
    }

    /* ---------------- 导出 ---------------- */

    private fun exportBackup() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
            putExtra(Intent.EXTRA_TITLE, "LocalBill-${DateUtil.today()}.json")
        }
        startActivityForResult(intent, REQ_EXPORT)
    }

    private fun exportTo(uri: Uri) {
        val json = buildJson().toString()
        try {
            val os = contentResolver.openOutputStream(uri) ?: throw RuntimeException("无法打开文件")
            os.use { it.write(json.toByteArray(Charsets.UTF_8)) }
            Toast.makeText(ctx, R.string.toast_backup_done, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(ctx, "导出失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun buildJson(): JSONObject {
        val root = JSONObject()
        root.put("app", "LocalBill")
        root.put("version", 1)
        root.put("exportedAt", DateUtil.now())

        root.put("ledgers", JSONArray().apply {
            App.db.allLedgers().forEach { put(ledgerJson(it)) }
        })
        root.put("accounts", JSONArray().apply {
            App.db.allAccounts().forEach { put(accountJson(it)) }
        })
        root.put("categories", JSONArray().apply {
            App.db.allCategories().forEach { put(categoryJson(it)) }
        })
        root.put("bills", JSONArray().apply {
            App.db.allBills().forEach { put(billJson(it)) }
        })
        return root
    }

    private fun ledgerJson(l: Ledger): JSONObject = JSONObject().apply {
        put("id", l.id); put("name", l.name); put("isDefault", l.isDefault); put("sort", l.sort)
    }

    private fun accountJson(a: Account): JSONObject = JSONObject().apply {
        put("id", a.id); put("ledgerId", a.ledgerId); put("name", a.name)
        put("type", a.type); put("balance", a.balance); put("color", a.color); put("sort", a.sort)
    }

    private fun categoryJson(c: Category): JSONObject = JSONObject().apply {
        put("id", c.id); put("parent", c.parent); put("name", c.name)
        put("kind", c.kind); put("color", c.color); put("isSystem", c.isSystem); put("sort", c.sort)
    }

    private fun billJson(b: Bill): JSONObject = JSONObject().apply {
        put("id", b.id); put("ledgerId", b.ledgerId); put("accountId", b.accountId); put("categoryId", b.categoryId)
        put("kind", b.kind); put("amount", b.amount); put("remark", b.remark); put("day", b.day)
        put("createdAt", b.createdAt); put("isDeleted", b.isDeleted); put("deletedAt", b.deletedAt)
    }

    /* ---------------- 导入 ---------------- */

    private fun importBackup() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
        }
        startActivityForResult(intent, REQ_IMPORT)
    }

    private fun importFrom(uri: Uri) {
        try {
            val text = contentResolver.openInputStream(uri)?.use {
                it.readBytes().toString(Charsets.UTF_8)
            } ?: throw RuntimeException("无法读取文件")
            val root = JSONObject(text)
            if (root.optString("app") != "LocalBill") {
                Toast.makeText(ctx, "不是有效的 LocalBill 备份文件", Toast.LENGTH_SHORT).show()
                return
            }
            val ledgers = ArrayList<Ledger>()
            val accounts = ArrayList<Account>()
            val categories = ArrayList<Category>()
            val bills = ArrayList<Bill>()

            val ledgerArr = root.optJSONArray("ledgers") ?: JSONArray()
            for (i in 0 until ledgerArr.length()) {
                val o = ledgerArr.getJSONObject(i)
                ledgers.add(Ledger(o.getLong("id"), o.getString("name"), o.optBoolean("isDefault"), o.optInt("sort")))
            }
            val accArr = root.optJSONArray("accounts") ?: JSONArray()
            for (i in 0 until accArr.length()) {
                val o = accArr.getJSONObject(i)
                accounts.add(Account(o.getLong("id"), o.getLong("ledgerId"), o.getString("name"),
                    o.getInt("type"), o.getLong("balance"), o.getInt("color"), o.getInt("sort")))
            }
            val catArr = root.optJSONArray("categories") ?: JSONArray()
            for (i in 0 until catArr.length()) {
                val o = catArr.getJSONObject(i)
                categories.add(Category(o.getLong("id"), o.getLong("parent"), o.getString("name"),
                    o.getInt("kind"), o.getInt("color"), o.optBoolean("isSystem"), o.getInt("sort")))
            }
            val billArr = root.optJSONArray("bills") ?: JSONArray()
            for (i in 0 until billArr.length()) {
                val o = billArr.getJSONObject(i)
                bills.add(Bill(o.getLong("id"), o.getLong("ledgerId"), o.getLong("accountId"), o.getLong("categoryId"),
                    o.getInt("kind"), o.getLong("amount"), o.optString("remark"), o.getInt("day"),
                    o.getLong("createdAt"), o.getInt("isDeleted"), o.getLong("deletedAt")))
            }

            App.db.restoreFromBackup(ledgers, accounts, categories, bills)
            Toast.makeText(ctx, R.string.toast_restore_done, Toast.LENGTH_SHORT).show()
            finish()
        } catch (e: Exception) {
            Toast.makeText(ctx, "${getString(R.string.toast_restore_fail)}: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK || data == null) return
        when (requestCode) {
            REQ_EXPORT -> data.data?.let { exportTo(it) }
            REQ_IMPORT -> data.data?.let { importFrom(it) }
        }
    }
}

