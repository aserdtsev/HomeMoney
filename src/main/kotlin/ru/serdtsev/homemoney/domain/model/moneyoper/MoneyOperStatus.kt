package ru.serdtsev.homemoney.domain.model.moneyoper

enum class MoneyOperStatus {
    New,
    /** выполнен */
    Done,
    /** в ожидании */
    Pending,
    /** ожидает повтора */
    Recurrence,
    /** отменен, конечный статус */
    Cancelled,
    /** шаблон */
    Template
}