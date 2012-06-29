package pl.com.it_crowd.cra.model;

import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pl.com.it_crowd.cra.scanner.QANote;
import pl.com.it_crowd.cra.scanner.QANoteScanner;

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
public class QANoteManager implements ProjectComponent {
// ------------------------------ FIELDS ------------------------------

    public static final String QA_NOTES_PROPERTY = "qaNotes";

    public static final String SELECTED_NOTE = "selectedNote";

    public void setDefaultAuthor(String defaultAuthor)
    {
        this.defaultAuthor = defaultAuthor;
    }

    private String defaultAuthor;

    private String defaultRevision;

    public String getDefaultRevision()
    {
        return defaultRevision;
    }

    public void setDefaultRevision(String defaultRevision)
    {
        this.defaultRevision = defaultRevision;
    }

    private PropertyChangeSupport changeSupport = new PropertyChangeSupport(this);

    private final List<QANote> qaNotes = new ArrayList<QANote>();

    private final List<QANote> unmodifiableQANotes;

    private QANoteScanner scanner;

    private QANote selectedNote;

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

    public QANoteManager()
    {
        unmodifiableQANotes = Collections.unmodifiableList(qaNotes);
    }
    // --------------------- GETTER / SETTER METHODS ---------------------

    public String getDefaultAuthor()
    {
        return defaultAuthor;
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

// --------------------- Interface ProjectComponent ---------------------

    public void projectOpened()
    {
        removeNotes();
    }

    private void loadQANotesFromCode()
    {
        ProgressManager.getInstance().run(new Task.Backgroundable(getProject(), "Looking for QA notes") {
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

    public File getFile(QANote note) throws FileNotFoundException
    {
        return scanner.getFile(note);
    }

    public Project getProject()
    {
        //        TODO get project in better way
        return ProjectManager.getInstance().getOpenProjects()[0];
    }

    public List<QANote> getQANotes()
    {
        return qaNotes;
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

    public void selectNote(@Nullable QANote note)
    {
        final QANote oldValue = this.selectedNote;
        this.selectedNote = note;
        changeSupport.firePropertyChange(SELECTED_NOTE, oldValue, note);
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
        scanner.setRootPath(getProject().getBasePath());
        //TODO save all opened files to disk before adjusting
        ProgressManager.getInstance().run(new Task.Backgroundable(getProject(), "Adjusting QA notes code") {
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

    public void finish() throws IOException
    {
        synchronized (qaNotes) {
            for (QANote note : qaNotes) {
                scanner.syncToFile(note, false);
            }
        }
        removeNotes();
    }
}
