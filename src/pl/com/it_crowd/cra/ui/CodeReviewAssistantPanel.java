package pl.com.it_crowd.cra.ui;

import com.intellij.ide.DataManager;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diff.DiffManager;
import com.intellij.openapi.diff.DiffNavigationContext;
import com.intellij.openapi.diff.DiffRequest;
import com.intellij.openapi.diff.DiffTool;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vcs.ObjectsConvertor;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.actions.ChangeDiffRequest;
import com.intellij.openapi.vcs.changes.actions.ChangeDiffRequestPresentable;
import com.intellij.openapi.vcs.changes.actions.DiffRequestPresentable;
import com.intellij.openapi.vcs.changes.actions.ShowDiffUIContext;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.util.containers.Convertor;
import org.tmatesoft.svn.core.wc.SVNStatusType;
import pl.com.it_crowd.cra.model.AssistantHelper;
import pl.com.it_crowd.cra.model.CodeReviewAssistant;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CodeReviewAssistantPanel {
// ------------------------------ FIELDS ------------------------------

    public static final String CODE_REVIEW_ASSISTANT_TOOLWINDOW = "Code review assistant";

    private CodeReviewAssistant assistant;

    private JButton closeButton;

    private JPanel contentPane;

    private JButton diffButton;

    private Project project;

    private JLabel revisionRangeLabel;

    private JButton revisionsButton;

    private JTable table;

// -------------------------- STATIC METHODS --------------------------

    private static ToolWindow getReviewAssistantWindow(Project project)
    {
        final ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
        ToolWindow toolWindow = toolWindowManager.getToolWindow(CODE_REVIEW_ASSISTANT_TOOLWINDOW);
        if (toolWindow == null) {
            toolWindow = toolWindowManager.registerToolWindow(CODE_REVIEW_ASSISTANT_TOOLWINDOW, true, ToolWindowAnchor.BOTTOM);
            CodeReviewAssistantPanel assistantPanel = new CodeReviewAssistantPanel(project);
            final Content content = ContentFactory.SERVICE.getInstance().createContent(assistantPanel.$$$getRootComponent$$$(), "Code review assistant", false);
            content.setDisposer(new Disposable() {
                public void dispose()
                {
                    toolWindowManager.unregisterToolWindow(CODE_REVIEW_ASSISTANT_TOOLWINDOW);
                }
            });
            toolWindow.getContentManager().addContent(content);
            toolWindow.setIcon(new ImageIcon(CodeReviewAssistantPanel.class.getResource("/icons/code-review-small.png")));
        }
        return toolWindow;
    }

    public static void show(Project project)
    {
        final ToolWindow toolWindow = getReviewAssistantWindow(project);
        toolWindow.show(null);
        toolWindow.activate(null);
    }

// --------------------------- CONSTRUCTORS ---------------------------

    public CodeReviewAssistantPanel(Project project)
    {
        this.project = project;
        this.assistant = project.getComponent(CodeReviewAssistant.class);
        $$$setupUI$$$();
        closeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e)
            {
                final ToolWindowManager instance = ToolWindowManager.getInstance(CodeReviewAssistantPanel.this.project);
                instance.unregisterToolWindow(CODE_REVIEW_ASSISTANT_TOOLWINDOW);
            }
        });
        assistant.addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt)
            {
                if (CodeReviewAssistant.REVISION_RANGE_PROPERTY.equals(evt.getPropertyName())) {
                    final CodeReviewAssistant.RevisionRange newValue = (CodeReviewAssistant.RevisionRange) evt.getNewValue();
                    updateRevisionRangeLabel(newValue);
                }
            }
        });
        updateRevisionRangeLabel(assistant.getRevisionRange());
        diffButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e)
            {
                doDiff();
            }
        });
        revisionsButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e)
            {
                final ActionManager actionManager = ActionManager.getInstance();
                final AnAction anAction = actionManager.getAction("CodeReview");
                anAction.actionPerformed(new AnActionEvent(null, DataManager.getInstance().getDataContext((Component) e.getSource()), ActionPlaces.UNKNOWN,
                    anAction.getTemplatePresentation().clone(), actionManager, 0));
            }
        });
        assistant.addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt)
            {
                if (CodeReviewAssistant.CURRENT_FILE_PROPERTY.equals(evt.getPropertyName())) {
                    diffButton.setEnabled(evt.getNewValue() != null && ((File) evt.getNewValue()).isFile());
                }
            }
        });
    }

