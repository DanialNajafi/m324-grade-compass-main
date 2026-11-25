# Übersicht und Zuordnung der Testarten (Grade Compass)

In diesem Dokument werden alle im Projekt vorhandenen Testklassen einer konkreten **Testart** zugeordnet und anhand eindeutiger Code-Merkmale erklärt.

---

## 1. Schnelle Übersicht & Zuordnung

| Testklasse | Testart | Getestete Schicht / Komponente | Wesentliche Code-Merkmale |
|---|---|---|---|
| **`SubjectServiceTest`** | **Unit Test** | Service-Schicht (`SubjectService`) | `@ExtendWith(MockitoExtension.class)`, `@Mock`, `@InjectMocks` |
| **`GradeServiceTest`** | **Unit Test** | Service-Schicht (`GradeService`) | `@ExtendWith(MockitoExtension.class)`, `@Mock`, `@InjectMocks` |
| **`CreateSubjectRequestTest`** | **Unit Test** | DTO-Validierung (`CreateSubjectRequest`) | Kein Spring-Kontext, reines Jakarta `Validator` API, `@ParameterizedTest` |
| **`SubjectControllerTest`** | **Controller Test** (Slice Test) | Web-/REST-Schicht (`SubjectController`) | `@WebMvcTest(SubjectController.class)`, `MockMvc`, `@MockitoBean` |
| **`SubjectRepositoryTest`** | **Repository Test** (Slice Test) | Daten-/Persistenzschicht (`SubjectRepository`) | `@DataJpaTest`, `@Autowired SubjectRepository`, H2 In-Memory DB |
| **`SubjectIntegrationTest`** | **Integration Test** | Gesamter Stack (Controller $\rightarrow$ Service $\rightarrow$ DB) | `@SpringBootTest`, `@AutoConfigureMockMvc`, echte Repositories ohne Mocks |
| **`GradeCompassApplicationTests`**| **Integration Test** (Context Boot) | Spring Boot ApplicationContext | `@SpringBootTest`, `contextLoads()` (Smoke Test) |

---

## 2. Woran erkennt man die Testarten im Code?

### A. Unit Test (Komponententest / Modultest)

Ein Unit Test prüft eine einzelne Klasse oder Methode vollständig isoliert von ihrer Umgebung und externen Abhängigkeiten.

#### Typische Code-Merkmale:
1. **Kein Spring-Kontext:**
   - Es werden **keine** Spring-Test-Annotationen wie `@SpringBootTest`, `@WebMvcTest` oder `@DataJpaTest` verwendet.
   - Der Test startet in Millisekunden, da kein Dependency-Injection-Container hochgefahren wird.
2. **Verwendung von Mocks (Mockito):**
   - `@ExtendWith(MockitoExtension.class)` initialisiert Mockito für JUnit 5.
   - Abhängigkeiten werden mit `@Mock` gemockt (z. B. `@Mock private SubjectRepository repository;`).
   - Die zu testende Klasse wird mit `@InjectMocks` annotiert (z. B. `@InjectMocks private SubjectService service;`).
3. **Stubbing & Verhalten verifizieren:**
   - Methodenaufrufe von Mocks werden mit `when(...).thenReturn(...)` simuliert.
   - Interaktionen werden mit `verify(repository).save(...)` oder `verify(repository, never()).save(...)` überprüft.
4. **Validierungs-Unit-Tests (Sonderfall `CreateSubjectRequestTest`):**
   - Prüft Validierungsannotationen (`@NotBlank`, `@Size`) direkt über das Jakarta Bean Validation API (`Validation.buildDefaultValidatorFactory().getValidator()`).
   - Benötigt weder Spring noch Mocks – reine Java-Objekterzeugung und Prüfung von `ConstraintViolation`.

#### Code-Beispiel aus `SubjectServiceTest.java`:
```java
@ExtendWith(MockitoExtension.class)
class SubjectServiceTest {

    @Mock
    private SubjectRepository repository;

    @InjectMocks
    private SubjectService service;

    @Test
    void create_savesSubjectAndReturnsResponse() {
        when(repository.existsByNameIgnoreCase("Mathe")).thenReturn(false);
        when(repository.save(any(Subject.class))).thenAnswer(inv -> inv.getArgument(0));

        SubjectResponse response = service.create(new CreateSubjectRequest("Mathe"));

        assertThat(response.name()).isEqualTo("Mathe");
        verify(repository).save(any(Subject.class));
    }
}
```

