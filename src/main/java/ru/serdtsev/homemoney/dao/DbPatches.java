package ru.serdtsev.homemoney.dao;

import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.BeanListHandler;
import ru.serdtsev.homemoney.dto.BalanceSheet;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class DbPatches {

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
}
