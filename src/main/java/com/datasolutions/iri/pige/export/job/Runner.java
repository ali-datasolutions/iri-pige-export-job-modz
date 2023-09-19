package com.datasolutions.iri.pige.export.job;

import com.datasolutions.iri.pige.export.job.bean.*;
import com.datasolutions.iri.pige.export.job.repository.DataRepository;
import com.datasolutions.iri.pige.export.job.repository.StateRepository;
import com.google.cloud.WriteChannel;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.common.base.Joiner;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.io.ByteStreams;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.xfer.FileSystemFile;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.channels.Channels;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by romain on 29/11/2019
 */
@Slf4j
@Component
public class Runner implements CommandLineRunner {

    private static final Joiner JOINER = Joiner.on("|").useForNull("NULL");
    private static final DateTimeFormatter FILE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private static final RetryPolicy<Object> IO_RETRY_POLICY = new RetryPolicy<>()
        .withMaxRetries(10)
        .withBackoff(1, 300, ChronoUnit.SECONDS)
        .handle(IOException.class);

    private final RunnerProperties properties;
    private final DataRepository dataRepository;
    private final StateRepository stateRepository;

    @Autowired
    public Runner(RunnerProperties properties, DataRepository dataRepository, Optional<StateRepository> stateRepository) {
        this.properties = properties;
        this.dataRepository = dataRepository;

        Boolean incremental = this.properties.getIncremental();
        if (incremental) {
            this.stateRepository = stateRepository
                .orElseThrow(() -> new RuntimeException("State repository must be provided in case incremental mode is activated!"));
        } else {
            this.stateRepository = null;
        }
    }

    @Override
    public void run(String... args) throws Exception {
        LocalDate startDate;
        LocalDate endDate;
        LocalDate yesterday = LocalDate.now().minusDays(1);

        String rawStartDate = properties.getStartDate();
        if (rawStartDate == null) {
            startDate = null;
        } else {
            startDate = LocalDate.parse(rawStartDate);
        }

        String rawEndDate = properties.getEndDate();
        if (rawEndDate == null) {
            endDate = yesterday;
        } else {
            endDate = LocalDate.parse(rawEndDate);
        }

        checkArgument(startDate == null || !startDate.isAfter(endDate), "Start date must be <= end date!");
        if (startDate == null && endDate.isEqual(yesterday)) {
            startDate = yesterday.minusWeeks(10).with(DayOfWeek.MONDAY);
        }

        Boolean uploadEnabled = properties.getUpload().getEnabled();
        RunnerProperties.Sftp sftp = properties.getUpload().getSftp();
        RunnerProperties.Gcs gcs = properties.getUpload().getGcs();
        if (uploadEnabled) {
            checkArgument(sftp != null || gcs != null, "SFTP or GCS properties must be provided when upload is enabled!");
        }

        Set<Integer> supportTypeIds = properties.getSupportTypeIds();
        Set<LeafletId> leafletIds = dataRepository.getLeafletIds(startDate, endDate, supportTypeIds);
        log.info("Found {} leaflet(s) that opn date started after {} and have been modified after {}!", leafletIds.size(), startDate, endDate);

        if (leafletIds.size() == 0) {
            return;
        }

        UUID processUuid = UUID.randomUUID();
        Map<Long, LeafletState> states = Maps.newHashMap();
        Boolean incremental = properties.getIncremental();
        if (incremental) {
            Map<Long, LeafletId> leafletIdIndex = leafletIds.stream()
                .collect(Collectors.toMap(LeafletId::getId, Function.identity()));
            long timestamp = System.currentTimeMillis();
            Set<Long> idsToUpdate = getLeafletIdsToUpdate(leafletIdIndex.keySet());

            Set<LeafletId> leafletIdsToUpdate = Sets.newHashSet();
            idsToUpdate.forEach(leafletId -> {
                LeafletState state = new LeafletState(leafletId, processUuid, timestamp);
                states.put(leafletId, state);

                LeafletId id = leafletIdIndex.get(leafletId);
                leafletIdsToUpdate.add(id);
            });
            leafletIds = leafletIdsToUpdate;
        }


        log.info("Running data export for {} leaflet(s) with startDate: {} and matchingDate >= : {}", leafletIds.size(), startDate, endDate);
        Path archive = exportData(leafletIds);

        String suffix = FILE_DATE_FORMATTER.format(LocalDate.now());
        String filename = "MOD_LEAFLET_DATA_SOLUTIONS_TO_IRI_" + suffix + ".tar.gz";
        if (uploadEnabled && gcs != null) {
            Failsafe.with(IO_RETRY_POLICY).run(() -> uploadData(gcs, archive, filename));
        }

        if (uploadEnabled && sftp != null) {
            Failsafe.with(IO_RETRY_POLICY).run(() -> uploadData(sftp, archive, filename));
        }

        Collection<LeafletState> statesList = states.values();
        if (incremental && !statesList.isEmpty()) {
            log.info("Updating leaflets state database with {} entries!", statesList.size());
            stateRepository.createOrUpdateStates(statesList);
        }
    }

