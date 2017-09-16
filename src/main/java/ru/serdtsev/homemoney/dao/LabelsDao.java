package ru.serdtsev.homemoney.dao;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.BeanHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;
import org.apache.commons.dbutils.handlers.ScalarHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.serdtsev.homemoney.balancesheet.BalanceSheet;
import ru.serdtsev.homemoney.balancesheet.BalanceSheetRepository;
import ru.serdtsev.homemoney.moneyoper.Label;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class LabelsDao {
  @Autowired
  private BalanceSheetRepository balanceSheetRepo;

  public void saveLabels(Connection conn, UUID bsId, UUID objId, String objType, List<String> labels)
      throws SQLException {
    List<Label> oldLabels = getLabels(conn, objId);

    QueryRunner run = new QueryRunner();
    run.update(conn, "delete from labels2objs where obj_id = ?", objId);
    labels.forEach(name -> {
      try {
        Optional<Label> labelOpt = findLabel(conn, bsId, name);
        Label label = labelOpt.isPresent()
            ? labelOpt.get()
            : createLabel(conn, bsId, name, null, false);
        run.update(conn, "insert into labels2objs(label_id, obj_id, obj_type) values(?, ?, ?)", label.getId(), objId, objType);
      } catch (SQLException e) {
        throw new HmSqlException(e);
      }
    });

    deleteUnusedLabels(conn, oldLabels);
  }

  public Label createLabel(Connection conn, UUID bsId, String name, UUID rootId, Boolean isCategory) throws SQLException {
    BalanceSheet balanceSheet = balanceSheetRepo.findOne(bsId);
    Label label = new Label(UUID.randomUUID(), balanceSheet, name, rootId, isCategory);
    new QueryRunner().update(conn,
        "insert into labels(id, bs_id, name, root_id, is_category, is_arc) values (?, ?, ?, ?, ?, false)",
        label.getId(), bsId, label.getName(), rootId, label.getIsCategory());
    return label;
  }

  public Optional<Label> findLabel(Connection conn, UUID bsId, String name) throws SQLException {
    Label label = new QueryRunner().query(conn, "" +
        "select id, name, root_id as rootId, is_category as isCategory, is_arc as isArc from labels where bs_id = ? and name = ?",
        new BeanHandler<>(Label.class), bsId, name);
    return Optional.ofNullable(label);
  }

  public void deleteUnusedLabels(Connection conn, List<Label> labels) throws SQLException {
    labels.forEach(label -> {
      try {
        Boolean exists = new QueryRunner().query(conn,
            "select true from dual where exists (select null from labels2objs where label_id = ?)",
            new ScalarHandler<>(), label.getId());
        if (exists == null || !exists) {
          deleteLabel(conn, label.getId());
        }
      } catch (SQLException e) {
        throw new HmSqlException(e);
      }
    });
  }

  public void deleteLabel(Connection conn, UUID id) throws SQLException {
    new QueryRunner().update(conn, "delete from labels where id = ?", id);
  }

  public List<String> getLabelNames(Connection conn, UUID objId) throws SQLException {
    return getLabels(conn, objId).stream().map(Label::getName).collect(Collectors.toList());
  }

  public List<Label> getLabels(Connection conn, UUID objId) throws SQLException {
    return new QueryRunner().query(conn, "" +
            "select l.id, l.name, root_id as rootId, is_category as isCategory, is_arc as isArc " +
            "  from labels2objs l2o, labels l " +
            "  where l2o.obj_id = ? and l.id = l2o.label_id",
        new BeanListHandler<>(Label.class), objId
    );
  }
}