---

### B. Controller Test (Web Slice Test)

Ein Controller Test prüft ausschliesslich die Präsentations- und Webschicht (REST API, Request-Mapping, JSON Serialisierung/Deserialisierung, HTTP-Statuscodes und Eingabevalidierung).

#### Typische Code-Merkmale:
1. **Slice-Annotation `@WebMvcTest`:**
   - `@WebMvcTest(SubjectController.class)` lädt **nur** Spring MVC, Filter und den spezifizierten Controller.
   - Es werden keine Services, Repositories oder Datenbankverbindungen geladen.
2. **HTTP-Simulation mit `MockMvc`:**
   - `@Autowired private MockMvc mockMvc;`
   - Anfragen werden wie echte HTTP-Requests abgeschickt (`mockMvc.perform(post("/api/subjects").content(...))`).
3. **Mocking tieferer Schichten:**
   - Abhängigkeiten wie Services werden mit `@MockitoBean` (oder `@MockBean`) in den Spring-Kontext als Mocks injiziert (`@MockitoBean private SubjectService service;`).
4. **Prüfung von HTTP-Status und JSON-Inhalten:**
   - Statusüberprüfung: `.andExpect(status().isCreated())`, `status().isBadRequest()`, `status().isConflict()`.
   - JSON-Überprüfung via JSONPath: `.andExpect(jsonPath("$.name").value("Mathe"))`.

#### Code-Beispiel aus `SubjectControllerTest.java`:
```java
@WebMvcTest(SubjectController.class)
class SubjectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SubjectService service;

    @Test
    void post_validBody_returns201AndCreatedSubject() throws Exception {
        when(service.create(any(CreateSubjectRequest.class)))
                .thenReturn(new SubjectResponse(1L, "Mathe"));

        mockMvc.perform(post("/api/subjects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Mathe\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Mathe"));

        verify(service).create(new CreateSubjectRequest("Mathe"));
    }
}
```

---

### C. Repository Test (Data JPA Slice Test)

Ein Repository Test prüft die Datenbank- und Persistenzschicht (JPA Entities, O/R-Mapping, abgeleitete Spring Data Methoden und Datenbank-Constraints).

#### Typische Code-Merkmale:
1. **Slice-Annotation `@DataJpaTest`:**
   - Konfiguriert ausschliesslich Spring Data JPA, Hibernate, Entities und Repositories.
   - Controller und Services werden **nicht** geladen.
2. **Integrierte Test-Datenbank:**
   - Verwendet typischerweise automatisch eine eingebettete In-Memory-Datenbank (z. B. H2).
3. **Echtes Repository injiziert:**
   - `@Autowired private SubjectRepository repository;` (kein Mock!).
4. **Transaktionen und Flush:**
   - Verwendet `saveAndFlush(...)`, um Daten sofort in die relationale Datenbank zu schreiben und Constraints zu erzwingen.
5. **Prüfung von SQL/JPA-Verhalten & Exceptions:**
   - Testet abgeleitete Query-Methoden (z. B. `existsByNameIgnoreCase("mathe")`).
   - Testet Datenbank-Constraints (wie `unique = true`, `nullable = false`), indem `DataIntegrityViolationException` abgefangen und verifiziert wird.

#### Code-Beispiel aus `SubjectRepositoryTest.java`:
```java
@DataJpaTest
class SubjectRepositoryTest {

    @Autowired
    private SubjectRepository repository;

    @Test
    void existsByNameIgnoreCase_ignoresCase() {
        repository.saveAndFlush(new Subject("Mathe"));

        assertThat(repository.existsByNameIgnoreCase("mathe")).isTrue();
        assertThat(repository.existsByNameIgnoreCase("MATHE")).isTrue();
    }

    @Test
    void save_duplicateName_violatesUniqueConstraint() {
        repository.saveAndFlush(new Subject("Mathe"));

        assertThatThrownBy(() -> repository.saveAndFlush(new Subject("Mathe")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
```