    private Path exportData(Set<LeafletId> leafletIds) throws IOException {
        Set<Long> finishedLeafletIds = leafletIds.stream()
            .filter(leafletId -> leafletId.getStatus().isFinish())
            .map(LeafletId::getId)
            .collect(Collectors.toSet());

        Set<Long> allLeafletIds = leafletIds.stream()
            .map(LeafletId::getId)
            .collect(Collectors.toSet());




        Map<Long, Leaflet.MatchStatus> statuses = Maps.newHashMap();

        log.info("Fetching and writing leaflets...");
        Path leafletsPath = Failsafe.with(IO_RETRY_POLICY)
            .get(() -> exportLeaflets(allLeafletIds, statuses));
        log.info("Exported leaflets to file at path: {}", leafletsPath);

        log.info("Fetching and writing leaflet stores...");
        Path leafletStoresPath = Failsafe.with(IO_RETRY_POLICY)
            .get(() -> exportLeafletStores(finishedLeafletIds));
        log.info("Exported leaflet stores to file at path: {}", leafletStoresPath);

        log.info("Fetching and writing leaflets EANs...");
        Map<Long, List<LeafletEan.MatchStatus>> eanStatuses = Maps.newConcurrentMap();
        Path leafletEansPath = Failsafe.with(IO_RETRY_POLICY)
                .get(() -> exportLeafletEans(finishedLeafletIds, properties.getWineSegmentIds(),
                        properties.getBazaarSegmentIds(), leafletEan -> {
                            Long leafletCode = leafletEan.getLeafletCode();
                            List<LeafletEan.MatchStatus> leafletEanStatuses = eanStatuses.computeIfAbsent(leafletCode, k -> Lists.newArrayList());
                            leafletEanStatuses.add(leafletEan.getStatusMatchEan());
                        }));
        log.info("Exported leaflet EANs to file at path: {}", leafletEansPath);

        eanStatuses.forEach((leafletCode, statusList) -> {
            long inProgressDs = statusList.stream()
                    .filter(status -> status.equals(LeafletEan.MatchStatus.IN_PROGRESS_DS))
                    .count();
            long inProgressIri = statusList.stream()
                    .filter(status -> status.equals(LeafletEan.MatchStatus.IN_PROGRESS_IRI))
                    .count();
            Leaflet.MatchStatus matchStatus = inProgressDs > 0 ? Leaflet.MatchStatus.IN_PROGRESS_DS :
                    inProgressIri > 0 ? Leaflet.MatchStatus.IN_PROGRESS_IRI : Leaflet.MatchStatus.FINISH;
            statuses.put(leafletCode, matchStatus);
        });

        log.info("Archiving exported files...");
        String suffix = FILE_DATE_FORMATTER.format(LocalDate.now());
        Path archivePath = Files.createTempFile("ARCHIVE", ".tar.gz");
        FileOutputStream fos = new FileOutputStream(archivePath.toFile());
        BufferedOutputStream bos = new BufferedOutputStream(fos);
        GZIPOutputStream gos = new GZIPOutputStream(bos);
        try (TarArchiveOutputStream tarOutputStream = new TarArchiveOutputStream(gos)) {
            createTarEntry(tarOutputStream, leafletStoresPath, "MOD_LEAFLET_STORE_" + suffix + ".dat");
            createTarEntry(tarOutputStream, leafletsPath, "MOD_LEAFLET_" + suffix + ".dat");
            createTarEntry(tarOutputStream, leafletEansPath, "MOD_LEAFLET_EANS_" + suffix + ".dat");
        }
        log.info("Archived exported files into file at path: {}", archivePath);

        return archivePath;
    }

