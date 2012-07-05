package pl.com.it_crowd.cra.ui;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import org.apache.commons.lang.StringUtils;
import pl.com.it_crowd.cra.QANotifications;
import pl.com.it_crowd.cra.model.QANoteManager;
import pl.com.it_crowd.cra.model.YoutrackTicketManager;
import pl.com.it_crowd.cra.scanner.QANote;
import pl.com.it_crowd.youtrack.api.rest.User;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import javax.xml.bind.JAXBException;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class QANotesList {
// ------------------------------ FIELDS ------------------------------

    public static final String FILTER_EMPTY_DESCRIPTION_NOTES_PROPERTY = "filterEmptyDscriptionNotes";

    public static final String FILTER_EMPTY_REPORTER_NOTES_PROPERTY = "filterEmptyReporterNotes";

    public static final String FILTER_EMPTY_REVISION_NOTES_PROPERTY = "filterEmptyRevisionNotes";

    public static final String FILTER_INVALID_ASSIGNEE_NOTES_PROPERTY = "filterInvalidAssigneeNotes";

    public static final String FILTER_NO_TICKET_NOTES_PROPERTY = "filterNoTicketotes";

    private PropertyChangeSupport changeSupport = new PropertyChangeSupport(this);

    private Action createTicketsAction = new AbstractAction("Create tickets") {
        public void actionPerformed(ActionEvent e)
        {
            createTickets();
        }
    };

    private boolean filterEmptyDescriptionNotes;

    private boolean filterEmptyReporterNotes;

    private boolean filterEmptyRevisionNotes;

    private boolean filterInvalidAssigneeNotes;

    private boolean filterNoTicketNotes;

    private QANoteManager noteManager;

    private Project project;

    private JPanel rootComponent;

    private JTable table;

    private YoutrackTicketManager ticketManager;

    private List<String> validAssignees;

// --------------------------- CONSTRUCTORS ---------------------------

    public QANotesList(final Project project)
    {
        this.project = project;
        this.noteManager = QANoteManager.getInstance(project);
        this.ticketManager = YoutrackTicketManager.getInstance(project);
        $$$setupUI$$$();
        noteManager.addPropertyChangeListener(QANoteManager.HIGHLIGHTED_NOTE_PROPERTY, new PropertyChangeListener() {
            public void propertyChange(final PropertyChangeEvent evt)
            {
                ApplicationManager.getApplication().invokeLater(new Runnable() {
                    public void run()
                    {
                        table.getSelectionModel().clearSelection();
                        if (evt.getNewValue() != null) {
                            QANotesManagementForm.show(project);
                            final List<QANote> qaNotes = noteManager.getQANotes();
                            for (int i = 0, qaNotesSize = qaNotes.size(); i < qaNotesSize; i++) {
                                QANote note = qaNotes.get(i);
                                if (evt.getNewValue().equals(note)) {
                                    int viewIndex = table.convertRowIndexToView(i);
                                    table.getSelectionModel().setSelectionInterval(viewIndex, viewIndex);
                                    table.requestFocusInWindow();
                                    table.scrollRectToVisible(new Rectangle(table.getCellRect(viewIndex, 0, true)));
                                    return;
                                }
                            }
                            QANotifications.inform("Cannot find selected QANote among available notes!", evt.getNewValue().toString(), project);
                        }
                    }
                });
            }
        });
    }

// --------------------- GETTER / SETTER METHODS ---------------------

    public Action getCreateTicketsAction()
    {
        return createTicketsAction;
    }

    public List<String> getValidAssignees()
    {
        if (validAssignees == null) {
            try {
                validAssignees = new ArrayList<String>();
                final List<User> assignees = ticketManager.getAssignees();
                for (User user : assignees) {
                    validAssignees.add(user.getLogin());
                }
            } catch (JAXBException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return validAssignees;
    }

// -------------------------- OTHER METHODS --------------------------

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$()
    {
        return rootComponent;
    }

    public void addPropertyChangeListener(PropertyChangeListener listener)
    {
        changeSupport.addPropertyChangeListener(listener);
    }

    public void createTickets()
    {
        final ListSelectionModel selectionModel = table.getSelectionModel();
        if (selectionModel.isSelectionEmpty()) {
            Messages.showWarningDialog("Select QANote to create Youtrack ticket for", "No QANote Selected");
            return;
        }
        final ArrayList<QANote> notesToTicketize = new ArrayList<QANote>();
        final List<QANote> skippedNotes = new ArrayList<QANote>();
        final String username = ticketManager.getUsername();
        if (StringUtils.isBlank(username)) {
            Messages.showWarningDialog("Youtrack ticket manager is not configured, please configure it first.", "Create Tickets");
            return;
        }
        for (int index = selectionModel.getMinSelectionIndex(); index <= selectionModel.getMaxSelectionIndex(); index++) {
            if (selectionModel.isSelectedIndex(index)) {
                final QANote note = noteManager.getQANotes().get(table.convertRowIndexToModel(index));
                if (StringUtils.isBlank(note.getTicket()) && username.equals(note.getReporter())) {
                    notesToTicketize.add(note);
                } else {
                    skippedNotes.add(note);
                }
            }
        }
        noteManager.createTickets(notesToTicketize);
        if (!skippedNotes.isEmpty()) {
            final String message = String.format("%d notes were skipped because they have ticket assigned already or are reported by different user then %s",
                skippedNotes.size(), username);
            QANotifications.inform("Some notes were skipped", message, project);
        }
    }

    public void setFilterEmptyDescriptionNotes(boolean filterEmptyDescriptionNotes)
    {
        boolean oldValue = this.filterEmptyDescriptionNotes;
        this.filterEmptyDescriptionNotes = filterEmptyDescriptionNotes;
        changeSupport.firePropertyChange(FILTER_EMPTY_DESCRIPTION_NOTES_PROPERTY, oldValue, this.filterEmptyDescriptionNotes);
    }

    public void setFilterEmptyReporterNotes(boolean filterEmptyReporterNotes)
    {
        boolean oldValue = this.filterEmptyReporterNotes;
        this.filterEmptyReporterNotes = filterEmptyReporterNotes;
        changeSupport.firePropertyChange(FILTER_EMPTY_REPORTER_NOTES_PROPERTY, oldValue, this.filterEmptyReporterNotes);
    }

    public void setFilterEmptyRevisionNotes(boolean filterEmptyRevisionNotes)
    {
        boolean oldValue = this.filterEmptyRevisionNotes;
        this.filterEmptyRevisionNotes = filterEmptyRevisionNotes;
        changeSupport.firePropertyChange(FILTER_EMPTY_REVISION_NOTES_PROPERTY, oldValue, this.filterEmptyRevisionNotes);
    }

    public void setFilterInvalidAssigneeNotes(boolean filterInvalidAssigneeNotes)
    {
        boolean oldValue = this.filterInvalidAssigneeNotes;
        this.filterInvalidAssigneeNotes = filterInvalidAssigneeNotes;
        changeSupport.firePropertyChange(FILTER_INVALID_ASSIGNEE_NOTES_PROPERTY, oldValue, this.filterInvalidAssigneeNotes);
    }

    public void setFilterNoTicketNotes(boolean filterNoTicketNotes)
    {
        boolean oldValue = this.filterNoTicketNotes;
        this.filterNoTicketNotes = filterNoTicketNotes;
        changeSupport.firePropertyChange(FILTER_NO_TICKET_NOTES_PROPERTY, oldValue, this.filterNoTicketNotes);
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$()
    {
        createUIComponents();
        rootComponent = new JPanel();
        rootComponent.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        final JScrollPane scrollPane1 = new JScrollPane();
        rootComponent.add(scrollPane1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        scrollPane1.setViewportView(table);
    }

    private void createUIComponents()
    {
        table = new JTable(new QANoteTablModel());
        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e)
            {
                if (e.getValueIsAdjusting()) {
                    return;
                }
                final ListSelectionModel selectionModel = table.getSelectionModel();
                final int index = selectionModel.getAnchorSelectionIndex();
                if (selectionModel.isSelectionEmpty() || !selectionModel.isSelectedIndex(index)) {
                    noteManager.selectNote(null);
                } else {
                    noteManager.selectNote(noteManager.getQANotes().get(table.convertRowIndexToModel(index)));
                }
            }
        });
        final TableRowSorter<TableModel> sorter = new TableRowSorter<TableModel>(table.getModel());
        sorter.setRowFilter(new RowFilter<TableModel, Integer>() {
            @Override
            public boolean include(Entry<? extends TableModel, ? extends Integer> entry)
            {
                if (filterNoTicketNotes && !StringUtils.isBlank(entry.getStringValue(4))) {
                    return false;
                }
                if (filterEmptyDescriptionNotes && !StringUtils.isBlank(entry.getStringValue(1))) {
                    return false;
                }
                if (filterEmptyReporterNotes && !StringUtils.isBlank(entry.getStringValue(2))) {
                    return false;
                }
                if (filterEmptyRevisionNotes && !StringUtils.isBlank(entry.getStringValue(5))) {
                    return false;
                }
                //noinspection RedundantIfStatement
                if (filterInvalidAssigneeNotes && getValidAssignees().contains(entry.getStringValue(3))) {
                    return false;
                }

                return true;
            }
        });
        table.setRowSorter(sorter);
    }

// -------------------------- INNER CLASSES --------------------------

    private class QANoteTablModel implements TableModel, PropertyChangeListener {
// ------------------------------ FIELDS ------------------------------

        private List<TableModelListener> listeners = new ArrayList<TableModelListener>();

// --------------------------- CONSTRUCTORS ---------------------------

        private QANoteTablModel()
        {
            noteManager.addPropertyChangeListener(QANoteManager.QA_NOTES_PROPERTY, this);
            QANotesList.this.addPropertyChangeListener(this);
        }

// ------------------------ INTERFACE METHODS ------------------------


// --------------------- Interface PropertyChangeListener ---------------------

        public void propertyChange(PropertyChangeEvent evt)
        {
            if (evt.getSource().equals(QANotesList.this)) {
                ApplicationManager.getApplication().invokeLater(new Runnable() {
                    public void run()
                    {
                        fireTableModelEvent();
                    }
                });
            } else {
                ApplicationManager.getApplication().invokeLater(new Runnable() {
                    public void run()
                    {
                        fireTableModelEvent();
                    }
                });
            }
        }

// --------------------- Interface TableModel ---------------------

        public int getRowCount()
        {
            return noteManager.getQANotes().size();
        }

        public int getColumnCount()
        {
            return 6;
        }

        public String getColumnName(int columnIndex)
        {
            switch (columnIndex) {
                case 0:
                    return "ID";
                case 1:
                    return "Description";
                case 2:
                    return "Reporter";
                case 3:
                    return "Assignee";
                case 4:
                    return "Ticket";
                case 5:
                    return "Revision";
                default:
                    throw new IllegalArgumentException("Invalid column index: " + columnIndex);
            }
        }

        public Class<?> getColumnClass(int columnIndex)
        {
            switch (columnIndex) {
                case 0:
                    return Long.class;
                case 5:
                    return Long.class;
                default:
                    return String.class;
            }
        }

        public boolean isCellEditable(int rowIndex, int columnIndex)
        {
            return false;
        }

        public Object getValueAt(int rowIndex, int columnIndex)
        {
            final List<QANote> notes = noteManager.getQANotes();
            if (notes.size() <= rowIndex) {
//                In case notes get cleared between TableModelEvent and call to this method
                return null;
            }
            final QANote qaNote = notes.get(rowIndex);
            switch (columnIndex) {
                case 0:
                    return qaNote.getId();
                case 1:
                    return qaNote.getDescription();
                case 2:
                    return qaNote.getReporter();
                case 3:
                    return qaNote.getAssignee();
                case 4:
                    return qaNote.getTicket();
                case 5:
                    return qaNote.getRevision();
                default:
                    throw new IllegalArgumentException("Invalid column index: " + columnIndex);
            }
        }

        public void setValueAt(Object aValue, int rowIndex, int columnIndex)
        {
            throw new UnsupportedOperationException();
        }

        public void addTableModelListener(TableModelListener l)
        {
            listeners.add(l);
        }

        public void removeTableModelListener(TableModelListener l)
        {
            listeners.remove(l);
        }

        private void fireTableModelEvent()
        {
            final TableModelEvent e = new TableModelEvent(this);
            for (TableModelListener listener : listeners) {
                listener.tableChanged(e);
            }
        }
    }
}
