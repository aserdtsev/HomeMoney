package ru.serdtsev.homemoney.dao;

import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.BeanListHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.serdtsev.homemoney.dto.*;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

import static ru.serdtsev.homemoney.utils.Utils.assertNonNulls;

public class DbPatches {
  private static Logger log = LoggerFactory.getLogger(DbPatches.class);

  private static void doPatch1() {
    try(Connection conn = MainDao.getConnection()) {
      QueryRunner run = new QueryRunner();

      run.update(conn, "delete from labels2objs");
      run.update(conn, "delete from labels");

      List<BalanceSheet> bsList = MainDao.getBalanceSheets(conn);

      bsList.forEach(bs -> {
        try {
          List<Labels2Obj> labels2ObjList = run.query(conn, "" +
                  "select id as objId, 'operation' as objType, labels from money_trns " +
                  "  where balance_sheet_id = ? and labels is not null and labels != '' " +
                  "union all " +
                  "select te.id as objId, 'template' as objType, te.labels " +
                  "  from money_trn_templs te, money_trns tr " +
                  "  where tr.balance_sheet_id = ? and te.labels is not null and te.labels != '' and tr.id = te.sample_id",
              new BeanListHandler<>(Labels2Obj.class), bs.getId(), bs.getId()
          );

          Set<String> names = labels2ObjList
              .stream()
              .flatMap(l2o -> l2o.getLabelsAsList().stream())
                  .distinct()
                  .collect(Collectors.toSet());

          names.forEach(name -> {
            UUID labelId = UUID.randomUUID();
            try {
              run.update(conn, "insert into labels(id, bs_id, name) values(?, ?, ?)", labelId, bs.getId(), name);
              labels2ObjList.stream()
                  .filter(l2o -> l2o.getLabelsAsList().contains(name))
                  .forEach(l2o -> {
                    try {
                      run.update(conn, "insert into labels2objs(label_id, obj_id, obj_type) values (?, ?, ?)",
                          labelId, l2o.objId, l2o.objType);
                    } catch (SQLException e) {
                      throw new HmSqlException(e);
                    }
                  });
            } catch (SQLException e) {
              throw new HmSqlException(e);
            }
          });
        } catch (SQLException e) {
          throw new HmSqlException(e);
        }
      });

      DbUtils.commitAndClose(conn);
    } catch (SQLException e) {
      throw new HmSqlException(e);
    }

  }

  public static class Labels2Obj {
    UUID objId;
    String objType;
    String labels;

    @SuppressWarnings("unused")
    public void setObjId(UUID objId) {
      this.objId = objId;
    }

    @SuppressWarnings("unused")
    public void setObjType(String objType) {
      this.objType = objType;
    }

    public void setLabels(String labels) {
      this.labels = labels;
    }

    List<String> getLabelsAsList() {
      return Arrays.asList(labels.split(","));
    }
  }

  public static void doPatch2() {
    try (Connection conn = MainDao.getConnection()) {
      List<BalanceSheet> bsList = MainDao.getBalanceSheets(conn);
      bsList.forEach(bs -> handleBs(conn, bs));
//      DbUtils.commitAndClose(conn);
      DbUtils.rollbackAndClose(conn);
    } catch (SQLException e) {
      throw new HmSqlException(e);
    }

  }

  private static void handleBs(Connection conn, BalanceSheet bs) {
    try {
      splitMoneyTrns(conn, bs);
      List<Category> categories = CategoriesDao.getCategories(bs.getId());
      categories.stream()
          .filter(category -> category.getRootId() == null)
          .forEach(category -> saveCategoryAsLabel(conn, bs, category));
      categories.stream()
          .filter(category -> category.getRootId() != null)
          .forEach(category -> saveCategoryAsLabel(conn, bs, category));

      removeCategoryOfWithoutCategory(conn, bs);
    } catch (SQLException e) {
      throw new HmSqlException(e);
    }
  }

  private static void removeCategoryOfWithoutCategory(Connection conn, BalanceSheet bs) throws SQLException {
    Optional<Label> label = LabelsDao.findLabel(conn, bs.getId(), "<Без категории>");
    if (label.isPresent()) {
      new QueryRunner().update(conn, "delete from labels2objs where label_id = ?", label.get().getId());
      new QueryRunner().update(conn, "delete from labels where id = ?", label.get().getId());
    }
  }

