package ru.serdtsev.homemoney.dto;

import java.util.List;

public class PagedList<T> {
  private List<T> items;
  private Integer limit;
  private Integer offset;
  private Boolean hasNext;
  private Paging paging;

  public PagedList(List<T> items, Integer limit, Integer offset, Boolean hasNext) {
    this.items = items;
    this.limit = limit;
    this.offset = offset;
    this.hasNext = hasNext;
    this.paging = new Paging(limit, offset, hasNext);
  }

  public List<T> getItems() {
    return items;
  }

  public void setItems(List<T> items) {
    this.items = items;
  }

  public Integer getLimit() {
    return limit;
  }

  public void setLimit(Integer limit) {
    this.limit = limit;
  }

  public Integer getOffset() {
    return offset;
  }

  public void setOffset(Integer offset) {
    this.offset = offset;
  }

  public Boolean getHasNext() {
    return hasNext;
  }

  public void setHasNext(Boolean hasNext) {
    this.hasNext = hasNext;
  }

  public Paging getPaging() {
    return paging;
  }

  public void setPaging(Paging paging) {
    this.paging = paging;
  }

  public class Paging {
    Integer limit;
    Integer offset;
    Boolean hasNext;

    public Paging(Integer limit, Integer offset, Boolean hasNext) {
      this.limit = limit;
      this.offset = offset;
      this.hasNext = hasNext;
    }

    public Integer getLimit() {
      return limit;
    }

    public void setLimit(Integer limit) {
      this.limit = limit;
    }

    public Integer getOffset() {
      return offset;
    }

    public void setOffset(Integer offset) {
      this.offset = offset;
    }

    public Boolean getHasNext() {
      return hasNext;
    }

    public void setHasNext(Boolean hasNext) {
      this.hasNext = hasNext;
    }
  }
}
