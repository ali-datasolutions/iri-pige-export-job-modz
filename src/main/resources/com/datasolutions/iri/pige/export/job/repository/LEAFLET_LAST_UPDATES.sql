SELECT
    support.SUP_OID AS leafletId,
    support.SUP_STATUT AS leafletStatus,
    support.SUP_UPDATED_AT AS leafletLastUpdate,
    MAX(support_magasin.NSM_CREATED_AT) AS leafletStoreLastUpdate,
    MAX(support_proche_magasin.NSM_CREATED_AT) AS closestLeafletStoreLastUpdate,
    MAX(occurence.OCR_UPDATED_AT) AS ubLastUpdate
FROM sup_support support
    LEFT JOIN ens_enseigne enseigne ON support.ENS_OID = enseigne.ENS_OID
    LEFT JOIN nsm_nn_sup_mag support_magasin ON support.SUP_OID = support_magasin.SUP_OID
    LEFT JOIN sup_support support_proche ON support_proche.CLOSEST_SUP_OID = support.SUP_OID
    LEFT JOIN nsm_nn_sup_mag support_proche_magasin ON support_proche.SUP_OID = support_proche_magasin.SUP_OID
    LEFT JOIN pie_piece piece  ON support.SUP_OID = piece.SUP_OID
    LEFT JOIN ube_unite_besoin ub ON piece.PIE_OID = ub.PIE_OID
    LEFT JOIN ocr_occurence occurence ON ub.UBE_OID = occurence.UBE_OID
WHERE support.SUP_OID IN (:leafletIds)
GROUP BY support.SUP_OID;