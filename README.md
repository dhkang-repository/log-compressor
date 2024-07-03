

<h3> 애플리케이션 빌드</h3>

<li>Maven 또는 Gradle을 사용하여 애플리케이션을 빌드합니다.</li>

```
[Maven]
mvn clean package

[Gradle]
gradle clean build
```

<h3>애플리케이션 실행</h3>
<li>빌드된 JAR 파일을 실행합니다.</li>

```
java -jar log-compressor-0.0.1-SNAPSHOT.jar /Users/dhk/Desktop/spring/log-compressor/build/libs/
```