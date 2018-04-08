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
        builder.addId("y", "z", " y ", "z", " x");
        assertEquals("select fld from Account where id in ['x', 'y', 'z']", builder.build());
    }

    @Test
    public void testQueryRemovePrefix() {
        SalesforceSelectBuilder builder = new SalesforceSelectBuilder("Pre_", null);
        builder.setsObject("Pre_Account").addField("Pre_fld");
        assertEquals("select fld from Account", builder.build());
        // prefix not removed from id's
        builder.addId("Pre_x");
        assertEquals("select fld from Account where id = 'Pre_x'", builder.build());
        builder.addId("Pre_y", "z");
        assertEquals("select fld from Account where id in ['Pre_x', 'Pre_y', 'z']", builder.build());
    }

    @Test
    public void testQueryReplacePrefix() {
        SalesforceSelectBuilder builder = new SalesforceSelectBuilder("P_", "R_");
        builder.setsObject("P_Account").addField("P_fld");
        assertEquals("select R_fld from R_Account", builder.build());
        // prefix not removed from id's
        builder.addId("P_x");
        assertEquals("select R_fld from R_Account where id = 'P_x'", builder.build());
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
    public void testRelFieldsRepeatValue() {
        SalesforceSelectBuilder builder = new SalesforceSelectBuilder();
        builder.setsObject("Account");
        builder.addRelationFields("r", Arrays.asList("f1", "f2"));
        assertEquals("select r.f1, r.f2 from Account", builder.build());
        builder.addRelationFields("r", Arrays.asList("f2"));
        assertEquals("select r.f1, r.f2 from Account", builder.build());
        builder.addRelationFields("rr", Arrays.asList("fx"));
        assertEquals("select r.f1, r.f2, rr.fx from Account", builder.build());
        builder.addRelationFields("r", Arrays.asList("fx"));
        assertEquals("select r.f1, r.f2, r.fx, rr.fx from Account", builder.build());
    }

    @Test
    public void testRelFieldsMultilevel() {
        SalesforceSelectBuilder builder = new SalesforceSelectBuilder();
        builder.setsObject("p_Custom");
        builder.addRelationFields("p_rx", "f1", "f2");
        assertEquals("select p_rx.f1, p_rx.f2 from p_Custom", builder.build());
        builder.addRelationFields(Arrays.asList("p_ry", "p_rz"), "f2", "f1");
        assertEquals("select p_rx.f1, p_rx.f2, p_ry.p_rz.f2, p_ry.p_rz.f1 from p_Custom", builder.build());
        builder.addRelationFields(Arrays.asList("p_ry", "p_rz"), Arrays.asList("f2", "fx"));
        assertEquals("select p_rx.f1, p_rx.f2, p_ry.p_rz.f2, p_ry.p_rz.f1, p_ry.p_rz.fx from p_Custom", builder.build());
    }

    @Test
    public void testRelFieldsMultilevelNSReplace() {
        SalesforceSelectBuilder builder = new SalesforceSelectBuilder("p_", "o__");
        builder.setsObject("p_Custom").addField("p_fy");
        builder.addRelationFields("p_rx", "f1", "f2");
        assertEquals("select o__fy, o__rx.f1, o__rx.f2 from o__Custom", builder.build());
        builder.addRelationFields(Arrays.asList("p_ry", "p_rz"), "f2", "f1");
        assertEquals("select o__fy, o__rx.f1, o__rx.f2, o__ry.o__rz.f2, o__ry.o__rz.f1 from o__Custom", builder.build());
        builder.addRelationFields(Arrays.asList("p_ry", "p_rz"), Arrays.asList("f2", "fx"));
        assertEquals("select o__fy, o__rx.f1, o__rx.f2, o__ry.o__rz.f2, o__ry.o__rz.f1, o__ry.o__rz.fx from o__Custom", builder.build());
    }

    @Test
    public void testRelFieldsMultilevelNSRemove() {
        SalesforceSelectBuilder builder = new SalesforceSelectBuilder("p_", null);
        builder.setsObject("p_Custom").addField("p_fy");
        builder.addRelationFields("p_rx", "f1", "f2");
        assertEquals("select fy, rx.f1, rx.f2 from Custom", builder.build());
        builder.addRelationFields(Arrays.asList("p_ry", "p_rz"), "f2", "f1");
        assertEquals("select fy, rx.f1, rx.f2, ry.rz.f2, ry.rz.f1 from Custom", builder.build());
        builder.addRelationFields(Arrays.asList("p_ry", "p_rz"), Arrays.asList("f2", "fx"));
        assertEquals("select fy, rx.f1, rx.f2, ry.rz.f2, ry.rz.f1, ry.rz.fx from Custom", builder.build());
    }

    @Test
    public void testLimit() {
        SalesforceSelectBuilder builder = new SalesforceSelectBuilder();
        builder.setsObject("Account").addFields(Arrays.asList("f1", "f2")).setLimit(3);
        assertEquals("select f1, f2 from Account limit = 3", builder.build());
    }

}
