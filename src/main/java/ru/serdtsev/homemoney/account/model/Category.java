package ru.serdtsev.homemoney.account.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import ru.serdtsev.homemoney.account.CategoryRepository;
import ru.serdtsev.homemoney.balancesheet.BalanceSheet;

import javax.annotation.Nonnull;
import javax.persistence.*;
import java.sql.Date;
import java.util.UUID;

@Entity
@Table(name = "categories")
@DiscriminatorValue("category")
public class Category extends Account implements Comparable {
  @ManyToOne
  @JoinColumn(name = "root_id")
  private Category root;

  @Transient
  private UUID rootId;

  protected Category() {
  }

  public Category(BalanceSheet balanceSheet, AccountType type, String name, Date created, Boolean isArc, Category root) {
    super(balanceSheet, type, name, created, isArc);
    this.root = root;
  }

  public void init(CategoryRepository categoryRepo) {
    super.init();
    if (rootId != null) {
      root = categoryRepo.findOne(rootId);
    } else {
      root = null;
    }
  }

  @JsonIgnore
  public Category getRoot() {
    return root;
  }

  public UUID getRootId() {
    return rootId;
  }

  @Override
  public String getSortIndex() {
    return (root != null)
        ? root.getSortIndex() + "#" + getName()
        : Integer.toString(getType().ordinal()) + "#" + getName();
  }

  @Override
  public int compareTo(@Nonnull Object o) {
    if (!(o instanceof Category)) {
      return 0;
    }
    Category other = (Category) o;
    int typeComparing = getType().compareTo(other.getType());
    if (typeComparing != 0) {
      return typeComparing;
    }
    return getSortIndex().compareTo(other.getSortIndex());
  }
}
