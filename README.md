# TP 5 — Intégration de Spring et Hibernate dans une Architecture Microservices

Projet Maven `microservice-product` réalisé dans le cadre du TP 5 du cours
**Architecture Microservices : Conception, Déploiement et Orchestration**
(MLIAEdu, cours 7, lab 154).

L'objectif est de construire un microservice de gestion de produits en combinant :

- **Spring Framework 5.3.20** : conteneur IoC, injection de dépendances, configuration Java ;
- **Hibernate 5.6.9.Final (JPA)** : mapping objet-relationnel et `SessionFactory` ;
- **le pattern DAO générique `<T>`** : un seul contrat CRUD réutilisable pour toutes les entités ;
- **la gestion déclarative des transactions** avec `@Transactional` ;
- **MySQL / MariaDB** comme base de données.

---

## 1. Environnement d'exécution utilisé

| Élément | Version / valeur |
|---|---|
| JDK | 17.0.18 (`C:\Program Files\Java\jdk-17.0.18`) |
| Maven | 3.x (Maven embarqué d'IntelliJ IDEA 2025.3.2) |
| Serveur de base | MariaDB 10.4.32 (XAMPP) — port 3306 |
| Base de données | `product_db` (créée automatiquement au premier lancement) |
| Utilisateur | `root` / mot de passe vide |
| Spring | 5.3.20 |
| Hibernate | 5.6.9.Final |
| Pilote JDBC | `mysql-connector-java` 8.0.33 |
| Tests | JUnit 5.9.2 + Spring Test 5.3.20 |

Prérequis de l'énoncé : JDK 11 ou supérieur, Maven, un IDE, un SGBD relationnel (MySQL ou PostgreSQL).

---

## 2. Structure du projet

```
lab5Microservice/
├── pom.xml
├── README.md
├── docs/
│   ├── execution-main.log            # sortie réelle : tests + MainApp
│   └── execution-testhibernate.log   # sortie réelle : TestHibernate
└── src/
    ├── main/
    │   ├── java/com/microservice/product/
    │   │   ├── MainApp.java                    # étape 12 : scénario complet
    │   │   ├── TestHibernate.java              # étape 11 : vérification du contexte
    │   │   ├── config/
    │   │   │   ├── AppConfig.java              # étape 10 : component scan
    │   │   │   ├── HibernateConfig.java        # étape 7 : DataSource + SessionFactory
    │   │   │   └── TransactionConfig.java      # étape 8 : gestionnaire de transactions
    │   │   ├── dao/
    │   │   │   ├── IDao.java                   # étape 5 : CRUD générique <T>
    │   │   │   ├── IProductDao.java            # HQL spécifiques au produit
    │   │   │   └── ProductDaoImpl.java         # étape 6 : implémentation Hibernate
    │   │   ├── entity/
    │   │   │   └── Product.java                # étape 4 : entité JPA
    │   │   └── service/
    │   │       ├── ProductService.java         # étape 9 : contrat métier
    │   │       └── ProductServiceImpl.java     # étape 9 : logique métier + @Transactional
    │   └── resources/
    │       └── application.properties          # étape 3 : configuration JDBC / Hibernate
    └── test/java/com/microservice/product/
        ├── ProductServiceTest.java             # 10 tests métier (rollback automatique)
        └── SpringContextTest.java              # 5 tests de configuration
```

---

## 3. Déroulé du TP

### Étape 2 — Projet Maven (`pom.xml`)

Dépendances principales : `spring-context`, `spring-orm` (qui apporte `spring-tx` et
`spring-jdbc`), `hibernate-core` et `mysql-connector-java`. En plus de l'énoncé, on ajoute
`spring-test` et `junit-jupiter-engine` pour pouvoir écrire des tests JUnit 5 réellement
exécutés par Surefire. Le plugin `exec-maven-plugin` déclare
`com.microservice.product.MainApp` comme classe principale.

### Étape 3 — Configuration de la base (`application.properties`)

```properties
jdbc.driver=com.mysql.cj.jdbc.Driver
jdbc.url=jdbc:mysql://localhost:3306/product_db?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
jdbc.username=root
jdbc.password=

hibernate.dialect=org.hibernate.dialect.MySQL8Dialect
hibernate.show_sql=true
hibernate.format_sql=true
hibernate.hbm2ddl.auto=update
```

Le fichier centralise les paramètres de connexion : un déploiement sur un autre
environnement ne nécessite que la modification de ce fichier (voire d'un profil externe).

### Étape 4 — Entité `Product`

```java
@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private double price;

    public Product() { }

    public Product(String name, double price) {
        this.name = name;
        this.price = price;
    }
    // getters / setters / toString
}
```

Hibernate génère alors le DDL suivant (extrait réel de `docs/execution-main.log`) :

```sql
create table products (
   id bigint not null auto_increment,
    name varchar(255) not null,
    price double precision not null,
    primary key (id)
) engine=InnoDB
```

### Étape 5 — Interface DAO générique

```java
public interface IDao<T> {
    void save(T entity);
    void update(T entity);
    void delete(T entity);
    T findById(Long id);
    List<T> findAll();
}
```

Le paramètre de type `<T>` garantit la sécurité des types à la compilation : `IDao<Product>`
ne peut recevoir que des `Product`. Le même contrat sera réutilisé tel quel pour toute nouvelle
entité du microservice (`Order`, `Customer`, …).

### Étape 6 — Implémentation du DAO

```java
@Repository
public class ProductDaoImpl implements IProductDao {

    @Autowired
    private SessionFactory sessionFactory;

    @Override
    public void save(Product product) {
        sessionFactory.getCurrentSession().save(product);
    }
    ...
    @Override
    public List<Product> findAll() {
        return sessionFactory.getCurrentSession()
                .createQuery("FROM Product p ORDER BY p.id", Product.class)
                .list();
    }
}
```

Point clé : le DAO n'ouvre **jamais** de session ni ne gère de transaction. Il utilise
`getCurrentSession()`, c'est-à-dire la session liée à la transaction Spring en cours.

`IProductDao` étend `IDao<Product>` et ajoute deux requêtes HQL :

```java
List<Product> findByName(String name);   // ... WHERE lower(p.name) = lower(:name)
long countAll();                         // select count(p) from Product p
```

### Étape 7 — Configuration Spring + Hibernate (`HibernateConfig`)

`DriverManagerDataSource` est alimenté par `Environment` (donc par `application.properties`),
puis `LocalSessionFactoryBean` construit la `SessionFactory` avec
`setPackagesToScan("com.microservice.product.entity")` : les entités JPA sont découvertes
automatiquement, aucune liste de mappings n'est à maintenir.

```java
@Bean
public DataSource dataSource() { ... }

@Bean
public LocalSessionFactoryBean sessionFactory() {
    sessionFactory.setDataSource(dataSource());
    sessionFactory.setPackagesToScan("com.microservice.product.entity");
    sessionFactory.setHibernateProperties(props); // dialect, show_sql, hbm2ddl
    return sessionFactory;
}
```

### Étape 8 — Transactions (`TransactionConfig`)

```java
@Configuration
@EnableTransactionManagement
public class TransactionConfig {

    @Bean
    public HibernateTransactionManager transactionManager(SessionFactory sessionFactory) {
        HibernateTransactionManager txManager = new HibernateTransactionManager();
        txManager.setSessionFactory(sessionFactory);
        return txManager;
    }
}
```

`@EnableTransactionManagement` active le proxy AOP : un appel à une méthode `@Transactional`
ouvre une transaction, la valide en cas de succès et la **annule (rollback)** si une
`RuntimeException` est levée.

### Étape 9 — Couche service

```java
@Service
public class ProductServiceImpl implements ProductService {

    private final IProductDao productDao;

    @Autowired
    public ProductServiceImpl(IProductDao productDao) { this.productDao = productDao; }

    @Override
    @Transactional
    public void addProduct(Product product) {
        validate(product);          // règle métier
        productDao.save(product);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Product> getAllProducts() { return productDao.findAll(); }
    ...
}
```

La couche service isole les règles métier : un prix négatif, un nom vide ou une mise à jour
sans identifiant lèvent une `IllegalArgumentException` **avant** d'atteindre la base, ce qui
provoque le rollback de la transaction.

### Étape 10 — Contexte Spring (`AppConfig`)

```java
@Configuration
@ComponentScan(basePackages = "com.microservice.product")
@Import({HibernateConfig.class, TransactionConfig.class})
public class AppConfig { }
```

Le component scan détecte `@Repository`, `@Service` et les classes `@Configuration` ; les
`@Import` rendent le câblage de la persistance explicite.

### Étape 11 — Vérification de la configuration (`TestHibernate`)

Sortie réelle (`docs/execution-testhibernate.log`) :

```
Contexte Spring initialise avec succes !
Nombre de beans declares : 19
SessionFactory initialisee : true
Dialecte Hibernate           : org.hibernate.dialect.MySQL8Dialect
Connexion JDBC operationnelle : MySQL 5.5.5-10.4.32-MariaDB
Produits presents en base    : 1
```

### Étape 12 — Scénario d'utilisation complet (`MainApp`)

Sortie réelle (extrait de `docs/execution-main.log`) :

```
Hibernate: 
    insert into products (name, price) values (?, ?)
Produit ajoute avec succes ! Identifiant genere : 9
Total en base : 1
--- Liste des produits ---
Product{id=9, name='Microservice Gateway', price=149.99}
Prix mis a jour : 199.99
--- Test du rollback transactionnel ---
Produit refuse par la couche service : Le prix ne peut pas etre negatif
Total en base apres le refus : 1
```

Le dernier bloc montre la transaction annulée : le produit invalide n'a jamais été inséré.

### Étape 13 — Résumé

| Concept | Mise en œuvre |
|---|---|
| Conteneur IoC | `AnnotationConfigApplicationContext` + `@Configuration` / `@Bean` |
| Injection de dépendances | constructeur (`ProductServiceImpl`), champ (`ProductDaoImpl`) |
| DAO générique | `IDao<T>` → `IProductDao` → `ProductDaoImpl` |
| ORM | entité `Product`, `SessionFactory`, `hbm2ddl.auto=update` |
| Transactions déclaratives | `@EnableTransactionManagement` + `HibernateTransactionManager` + `@Transactional` |
| Séparation des couches | `entity` → `dao` → `service` → `MainApp` |

---

## 4. Tests automatisés

`mvn clean test` exécute **15 tests**, tous verts :

```
Tests run: 10, Failures: 0, Errors: 0, Skipped: 0 -- ProductServiceTest
Tests run: 5,  Failures: 0, Errors: 0, Skipped: 0 -- SpringContextTest
Tests run: 15, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

`ProductServiceTest` (`@ExtendWith(SpringExtension.class)`, `@ContextConfiguration(classes = AppConfig.class)`,
`@Transactional`) — chaque test est annulé à la fin, la base reste donc propre :

| # | Test | Vérifie |
|---|---|---|
| 1 | `addProductEnregistreLeProduit` | insertion réelle + identifiant généré |
| 2 | `addProductRefuseUnPrixNegatif` | règle métier + rollback |
| 3 | `addProductRefuseUnNomVide` | validation du nom |
| 4 | `updateProductMetAJourLePrix` | `session.update()` |
| 5 | `getAllProductsRetourneLesProduits` | HQL `FROM Product` |
| 6 | `deleteProductSupprimeLeProduit` | `session.delete()` |
| 7 | `getProductsByNameRetrouveLeProduit` | HQL paramétré `:name` (insensible à la casse) |
| 8 | `getProductsByNameRetourneListeVide` | nom inconnu |
| 9 | `getProductRetourneNullSiInconnu` | `session.get()` sur id absent |
| 10 | `countProductsCompteLesProduits` | `select count(p)` |

`SpringContextTest` — vérifie le démarrage du conteneur :

| # | Test | Vérifie |
|---|---|---|
| 11 | `leConteneurExposeLesBeansAttendus` | DAO, service, `dataSource`, `sessionFactory`, `transactionManager` |
| 12 | `laSessionFactoryEstConfiguree` | dialecte `MySQL8Dialect`, `EntityManagerFactory` |
| 13 | `leGestionnaireDeTransactionsEstLie` | `HibernateTransactionManager` ↔ `SessionFactory` |
| 14 | `laConnexionJdbcEstOperationnelle` | connexion valide sur `product_db` |
| 15 | `laTableProductsEstCreee` | table créée par `hbm2ddl.auto=update` |

---

## 5. Exécution

```bash
# 1. Compilation + tests
mvn clean test

# 2. Étape 12 : scénario complet (crée la base product_db si nécessaire)
mvn exec:java

# 3. Étape 11 : vérification de la configuration Spring/Hibernate
mvn -q dependency:build-classpath -Dmdep.outputFile=cp.txt
java -cp "target/classes;$(cat cp.txt)" com.microservice.product.TestHibernate   # Windows : %cd%\cp.txt
```

`mvn exec:java` lance `MainApp` car la classe principale est fixée dans le `pom.xml` ; le
plugin ne permet pas de la remplacer par `-Dexec.mainClass` quand cette configuration est
présente, d'où l'appel direct à `java -cp` pour `TestHibernate`.

Résultat en base après exécution :

```
mysql> select * from products;
+----+----------------------+--------+
| id | name                 | price  |
+----+----------------------+--------+
|  9 | Microservice Gateway | 199.99 |
+----+----------------------+--------+
```

---

## 6. Écarts et corrections par rapport à l'énoncé

| # | Énoncé | Réalisation | Raison |
|---|---|---|---|
| 1 | `import org.SessionFactory;` (étape 8) | `import org.hibernate.SessionFactory;` | faute de frappe : le package est `org.hibernate` |
| 2 | `AppConfig` limité au `@ComponentScan` | ajout de `@Import({HibernateConfig, TransactionConfig})` | garantit l'enregistrement du `SessionFactory` et du `transactionManager` |
| 3 | service = classe `ProductServiceImpl` seule | interface `ProductService` + implémentation | l'énoncé évoque l'interface sans la fournir ; contrat explicite |
| 4 | `@Autowired` sur champ dans le service | injection par constructeur | immuabilité et testabilité |
| 5 | `jdbc.password=secret` | `jdbc.password=` (vide) | le `root` local (XAMPP) n'a pas de mot de passe |
| 6 | URL `jdbc:mysql://localhost:3306/product_db?useSSL=false&serverTimezone=UTC` | ajout de `createDatabaseIfNotExist=true` et `allowPublicKeyRetrieval=true` | permet un premier lancement sans `CREATE DATABASE` manuel |
| 7 | dialecte `MySQL8Dialect` sur un serveur MariaDB 10.4 | conservé tel quel | testé avec succès (`Using dialect: org.hibernate.dialect.MySQL8Dialect`) |
| 8 | `TestHibernate` affiche seulement « Contexte initialisé » | affiche aussi bean count, dialecte, version du serveur, nombre de produits | preuve réelle du bon démarrage (étape 11) |
| 9 | seulement `addProduct` / `getAllProducts` | ajout de `update`, `delete`, `getById`, `findByName`, `count` | couverture complète du CRUD du DAO générique |
| 10 | pas de tests | 15 tests JUnit 5 + Spring Test | vérification automatisée du TP |
| 11 | — | `AbandonedConnectionCleanupThread.checkedShutdown()` en fin de `main` | sans cela `mvn exec:java` attend 15 s le thread de nettoyage du pilote MySQL |

---

## 7. Conclusion

Ce TP montre comment un microservice Java combine trois niveaux de responsabilité :

1. **Spring** fournit le conteneur et l'injection de dépendances : le service ne connaît que
   l'interface `IProductDao`, jamais l'implémentation ;
2. **Hibernate** assure le mapping et la persistance via une `SessionFactory` unique gérée
   par le conteneur ;
3. **`@Transactional`** remplace la gestion manuelle `beginTransaction/commit/rollback` du
   TP 4 : la transaction est ouverte et fermée par le proxy Spring, ce qui supprime le risque
   de session fermée pendant un rollback.

Le pattern DAO générique `IDao<T>` rend le CRUD réutilisable pour n'importe quelle entité du
microservice. La suite logique (donnée en conclusion de l'énoncé) est d'exposer cette couche
service via des contrôleurs REST — objets des TP 6 et suivants.

---

**Auteur :** Yasmine Ait Halibi — [github.com/aithalibi](https://github.com/aithalibi)