    private void uploadData(RunnerProperties.Sftp sftp, Path archivePath, String filename) throws IOException {
        log.info("Uploading archive to sFTP: {}", archivePath);
        SSHClient ssh = new SSHClient();
        ssh.addHostKeyVerifier((s, i, publicKey) -> true);
        ssh.connect(sftp.getHostname(), sftp.getPort());

        try {
            ssh.authPassword(sftp.getUsername(), sftp.getPassword());
            SFTPClient client = ssh.newSFTPClient();

            FileSystemFile fileSystemFile = new FileSystemFile(archivePath.toFile());
            client.put(fileSystemFile, "/up/" + filename);
        } finally {
            ssh.disconnect();
        }
        log.info("Archive was successfully uploaded to {}", sftp.getHostname());
    }

    private void uploadData(RunnerProperties.Gcs gcs, Path archivePath, String filename) throws IOException {
        log.info("Uploading archive to GCS: {}", archivePath);
        Storage storage = StorageOptions.getDefaultInstance().getService();

        String identifier = Joiner.on("-").join(LocalDateTime.now(), UUID.randomUUID());
        BlobInfo blobInfo = BlobInfo.newBuilder(gcs.getBucket(), Joiner.on("/").join(identifier, filename)).build();
        try (InputStream inputStream = Files.newInputStream(archivePath);
             WriteChannel writer = storage.writer(blobInfo)) {
            OutputStream outputStream = Channels.newOutputStream(writer);
            ByteStreams.copy(inputStream, outputStream);
        }
        log.info("Archive was successfully uploaded to {}", blobInfo.getBlobId());
    }

    private Set<Long> getLeafletIdsToUpdate(Set<Long> leafletIds) {
        List<LeafletState> leafletStates = stateRepository.getByLeafletId(leafletIds);
        Map<Long, LeafletState> leafletStateIndex = leafletStates.stream()
            .collect(Collectors.toMap(LeafletState::getLeafletId, Function.identity()));

        Set<Long> leafletIdsToUpdate = Sets.newHashSet();
        List<LeafletLastUpdate> leafletLastUpdates = dataRepository.getLeafletLastUpdates(leafletIds);
        for (LeafletLastUpdate leafletLastUpdate : leafletLastUpdates) {
            Long leafletId = leafletLastUpdate.getLeafletId();
            LeafletState leafletState = leafletStateIndex.get(leafletId);
            if (leafletState == null || leafletLastUpdate.getLastUpdate() > leafletState.getProcessLastUpdate()) {
                leafletIdsToUpdate.add(leafletId);
            }
        }
        return leafletIdsToUpdate;
    }

    private Path exportLeafletStores(Set<Long> leafletIds) {
        String header = "LEAFLET_CODE|STORECODE_DS|DS_CREATE_DATE|DS_UPDATE_DATE";
        return writeToTemporaryFile(writer -> {
            writeLine(writer, header);
            dataRepository.getLeafletStores(leafletIds, row -> {
                String line = JOINER.join(row.getLeafletCode(), row.getStoreCodeDs(), row.getDsCreateDate(), row.getDsUpdateDate());
                writeLine(writer, line);
            });
        });
    }

