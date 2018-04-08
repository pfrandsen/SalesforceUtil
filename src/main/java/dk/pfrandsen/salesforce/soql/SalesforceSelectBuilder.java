package dk.pfrandsen.salesforce.soql;

import dk.pfrandsen.salesforce.NSMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Helper class for building SOQL select queries. If prefix and/or replace are set the namespace prefix for
 * sObject, fields, and relations values are removed/replaced.
 */
public class SalesforceSelectBuilder {
    private static final Logger logger = LoggerFactory.getLogger(SalesforceSelectBuilder.class);

    private String prefix; // prefix value to remove or replace
    private String replace; // prefix replace value
    private boolean logQuery; // if true final query is logged when build
    private String sObject;
    private final List<String> idSet;
    private final List<String> fields;
    private final List<AbstractMap.SimpleEntry<String, List<String>>> relationFields;
    private int limit;

    public SalesforceSelectBuilder() {
        idSet = new ArrayList<>();
        fields = new ArrayList<>();
        relationFields = new ArrayList<>();
        limit = 0;
    }

    public SalesforceSelectBuilder(boolean logQuery) {
        this();
        this.logQuery = logQuery;
    }

    public SalesforceSelectBuilder(String prefix, String replace) {
        this();
        this.prefix = prefix;
        this.replace = replace;
    }

    public SalesforceSelectBuilder(String prefix, String replace, boolean logQuery) {
        this(logQuery);
        this.prefix = prefix;
        this.replace = replace;
    }

    public SalesforceSelectBuilder setsObject(String sObject) {
        this.sObject = map(sObject);
        return this;
    }

    /**
     * Add id value to query id set.
     *
     * @param id id value, only added if non-empty.
     * @return
     */
    public SalesforceSelectBuilder addId(String id) {
        String i = id == null ? "" : id.trim();
        if (i.length() > 0) {
            add(i, idSet);
        }
        return this;
    }

    /**
     * Add list of id values to query id set.
     *
     * @param ids list of id values, values that are non-empty are added
     * @return builder
     */
    public SalesforceSelectBuilder addId(String... ids) {
        return addId(Arrays.asList(ids));
    }

    /**
     * Add list of id values to query id set.
     *
     * @param ids list of id values, values that are non-empty are added
     * @return builder
     */
    public SalesforceSelectBuilder addId(List<String> ids) {
        List<String> list = ids == null ? new ArrayList<>() : getNonEmpty(ids);
        list.forEach(id -> {
            addId(id);
        });
        return this;
    }

    /**
     * Add field to query. Field is only added if it is not empty/whitespace and
     * it is not already added.
     * @param field field name
     * @return builder
     */
    public SalesforceSelectBuilder addField(String field) {
        String f = field == null ? "" : field.trim();
        if (f.length() > 0) {
            add(map(f), fields);
        }
        return this;
    }

    /**
     * Add list of fields to query. An entry in the list is only added if it is
     * not empty/whitespace and it is not already added.
     *
     * @param fields field names
     * @return builder
     */
    public SalesforceSelectBuilder addFields(List<String> fields) {
        List<String> list = getNonEmpty(fields);
        if (!list.isEmpty()) {
            list.forEach(this::addField);
        }
        return this;
    }

    /**
     * Add list of fields to query. An entry in the list is only added if it is
     * not empty/whitespace and it is not already added.
     *
     * @param fields field names
     * @return builder
     */
    public SalesforceSelectBuilder addFields(String... fields) {
        return addFields(Arrays.asList(fields));
    }

    /**
     * Add single level relationship (x__r.y) lookup to query.
     *
     * @param rel relationship name
     * @param field field name
     * @return builder
     */
    public SalesforceSelectBuilder addRelationField(String rel, String field) {
        String r = rel == null ? "" : rel.trim();
        String f = field == null ? "" : field.trim();
        if (r.length() > 0 && f.length() > 0) {
            addRel(new AbstractMap.SimpleEntry<>(map(r), Collections.singletonList(map(f))));
        }
        return this;
    }

    /**
     * Add multi-level relationship (x__r.y__r[...].z) lookup to query.
     *
     * @param rel relationship names (max 5, not checked).
     * @param field field name
     * @return builder
     */
    public SalesforceSelectBuilder addRelationField(List<String> rel, String field) {
        String r = String.join(".", getNonEmpty(rel).stream().map(this::map).collect(Collectors.toList()));
        String f = field == null ? "" : field.trim();
        if (r.length() > 0 && f.length() > 0) {
            addRel(new AbstractMap.SimpleEntry<>(r, Collections.singletonList(map(f))));
        }
        return this;
    }

    /**
     * Add single level relationships (x__r.y, x__r.z, ...) lookup to query.
     *
     * @param rel relationship name
     * @param fields field names
     * @return builder
     */
    public SalesforceSelectBuilder addRelationFields(String rel, String... fields) {
        return addRelationFields(rel, Arrays.asList(fields));
    }

