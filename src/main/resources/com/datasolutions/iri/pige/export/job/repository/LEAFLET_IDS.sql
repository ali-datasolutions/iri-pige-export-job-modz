SELECT DISTINCT support.SUP_OID AS id, SUP_STATUT AS status
FROM sup_support support
    INNER JOIN opn_operation operation ON support.opn_oid = operation.opn_oid
    INNER JOIN pie_piece pie ON support.SUP_OID = pie.SUP_OID
    INNER JOIN ube_unite_besoin ube ON pie.PIE_OID = ube.PIE_OID
    INNER JOIN eco_ean_connection eco ON ube.UBE_OID = eco.UBE_OID
    INNER JOIN ens_enseigne enseigne ON support.ENS_OID = enseigne.ENS_OID
    INNER JOIN tys_type_support type_support ON support.TYS_OID = type_support.TYS_OID
WHERE
	operation.OPN_DATE_DEBUT >= :startDate AND operation.OPN_DATE_DEBUT <= :endDate
	AND support.sup_statut = 4
	AND type_support.TYS_OID IN (:supportTypeIds)
	AND ENS_EAN_MATCHING_PRIORITY = 1
	AND SUP_IS_STORE_ATTACHMENT_VALIDATED = true
	AND OPN_DATE_DEBUT < DATE_SUB(ECO_UPDATED_AT, INTERVAL 11 WEEK)
	AND ECO_UPDATED_AT >= :endDate
	AND MIS_OID IS NULL
	AND ECO_STATUS in (8,9)