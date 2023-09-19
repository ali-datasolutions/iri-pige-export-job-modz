package com.datasolutions.iri.pige.export.job.repository;

import com.datasolutions.iri.pige.export.job.bean.*;
import com.google.common.collect.Maps;
import lombok.AllArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Repository;
import org.springframework.util.StreamUtils;
import org.sql2o.Connection;
import org.sql2o.Query;
import org.sql2o.ResultSetIterable;
import org.sql2o.Sql2o;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Created by romain on 29/11/2019
 */
@Repository
@AllArgsConstructor
public class Sql2oDataRepository implements DataRepository {

    private final Sql2o sql2o;
    private final ResourceLoader resourceLoader;

    @Override
    public Set<LeafletId> getLeafletIds(LocalDate startDate, LocalDate endDate, Set<Integer> supportTypeIds) {
        try (Connection con = sql2o.open()) {
            String rawQuery = getQueryFromResource("LEAFLET_IDS.sql");
            return con.createQuery(rawQuery)
                .addParameter("startDate", startDate.toString())
                .addParameter("endDate", endDate.toString())
                .addParameter("supportTypeIds", supportTypeIds != null ? supportTypeIds : Collections.singleton(-1))
                .executeAndFetch(LeafletIdRow.class).stream()
                .map(row -> new LeafletId(row.id, row.status == 4 ? LeafletStatus.FINISH : LeafletStatus.IN_PROGRESS))
                .collect(Collectors.toSet());
        }
    }

    @Override
    public List<LeafletLastUpdate> getLeafletLastUpdates(Collection<Long> leafletIds) {
        try (Connection con = sql2o.open()) {
            String rawQuery = getQueryFromResource("LEAFLET_LAST_UPDATES.sql");
            return con.createQuery(rawQuery)
                .addParameter("leafletIds", leafletIds)
                .executeAndFetch(LeafletLastUpdateRow.class).stream()
                .map(row -> {
                    long lastUpdate = Stream.of(row.leafletLastUpdate, row.leafletStoreLastUpdate, row.closestLeafletStoreLastUpdate, row.ubLastUpdate)
                        .filter(Objects::nonNull)
                        .mapToLong(Timestamp::getTime)
                        .max().orElse(0L);
                    return new LeafletLastUpdate(row.leafletId, lastUpdate);
                })
                .collect(Collectors.toList());
        }
    }

    @Override
    public void getAdvantages(Consumer<Advantage> consumer) {
        try (Connection con = sql2o.open()) {
            String rawQuery = getQueryFromResource("ADVANTAGES_REQUEST.sql");
            Query query = con.createQuery(rawQuery);
            executeAndFetch(query, AdvantageRow.class, row -> new Advantage(row.code, row.description), consumer);
        }
    }

    @Override
    public void getLeafletEans(Set<Long> leafletIds, Set<Integer> wineSegmentIds,
                               Set<Integer> bazaarSegmentIds, Consumer<LeafletEan> consumer) {
        try (Connection con = sql2o.open()) {
            String rawQuery = getQueryFromResource("LEAFLET_EANS_REQUEST.sql");
            Query query = con.createQuery(rawQuery)
                .addParameter("leafletIds", leafletIds)
                .addParameter("wineSegmentIds", wineSegmentIds)
                .addParameter("bazaarSegmentIds", bazaarSegmentIds);
            executeAndFetch(query, LeafletEanRow.class, row -> {
                LeafletEan.MatchStatus matchStatus = LeafletEan.MatchStatus.valueOf(row.statusMatchEan);
                LeafletEan.MatchType matchType = LeafletEan.MatchType.valueOf(row.matchingType);
                return new LeafletEan(row.leafletCode, row.occProdCodes, row.ean, row.promoCode, row.prodCode, row.scope, matchStatus, matchType,
                    row.implicit, row.occProdDescription, row.promoDescrDs, row.fgCoverPage, row.nbUnitInPack, row.nbUnitMinForPromo,
                    row.fgLoyaltyCard == 1, row.pricePaidInclReduc, row.priceBeforeReduc, row.priceInstantPromo, row.priceAllTypesPromo,
                    row.rateDiscountNip, row.rateGratuityOnPack, row.ratePriceCrossedOut, row.startingDatePromo,
                    row.endingDatePromo, row.urlPictureUb, row.urlAppliUb, row.dsCreateDate, row.dsUpdateDate, row.supportPrice,
                    row.crossedOutPrice, row.netPrice, row.ubCode);
            }, consumer);
        }
    }

