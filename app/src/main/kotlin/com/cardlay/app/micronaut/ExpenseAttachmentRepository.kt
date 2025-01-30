package com.cardlay.app.micronaut

data class ExpenseAttachment(val id: String, val expenseId: String, val token: String)

interface ExpenseAttachmentRepository {
    fun list(expenseId: String): List<ExpenseAttachment>
}
