package com.localbill.db

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.localbill.model.Account
import com.localbill.model.Bill
import com.localbill.model.Category
import com.localbill.model.Kinds
import com.localbill.model.Ledger
import com.localbill.util.C

class LocalBillDB(ctx: Context) : SQLiteOpenHelper(ctx, "localbill.db", null, 2) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE ledger(
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                is_default INTEGER NOT NULL DEFAULT 0,
                sort INTEGER NOT NULL DEFAULT 0)"""
        )
        db.execSQL(
            """CREATE TABLE account(
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                ledger_id INTEGER NOT NULL,
                name TEXT NOT NULL,
                type INTEGER NOT NULL DEFAULT 0,
                balance INTEGER NOT NULL DEFAULT 0,
                color INTEGER NOT NULL,
                sort INTEGER NOT NULL DEFAULT 0)"""
        )
        db.execSQL(
            """CREATE TABLE category(
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                parent INTEGER NOT NULL DEFAULT 0,
                name TEXT NOT NULL,
                kind INTEGER NOT NULL DEFAULT 1,
                color INTEGER NOT NULL,
                is_system INTEGER NOT NULL DEFAULT 0,
                sort INTEGER NOT NULL DEFAULT 0)"""
        )
        db.execSQL(
            """CREATE TABLE bill(
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                ledger_id INTEGER NOT NULL,
                account_id INTEGER NOT NULL,
                category_id INTEGER NOT NULL,
                kind INTEGER NOT NULL,
                amount INTEGER NOT NULL,
                remark TEXT NOT NULL DEFAULT '',
                day INTEGER NOT NULL,
                time INTEGER NOT NULL DEFAULT 0,
                created_at INTEGER NOT NULL,
                is_deleted INTEGER NOT NULL DEFAULT 0,
                deleted_at INTEGER NOT NULL DEFAULT 0)"""
        )
        db.execSQL("CREATE INDEX idx_bill_day ON bill(day)")
        db.execSQL("CREATE INDEX idx_bill_ledger_day ON bill(ledger_id, day)")
        db.execSQL("CREATE INDEX idx_category_parent ON category(parent)")
        seed(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // 无需兼容旧版本：schema 变更直接维护在 onCreate，DB 版本号用于标识当前结构
    }

    private fun seed(db: SQLiteDatabase) {
        db.insert("ledger", null, ContentValues().apply {
            put("name", "默认账本")
            put("is_default", 1)
            put("sort", 0)
        })

        seedAccounts(db)
        seedCategories(db)
    }

    private fun seedAccounts(db: SQLiteDatabase) {
        val list = listOf(
            Triple("现金", 0, C.ANT_ORANGE),
            Triple("银行卡", 1, C.ANT_BLUE),
            Triple("支付宝", 2, C.ANT_BLUE),
            Triple("微信", 2, C.ANT_GREEN),
            Triple("信用卡", 3, C.ANT_RED)
        )
        list.forEachIndexed { i, (name, type, color) ->
            db.insert("account", null, ContentValues().apply {
                put("ledger_id", 1)
                put("name", name)
                put("type", type)
                put("balance", 0)
                put("color", color)
                put("sort", i)
            })
        }
    }

    private fun seedCategories(db: SQLiteDatabase) {
        val expense = listOf(
            "餐饮" to C.ANT_ORANGE to listOf("早餐", "午餐", "晚餐", "外卖", "水果", "零食", "饮料", "烟酒"),
            "交通" to C.ANT_BLUE to listOf("公交地铁", "打车", "加油", "停车", "火车", "飞机", "维修保养"),
            "购物" to C.ANT_MAGENTA to listOf("服饰", "日用百货", "美妆", "数码", "家电", "图书文具"),
            "居住" to C.ANT_PURPLE to listOf("房租", "房贷", "水电燃气", "物业", "装修", "家居"),
            "医疗" to C.ANT_CYAN to listOf("药品", "医院", "体检", "健身", "保险"),
            "娱乐" to C.ANT_GOLD to listOf("电影", "游戏", "旅游", "宠物", "运动", "唱歌"),
            "通讯" to C.ANT_GREEN to listOf("话费", "宽带", "会员订阅"),
            "人情" to C.ANT_RED to listOf("礼物", "红包", "请客", "长辈", "随礼"),
            "学习" to C.ANT_INDIGO to listOf("学费", "培训", "书籍", "考试报名"),
            "其他" to C.ANT_GRAY to listOf("其他")
        )
        expense.forEachIndexed { topIdx, entry ->
            val top = entry.first
            val subs = entry.second
            val cv = ContentValues().apply {
                put("parent", 0)
                put("name", top.first)
                put("kind", 1)
                put("color", top.second)
                put("is_system", 1)
                put("sort", topIdx)
            }
            val topId = db.insert("category", null, cv)
            subs.forEachIndexed { subIdx, name ->
                db.insert("category", null, ContentValues().apply {
                    put("parent", topId)
                    put("name", name)
                    put("kind", 1)
                    put("color", top.second)
                    put("is_system", 1)
                    put("sort", subIdx)
                })
            }
        }

        val income = listOf(
            "工资" to C.ANT_GREEN,
            "兼职" to C.ANT_GREEN,
            "理财" to C.ANT_GOLD,
            "红包" to C.ANT_RED,
            "报销" to C.ANT_BLUE,
            "其他收入" to C.ANT_GRAY
        )
        income.forEachIndexed { i, (name, color) ->
            db.insert("category", null, ContentValues().apply {
                put("parent", 0)
                put("name", name)
                put("kind", 2)
                put("color", color)
                put("is_system", 1)
                put("sort", i)
            })
        }
    }

    /* ---------------- Ledger ---------------- */

    fun ledgers(): List<Ledger> {
        val out = ArrayList<Ledger>()
        db.query("ledger", null, null, null, null, null, "sort ASC").use { c ->
            while (c.moveToNext()) out.add(cursorLedger(c))
        }
        return out
    }

    fun activeLedger(): Ledger {
        val id = com.localbill.util.Prefs.activeLedgerId
        val l = ledgers().firstOrNull { it.id == id } ?: ledgers().firstOrNull { it.isDefault } ?: ledgers().first()
        com.localbill.util.Prefs.activeLedgerId = l.id
        return l
    }

    fun addLedger(name: String): Long {
        return db.insert("ledger", null, ContentValues().apply {
            put("name", name)
            put("is_default", 0)
            put("sort", ledgers().size)
        })
    }

    fun deleteLedger(id: Long) {
        if (id <= 0) return
        db.delete("ledger", "id=?", arrayOf(id.toString()))
        val rest = ledgers()
        if (rest.isEmpty()) {
            addLedger("默认账本")
        } else if (com.localbill.util.Prefs.activeLedgerId == id) {
            com.localbill.util.Prefs.activeLedgerId = rest[0].id
        }
    }

    fun renameLedger(id: Long, name: String) {
        db.update("ledger", ContentValues().apply { put("name", name) }, "id=?", arrayOf(id.toString()))
    }

    private fun cursorLedger(c: Cursor): Ledger = Ledger(
        c.getLong(c.getColumnIndexOrThrow("id")),
        c.getString(c.getColumnIndexOrThrow("name")),
        c.getInt(c.getColumnIndexOrThrow("is_default")) == 1,
        c.getInt(c.getColumnIndexOrThrow("sort"))
    )

    /* ---------------- Account ---------------- */

    fun accounts(ledgerId: Long): List<Account> {
        val out = ArrayList<Account>()
        db.query("account", null, "ledger_id=?", arrayOf(ledgerId.toString()), null, null, "sort ASC").use { c ->
            while (c.moveToNext()) out.add(cursorAccount(c))
        }
        return out
    }

    fun accountById(id: Long): Account? {
        if (id <= 0) return null
        db.query("account", null, "id=?", arrayOf(id.toString()), null, null, null).use { c ->
            return if (c.moveToFirst()) cursorAccount(c) else null
        }
    }

    fun addAccount(ledgerId: Long, name: String, type: Int, balance: Long, color: Int): Long {
        return db.insert("account", null, ContentValues().apply {
            put("ledger_id", ledgerId)
            put("name", name)
            put("type", type)
            put("balance", balance)
            put("color", color)
            put("sort", accounts(ledgerId).size)
        })
    }

    fun updateAccount(id: Long, name: String, type: Int, balance: Long, color: Int) {
        db.update("account", ContentValues().apply {
            put("name", name)
            put("type", type)
            put("balance", balance)
            put("color", color)
        }, "id=?", arrayOf(id.toString()))
    }

    fun deleteAccount(id: Long) {
        db.delete("account", "id=?", arrayOf(id.toString()))
    }

    fun totalAssets(ledgerId: Long): Long {
        var total = 0L
        db.rawQuery("SELECT SUM(balance) FROM account WHERE ledger_id=?", arrayOf(ledgerId.toString())).use { c ->
            if (c.moveToFirst() && !c.isNull(0)) total = c.getLong(0)
        }
        return total
    }

    private fun cursorAccount(c: Cursor): Account = Account(
        c.getLong(c.getColumnIndexOrThrow("id")),
        c.getLong(c.getColumnIndexOrThrow("ledger_id")),
        c.getString(c.getColumnIndexOrThrow("name")),
        c.getInt(c.getColumnIndexOrThrow("type")),
        c.getLong(c.getColumnIndexOrThrow("balance")),
        c.getInt(c.getColumnIndexOrThrow("color")),
        c.getInt(c.getColumnIndexOrThrow("sort"))
    )

    /* ---------------- Category ---------------- */

    fun topCategories(kind: Int): List<Category> {
        val out = ArrayList<Category>()
        db.query("category", null, "parent=0 AND kind=?", arrayOf(kind.toString()), null, null, "sort ASC").use { c ->
            while (c.moveToNext()) out.add(cursorCategory(c))
        }
        return out
    }

    /** 按名称查一级分类（parent=0），用于自动记账按商家匹配分类 */
    fun categoryByName(name: String, kind: Int): Category? {
        db.query("category", null, "name=? AND kind=? AND parent=0",
            arrayOf(name, kind.toString()), null, null, "sort ASC").use { c ->
            return if (c.moveToFirst()) cursorCategory(c) else null
        }
    }

    fun subCategories(parentId: Long): List<Category> {
        val out = ArrayList<Category>()
        db.query("category", null, "parent=?", arrayOf(parentId.toString()), null, null, "sort ASC").use { c ->
            while (c.moveToNext()) out.add(cursorCategory(c))
        }
        return out
    }

    fun categoryById(id: Long): Category? {
        if (id <= 0) return null
        db.query("category", null, "id=?", arrayOf(id.toString()), null, null, null).use { c ->
            return if (c.moveToFirst()) cursorCategory(c) else null
        }
    }

    fun addCategory(parent: Long, name: String, kind: Int, color: Int): Long {
        return db.insert("category", null, ContentValues().apply {
            put("parent", parent)
            put("name", name)
            put("kind", kind)
            put("color", color)
            put("is_system", 0)
            put("sort", subCategories(parent).size + if (parent == 0L) topCategories(kind).size else 0)
        })
    }

    fun updateCategory(id: Long, name: String, color: Int) {
        db.update("category", ContentValues().apply {
            put("name", name)
            put("color", color)
        }, "id=?", arrayOf(id.toString()))
    }

    fun categoryUsed(catId: Long): Boolean {
        db.rawQuery("SELECT COUNT(*) FROM bill WHERE category_id=?", arrayOf(catId.toString())).use { c ->
            if (c.moveToFirst()) return c.getLong(0) > 0
        }
        return false
    }

    fun deleteCategory(id: Long): Boolean {
        val cat = categoryById(id) ?: return false
        if (cat.isSystem) return false
        if (categoryUsed(id)) return false
        if (cat.parent == 0L) {
            subCategories(id).forEach { sub ->
                if (!sub.isSystem) db.delete("category", "id=?", arrayOf(sub.id.toString()))
            }
        }
        db.delete("category", "id=?", arrayOf(id.toString()))
        return true
    }

    private fun cursorCategory(c: Cursor): Category = Category(
        c.getLong(c.getColumnIndexOrThrow("id")),
        c.getLong(c.getColumnIndexOrThrow("parent")),
        c.getString(c.getColumnIndexOrThrow("name")),
        c.getInt(c.getColumnIndexOrThrow("kind")),
        c.getInt(c.getColumnIndexOrThrow("color")),
        c.getInt(c.getColumnIndexOrThrow("is_system")) == 1,
        c.getInt(c.getColumnIndexOrThrow("sort"))
    )

    /* ---------------- Bill ---------------- */

    fun addBill(
        ledgerId: Long, accountId: Long, categoryId: Long, kind: Int,
        amount: Long, remark: String, day: Int, time: Int
    ): Long {
        val id = db.insert("bill", null, ContentValues().apply {
            put("ledger_id", ledgerId)
            put("account_id", accountId)
            put("category_id", categoryId)
            put("kind", kind)
            put("amount", amount)
            put("remark", remark)
            put("day", day)
            put("time", time)
            put("created_at", System.currentTimeMillis())
            put("is_deleted", 0)
            put("deleted_at", 0)
        })
        billById(id)?.let { applyBalance(it, +1) }
        return id
    }

    fun updateBill(
        id: Long, accountId: Long, categoryId: Long, kind: Int,
        amount: Long, remark: String, day: Int, time: Int
    ) {
        billById(id)?.let { applyBalance(it, -1) }
        db.update("bill", ContentValues().apply {
            put("account_id", accountId)
            put("category_id", categoryId)
            put("kind", kind)
            put("amount", amount)
            put("remark", remark)
            put("day", day)
            put("time", time)
        }, "id=?", arrayOf(id.toString()))
        billById(id)?.let { applyBalance(it, +1) }
    }

    /** 账单对账户余额的影响：收入 +amount，支出 -amount。sign=+1 应用，sign=-1 回退 */
    private fun applyBalance(bill: Bill, sign: Long) {
        val account = accountById(bill.accountId) ?: return
        val delta = if (bill.kind == Kinds.INCOME) bill.amount else -bill.amount
        val amount = delta * sign
        db.execSQL(
            "UPDATE account SET balance = balance + ? WHERE id=?",
            arrayOf(amount.toString(), bill.accountId.toString())
        )
    }

    fun billById(id: Long): Bill? {
        if (id <= 0) return null
        db.query("bill", null, "id=?", arrayOf(id.toString()), null, null, null).use { c ->
            return if (c.moveToFirst()) cursorBill(c) else null
        }
    }

    /** 有效账单（非回收站） */
    fun bills(ledgerId: Long, fromDay: Int, toDay: Int): List<Bill> {
        val out = ArrayList<Bill>()
        db.query(
            "bill", null,
            "ledger_id=? AND is_deleted=0 AND day BETWEEN ? AND ?",
            arrayOf(ledgerId.toString(), fromDay.toString(), toDay.toString()),
            null, null, "day DESC, time DESC, created_at DESC"
        ).use { c -> while (c.moveToNext()) out.add(cursorBill(c)) }
        return out
    }

    fun softDelete(id: Long) {
        billById(id)?.let { applyBalance(it, -1) }
        db.update("bill", ContentValues().apply {
            put("is_deleted", 1)
            put("deleted_at", System.currentTimeMillis())
        }, "id=?", arrayOf(id.toString()))
    }

    fun restore(id: Long) {
        billById(id)?.let { applyBalance(it, +1) }
        db.update("bill", ContentValues().apply {
            put("is_deleted", 0)
            put("deleted_at", 0)
        }, "id=?", arrayOf(id.toString()))
    }

    fun hardDelete(id: Long) {
        billById(id)?.let { applyBalance(it, -1) }
        db.delete("bill", "id=?", arrayOf(id.toString()))
    }

    fun recycledBills(): List<Bill> {
        val out = ArrayList<Bill>()
        db.query("bill", null, "is_deleted=1", null, null, null, "deleted_at DESC").use { c ->
            while (c.moveToNext()) out.add(cursorBill(c))
        }
        return out
    }

    fun emptyRecycle() {
        db.delete("bill", "is_deleted=1", null)
    }

    fun sumKind(ledgerId: Long, kind: Int, fromDay: Int, toDay: Int): Long {
        var sum = 0L
        db.rawQuery(
            "SELECT COALESCE(SUM(amount),0) FROM bill WHERE ledger_id=? AND kind=? AND is_deleted=0 AND day BETWEEN ? AND ?",
            arrayOf(ledgerId.toString(), kind.toString(), fromDay.toString(), toDay.toString())
        ).use { c -> if (c.moveToFirst()) sum = c.getLong(0) }
        return sum
    }

    fun sumKindMonth(ledgerId: Long, kind: Int, monthKey: Int): Long {
        return sumKind(ledgerId, kind, firstDayOf(monthKey), lastDayOf(monthKey))
    }

    fun sumKindByMonth(ledgerId: Long, kind: Int, monthKey: Int): Long {
        return sumKindMonth(ledgerId, kind, monthKey)
    }

    /** 按一级分类聚合金额（不含已删除） */
    fun categoryAmounts(ledgerId: Long, kind: Int, fromDay: Int, toDay: Int): List<Pair<Category, Long>> {
        val map = HashMap<Long, Long>()
        val cats = HashMap<Long, Category>()
        topCategories(kind).forEach { cats[it.id] = it }
        db.rawQuery(
            """SELECT b.category_id, COALESCE(SUM(b.amount),0)
               FROM bill b
               WHERE b.ledger_id=? AND b.kind=? AND b.is_deleted=0 AND b.day BETWEEN ? AND ?
               GROUP BY b.category_id""",
            arrayOf(ledgerId.toString(), kind.toString(), fromDay.toString(), toDay.toString())
        ).use { c ->
            while (c.moveToNext()) {
                val catId = c.getLong(0)
                val amt = c.getLong(1)
                // 归类到一级分类
                val cat = categoryById(catId) ?: continue
                val topId = if (cat.parent == 0L) cat.id else cat.parent
                map[topId] = (map[topId] ?: 0L) + amt
            }
        }
        val out = ArrayList<Pair<Category, Long>>()
        map.forEach { (topId, amt) ->
            cats[topId]?.let { out.add(it to amt) }
        }
        out.sortByDescending { it.second }
        return out
    }

    /** 近 n 月某类金额，返回 [月份, 金额] 列表，按时间升序 */
    fun monthlySeries(ledgerId: Long, kind: Int, months: Int): List<Pair<Int, Long>> {
        val nowMonth = com.localbill.util.DateUtil.monthNow()
        val out = ArrayList<Pair<Int, Long>>()
        for (i in months - 1 downTo 0) {
            val mk = com.localbill.util.DateUtil.addMonths(nowMonth, -i)
            out.add(mk to sumKindMonth(ledgerId, kind, mk))
        }
        return out
    }

    private fun firstDayOf(monthKey: Int): Int = monthKey * 100 + 1

    private fun lastDayOf(monthKey: Int): Int {
        val y = monthKey / 100
        val m = monthKey % 100
        return com.localbill.util.DateUtil.lastDayOfMonth(monthKey).let { it }
    }

    private fun cursorBill(c: Cursor): Bill = Bill(
        c.getLong(c.getColumnIndexOrThrow("id")),
        c.getLong(c.getColumnIndexOrThrow("ledger_id")),
        c.getLong(c.getColumnIndexOrThrow("account_id")),
        c.getLong(c.getColumnIndexOrThrow("category_id")),
        c.getInt(c.getColumnIndexOrThrow("kind")),
        c.getLong(c.getColumnIndexOrThrow("amount")),
        c.getString(c.getColumnIndexOrThrow("remark")),
        c.getInt(c.getColumnIndexOrThrow("day")),
        c.getInt(c.getColumnIndexOrThrow("time")),
        c.getLong(c.getColumnIndexOrThrow("created_at")),
        c.getInt(c.getColumnIndexOrThrow("is_deleted")),
        c.getLong(c.getColumnIndexOrThrow("deleted_at"))
    )

    /* ---------------- Backup ---------------- */

    fun allLedgers(): List<Ledger> = ledgers()

    fun allAccounts(): List<Account> {
        val out = ArrayList<Account>()
        db.query("account", null, null, null, null, null, "id ASC").use { c ->
            while (c.moveToNext()) out.add(cursorAccount(c))
        }
        return out
    }

    fun allCategories(): List<Category> {
        val out = ArrayList<Category>()
        db.query("category", null, null, null, null, null, "id ASC").use { c ->
            while (c.moveToNext()) out.add(cursorCategory(c))
        }
        return out
    }

    fun allBills(): List<Bill> {
        val out = ArrayList<Bill>()
        db.query("bill", null, null, null, null, null, "id ASC").use { c ->
            while (c.moveToNext()) out.add(cursorBill(c))
        }
        return out
    }

    fun clearAll() {
        db.delete("bill", null, null)
        db.delete("account", null, null)
        db.delete("category", null, null)
        db.delete("ledger", null, null)
        seed(db)
        com.localbill.util.Prefs.activeLedgerId = 1
    }

    fun restoreFromBackup(
        ledgers: List<Ledger>,
        accounts: List<Account>,
        categories: List<Category>,
        bills: List<Bill>
    ) {
        db.delete("bill", null, null)
        db.delete("account", null, null)
        db.delete("category", null, null)
        db.delete("ledger", null, null)
        ledgers.forEach { l ->
            db.insert("ledger", null, ContentValues().apply {
                put("id", l.id)
                put("name", l.name)
                put("is_default", if (l.isDefault) 1 else 0)
                put("sort", l.sort)
            })
        }
        accounts.forEach { a ->
            db.insert("account", null, ContentValues().apply {
                put("id", a.id)
                put("ledger_id", a.ledgerId)
                put("name", a.name)
                put("type", a.type)
                put("balance", a.balance)
                put("color", a.color)
                put("sort", a.sort)
            })
        }
        categories.forEach { c ->
            db.insert("category", null, ContentValues().apply {
                put("id", c.id)
                put("parent", c.parent)
                put("name", c.name)
                put("kind", c.kind)
                put("color", c.color)
                put("is_system", if (c.isSystem) 1 else 0)
                put("sort", c.sort)
            })
        }
        bills.forEach { b ->
            db.insert("bill", null, ContentValues().apply {
                put("id", b.id)
                put("ledger_id", b.ledgerId)
                put("account_id", b.accountId)
                put("category_id", b.categoryId)
                put("kind", b.kind)
                put("amount", b.amount)
                put("remark", b.remark)
                put("day", b.day)
                put("time", b.time)
                put("created_at", b.createdAt)
                put("is_deleted", b.isDeleted)
                put("deleted_at", b.deletedAt)
            })
        }
        if (ledgers.isNotEmpty()) {
            com.localbill.util.Prefs.activeLedgerId = ledgers[0].id
        }
    }

    private val db: SQLiteDatabase get() = writableDatabase
}
