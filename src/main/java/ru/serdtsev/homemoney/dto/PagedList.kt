package ru.serdtsev.homemoney.dto

import java.io.Serializable

class PagedList<T>(var data: List<T>, limit: Int, offset: Int, hasNext: Boolean?) {
  var paging: Paging

  init {
    this.paging = Paging(limit, offset, hasNext)
  }

  class Paging(var limit: Int, var offset: Int, var hasNext: Boolean?) : Serializable
}
