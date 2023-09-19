package com.datasolutions.iri.pige.export.job.repository;

import com.datasolutions.iri.pige.export.job.bean.LeafletState;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import org.sql2o.Connection;
import org.sql2o.Query;
import org.sql2o.Sql2o;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Created by romain on 23/12/2019
 */
@Repository
@ConditionalOnProperty(name = "runner.incremental", havingValue = "true")
public class Sql2oStateRepository implements StateRepository {

    private final Sql2o sql2o;

    public Sql2oStateRepository(@Qualifier("stateSql2o") Sql2o sql2o) {
        this.sql2o = sql2o;
    }

    @Override
    public List<LeafletState> getByLeafletId(Collection<Long> leafletIds) {
        try (Connection con = sql2o.open()) {
            return con.createQuery("SELECT LEAFLET_ID AS leafletId, PROCESS_UUID AS processUid, " +
                "PROCESS_LAST_UPDATE AS processLastUpdate FROM LEAFLET_STATE WHERE LEAFLET_ID IN (:leafletIds)")
                .addParameter("leafletIds", leafletIds)
                .executeAndFetch(LeafletStateRow.class).stream()
                .map(row -> new LeafletState(row.leafletId, UUID.fromString(row.processUid), row.processLastUpdate))
                .collect(Collectors.toList());
        }
    }

    @Override
    public void createOrUpdateStates(Collection<LeafletState> states) {
        try (Connection con = sql2o.beginTransaction()) {
            Query query = con.createQuery("INSERT INTO LEAFLET_STATE (LEAFLET_ID, PROCESS_UUID, PROCESS_LAST_UPDATE) " +
                "VALUES (:leafletId, :processUuid, :processLastUpdate) " +
                "ON DUPLICATE KEY UPDATE PROCESS_UUID = :processUuid, PROCESS_LAST_UPDATE = :processLastUpdate");
            for (LeafletState state : states) {
                query.addParameter("leafletId", state.getLeafletId())
                    .addParameter("processUuid", state.getProcessUuid().toString())
                    .addParameter("processLastUpdate", state.getProcessLastUpdate())
                    .addToBatch();
            }
            query.executeBatch();
            con.commit();
        }
    }

    private static class LeafletStateRow {
        private Long leafletId;
        private String processUid;
        private Long processLastUpdate;
    }

}
