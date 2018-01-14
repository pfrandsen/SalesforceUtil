package dk.pfrandsen.salesforce.soql;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class SalesforceSelectBuilderTest {

    @Test
    public void testNotInitializedFields() {
        SalesforceSelectBuilder builder = new SalesforceSelectBuilder();
        assertNull(builder.build());
        assertNull(builder.setsObject("Account").build());
    }

    @Test
    public void testNotInitializedSObject() {
        SalesforceSelectBuilder builder = new SalesforceSelectBuilder();
        assertNull(builder.build());
        assertNull(builder.addField("xyz").build());
        assertNull(builder.addRelationField("rel", "fld").build());
    }

    @Test
    public void testQuery() {
        SalesforceSelectBuilder builder = new SalesforceSelectBuilder();
        builder.setsObject("Account").addField("fld");
        assertEquals("select fld from Account", builder.build());
        builder.addId("x");
        assertEquals("select fld from Account where id = 'x'", builder.build());
        builder.addId("y", "z");
        assertEquals("select fld from Account where id in ['x', 'y', 'z']", builder.build());
    }

    @Test
    public void testRelFields() {
        SalesforceSelectBuilder builder = new SalesforceSelectBuilder();
        builder.setsObject("Account").addFields(Arrays.asList("f1", "f2"));
        assertEquals("select f1, f2 from Account", builder.build());
        builder.addRelationFields("r", Arrays.asList("f3", "f4"));
        assertEquals("select f1, f2, r.f3, r.f4 from Account", builder.build());
        builder.addRelationField("m", "f5");
        assertEquals("select f1, f2, r.f3, r.f4, m.f5 from Account", builder.build());
        builder.addId("x");
        assertEquals("select f1, f2, r.f3, r.f4, m.f5 from Account where id = 'x'", builder.build());
        builder.addId("y");
        assertEquals("select f1, f2, r.f3, r.f4, m.f5 from Account where id in ['x', 'y']", builder.build());
    }

    @Test
    public void testLimit() {
        SalesforceSelectBuilder builder = new SalesforceSelectBuilder();
        builder.setsObject("Account").addFields(Arrays.asList("f1", "f2")).setLimit(3);
        assertEquals("select f1, f2 from Account limit = 3", builder.build());
    }

}
