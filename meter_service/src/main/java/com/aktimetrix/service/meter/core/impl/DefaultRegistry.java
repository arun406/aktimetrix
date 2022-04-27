package com.aktimetrix.service.meter.core.impl;


import com.aktimetrix.service.meter.UnknownNameException;
import com.aktimetrix.service.meter.core.api.Constants;
import com.aktimetrix.service.meter.core.api.Registry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;


/**
 * @author arun kumar kandakatla
 */
@Component
@Slf4j
public class DefaultRegistry implements Registry {

    private HashMap<String, List<RegistryEntry>> registryMap = new HashMap<>();
    private boolean attributeValidationOn = false;

    @Override
    public void unregister(String name) {
        registryMap.remove(name);
    }

    @Override
    public void register(String name, Map attributes, Object o) {
        if (log.isDebugEnabled()) {
            log.debug(String.format("Registering %s with name %s and %s", o, name, attributes));
        }

        if (name == null || o == null) {
            throw new IllegalArgumentException("Name or Instance/Type cannot be null");
        }

        if (name.trim().length() == 0) {
            throw new IllegalArgumentException("Name cannot be empty");
        }

        if (null == attributes)
            attributes = new HashMap<>();

        if (attributeValidationOn) {
            if (!(Constants.ATTRIBUTE_MAP.equals(name) || Constants.FUNCTION_MAP.equals(name))) { //Dont process attributes for ATTRIBUTE map itself...
                //process and transform the attributes before actual registration
                attributes = processAttributes(this, attributes);
            }
        }

        createEntry(name, attributes, o);
    }

    @Override
    public void register(String name, Object o) {
        register(name, new HashMap(), o);
    }


    @Override
    public void register(Class type, Map attributes, Object o) {
        //just delegate to the name version of the register
        register(type.getName(), attributes, o);
    }

    private void createEntry(String name, Map attributes, Object o) {
        List<RegistryEntry> registryEntries = registryMap.containsKey(name) ?
                registryMap.get(name) : new ArrayList<>();

        fillDefaultAttributes(name, attributes, o);

        registryEntries.add(new RegistryEntry(o, attributes));

        //update the registry again - we may end up using a distributed cache
        registryMap.put(name, registryEntries);
    }

    private void fillDefaultAttributes(String name, Map attributes, Object o) {

        //fill ATT_NAME attribute
        attributes.put(Constants.ATT_NAME, name);
        if (!attributes.containsKey(Constants.ATT_VERSION)) {
            attributes.put(Constants.ATT_VERSION, Constants.VAL_VERSION_DEFAULT);
        }

        //fill ATT_CLASS attribute if not already done
        if (!attributes.containsKey(Constants.ATT_CLASS)) {
            if (!(o instanceof Class)) {
                attributes.put(Constants.ATT_CLASS, o.getClass().getName());
            } else {
                attributes.put(Constants.ATT_CLASS, ((Class) o).getName());
            }
        }

        //fill ATT_TYPE attribute if not already done
        if (!attributes.containsKey(Constants.ATT_TYPE)) {
            if (!(o instanceof Class)) {
                attributes.put(Constants.ATT_TYPE, o.getClass());
            } else {
                attributes.put(Constants.ATT_TYPE, (Class) o);
            }
        }

        //For object instances the ATT_SCOPE is defaulted to Singleton scope
        if (!(o instanceof Class)) {
            attributes.put(Constants.ATT_SCOPE, Constants.VAL_SCOPE_SINGLETON);
        }

        //fill ATT_SCOPE attribute if not already done
        if (!attributes.containsKey(Constants.ATT_SCOPE)) {
            attributes.put(Constants.ATT_SCOPE, Constants.VAL_SCOPE_DEFAULT);
        }
    }


