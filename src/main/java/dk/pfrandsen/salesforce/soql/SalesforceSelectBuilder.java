package dk.pfrandsen.salesforce.soql;

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
 * Helper class for building SOQL select queries.
 */
public class SalesforceSelectBuilder {
    private static final Logger logger = LoggerFactory.getLogger(SalesforceSelectBuilder.class);

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

    public SalesforceSelectBuilder setsObject(String sObject) {
        this.sObject = sObject;
        return this;
    }

    public SalesforceSelectBuilder addId(String... ids) {
        return addId(Arrays.asList(ids));
    }

    public SalesforceSelectBuilder addId(String id) {
        String i = id == null ? "" : id.trim();
        if (i.length() > 0) {
            idSet.add(i);
        }
        return this;
    }

    public SalesforceSelectBuilder addId(List<String> ids) {
        List<String> list = getNonEmpty(ids);
        if (!list.isEmpty()) {
            idSet.addAll(list);
        }
        return this;
    }

    public SalesforceSelectBuilder addField(String field) {
        String f = field == null ? "" : field.trim();
        if (f.length() > 0) {
            fields.add(f);
        }
        return this;
    }

    public SalesforceSelectBuilder addFields(List<String> fields) {
        List<String> list = getNonEmpty(fields);
        if (!list.isEmpty()) {
            this.fields.addAll(list);
        }
        return this;
    }

    public SalesforceSelectBuilder addRelationField(String rel, String field) {
        String r = rel == null ? "" : rel.trim();
        String f = field == null ? "" : field.trim();
        if (r.length() > 0 && f.length() > 0) {
            relationFields.add(new AbstractMap.SimpleEntry<>(r, Collections.singletonList(f)));
        }
        return this;
    }

    public SalesforceSelectBuilder addRelationFields(String rel, List<String> fields) {
        String r = rel == null ? "" : rel.trim();
        if (r.length() > 0 && fields != null) {
            List<String> f = getNonEmpty(fields);
            if (!f.isEmpty()) {
                relationFields.add(new AbstractMap.SimpleEntry<>(r, f));
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
     *
     * @param entry relation name and list of fields in relation
     * @return a list of strings where each string is the value from {@code entry} prefixed with the key
     *         from {@code entry} and "."
     */
    private List<String> join(AbstractMap.SimpleEntry<String, List<String>> entry) {
        String rel = entry.getKey();
        return entry.getValue().stream().map(v -> rel + "." + v).collect(Collectors.toList());
    }

}
