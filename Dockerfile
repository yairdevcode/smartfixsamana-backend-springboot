# Imagen de Java
FROM openjdk:17-jdk-slim

ARG JAR_FILE=target/smartfixsamana_back-0.0.1.jar

# Copia del jar compilado al contenedor
COPY ${JAR_FILE} app_smartfixsamana.jar

# Definir el perfil activo (clave para Spring Boot)
ENV SPRING_PROFILES_ACTIVE=prod

# Puerto que va a usar el contenedor
EXPOSE 8080

# Comando para ejecutar el jar
ENTRYPOINT ["java", "-jar", "app_smartfixsamana.jar"]
