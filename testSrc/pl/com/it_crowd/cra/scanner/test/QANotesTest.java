package pl.com.it_crowd.cra.scanner.test;

import org.apache.commons.collections.comparators.NullComparator;
import org.junit.Assert;
import org.junit.Test;
import pl.com.it_crowd.cra.scanner.QANoteConverter;
import pl.com.it_crowd.cra.scanner.QANoteScanner;

import java.util.NoSuchElementException;

public class QANotesTest {
// ------------------------------ FIELDS ------------------------------

    final String codeForIterator = "public class SupplierInvitationHome extends EntityHome<SupplierInvitation> {\n"
        + "// -------------------------- OTHER METHODS --------------------------\n" + "\n" + "    /**\n"
        + "     * QA-REVIEW 1 business components should not be aware of view layer so don't mention in in here\n"
        + "     * Decline supplier invitation for invitationsForMeView\n" + "     * QA-REVIEW missing javadoc\n" + "     * @param invitation\n" + "     */\n"
        + "    public void decline(SupplierInvitation invitation)\n" + "    {\n" + "//        QA-REVIEW 2 this method should operate on getInstance()\n"
        + "        SupplierInvitation supplierInvitationFromDb = getSupplierInvitationById(invitation.getId());\n"
        + "        supplierInvitationFromDb.setInvitationRefusalDate(new Date());\n" + "        setInstance(supplierInvitationFromDb);\n"
        + "//        QA-REVIEW 3 don't ignore result of update method\n" + "        update();\n" + "    }\n" + "\n" + "    /**\n"
        + "     * Return supplier invitations by id\n" + "     * QA-REVIEW 4 missing javadoc\n" + "     * @param supplierInvitationId\n" + "     *\n"
        + "     * @return supplier invitations by id\n" + "     */\n" + "    public SupplierInvitation getSupplierInvitationById(Long supplierInvitationId)\n"
        + "    {\n" + "//        QA-REVIEW 5 remove this method\n" + "        return getEntityManager().find(SupplierInvitation.class, supplierInvitationId);\n"
        + "    }\n" + "\n" + "    /**\n" + "     * Gets list of valid invitations for given project key.\n"
        + "     * Invitation is valid if it's invitation reply date is in future.\n" + "     *\n"
        + "     * @param projectKey project key to identify invitations\n" + "     *\n" + "     * @return list of valid invitations\n" + "     */\n"
        + "    @SuppressWarnings(\"unchecked\")\n" + "    public List<SupplierInvitation> getValidInvitatiosnByProjectKey(String projectKey)\n" + "    {\n"
        + "        return getEntityManager().createQuery(\"select i from SupplierInvitation i where i.project.invitationReplyDeadline>=:now and i.projectKey=:projectKey\")\n"
        + "            .setParameter(\"now\", new Date())\n" + "            .setParameter(\"projectKey\", projectKey)\n" + "            .getResultList();\n"
        + "    }\n" + "}";

// -------------------------- OTHER METHODS --------------------------

    @Test
    public void extractAssignee()
    {
        String comment = "/**\n" + " * QA-SUGGESTION\n" + " * unused method\n" + " * @reporter: bernard\n" + " * @assignee: k.miksa\n" + " */";
        Assert.assertEquals("k.miksa", new QANoteConverter().getAsObject(comment).getAssignee());
    }

    @Test
    public void extractAuthor()
    {
        String comment = "/**\n" + " * QA-SUGGESTION\n" + " * unused method\n" + " * @reporter: bernard\n" + " */";
        Assert.assertEquals("bernard", new QANoteConverter().getAsObject(comment).getReporter());
    }

