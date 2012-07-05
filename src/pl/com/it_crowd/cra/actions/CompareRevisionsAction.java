package pl.com.it_crowd.cra.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.intellij.openapi.vcs.actions.VcsContext;
import com.intellij.openapi.vcs.actions.VcsContextFactory;
import pl.com.it_crowd.cra.ui.RevisionRangeDialog;

import java.awt.Dimension;
import java.awt.Toolkit;

public class CompareRevisionsAction extends AnAction {
// ------------------------------ FIELDS ------------------------------

    public static String ACTION_ID = "CompareRevisions";

// -------------------------- OTHER METHODS --------------------------

    @Override
    public void actionPerformed(AnActionEvent event)
    {
        final RevisionRangeDialog dialog = new RevisionRangeDialog(event.getProject());
        final Toolkit toolkit = Toolkit.getDefaultToolkit();
        final Dimension screenSize = toolkit.getScreenSize();
        final int x = (screenSize.width - dialog.getWidth()) / 2;
        final int y = (screenSize.height - dialog.getHeight()) / 2;
        dialog.setLocation(x, y);
        dialog.pack();
        dialog.setVisible(true);
    }

    @Override
    public void update(AnActionEvent e)
    {
        final VcsContext vcsContext = VcsContextFactory.SERVICE.getInstance().createContextOn(e);
        final Presentation presentation = e.getPresentation();
        Project project = vcsContext.getProject();
        if (project == null) {
            presentation.setEnabled(false);
            presentation.setVisible(false);
            return;
        }
        final ProjectLevelVcsManager plVcsManager = ProjectLevelVcsManager.getInstance(project);
        if (!plVcsManager.hasActiveVcss()) {
            presentation.setEnabled(false);
            presentation.setVisible(false);
            return;
        }
        presentation.setEnabled(!plVcsManager.isBackgroundVcsOperationRunning());
        presentation.setVisible(true);
    }
}
