FROM eclipse-temurin:21
RUN mkdir /opt/app

ADD https://api.papermc.io/v2/projects/paper/versions/1.21.5/builds/112/downloads/paper-1.21.5-112.jar /opt/app/server.jar
ADD build/libs/plugin-1.0.0-SNAPSHOT-all.jar /opt/app/plugins/plugin.jar

CMD ["java", "-Dcom.mojang.eula.agree=true", "-jar", "/opt/app/server.jar"]
