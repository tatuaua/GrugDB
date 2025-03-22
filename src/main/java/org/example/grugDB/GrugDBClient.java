package org.example.grugDB;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

public class GrugDBClient {

    private static GrugDBClient instance = null;
    private static File dbDir = null;
    private static final Logger log = LoggerFactory.getLogger(GrugDBClient.class);

    private GrugDBClient() {
        log.info("GrugDBClient instance created");
    }

    public static GrugDBClient getInstance(boolean clearDatabase) {
        if (instance != null) {
            return instance;
        }
        dbDir = new File("grug_db");
        if (clearDatabase && dbDir.exists()) {
            for (File file : Objects.requireNonNull(dbDir.listFiles())) {
                file.delete();
            }
        }
        if (!dbDir.exists()) {
            dbDir.mkdirs();
        }
        instance = new GrugDBClient();
        return instance;
    }

    synchronized public <T> void save(T object) throws IOException {
        String className = object.getClass().getSimpleName().toLowerCase();
        File file = new File(dbDir, "grug_" + className + ".ser");

        List<T> collection = loadCollection(file, className);
        collection.add(object);

        try (FileOutputStream fos = new FileOutputStream(file);
             ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            oos.writeObject(collection);
        }
    }

    synchronized public <T> List<T> find(Class<T> clazz) throws IOException {
        String className = clazz.getSimpleName().toLowerCase();
        File file = new File(dbDir, "grug_" + className + ".ser");
        return loadCollection(file, className);
    }

    synchronized public <T> void delete(Class<T> clazz, Predicate<T> predicate) throws IOException {
        String className = clazz.getSimpleName().toLowerCase();
        File file = new File(dbDir, "grug_" + className + ".ser");
        List<T> collection = loadCollection(file, className);

        var sizeBefore = collection.size();
        collection.removeIf(predicate);
        var sizeAfter = collection.size();

        log.debug("Deleted {} objects of type {}", sizeBefore - sizeAfter, className);

        if(collection.isEmpty()) {
            file.delete();
            log.debug("Collection empty, deleted file {}", file.getName());
            return;
        }

        try (FileOutputStream fos = new FileOutputStream(file);
             ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            oos.writeObject(collection);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> List<T> loadCollection(File file, String className) throws IOException {
        if (!file.exists() || file.length() == 0) {
            log.debug("No data found for {}, returning empty list", className);
            return new ArrayList<>();
        }

        try (FileInputStream fis = new FileInputStream(file);
             ObjectInputStream ois = new ObjectInputStream(fis)) {
            Object obj = ois.readObject();
            if (obj instanceof List) {
                return (List<T>) obj;
            } else {
                log.warn("Corrupted data for {}, resetting to empty list", className);
                return new ArrayList<>();
            }
        } catch (ClassNotFoundException e) {
            log.error("Class not found while loading {}: {}", className, e.getMessage());
            throw new IOException("Failed to deserialize collection", e);
        }
    }
}