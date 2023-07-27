package ru.serdtsev.homemoney.domain.model.account

enum class AccountType {
    debit, credit, reserve, asset, service;

    val isBalance: Boolean
        get() = this in listOf(debit, credit, reserve, asset)
}