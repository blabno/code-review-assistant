package pl.com.it_crowd.cra.model;

import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ContentRevision;

import java.io.File;

public final class AssistantHelper {
// -------------------------- STATIC METHODS --------------------------

    public static boolean affectsFile(Change change, File ioFile)
    {
        final ContentRevision beforeRevision = change.getBeforeRevision();
        final ContentRevision afterRevision = change.getAfterRevision();
        final String ioFileAbsolutePath = ioFile.getAbsolutePath();
        return beforeRevision != null && ioFileAbsolutePath.endsWith(beforeRevision.getFile().getPath())
            || afterRevision != null && ioFileAbsolutePath.endsWith(afterRevision.getFile().getPath());
    }

    // --------------------------- CONSTRUCTORS ---------------------------

    private AssistantHelper()
    {
    }
}
