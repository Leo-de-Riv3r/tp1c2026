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

    @Scheduled(cron = "${app.scheduled.example.cron:0 0 * * * *}")
    public void generarSugerencias() {
        bucketService.generarSugerencias();
    }

}
