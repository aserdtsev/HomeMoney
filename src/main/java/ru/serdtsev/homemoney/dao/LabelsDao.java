package ru.serdtsev.homemoney.dao;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.BeanHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;
import org.apache.commons.dbutils.handlers.ScalarHandler;
import ru.serdtsev.homemoney.dto.Label;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

class LabelsDao {
  static void saveLabels(Connection conn, UUID bsId, UUID objId, String objType, List<String> labels)
      throws SQLException {
    List<Label> oldLabels = getLabels(conn, objId);

    QueryRunner run = new QueryRunner();
    run.update(conn, "delete from labels2objs where obj_id = ?", objId);
    labels.forEach(name -> {
      try {
        Optional<Label> labelOpt = findLabel(conn, bsId, name);
        Label label = labelOpt.isPresent() ? labelOpt.get() : createLabel(conn, bsId, name);
        run.update(conn, "insert into labels2objs(label_id, obj_id, obj_type) values(?, ?, ?)", label.getId(), objId, objType);
      } catch (SQLException e) {
        throw new HmSqlException(e);
      }
    });

    deleteUnusedLabels(conn, oldLabels);
  }

  private static Label createLabel(Connection conn, UUID bsId, String name) throws SQLException {
    Label label = new Label(UUID.randomUUID(), name);
    new QueryRunner().update(conn, "insert into labels(id, bs_id, name) values (?, ?, ?)",
        label.getId(), bsId, label.getName());
    return label;
  }

  private static Optional<Label> findLabel(Connection conn, UUID bsId, String name) throws SQLException {
    Label label = new QueryRunner().query(conn, "" +
        "select id, name from labels where bs_id = ? and name = ?",
        new BeanHandler<>(Label.class), bsId, name);
    return Optional.ofNullable(label);
  }

  private static void deleteUnusedLabels(Connection conn, List<Label> labels) throws SQLException {
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

  private static void deleteLabel(Connection conn, UUID id) throws SQLException {
    new QueryRunner().update(conn, "delete from labels where id = ?", id);
  }

  static List<String> getLabelNames(Connection conn, UUID objId) throws SQLException {
    return getLabels(conn, objId).stream().map(Label::getName).collect(Collectors.toList());
  }

  private static List<Label> getLabels(Connection conn, UUID objId) throws SQLException {
    return new QueryRunner().query(conn, "" +
            "select l.id, l.name from labels2objs l2o, labels l where l2o.obj_id = ? and l.id = l2o.label_id",
        new BeanListHandler<>(Label.class), objId
    );
  }
}
