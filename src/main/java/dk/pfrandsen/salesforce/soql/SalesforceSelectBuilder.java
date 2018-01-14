package dk.pfrandsen.salesforce.soql;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class SalesforceSelectBuilder {

    private static final Logger logger = LoggerFactory.getLogger(SalesforceSelectBuilder.class);

    private String sObject;
    private List<String> idSet;
    List<String> fields;
    private List<AbstractMap.SimpleEntry<String, List<String>>> relationFields;

    private void init() {
        if (idSet == null) {
            idSet = new ArrayList<>();
            fields = new ArrayList<>();
            relationFields = new ArrayList<>();
        }
    }

    public SalesforceSelectBuilder setsObject(String sObject) {
        this.sObject = sObject;
        return this;
    }

    public SalesforceSelectBuilder addId(String id) {
        String i = id == null ? "" : id.trim();
        if (i.length() > 0) {
            init();
            idSet.add(i);
        }
        return this;
    }

    public SalesforceSelectBuilder addId(List<String> ids) {
        List<String> list = getNonEmpty(ids);
        if (!list.isEmpty()) {
            init();
            idSet.addAll(list);
        }
        return this;
    }

    public SalesforceSelectBuilder addField(String field) {
        String f = field == null ? "" : field.trim();
        if (f.length() > 0) {
            init();
            fields.add(f);
        }
        return this;
    }

    public SalesforceSelectBuilder addFields(List<String> fields) {
        List<String> list = getNonEmpty(fields);
        if (!list.isEmpty()) {
            init();
            this.fields.addAll(list);
        }
        return this;
    }

    public SalesforceSelectBuilder addRelationField(String rel, String field) {
        String r = rel == null ? "" : rel.trim();
        String f = field == null ? "" : field.trim();
        if (r.length() > 0 && f.length() > 0) {
            init();
            relationFields.add(new AbstractMap.SimpleEntry<>(r, Arrays.asList(f)));
        }
        return this;
    }

    public SalesforceSelectBuilder addRelationFields(String rel, List<String> fields) {
        String r = rel == null ? "" : rel.trim();
        if (r.length() > 0 && fields != null) {
            List<String> f = getNonEmpty(fields);
            if (!f.isEmpty()) {
                init();
                relationFields.add(new AbstractMap.SimpleEntry<>(r, f));
            }
        }
        return this;
    }

    public String build() {
        init();
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
        fld.addAll(relationFields.stream().map(e -> join(e)).flatMap(Collection::stream).collect(Collectors.toList()));
        String query = "select " + String.join(", ", fld) + " from " + sObject;
        if (!idSet.isEmpty()) {
            if (idSet.size() == 1) {
                query += " where id = '" + idSet.get(0) + "'";
            } else {
                query += " where id in [" + String.join(", ", idSet.stream().map(i -> "'" + i + "'").collect(Collectors.toList())) + "]";
            }
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

    private List<String> join(AbstractMap.SimpleEntry<String, List<String>> entry) {
        String rel = entry.getKey();
        return entry.getValue().stream().map(v -> rel + "." + v).collect(Collectors.toList());
    }

}
