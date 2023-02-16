package ru.serdtsev.homemoney.account.model

enum class AccountType {
    debit, credit, reserve, asset, service;

    val isBalance: Boolean
        get() = this in listOf(debit, credit, reserve, asset)
}