    @Override
    public void getLeaflets(Set<Long> leafletIds, Consumer<Leaflet> consumer) {
        try (Connection con = sql2o.open()) {
            String rawQuery = getQueryFromResource("LEAFLETS_REQUEST.sql");
            Query query = con.createQuery(rawQuery)
                .addParameter("leafletIds", leafletIds);
            executeAndFetch(query, LeafletRow.class, row -> new Leaflet(row.leafletCode, row.opCode, row.description, row.startingDate,
                row.endingDate, row.bannerCode, row.bannerDescr, row.nbStoreBanner, row.theme, row.nbPageLeaflet,
                row.nbStoreOp, row.nbStoreLeaflet, row.nbOccProd, row.typeOp, row.typeSupport,
                row.urlFirstPage, row.urlLastPage, row.dsCreateDate, row.dsUpdateDate,
                row.status == 4 ? LeafletStatus.FINISH : LeafletStatus.IN_PROGRESS), consumer);
        }
    }

    @Override
    public void getLeafletStores(Set<Long> leafletIds, Consumer<LeafletStore> consumer) {
        try (Connection con = sql2o.open()) {
            String rawQuery = getQueryFromResource("LEAFLET_STORES_REQUEST.sql");
            Query query = con.createQuery(rawQuery)
                .addParameter("leafletIds", leafletIds);
            executeAndFetch(query, LeafletStoreRow.class, row -> new LeafletStore(row.leafletCode, row.storeCodeDs,
                row.dsCreateDate, row.dsUpdateDate), consumer);
        }
    }

    @Override
    public void getStores(Consumer<Store> consumer) {
        try (Connection con = sql2o.open()) {
            String rawQuery = getQueryFromResource("STORES_REQUEST.sql");
            Query query = con.createQuery(rawQuery);
            executeAndFetch(query, StoreRow.class, row -> new Store(row.storeCodeDs, row.storeCodeIri, row.openDate, row.closureDate,
                row.lsaCode, row.bannerCode, row.bannerName, row.addr1, row.addr2, row.zipcode, row.town, row.inseeCode,
                row.longitude, row.latitude, row.dsCreateDate, row.dsUpdateDate), consumer);
        }
    }

    @Override
    public void getClassificationEntries(Consumer<ClassificationEntry> consumer) {
        try (Connection con = sql2o.open()) {
            String rawQuery = getQueryFromResource("CLASSIFICATION_REQUEST.sql");
            Query query = con.createQuery(rawQuery);
            executeAndFetch(query, ClassificationEntryRow.class, row -> new ClassificationEntry(row.sinOid,
                row.classification, row.shelf, row.category, row.segment, row.subSegment, row.nonVaryingSubSegment), consumer);
        }
    }

    @Override
    public void getProducts(Consumer<Product> consumer) {
        try (Connection con = sql2o.open()) {
            String rawQuery = getQueryFromResource("PRODUCTS_REQUEST.sql");
            Query query = con.createQuery(rawQuery);
            executeAndFetch(query, ProductRow.class, row -> {
                try {
                    return new Product(
                            row.id,
                            row.label,
                            row.quantity,
                            row.gratuity,
                            row.sinOid,
                            row.brandName);
                } catch (Throwable e) {
                    throw new NullPointerException("Product with properties: id:" + row.id + ", label:"+ row.label + " ,quantity:" + row.quantity + " ,gratuity:" + row.gratuity + " ,sinOid" + row.sinOid + " ,brandName)" + row.brandName);
                }
            }, consumer);
        }
    }

    @Override
    public List<EanLink> getEanSeg(Collection<String> eans) {
        try (Connection con = sql2o.open()) {
            String rawQuery = "SELECT esi_ean AS ean, esi_seg_ean AS segEan FROM esi_ean_seg_iri WHERE esi_ean IN (:eans)";
            return con.createQuery(rawQuery)
                    .addParameter("eans", eans)
                    .executeAndFetch(SegEan.class)
                    .stream()
                    .map(segEan -> new EanLink(segEan.ean, segEan.segEan))
                    .collect(Collectors.toList());
        }
    }

    @Override
    public List<EanLink> getEanMulti(Collection<String> eans) {
        try (Connection con = sql2o.open()) {
            String rawQuery = "SELECT emi_ean AS EAN, emi_multi_ean AS multiEan FROM emi_ean_multi_iri WHERE emi_ean IN (:eans)";
            Map<String, List<String>> multiEans = Maps.newHashMap();
            return con.createQuery(rawQuery)
                    .addParameter("eans", eans)
                    .executeAndFetch(MultiEan.class)
                    .stream()
                    .map(multiEan -> new EanLink(multiEan.ean, multiEan.multiEan))
                    .collect(Collectors.toList());
        }
    }

