package ru.serdtsev.homemoney.common.dto

class PagedList<T>(val items: List<T>, limit: Int, offset: Int, hasNext: Boolean) {
    @Suppress("unused")
    val paging = Paging(limit, offset, hasNext)
    inner class Paging internal constructor(val limit: Int, val offset: Int, val hasNext: Boolean)
}