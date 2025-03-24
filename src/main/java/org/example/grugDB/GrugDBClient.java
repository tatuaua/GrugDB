package org.example.grugDB;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class GrugDBClient {

    private static GrugDBClient singletonInstance = null;
    private static final File databaseDirectory = new File("grug_db");
    private static final Logger logger = LoggerFactory.getLogger(GrugDBClient.class);

    private GrugDBClient() {
        logger.info("GrugDBClient instance created");
    }

    /**
     * Retrieves or creates a singleton instance of GrugDBClient.
     *
     * @param shouldClearDatabase If true, clears the existing database directory before initialization.
     * @return The singleton instance of GrugDBClient.
     */
    public static GrugDBClient getInstance(boolean shouldClearDatabase) {
        if(databaseDirectory.exists()) {
            if(shouldClearDatabase) { clearDatabaseDirectory(); }
        } else {
            if (!databaseDirectory.mkdirs()) { logger.error("Failed to create database directory"); }
        }

        return singletonInstance == null ? singletonInstance = new GrugDBClient() : singletonInstance;
    }

    private static void clearDatabaseDirectory() {
        for (File storedFile : Objects.requireNonNull(databaseDirectory.listFiles())) {
            if (!storedFile.delete()) { logger.error("Failed to delete database file {}", storedFile.getName()); }
        }
    }

    /**
     * Persists a single entity to the Grug database by appending it to the appropriate collection.
     *
     * @param entity The entity to persist.
     * @param <T> The type of the entity.
     * @throws IOException If an I/O error occurs during serialization.
     */
    @SuppressWarnings("unchecked")
    synchronized public <T> void save(T entity) throws IOException {
        Class<T> entityClass = (Class<T>) entity.getClass();
        String entityTypeName = entityClass.getSimpleName().toLowerCase();
        File storageFile = new File(databaseDirectory, "grug_" + entityTypeName + ".ser");

        List<T> entityList = loadCollection(entityClass, storageFile);
        entityList.add(entity);

        try (FileOutputStream fileOutput = new FileOutputStream(storageFile);
             ObjectOutputStream objectOutput = new ObjectOutputStream(fileOutput)) {
            objectOutput.writeObject(entityList);
        }
    }

    /**
     * Updates all entities of a given type in the Grug database that match the specified condition.
     *
     * @param entityClass The class of the entities to update.
     * @param condition The predicate to identify entities to update.
     * @param update The consumer that defines how to modify matching entities.
     * @param <T> The type of the entities.
     * @throws IOException If an I/O error occurs during serialization or deserialization.
     */
    synchronized public <T> void update(Class<T> entityClass, Predicate<T> condition, Consumer<T> update) throws IOException {
        String entityTypeName = entityClass.getSimpleName().toLowerCase();
        File storageFile = new File(databaseDirectory, "grug_" + entityTypeName + ".ser");

        List<T> entityList = loadCollection(entityClass, storageFile);
        entityList.stream().filter(condition).forEach(update);

        try (FileOutputStream fileOutput = new FileOutputStream(storageFile);
             ObjectOutputStream objectOutput = new ObjectOutputStream(fileOutput)) {
            objectOutput.writeObject(entityList);
        }
    }

    /**
     * Updates the first entity matching the condition or saves the entity if no match is found.
     *
     * @param entity The entity to save if no match is found.
     * @param condition The predicate to identify an entity to update.
     * @param update The consumer that defines how to modify a matching entity.
     * @param <T> The type of the entity.
     * @throws IOException If an I/O error occurs during serialization or deserialization.
     */
    @SuppressWarnings("unchecked")
    synchronized public <T> void updateOrSave(T entity, Predicate<T> condition, Consumer<T> update) throws IOException {
        Class<T> entityClass = (Class<T>) entity.getClass();
        String entityTypeName = entityClass.getSimpleName().toLowerCase();
        File storageFile = new File(databaseDirectory, "grug_" + entityTypeName + ".ser");

        List<T> entityList = loadCollection(entityClass, storageFile);
        boolean found = false;
        for(T existingEntity : entityList) {
            if(condition.test(existingEntity)) {
                found = true;
                update.accept(existingEntity);
                break;
            }
        }

        if(!found) { entityList.add(entity); }

        try (FileOutputStream fileOutput = new FileOutputStream(storageFile);
             ObjectOutputStream objectOutput = new ObjectOutputStream(fileOutput)) {
            objectOutput.writeObject(entityList);
        }
    }

    /**
     * Persists a batch of entities of the same type to the Grug database.
     *
     * @param entities The list of entities to persist.
     * @param <T> The type of the entities.
     * @throws IOException If an I/O error occurs during serialization.
     */
    @SuppressWarnings("unchecked")
    synchronized public <T> void saveBatch(List<T> entities) throws IOException {
        Class<T> entityClass = (Class<T>) entities.getFirst().getClass();
        String entityTypeName = entities.getFirst().getClass().getSimpleName().toLowerCase();
        File storageFile = new File(databaseDirectory, "grug_" + entityTypeName + ".ser");

        List<T> existingEntityList = loadCollection(entityClass, storageFile);
        existingEntityList.addAll(entities);

        try (FileOutputStream fileOutput = new FileOutputStream(storageFile);
             ObjectOutputStream objectOutput = new ObjectOutputStream(fileOutput)) {
            objectOutput.writeObject(existingEntityList);
        }
    }

    /**
     * Retrieves all entities of a given type from the Grug database.
     *
     * @param entityClass The class of the entities to retrieve.
     * @param <T> The type of the entities.
     * @return A list of all entities of the specified type.
     * @throws IOException If an I/O error occurs during deserialization.
     */
    synchronized public <T> List<T> find(Class<T> entityClass) throws IOException {
        String entityTypeName = entityClass.getSimpleName().toLowerCase();
        File storageFile = new File(databaseDirectory, "grug_" + entityTypeName + ".ser");
        return loadCollection(entityClass, storageFile);
    }

    /**
     * Retrieves entities of a given type from the Grug database that match the specified condition.
     *
     * @param entityClass The class of the entities to retrieve.
     * @param condition The predicate to filter entities.
     * @param <T> The type of the entities.
     * @return A list of entities matching the condition.
     * @throws IOException If an I/O error occurs during deserialization.
     */
    synchronized public <T> List<T> find(Class<T> entityClass, Predicate<T> condition) throws IOException {
        String entityTypeName = entityClass.getSimpleName().toLowerCase();
        File storageFile = new File(databaseDirectory, "grug_" + entityTypeName + ".ser");
        return loadCollection(entityClass, storageFile).stream().filter(condition).toList();
    }

    /**
     * Deletes entities of a given type from the Grug database that match the specified condition.
     *
     * @param entityClass The class of the entities to delete.
     * @param condition The predicate to identify entities to delete.
     * @param <T> The type of the entities.
     * @throws IOException If an I/O error occurs during serialization or deserialization.
     *
     */
    synchronized public <T> void delete(Class<T> entityClass, Predicate<T> condition) throws IOException {
        String entityTypeName = entityClass.getSimpleName().toLowerCase();
        File storageFile = new File(databaseDirectory, "grug_" + entityTypeName + ".ser");
        List<T> entityList = loadCollection(entityClass, storageFile);

        int originalSize = entityList.size();
        entityList.removeIf(condition);
        int updatedSize = entityList.size();

        logger.debug("Deleted {} objects of type {}", originalSize - updatedSize, entityTypeName);

        if (entityList.isEmpty()) {
            if (!storageFile.delete()) { logger.debug("File couldn't be deleted"); }
            logger.debug("Collection empty, deleted file {}", storageFile.getName());
            return;
        }

        try (FileOutputStream fileOutput = new FileOutputStream(storageFile);
             ObjectOutputStream objectOutput = new ObjectOutputStream(fileOutput)) {
            objectOutput.writeObject(entityList);
        }
    }

    /**
     * Loads a collection of entities of a given type from the Grug database file.
     *
     * @param entityClass The class of the entities to load.
     * @param storageFile The file containing the serialized entity collection.
     * @param <T> The type of the entities.
     * @return A list of deserialized entities, or an empty list if the file is missing or corrupted.
     * @throws IOException If an I/O error occurs during deserialization.
     */
    @SuppressWarnings("unchecked")
    private <T> List<T> loadCollection(Class<T> entityClass, File storageFile) throws IOException {
        String entityTypeName = entityClass.getSimpleName().toLowerCase();
        if (!storageFile.exists() || storageFile.length() == 0) {
            logger.debug("No data found for {}, returning empty list", entityTypeName);
            return new ArrayList<>();
        }

        try (FileInputStream fileInput = new FileInputStream(storageFile);
             ObjectInputStream objectInput = new ObjectInputStream(fileInput)) {
            Object deserializedObject = objectInput.readObject();
            if (deserializedObject instanceof List) {
                return (List<T>) deserializedObject;
            } else {
                logger.warn("Corrupted data for {}, resetting to empty list", entityTypeName);
                return new ArrayList<>();
            }
        } catch (ClassNotFoundException e) {
            logger.error("Class not found while loading {}: {}", entityTypeName, e.getMessage());
            throw new IOException("Failed to deserialize collection", e);
        }
    }
}