    @Override
    public Object lookup(String name, Predicate<RegistryEntry> filter) throws UnknownNameException {
        if (log.isDebugEnabled()) {
            log.debug(String.format("About to lookup %s with filter %s", name, filter));
        }

        if (!registryMap.containsKey(name)) {
            throw new UnknownNameException(String.format("Lookup Name %s is unknown to me", name));
        }

        List<RegistryEntry> registryEntries = registryMap.get(name);

        RegistryEntry entry = null;

        if (null == filter) {
            //pick up the first item from the list
            entry = registryEntries.get(0);
        } else {
            //apply the filter and get the entry
            try {
                entry = applyFilter(registryEntries, filter);
            } catch (NoSuchElementException e) {
                //this means there was no match for the filter
                throw new UnknownNameException(String.format("Lookup Name %s is unknown to me, filter returned empty", name));
            }
        }

        try {
            Object retVal = entry.getInstance();
            return retVal;
        } catch (IllegalAccessException e) {
            throw new UnknownNameException(String.format("Lookup Name %s is unknown to me, Instantiation failed", name), e);
        } catch (InstantiationException e) {
            throw new UnknownNameException(String.format("Lookup Name %s is unknown to me, Instantiation failed", name), e);
        }


    }

    @Override
    public <T> T lookup(String name, Class<T> type) throws UnknownNameException {

        return (T) lookup(name, registryEntry -> {
            boolean retVal = false;
            Class clz = (Class) registryEntry.attribute(Constants.ATT_CLASS);
            //check whether the declared type is assignable to the given type
            if (type.isAssignableFrom(clz)) retVal = true;
            return retVal;

        });
    }

    @Override
    public <T> T lookup(String name, Class<T> type, Predicate<RegistryEntry> filter) throws UnknownNameException {


        return (T) lookup(name, filter.and(registryEntry -> {
            boolean retVal = false;
            //Class clz = (Class)registryEntry.attribute(Constants.ATT_CLASS);
            Class clz = (Class) registryEntry.attribute(Constants.ATT_TYPE);
            //check whether the declared type is assignable to the given type
            if (type.isAssignableFrom(clz)) {
                retVal = true;
            }
            return retVal;
        }));
    }


    @Override
    public List<Object> lookupAll(String name, Predicate<RegistryEntry> filter) throws UnknownNameException {
        if (log.isDebugEnabled()) {
            log.debug(String.format("About to lookup %s with filter %s", name, filter));
        }

        if (!registryMap.containsKey(name)) {
            throw new UnknownNameException(String.format("Lookup Name %s is unknown to me", name));
        }

        List<RegistryEntry> registryEntries = registryMap.get(name);

        List<RegistryEntry> entriesByFilter = getEntriesByFilter(filter, registryEntries);

        List<Object> finalList = getObjects(entriesByFilter);

        return finalList;


    }


    /**
     * Lists and filters the given list of Registry entries and returns a list of Registry Entries
     *
     * @param filter
     * @param registryEntries
     * @return
     */
    private List<RegistryEntry> getEntriesByFilter(Predicate<RegistryEntry> filter, List<RegistryEntry> registryEntries) {
        List<RegistryEntry> finalList = registryEntries.stream().filter(filter).collect(Collectors.toList());
        return finalList;
    }


    private List<Object> getObjects(List<RegistryEntry> registryEntries) {
        Iterator<RegistryEntry> iterator = registryEntries.iterator();
        ArrayList<Object> finalList = new ArrayList<>();

        while (iterator.hasNext()) {
            try {
                finalList.add(iterator.next().getInstance());
            } catch (IllegalAccessException e) {
                log.error("Error get registry entry instance", e);
            } catch (InstantiationException e) {
                log.error("Error get registry entry instance", e);
            }
        }
        return finalList;
    }

    @Override
    public List<Object> lookupAll(Class type, Predicate<RegistryEntry> filter) throws UnknownNameException {
        //just delegate the the name version of the same method
        return lookupAll(type.getName(), filter);
    }

    @Override
    public List<Object> lookupAll(Predicate<RegistryEntry> filter) {
        //Get the entries filtered as a list
        //then instantiate them and provide the list of objects

        List<RegistryEntry> registryEntries = lookupAllEntries(filter);

        return getObjects(registryEntries);

    }