---

### D. Integration Test (Integrations- & E2E-Test)

Ein Integration Test prüft das Zusammenspiel mehrerer oder aller Schichten der Anwendung von der HTTP-Schnittstelle bis zur Datenbank.

#### Typische Code-Merkmale:
1. **Vollständiger Kontext mit `@SpringBootTest`:**
   - Startet den gesamten Spring `ApplicationContext` inklusive aller Beans (Controller, Services, Repositories).
2. **Keine Mocks für die Anwendungslogik:**
   - Services und Repositories sind echte Spring-Beans, nicht mit `@Mock` oder `@MockitoBean` ersetzt.
3. **Durchgängiger Datenfluss:**
   - Anfragen werden über `MockMvc` (`@AutoConfigureMockMvc`) geschickt und durchlaufen Controller, Service und Repository bis in die Datenbank.
   - Der finale Zustand wird direkt in der Datenbank via Repository verifiziert (`assertThat(repository.count()).isEqualTo(1)`).
4. **Test-Bereinigung (`cleanDatabase`):**
   - Da echte Datenbankoperationen persistiert werden, wird der Zustand vor jedem Test oft bereinigt (`@BeforeEach void cleanDatabase() { repository.deleteAll(); }`).
5. **Context-Loads Test (`GradeCompassApplicationTests`):**
   - Der elementare Integrationstest prüft, ob die Spring Boot-Konfiguration fehlerfrei instanziiert und gestartet werden kann.

#### Code-Beispiel aus `SubjectIntegrationTest.java`:
```java
@SpringBootTest
@AutoConfigureMockMvc
class SubjectIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SubjectRepository repository; // Echte Datenbank-Anbindung

    @BeforeEach
    void cleanDatabase() {
        repository.deleteAll();
    }

    @Test
    void createThenList_showsCreatedSubject() throws Exception {
        // 1. Per HTTP anlegen
        mockMvc.perform(post("/api/subjects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Mathe\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Mathe"));

        // 2. Per HTTP abrufen
        mockMvc.perform(get("/api/subjects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        // 3. Datenbank-Zustand direkt prüfen
        assertThat(repository.count()).isEqualTo(1);
    }
}
```

---

## 3. Vergleichstabelle der Merkmale

| Kriterium | Unit Test | Controller Test | Repository Test | Integration Test |
|---|---|---|---|---|
| **Klassen-Annotation** | `@ExtendWith(MockitoExtension.class)` oder keine | `@WebMvcTest(...)` | `@DataJpaTest` | `@SpringBootTest` |
| **Spring Context** | Keiner | Teilkontext (nur Web MVC) | Teilkontext (nur JPA/Hibernate) | Vollständiger Kontext |
| **Ausführungsdauer** | Extrem schnell (ms) | Schnell (< 1s) | Schnell (< 1s) | Mittel (benötigt vollen App-Start) |
| **Abhängigkeiten** | Gemockt (`@Mock`) | Gemockt (`@MockitoBean`) | Echte DB (H2) | Echte Komponenten & DB |
| **HTTP-Aufruf** | Nein (Methodenaufruf) | Ja (`MockMvc`) | Nein | Ja (`MockMvc` oder `TestRestTemplate`) |
| **Datenbank** | Keine | Keine | Ja (In-Memory H2) | Ja (In-Memory H2 / Test-DB) |

---

## 4. Exkurs: Frontend-Tests (`frontend/src/app/`)

Zur Vollständigkeit enthält das Frontend ebenfalls Tests (ausgeführt mit Vitest/Angular Testing):
- `grade-average.spec.ts`: **Unit Test** (testet reine mathematische Hilfsfunktionen wie Durchschnittsberechnungen ohne DOM/Framework).
- `grade.service.spec.ts`: **Unit Test** (testet den Angular HTTP-Service isoliert mit `HttpTestingController`).
- `app.spec.ts` & `dashboard.spec.ts`: **Komponenten-Unit-Tests** (testen Angular-Komponenten und Template-Rendering via `TestBed`).
