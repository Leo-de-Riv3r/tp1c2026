package com.tacs.tp1c2026.scheduled;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.tacs.tp1c2026.services.BucketService;


@Component
public class GeneradorSugerencias {

    private final BucketService bucketService;

    public GeneradorSugerencias(BucketService bucketService) {
        this.bucketService = bucketService;
    }

    /**
     * Tarea programada que ejecuta el cálculo de sugerencias de intercambio para todos
     * los usuarios del sistema. Se ejecuta según el cron configurado en
     * {@code app.scheduled.example.cron} (por defecto cada hora en punto).
     */
    @Scheduled(cron = "${app.scheduled.example.cron:0 0 * * * *}")
    public void generarSugerencias() {
        bucketService.generarSugerencias();
    }

}
