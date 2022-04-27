package com.aktimetrix.service.meter.core.api;

/**
 * @author arun kumar kandakatla
 */

import com.aktimetrix.service.meter.UnknownNameException;
import com.aktimetrix.service.meter.core.impl.RegistryEntry;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

/**
 * The Registry is the abstraction of a Directory service within the JVM.
 *
 * @author vj
 */
public interface Registry {

    /**
     * Unregisters an instance. Preferably to be used only when testing.
     *
     * @param name
     */
    void unregister(String name);

    /**
     * Registers an instance with a given set of attributes
     *
     * @param attributes
     * @param o
     */
    void register(String name, Map attributes, Object o);

    /**
     * Register without any attributes
     *
     * @param name
     * @param o
     */
    void register(String name, Object o);


    /**
     * The attribute version of the Class registration
     *
     * @param type
     * @param attributes
     * @param o
     */
    void register(Class type, Map attributes, Object o);

    /**
     * Look up an instance given the expected attributes
     * IF More than one attribute all the attributes are expected to be an exact match
     *
     * @param filter
     * @return
     */
    Object lookup(String name, Predicate<RegistryEntry> filter) throws UnknownNameException;

    /**
     * Look up an instance given the name and Type
     *
     * @param name
     * @param type
     * @param <T>
     * @return
     * @throws UnknownNameException
     */
    <T> T lookup(String name, Class<T> type) throws UnknownNameException;

    /**
     * Looks up the given name and Type matching the given filter
     *
     * @param name
     * @param type
     * @param filter
     * @param <T>
     * @return
     * @throws UnknownNameException
     */
    <T> T lookup(String name, Class<T> type, Predicate<RegistryEntry> filter) throws UnknownNameException;

    /**
     * Looks up all instances given the criteria
     *
     * @param name
     * @param filter
     * @return
     * @throws UnknownNameException
     */
    List<Object> lookupAll(String name, Predicate<RegistryEntry> filter) throws UnknownNameException;


    /**
     * The lookup all instances of a given interface given the filter
     *
     * @param type
     * @param filter
     * @return
     * @throws UnknownNameException
     */
    List<Object> lookupAll(Class type, Predicate<RegistryEntry> filter) throws UnknownNameException;


    /**
     * Looksup all instances across all Registered types matching the given criteria
     *
     * @param filter
     * @return
     * @throws UnknownNameException
     */
    List<Object> lookupAll(Predicate<RegistryEntry> filter);

    /**
     * Looksup all instances across all Registered types matching the given criteria along with the sort behaviour
     *
     * @param filter
     * @param sorter
     * @return
     * @throws UnknownNameException
     */
    List<Object> lookupAll(Predicate<RegistryEntry> filter, Comparator<RegistryEntry> sorter) throws UnknownNameException;

    /**
     * Lookup all the Entries which match a corresponding criteria
     *
     * @param filter
     * @return
     * @throws UnknownNameException
     */
    List<RegistryEntry> lookupAllEntries(Predicate<RegistryEntry> filter);

    /**
     * Lookup all the Registry Entries which match a corresponding criteria and name
     *
     * @param name
     * @param filter
     * @return
     * @throws UnknownNameException
     */
    List<RegistryEntry> lookupAllEntries(String name, Predicate<RegistryEntry> filter) throws UnknownNameException;


    /**
     * Support for iterating and doing something for each of the entry in the registry
     * What the consumer does with the entry is upto the consumer
     *
     * @param consumer
     */
    void forEach(BiConsumer<String, List<RegistryEntry>> consumer);

    /**
     * Simpler lookup with no filters
     *
     * @param name
     * @return
     * @throws UnknownNameException
     */
    Object lookup(String name) throws UnknownNameException;

    /**
     * Simpler lookup with no filters
     *
     * @param name
     * @return
     * @throws UnknownNameException
     */
    Object lookupRegistryEntry(String name) throws UnknownNameException;

    /**
     * Enable or Disable attribute validations
     *
     * @param b
     */
    void validateAttributes(boolean b);
}
