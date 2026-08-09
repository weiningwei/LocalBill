package com.localbill.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.localbill.ui.RecordActivity
import com.localbill.util.DateUtil
import com.localbill.util.Money
import com.localbill.util.Prefs
import java.util.regex.Pattern

/**
 * 监听微信/支付宝付款成功界面，提取金额/商家/时间后，
 * 打开记一笔页面预填表单，由用户确认/编辑后手动保存。
 */
class BillImportService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (!Prefs.importEnabled) return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg != PKG_WECHAT && pkg != PKG_ALIPAY) return

        // 防抖：60s 内不重复触发
        val now = System.currentTimeMillis()
        if (now - Prefs.lastImportAtMillis < DEBOUNCE_MS) return

        val root = rootInActiveWindow ?: return
        try {
            val texts = ArrayList<String>()
            collectTexts(root, texts, 0)

            if (!texts.any { SUCCESS_WORDS.any { w -> it.contains(w) } }) return

            val cents = extractAmount(texts) ?: return
            if (cents <= 0L) return

            val merchant = extractMerchant(texts, pkg)
            val (day, time) = extractTime(texts)

            // 去重：同包名+同金额+同日期+同分钟
            val sig = "$pkg|$cents|$day|${time / 60}"
            if (sig == Prefs.lastImportSignature) return

            val intent = Intent(this, RecordActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra(RecordActivity.EXTRA_AMOUNT_CENTS, cents)
                putExtra(RecordActivity.EXTRA_REMARK, merchant)
                putExtra(RecordActivity.EXTRA_DAY, day)
                putExtra(RecordActivity.EXTRA_TIME, time)
                MerchantMap.match(merchant)?.let { putExtra(RecordActivity.EXTRA_CATEGORY, it) }
            }
            startActivity(intent)

            Prefs.lastImportSignature = sig
            Prefs.lastImportAtMillis = now
        } finally {
            root.recycle()
        }
    }

    override fun onInterrupt() {}

    /* ---------------- 解析 ---------------- */

    private fun collectTexts(node: AccessibilityNodeInfo, out: ArrayList<String>, depth: Int) {
        if (depth > 20) return
        node.text?.toString()?.takeIf { it.isNotBlank() }?.let { out.add(it.trim()) }
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { collectTexts(it, out, depth + 1) }
        }
    }

    // 带货币符号的金额，如 ¥12.50 / ￥ 1,234.56
    private val AMOUNT_SYMBOL_RE = Pattern.compile("[¥￥]\\s*([0-9][0-9,]*(?:\\.[0-9]{1,2})?)")
    // 带"元"结尾的金额，如 12.50元
    private val AMOUNT_YUAN_RE = Pattern.compile("([0-9][0-9,]*(?:\\.[0-9]{1,2})?)\\s*元")
    // 纯数字金额（整段即金额），仅作为兜底：要求带小数或短整数，避免误配时间/日期/订单号
    private val AMOUNT_PLAIN_RE = Pattern.compile("^([0-9][0-9,]*(?:\\.[0-9]{1,2})?)$")

    /**
     * 从全部文本中提取金额（分）。优先级：带符号 > 带"元" > 纯数字兜底。
     * 纯数字兜底有约束：必须是整段文本且不含逗号分组错误，防止把时间/日期/长订单号当金额。
     */
    private fun extractAmount(texts: List<String>): Long? {
        // 1. 带 ¥/￥ 符号
        for (t in texts) {
            val m = AMOUNT_SYMBOL_RE.matcher(t)
            if (m.find()) {
                parseAmountGroup(m.group(1))?.let { return it }
            }
        }
        // 2. 带"元"结尾
        for (t in texts) {
            val m = AMOUNT_YUAN_RE.matcher(t)
            if (m.find()) {
                parseAmountGroup(m.group(1))?.let { return it }
            }
        }
        // 3. 纯数字兜底：仅当整段文本就是金额格式，且长度在合理范围内
        for (t in texts) {
            val trimmed = t.trim()
            if (trimmed.length in 1..14) {
                val m = AMOUNT_PLAIN_RE.matcher(trimmed)
                if (m.matches()) {
                    parseAmountGroup(m.group(1))?.let { return it }
                }
            }
        }
        return null
    }

    /** 校验并转换金额字符串（含逗号分组校验），返回分；无效返回 null */
    private fun parseAmountGroup(raw: String): Long? {
        val clean = raw.replace(",", "")
        // 逗号分组校验：形如 1,234.56；不允许 12,34 或前导逗号
        if (raw.contains(",") && !raw.matches(Regex("[0-9]{1,3}(,[0-9]{3})+(\\.[0-9]{1,2})?"))) return null
        return Money.parseToCents(clean)
    }

    /** 从文本列表中提取商家名 */
    private fun extractMerchant(texts: List<String>, pkg: String): String {
        for (t in texts) {
            val idx = t.indexOf("向")
            if (idx >= 0) {
                val s = t.substring(idx + 1)
                val end = s.indexOf("付款")
                val name = if (end > 0) s.substring(0, end) else s
                if (name.isNotBlank()) return name.trim().take(30)
            }
        }
        for (t in texts) {
            for (kw in listOf("商家", "商户", "收款方")) {
                val idx = t.indexOf(kw)
                if (idx >= 0) {
                    val s = t.substring(idx + kw.length).trim()
                    if (s.isNotBlank()) return s.take(30)
                }
            }
        }
        return if (pkg == PKG_WECHAT) "微信" else "支付宝"
    }

    private val DT_RE = Pattern.compile("(\\d{4})-(\\d{2})-(\\d{2})[ T](\\d{1,2}):(\\d{2})")
    private val HM_RE = Pattern.compile("(\\d{1,2}):(\\d{2})")

    /** 提取日期与时间（day=YYYYMMDD, time=当日秒数）；无则用当前时间 */
    private fun extractTime(texts: List<String>): Pair<Int, Int> {
        for (t in texts) {
            val m = DT_RE.matcher(t)
            if (m.find()) {
                val day = m.group(1).toInt() * 10000 + m.group(2).toInt() * 100 + m.group(3).toInt()
                val time = m.group(4).toInt() * 3600 + m.group(5).toInt() * 60
                return day to time
            }
        }
        for (t in texts) {
            val m = HM_RE.matcher(t)
            if (m.find()) {
                val time = m.group(1).toInt() * 3600 + m.group(2).toInt() * 60
                return DateUtil.today() to time
            }
        }
        return DateUtil.today() to DateUtil.timeNow()
    }

    companion object {
        private const val PKG_WECHAT = "com.tencent.mm"
        private const val PKG_ALIPAY = "com.eg.android.AlipayGphone"
        private const val DEBOUNCE_MS = 60_000L

        private val SUCCESS_WORDS = listOf("支付成功", "付款成功", "已支付", "交易成功", "支付完成")
    }
}
