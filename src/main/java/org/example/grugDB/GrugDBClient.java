package org.example.grugDB;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

public class GrugDBClient {

    private static GrugDBClient singletonInstance = null;
    private static File databaseDirectory = null;
    private static final Logger logger = LoggerFactory.getLogger(GrugDBClient.class);

    private GrugDBClient() {
        logger.info("GrugDBClient instance created");
    }

    public static GrugDBClient getInstance(boolean shouldClearDatabase) {
        if (singletonInstance != null) {
            return singletonInstance;
        }
        databaseDirectory = new File("grug_db");
        if (shouldClearDatabase && databaseDirectory.exists()) {
            for (File storedFile : Objects.requireNonNull(databaseDirectory.listFiles())) {
                storedFile.delete();
            }
        }
        if (!databaseDirectory.exists()) {
            databaseDirectory.mkdirs();
        }
        singletonInstance = new GrugDBClient();
        return singletonInstance;
    }

    synchronized public <T> void save(T entity) throws IOException {
        String entityTypeName = entity.getClass().getSimpleName().toLowerCase();
        File storageFile = new File(databaseDirectory, "grug_" + entityTypeName + ".ser");

        List<T> entityList = loadCollection(storageFile, entityTypeName);
        entityList.add(entity);

        try (FileOutputStream fileOutput = new FileOutputStream(storageFile);
             ObjectOutputStream objectOutput = new ObjectOutputStream(fileOutput)) {
            objectOutput.writeObject(entityList);
        }
    }

    synchronized public <T> void saveBatch(List<T> entities) throws IOException {
        String entityTypeName = entities.get(0).getClass().getSimpleName().toLowerCase();
        File storageFile = new File(databaseDirectory, "grug_" + entityTypeName + ".ser");

        List<T> existingEntityList = loadCollection(storageFile, entityTypeName);
        Collections.addAll()
    }

    synchronized public <T> List<T> find(Class<T> entityClass) throws IOException {
        String entityTypeName = entityClass.getSimpleName().toLowerCase();
        File storageFile = new File(databaseDirectory, "grug_" + entityTypeName + ".ser");
        return loadCollection(storageFile, entityTypeName);
    }

    synchronized public <T> void delete(Class<T> entityClass, Predicate<T> condition) throws IOException {
        String entityTypeName = entityClass.getSimpleName().toLowerCase();
        File storageFile = new File(databaseDirectory, "grug_" + entityTypeName + ".ser");
        List<T> entityList = loadCollection(storageFile, entityTypeName);

        int originalSize = entityList.size();
        entityList.removeIf(condition);
        int updatedSize = entityList.size();

        logger.debug("Deleted {} objects of type {}", originalSize - updatedSize, entityTypeName);

        if (entityList.isEmpty()) {
            storageFile.delete();
            logger.debug("Collection empty, deleted file {}", storageFile.getName());
            return;
        }

        try (FileOutputStream fileOutput = new FileOutputStream(storageFile);
             ObjectOutputStream objectOutput = new ObjectOutputStream(fileOutput)) {
            objectOutput.writeObject(entityList);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> List<T> loadCollection(File storageFile, String entityTypeName) throws IOException {
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