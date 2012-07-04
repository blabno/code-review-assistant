package pl.com.it_crowd.cra.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.jetbrains.idea.svn.SvnVcs;
import pl.com.it_crowd.cra.ui.RevisionRangeDialog;

import java.awt.Dimension;
import java.awt.Toolkit;

public class CodeReviewAction extends AnAction {
// ------------------------------ FIELDS ------------------------------

    public static String ACTION_ID = "CodeReview";

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
        final Project project = e.getProject();
        final boolean visible = project != null && SvnVcs.getInstance(project) != null;
        e.getPresentation().setEnabledAndVisible(visible);
    }
}
