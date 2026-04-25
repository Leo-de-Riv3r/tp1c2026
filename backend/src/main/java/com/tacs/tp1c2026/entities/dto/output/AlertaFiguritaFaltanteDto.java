package com.tacs.tp1c2026.entities.dto.output;

public class AlertaFiguritaFaltanteDto extends AlertaDto {

  private final Integer stickerId;
  private final Integer stickerNumber;
  private final String stickerDescription;
  private final String stickerCountry;
  private final String stickerTeam;
  private final String stickerCategory;

  public AlertaFiguritaFaltanteDto(
      Integer id,
      Integer stickerId,
      Integer stickerNumber,
      String stickerDescription,
      String stickerCountry,
      String stickerTeam,
      String stickerCategory) {
    super(id, "FIGURITA_FALTANTE");
    this.stickerId = stickerId;
    this.stickerNumber = stickerNumber;
    this.stickerDescription = stickerDescription;
    this.stickerCountry = stickerCountry;
    this.stickerTeam = stickerTeam;
    this.stickerCategory = stickerCategory;
  }

  public Integer getStickerId() {
    return stickerId;
  }

  public Integer getStickerNumber() {
    return stickerNumber;
  }

  public String getStickerDescription() {
    return stickerDescription;
  }

  public String getStickerCountry() {
    return stickerCountry;
  }

  public String getStickerTeam() {
    return stickerTeam;
  }

  public String getStickerCategory() {
    return stickerCategory;
  }
}
