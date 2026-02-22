# Gestion des Profils Spring Boot - Appointment Feature

## Vue d'ensemble

Ce projet Spring Boot supporte plusieurs profils pour différents environnements :

- **dev** : Base de données H2 en mémoire (développement local)
- **prod** : Base de données PostgreSQL (production)

## Fichiers de Configuration

```
src/main/resources/
├── application.properties              (configuration par défaut - H2)
├── application-dev.properties          (profil dev - H2 en mémoire)
├── application-prod.properties         (profil prod - PostgreSQL)
├── application-mysql.properties        (support MySQL optionnel)
└── application-postgres.properties     (référence PostgreSQL)
```

## Profil DEV (H2 en mémoire)

### Configuration
- **Base de données** : H2 en mémoire
- **URL** : `jdbc:h2:mem:testdb`
- **Console H2** : http://localhost:8080/h2-console
- **Logging** : DEBUG
- **Port** : 8080

### Utilisation

**Lancer avec Maven** :
```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"
```

**Lancer le JAR** :
```bash
java -jar target/spring-petclinic-4.0.0-SNAPSHOT.jar --spring.profiles.active=dev
```

**Depuis l'IDE** (Eclipse, IntelliJ) :
- Paramètres VM : `-Dspring.profiles.active=dev`
- Ou variable d'environnement : `SPRING_PROFILES_ACTIVE=dev`

### Accès H2 Console
1. Lancer l'application avec profil `dev`
2. Aller à : http://localhost:8080/h2-console
3. Connexion :
   - JDBC URL: `jdbc:h2:mem:testdb`
   - User: `sa`
   - Password: (vide)

---

## Profil PROD (PostgreSQL)

### Configuration
- **Base de données** : PostgreSQL
- **Host** : localhost (par défaut)
- **Port** : 5432
- **Database** : petclinic
- **Logging** : WARN (production mode)
- **Server Port** : 8080

### Variables d'environnement

```bash
# Configuration PostgreSQL
export DB_URL=jdbc:postgresql://localhost:5432/petclinic
export DB_USER=petclinic
export DB_PASS=petclinic
```

### Installation de PostgreSQL

#### Windows
```bash
# Avec Chocolatey
choco install postgresql

# Créer la base et l'utilisateur
psql -U postgres
CREATE DATABASE petclinic;
CREATE USER petclinic WITH PASSWORD 'petclinic';
ALTER ROLE petclinic WITH CREATEDB;
GRANT ALL PRIVILEGES ON DATABASE petclinic TO petclinic;
```

#### Linux / macOS
```bash
# Avec Homebrew (macOS)
brew install postgresql
brew services start postgresql

# Créer la base et l'utilisateur
createdb petclinic
createuser -P petclinic  # Password: petclinic
psql -d petclinic -c "ALTER USER petclinic CREATEDB;"
```

#### Docker
```bash
docker run --name petclinic-postgres \
  -e POSTGRES_DB=petclinic \
  -e POSTGRES_USER=petclinic \
  -e POSTGRES_PASSWORD=petclinic \
  -p 5432:5432 \
  -d postgres:15
```

### Utilisation

**Lancer avec Maven** :
```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=prod"
```

**Lancer le JAR** :
```bash
java -jar target/spring-petclinic-4.0.0-SNAPSHOT.jar \
  --spring.profiles.active=prod \
  --DB_URL=jdbc:postgresql://localhost:5432/petclinic \
  --DB_USER=petclinic \
  --DB_PASS=petclinic
```

**Depuis l'IDE** :
- Paramètres VM : `-Dspring.profiles.active=prod`
- Variables d'environnement : `SPRING_PROFILES_ACTIVE=prod`

---

## Build pour les deux profils

### Vérifier que le projet compile

```bash
# Build complet (inclut les deux configurations)
mvn clean package -DskipTests -Dcheckstyle.skip=true

# Résultat : target/spring-petclinic-4.0.0-SNAPSHOT.jar
```

### Tester avec chaque profil

```bash
# DEV - H2
java -jar target/spring-petclinic-4.0.0-SNAPSHOT.jar --spring.profiles.active=dev

# PROD - PostgreSQL
java -jar target/spring-petclinic-4.0.0-SNAPSHOT.jar --spring.profiles.active=prod
```

---

## Contenu des Profils

### application-dev.properties

```properties
# Développement avec H2
database=h2
spring.datasource.url=jdbc:h2:mem:testdb
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.h2.console.enabled=true
logging.level.org.springframework.samples.petclinic=DEBUG
```

### application-prod.properties

```properties
# Production avec PostgreSQL
database=postgres
spring.datasource.url=${DB_URL:jdbc:postgresql://localhost:5432/petclinic}
spring.datasource.username=${DB_USER:petclinic}
spring.datasource.password=${DB_PASS:petclinic}
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
spring.datasource.hikari.maximum-pool-size=10
logging.level.root=WARN
```

---

## Vérification de la configuration

### Pour DEV

```bash
# 1. Builder
mvn clean package -DskipTests

# 2. Lancer
java -jar target/spring-petclinic-4.0.0-SNAPSHOT.jar --spring.profiles.active=dev

# 3. Vérifier les logs
# Vous devez voir : "Started PetClinicApplication in ... seconds"
# H2 Console disponible sur http://localhost:8080/h2-console
# Application sur http://localhost:8080
```

### Pour PROD

```bash
# 1. Vérifier PostgreSQL
psql -c "SELECT version();"

# 2. Builder
mvn clean package -DskipTests

# 3. Exporter variables d'environnement
export DB_URL=jdbc:postgresql://localhost:5432/petclinic
export DB_USER=petclinic
export DB_PASS=petclinic

# 4. Lancer
java -jar target/spring-petclinic-4.0.0-SNAPSHOT.jar --spring.profiles.active=prod

# 5. Vérifier les logs
# Vous devez voir : "Started PetClinicApplication in ... seconds"
# Vous devez voir : "HikariPool-1 - Starting..."
# Application disponible sur http://localhost:8080
```

---

## Troubleshooting

### "Connection refused" en production

```bash
# Vérifier PostgreSQL tourne
psql -c "SELECT 1"

# Vérifier les variables d'environnement
echo $DB_URL
echo $DB_USER
echo $DB_PASS

# Relancer avec variables explicites
java -jar spring-petclinic-4.0.0-SNAPSHOT.jar \
  --spring.profiles.active=prod \
  --spring.datasource.url=jdbc:postgresql://localhost:5432/petclinic \
  --spring.datasource.username=petclinic \
  --spring.datasource.password=petclinic
```

### H2 Console pas accessible

```bash
# Vérifier que le profil est dev
# Vérifier que c'est dispo sur : http://localhost:8080/h2-console
# Connexion : JDBC URL = jdbc:h2:mem:testdb, User = sa, Password = (vide)
```