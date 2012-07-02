package pl.com.it_crowd.cra.model;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pl.com.it_crowd.cra.scanner.QANote;
import pl.com.it_crowd.cra.scanner.QANoteScanner;
import pl.com.it_crowd.youtrack.api.IssueWrapper;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

//TODO would be great to first save all edited files to disk and then run the scanner
@com.intellij.openapi.components.State(
    name = QANoteManager.COMPONENT_NAME,
    storages = {@Storage(
        file = "$PROJECT_FILE$")})
public class QANoteManager implements ProjectComponent, PersistentStateComponent<QANoteManager.State> {
// ------------------------------ FIELDS ------------------------------

    public static final String COMPONENT_NAME = "QANoteManager";

    public static final String QA_NOTES_PROPERTY = "qaNotes";

    public static final String SELECTED_NOTE = "selectedNote";

    private PropertyChangeSupport changeSupport = new PropertyChangeSupport(this);

    private String defaultAuthor;

    private String defaultRevision;

    private Project project;

    private PropertyChangeListener qaNotePropertyChangeListener = new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent evt)
        {
            final QANote source = (QANote) evt.getSource();
            final String title = "Problem Saving QANote to File (" + evt.getPropertyName() + " changed)";
            try {
//                TODO we should not sync to file from here but create dedicated action
//                TODO problems should be logged differently
                if (!scanner.syncToFile(source, true)) {
                    Messages.showWarningDialog(String.format("QANote %s not found in file %s", source.toString(), source.getFileName()), title);
                }
            } catch (FileNotFoundException e) {
                Messages.showWarningDialog(String.format("File %s not found for note %s", source.getFileName(), source.toString()), title);
            } catch (IOException e) {
                Messages.showWarningDialog(String.format("Problem saving note %s to file %s", source.toString(), source.getFileName()), title);
            }
        }
    };

    private final List<QANote> qaNotes = new ArrayList<QANote>();

    private QANoteScanner scanner;

    private QANote selectedNote;

    private final List<QANote> unmodifiableQANotes;

// --------------------------- CONSTRUCTORS ---------------------------

    public QANoteManager(Project project)
    {
        this.project = project;
        unmodifiableQANotes = Collections.unmodifiableList(qaNotes);
    }

// --------------------- GETTER / SETTER METHODS ---------------------

    public String getDefaultAuthor()
    {
        return defaultAuthor;
    }

    public void setDefaultAuthor(String defaultAuthor)
    {
        this.defaultAuthor = defaultAuthor;
    }

    public String getDefaultRevision()
    {
        return defaultRevision;
    }

    public void setDefaultRevision(String defaultRevision)
    {
        this.defaultRevision = defaultRevision;
    }

    public Project getProject()
    {
        return project;
    }

    @Nullable
    public QANote getSelectedNote()
    {
        return selectedNote;
    }

// ------------------------ INTERFACE METHODS ------------------------


// --------------------- Interface BaseComponent ---------------------

    public void initComponent()
    {
//TODO setup scanner with all source folders
    }

    public void disposeComponent()
    {

    }

// --------------------- Interface NamedComponent ---------------------

    @NotNull
    public String getComponentName()
    {
        return "QANoteManager";
    }

// --------------------- Interface PersistentStateComponent ---------------------

    public State getState()
    {
        return new State(defaultAuthor);
    }

    public void loadState(State state)
    {
        this.defaultAuthor = state.defaultAuthor;
    }

// --------------------- Interface ProjectComponent ---------------------

    public void projectOpened()
    {
        removeNotes();
    }

    public void projectClosed()
    {
        try {
            finish();
        } catch (IOException e) {
            throw new RuntimeException(e);
            //todo find better way of logging this exception
        }
    }

// -------------------------- OTHER METHODS --------------------------

    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener)
    {
        changeSupport.addPropertyChangeListener(propertyName, listener);
    }

    /**
     * Starting point
     */
    public void adjustQANotesCode()
    {
        /**
         * In order to reset noteId sequence generator and not expose such method out we create new instance of scanner
         */
        scanner = new QANoteScanner();
        scanner.setDefaultAuthor(getDefaultAuthor());
        scanner.setRootPath(project.getBasePath());
        //TODO save all opened files to disk before adjusting
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Adjusting QA notes code") {
            public void run(@NotNull ProgressIndicator progressIndicator)
            {
//                TODO use progressIndicator
                final File directoryToScan = new File(getProject().getBasePath());
                scanner.setDirectoryToScan(directoryToScan);
                scanner.adjustQANotes();
            }

            @Override
            public void onCancel()
            {
                loadQANotesFromCode();
            }

            @Override
            public void onSuccess()
            {
                loadQANotesFromCode();
            }
        });
        //TODO synchronize all opened files with disk
    }

    public void createTicket(QANote note)
    {
        if (!StringUtils.isBlank(note.getTicket())) {
            throw new IllegalStateException("QANote already has assigned ticket: " + note.getTicket());
        }
        final IssueWrapper ticket = YoutrackTicketManager.getInstance(project).createTicket(note.getDescription(), note.getFileName());
        note.setTicket(ticket.getId());
    }

    public void finish() throws IOException
    {
        synchronized (qaNotes) {
            for (QANote note : qaNotes) {
                scanner.syncToFile(note, false);
            }
        }
        removeNotes();
    }

    public File getFile(QANote note) throws FileNotFoundException
    {
        return scanner.getFile(note);
    }

    public List<QANote> getQANotes()
    {
        return qaNotes;
    }

    public void selectNote(@Nullable QANote note)
    {
        final QANote oldValue = this.selectedNote;
        this.selectedNote = note;
        changeSupport.firePropertyChange(SELECTED_NOTE, oldValue, note);
    }

    /**
     * Adds QA note and attaches property change listener.
     * Remember to synchronize on #qaNotes before calling this method.
     *
     * @param note note to add
     */
    private void addQANote(QANote note)
    {
        note.addPropertyChangeListener(qaNotePropertyChangeListener);
        qaNotes.add(note);
    }

    private void loadQANotesFromCode()
    {
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Looking for QA notes") {
            public void run(@NotNull ProgressIndicator progressIndicator)
            {
//                TODO use progressIndicator
                final File directoryToScan = new File(getProject().getBasePath());
                scanner.setDirectoryToScan(directoryToScan);
                removeNotes();
                synchronized (qaNotes) {
                    for (QANote note : scanner.getQANotes()) {
                        addQANote(note);
                    }
                }
                changeSupport.firePropertyChange(QA_NOTES_PROPERTY, null, unmodifiableQANotes);
            }
        });
    }

    private void removeNotes()
    {
        synchronized (qaNotes) {
            for (Iterator<QANote> iterator = qaNotes.iterator(); iterator.hasNext(); ) {
                QANote note = iterator.next();
                note.removePropertyChangeListener(qaNotePropertyChangeListener);
                iterator.remove();
            }
        }
    }

// -------------------------- INNER CLASSES --------------------------

    public static class State {
// ------------------------------ FIELDS ------------------------------

        public String defaultAuthor;

// --------------------------- CONSTRUCTORS ---------------------------

        public State()
        {
        }

        public State(String defaultAuthor)
        {
            this();
            this.defaultAuthor = defaultAuthor;
        }
    }
}