    private Path exportStores() {
        String header = "STORECODE_DS|OPEN_DATE|CLOSURE_DATE|LSA_CODE|BANNER_CODE|BANNER_NAME|ADDR1|ADDR2|ZIPCODE|TOWN|INSEE_CODE|LONGITUDE|LATITUDE|DS_CREATE_DATE|DS_UPDATE_DATE|STORECODE_IRI";
        return writeToTemporaryFile(writer -> {
            writeLine(writer, header);
            dataRepository.getStores(row -> {
                String line = JOINER.join(row.getStoreCodeDs(), row.getOpenDate(), row.getClosureDate(), row.getLsaCode(),
                    row.getBannerCode(), row.getBannerName(), row.getAddr1(), row.getAddr2(),
                    row.getZipcode(), row.getTown(), row.getInseeCode(), row.getLongitude(), row.getLatitude(),
                    row.getDsCreateDate(), row.getDsUpdateDate(), row.getStoreCodeIri());
                writeLine(writer, line);
            });
        });
    }

    private Path exportClassificationEntries() {
        String header = "SIN_OID|NOMENCLATURE|RAYON|CATEGORIE|SEGMENT|SOUS_SEGMENT|SOUS_SEGMENT_INVARIANT";
        return writeToTemporaryFile(writer -> {
            writeLine(writer, header);
            dataRepository.getClassificationEntries(row -> {
                String line = JOINER.join(row.getSinOid(), row.getClassification(), row.getShelf(), row.getCategory(),
                    row.getSegment(), row.getSubSegment(), row.getNonVaryingSubSegment());
                writeLine(writer, line);
            });
        });
    }

    private Path exportLeaflets(Set<Long> leafletIds, Map<Long, Leaflet.MatchStatus> statuses) {
        String header = "LEAFLET_CODE|OP_CODE|DESCRIPTION|STARTING_DATE|ENDING_DATE|BANNER_CODE|BANNER_DESC|" +
            "NB_STORE_BANNER|THEME|NB_PAGE_LEAFLET|NB_STORE_OP|NB_STORE_LEAFLET|NB_OCC_PROD_LEAFLET|TYPE_OP|TYPE_SUPPORT|" +
            "URL_FIRST_PAGE|URL_LAST_PAGE|DS_CREATE_DATE|DS_UPDATE_DATE|STATUS_LEAFLET|STATUS_LEAFLET_EAN";
        return writeToTemporaryFile(writer -> {
            writeLine(writer, header);
            dataRepository.getLeaflets(leafletIds, row -> {
                Leaflet.MatchStatus leafletStatusEan = statuses.get(row.getLeafletCode());
                leafletStatusEan = MoreObjects.firstNonNull(leafletStatusEan, Leaflet.MatchStatus.IN_PROGRESS_DS);
                String line = JOINER.join(row.getLeafletCode(), row.getOpCode(), row.getDescription(), row.getStartingDate(),
                    row.getEndingDate(), row.getBannerCode(), row.getBannerDescr(), row.getNbStoreBanner(), row.getTheme(),
                    row.getNbPageLeaflet(), row.getNbStoreOp(), row.getNbStoreLeaflet(), row.getNbOccProd(), row.getTypeOp(),
                    row.getTypeSupport(), row.getUrlFirstPage(), row.getUrlLastPage(), row.getDsCreateDate(), row.getDsUpdateDate(),
                    row.getStatus().getValue(), leafletStatusEan.getValue());
                writeLine(writer, line);
            });
        });
    }

