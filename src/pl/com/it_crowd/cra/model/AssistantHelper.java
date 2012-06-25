package pl.com.it_crowd.cra.model;

import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ContentRevision;
import org.jetbrains.idea.svn.RootUrlInfo;
import org.jetbrains.idea.svn.SvnVcs;

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

    public static String getRelativePath(File file, SvnVcs vcs)
    {
        final RootUrlInfo wcRootForFilePath = vcs.getSvnFileUrlMapping().getWcRootForFilePath(file);
        if (wcRootForFilePath == null) {
            throw new IllegalArgumentException("Cannot get working copy root for file: " + file.getAbsolutePath());
        }
        final int length = wcRootForFilePath.getIoFile().getAbsolutePath().length();
        return file.getAbsolutePath().substring(length);
    }

// --------------------------- CONSTRUCTORS ---------------------------

    private AssistantHelper()
    {
    }
}
