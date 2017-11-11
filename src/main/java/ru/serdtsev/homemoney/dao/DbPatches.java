package ru.serdtsev.homemoney.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DbPatches {
  private static Logger log = LoggerFactory.getLogger(DbPatches.class);

//  public static void doPatch2() {
//    try (Connection conn = MainDao.getConnection()) {
//      List<BalanceSheet> bsList = BalanceSheet.getAllInstances();
//      bsList.forEach(bs -> handleBs(conn, bs));
//      DbUtils.commitAndClose(conn);
////      DbUtils.rollbackAndClose(conn);
//    } catch (SQLException e) {
//      throw new HmSqlException(e);
//    }
//
//  }
//
//  private static void handleBs(Connection conn, BalanceSheet bs) {
//    log.info(">> handleBs {}", bs.getId());
//    try {
//      splitMoneyTrns(conn, bs);
//      splitMoneyTrnTempls(conn, bs);
//      List<Category> categories = CategoriesDao.getCategories(bs.getId());
//      categories.stream()
//          .filter(category -> category.getRootId() == null)
//          .forEach(category -> saveCategoryAsLabel(conn, bs, category));
//      categories.stream()
//          .filter(category -> category.getRootId() != null)
//          .forEach(category -> saveCategoryAsLabel(conn, bs, category));
//
//      removeCategoryOfWithoutCategory(conn, bs);
//    } catch (SQLException e) {
//      throw new HmSqlException(e);
//    }
//    log.info("<< handleBs {}", bs.getId());
//  }
//
//  private static void removeCategoryOfWithoutCategory(Connection conn, BalanceSheet bs) throws SQLException {
//    Optional<Label> label = LabelsDao.findLabel(conn, bs.getId(), "<Без категории>");
//    if (label.isPresent()) {
//      new QueryRunner().update(conn, "delete from labels2objs where label_id = ?", label.get().getId());
//      new QueryRunner().update(conn, "delete from labels where id = ?", label.get().getId());
//    }
//  }
//
//  private static void saveCategoryAsLabel(Connection conn, BalanceSheet bs, Category category) {
//    if (category.getName().equals("<Без категории>")) return;
//    Optional<Label> labelOpt;
//    try {
//      labelOpt = LabelsDao.findLabel(conn, bs.getId(), category.getName());
//      UUID rootId = (category.getRootId() != null)
//          ? LabelsDao.findLabel(conn, bs.getId(),CategoriesDao.getCategory(conn, category.getRootId()).getName()).get().getId()
//          : null;
//      Label label = labelOpt.isPresent()
//          ? labelOpt.get()
//          : LabelsDao.createLabel(conn, bs.getId(), category.getName(), rootId, true, category.getIsArc());
//
//      List<MoneyOperDto> trns = MoneyTrnsDao.getMoneyTrns(conn, bs.getId(), category);
//      trns.forEach(trn -> {
//        try {
//          addLabelToMoneyTrn(conn, bs, label, trn);
//        } catch (SQLException e) {
//          throw new HmSqlException(e);
//        }
//      });
//
//      List<RecurrenceOperDto> templs = MoneyTrnTemplsDao.getMoneyTrnTempls(conn, bs.getId(), null);
//      templs.forEach(templ -> {
//        try {
//          addLabelToMoneyTrnTempl(conn, bs, label, templ);
//        } catch (SQLException e) {
//          throw new HmSqlException(e);
//        }
//      });
//    } catch (SQLException e) {
//      throw new HmSqlException(e);
//    }
//  }
//
//  private static void splitMoneyTrns(Connection conn, BalanceSheet bs) throws SQLException {
//    log.trace(">> splitMoneyTrns");
//    List<MoneyOperDto> trns = MoneyTrnsDao.getMoneyTrns(conn, bs.getId());
//    trns.forEach(trn -> {
//      try {
//        updateTrnAmountsIfNeed(conn, bs, trn);
//        splitMoneyTrn(conn, trn);
//      } catch (SQLException e) {
//        throw new HmSqlException(e);
//      }
//    });
//    log.trace("<< splitMoneyTrns");
//  }
//
//  private static void splitMoneyTrnTempls(Connection conn, BalanceSheet bs) throws SQLException {
//    log.trace(">> splitMoneyTrnTempls");
//    List<RecurrenceOperDto> templs = MoneyTrnTemplsDao.getMoneyTrnTempls(conn, bs.getId(), null);
//    templs.forEach(templ -> {
//      try {
//        splitMoneyTrnTempl(conn, templ);
//      } catch (SQLException e) {
//        throw new HmSqlException(e);
//      }
//    });
//    log.trace("<< splitMoneyTrnTempls");
//  }
//
//
//  private static void addLabelToMoneyTrn(Connection conn, BalanceSheet bs, Label label, MoneyOperDto trn) throws SQLException {
//    if (trn.getLabels().contains(label.getName())) {
//      return;
//    }
//    trn.getLabels().add(label.getName());
//    MoneyTrnsDao.updateMoneyTrn(conn, bs.getId(), trn);
//  }
//
//  private static void addLabelToMoneyTrnTempl(Connection conn, BalanceSheet bs, Label label, RecurrenceOperDto templ) throws SQLException {
//    if (templ.getLabels().contains(label.getName())) {
//      return;
//    }
//    templ.getLabels().add(label.getName());
//    MoneyTrnTemplsDao.updateMoneyTrnTempl(conn, bs.getId(), templ);
//  }
//
//  private static MoneyOperDto updateTrnAmountsIfNeed(Connection conn, BalanceSheet bs, MoneyOperDto trn) throws SQLException {
//    QueryRunner run = new QueryRunner();
//    if (trn.isMonoCurrencies() && !Objects.equals(trn.getAmount(), trn.getToAmount())) {
//      log.info("Updating MoneyOperDto.toAmount ({}): expected {}, found {}, {}", trn.getType(), trn.getAmount(), trn.getToAmount(), trn);
//      run.update(conn, "" +
//          "update money_trns set to_amount = ? where id = ?", trn.getAmount(), trn.getId()
//      );
//      return MoneyTrnsDao.getMoneyTrn(conn, bs.getId(), trn.getId());
//    }
//    return trn;
//  }
//
//  private static void splitMoneyTrn(Connection conn, MoneyOperDto trn) throws SQLException {
//    assertNonNulls(conn, trn);
//    List<BalanceChange> balanceChanges = MoneyTrnsDao.getBalanceChanges(conn, trn.getId());
//    if (!balanceChanges.isEmpty()) {
//      if (trn.getType().equals("expense")) {
//        Optional<BalanceChange> change = balanceChanges.stream()
//            .filter(c -> c.getValue().compareTo(BigDecimal.ZERO) > 0)
//            .findFirst();
//        change.ifPresent(c -> MoneyTrnsDao.deleteBalanceChange(conn, c.getId()));
//      }
//      if (trn.getType().equals("income")) {
//        Optional<BalanceChange> change = balanceChanges.stream()
//            .filter(c -> c.getValue().compareTo(BigDecimal.ZERO) < 0)
//            .findFirst();
//        change.ifPresent(c -> MoneyTrnsDao.deleteBalanceChange(conn, c.getId()));
//      }
//      return;
//    }
//
//    if (trn.getType().equals("expense") || trn.getType().equals("transfer")) {
//      MoneyTrnsDao.createBalanceChange(conn, trn.getId(), trn.getFromAccId(), trn.getAmount().negate(), trn.getTrnDate(), 0);
//    }
//
//    if (trn.getType().equals("income") || trn.getType().equals("transfer")) {
//      BigDecimal toAmount = trn.getToAmount() != null ? trn.getToAmount() : trn.getAmount();
//      MoneyTrnsDao.createBalanceChange(conn, trn.getId(), trn.getToAccId(), toAmount, trn.getTrnDate(), 1);
//    }
//  }
//
//  private static void splitMoneyTrnTempl(Connection conn, RecurrenceOperDto templ) throws SQLException {
//    assertNonNulls(conn, templ);
//    List<BalanceChange> balanceChanges = MoneyTrnsDao.getBalanceChanges(conn, templ.getId());
//    if (!balanceChanges.isEmpty()) {
//      if (templ.getType().equals("expense")) {
//        Optional<BalanceChange> change = balanceChanges.stream()
//            .filter(c -> c.getValue().compareTo(BigDecimal.ZERO) > 0)
//            .findFirst();
//        change.ifPresent(c -> MoneyTrnsDao.deleteBalanceChange(conn, c.getId()));
//      }
//      if (templ.getType().equals("income")) {
//        Optional<BalanceChange> change = balanceChanges.stream()
//            .filter(c -> c.getValue().compareTo(BigDecimal.ZERO) < 0)
//            .findFirst();
//        change.ifPresent(c -> MoneyTrnsDao.deleteBalanceChange(conn, c.getId()));
//      }
//      return;
//    }
//
//    if (templ.getType().equals("expense") || templ.getType().equals("transfer")) {
//      MoneyTrnsDao.createBalanceChange(conn, templ.getId(), templ.getFromAccId(), templ.getAmount().negate(), null, 0);
//    }
//
//    if (templ.getType().equals("income") || templ.getType().equals("transfer")) {
//      BigDecimal toAmount = templ.getToAmount() != null ? templ.getToAmount() : templ.getAmount();
//      MoneyTrnsDao.createBalanceChange(conn, templ.getId(), templ.getToAccId(), toAmount, null, 1);
//    }
//  }

}