    @Test
    public void extractDescription()
    {
        String comment = "/**\n" + " * QA-SUGGESTION\n" + " * unused method\n" + " * @reporter: bernard\n" + " * @assignee: k.miksa\n" + " */";
        Assert.assertEquals("unused method", new QANoteConverter().getAsObject(comment).getDescription());
        comment = "/**\n" + " * QA-VIOLATION\n" + " * Assign method result to a variable instead of invoking the method over and over\n" + " * @noteId: 1\n"
            + " * @reporter: bernard\n" + " * @ticket: TEST_QA-26\n" + " * @assignee: k.miksa\n" + " * @revision: 1223\n" + " */";
        Assert.assertEquals("Assign method result to a variable instead of invoking the method over and over",
            new QANoteConverter().getAsObject(comment).getDescription());
    }

    @Test
    public void extractRevision()
    {
        String comment = "/**\n" + " * QA-SUGGESTION\n" + " * unused method\n" + " * @reporter: bernard\n" + " * @revision: 32\n" + " */";
        Assert.assertEquals(new Long(32), new QANoteConverter().getAsObject(comment).getRevision());
    }

    @Test
    public void extractTicket()
    {
        String comment = "/**\n" + " * QA-SUGGESTION\n" + " * unused method\n" + " * @reporter: bernard\n" + " * @ticket: QWE_QA-123\n" + " */";
        Assert.assertEquals("QWE_QA-123", new QANoteConverter().getAsObject(comment).getTicket());
    }

    @Test
    public void iterator()
    {
        QANoteScanner.CommentIterator iterator = new QANoteScanner.CommentIterator(new StringBuilder(codeForIterator));
        Assert.assertTrue(iterator.hasNext());
        Assert.assertTrue(iterator.hasNext());
        iterator.next();
        iterator.next();
        iterator.next();
        iterator.next();
        iterator.next();
        Assert.assertFalse(iterator.hasNext());
        try {
            iterator.next();
            Assert.assertTrue(false);
        } catch (NoSuchElementException ignore) {
        }
    }

    @Test
    public void iteratorReplace()
    {
        final String qaComment1 = "/**\n" + "     * QA-REVIEW 1 business components should not be aware of view layer so don't mention in in here\n"
            + "     * Decline supplier invitation for invitationsForMeView\n" + "     * QA-REVIEW missing javadoc\n" + "     * @param invitation\n" + "     */";
        final String qaComment2 = "//        QA-REVIEW 2 this method should operate on getInstance()";
        final String qaComment3 = "//        QA-REVIEW 3 don't ignore result of update method";
        final String qaComment4 =
            "/**\n" + "     * Return supplier invitations by id\n" + "     * QA-REVIEW 4 missing javadoc\n" + "     * @param supplierInvitationId\n"
                + "     *\n" + "     * @return supplier invitations by id\n" + "     */";
        final String qaComment5 = "//        QA-REVIEW 5 remove this method";
        final StringBuilder code = new StringBuilder(codeForIterator);

        final QANoteScanner.CommentIterator iterator = new QANoteScanner.CommentIterator(code);
        Assert.assertEquals(qaComment1, iterator.next());
        final String newComment = "/** The Change */";
        Assert.assertFalse(code.toString().contains(newComment));
        iterator.replace(newComment);
        Assert.assertTrue(code.toString().contains(newComment));

        final String evenNewerComment = "/** Other Change */";
        iterator.replace(evenNewerComment);
        Assert.assertFalse(code.toString().contains(newComment));
        Assert.assertTrue(code.toString().contains(evenNewerComment));

        Assert.assertEquals(qaComment2, iterator.next());
        Assert.assertTrue(iterator.hasNext());
        iterator.remove();
        Assert.assertTrue(iterator.hasNext());
        Assert.assertEquals(qaComment3, iterator.next());
        iterator.remove();
        Assert.assertEquals(qaComment4, iterator.next());
        Assert.assertEquals(qaComment5, iterator.next());
        final String qaComment5Change = "/** Final change */";
        iterator.replace(qaComment5Change);
        Assert.assertFalse(code.toString().contains(qaComment5));
        Assert.assertTrue(code.toString().contains(qaComment5Change));

        Assert.assertFalse(iterator.hasNext());
        iterator.remove();
        Assert.assertFalse(iterator.hasNext());
        Assert.assertFalse(code.toString().contains(qaComment5Change));
        System.out.println(code);
    }

