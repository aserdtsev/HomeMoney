package ru.serdtsev.homemoney.dto

class PagedList<T>(val items: List<T>, limit: Int, offset: Int, hasNext: Boolean) {
  val paging = Paging(limit, offset, hasNext)
  class Paging(val limit: Int, val offset: Int, val hasNext: Boolean)
}
