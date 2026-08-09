package com.localbill.model

data class Ledger(
    val id: Long,
    val name: String,
    val isDefault: Boolean,
    val sort: Int
)

data class Account(
    val id: Long,
    val ledgerId: Long,
    val name: String,
    val type: Int,
    val balance: Long,
    val color: Int,
    val sort: Int
)

data class Category(
    val id: Long,
    val parent: Long,
    val name: String,
    val kind: Int,
    val color: Int,
    val isSystem: Boolean,
    val sort: Int
)

data class Bill(
    val id: Long,
    val ledgerId: Long,
    val accountId: Long,
    val categoryId: Long,
    val kind: Int,
    val amount: Long,
    val remark: String,
    val day: Int,
    val createdAt: Long,
    val isDeleted: Int,
    val deletedAt: Long
)

object Kinds {
    const val EXPENSE = 1
    const val INCOME = 2
}