    @Override
    public List<Object> lookupAll(Predicate<RegistryEntry> filter, Comparator<RegistryEntry> sorter) throws UnknownNameException {
        //Get the entries filtered as a list
        //then instantiate them and provide the list of objects

        List<RegistryEntry> registryEntries = lookupAllEntries(filter);

        //sort them using the comparator / sorter
        registryEntries.sort(sorter);

        return getObjects(registryEntries);
    }

    @Override
    public List<RegistryEntry> lookupAllEntries(Predicate<RegistryEntry> filter) {
        final ArrayList<RegistryEntry> entries = new ArrayList<>();

        final Collection<List<RegistryEntry>> values = registryMap.values();
        for (List<RegistryEntry> value : values) {
            entries.addAll(value);
        }

        final List<RegistryEntry> finalList = getEntriesByFilter(filter, entries);
        return finalList;
    }

    @Override
    public List<RegistryEntry> lookupAllEntries(String name, Predicate<RegistryEntry> filter) throws UnknownNameException {
        if (log.isDebugEnabled()) {
            log.debug(String.format("About to lookup %s with filter %s", name, filter));
        }

        if (!registryMap.containsKey(name)) {
            throw new UnknownNameException(String.format("Lookup Name %s is unknown to me", name));
        }

        List<RegistryEntry> registryEntries = registryMap.get(name);

        List<RegistryEntry> entriesByFilter = getEntriesByFilter(filter, registryEntries);

        return entriesByFilter;
    }

    @Override
    public void forEach(BiConsumer<String, List<RegistryEntry>> consumer) {
        this.registryMap.forEach(consumer); //simple delegation to the underlying map
    }


    @Override
    public Object lookup(String name) throws UnknownNameException {
        return lookup(name, registryEntry -> {
            return true; //match every valid entry
        });
    }

    @Override
    public Object lookupRegistryEntry(String name) throws UnknownNameException {
        if (log.isDebugEnabled()) {
            log.debug(String.format("About to lookup %s", name));
        }

        if (!registryMap.containsKey(name)) {
            throw new UnknownNameException(String.format("Lookup Name %s is unknown to me", name));
        }

        final List<RegistryEntry> registryEntries = registryMap.get(name);
        if (registryEntries.isEmpty()) {
            throw new UnknownNameException(String.format("Lookup Name %s exist but there are no entries", name));
        }

        final RegistryEntry entry = registryEntries.get(0);
        return entry;
    }


    @Override
    public void validateAttributes(boolean b) {
        attributeValidationOn = b;
    }

    private RegistryEntry applyFilter(List<RegistryEntry> registryEntries, Predicate<RegistryEntry> filter) {
        Optional<RegistryEntry> registryEntryOptional = registryEntries.stream().filter(filter).findFirst();

        return registryEntryOptional.get();
    }

    /**
     * processes the attributes
     *
     * @param registry
     * @param attributes
     */
    private Map processAttributes(Registry registry, Map attributes) {
        //check if there is a provider registered
        final Map retVal = new HashMap<>();
        try {
            Map<String, Function<Object, ?>> attributeMap = (Map<String, Function<Object, ?>>) registry.lookup(Constants.ATTRIBUTE_MAP);

            //process all the attributes
            attributes.forEach((key, value) -> {
                //validate and transform the value as per the attribute map
                if (attributeMap.containsKey(key)) {
                    Function<Object, ?> attributeHandler = attributeMap.get(key);
                    try {
                        Object transformedVal = attributeHandler.apply(value);
                        retVal.put(key, transformedVal);
                    } catch (Exception e) {
                        log.debug("Processing (%s,%s) failed due to Exception %s. So ignoring the attribute value", key, value, e);
                    }
                } else {
                    log.debug("Seems no Attribute Provider for Attribute(%s), Please check the configuration...", key);
                }
            });

        } catch (UnknownNameException e) {
            log.debug("Seems no Attribute Providers are present. So Attribute conversion failed (%s) ..", e.getMessage());
            return attributes;
        }
        return retVal;
    }


    @Override
    public String toString() {
        return "RegistryImpl{" +
                "registryMap=" + registryMap +
                ", attributeValidationOn=" + attributeValidationOn +
                '}';
    }
}
