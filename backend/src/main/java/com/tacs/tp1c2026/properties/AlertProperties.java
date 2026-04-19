package com.tacs.tp1c2026.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.scheduled.alert")
public class AlertProperties {

  private Integer delayMinutes = 60;
  private Integer warningThresholdMinutes = 30;

  public Integer getDelayMinutes() {
    return delayMinutes;
  }

  public void setDelayMinutes(Integer delayMinutes) {
    this.delayMinutes = delayMinutes;
  }

  public Integer getWarningThresholdMinutes() {
    return warningThresholdMinutes;
  }

  public void setWarningThresholdMinutes(Integer warningThresholdMinutes) {
    this.warningThresholdMinutes = warningThresholdMinutes;
  }

  /**
   * Retorna el retardo de ejecución del proceso de alertas expresado en milisegundos.
   *
   * @return retardo en milisegundos equivalente a {@link #delayMinutes} minutos
   */
  public long getDelayMillis() {
    return delayMinutes * 60L * 1000L;
  }

  /**
   * Retorna el umbral de advertencia de subastas próximas expresado en milisegundos.
   *
   * @return umbral de advertencia en milisegundos equivalente a {@link #warningThresholdMinutes} minutos
   */
  public long getWarningThresholdMillis() {
    return warningThresholdMinutes * 60L * 1000L;
  }
}