    private Path exportLeafletEans(Set<Long> leafletIds, Set<Integer> wineSegmentIds, Set<Integer> bazaarSegmentIds,
                                   Consumer<LeafletEan> consumer) {
        String header = "LEAFLET_CODE|PROD_CODE|EAN|PROMO_CODE|LIST_OCC_PROD_CODE|SCOPE|STATUS_MATCH_EAN|TYPE_MATCHING|PROD_DESCRIPTION|" +
            "PROMO_DESCR_DS|FG_COVER_PAGE|NB_UNIT_IN_PACK|NB_UNIT_MIN_FOR_PROMO|FG_LOYALTY_CARD|PRICE_PAID_INCL_REDUC|" +
            "PRICE_BEFORE_REDUC|PRICE_INSTANT_PROMO|PRICE_ALL_TYPES_PROMO|RATE_DISCOUNT_NIP|RATE_GRATUITY_ON_PACK|" +
            "RATE_PRICE_CROSSED_OUT|RATE_TOTAL_DISCOUNT|STARTING_DATE_PROMO|ENDING_DATE_PROMO|URL_PICTURE_UB|" +
            "URL_APPLI_UB|DS_CREATE_DATE|DS_UPDATE_DATE|FG_UB_TYPE|UB_CODE";

        Set<String> allEans = Sets.newHashSet();
        dataRepository.getLeafletEans(leafletIds, wineSegmentIds, bazaarSegmentIds, line -> {
            String ean = line.getEan();
            if (ean != null) {
                allEans.add(ean);
            }
        });
        log.info("Fetched {} unique EANs from {} leaflets...", allEans.size(), leafletIds.size());

        EanLinks segLinks = new EanLinks(allEans);
        dataRepository.getEanSeg(segLinks.getEans()).forEach(segLinks::putLink);
        log.info("Fetched {} SEG EANs from {} EANs...", segLinks.size(), allEans.size());

        EanLinks multiLinks = new EanLinks(allEans);
        dataRepository.getEanMulti(multiLinks.getEans()).forEach(multiLinks::putLink);
        log.info("Fetched {} MULTI EANs from {} EANs...", multiLinks.size(), allEans.size());

        return writeToTemporaryFile(writer -> {
            writeLine(writer, header);

            Long defaultProductCode = -1L;
            AtomicReference<Long> currentProductCode = new AtomicReference<>(defaultProductCode);
            List<LeafletEan> currentProductLines = Lists.newArrayList();
            dataRepository.getLeafletEans(leafletIds, wineSegmentIds, bazaarSegmentIds, line -> {
                consumer.accept(line);

                Long productCode = line.getProdCode();
                if (currentProductCode.get().equals(defaultProductCode)) {
                    // Init for first line
                    currentProductCode.set(productCode);
                    currentProductLines.add(line);
                    return;
                }

                if (!currentProductCode.get().equals(productCode)) {
                    writeProductLines(currentProductLines, writer, segLinks, multiLinks);
                    currentProductLines.clear();
                    currentProductCode.set(productCode);
                }

                currentProductLines.add(line);
            });

            if (!currentProductLines.isEmpty()) {
                writeProductLines(currentProductLines, writer, segLinks, multiLinks);
            }
        });
    }

    private Path exportAdvantages() {
        String header = "ADVANTAGE_CODE|ADVANTAGE_DESCRIPTION";
        return writeToTemporaryFile(writer -> {
            writeLine(writer, header);
            dataRepository.getAdvantages(row -> {
                String line = JOINER.join(row.getCode(), row.getDescription());
                writeLine(writer, line);
            });
        });
    }

    private Path exportProducts() {
        String header = "UNIQ_CODE_PRODUIT|SIN_OID_CODE|MARQUE|LIBELLE|VOLUME_TOTAL|%GRT";
        return writeToTemporaryFile(writer -> {
            writeLine(writer, header);
            dataRepository.getProducts(row -> {
                String line = JOINER.join(row.getId(), row.getSinOid(), row.getBrandName(),
                        row.getLabel().replace("\\|", ""),
                        row.getQuantity(), row.getGratuity());
                writeLine(writer, line);
            });
        });
    }

