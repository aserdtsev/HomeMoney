package ru.serdtsev.homemoney.common

interface Model {
    fun merge(other: Any): Collection<Model>
}