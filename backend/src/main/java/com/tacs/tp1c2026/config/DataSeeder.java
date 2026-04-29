package com.tacs.tp1c2026.config;

import com.tacs.tp1c2026.entities.Card;
import com.tacs.tp1c2026.entities.enums.CardCategory;
import com.tacs.tp1c2026.repositories.CardsRepository;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DataSeeder implements CommandLineRunner {
  @Autowired
  private CardsRepository cardRepository;

  @Override
  public void run(String... args) throws Exception {
    if (cardRepository.count() == 0) {

      log.info("⏳ Base vacía. Cargando el catálogo de 500 cartas...");
      List<Card> cardsToLoad = new ArrayList<>();

      // Leemos el archivo desde el classpath
      InputStream is = new ClassPathResource("catalog/cards_catalog_500.xlsx").getInputStream();
      Workbook workbook = new XSSFWorkbook(is);
      Sheet sheet = workbook.getSheetAt(0); // La primera solapa

      // Iteramos las filas (empezamos en 1 para saltar el encabezado)
      for (int i = 1; i <= sheet.getLastRowNum(); i++) {
        Row row = sheet.getRow(i);
        if (row == null) continue;

        // Mapeamos las celdas a tu clase Card
        Card card = Card.builder()
            .number((int) row.getCell(0).getNumericCellValue())
            .name(row.getCell(1).getStringCellValue())
            .description(row.getCell(2).getStringCellValue())
            .country(row.getCell(3).getStringCellValue())
            .team(row.getCell(4).getStringCellValue())
            .category(CardCategory.valueOf(row.getCell(5).getStringCellValue()))
            .build();

        cardsToLoad.add(card);
      }

      cardRepository.saveAll(cardsToLoad);
      workbook.close();
      log.info("✅ ¡Catalogo de 500 cartas cargado! ");
    }
  }
}
