package pl.com.it_crowd.cra.model;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import org.apache.commons.collections.comparators.NullComparator;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pl.com.it_crowd.cra.QANotifications;
import pl.com.it_crowd.cra.scanner.QANote;
import pl.com.it_crowd.cra.scanner.QANoteScanner;
import pl.com.it_crowd.cra.youtrack.QACommand;
import pl.com.it_crowd.cra.youtrack.QANoteTypeValues;
import pl.com.it_crowd.youtrack.api.IssueWrapper;
import pl.com.it_crowd.youtrack.api.rest.User;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

//TODO would be great to first save all edited files to disk and then run the scanner
@com.intellij.openapi.components.State(
    name = QANoteManager.COMPONENT_NAME,
    storages = {@Storage(
        file = "$PROJECT_FILE$")})
public class QANoteManager implements ProjectComponent, PersistentStateComponent<QANoteManager.State> {
// ------------------------------ FIELDS ------------------------------

    public static final String COMPONENT_NAME = "QANoteManager";

    public static final String HIGHLIGHTED_NOTE_PROPERTY = "highlightedNote";

    public static final String QA_NOTES_PROPERTY = "qaNotes";

    public static final String SELECTED_NOTE_PROPERTY = "selectedNote";

    public static final String VALID_ASSIGNEES_PROPERTY = "validAssignees";

    private PropertyChangeSupport changeSupport = new PropertyChangeSupport(this);

    private String defaultAuthor;

    private Long defaultRevision;

    private Project project;

    private final List<QANote> qaNotes = new ArrayList<QANote>();

    private QANoteScanner scanner;

    private QANote selectedNote;

    private final List<QANote> unmodifiableQANotes;

    private List<String> validAssignees;

// -------------------------- STATIC METHODS --------------------------

    public static QANoteManager getInstance(Project project)
    {
        return project.getComponent(QANoteManager.class);
    }

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

    public List<String> getValidAssignees()
    {
        if (validAssignees == null) {
            try {
                final List<User> assignees = YoutrackTicketManager.getInstance(project).getAssignees();
                validAssignees = new ArrayList<String>();
                for (User user : assignees) {
                    validAssignees.add(user.getLogin());
                }
                changeSupport.firePropertyChange(VALID_ASSIGNEES_PROPERTY, null, validAssignees);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return validAssignees;
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

    public void createTickets(final Collection<QANote> notes)
    {
        for (QANote note : notes) {
            if (!StringUtils.isBlank(note.getTicket())) {
                throw new IllegalArgumentException("One of QANotes already has ticket assigned: " + note.getTicket());
            }
        }
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Creating Youtrack tickets") {
            public void run(@NotNull ProgressIndicator progressIndicator)
            {
                final YoutrackTicketManager ticketManager = YoutrackTicketManager.getInstance(project);
                final List<QANote> qaNoteList = new ArrayList<QANote>(notes);
                Collections.sort(qaNoteList, new Comparator<QANote>() {
                    private NullComparator nullComparator = new NullComparator();

                    public int compare(QANote o1, QANote o2)
                    {
                        final int result = nullComparator.compare(o1.getId(), o2.getId());
                        if (result != 0) {
                            return result;
                        } else {
                            return nullComparator.compare(o1.getFileName(), o2.getFileName());
                        }
                    }
                });
                for (int i = 0, qaNoteListSize = qaNoteList.size(); i < qaNoteListSize; i++) {
                    QANote note = qaNoteList.get(i);
                    if (progressIndicator.isCanceled()) {
                        break;
                    }
                    progressIndicator.setFraction(i / (double) notes.size());
                    progressIndicator.setText(note.getFileName());
                    progressIndicator.setText2(String.format("%d. %s", note.getId(), note.getDescription()));
                    final IssueWrapper ticket = ticketManager.createTicket(note.getDescription(), note.getFileName());
                    note.setTicket(ticket.getId());
                    save(note);
                }
            }

            private void save(QANote note)
            {
                try {
                    syncToFile(note);
                } catch (Exception e) {
                    QANotifications.handle(e, "Problem saving note in file", note, project);
                    return;
                }
                try {
                    updateTicket(note);
                } catch (Exception e) {
                    QANotifications.handle(e, "Problem updating youtrack ticket", note, project);
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

    public void highlightSelectedNote()
    {
        changeSupport.firePropertyChange(HIGHLIGHTED_NOTE_PROPERTY, null, getSelectedNote());
    }

    public void refreshValidAssignees()
    {
        validAssignees = null;
    }

    public void saveNote(QANote note)
    {
        syncToFile(note);
        if (!StringUtils.isBlank(note.getTicket())) {
            updateTicketAsynchronously(note);
        }
    }

    public void selectNote(@Nullable QANote note)
    {
        final QANote oldValue = this.selectedNote;
        this.selectedNote = note;
        changeSupport.firePropertyChange(SELECTED_NOTE_PROPERTY, oldValue, note);
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

    private void syncToFile(QANote note)
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
    }

    private void updateTicket(final QANote note) throws IOException
    {
        if (StringUtils.isBlank(note.getTicket())) {
            throw new IllegalArgumentException("QANote must have ticket specified");
        }

        final YoutrackTicketManager ticketManager = YoutrackTicketManager.getInstance(project);
        final QACommand command = QACommand.qaNoteTypeCommand(QANoteTypeValues.valueOf(note));
        if (!StringUtils.isBlank(note.getAssignee())) {
            command.assignee(note.getAssignee());
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

    private void updateTicketAsynchronously(final QANote note)
    {
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Creating Youtrack ticket") {
            public void run(@NotNull ProgressIndicator progressIndicator)
            {
                try {
                    updateTicket(note);
                } catch (Exception e) {
                    QANotifications.handle(e, "Problem updating youtrack ticket", note, project);
                }
            }
        });
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
