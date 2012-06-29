package pl.com.it_crowd.cra.ui;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
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
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.event.DocumentEvent;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

public class QANotesManagementForm {
// ------------------------------ FIELDS ------------------------------

    private static final String QA_NOTES_MANAGEMER_TOOLWINDOW = "QANotes Manager";

    private JButton scanCodeButton;

    private JButton applyDefaultsButton;

    private JButton closeButton;

    private JTextField defaultAuthor;

    private JTextField defaultRevision;

    private final QANoteManager noteManager;

    private QANoteDetails qaNoteDetails;

    private QANotesList qaNotesList;

    private JPanel rootComponent;

    // -------------------------- STATIC METHODS --------------------------

    private static ToolWindow getQANotesManagementWindow(Project project)
    {
        final ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
        ToolWindow toolWindow = toolWindowManager.getToolWindow(QA_NOTES_MANAGEMER_TOOLWINDOW);
        if (toolWindow == null) {
            toolWindow = toolWindowManager.registerToolWindow(QA_NOTES_MANAGEMER_TOOLWINDOW, true, ToolWindowAnchor.BOTTOM);
            QANotesManagementForm notesManagementForm = new QANotesManagementForm(project);
            final Content content = ContentFactory.SERVICE.getInstance().createContent(notesManagementForm.$$$getRootComponent$$$(), null, false);
            content.setDisposer(new Disposable() {
                public void dispose()
                {
                    toolWindowManager.unregisterToolWindow(QA_NOTES_MANAGEMER_TOOLWINDOW);
                }
            });
            toolWindow.getContentManager().addContent(content);
//                TODO create icon for QANotes Manager toolwindow
            toolWindow.setIcon(new ImageIcon(CodeReviewAssistantPanel.class.getResource("/icons/qa-note-manager-small.png")));
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
                    Messages.showWarningDialog(ex.getMessage(), "Problem Finishing QANote Management");
                }
            }
        });
        defaultAuthor.setText(noteManager.getDefaultAuthor());
        defaultRevision.setText(noteManager.getDefaultRevision());
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
                noteManager.setDefaultRevision(defaultRevision.getText());
            }
        });
        applyDefaultsButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e)
            {
                final String reporter = noteManager.getDefaultAuthor();
                final String revision = noteManager.getDefaultRevision();
                for (QANote note : noteManager.getQANotes()) {
                    if (StringUtils.isBlank(note.getReporter())) {
                        note.setReporter(reporter);
                    }
                    if (StringUtils.isBlank(note.getRevision())) {
                        note.setRevision(revision);
                    }
                }
            }
        });
    }

// --------------------- GETTER / SETTER METHODS ---------------------

    public JPanel getRootComponent()
    {
        return rootComponent;
    }

// -------------------------- OTHER METHODS --------------------------

    private void createUIComponents()
    {
        qaNotesList = new QANotesList(noteManager);
        qaNoteDetails = new QANoteDetails(noteManager);
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
        closeButton.setIcon(new ImageIcon(getClass().getResource("/actions/cancel.png")));
        closeButton.setText("");
        toolBar1.add(closeButton);
        scanCodeButton = new JButton();
        scanCodeButton.setText("Scan code");
        toolBar1.add(scanCodeButton);
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

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$()
    {
        return rootComponent;
    }
}
