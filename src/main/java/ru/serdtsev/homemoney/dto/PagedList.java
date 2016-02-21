package ru.serdtsev.homemoney.dto;

import java.io.Serializable;
import java.util.List;

public class PagedList<T> implements java.io.Serializable {
  public List<T> data;
  public Paging paging;

  public PagedList(List<T> data, int limit, int offset, Boolean hasNext) {
    this.data = data;
    this.paging = new Paging(limit, offset, hasNext);
  }

  public static class Paging implements Serializable {
    public int limit;
    public int offset;
    public Boolean hasNext;

    public Paging(int limit, int offset, Boolean hasNext) {
      this.limit = limit;
      this.offset = offset;
      this.hasNext = hasNext;
    }
  }
}
