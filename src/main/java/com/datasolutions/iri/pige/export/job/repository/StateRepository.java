package com.datasolutions.iri.pige.export.job.repository;

import com.datasolutions.iri.pige.export.job.bean.LeafletState;

import java.util.Collection;
import java.util.List;

/**
 * Created by romain on 23/12/2019
 */
public interface StateRepository {

    List<LeafletState> getByLeafletId(Collection<Long> leafletIds);

    void createOrUpdateStates(Collection<LeafletState> states);

}