    private void writeProductLines(List<LeafletEan> currentProductLines, Writer writer,
                                   EanLinks segLinks, EanLinks multiLinks) {
        LeafletEan line = currentProductLines.stream().findFirst().get();

        Double totalDiscountRate;
        try {
            totalDiscountRate = calculateTotalDiscountRate(line);
        } catch (Exception e) {
            log.error("Could not calculate discount rate for leaflet EAN with code: {}", line.getProdCode(), e);
            return;
        }

        String joinedPromotionDescriptions = currentProductLines.stream()
            .map(leafletEan -> new Promotion(leafletEan.getPromoCode(), leafletEan.getPromoDescrDs()))
            .distinct()
            .map(Promotion::getDescription)
            .collect(Collectors.joining("+"));

        Set<String> matchedEans = currentProductLines.stream()
            .map(LeafletEan::getEan)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        Set<String> segEans = Sets.newHashSet();
        Set<String> multiEans = Sets.newHashSet();
        for (String ean : matchedEans) {
            if (ean != null) {
                Set<String> eanSegLinks = segLinks.getLinks(ean);
                if (eanSegLinks == null) {
                    log.warn("Falling back to direct SEG EAN fetch for EAN: {}...", ean);
                    eanSegLinks = dataRepository.getEanSeg(ean);
                }
                segEans.addAll(eanSegLinks);

                if (line.getImplicit()) {
                    Set<String> eanMultiLinks = multiLinks.getLinks(ean);
                    if (eanMultiLinks == null) {
                        log.warn("Falling back to direct MULTI EAN fetch for EAN: {}...", ean);
                        eanMultiLinks = dataRepository.getEanMulti(ean);
                    }
                    multiEans.addAll(eanMultiLinks);
                }
            }
        }

        multiEans.removeAll(segEans);
        multiEans.removeAll(matchedEans);
        segEans.removeAll(matchedEans);

        for (LeafletEan row : currentProductLines) {
            String ean = row.getEan();

            ean = ean != null ? StringUtils.leftPad(ean, 13, "0") : "UNKNOWN";
            String strLine = createLeafletEanLine(row, ean, row.getMatchingType(), totalDiscountRate, joinedPromotionDescriptions, row.getPromoCode());
            writeLine(writer, strLine);
        }

        Set<String> promoCodes = currentProductLines.stream()
            .map(LeafletEan::getPromoCode)
            .collect(Collectors.toSet());
        for (String promoCode : promoCodes) {
            segEans.forEach(segEan -> {
                String padded = StringUtils.leftPad(segEan, 13, "0");
                String segLine = createLeafletEanLine(line, padded, LeafletEan.MatchType.MATCH_DS_SEGMENTING, totalDiscountRate, joinedPromotionDescriptions, promoCode);
                writeLine(writer, segLine);
            });

            multiEans.forEach(multiEan -> {
                String padded = StringUtils.leftPad(multiEan, 13, "0");
                String multiLine = createLeafletEanLine(line, padded, LeafletEan.MatchType.MATCH_DS_MULTI, totalDiscountRate, joinedPromotionDescriptions, promoCode);
                writeLine(writer, multiLine);
            });
        }
    }

    private Double calculateTotalDiscountRate(LeafletEan leafletEan) {
        Double netPrice = leafletEan.getNetPrice();
        Double supportPrice = leafletEan.getSupportPrice();
        Double crossedOutPrice = leafletEan.getCrossedOutPrice();
        Double rateGratuityOnPack = leafletEan.getRateGratuityOnPack();

        if (supportPrice == null) {
            return leafletEan.getRateDiscountNip();
        }

        boolean hasGratuityOnPack = rateGratuityOnPack != 0;
        boolean hasCrossedOutPrice = crossedOutPrice != null && crossedOutPrice != 0;
        boolean hasNip = supportPrice.equals(netPrice);

        if (hasGratuityOnPack) {
            Double grossPrice = supportPrice / (1 - rateGratuityOnPack);
            return (grossPrice - netPrice) / grossPrice;
        }

        if (hasCrossedOutPrice && hasNip) {
            return (crossedOutPrice - netPrice) / crossedOutPrice;
        }

        return (supportPrice - netPrice) / supportPrice;
    }

