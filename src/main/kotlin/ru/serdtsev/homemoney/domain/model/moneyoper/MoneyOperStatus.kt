package ru.serdtsev.homemoney.domain.model.moneyoper

enum class MoneyOperStatus {
    new,
    /** в ожидании */
    pending,
    /** ожидает повтора */
    recurrence,
    /** выполнен */
    done,
    /** отменен, конечный статус */
    cancelled,
    /** шаблон */
    template
}