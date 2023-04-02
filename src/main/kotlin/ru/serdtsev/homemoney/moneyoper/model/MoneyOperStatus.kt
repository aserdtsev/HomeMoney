package ru.serdtsev.homemoney.moneyoper.model

enum class MoneyOperStatus {
    // todo Порефакторить.
    /** в ожидании */
    pending,
    /** ожидает повтора */
    recurrence,
    /** выполнен */
    done,
    /** выполнен, для новых операций (промежуточный статус, который транслируется либо в done, либо в pending) */
    doneNew,
    /** отменен, конечный статус */
    cancelled,
    /** шаблон */
    template
}