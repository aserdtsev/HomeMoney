package ru.serdtsev.homemoney.account.model

enum class AccountType {
    debit, credit, expense, income, reserve, asset, service;

    val isBalance: Boolean
        get() = this in listOf(debit, credit, reserve)
}