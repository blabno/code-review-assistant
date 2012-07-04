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

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

public class QANotesList {
// ------------------------------ FIELDS ------------------------------

    private Action createTicketsAction = new AbstractAction("Create tickets") {
        public void actionPerformed(ActionEvent e)
        {
            createTickets();
        }
    };

    private QANoteManager noteManager;

    private Project project;

    private JPanel rootComponent;

    private JTable table;

    private YoutrackTicketManager ticketManager;

// --------------------------- CONSTRUCTORS ---------------------------

    public QANotesList(final Project project)
    {
        this.project = project;
        this.noteManager = QANoteManager.getInstance(project);
        this.ticketManager = YoutrackTicketManager.getInstance(project);
        $$$setupUI$$$();
        noteManager.addPropertyChangeListener(QANoteManager.HIGHLIGHTED_NOTE, new PropertyChangeListener() {
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
                                    table.getSelectionModel().setSelectionInterval(i, i);
                                    table.requestFocusInWindow();
//TODO scroll to selected row
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

// -------------------------- OTHER METHODS --------------------------

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$()
    {
        return rootComponent;
    }

    public void createTickets()
    {
        final ListSelectionModel selectionModel = table.getSelectionModel();
        if (selectionModel.isSelectionEmpty()) {
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
                final QANote note = noteManager.getQANotes().get(index);
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
                    noteManager.selectNote(noteManager.getQANotes().get(index));
                }
            }
        });
    }

// -------------------------- INNER CLASSES --------------------------

    private class QANoteTablModel implements TableModel, PropertyChangeListener {
// ------------------------------ FIELDS ------------------------------

        private List<TableModelListener> listeners = new ArrayList<TableModelListener>();

// --------------------------- CONSTRUCTORS ---------------------------

        private QANoteTablModel()
        {
            noteManager.addPropertyChangeListener(QANoteManager.QA_NOTES_PROPERTY, this);
        }

// ------------------------ INTERFACE METHODS ------------------------


// --------------------- Interface PropertyChangeListener ---------------------

        public void propertyChange(PropertyChangeEvent evt)
        {
            ApplicationManager.getApplication().invokeLater(new Runnable() {
                public void run()
                {
                    final TableModelEvent e = new TableModelEvent(QANoteTablModel.this);
                    for (TableModelListener listener : listeners) {
                        listener.tableChanged(e);
                    }
                }
            });
        }

// --------------------- Interface TableModel ---------------------

        public int getRowCount()
        {
            return noteManager.getQANotes().size();
        }

        public int getColumnCount()
        {
            return 1;
        }

        public String getColumnName(int columnIndex)
        {
            switch (columnIndex) {
                case 0:
                    return "Text";
                default:
                    throw new IllegalArgumentException("Invalid column index: " + columnIndex);
            }
        }

        public Class<?> getColumnClass(int columnIndex)
        {
            switch (columnIndex) {
                case 0:
                    return String.class;
                default:
                    throw new IllegalArgumentException("Invalid column index: " + columnIndex);
            }
        }

        public boolean isCellEditable(int rowIndex, int columnIndex)
        {
            return false;
        }

        public Object getValueAt(int rowIndex, int columnIndex)
        {
            final QANote qaNote = noteManager.getQANotes().get(rowIndex);
            switch (columnIndex) {
                case 0:
                    return qaNote.getDescription();
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
    }
}
