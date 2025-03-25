import org.example.grugDB.GrugDBClient;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class GrugDBClientTest {

    private static final Logger log = LoggerFactory.getLogger(GrugDBClientTest.class);

    @BeforeEach
    void clearDatabase() {
        GrugDBClient.clearDatabaseDirectory();
    }

    @Nested
    class SingletonTests {
        @Test
        void getInstance_createsSingletonInstance() {
            GrugDBClient instance1 = GrugDBClient.getInstance();
            GrugDBClient instance2 = GrugDBClient.getInstance();
            Assertions.assertSame(instance1, instance2);
        }
    }

    @Nested
    class BasicCrudTests {
        @Test
        void save_persistsEntity() throws IOException {
            GrugDBClient client = GrugDBClient.getInstance();
            TestEntity entity = new TestEntity("test");
            client.save(entity);
            List<TestEntity> entities = client.find(TestEntity.class);
            assertEquals(1, entities.size());
            assertEquals("test", entities.getFirst().getName());
        }

        @Test
        void update_updatesMatchingEntities() throws IOException {
            GrugDBClient client = GrugDBClient.getInstance();
            TestEntity entity = new TestEntity("test");
            client.save(entity);
            client.update(TestEntity.class, e -> e.getName().equals("test"), e -> e.setName("updated"));
            List<TestEntity> entities = client.find(TestEntity.class);
            assertEquals(1, entities.size());
            assertEquals("updated", entities.getFirst().getName());
        }

        @Test
        void updateOrSave_updatesOrSavesEntity() throws IOException {
            GrugDBClient client = GrugDBClient.getInstance();
            TestEntity entity = new TestEntity("test");
            client.updateOrSave(entity, e -> e.getName().equals("test"), e -> e.setName("updated"));
            List<TestEntity> entities = client.find(TestEntity.class);
            assertEquals(1, entities.size());
            assertEquals("test", entities.getFirst().getName());

            client.updateOrSave(entity, e -> e.getName().equals("test"), e -> e.setName("updated"));
            entities = client.find(TestEntity.class);
            assertEquals(1, entities.size());
            assertEquals("updated", entities.getFirst().getName());
        }

        @Test
        void delete_removesMatchingEntities() throws IOException {
            GrugDBClient client = GrugDBClient.getInstance();
            TestEntity entity1 = new TestEntity("test1");
            TestEntity entity2 = new TestEntity("test2");
            client.save(entity1);
            client.save(entity2);
            client.delete(TestEntity.class, e -> e.getName().equals("test1"));
            List<TestEntity> entities = client.find(TestEntity.class);
            assertEquals(1, entities.size());
            assertEquals("test2", entities.getFirst().getName());
        }

        @Test
        void save_savesNestedEntity() throws IOException {
            GrugDBClient client = GrugDBClient.getInstance();
            NestedTestEntity nestedEntity = new NestedTestEntity(new TestEntity("test"));
            client.save(nestedEntity);
            List<NestedTestEntity> entities = client.find(NestedTestEntity.class);
            assertEquals(1, entities.size());
        }
    }

    @Nested
    class BatchOperationsTests {
        @Test
        void saveBatch_persistsMultipleEntities() throws IOException {
            GrugDBClient client = GrugDBClient.getInstance();
            List<TestEntity> entities = Arrays.asList(new TestEntity("test1"), new TestEntity("test2"));
            client.saveBatch(entities);
            List<TestEntity> retrievedEntities = client.find(TestEntity.class);
            assertEquals(2, retrievedEntities.size());
        }
    }

    @Nested
    class QueryTests {
        @Test
        void find_retrievesAllEntities() throws IOException {
            GrugDBClient client = GrugDBClient.getInstance();
            TestEntity entity1 = new TestEntity("test1");
            TestEntity entity2 = new TestEntity("test2");
            client.save(entity1);
            client.save(entity2);
            List<TestEntity> entities = client.find(TestEntity.class);
            assertEquals(2, entities.size());
        }

        @Test
        void find_withCondition_retrievesMatchingEntities() throws IOException {
            GrugDBClient client = GrugDBClient.getInstance();
            TestEntity entity1 = new TestEntity("test1");
            TestEntity entity2 = new TestEntity("test2");
            client.save(entity1);
            client.save(entity2);
            List<TestEntity> entities = client.find(TestEntity.class, e -> e.getName().equals("test1"));
            assertEquals(1, entities.size());
            assertEquals("test1", entities.getFirst().getName());
        }
    }

    @Nested
    class PerformanceTests {
        @Test
        void profiling() throws IOException {
            int entityAmount = 1000;
            GrugDBClient client = GrugDBClient.getInstance();

            long start = System.currentTimeMillis();
            client.saveBatch(mockEntities(entityAmount));
            long end = System.currentTimeMillis();
            log.info("Batch save took {} ms", end-start);

            start = System.currentTimeMillis();
            client.find(TestEntity.class, e -> e.getName().equals("999999"));
            end = System.currentTimeMillis();
            log.info("Find took {} ms", end-start);
        }
    }

    static class TestEntity implements Serializable {
        private String name;

        public TestEntity(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    static class NestedTestEntity implements Serializable {
        private TestEntity entity;

        public NestedTestEntity(TestEntity entity) {
            this.entity = entity;
        }

        public TestEntity getEntity() {
            return entity;
        }

        public void setEntity(TestEntity entity) {
            this.entity = entity;
        }
    }

    static List<TestEntity> mockEntities(int amount) {
        return Stream.iterate(amount, n -> n > 0, n -> n - 1)
                .map(String::valueOf)
                .map(TestEntity::new)
                .collect(Collectors.toList());
    }
}