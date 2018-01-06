package ru.serdtsev.homemoney.moneyoper.model;

import ru.serdtsev.homemoney.balancesheet.BalanceSheet;

import javax.persistence.*;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "labels")
public class Label {
  @Id
  private UUID id;

  @ManyToOne
  @JoinColumn(name = "bs_id")
  private BalanceSheet balanceSheet;

  private String name;

  @Column(name = "root_id")
  private UUID rootId;

  @Column(name = "is_category")
  private Boolean isCategory;

  @Column(name = "is_arc")
  private Boolean isArc;

  @SuppressWarnings("unused")
  public Label() {
  }

  public Label(UUID id, BalanceSheet balanceSheet, String name) {
    this(id, balanceSheet, name, null, false);
  }

  public Label(UUID id, BalanceSheet balanceSheet, String name, UUID rootId, Boolean isCategory) {
    this.id = id;
    this.balanceSheet = balanceSheet;
    this.name = name;
    this.rootId = rootId;
    this.isCategory = isCategory;
    this.isArc = false;
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public BalanceSheet getBalanceSheet() {
    return balanceSheet;
  }

  public void setBalanceSheet(BalanceSheet balanceSheet) {
    this.balanceSheet = balanceSheet;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public UUID getRootId() {
    return rootId;
  }

  public void setRootId(UUID rootId) {
    this.rootId = rootId;
  }

  public Boolean getIsCategory() {
    return isCategory;
  }

  public void setIsCategory(Boolean category) {
    isCategory = category;
  }

  public Boolean getArc() {
    return isArc;
  }

  public void setArc(Boolean arc) {
    isArc = arc;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Label)) return false;
    Label label = (Label) o;
    return Objects.equals(balanceSheet, label.balanceSheet) &&
        Objects.equals(name, label.name) &&
        Objects.equals(rootId, label.rootId) &&
        Objects.equals(isCategory, label.isCategory) &&
        Objects.equals(isArc, label.isArc);
  }

  @Override
  public int hashCode() {

    return Objects.hash(balanceSheet, name, rootId, isCategory, isArc);
  }

  @Override
  public String toString() {
    return "Label{" +
        "id=" + id +
        ", balanceSheet=" + balanceSheet +
        ", name='" + name + '\'' +
        ", rootId=" + rootId +
        ", isCategory=" + isCategory +
        ", isArc=" + isArc +
        '}';
  }
}
