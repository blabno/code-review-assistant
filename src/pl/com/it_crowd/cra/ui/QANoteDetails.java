package pl.com.it_crowd.cra.ui;

import com.intellij.ide.BrowserUtil;
import com.intellij.ide.actions.OpenFileAction;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import com.intellij.vcsUtil.VcsUtil;
import org.apache.commons.lang.StringUtils;
import pl.com.it_crowd.cra.model.QANoteManager;
import pl.com.it_crowd.cra.model.SyncToFileException;
import pl.com.it_crowd.cra.model.YoutrackTicketManager;
import pl.com.it_crowd.cra.scanner.QANote;
import pl.com.it_crowd.cra.scanner.QASuggestion;
import pl.com.it_crowd.cra.scanner.QAViolation;

import javax.swing.AbstractAction;
import javax.swing.ButtonGroup;
import javax.swing.ComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

public class QANoteDetails {
// ------------------------------ FIELDS ------------------------------

    private JComboBox assignee;

    private JButton cancelButton;

    private JTextArea description;

    private JTextField file;

    private JButton jumpToSourceButton;

    private JButton navigateToYoutrackTicket;

    private QANote note;

    private QANoteManager noteManager;

    private final PropertyChangeListener notePropertyChangeListener = new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent evt)
        {
            if (note.equals(evt.getSource())) {
                ApplicationManager.getApplication().invokeLater(new Runnable() {
                    public void run()
                    {
                        loadDetails(note);
                    }
                });
            }
        }
    };

    private Project project;

    private JButton refreshAssigneesButton;

    private JTextField reporter;

    private JTextField revision;

    private JPanel rootComponent;

    private JButton saveButton;

    private JRadioButton suggestionRadioButton;

    private JTextField temporaryId;

    private JTextField ticket;

    private JRadioButton violationRadioButton;

// --------------------------- CONSTRUCTORS ---------------------------

    public QANoteDetails(final Project project)
    {
        this.project = project;
        this.noteManager = QANoteManager.getInstance(project);
        $$$setupUI$$$();
        final GoToYoutrackTicketAction goToYoutrackTicketAction = new GoToYoutrackTicketAction();
        noteManager.addPropertyChangeListener(QANoteManager.SELECTED_NOTE_PROPERTY, new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt)
            {
                setNote((QANote) evt.getNewValue());
                goToYoutrackTicketAction.fireEnablePropertyChange();
            }
        });
        jumpToSourceButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e)
            {
                final QANote selectedNote = noteManager.getSelectedNote();
                final String title = "Jump to Source";
                if (selectedNote == null) {
                    Messages.showWarningDialog("No QANote selected", title);
                    return;
                }
                if (StringUtils.isBlank(selectedNote.getFileName())) {
                    Messages.showWarningDialog("QANote is not associated with any file", title);
                    return;
                }
                try {
                    final File file = noteManager.getFile(selectedNote);
                    OpenFileAction.openFile(VcsUtil.getVirtualFile(file), noteManager.getProject());
                } catch (FileNotFoundException ex) {
                    Messages.showWarningDialog(ex.getMessage(), title);
                }
            }
        });
        setNote(noteManager.getSelectedNote());


        saveButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e)
            {
                if (!StringUtils.isBlank(revision.getText())) {
                    try {
                        note.setRevision(Long.parseLong(revision.getText()));
                    } catch (NumberFormatException e1) {
                        Messages.showWarningDialog(e1.getMessage(), "Invalid Revision");
                        return;
                    }
                }
                if (!StringUtils.isBlank(description.getText())) {
                    note.setDescription(description.getText());
                }
                if (!StringUtils.isBlank(file.getText())) {
                    note.setFileName(file.getText());
                }
                if (assignee.getSelectedItem() != null) {
                    note.setAssignee(assignee.getSelectedItem().toString());
                }
                if (!StringUtils.isBlank(reporter.getText())) {
                    note.setReporter(reporter.getText());
                }
                if (!StringUtils.isBlank(ticket.getText())) {
                    note.setTicket(ticket.getText());
                }
                try {
                    noteManager.saveNote(note);
                } catch (SyncToFileException ex) {
                    Messages.showWarningDialog(ex.getMessage(), "Problem Saving QANote to File");
                }
            }
        });
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e)
            {
                loadDetails(note);
            }
        });
        refreshAssigneesButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e)
            {
                noteManager.refreshValidAssignees();
            }
        });

        navigateToYoutrackTicket.setAction(goToYoutrackTicketAction);
    }

