package ru.serdtsev.homemoney.dto

class PagedList<T>(var data: List<T>, limit: Int, offset: Int, hasNext: Boolean) {
  class Paging(var limit: Int, var offset: Int, var hasNext: Boolean)
  var paging: Paging = Paging(limit, offset, hasNext)
}
