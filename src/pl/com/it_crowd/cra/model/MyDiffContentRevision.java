package pl.com.it_crowd.cra.model;

import com.intellij.openapi.util.io.BufferExposingByteArrayOutputStream;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vfs.CharsetToolkit;
import com.intellij.vcsUtil.VcsUtil;
import org.jetbrains.idea.svn.status.DiffContentRevision;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.io.SVNRepository;

import java.nio.charset.Charset;

public class MyDiffContentRevision extends DiffContentRevision {
// ------------------------------ FIELDS ------------------------------

    private String myContents;

    private FilePath myFilePath;

    private String myPath;

    private SVNRepository myRepository;

    private long revision;

// --------------------------- CONSTRUCTORS ---------------------------

    public MyDiffContentRevision(String path, @org.jetbrains.annotations.NotNull SVNRepository repos, long revision)
    {
        super(path, repos, revision);
        this.myRepository = repos;
        this.revision = revision;
        this.myPath = path;
        this.myFilePath = VcsUtil.getFilePath(path);
    }

// ------------------------ INTERFACE METHODS ------------------------


// --------------------- Interface ContentRevision ---------------------

    public String getContent() throws VcsException
    {
        if (myContents == null) {
            BufferExposingByteArrayOutputStream bos = new BufferExposingByteArrayOutputStream(2048);
            try {
                myRepository.getFile(myPath, revision, null, bos);
                myRepository.closeSession();
            } catch (SVNException e) {
                throw new VcsException(e);
            }
            final byte[] bytes = bos.toByteArray();
            final Charset charset = myFilePath.getCharset();
            myContents = charset == null ? CharsetToolkit.bytesToString(bytes) : CharsetToolkit.bytesToString(bytes, charset);
        }
        return myContents;
    }
}
