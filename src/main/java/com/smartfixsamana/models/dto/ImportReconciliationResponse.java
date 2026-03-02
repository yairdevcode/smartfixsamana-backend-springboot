package com.smartfixsamana.models.dto;

import java.util.List;

public record ImportReconciliationResponse(
        List<ExternalRepairResponse> entregadas,
        List<ExternalRepairResponse> pendientesRecoger,
        int totalEntregadas,
        int totalPendientesRecoger,
        int entregadasNuevas,
        int entregadasPendientesAnteriores
) {
    public ImportReconciliationResponse(
            List<ExternalRepairResponse> entregadas,
            List<ExternalRepairResponse> pendientesRecoger,
            int totalEntregadas,
            int totalPendientesRecoger) {
        this(entregadas, pendientesRecoger, totalEntregadas, totalPendientesRecoger, 0, 0);
    }
}
