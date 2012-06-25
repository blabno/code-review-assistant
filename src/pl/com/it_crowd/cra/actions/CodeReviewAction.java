package pl.com.it_crowd.cra.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.AbstractVcs;
import com.intellij.openapi.vcs.FilePathImpl;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.intellij.openapi.vcs.actions.VcsContext;
import com.intellij.openapi.vcs.diff.DiffProvider;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.Nullable;
import pl.com.it_crowd.cra.ui.RevisionRangeDialog;

import java.awt.Dimension;
import java.awt.Toolkit;

public class CodeReviewAction extends AnAction {
// -------------------------- STATIC METHODS --------------------------

    @Nullable
    protected static AbstractVcs isEnabled(final VcsContext vcsContext)
    {
        if (!(isVisible(vcsContext))) {
            return null;
        }

        final Project project = vcsContext.getProject();
        if (project == null) {
            return null;
        }
        final ProjectLevelVcsManager vcsManager = ProjectLevelVcsManager.getInstance(project);

        final VirtualFile[] selectedFilePaths = vcsContext.getSelectedFiles();
        if (selectedFilePaths == null || selectedFilePaths.length != 1) {
            return null;
        }

        final VirtualFile selectedFile = selectedFilePaths[0];
        if (selectedFile.isDirectory()) {
            return null;
        }

        final AbstractVcs vcs = vcsManager.getVcsFor(selectedFile);
        if (vcs == null) {
            return null;
        }

        final DiffProvider diffProvider = vcs.getDiffProvider();

        if (diffProvider == null) {
            return null;
        }

        if (AbstractVcs.fileInVcsByFileStatus(project, new FilePathImpl(selectedFile))) {
            return vcs;
        }
        return null;
    }

    protected static boolean isVisible(final VcsContext vcsContext)
    {
        final Project project = vcsContext.getProject();
        if (project == null) {
            return false;
        }
        final AbstractVcs[] vcss = ProjectLevelVcsManager.getInstance(project).getAllActiveVcss();
        for (AbstractVcs vcs : vcss) {
            if (vcs.getDiffProvider() != null) {
                return true;
            }
        }
        return false;
    }

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
        final boolean visible = project != null && ProjectLevelVcsManager.getInstance(project).getAllActiveVcss().length > 0;
        e.getPresentation().setVisible(visible);
    }
}
