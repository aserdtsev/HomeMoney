package ru.serdtsev.homemoney.domain.model.moneyoper

enum class MoneyOperStatus {
    /** в ожидании */
    pending,
    /** ожидает повтора */
    recurrence,
    /** выполнен */
    done,
    /** выполнен, для новых операций (промежуточный статус, который транслируется либо в done, либо в pending) */
    // todo Удалить, поскольку для новой операции можно однозначно определить, что нужно скорректировать баланс
    doneNew,
    /** отменен, конечный статус */
    cancelled,
    /** шаблон */
    template
}