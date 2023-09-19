SELECT
    SIN_OID AS sinOid,
    NOM_LIBELLE AS classification,
    RAY_LIBELLE AS shelf,
    CAE_LIBELLE AS category,
    SEG_LIBELLE AS segment,
    SSE_LIBELLE AS subSegment,
    SIN_LIBELLE AS nonVaryingSubSegment
FROM vno_vue_nomenclature
WHERE NOM_OID = 1;
