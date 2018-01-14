package ru.serdtsev.homemoney.moneyoper.model;

import lombok.*;
import ru.serdtsev.homemoney.balancesheet.BalanceSheet;

import javax.persistence.*;
import java.util.UUID;

import static java.util.Objects.nonNull;

@Entity
@Table(name = "labels")
@Getter
@Setter
@ToString
@EqualsAndHashCode
@NoArgsConstructor
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

  @Column(name = "cat_type")
  @Enumerated(EnumType.STRING)
  private CategoryType categoryType;

  @Column(name = "is_arc")
  private Boolean isArc;

  public Label(UUID id, BalanceSheet balanceSheet, String name) {
    this(id, balanceSheet, name, null, false, null);
  }

  public Label(UUID id, BalanceSheet balanceSheet, String name, UUID rootId, Boolean isCategory, CategoryType categoryType) {
    this.id = id;
    this.balanceSheet = balanceSheet;
    this.name = name;
    this.rootId = rootId;
    this.isCategory = isCategory;
    this.categoryType = categoryType;
    this.isArc = false;
  }

  public Boolean getIsCategory() {
    return nonNull(isCategory) ? isCategory : false;
  }
}
