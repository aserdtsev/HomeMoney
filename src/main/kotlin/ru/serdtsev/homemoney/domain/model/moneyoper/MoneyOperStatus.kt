package ru.serdtsev.homemoney.domain.model.moneyoper

enum class MoneyOperStatus {
    New,
    /** Выполнен */
    Done,
    /** В ожидании */
    Pending,
    /** Ожидает повтора */
    Recurrence,
    /** Отменен, конечный статус */
    Cancelled,
    /** Шаблон */
    Template,
    /** Тренд */
    Trend
}