// -------------------------- OTHER METHODS --------------------------

    private void createUIComponents()
    {
        table = new JTable(new ChangedFilesModel());
        TableColumn col = table.getColumnModel().getColumn(0);
        final int width = 60;
        col.setMinWidth(width);
        col.setMaxWidth(width);
        col.setPreferredWidth(width);
        table.setShowGrid(false);

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e)
            {
                if (e.getValueIsAdjusting()) {
                    return;
                }
                final ListSelectionModel selectionModel = table.getSelectionModel();
                final int index = selectionModel.getAnchorSelectionIndex();
                if (selectionModel.isSelectionEmpty() || !selectionModel.isSelectedIndex(index)) {
                    assistant.selectFile(null);
                } else {
                    assistant.selectFile(index);
                }
            }
        });
    }

    private void doDiff()
    {
        final File file = assistant.getCurrentFile();
        if (file == null) {
            Messages.showWarningDialog("No file selected", "Show Diff");
            return;
        }
        List<Change> changes = assistant.getChanges();
        final List<DiffRequestPresentable> changeList = ObjectsConvertor.convert(changes, new Convertor<Change, DiffRequestPresentable>() {
            public ChangeDiffRequestPresentable convert(Change o)
            {
                return new ChangeDiffRequestPresentable(CodeReviewAssistantPanel.this.project, o);
            }
        });
        final ShowDiffUIContext context = new ShowDiffUIContext(true);
        final ChangeDiffRequest request = new ChangeDiffRequest(CodeReviewAssistantPanel.this.project, changeList, context.getActionsFactory(),
            context.isShowFrame());

        DiffRequest simpleRequest;
        try {
            request.quickCheckHaveStuff();
            simpleRequest = request.init(0);
            for (Change change : changes) {
                if (AssistantHelper.affectsFile(change, file)) {
                    break;
                }
                if (request.canMoveForward()) {
                    simpleRequest = request.moveForward();
                }
            }
        } catch (VcsException ex) {
            Messages.showWarningDialog(ex.getMessage(), "Show Diff");
            return;
        }
        if (simpleRequest != null) {
            final DiffNavigationContext navigationContext = context.getDiffNavigationContext();
            if (navigationContext != null) {
                simpleRequest.passForDataContext(DiffTool.SCROLL_TO_LINE, navigationContext);
            }
            DiffManager.getInstance().getDiffTool().show(simpleRequest);
        }
    }

    private void updateRevisionRangeLabel(CodeReviewAssistant.RevisionRange newValue)
    {
        if (newValue == null) {
            revisionRangeLabel.setText("");
        } else {
            revisionRangeLabel.setText(String.format("%d-%d", newValue.getStartRevision(), newValue.getEndRevision()));
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
        contentPane = new JPanel();
        contentPane.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        final JScrollPane scrollPane1 = new JScrollPane();
        contentPane.add(scrollPane1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        scrollPane1.setViewportView(table);
        final JToolBar toolBar1 = new JToolBar();
        toolBar1.setFloatable(false);
        contentPane.add(toolBar1,
            new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW,
                GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(-1, 20), null, 0, false));
        closeButton = new JButton();
        closeButton.setIcon(new ImageIcon(getClass().getResource("/actions/cancel.png")));
        closeButton.setText("");
        closeButton.setToolTipText("Close");
        toolBar1.add(closeButton);
        diffButton = new JButton();
        diffButton.setIcon(new ImageIcon(getClass().getResource("/actions/diff.png")));
        diffButton.setText("");
        diffButton.setToolTipText("Diff");
        toolBar1.add(diffButton);
        revisionsButton = new JButton();
        revisionsButton.setText("Revisions:");
        toolBar1.add(revisionsButton);
        revisionRangeLabel = new JLabel();
        revisionRangeLabel.setForeground(UIManager.getColor("Label.foreground"));
        revisionRangeLabel.setRequestFocusEnabled(true);
        revisionRangeLabel.setText("");
        toolBar1.add(revisionRangeLabel);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$()
    {
        return contentPane;
    }

// -------------------------- INNER CLASSES --------------------------

    private class ChangedFilesModel implements TableModel, PropertyChangeListener {
// ------------------------------ FIELDS ------------------------------

        private List<TableModelListener> listeners = new ArrayList<TableModelListener>();

// --------------------------- CONSTRUCTORS ---------------------------

        private ChangedFilesModel()
        {
            assistant.addPropertyChangeListener(this);
        }

// ------------------------ INTERFACE METHODS ------------------------


// --------------------- Interface PropertyChangeListener ---------------------

        public void propertyChange(PropertyChangeEvent evt)
        {
            if (CodeReviewAssistant.CHANGED_FILES_PROPERTY.equals(evt.getPropertyName())) {
                final TableModelEvent e = new TableModelEvent(this);
                for (TableModelListener listener : listeners) {
                    listener.tableChanged(e);
                }
            }
        }

// --------------------- Interface TableModel ---------------------

        public int getRowCount()
        {
            return assistant.getChangedFiles().size();
        }

        public int getColumnCount()
        {
            return 2;
        }

        public String getColumnName(int columnIndex)
        {
            switch (columnIndex) {
                case 0:
                    return "Status";
                default:
                    return "File name";
            }
        }

        public Class<?> getColumnClass(int columnIndex)
        {
            switch (columnIndex) {
                case 0:
                    return SVNStatusType.class;
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
            final File file = assistant.getChangedFiles().get(rowIndex);
            final File canonicalFile;
            try {
                canonicalFile = file.getCanonicalFile();
            } catch (IOException e) {
                throw new RuntimeException("Cannot get canonical canonicalFile for " + file);
            }
            switch (columnIndex) {
                case 0:
                    return assistant.getFileStatus(file);
                case 1:
                    final File parentFile = canonicalFile.getParentFile();
                    return parentFile == null ? canonicalFile : canonicalFile.getName() + " (" + parentFile.getPath() + ")";
                default:
                    throw new IllegalArgumentException("Invalid column index " + columnIndex);
            }
        }

        public void setValueAt(Object aValue, int rowIndex, int columnIndex)
        {
            throw new UnsupportedOperationException();
        }

        public void addTableModelListener(TableModelListener listener)
        {
            if (!listeners.contains(listener)) {
                listeners.add(listener);
            }
        }

        public void removeTableModelListener(TableModelListener listener)
        {
            listeners.remove(listener);
        }
    }
}
