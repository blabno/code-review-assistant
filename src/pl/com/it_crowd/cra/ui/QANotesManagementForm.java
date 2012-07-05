package pl.com.it_crowd.cra.ui;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import org.apache.commons.lang.StringUtils;
import pl.com.it_crowd.cra.model.QANoteManager;
import pl.com.it_crowd.cra.scanner.QANote;

import javax.swing.ImageIcon;
import javax.swing.InputVerifier;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.event.DocumentEvent;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;

public class QANotesManagementForm {
// ------------------------------ FIELDS ------------------------------

    private static final String QA_NOTES_MANAGEMER_TOOLWINDOW = "QANotes Manager";

    private JButton applyDefaultsButton;

    private JButton closeButton;

    private JButton createTicketsButton;

    private JTextField defaultAuthor;

    private JTextField defaultRevision;

    private JButton filtersButton;

    private final QANoteManager noteManager;

    private Project project;

    private QANoteDetails qaNoteDetails;

    private QANotesList qaNotesList;

    private JPanel rootComponent;

    private JButton saveAllButton;

    private JButton scanCodeButton;

// -------------------------- STATIC METHODS --------------------------

    private static ToolWindow getQANotesManagementWindow(Project project)
    {
        final ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
        ToolWindow toolWindow = toolWindowManager.getToolWindow(QA_NOTES_MANAGEMER_TOOLWINDOW);
        if (toolWindow == null) {
            toolWindow = toolWindowManager.registerToolWindow(QA_NOTES_MANAGEMER_TOOLWINDOW, true, ToolWindowAnchor.BOTTOM);
            toolWindow.setIcon(new ImageIcon(CodeReviewAssistantPanel.class.getResource("/icons/qa-note-manager-small.png")));

            QANotesManagementForm notesManagementForm = new QANotesManagementForm(project);
            Content content = ContentFactory.SERVICE.getInstance().createContent(notesManagementForm.$$$getRootComponent$$$(), "QANotes", false);
            content.setDisposer(new Disposable() {
                public void dispose()
                {
                    toolWindowManager.unregisterToolWindow(QA_NOTES_MANAGEMER_TOOLWINDOW);
                }
            });
            toolWindow.getContentManager().addContent(content);

            TicketsManagementForm ticketsManagementForm = new TicketsManagementForm(project);
            content = ContentFactory.SERVICE.getInstance().createContent(ticketsManagementForm.$$$getRootComponent$$$(), "Youtrack Tickets", false);
            toolWindow.getContentManager().addContent(content);
        }
        return toolWindow;
    }

    public static void show(Project project)
    {
        final ToolWindow toolWindow = getQANotesManagementWindow(project);
        toolWindow.show(null);
        toolWindow.activate(null);
    }

// --------------------------- CONSTRUCTORS ---------------------------

