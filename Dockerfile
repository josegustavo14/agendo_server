FROM gradle:jdk21-corretto

WORKDIR /app

# copia os arquivos de configuração do Gradle primeiro -> melhor uso de cache
COPY build.gradle settings.gradle ./
COPY gradle ./gradle

# baixa as dependências
RUN gradle dependencies --no-daemon

# copia o código-fonte
COPY src ./src

EXPOSE 8080

# roda com hot reload (spring-boot-devtools detecta mudanças no código)
CMD ["gradle", "bootRun", "--no-daemon"]