    @Override
    public Set<String> getEanSeg(String ean) {
        try (Connection con = sql2o.open()) {
            String rawQuery = getQueryFromResource("EAN_SEG_REQUEST.sql");
            return new HashSet<>(con.createQuery(rawQuery)
                .addParameter("ean", ean)
                .executeAndFetch(String.class));
        }
    }

    @Override
    public Set<String> getEanMulti(String ean) {
        try (Connection con = sql2o.open()) {
            String rawQuery = getQueryFromResource("EAN_MULTI_REQUEST.sql");
            return new HashSet<>(con.createQuery(rawQuery)
                .addParameter("ean", ean)
                .executeAndFetch(String.class));
        }
    }

    private <R, T> void executeAndFetch(Query query, Class<R> rowClass, Function<R, T> mapper, Consumer<T> consumer) {
        try (ResultSetIterable<R> rows = query.executeAndFetchLazy(rowClass)) {
            for (R row : rows) {
                T item = mapper.apply(row);
                consumer.accept(item);
            }
        }
    }

    private String getQueryFromResource(String file) {
        String basePath = "classpath:com/datasolutions/iri/pige/export/job/repository/";
        Resource resource = resourceLoader.getResource(basePath + file);
        try (InputStream is = resource.getInputStream()) {
            return StreamUtils.copyToString(is, UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static class LeafletIdRow {
        private Long id;
        private Double status;
    }

    private static class LeafletLastUpdateRow {
        private Long leafletId;
        private Double leafletStatus;
        private Timestamp leafletLastUpdate;
        private Timestamp leafletStoreLastUpdate;
        private Timestamp closestLeafletStoreLastUpdate;
        private Timestamp ubLastUpdate;
    }

    private static class AdvantageRow {
        private Long code;
        private String description;
    }

    private static class LeafletEanRow {
        private Long leafletCode;
        private String occProdCodes;
        private String ean;
        private String promoCode;
        private Long prodCode;
        private String scope;
        private String statusMatchEan;
        private String matchingType;
        private Boolean implicit;
        private String occProdDescription;
        private String promoDescrDs;
        private Boolean fgCoverPage;
        private Long nbUnitInPack;
        private Long nbUnitMinForPromo;
        private Integer fgLoyaltyCard;
        private Double pricePaidInclReduc;
        private Double priceBeforeReduc;
        private Double priceInstantPromo;
        private Double priceAllTypesPromo;
        private Double rateDiscountNip;
        private Double rateGratuityOnPack;
        private Double ratePriceCrossedOut;
        private String startingDatePromo;
        private String endingDatePromo;
        private String urlPictureUb;
        private String urlAppliUb;
        private String dsCreateDate;
        private String dsUpdateDate;
        private Double supportPrice;
        private Double crossedOutPrice;
        private Double netPrice;
        private Long ubCode;
    }

    private static class LeafletRow {
        private Long leafletCode;
        private Long opCode;
        private String description;
        private String startingDate;
        private String endingDate;
        private Long bannerCode;
        private String bannerDescr;
        private Long nbStoreBanner;
        private String theme;
        private Long nbPageLeaflet;
        private Long nbStoreOp;
        private Long nbStoreLeaflet;
        private Long nbOccProd;
        private String typeOp;
        private String typeSupport;
        private String urlFirstPage;
        private String urlLastPage;
        private String dsCreateDate;
        private String dsUpdateDate;
        private Double status;
    }

    private static class LeafletStoreRow {
        private Long leafletCode;
        private String storeCodeDs;
        private String dsCreateDate;
        private String dsUpdateDate;
    }

    private static class StoreRow {
        private String storeCodeDs;
        private String storeCodeIri;
        private String openDate;
        private String closureDate;
        private String lsaCode;
        private String bannerCode;
        private String bannerName;
        private String addr1;
        private String addr2;
        private String zipcode;
        private String town;
        private String inseeCode;
        private String longitude;
        private String latitude;
        private String dsCreateDate;
        private String dsUpdateDate;
    }

    private static class ClassificationEntryRow {
        private Integer sinOid;
        private String classification;
        private String shelf;
        private String category;
        private String segment;
        private String subSegment;
        private String nonVaryingSubSegment;
    }

    private static class ProductRow {
        private Integer id;
        private Integer sinOid;
        private String brandName;
        private String label;
        private String quantity;
        private String gratuity;
    }

    private static class SegEan {
        private String ean;
        private String segEan;
    }

    private static class MultiEan {
        private String ean;
        private String multiEan;
    }

}