// -------------------------- OTHER METHODS --------------------------

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$()
    {
        return rootComponent;
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
        rootComponent.setLayout(new GridLayoutManager(10, 4, new Insets(0, 0, 0, 0), -1, -1));
        final Spacer spacer1 = new Spacer();
        rootComponent.add(spacer1,
            new GridConstraints(8, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null,
                null, 0, false));
        reporter = new JTextField();
        rootComponent.add(reporter,
            new GridConstraints(1, 1, 1, 3, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW,
                GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("Reporter");
        rootComponent.add(label1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED,
            GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        file = new JTextField();
        file.setEditable(false);
        rootComponent.add(file,
            new GridConstraints(2, 1, 1, 3, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW,
                GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("File");
        rootComponent.add(label2, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED,
            GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label3 = new JLabel();
        label3.setText("Assignee");
        rootComponent.add(label3, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED,
            GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label4 = new JLabel();
        label4.setText("Revision");
        rootComponent.add(label4, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED,
            GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        revision = new JTextField();
        revision.setEditable(true);
        revision.setText("");
        rootComponent.add(revision,
            new GridConstraints(4, 1, 1, 3, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW,
                GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label5 = new JLabel();
        label5.setText("Ticket");
        rootComponent.add(label5, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED,
            GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label6 = new JLabel();
        label6.setText("Type");
        rootComponent.add(label6, new GridConstraints(6, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED,
            GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        suggestionRadioButton = new JRadioButton();
        suggestionRadioButton.setEnabled(false);
        suggestionRadioButton.setText("Suggestion");
        rootComponent.add(suggestionRadioButton, new GridConstraints(6, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        violationRadioButton = new JRadioButton();
        violationRadioButton.setEnabled(false);
        violationRadioButton.setText("Violation");
        rootComponent.add(violationRadioButton, new GridConstraints(6, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label7 = new JLabel();
        label7.setText("Description");
        rootComponent.add(label7, new GridConstraints(7, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED,
            GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        rootComponent.add(scrollPane1, new GridConstraints(7, 1, 2, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(300, 100), null, 0, false));
        description = new JTextArea();
        description.setPreferredSize(new Dimension(-1, -1));
        scrollPane1.setViewportView(description);
        final JLabel label8 = new JLabel();
        label8.setText("Temporary ID");
        rootComponent.add(label8, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED,
            GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        temporaryId = new JTextField();
        temporaryId.setEditable(false);
        temporaryId.setEnabled(true);
        rootComponent.add(temporaryId,
            new GridConstraints(0, 1, 1, 3, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW,
                GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        jumpToSourceButton = new JButton();
        jumpToSourceButton.setText("Jump to source");
        rootComponent.add(jumpToSourceButton, new GridConstraints(9, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        saveButton = new JButton();
        saveButton.setText("Save");
        rootComponent.add(saveButton, new GridConstraints(9, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        cancelButton = new JButton();
        cancelButton.setText("Cancel");
        rootComponent.add(cancelButton, new GridConstraints(9, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        rootComponent.add(panel1, new GridConstraints(3, 1, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        assignee.setEditable(true);
        panel1.add(assignee, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW,
            GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        refreshAssigneesButton = new JButton();
        refreshAssigneesButton.setIcon(new ImageIcon(getClass().getResource("/actions/sync.png")));
        refreshAssigneesButton.setText("");
        panel1.add(refreshAssigneesButton, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        rootComponent.add(panel2, new GridConstraints(5, 1, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        ticket = new JTextField();
        ticket.setColumns(0);
        ticket.setEditable(false);
        panel2.add(ticket, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW,
            GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        navigateToYoutrackTicket = new JButton();
        navigateToYoutrackTicket.setIcon(new ImageIcon(getClass().getResource("/ide/link.png")));
        navigateToYoutrackTicket.setText("");
        panel2.add(navigateToYoutrackTicket, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        label1.setLabelFor(reporter);
        label2.setLabelFor(file);
        label3.setLabelFor(assignee);
        label4.setLabelFor(revision);
        label5.setLabelFor(ticket);
        label7.setLabelFor(description);
        label8.setLabelFor(temporaryId);
        ButtonGroup buttonGroup;
        buttonGroup = new ButtonGroup();
        buttonGroup.add(suggestionRadioButton);
        buttonGroup.add(violationRadioButton);
    }

    private void createUIComponents()
    {
        assignee = new JComboBox(new AssigneeModel());
    }

    private void loadDetails(QANote note)
    {
        temporaryId.setText("");
        description.setText("");
        file.setText("");
        assignee.setSelectedItem(null);
        reporter.setText("");
        revision.setText("");
        suggestionRadioButton.setSelected(false);
        ticket.setText("");
        violationRadioButton.setSelected(false);
        if (note != null) {
            description.setText(note.getDescription());
            file.setText(note.getFileName());
            if (!StringUtils.isBlank(note.getAssignee())) {
                assignee.setSelectedItem(note.getAssignee());
            }
            reporter.setText(note.getReporter());
            if (note.getRevision() != null) {
                revision.setText(note.getRevision().toString());
            }
            suggestionRadioButton.setSelected(note instanceof QASuggestion);
            if (note.getId() != null) {
                temporaryId.setText(note.getId().toString());
            }
            ticket.setText(note.getTicket());
            violationRadioButton.setSelected(note instanceof QAViolation);
        }
    }

    private void setNote(QANote note)
    {
        if (this.note != null) {
            this.note.removePropertyChangeListener(notePropertyChangeListener);
        }
        this.note = note;
        if (this.note != null) {
            this.note.addPropertyChangeListener(notePropertyChangeListener);
        }
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            public void run()
            {
                loadDetails(QANoteDetails.this.note);
            }
        });
    }

// -------------------------- INNER CLASSES --------------------------

    private class AssigneeModel implements ComboBoxModel, PropertyChangeListener {
// ------------------------------ FIELDS ------------------------------

        private List<ListDataListener> listeners = new ArrayList<ListDataListener>();

        private Object selectedItem;

// --------------------------- CONSTRUCTORS ---------------------------

        private AssigneeModel()
        {
            noteManager.addPropertyChangeListener(QANoteManager.VALID_ASSIGNEES_PROPERTY, this);
        }

// --------------------- GETTER / SETTER METHODS ---------------------

        public Object getSelectedItem()
        {
            return selectedItem;
        }

        public void setSelectedItem(Object anItem)
        {
            this.selectedItem = anItem;
            final ListDataEvent event = new ListDataEvent(this, ListDataEvent.INTERVAL_ADDED, 0, getSize());
            for (ListDataListener listener : listeners) {
                listener.intervalAdded(event);
            }
        }

// ------------------------ INTERFACE METHODS ------------------------


// --------------------- Interface ListModel ---------------------

        public int getSize()
        {
            return noteManager.getValidAssignees().size();
        }

        public Object getElementAt(int index)
        {
            return noteManager.getValidAssignees().get(index);
        }

        public void addListDataListener(ListDataListener l)
        {
            listeners.add(l);
        }

        public void removeListDataListener(ListDataListener l)
        {
            listeners.remove(l);
        }

// --------------------- Interface PropertyChangeListener ---------------------

        public void propertyChange(PropertyChangeEvent evt)
        {
            final ListDataEvent event = new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, 0, getSize());
            for (ListDataListener listener : listeners) {
                listener.contentsChanged(event);
            }
        }
    }

    private class GoToYoutrackTicketAction extends AbstractAction {
// ------------------------ INTERFACE METHODS ------------------------


// --------------------- Interface Action ---------------------

        @Override
        public boolean isEnabled()
        {
            return note != null && !StringUtils.isBlank(note.getTicket());
        }

// --------------------- Interface ActionListener ---------------------

        public void actionPerformed(ActionEvent e)
        {
            if (!isEnabled() || note == null) {
                return;
            }
            final String ticketURL = YoutrackTicketManager.getInstance(project).getTicketURL(note.getTicket());
            BrowserUtil.launchBrowser(ticketURL);
        }

        private void fireEnablePropertyChange()
        {
            final boolean value = isEnabled();
            changeSupport.firePropertyChange("enabled", !value, value);
        }
    }
}

