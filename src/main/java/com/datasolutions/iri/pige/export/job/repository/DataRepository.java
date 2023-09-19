package com.datasolutions.iri.pige.export.job.repository;

import com.datasolutions.iri.pige.export.job.bean.*;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Created by romain on 29/11/2019
 */
public interface DataRepository {

    /**
     * Get a list of leaflet ids that are currently running in given period (startDate -> endDate) and
     * from given list of dealerIds (may be null to ignore)
     *
     * @param startDate
     * @param endDate
     * @param supportTypeIds
     * @return
     */
    Set<LeafletId> getLeafletIds(LocalDate startDate, LocalDate endDate, Set<Integer> supportTypeIds);

    /**
     * Finds last updates of given leaflet ids
     *
     * @param leafletIds
     * @return
     */
    List<LeafletLastUpdate> getLeafletLastUpdates(Collection<Long> leafletIds);

    void getAdvantages(Consumer<Advantage> consumer);

    void getLeafletEans(Set<Long> leafletIds, Set<Integer> wineSegmentIds,
                        Set<Integer> bazaarSegmentIds, Consumer<LeafletEan> consumer);

    void getLeaflets(Set<Long> leafletIds, Consumer<Leaflet> consumer);

    void getLeafletStores(Set<Long> leafletIds, Consumer<LeafletStore> consumer);

    void getStores(Consumer<Store> consumer);

    void getClassificationEntries(Consumer<ClassificationEntry> consumer);

    void getProducts(Consumer<Product> consumer);

    List<EanLink> getEanSeg(Collection<String> eans);

    List<EanLink> getEanMulti(Collection<String> eans);

    Set<String> getEanSeg(String ean);

    Set<String> getEanMulti(String ean);

}