    /**
     * Add single level relationships (x__r.y, x__r.z, ...) lookup to query.
     *
     * @param rel relationship name
     * @param fields field names
     * @return builder
     */
    public SalesforceSelectBuilder addRelationFields(String rel, List<String> fields) {
        String r = rel == null ? "" : rel.trim();
        if (r.length() > 0 && fields != null) {
            List<String> f = getNonEmpty(fields).stream().map(this::map).collect(Collectors.toList());
            if (!f.isEmpty()) {
                addRel(new AbstractMap.SimpleEntry<>(map(r), f));
            }
        }
        return this;
    }

    /**
     * Add multi-level relationships (x__r.y__r[...].a, x__r.y__r[...].b, ...) lookup to query.
     *
     * @param rel relationship names (max 5, not checked).
     * @param fields field names
     * @return builder
     */
    public SalesforceSelectBuilder addRelationFields(List<String> rel, String... fields) {
        return addRelationFields(rel, Arrays.asList(fields));
    }

    /**
     * Add multi-level relationships (x__r.y__r[...].a, x__r.y__r[...].b, ...) lookup to query.
     *
     * @param rel relationship names (max 5, not checked).
     * @param fields field names
     * @return builder
     */
    public SalesforceSelectBuilder addRelationFields(List<String> rel, List<String> fields) {
        String r = String.join(".", getNonEmpty(rel).stream().map(this::map).collect(Collectors.toList()));
        if (r.length() > 0 && fields != null) {
            List<String> f = getNonEmpty(fields).stream().map(this::map).collect(Collectors.toList());
            if (!f.isEmpty()) {
                addRel(new AbstractMap.SimpleEntry<>(r, f));
            }
        }
        return this;
    }

    /**
     * Set limit (max number of sObjects returned by select query). Use 0 (default) or negative value to remove limit.
     *
     * @param limit select limit value, use 0 to remove limit
     * @return builder
     */
    public SalesforceSelectBuilder setLimit(int limit) {
        this.limit = limit;
        return this;
    }

    public String build() {
        if (sObject == null || sObject.trim().length() == 0) {
            logger.error("sObject not specified");
            return null;
        }
        if (fields.isEmpty() && relationFields.isEmpty()) {
            logger.error("query field(s) not specified");
            return null;
        }
        List<String> fld = new ArrayList<>();
        fld.addAll(fields);
        fld.addAll(relationFields.stream().map(this::join).flatMap(Collection::stream).collect(Collectors.toList()));
        String query = "select " + String.join(", ", fld) + " from " + sObject;
        if (!idSet.isEmpty()) {
            if (idSet.size() == 1) {
                query += " where id = '" + idSet.get(0) + "'";
            } else {
                query += " where id in [" + String.join(", ", idSet.stream().map(i -> "'" + i + "'").collect(Collectors.toList())) + "]";
            }
        }
        if (limit > 0) {
            query += " limit = " + limit;
        }
        if (logQuery) {
            logger.info(query);
        }
        return query;
    }

    /**
     * Get all strings with non-blank text.
     *
     * @param lst list of strings to filter and map
     * @return list of non-empty trimmed values
     */
    private List<String> getNonEmpty(List<String> lst) {
        Predicate<String> isEmpty = String::isEmpty;
        Predicate<String> notEmpty = isEmpty.negate();
        return lst.stream().filter(Objects::nonNull).map(String::trim).filter(notEmpty).collect(Collectors.toList());
    }

    /**
     * Join key with each element in list. Returns list of key.value strings.
     *
     * @param entry relation name and list of fields in relation
     * @return a list of strings where each string is the value from {@code entry} prefixed with the key
     *         from {@code entry} and "."
     */
    private List<String> join(AbstractMap.SimpleEntry<String, List<String>> entry) {
        String rel = entry.getKey();
        return entry.getValue().stream().map(v -> rel + "." + v).collect(Collectors.toList());
    }

    /**
     * Remove/replace namespace prefix in string.
     * Only removes/replaces prefix if prefix is non-empty and value starts with prefix.
     * Only replaces prefix if replace is non-empty string
     *
     * @param value string to replace prefix in / remove
     * @return modified value or identity
     */
    private String map(String value) {
        return NSMap.map(prefix, replace, value);
    }

    /**
     * Add value to value "set" if it does not already exist in set.
     *
     * @param value value to add to "set"
     * @param valueSet list to add value to (treated like an ordered set)
     */
    private void add(String value, List<String> valueSet) {
        if (!valueSet.contains(value)) {
            valueSet.add(value);
        }
    }

    /**
     * Add values to value "set". Only values thar does not already exist in set are added.
     *
     * @param values values to add to "set"
     * @param valueSet list to add value to (treated like an ordered set)
     */
    private void add(List<String> values, List<String> valueSet) {
        values.forEach(v -> {
            add(v, valueSet);
        });
    }

    private void addRel(AbstractMap.SimpleEntry<String, List<String>> entry) {
        List<String> ex = relationFields.stream().filter(e -> e.getKey().equals(entry.getKey()))
                .map(AbstractMap.SimpleEntry::getValue).findFirst().orElse(null);
        if (ex == null) {
            relationFields.add(entry);
        } else {
            entry.getValue().forEach(v -> {
                if (!ex.contains(v)) {
                    ex.add(v);
                }
            });
        }
    }

}
