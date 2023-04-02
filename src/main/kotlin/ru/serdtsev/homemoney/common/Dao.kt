package ru.serdtsev.homemoney.common

interface Dao<T : Model> {
    fun save(model: T)
}