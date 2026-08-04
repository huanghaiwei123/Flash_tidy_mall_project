# 多阶段构建：先编译，再运行
FROM amazoncorretto:8-alpine AS build
WORKDIR /app
# 先复制 POM 利用 Docker 缓存层
COPY pom.xml .
COPY seckill-app/pom.xml seckill-app/
COPY captcha-spring-boot-starter/pom.xml captcha-spring-boot-starter/
# 下载依赖（充分利用缓存，源码改动时不重下）
RUN sed -i 's/dl-cdn.alpinelinux.org/mirrors.tuna.tsinghua.edu.cn/g' /etc/apk/repositories \
    && apk add --no-cache maven \
    && mvn dependency:go-offline -q -B
# 再复制源码
COPY seckill-app/src seckill-app/src/
COPY captcha-spring-boot-starter/src captcha-spring-boot-starter/src/
RUN mvn package -q -DskipTests

# 运行阶段：用 JRE 镜像减小体积
FROM amazoncorretto:8-alpine
WORKDIR /app
COPY --from=build /app/seckill-app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
