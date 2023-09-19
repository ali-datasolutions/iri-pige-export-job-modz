SELECT
    IFNULL(a.STORECODE_DS, '-1') storeCodeDs,
    IFNULL(a.STORECODE_IRI, '') storeCodeIri,
    IFNULL(a.OPEN_DATE, '') openDate,
    IFNULL(a.CLOSURE_DATE, '') closureDate,
    IFNULL(a.LSA_CODE, '0') lsaCode,
    IFNULL(a.BANNER_CODE, '-1') bannerCode,
    IFNULL(a.BANNER_NAME, '-') bannerName,
    IFNULL(a.ADDR1, '-') addr1,
    IFNULL(a.ADDR2, '-') addr2,
    IFNULL(a.ZIPCODE, '-') zipcode,
    IFNULL(a.TOWN, '-') town,
    IFNULL(a.INSEE_CODE, '') inseeCode,
    IFNULL(a.LONGITUDE, '0') longitude,
    IFNULL(a.LATITUDE, '0') latitude,
    a.DS_CREATE_DATE dsCreateDate,
    a.DS_UPDATE_DATE dsUpdateDate
FROM (
    SELECT
        m1.mag_oid as STORECODE_DS,
        mae.MAE_ID_EXTERNE as STORECODE_IRI,
        NULL AS OPEN_DATE,
        NULL AS CLOSURE_DATE,
        MAG_ID_FOURNISSEUR AS LSA_CODE,
        m1.ENS_OID AS BANNER_CODE,
        ENS_RAISON_SOCIALE AS BANNER_NAME,
        MAG_ADRESSE1 AS ADDR1,
        IFNULL(MAG_ADRESSE2, '_') AS ADDR2,
        MAG_CODE_POSTAL AS ZIPCODE,
        MAG_VILLE AS TOWN,
        MAG_INSEE AS INSEE_CODE,
        MAG_COORDONNEE_X as LONGITUDE,
        MAG_COORDONNEE_Y as LATITUDE,
        DATE_FORMAT(MAG_CREATION_DATE, '%Y%m%d%H%i%s') AS DS_CREATE_DATE,
        DATE_FORMAT(MAG_LAST_MODIFICATION_DATE, '%Y%m%d%H%i%s') AS DS_UPDATE_DATE
    FROM mag_magasin m1
        INNER JOIN ens_enseigne e1 ON m1.ens_oid = e1.ens_oid
        INNER JOIN mae_magasin_externe mae ON (m1.MAG_OID = mae.MAG_OID)
    WHERE ENS_EAN_MATCHING_PRIORITY = 1 and DAF_OID = 2
) a
