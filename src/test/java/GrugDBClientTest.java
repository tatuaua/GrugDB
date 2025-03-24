import org.example.grugDB.GrugDBClient;
import org.junit.jupiter.api.*;
import java.io.*;
import java.util.*;

import static org.junit.Assert.assertSame;
import static org.junit.jupiter.api.Assertions.*;

class GrugDBClientTest {

    @BeforeEach
    void setUp() {
        GrugDBClient.getInstance(true); // Clear database before each test
    }

    @Test
    void getInstance_createsSingletonInstance() {
        GrugDBClient instance1 = GrugDBClient.getInstance(false);
        GrugDBClient instance2 = GrugDBClient.getInstance(false);
        Assertions.assertSame(instance1, instance2);
    }

    @Test
    void save_persistsEntity() throws IOException {
        GrugDBClient client = GrugDBClient.getInstance(false);
        TestEntity entity = new TestEntity("test");
        client.save(entity);
        List<TestEntity> entities = client.find(TestEntity.class);
        assertEquals(1, entities.size());
        assertEquals("test", entities.getFirst().getName());
    }

    @Test
    void update_updatesMatchingEntities() throws IOException {
        GrugDBClient client = GrugDBClient.getInstance(false);
        TestEntity entity = new TestEntity("test");
        client.save(entity);
        client.update(TestEntity.class, e -> e.getName().equals("test"), e -> e.setName("updated"));
        List<TestEntity> entities = client.find(TestEntity.class);
        assertEquals(1, entities.size());
        assertEquals("updated", entities.getFirst().getName());
    }

    @Test
    void updateOrSave_updatesOrSavesEntity() throws IOException {
        GrugDBClient client = GrugDBClient.getInstance(false);
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
    void saveBatch_persistsMultipleEntities() throws IOException {
        GrugDBClient client = GrugDBClient.getInstance(false);
        List<TestEntity> entities = Arrays.asList(new TestEntity("test1"), new TestEntity("test2"));
        client.saveBatch(entities);
        List<TestEntity> retrievedEntities = client.find(TestEntity.class);
        assertEquals(2, retrievedEntities.size());
    }

    @Test
    void find_retrievesAllEntities() throws IOException {
        GrugDBClient client = GrugDBClient.getInstance(false);
        TestEntity entity1 = new TestEntity("test1");
        TestEntity entity2 = new TestEntity("test2");
        client.save(entity1);
        client.save(entity2);
        List<TestEntity> entities = client.find(TestEntity.class);
        assertEquals(2, entities.size());
    }

    @Test
    void find_withCondition_retrievesMatchingEntities() throws IOException {
        GrugDBClient client = GrugDBClient.getInstance(false);
        TestEntity entity1 = new TestEntity("test1");
        TestEntity entity2 = new TestEntity("test2");
        client.save(entity1);
        client.save(entity2);
        List<TestEntity> entities = client.find(TestEntity.class, e -> e.getName().equals("test1"));
        assertEquals(1, entities.size());
        assertEquals("test1", entities.getFirst().getName());
    }

    @Test
    void delete_removesMatchingEntities() throws IOException {
        GrugDBClient client = GrugDBClient.getInstance(false);
        TestEntity entity1 = new TestEntity("test1");
        TestEntity entity2 = new TestEntity("test2");
        client.save(entity1);
        client.save(entity2);
        client.delete(TestEntity.class, e -> e.getName().equals("test1"));
        List<TestEntity> entities = client.find(TestEntity.class);
        assertEquals(1, entities.size());
        assertEquals("test2", entities.getFirst().getName());
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
}