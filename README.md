# Test Task for QA Automation Engineer

```bash
mvn clean spring-boot:run
```

```bash
mvn clean spring-boot:run "-Dspring-boot.run.arguments=up"
```

```bash
mvn clean spring-boot:run "-Dspring-boot.run.arguments=status"
```

```bash
mvn clean spring-boot:run "-Dspring-boot.run.arguments=down"
```

```bash
mvn clean spring-boot:run "-Dspring-boot.run.arguments=history --sort asc -status STARTING --from 2024-11-01 --to 2024-11-30"
```

```bash
mvn clean spring-boot:run "-Dspring-boot.run.arguments=unknown"
```