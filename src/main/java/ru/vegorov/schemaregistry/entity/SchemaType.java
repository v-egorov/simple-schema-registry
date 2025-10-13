package ru.vegorov.schemaregistry.entity;

/**
 * Enum representing the type of schema stored in the database
 */
public enum SchemaType {
    /**
     * Canonical schemas define the authoritative structure for a subject
     */
    canonical,

    /**
     * Consumer output schemas define what a specific consumer expects as output
     */
    consumer_output
}