    public QANotesManagementForm(final Project project)
    {
        this.project = project;
        noteManager = project.getComponent(QANoteManager.class);
        $$$setupUI$$$();

        scanCodeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e)
            {
                noteManager.adjustQANotesCode();
            }
        });
        closeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e)
            {
                final ToolWindowManager instance = ToolWindowManager.getInstance(project);
                instance.unregisterToolWindow(QA_NOTES_MANAGEMER_TOOLWINDOW);
                try {
                    noteManager.finish();
                } catch (IOException ex) {
                    throw new RuntimeException("Problem finishing QANote management", ex);
                }
            }
        });
        defaultAuthor.setText(noteManager.getDefaultAuthor());
        final Long revision = noteManager.getDefaultRevision();
        defaultRevision.setText(revision == null ? "" : revision.toString());
        defaultAuthor.getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(DocumentEvent documentEvent)
            {
                noteManager.setDefaultAuthor(defaultAuthor.getText());
            }
        });
        defaultRevision.getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(DocumentEvent documentEvent)
            {
                try {
                    noteManager.setDefaultRevision(Long.parseLong(defaultRevision.getText()));
                } catch (NumberFormatException ignore) {
                }
            }
        });
        defaultRevision.setInputVerifier(new InputVerifier() {
            @Override
            public boolean verify(JComponent input)
            {
                final JTextField textField = (JTextField) input;
                try {
                    Long.parseLong(textField.getText());
                    return true;
                } catch (NumberFormatException e) {
                    return false;
                }
            }
        });
        applyDefaultsButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e)
            {
                final String reporter = noteManager.getDefaultAuthor();
                final Long revision = noteManager.getDefaultRevision();
                for (QANote note : noteManager.getQANotes()) {
                    if (StringUtils.isBlank(note.getReporter())) {
                        note.setReporter(reporter);
                    }
                    if (note.getRevision() == null) {
                        note.setRevision(revision);
                    }
                }
            }
        });
        createTicketsButton.setAction(qaNotesList.getCreateTicketsAction());

        final JPopupMenu filtersPopupMenu = new JPopupMenu("Filters");
        final JCheckBoxMenuItem noTicketMenuItem = new JCheckBoxMenuItem("No ticket");
        final JCheckBoxMenuItem missingTicketMenuItem = new JCheckBoxMenuItem("Missing ticket");
        final JCheckBoxMenuItem invalidAssigneeMenuItem = new JCheckBoxMenuItem("Invalid assignee");
        final JCheckBoxMenuItem emptyRevisonMenuItem = new JCheckBoxMenuItem("Empty revision");
        final JCheckBoxMenuItem emptyReporterMenuItem = new JCheckBoxMenuItem("Empty reporter");
        final JCheckBoxMenuItem emptyDescriptionMenuItem = new JCheckBoxMenuItem("Empty description");
        filtersPopupMenu.add(noTicketMenuItem);
        filtersPopupMenu.add(missingTicketMenuItem);
        filtersPopupMenu.add(invalidAssigneeMenuItem);
        filtersPopupMenu.add(emptyDescriptionMenuItem);
        filtersPopupMenu.add(emptyReporterMenuItem);
        filtersPopupMenu.add(emptyRevisonMenuItem);
        final ItemListener itemListener = new ItemListener() {
            public void itemStateChanged(ItemEvent e)
            {
                if (noTicketMenuItem.equals(e.getItemSelectable())) {
                    qaNotesList.setFilterNoTicketNotes(ItemEvent.SELECTED == e.getStateChange());
                } else if (missingTicketMenuItem.equals(e.getItemSelectable())) {
                    qaNotesList.setFilterMissingTicketNotes(ItemEvent.SELECTED == e.getStateChange());
                } else if (invalidAssigneeMenuItem.equals(e.getItemSelectable())) {
                    qaNotesList.setFilterInvalidAssigneeNotes(ItemEvent.SELECTED == e.getStateChange());
                } else if (emptyRevisonMenuItem.equals(e.getItemSelectable())) {
                    qaNotesList.setFilterEmptyRevisionNotes(ItemEvent.SELECTED == e.getStateChange());
                } else if (emptyReporterMenuItem.equals(e.getItemSelectable())) {
                    qaNotesList.setFilterEmptyReporterNotes(ItemEvent.SELECTED == e.getStateChange());
                } else if (emptyDescriptionMenuItem.equals(e.getItemSelectable())) {
                    qaNotesList.setFilterEmptyDescriptionNotes(ItemEvent.SELECTED == e.getStateChange());
                }
            }
        };
        missingTicketMenuItem.addItemListener(itemListener);
        noTicketMenuItem.addItemListener(itemListener);
        invalidAssigneeMenuItem.addItemListener(itemListener);
        emptyDescriptionMenuItem.addItemListener(itemListener);
        emptyReporterMenuItem.addItemListener(itemListener);
        emptyRevisonMenuItem.addItemListener(itemListener);

        filtersButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e)
            {
                filtersPopupMenu.show(e.getComponent(), e.getX(), e.getY());
            }
        });
        saveAllButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e)
            {
                noteManager.saveAllNotes();
            }
        });
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
        rootComponent.setLayout(new GridLayoutManager(2, 2, new Insets(0, 0, 0, 0), -1, -1));
        final JToolBar toolBar1 = new JToolBar();
        toolBar1.setFloatable(false);
        rootComponent.add(toolBar1,
            new GridConstraints(0, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW,
                GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(-1, 20), null, 0, false));
        closeButton = new JButton();
        closeButton.setIcon(new ImageIcon(getClass().getResource("/actions/exit.png")));
        closeButton.setText("");
        closeButton.setToolTipText("Exit");
        toolBar1.add(closeButton);
        saveAllButton = new JButton();
        saveAllButton.setIcon(new ImageIcon(getClass().getResource("/actions/menu-saveall.png")));
        saveAllButton.setText("");
        saveAllButton.setToolTipText("Save all");
        toolBar1.add(saveAllButton);
        scanCodeButton = new JButton();
        scanCodeButton.setIcon(new ImageIcon(getClass().getResource("/actions/sync.png")));
        scanCodeButton.setText("");
        scanCodeButton.setToolTipText("Scan code");
        toolBar1.add(scanCodeButton);
        createTicketsButton = new JButton();
        createTicketsButton.setIcon(new ImageIcon(getClass().getResource("/actions/execute.png")));
        createTicketsButton.setText("");
        createTicketsButton.setToolTipText("Create tickets");
        toolBar1.add(createTicketsButton);
        filtersButton = new JButton();
        filtersButton.setText("Filters");
        toolBar1.add(filtersButton);
        final JLabel label1 = new JLabel();
        label1.setText("Default author");
        toolBar1.add(label1);
        defaultAuthor = new JTextField();
        toolBar1.add(defaultAuthor);
        final JLabel label2 = new JLabel();
        label2.setText("Default revision");
        toolBar1.add(label2);
        defaultRevision = new JTextField();
        toolBar1.add(defaultRevision);
        applyDefaultsButton = new JButton();
        applyDefaultsButton.setText("Apply defaults");
        toolBar1.add(applyDefaultsButton);
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        rootComponent.add(panel1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        panel1.add(qaNotesList.$$$getRootComponent$$$(), new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        rootComponent.add(qaNoteDetails.$$$getRootComponent$$$(),
            new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_NORTH, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED,
                GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        label1.setLabelFor(defaultAuthor);
        label2.setLabelFor(defaultRevision);
    }

    private void createUIComponents()
    {
        qaNotesList = new QANotesList(project);
        qaNoteDetails = new QANoteDetails(project);
    }
}
