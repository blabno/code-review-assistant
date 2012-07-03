package pl.com.it_crowd.cra.model;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pl.com.it_crowd.cra.scanner.QANote;
import pl.com.it_crowd.cra.scanner.QANoteScanner;
import pl.com.it_crowd.cra.youtrack.QACommand;
import pl.com.it_crowd.cra.youtrack.QANoteTypeValues;
import pl.com.it_crowd.youtrack.api.IssueWrapper;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
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

    private Long defaultRevision;

    private Project project;

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

    public Long getDefaultRevision()
    {
        return defaultRevision;
    }

    public void setDefaultRevision(Long defaultRevision)
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
//TODO find better way of logging this exception
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
//TODO use progressIndicator
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

    public void createTicket(final QANote note) throws IOException
    {
        if (!StringUtils.isBlank(note.getTicket())) {
            throw new IllegalArgumentException("QANote already has ticket assigned: " + note.getTicket());
        }
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Creating Youtrack ticket") {
            public void run(@NotNull ProgressIndicator progressIndicator)
            {
                final YoutrackTicketManager ticketManager = YoutrackTicketManager.getInstance(project);
                final IssueWrapper ticket = ticketManager.createTicket(note.getDescription(), note.getFileName());
                note.setTicket(ticket.getId());
            }

            @Override
            public void onCancel()
            {
                try {
                    updateTicket(note);
                } catch (IOException e) {
                    throw new RuntimeException("Problem updating ticket", e);
                }
            }

            @Override
            public void onSuccess()
            {
                try {
                    updateTicket(note);
                } catch (IOException e) {
                    throw new RuntimeException("Problem updating ticket", e);
                }
            }
        });
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

    public void saveNote(QANote note)
    {
        try {
//TODO problems should be logged differently
            if (!scanner.syncToFile(note, true)) {
                throw new SyncToFileException(String.format("QANote %s not found in file %s", note.toString(), note.getFileName()));
            }
        } catch (FileNotFoundException e) {
            throw new SyncToFileException(String.format("File %s not found for note %s", note.getFileName(), note.toString()), e);
        } catch (IOException e) {
            throw new SyncToFileException(String.format("Problem saving note %s to file %s", note.toString(), note.getFileName()), e);
        }
        if (!StringUtils.isBlank(note.getTicket())) {
            try {
                updateTicket(note);
            } catch (IOException e) {
                throw new SyncToYoutrackException(String.format("Problem updating Youtrack ticket for note %s", note.toString()), e);
            }
        }
    }

    public void selectNote(@Nullable QANote note)
    {
        final QANote oldValue = this.selectedNote;
        this.selectedNote = note;
        changeSupport.firePropertyChange(SELECTED_NOTE, oldValue, note);
    }

    public void updateTicket(final QANote note) throws IOException
    {
        if (StringUtils.isBlank(note.getTicket())) {
            throw new IllegalArgumentException("QANote must have ticket specified");
        }
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Creating Youtrack ticket") {
            public void run(@NotNull ProgressIndicator progressIndicator)
            {
                final YoutrackTicketManager ticketManager = YoutrackTicketManager.getInstance(project);
                final QACommand command = QACommand.qaNoteTypeCommand(QANoteTypeValues.valueOf(note));
                if (!StringUtils.isBlank(note.getRecipient())) {
                    command.assignee(note.getRecipient());
                }
                if (note.getRevision() != null) {
                    command.revision(note.getRevision());
                }
                try {
                    //TODO set rule
                    ticketManager.updateTicket(note.getTicket(), command.toString());
                } catch (IOException e) {
                    throw new RuntimeException("Problems updating Youtrack ticket", e);
                }
            }
        });
    }

    /**
     * Adds QA note and attaches property change listener.
     * Remember to synchronize on #qaNotes before calling this method.
     *
     * @param note note to add
     */
    private void addQANote(QANote note)
    {
        qaNotes.add(note);
    }

    private void loadQANotesFromCode()
    {
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Looking for QA notes") {
            public void run(@NotNull ProgressIndicator progressIndicator)
            {
//TODO use progressIndicator
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
            qaNotes.clear();
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
