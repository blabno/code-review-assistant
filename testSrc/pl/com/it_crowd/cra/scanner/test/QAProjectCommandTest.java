package pl.com.it_crowd.cra.scanner.test;

import junit.framework.Assert;
import org.junit.Test;
import pl.com.it_crowd.cra.youtrack.QACommand;
import pl.com.it_crowd.cra.youtrack.QANoteTypeValues;
import pl.com.it_crowd.youtrack.api.defaults.StateValues;

public class QAProjectCommandTest {
// -------------------------- OTHER METHODS --------------------------

    @Test
    public void qaNoteTypeCommand()
    {
        Assert.assertEquals("qa note type Suggestion assignee Tomek", QACommand.qaNoteTypeCommand(QANoteTypeValues.Suggestion).assignee("Tomek").toString());
        Assert.assertEquals("revision 123 qa note type Suggestion", QACommand.revisionCommand(123L).qaNoteType(QANoteTypeValues.Suggestion).toString());
    }

    @Test
    public void revisionCommand()
    {
        Assert.assertEquals("revision 321 assignee Tomek", QACommand.revisionCommand(321L).assignee("Tomek").toString());
        Assert.assertEquals("rule No unused members revision 123", QACommand.ruleCommand("No unused members").revision(123L).toString());
    }

    @Test
    public void ruleCommand()
    {
        Assert.assertEquals("rule No unused members state Fixed", QACommand.ruleCommand("No unused members").state(StateValues.Fixed).toString());
        Assert.assertEquals("source file index.html rule No unused members", QACommand.sourceFileCommand("index.html").rule("No unused members").toString());
    }

    @Test
    public void sourceFileCommand()
    {
        Assert.assertEquals("source file src/main/java/User.java assignee Tomek",
            QACommand.sourceFileCommand("src/main/java/User.java").assignee("Tomek").toString());
        Assert.assertEquals("rule No unused members source file theme.css", QACommand.ruleCommand("No unused members").sourceFile("theme.css").toString());
    }
}
