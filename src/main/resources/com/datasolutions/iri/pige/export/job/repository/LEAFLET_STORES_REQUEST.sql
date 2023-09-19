SELECT
	leafletCode leafletCode,
    storeCodeDs storeCodeDs,
	MAX(dsCreateDate) dsCreateDate,
    MAX(dsUpdateDate) dsUpdateDate
FROM (
    SELECT
        s1.sup_oid AS leafletCode,
        m1.mag_oid AS storeCodeDs,
        DATE_FORMAT(n1.nsm_created_at,  '%Y%m%d%H%i%s') AS dsCreateDate,
        DATE_FORMAT(n1.nsm_created_at,  '%Y%m%d%H%i%s') AS dsUpdateDate
    FROM mag_magasin m1
        INNER JOIN mae_magasin_externe mae1 ON (m1.MAG_OID = mae1.MAG_OID)
        INNER JOIN ens_enseigne e1  ON m1.ens_oid = e1.ens_oid
        INNER JOIN nsm_nn_sup_mag n1 ON m1.mag_oid = n1.mag_oid
        INNER JOIN sup_support s1 ON s1.sup_oid = n1.sup_oid
    WHERE
        s1.sup_oid IN (:leafletIds)
        AND ENS_EAN_MATCHING_PRIORITY = 1 and DAF_OID = 2
UNION ALL
    SELECT
        s1.closest_sup_oid AS leafletCode,
        m1.mag_oid AS storeCodeDs,
        DATE_FORMAT(n1.nsm_created_at,  '%Y%m%d%H%i%s') AS dsCreateDate,
        DATE_FORMAT(n1.nsm_created_at,  '%Y%m%d%H%i%s') AS dsUpdateDate
    FROM mag_magasin m1
        INNER JOIN mae_magasin_externe mae1 ON (m1.MAG_OID = mae1.MAG_OID)
        INNER JOIN ens_enseigne e1  ON m1.ens_oid = e1.ens_oid
        INNER JOIN nsm_nn_sup_mag n1 ON m1.mag_oid = n1.mag_oid
        INNER JOIN sup_support s1 ON s1.sup_oid = n1.sup_oid
        INNER JOIN sup_support s2 ON s1.closest_sup_oid = s2.sup_oid
    WHERE
        s1.closest_sup_oid IN (:leafletIds)
        AND ENS_EAN_MATCHING_PRIORITY = 1 and DAF_OID = 2
        AND s1.sup_statut = -1
) b
GROUP BY leafletCode, storeCodeDs;