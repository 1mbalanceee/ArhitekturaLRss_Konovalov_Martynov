package ru.ssau.jewelry;

import jakarta.persistence.*;
import jakarta.validation.ConstraintViolationException;
import java.math.BigDecimal;
import java.nio.file.Path;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.utility.MountableFile;
import ru.ssau.jewelry.dto.*;
import ru.ssau.jewelry.exception.BusinessException;
import ru.ssau.jewelry.model.*;
import ru.ssau.jewelry.service.*;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static ru.ssau.jewelry.TestData.*;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class JewelryPostgresIT {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17.6")
            .withEnv("POSTGRES_INITDB_ARGS", "--encoding=UTF8 --locale=C.UTF-8")
            .withCopyFileToContainer(MountableFile.forHostPath(Path.of("sql/01-schema.sql").toAbsolutePath()),
                    "/docker-entrypoint-initdb.d/01-schema.sql")
            .withCopyFileToContainer(MountableFile.forHostPath(Path.of("sql/02-seed.sql").toAbsolutePath()),
                    "/docker-entrypoint-initdb.d/02-seed.sql");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired CategoryService categories;
    @Autowired ProductService products;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManagerFactory emf;
    @Autowired MockMvc mvc;

    @BeforeEach
    void clearOwnTestDatabase() {
        jdbc.update("delete from products");
        jdbc.update("delete from categories");
    }

    @Test
    void fullCrudUsesHibernateSchemaValidationAndExactDecimals() {
        Long categoryId = createCategory("Тестовая");
        CategoryForm category = categories.findForm(categoryId);
        category.setName("Изменённая");
        categories.save(category);
        assertEquals(1L, categories.findForm(categoryId).getVersion());
        products.save(productForm(null, 0, categoryId));
        Long productId = products.findAll().getFirst().id();
        ProductForm product = products.findForm(productId);
        assertEquals(new BigDecimal("9876543210.12"), product.getPrice());
        assertEquals(new BigDecimal("0.123"), product.getWeightGrams());
        assertThrows(BusinessException.class, () -> categories.delete(categoryId, 1));
        product.setPrice(new BigDecimal("0.01"));
        products.save(product);
        assertEquals(new BigDecimal("0.01"), products.findForm(productId).getPrice());
        assertThrows(BusinessException.class, () -> products.save(product));
        assertThrows(BusinessException.class, () -> products.delete(productId, 0));
        products.delete(productId, 1);
        categories.delete(categoryId, 1);
        assertTrue(products.findAll().isEmpty());
        assertTrue(categories.findAll().isEmpty());
    }

    @Test
    void duplicateCategoryCheckUsesPostgresCaseFolding() {
        createCategory("Кольца");
        CategoryForm duplicate = categoryForm(null, 0);
        duplicate.setName("  кольЦА  ");
        assertThrows(BusinessException.class, () -> categories.save(duplicate));
        assertThrows(DataIntegrityViolationException.class,
                () -> jdbc.update("insert into categories(name) values (?)", "КОЛЬЦА"));
        assertEquals(1, categories.findAll().size());
    }

    @Test
    void normalizedSkuIsUniqueInServiceAndDatabase() {
        Long category = createCategory("Изделия");
        products.save(productForm(null, 0, category));
        ProductForm duplicate = productForm(null, 0, category);
        duplicate.setSku(" ring-001 ");
        assertThrows(BusinessException.class, () -> products.save(duplicate));
        assertThrows(DataIntegrityViolationException.class, () -> insertProduct(category));
    }

    @Test
    void serviceBeanValidationProtectsNonWebCalls() {
        Long category = createCategory("Изделия");
        ProductForm invalid = productForm(null, 0, category);
        invalid.setPrice(new BigDecimal("-1"));
        assertThrows(ConstraintViolationException.class, () -> products.save(invalid));
        invalid.setPrice(new BigDecimal("1.234"));
        assertThrows(ConstraintViolationException.class, () -> products.save(invalid));
        assertTrue(products.findAll().isEmpty());
    }

    @ParameterizedTest
    @CsvSource({
        "price, -1", "price, 0", "weight_grams, -1", "weight_grams, 0",
        "stock, -1", "fineness, 0", "fineness, 1000",
        "sku, '''lowercase'''", "name, '''   '''", "metal, '''   '''",
        "category_id, 9999999", "category_id, null", "version, null",
        "weight_grams, 10000000", "price, 10000000000"
    })
    void realCheckNotNullFkAndPrecisionConstraintsRejectInvalidValues(String column, String expression) {
        Long category = createCategory("Изделия");
        Long productId = insertProduct(category);
        assertThrows(DataIntegrityViolationException.class,
                () -> jdbc.update("update products set " + column + " = " + expression + " where id = ?", productId));
    }

    @Test
    void databaseProtectsRequiredCategoryAndRestrictDeleteEvenWhenServiceIsBypassed() {
        Long category = createCategory("Изделия");
        insertProduct(category);
        assertThrows(DataIntegrityViolationException.class,
                () -> jdbc.update("delete from categories where id = ?", category));
        assertThrows(DataIntegrityViolationException.class,
                () -> jdbc.update("insert into categories(name) values ('  ')"));
        BadSqlGrammarException identityError = assertThrows(BadSqlGrammarException.class,
                () -> jdbc.update("insert into categories(id, name) values (99999, 'Явный ID')"));
        assertEquals("428C9", identityError.getSQLException().getSQLState());
    }

    @Test
    void competingHibernateUpdatesAndDeletesCannotOverwriteCommittedVersion() {
        Long category = createCategory("Исходная");
        EntityManager first = emf.createEntityManager();
        EntityManager second = emf.createEntityManager();
        try {
            first.getTransaction().begin();
            second.getTransaction().begin();
            Category old = first.find(Category.class, category);
            Category current = second.find(Category.class, category);
            current.setName("Сохранённая");
            second.getTransaction().commit();
            old.setName("Устаревшая");
            assertThrows(RollbackException.class, () -> first.getTransaction().commit());
            assertEquals("Сохранённая", categories.findForm(category).getName());
        } finally {
            if (first.getTransaction().isActive()) first.getTransaction().rollback();
            if (second.getTransaction().isActive()) second.getTransaction().rollback();
            first.close();
            second.close();
        }

        EntityManager deleting = emf.createEntityManager();
        try {
            deleting.getTransaction().begin();
            Category stale = deleting.find(Category.class, category);
            CategoryForm changed = categories.findForm(category);
            changed.setName("Ещё новее");
            categories.save(changed);
            deleting.remove(stale);
            assertThrows(RollbackException.class, () -> deleting.getTransaction().commit());
            assertEquals("Ещё новее", categories.findForm(category).getName());
        } finally {
            if (deleting.getTransaction().isActive()) deleting.getTransaction().rollback();
            deleting.close();
        }
    }

    @Test
    void optimisticReadAlsoProtectsNoOpEditAtCommit() {
        Long category = createCategory("Исходная");
        EntityManager reading = emf.createEntityManager();
        try {
            reading.getTransaction().begin();
            reading.find(Category.class, category, LockModeType.OPTIMISTIC);
            CategoryForm changed = categories.findForm(category);
            changed.setName("Изменена параллельно");
            categories.save(changed);
            assertThrows(RollbackException.class, () -> reading.getTransaction().commit());
        } finally {
            if (reading.getTransaction().isActive()) reading.getTransaction().rollback();
            reading.close();
        }
    }

    @Test
    void renderedMvcCrudRejectsStaleFormsAndDisplaysErrors() throws Exception {
        mvc.perform(post("/categories/save").param("version", "0").param("name", "Веб-категория"))
                .andExpect(redirectedUrl("/categories"));
        Long category = categories.findAll().getFirst().id();
        mvc.perform(post("/products/save").param("version", "0")
                        .param("sku", " web-1 ").param("name", "Веб-изделие").param("metal", "Золото")
                        .param("fineness", "585").param("weightGrams", "0,123").param("price", "123.45")
                        .param("stock", "0").param("categoryId", category.toString()))
                .andExpect(redirectedUrl("/products"));
        Long productId = products.findAll().getFirst().id();
        mvc.perform(get("/products")).andExpect(status().isOk())
                .andExpect(content().string(containsString("WEB-1")))
                .andExpect(content().string(containsString("0,123")))
                .andExpect(content().string(containsString("123,45")))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));
        mvc.perform(get("/products/" + productId + "/edit")).andExpect(status().isOk());
        ProductForm changed = products.findForm(productId);
        changed.setName("Изменено");
        products.save(changed);
        mvc.perform(post("/products/" + productId + "/delete").param("version", "0"))
                .andExpect(flash().attribute("error", containsString("другой вкладке")));
        assertEquals("Изменено", products.findForm(productId).getName());
        mvc.perform(post("/products/" + productId + "/delete").param("version", "1"))
                .andExpect(flash().attribute("success", "Изделие удалено"));
        mvc.perform(post("/categories/" + category + "/delete").param("version", "0"))
                .andExpect(flash().attribute("success", "Категория удалена"));
    }

    @Test
    void missingRecordAndMissingCategoryRemainBusinessErrors() {
        assertThrows(BusinessException.class, () -> categories.findForm(Long.MAX_VALUE));
        assertThrows(BusinessException.class, () -> products.findForm(Long.MAX_VALUE));
        assertThrows(BusinessException.class, () -> products.save(productForm(null, 0, Long.MAX_VALUE)));
        assertThrows(BusinessException.class, () -> categories.delete(Long.MAX_VALUE, 0));
        assertThrows(BusinessException.class, () -> products.delete(Long.MAX_VALUE, 0));
    }

    private Long createCategory(String name) {
        CategoryForm form = categoryForm(null, 0);
        form.setName(name);
        categories.save(form);
        return jdbc.queryForObject("select id from categories where name = ?", Long.class, name);
    }

    private Long insertProduct(Long category) {
        return jdbc.queryForObject("""
                insert into products(sku, name, metal, fineness, weight_grams, price, stock, category_id)
                values ('RING-001', 'Кольцо', 'Золото', 585, 0.123, 123.45, 0, ?) returning id
                """, Long.class, category);
    }
}
