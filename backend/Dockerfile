# Etapa 1: Construcción (Build)
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# 1. Optimización de Caché: Descargar dependencias antes del código
COPY pom.xml .
RUN mvn dependency:go-offline

# Copiamos el código y compilamos
COPY src ./src
RUN mvn clean package -DskipTests

# Etapa 2: Ejecución (Run) - ARREGLO: Cambiado a Java 21
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# 2. Optimización de Encoding: Previene errores con tildes y caracteres especiales
ENV JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF-8"

# 3. Optimización de Seguridad: Correr sin permisos de root (Best Practice)
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copiamos el .jar
COPY --from=build /app/target/*.jar app.jar

# Exponemos el puerto
EXPOSE 8080

# Comando para arrancar
ENTRYPOINT ["java", "-jar", "app.jar"]