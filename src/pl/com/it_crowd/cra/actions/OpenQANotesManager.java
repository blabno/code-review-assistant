package pl.com.it_crowd.cra.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import pl.com.it_crowd.cra.model.QANoteManager;
import pl.com.it_crowd.cra.ui.QANotesManagementForm;

public class OpenQANotesManager extends AnAction {
// -------------------------- OTHER METHODS --------------------------

    @Override
    public void actionPerformed(AnActionEvent event)
    {
        final Project project = event.getProject();
        if (project == null) {
            Messages.showWarningDialog("Cannot obtain project from AnActionEvent", "Invalid State");
            return;
        }
        project.getComponent(QANoteManager.class).adjustQANotesCode();
        QANotesManagementForm.show(project);
    }
}
