package com.smartfixsamana.models.dto;

import java.util.List;

public record ImportReconciliationResponse(
        List<ExternalRepairResponse> entregadas,
        List<ExternalRepairResponse> pendientesRecoger,
        int totalEntregadas,
        int totalPendientesRecoger
) {}
