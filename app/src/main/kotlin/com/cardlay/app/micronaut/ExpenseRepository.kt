package com.cardlay.app.micronaut

data class Expense(val id: String)

interface ExpenseRepository {
    fun get(id: String): Expense
}
