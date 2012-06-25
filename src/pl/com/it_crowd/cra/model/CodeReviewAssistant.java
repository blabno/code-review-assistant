package pl.com.it_crowd.cra.model;

import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.changes.Change;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.idea.svn.SvnVcs;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.wc.ISVNDiffStatusHandler;
import org.tmatesoft.svn.core.wc.SVNDiffStatus;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNStatusType;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CodeReviewAssistant implements ProjectComponent {
// ------------------------------ FIELDS ------------------------------

    public static final String CHANGED_FILES_PROPERTY = "changedFiles";

    public static final String CURRENT_FILE_PROPERTY = "currentFile";

    public static final String REVISION_RANGE_PROPERTY = "revisionRange";

    private final PropertyChangeSupport changeSupport = new PropertyChangeSupport(this);

    private final List<File> changedFiles = new ArrayList<File>();

    private List<Change> changes = new ArrayList<Change>();

    private File currentFile;

    private final CollectingDiffStatusHandler diffStatusHandler = new CollectingDiffStatusHandler();

    private Project project;

    private RevisionRange revisionRange;

    private final List<File> unmodifiableChangedFilesView;

// -------------------------- STATIC METHODS --------------------------

    private static Change createChange(SvnVcs vcs, File file, int startRevision, int endRevision)
    {
        Change change;
        final String relativePath = AssistantHelper.getRelativePath(file, vcs).substring(1);
        final SVNRepository repository;
        try {
            repository = vcs.createRepository(vcs.getInfo(file).getRepositoryRootURL());
        } catch (SVNException ex) {
            throw new RuntimeException(ex);
        }
        change = new Change(new MyDiffContentRevision(relativePath, repository, startRevision),
            new MyDiffContentRevision(relativePath, repository, endRevision));
        return change;
    }

// --------------------------- CONSTRUCTORS ---------------------------

    public CodeReviewAssistant(Project project)
    {
        this.project = project;
        unmodifiableChangedFilesView = Collections.unmodifiableList(changedFiles);
    }

// --------------------- GETTER / SETTER METHODS ---------------------

    public List<Change> getChanges()
    {
        return changes;
    }

    public File getCurrentFile()
    {
        return currentFile;
    }

    public RevisionRange getRevisionRange()
    {
        return revisionRange;
    }

// ------------------------ INTERFACE METHODS ------------------------


// --------------------- Interface BaseComponent ---------------------

    public void initComponent()
    {
    }

    public void disposeComponent()
    {
    }

// --------------------- Interface NamedComponent ---------------------

    @NotNull
    public String getComponentName()
    {
        return "CodeReviewAssistant";
    }

// --------------------- Interface ProjectComponent ---------------------

    public void projectOpened()
    {
    }

    public void projectClosed()
    {
        changedFiles.clear();
        changeSupport.firePropertyChange(CHANGED_FILES_PROPERTY, null, changedFiles);
    }

// -------------------------- OTHER METHODS --------------------------

    public void addPropertyChangeListener(PropertyChangeListener listener)
    {
        changeSupport.addPropertyChangeListener(listener);
    }

    public List<File> getChangedFiles()
    {
        return unmodifiableChangedFilesView;
    }

    public void selectFile(@Nullable Integer index)
    {
        File oldValue = this.currentFile;
        this.currentFile = index == null ? null : changedFiles.get(index);
        changeSupport.firePropertyChange(CURRENT_FILE_PROPERTY, oldValue, this.currentFile);
    }

    public void setRevisionRange(int startRevision, int endRevision) throws SVNException
    {
        final RevisionRange oldRange = this.revisionRange;
        this.revisionRange = new RevisionRange(startRevision, endRevision);
        changeSupport.firePropertyChange(REVISION_RANGE_PROPERTY, oldRange, this.revisionRange);
        changedFiles.clear();
        changes.clear();
        changeSupport.firePropertyChange(CHANGED_FILES_PROPERTY, null, changedFiles);

        final SvnVcs svnVcs = SvnVcs.getInstance(project);
        final SVNRevision svnRevision = SVNRevision.create(startRevision);
        svnVcs.createDiffClient()
            .doDiffStatus(new File(project.getBasePath()), svnRevision, SVNRevision.create(endRevision), svnRevision, SVNDepth.INFINITY, true,
                diffStatusHandler);
        changeSupport.firePropertyChange(CHANGED_FILES_PROPERTY, null, changedFiles);
        selectFile(null);
    }


// -------------------------- INNER CLASSES --------------------------

    private class CollectingDiffStatusHandler implements ISVNDiffStatusHandler {
// ------------------------ INTERFACE METHODS ------------------------


// --------------------- Interface ISVNDiffStatusHandler ---------------------

        public void handleDiffStatus(SVNDiffStatus svnDiffStatus) throws SVNException
        {
            final SvnVcs vcs = SvnVcs.getInstance(project);
            final int startRevision = getRevisionRange().getStartRevision();
            final int endRevision = getRevisionRange().getEndRevision();
            final SVNStatusType modificationType = svnDiffStatus.getModificationType();
            if (!SVNStatusType.UNCHANGED.equals(modificationType) && !SVNStatusType.STATUS_NONE.equals(modificationType)) {
                changedFiles.add(svnDiffStatus.getFile());
                if (SVNNodeKind.FILE.equals(svnDiffStatus.getKind())) {
                    changes.add(createChange(vcs, svnDiffStatus.getFile(), startRevision, endRevision));
                }
            }
        }
    }

    public static class RevisionRange implements Serializable {
// ------------------------------ FIELDS ------------------------------

        private int endRevision;

        private int startRevision;

// --------------------------- CONSTRUCTORS ---------------------------

        public RevisionRange(int startRevision, int endRevision)
        {
            this.endRevision = endRevision;
            this.startRevision = startRevision;
        }

// --------------------- GETTER / SETTER METHODS ---------------------

        public int getEndRevision()
        {
            return endRevision;
        }

        public int getStartRevision()
        {
            return startRevision;
        }
    }
}
