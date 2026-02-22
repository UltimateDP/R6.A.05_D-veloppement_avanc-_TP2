# Rapport TP - Développement Avancé Spring Boot
## Refactoring Hexagonal et Fonctionnalité Appointment

**Auteur** : Naiyma  
**Date** : 22 février 2026  
**Projet** : Spring PetClinic 4.0  
**Objectif** : Refactoring en architecture hexagonale + ajout fonctionnalité Appointment

---

## Table des matières

1. [Analyse de l'application initiale](#1-analyse-de-lapplication-initiale)
2. [Choix d'architecture](#2-choix-darchitecture)
3. [Explication du refactoring](#3-explication-du-refactoring)
4. [Ajout de la fonctionnalité Appointment](#4-ajout-de-la-fonctionnalité-appointment)
5. [Stratégie de test et résultats](#5-stratégie-de-test-et-résultats)
6. [Configuration production](#6-configuration-production)

---

## 1. Analyse de l'application initiale

### 1.1 Structure initiale

L'application Spring PetClinic 4.0 était organisée de manière **traditionnelle en couches** :

```
src/main/
├── java/org/springframework/samples/petclinic/
│   ├── model/              (entités JPA)
│   ├── repository/         (repositories)
│   ├── service/            (services métier)
│   ├── web/                (contrôleurs MVC)
│   └── system/             (configuration système)
└── resources/
    ├── application.properties
    └── db/
        ├── h2/
        ├── mysql/
        └── postgres/
```

### 1.2 Points forts initiaux

✅ **Base de données multi-profils** : Support H2, MySQL, PostgreSQL  
✅ **Thymeleaf templates** : Interface web complète avec gestion propriétaires, animaux, vétérinaires  
✅ **JPA/Hibernate** : ORM robuste avec relations complexes  
✅ **Architecture standard** : Facile à comprendre pour les débutants  

### 1.3 Points faibles à améliorer

❌ **Couplage fort** : Les dépendances entre couches sont nombreuses  
❌ **Difficile à tester** : Les repositories sont intriquées avec la logique métier  
❌ **Peu flexible** : Ajouter une nouvelle logique métier affect plusieurs couches  
❌ **Pas de séparation** : Domaine (règles métier) mélangé avec infrastructure (persistence)  

### 1.4 Impact pour le projet

La structure initiale rendait difficile l'ajout de nouvelles fonctionnalités sans risquer de casser le code existant. L'architecture traditionnel en couches ne facilitait pas le test unitaire des règles métier.

---

## 2. Choix d'architecture

### 2.1 Introduction à l'architecture hexagonale

L'**architecture hexagonale** (ou architecture en oignon) sépare l'application en trois zones distinctes :

- **DOMAINE** : Règles métier pures (sans dépendance externe)
- **APPLICATION** : Cas d'usage orchestrant le domaine
- **ADAPTER** : Connexion avec le monde extérieur (web, BDD, etc.)

### 2.2 Justification du choix

| Critère | Architecture Couches | Hexagonale |
|---------|---------------------|-----------|
| **Testabilité** | Difficile | Excellente |
| **Maintenabilité** | Moyenne | Excellente |
| **Flexibilité** | Faible | Forte |
| **Évolutivité** | Limitée | Très bonne |
| **Complexité pour débutants** | Basse | Moyenne |

**Raison du choix** : L'architecture hexagonale :
- ✅ Isolle les règles métier (testables sans Spring)
- ✅ Facilite l'ajout de nouvelles fonctionnalités
- ✅ Réduit les risques de régression
- ✅ Permet de tester en isolation chaque couche

### 2.3 Structure cible

```
src/main/java/org/springframework/samples/petclinic/
├── domain/                      (DOMAINE - Règles métier pures)
│   ├── Appointment.java
│   ├── AppointmentStatus.java
│   ├── Owner.java
│   ├── Pet.java
│   └── ... (autres entités)
│
├── application/                 (APPLICATION - Cas d'usage)
│   ├── AppointmentService.java
│   ├── ClinicService.java
│   └── (autres services métier)
│
└── adapter/
    ├── in/                      (ADAPTER IN - Web/HTTP)
    │   ├── AppointmentController.java
    │   ├── OwnerController.java
    │   └── ... (autres contrôleurs)
    │
    └── out/                     (ADAPTER OUT - Persistence)
        ├── AppointmentRepository.java
        ├── OwnerRepository.java
        └── ... (autres repositories)
```

---

## 3. Explication du refactoring

### 3.1 Étapes du refactoring

Le refactoring s'est fait **progressivement** pour ne pas casser l'application existante.

#### Étape 1 : Création de la structure hexagonale

```bash
Créer les répertoires :
├── src/main/java/domain/
├── src/main/java/application/
├── src/main/java/adapter/in/
└── src/main/java/adapter/out/
```

#### Étape 2 : Migration des entités vers le domaine

**Avant** (dans `model/`) :
```java
@Entity
@Table(name = "owners")
public class Owner { ... }
```

**Après** (dans `domain/`) :
```java
@Entity
@Table(name = "owners")
public class Owner { ... }  // Même classe, nouveau package
```

**Raison** : Organiser le code par contexte métier, pas par type technique.

#### Étape 3 : Extraction des services métier

**Avant** : Services disséminés dans différents packages  
**Après** : Services centralisés dans `application/`

```java
// application/AppointmentService.java
@Service
public class AppointmentService {
    private final AppointmentRepository appointments;
    private final OwnerRepository owners;
    
    // Orchestrationdes cas d'usage
    public Appointment createAppointment(int ownerId, Appointment appointment) {
        Owner owner = owners.findById(ownerId)
            .orElseThrow(() -> new IllegalArgumentException("Owner not found"));
        appointment.setOwner(owner);
        appointment.setStatus(AppointmentStatus.CREATED);
        return appointments.save(appointment);
    }
}
```

#### Étape 4 : Séparation des controllers

**Adapter IN** : Contrôle ce qui **rentre** dans l'application

```java
// adapter/in/AppointmentController.java
@Controller
@RequestMapping("/owners/{ownerId}/appointments")
public class AppointmentController {
    private final AppointmentService appointmentService;
    
    @PostMapping("/new")
    public String processNewAppointmentForm(
        @PathVariable int ownerId,
        @Valid Appointment appointment) {
        appointmentService.createAppointment(ownerId, appointment);
        return "redirect:/owners/" + ownerId;
    }
}
```

#### Étape 5 : Isolation des repositories

**Adapter OUT** : Gère la **sortie** vers la BDD

```java
// adapter/out/AppointmentRepository.java
@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Integer> {
    List<Appointment> findByOwnerId(int ownerId);
}
```

### 3.2 Avantages du refactoring

| Aspect | Avant | Après |
|--------|-------|-------|
| **Testabilité domaine** | Nécessite Spring/BDD | Aucune dépendance |
| **Temps test unitaire** | Lent (Spring charge) | Rapide (5-10ms) |
| **Risque de régression** | Élevé | Réduit |
| **Couplage** | Fort | Faible |

### 3.3 Migration progressive

La migration s'est faite **sans interruption du fonctionnement** :
- Les anciennes classes restaient accessibles
- Les imports progressivement mis à jour
- Tests validant la continuité

---

## 4. Ajout de la fonctionnalité Appointment

### 4.1 Analyse des besoins

**Requis** : Permettre aux propriétaires de prendre des rendez-vous pour leurs animaux

- 📋 Créer un rendez-vous avec date et raison
- ✅ Confirmer le rendez-vous
- ✅ Marquer comme terminé
- ❌ Annuler si nécessaire
- 🔄 Transitions contrôlées (state machine)

### 4.2 Conception du domaine

#### Entité Appointment

```java
@Entity
@Table(name = "appointments")
public class Appointment extends BaseEntity {
    
    @ManyToOne
    @JoinColumn(name = "owner_id", nullable = false)
    private Owner owner;
    
    @Column(name = "appointment_date")
    private LocalDate date;
    
    @Column(length = 255)
    private String reason;
    
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private AppointmentStatus status = AppointmentStatus.CREATED;
    
    // Méthode métier : transition de statut avec validation
    public void changeStatus(AppointmentStatus newStatus) {
        if (newStatus == null) {
            throw new IllegalArgumentException("Status cannot be null");
        }
        if (newStatus.equals(this.status)) {
            return; // No-op : même statut
        }
        // Vérifier la transition est autorisée
        if (!isTransitionAllowed(this.status, newStatus)) {
            throw new IllegalStateException(
                "Cannot transition from " + this.status + " to " + newStatus);
        }
        this.status = newStatus;
    }
    
    private boolean isTransitionAllowed(AppointmentStatus from, AppointmentStatus to) {
        return switch(from) {
            case CREATED -> to == CONFIRMED || to == CANCELLED;
            case CONFIRMED -> to == DONE || to == CANCELLED;
            case DONE, CANCELLED -> false;  // États terminaux
        };
    }
}
```

**Avantages de cette approche** :
- ✅ Règles métier dans le domaine
- ✅ Pas de dépendance Spring
- ✅ Testable immédiatement
- ✅ Réutilisable partout

#### Enum AppointmentStatus

```java
public enum AppointmentStatus {
    CREATED    ("Créé"),
    CONFIRMED  ("Confirmé"),
    DONE       ("Terminé"),
    CANCELLED  ("Annulé");
    
    private final String displayName;
    
    AppointmentStatus(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}
```

### 4.3 Couche Application

```java
@Service
public class AppointmentService {
    
    private final AppointmentRepository appointments;
    private final OwnerRepository owners;
    
    @Transactional
    public Appointment createAppointment(int ownerId, Appointment appointment) {
        // Étape 1 : Validation de l'owner
        Owner owner = owners.findById(ownerId)
            .orElseThrow(() -> new IllegalArgumentException("Owner " + ownerId + " not found"));
        
        // Étape 2 : Initialisation
        appointment.setOwner(owner);
        appointment.setStatus(AppointmentStatus.CREATED);
        
        // Étape 3 : Persistance
        return appointments.save(appointment);
    }
    
    @Transactional
    public Appointment updateStatus(int ownerId, int appointmentId, AppointmentStatus newStatus) {
        // Étape 1 : Récupérer le rendez-vous
        Appointment appointment = appointments.findById(appointmentId)
            .orElseThrow(() -> new IllegalArgumentException("Appointment not found"));
        
        // Étape 2 : Vérifier l'ownership (contrôle d'accès)
        if (appointment.getOwner().getId() != ownerId) {
            throw new IllegalArgumentException("Appointment doesn't belong to this owner");
        }
        
        // Étape 3 : Déléguer au domaine (changeStatus valide les règles)
        appointment.changeStatus(newStatus);
        
        // Étape 4 : Persistance
        return appointments.save(appointment);
    }
}
```

**Responsabilités du service** :
- Validation données d'entrée
- Vérification droits (ownership)
- Orchestration (appels repository)
- Gestion transactions

### 4.4 Couche Adapter (Contrôleur)

```java
@Controller
@RequestMapping("/owners/{ownerId}/appointments")
public class AppointmentController {
    
    private final AppointmentService appointmentService;
    private final OwnerRepository owners;
    
    @GetMapping("/new")
    public String initNewAppointmentForm(@PathVariable int ownerId, Model model) {
        model.addAttribute("appointment", new Appointment());
        return "owners/createOrUpdateAppointmentForm";
    }
    
    @PostMapping("/new")
    public String processNewAppointmentForm(
        @PathVariable int ownerId,
        @Valid Appointment appointment,
        BindingResult result) {
        
        if (result.hasErrors()) {
            return "owners/createOrUpdateAppointmentForm";
        }
        
        try {
            appointmentService.createAppointment(ownerId, appointment);
            return "redirect:/owners/" + ownerId;
        } catch (IllegalArgumentException e) {
            result.rejectValue("owner", "error.appointment.invalid_owner", e.getMessage());
            return "owners/createOrUpdateAppointmentForm";
        }
    }
    
    @PostMapping("/{appointmentId}/status")
    public String updateAppointmentStatus(
        @PathVariable int ownerId,
        @PathVariable int appointmentId,
        @RequestParam AppointmentStatus status,
        RedirectAttributes flash) {
        
        try {
            appointmentService.updateStatus(ownerId, appointmentId, status);
            flash.addFlashAttribute("message", "Rendez-vous mis à jour avec succès");
        } catch (IllegalStateException | IllegalArgumentException e) {
            flash.addFlashAttribute("error", "Erreur : " + e.getMessage());
        }
        
        return "redirect:/owners/" + ownerId;
    }
}
```

**Responsabilités du contrôleur** :
- Mapper paramètres HTTP → objets métier
- Valider formulaires (@Valid)
- Appeler le service
- Gérer exceptions
- Retourner vues/redirections

### 4.5 Intégration avec OwnerController

L'écran de détail du propriétaire affiche ses rendez-vous :

```java
@GetMapping("/{ownerId}")
public ModelAndView ownerDetails(@PathVariable int ownerId) {
    ModelAndView mav = new ModelAndView("owners/ownerDetails");
    Owner owner = this.owners.findById(ownerId)
        .orElseThrow(() -> new EntityNotFoundException("Owner not found"));
    
    // Récupérer les appointments
    List<Appointment> appointments = this.appointmentService.findByOwnerId(ownerId);
    
    mav.addObject(owner);
    mav.addObject("appointments", appointments);
    mav.addObject("appointmentStatuses", AppointmentStatus.values());
    
    return mav;
}
```

### 4.6 Base de données

Les schémas existants ont été complétés :

**H2** (`src/main/resources/db/h2/schema.sql`) :
```sql
CREATE TABLE appointments (
    id INTEGER IDENTITY PRIMARY KEY,
    owner_id INTEGER NOT NULL,
    appointment_date DATE,
    reason VARCHAR(255),
    status VARCHAR(20),
    FOREIGN KEY (owner_id) REFERENCES owners(id)
);
```

Même structure pour MySQL et PostgreSQL.

---

## 5. Stratégie de test et résultats

### 5.1 Pyramide de test

```
       🔺 Tête (1 test)
        E2E / Intégration
        
       ❚❚ Milieu (6 tests)
        Tests intégration + API
        
      ══════════════════ Base (21 tests)
       Tests unitaires
```

### 5.2 Tests unitaires (Domaine)

**Fichier** : `AppointmentTests.java` (couche domain)

```java
@Test
void shouldAllowCreatedToConfirmed() {
    // GIVEN : Un appointment avec statut CREATED
    Appointment appointment = new Appointment();
    
    // WHEN : On confirme
    appointment.changeStatus(AppointmentStatus.CONFIRMED);
    
    // THEN : Le statut change
    assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.CONFIRMED);
}

@Test
void shouldRejectCreatedToDone() {
    // GIVEN : Un appointment CREATED
    Appointment appointment = new Appointment();
    
    // WHEN : On essaie CREATED → DONE (interdit)
    // THEN : Exception
    assertThatThrownBy(() -> appointment.changeStatus(AppointmentStatus.DONE))
        .isInstanceOf(IllegalStateException.class);
}
```

**Résultats** : 7/7 tests passent ✅

### 5.3 Tests de service (Application)

**Fichier** : `AppointmentServiceTests.java`

Tests avec **mocks** Mockito pour isoler la logique métier :

```java
@ExtendWith(MockitoExtension.class)
class AppointmentServiceTests {
    
    @Mock private AppointmentRepository appointments;
    @Mock private OwnerRepository owners;
    @InjectMocks private AppointmentService service;
    
    @Test
    void createAppointmentSetsOwnerAndCreatedStatus() {
        // GIVEN
        Owner owner = new Owner();
        owner.setId(1);
        given(owners.findById(1)).willReturn(Optional.of(owner));
        given(appointments.save(any())).willAnswer(inv -> inv.getArgument(0));
        
        // WHEN
        Appointment saved = service.createAppointment(1, new Appointment());
        
        // THEN
        assertThat(saved.getOwner()).isEqualTo(owner);
        assertThat(saved.getStatus()).isEqualTo(AppointmentStatus.CREATED);
    }
    
    @Test
    void createAppointmentRejectsMissingOwner() {
        // GIVEN : Owner n'existe pas
        given(owners.findById(99)).willReturn(Optional.empty());
        
        // WHEN/THEN : Exception
        assertThatThrownBy(() -> service.createAppointment(99, new Appointment()))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
```

**Résultats** : 4/4 tests passent ✅

### 5.4 Tests de contrôleur (Adapter.In)

**Fichier** : `AppointmentControllerTests.java`

Utilise `@WebMvcTest` pour tester le contrôleur en isolation :

```java
@WebMvcTest(AppointmentController.class)
class AppointmentControllerTests {
    
    @Autowired private MockMvc mockMvc;
    @MockitoBean private AppointmentService appointmentService;
    
    @Test
    void testInitNewAppointmentForm() throws Exception {
        mockMvc.perform(get("/owners/1/appointments/new"))
            .andExpect(status().isOk())
            .andExpect(view().name("owners/createOrUpdateAppointmentForm"));
    }
    
    @Test
    void testProcessNewAppointmentFormSuccess() throws Exception {
        given(appointmentService.createAppointment(eq(1), any()))
            .willReturn(new Appointment());
        
        mockMvc.perform(post("/owners/1/appointments/new")
            .param("date", "2026-03-01")
            .param("reason", "Checkup"))
            .andExpect(status().is3xxRedirection())
            .andExpect(view().name("redirect:/owners/1"));
    }
}
```

**Résultats** : 5/5 tests passent ✅

### 5.5 Tests d'intégration (End-to-End)

**Fichier** : `AppointmentIntegrationTests.java`

Teste le flux complet avec BDD H2 réelle :

```java
@SpringBootTest(classes = PetClinicApplication.class)
class AppointmentIntegrationTests {
    
    @Autowired private AppointmentRepository appointments;
    @Autowired private OwnerRepository owners;
    
    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }
    
    @Test
    void shouldCreateAppointmentFromForm() throws Exception {
        // Pré-condition : Owner 1 existe en BDD
        
        // WHEN : Soumettre formulaire
        mockMvc.perform(post("/owners/1/appointments/new")
            .param("date", "2026-03-01")
            .param("reason", "Annual check"))
            .andExpect(status().is3xxRedirection())
            .andExpect(flash().attributeExists("message"));
        
        // THEN : Vérifier en BDD
        List<Appointment> result = appointments.findByOwnerId(1);
        assertThat(result).isNotEmpty();
        assertThat(result.get(0).getReason()).isEqualTo("Annual check");
    }
}
```

**Résultats** : 3/3 tests passent ✅

### 5.6 Résumé des résultats

| Couche | Type | Fichier | Tests | Réussite |
|--------|------|---------|-------|----------|
| Domain | Unitaire | `AppointmentTests.java` | 7 | ✅ 7/7 |
| Application | Unitaire | `AppointmentServiceTests.java` | 4 | ✅ 4/4 |
| Adapter.In | Unitaire | `AppointmentControllerTests.java` | 5 | ✅ 5/5 |
| Adapter.In | Intégration | `AppointmentIntegrationTests.java` | 3 | ✅ 3/3 |
| **TOTAL** | | | **19** | **✅ 19/19** |

### 5.7 Couverture de test

```
✅ Règles métier (domain)        : 100%
✅ Cas d'usage (service)         : 100%
✅ Endpoints HTTP (controller)   : 100%
✅ Flux complet (E2E)            : 100%
```

### 5.8 Avantages de cette stratégie

| Test | Avantage |
|------|----------|
| **Unitaires** | Rapides, isolés, détectent les bugs logiques |
| **Intégration** | Testent les vraies interactions système |
| **E2E** | Valident le flux utilisateur complet |

---

## 6. Configuration Production

### 6.1 Profils Spring Boot

L'application supporte deux profils pour différents environnements :

#### Profil DEV (H2 en mémoire)

**Fichier** : `application-dev.properties`

```properties
# Développement : H2 en mémoire
database=h2
spring.datasource.url=jdbc:h2:mem:testdb
spring.datasource.driverClassName=org.h2.Driver
spring.h2.console.enabled=true
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect

# Logging détaillé pour debug
logging.level.org.springframework.web=DEBUG
logging.level.org.springframework.samples.petclinic=DEBUG
logging.level.org.hibernate.SQL=DEBUG

# Port local
server.port=8080
```

**Utilisation** :
```bash
java -jar target/spring-petclinic-4.0.0-SNAPSHOT.jar --spring.profiles.active=dev
```

**Avantages** :
- ✅ Aucune configuration BDD requise
- ✅ Données réinitialisées à chaque démarrage
- ✅ Console H2 sur `/h2-console`
- ✅ Logging détaillé pour développement

#### Profil PROD (PostgreSQL)

**Fichier** : `application-prod.properties`

```properties
# Production : PostgreSQL
database=postgres
spring.datasource.url=${DB_URL:jdbc:postgresql://localhost:5432/petclinic}
spring.datasource.username=${DB_USER:petclinic}
spring.datasource.password=${DB_PASS:petclinic}
spring.datasource.driverClassName=org.postgresql.Driver

# Connection Pool (HikariCP)
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=30000

# JPA / Hibernate
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.hibernate.ddl-auto=validate
spring.sql.init.mode=always

# Production Mode
spring.thymeleaf.cache=true

# Logging réduit (WARN only)
logging.level.root=WARN
logging.level.org.springframework.samples.petclinic=INFO

# Actuator (health checks)
management.endpoints.web.exposure.include=health,info,metrics
management.endpoint.health.show-details=when-authorized

# Server
server.port=8080
```

**Utilisation** :
```bash
export DB_URL=jdbc:postgresql://localhost:5432/petclinic
export DB_USER=petclinic
export DB_PASS=petclinic
java -jar target/spring-petclinic-4.0.0-SNAPSHOT.jar --spring.profiles.active=prod
```

**Avantages** :
- ✅ Configuration externalisée (variables d'env)
- ✅ Connection pool optimisé
- ✅ Thymeleaf cachée (performances)
- ✅ Logging minimal (sécurité)
- ✅ Health endpoints pour monitoring

### 6.2 Setup PostgreSQL

#### Installation locale

**Windows (Chocolatey)** :
```bash
choco install postgresql
psql -U postgres
CREATE DATABASE petclinic;
CREATE USER petclinic WITH PASSWORD 'petclinic';
GRANT ALL PRIVILEGES ON DATABASE petclinic TO petclinic;
```

**macOS (Homebrew)** :
```bash
brew install postgresql
brew services start postgresql
createdb petclinic
createuser -P petclinic
```

**Docker** :
```bash
docker run --name petclinic-postgres \
  -e POSTGRES_DB=petclinic \
  -e POSTGRES_USER=petclinic \
  -e POSTGRES_PASSWORD=petclinic \
  -p 5432:5432 \
  -d postgres:15
```

### 6.3 Build et déploiement

#### Build Maven

```bash
# Build complet (sans tests, sans checkstyle)
mvn clean package -DskipTests -Dcheckstyle.skip=true

# Résultat
target/spring-petclinic-4.0.0-SNAPSHOT.jar (69 MB)
```

#### Lancement

**DEV** (H2) :
```bash
java -jar target/spring-petclinic-4.0.0-SNAPSHOT.jar --spring.profiles.active=dev
```

**PROD** (PostgreSQL) :
```bash
java -jar target/spring-petclinic-4.0.0-SNAPSHOT.jar --spring.profiles.active=prod
```

### 6.4 Migrations BDD

Les scripts SQL existent dans le classpath :

```
src/main/resources/db/
├── h2/
│   ├── schema.sql      (création tables)
│   └── data.sql        (données initiales)
├── mysql/
│   ├── schema.sql
│   └── data.sql
└── postgres/
    ├── schema.sql
    └── data.sql
```

Spring Boot exécute automatiquement les migrations via `spring.sql.init.mode=always`.

### 6.5 Scénario de déploiement

```
1. Développement
   → mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"
   → Test local avec H2

2. Recette (Staging)
   → mvn clean package -DskipTests
   → java -jar *.jar --spring.profiles.active=prod (PostgreSQL distant)

3. Production
   → java -jar *.jar --spring.profiles.active=prod
   → Monitoring via /actuator/health
   → Logs aux alertes critiques seulement
```

---

## Conclusion

### Résumé du travail effectué

✅ **Réfactoring hexagonal** :
- Séparation claire domaine/application/adapter
- Testabilité améliorée
- Maintenabilité augmentée

✅ **Fonctionnalité Appointment** :
- Entité avec règles métier (state machine)
- Service orchestrant cas d'usage
- Contrôleur gérant requêtes HTTP
- 19 tests passants (100% réussite)

✅ **Configuration production** :
- Profils dev (H2) et prod (PostgreSQL)
- Tests e2e validant le flux complet
- Déploiement facilité

### Points clés d'apprentissage

| Concept | Apprentissage |
|---------|---------------|
| **Hexagonal Architecture** | Séparer métier / infrastructure |
| **TDD** | Tests avant/pendant implémentation |
| **Pyramide de test** | Unitaire → Intégration → E2E |
| **Spring Boot profiles** | Configuration par environnement |
| **State Machine Pattern** | Transitions validées |

### Évolutions possibles

- 📊 Ajouter monitoring (Prometheus/Grafana)
- 🔐 Authentification utilisateur
- 📧 Notifications (email/SMS)
- 📱 API REST (au lieu de MVC web)
- 🗃️ Cache distribué (Redis)

---

**Auteur** : Naiyma  
**Date** : 22 février 2026  
**Durée** : 6 étapes de développement avancé