    @Test
    public void multipleRun()
    {
        String comment = "/**\n" + " * QA-SUGGESTION\n" + " * business components should not be aware of view layer so don't mention in in here\n"
            + "Decline supplier invitation for invitationsForMeView\n" + "missing javadoc\n" + "@param invitation\n" + " * @reporter: bernard\n"
            + " * @revision: 1109\n" + " */";
        String newComment = new QANoteConverter().getAsString(new QANoteScanner().toQANote(comment, false));
        Assert.assertEquals(comment, newComment);
        newComment = new QANoteConverter().getAsString(new QANoteScanner().toQANote(newComment, false));
        Assert.assertEquals(comment, newComment);
        newComment = new QANoteConverter().getAsString(new QANoteScanner().toQANote(newComment, false));
        Assert.assertEquals(comment, newComment);
    }

    @Test
    public void rubish()
    {
        System.out.println(new NullComparator().compare(null, null));
        System.out.println(new NullComparator().compare(null, 1));
        System.out.println(new NullComparator().compare(2, 1));
        System.out.println(new NullComparator().compare(1, 1));
        System.out.println(new NullComparator().compare(2, null));
    }

    @Test
    public void veryHard()
    {
        String code = "package com.qwestmark.test.selenium;\n" + "\n" + "import org.jboss.arquillian.ajocado.locator.IdLocator;\n"
            + "import org.jboss.arquillian.ajocado.locator.XPathLocator;\n" + "\n" + "import static org.jboss.arquillian.ajocado.Graphene.id;\n"
            + "import static org.jboss.arquillian.ajocado.Graphene.xp;\n" + "\n" + "public final class ProjectTestHelper {\n"
            + "// ------------------------------ FIELDS ------------------------------\n" + "\n"
            + "    public static final XPathLocator BUTTON_PARTICIPATE_INVITE = xp(\"//*[contains(./@id, ':participate')]\");\n" + "\n"
            + "//QA-REVIEW 1 comment\n" + "    public static final IdLocator BUTTON_PUBLISH_SUPPLIERS = id(\"sF:sB\");\n" + "\n"
            + "    public static final IdLocator BUTTON_REJECT_ANSWERS = id(\"sAA:rB\");\n" + "\n"
            + "    public static final IdLocator BUTTON_REJECT_RFP = id(\"rAMF:rR\");\n" + "\n"
            + "    public static final IdLocator BUTTON_REJECT_SUPPLIERS = id(\"aVF:rB\");\n" + "\n"
            + "    public static final IdLocator BUTTON_SAVE_ANSWER_Q1 = id(\"f:queR:0:qA:qAS\");\n" + "\n"
            + "    public static final IdLocator BUTTON_SAVE_ANSWER_Q2 = id(\"f:queR:1:qA:qAS\");\n" + "\n"
            + "    public static final IdLocator BUTTON_SAVE_DEFINE_TEAM = id(\"sPTF:s\");\n" + "\n"
            + "    public static final IdLocator BUTTON_SAVE_SUPPLIERS = id(\"sLF:s\");\n" + "\n"
            + "    public static final IdLocator BUTTON_SUBMIT_ANSWERS_FOR_APPROVAL = id(\"f:sAFA\");\n" + "\n"

            + "// --------------------------- CONSTRUCTORS ---------------------------\n" + "\n" + "    private ProjectTestHelper()\n" + "    {\n" + "    }\n"
            + "}\n";
        final QANoteScanner.CommentIterator iterator = new QANoteScanner.CommentIterator(new StringBuilder(code));
        int i = 0;
        while (iterator.hasNext()) {
            iterator.next();
            i++;
        }
        Assert.assertEquals(1, i);
    }
}
