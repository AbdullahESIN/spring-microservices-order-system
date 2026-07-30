# ---- 1. asama: derleme ----
# Kaynak kodu bu asamada derlenir; olusan jar bir sonraki asamaya kopyalanir.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Once sadece pom.xml kopyalanir ki bagimliliklar Docker katmaninda onbellege alinsin.
# Boylece kod degistiginde bagimliliklar tekrar indirilmez (build cok hizlanir).
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B -DskipTests package

# ---- 2. asama: calistirma ----
# Sadece JRE iceren kucuk imaj. Maven ve kaynak kod son imajda yer almaz.
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Guvenlik: root yerine yetkisiz kullanici ile calistir
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --from=build /app/target/user-service-0.0.1-SNAPSHOT.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
