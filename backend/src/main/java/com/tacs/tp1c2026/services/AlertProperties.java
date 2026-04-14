package com.tacs.tp1c2026.services;

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

  public long getDelayMillis() {
    return delayMinutes * 60L * 1000L;
  }

  public long getWarningThresholdMillis() {
    return warningThresholdMinutes * 60L * 1000L;
  }
}