    private String createLeafletEanLine(LeafletEan row, String ean, LeafletEan.MatchType matchType,
                                        Double totalDiscountRate, String promotionDescription, String promoCode) {
        return JOINER.join(row.getLeafletCode(), row.getProdCode(), ean, promoCode, row.getOccProdCodes(),
            row.getScope(), row.getStatusMatchEan().getValue(), matchType == LeafletEan.MatchType.NA ? "" : matchType.getValue(),
            row.getOccProdDescription(), promotionDescription, row.getFgCoverPage() ? 1 : 0, row.getNbUnitInPack(),
            row.getNbUnitMinForPromo(), row.getFgLoyaltyCard() ? 1 : 0, row.getPricePaidInclReduc(),
            row.getPriceBeforeReduc(), row.getPriceInstantPromo(), row.getPriceAllTypesPromo(),
            round(row.getRateDiscountNip()) > 1 ? round(row.getRateDiscountNip()/100): round(row.getRateDiscountNip()), round(row.getRateGratuityOnPack()), row.getRatePriceCrossedOut(),
            round(totalDiscountRate) > 1 ? round(totalDiscountRate/100): round(totalDiscountRate), row.getStartingDatePromo(), row.getEndingDatePromo(),
            row.getUrlPictureUb(), row.getUrlAppliUb(), row.getDsCreateDate(), row.getDsUpdateDate(),
            row.getImplicit() ? 1 : 0, row.getUbCode());
    }

    private Double round(Double value) {
        return BigDecimal.valueOf(value)
            .setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    @Getter
    @EqualsAndHashCode(of = "code")
    private static class Promotion {

        private final String code;
        private final String description;

        private Promotion(String code, String description) {
            this.code = checkNotNull(code);
            this.description = checkNotNull(description);
        }
    }

    private Path writeToTemporaryFile(Consumer<BufferedWriter> consumer) {
        TemporaryFile temporaryFile = new TemporaryFile();
        try (BufferedWriter writer = temporaryFile.writer()) {
            consumer.accept(writer);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        Path path = temporaryFile.path;
        try {
            long size = Files.size(path);
            log.info("Wrote {} of data to path: {}", humanReadableByteCountBin(size), path);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return path;
    }

    private String humanReadableByteCountBin(long bytes) {
        return bytes < 1024L ? bytes + " B"
            : bytes < 0xfffccccccccccccL >> 40 ? String.format("%.1f KiB", bytes / 0x1p10)
            : bytes < 0xfffccccccccccccL >> 30 ? String.format("%.1f MiB", bytes / 0x1p20)
            : bytes < 0xfffccccccccccccL >> 20 ? String.format("%.1f GiB", bytes / 0x1p30)
            : bytes < 0xfffccccccccccccL >> 10 ? String.format("%.1f TiB", bytes / 0x1p40)
            : bytes < 0xfffccccccccccccL ? String.format("%.1f PiB", (bytes >> 10) / 0x1p40)
            : String.format("%.1f EiB", (bytes >> 20) / 0x1p40);
    }

    private void writeLine(Writer writer, String line) {
        try {
            writer.write(line + "\n");
        } catch (IOException e) {
            throw new RuntimeException("Could not write line: " + line, e);
        }
    }

    private void createTarEntry(TarArchiveOutputStream outputStream, Path path, String name) throws IOException {
        TarArchiveEntry entry = new TarArchiveEntry(name);
        entry.setSize(Files.size(path));
        outputStream.putArchiveEntry(entry);
        IOUtils.copy(Files.newInputStream(path), outputStream);
        outputStream.closeArchiveEntry();
    }

    private static class TemporaryFile {

        private final Path path;

        private TemporaryFile() {
            try {
                this.path = Files.createTempFile("EXPORT_TEMP", ".dat");
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        public BufferedWriter writer() {
            BufferedWriter writer;
            try {
                FileWriter fileWriter = new FileWriter(path.toFile());
                writer = new BufferedWriter(fileWriter);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            return writer;
        }

    }

}
