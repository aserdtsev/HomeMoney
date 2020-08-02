package ru.serdtsev.homemoney.moneyoper.model

enum class MoneyOperStatus {
    // todo Порефакторить.
    /** в ожидании */
    pending,
    /** ожидает повтора */
    recurrence,
    /** выполнен */
    done,
    /** выполнен, для повторяющихся операций */
    doneNew,
    /** отменен, конечный статус */
    cancelled,
    /** шаблон */
    template
}