  private static void saveCategoryAsLabel(Connection conn, BalanceSheet bs, Category category) {
    if (category.getName().equals("<Без категории>")) return;
    Optional<Label> labelOpt;
    try {
      labelOpt = LabelsDao.findLabel(conn, bs.getId(), category.getName());
      UUID rootId = (category.getRootId() != null)
          ? LabelsDao.findLabel(conn, bs.getId(),CategoriesDao.getCategory(conn, category.getRootId()).getName()).get().getId()
          : null;
      Label label = labelOpt.isPresent()
          ? labelOpt.get()
          : LabelsDao.createLabel(conn, bs.getId(), category.getName(), rootId, true, category.getIsArc());
      List<MoneyTrn> trns = MoneyTrnsDao.getMoneyTrns(conn, bs.getId(), category);
      trns.forEach(trn -> {
        try {
          addLabelToMoneyTrn(conn, bs, label, trn);
        } catch (SQLException e) {
          throw new HmSqlException(e);
        }
      });
    } catch (SQLException e) {
      throw new HmSqlException(e);
    }
  }

  private static void splitMoneyTrns(Connection conn, BalanceSheet bs) throws SQLException {
    log.trace(">> splitMoneyTrns");
    List<MoneyTrn> trns = MoneyTrnsDao.getMoneyTrns(conn, bs.getId());
    trns.forEach(trn -> {
      try {
        updateTrnAmountsIfNeed(conn, bs, trn);
        splitMoneyTrn(conn, trn);
      } catch (SQLException e) {
        throw new HmSqlException(e);
      }
    });
    log.trace("<< splitMoneyTrns");
  }

  private static void addLabelToMoneyTrn(Connection conn, BalanceSheet bs, Label label, MoneyTrn trn) throws SQLException {
    if (trn.getLabels().contains(label.getName())) {
      return;
    }
    trn.getLabels().add(label.getName());
    MoneyTrnsDao.updateMoneyTrn(conn, bs.getId(), trn);
  }

  private static MoneyTrn updateTrnAmountsIfNeed(Connection conn, BalanceSheet bs, MoneyTrn trn) throws SQLException {
    QueryRunner run = new QueryRunner();
    if (trn.isMonoCurrencies() && !Objects.equals(trn.getAmount(), trn.getToAmount())) {
      log.info("Updating MoneyTrn.toAmount ({}): expected {}, found {}, {}", trn.getType(), trn.getAmount(), trn.getToAmount(), trn);
      run.update(conn, "" +
          "update money_trns set to_amount = ? where id = ?", trn.getAmount(), trn.getId()
      );
      return MoneyTrnsDao.getMoneyTrn(conn, bs.getId(), trn.getId());
    }
    return trn;
  }

  private static void splitMoneyTrn(Connection conn, MoneyTrn trn) throws SQLException {
    assertNonNulls(conn, trn);
    List<BalanceChange> balanceChanges = MoneyTrnsDao.getBalanceChanges(conn, trn.getId());
    if (!balanceChanges.isEmpty()) {
      if (trn.getType().equals("expense")) {
        Optional<BalanceChange> change = balanceChanges.stream()
            .filter(c -> c.getValue().compareTo(BigDecimal.ZERO) > 0)
            .findFirst();
        change.ifPresent(c -> MoneyTrnsDao.deleteBalanceChange(conn, c.getId()));
      }
      if (trn.getType().equals("income")) {
        Optional<BalanceChange> change = balanceChanges.stream()
            .filter(c -> c.getValue().compareTo(BigDecimal.ZERO) < 0)
            .findFirst();
        change.ifPresent(c -> MoneyTrnsDao.deleteBalanceChange(conn, c.getId()));
      }
      return;
    }

    if (trn.getType().equals("expense") || trn.getType().equals("transfer")) {
      MoneyTrnsDao.createBalanceChange(conn, trn.getId(), trn.getFromAccId(), trn.getAmount().negate(), trn.getTrnDate(), 0);
    }

    if (trn.getType().equals("income") || trn.getType().equals("transfer")) {
      BigDecimal toAmount = trn.getToAmount() != null ? trn.getToAmount() : trn.getAmount();
      MoneyTrnsDao.createBalanceChange(conn, trn.getId(), trn.getToAccId(), toAmount, trn.getTrnDate(), 1);
    }
  }
}
