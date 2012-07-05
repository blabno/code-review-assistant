package pl.com.it_crowd.cra.ui;

import com.intellij.ide.BrowserUtil;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

public class LinkCellRenderer extends DefaultTableCellRenderer implements MouseListener, MouseMotionListener {
// ------------------------------ FIELDS ------------------------------

    private int col = -1;

    private boolean isRollover = false;

    private int row = -1;

// -------------------------- STATIC METHODS --------------------------

    private static boolean isURLColumn(JTable table, int column)
    {
        return column >= 0 && Link.class.equals(table.getColumnClass(column));
    }

// ------------------------ INTERFACE METHODS ------------------------


// --------------------- Interface MouseListener ---------------------

    public void mouseClicked(MouseEvent e)
    {
        JTable table = (JTable) e.getSource();
        Point point = e.getPoint();
        int ccol = table.columnAtPoint(point);
        int crow = table.rowAtPoint(point);
        final Object value = table.getValueAt(crow, ccol);
        if (value instanceof Link) {
            final Link link = (Link) value;
            BrowserUtil.launchBrowser(link.getUrl());
        }
    }

    public void mousePressed(MouseEvent e)
    {
    }

    public void mouseReleased(MouseEvent e)
    {
    }

    public void mouseEntered(MouseEvent e)
    {
    }

    public void mouseExited(MouseEvent e)
    {
        JTable table = (JTable) e.getSource();
        if (isURLColumn(table, col)) {
            table.repaint(table.getCellRect(row, col, false));
            row = -1;
            col = -1;
            isRollover = false;
        }
    }

// --------------------- Interface MouseMotionListener ---------------------

    public void mouseDragged(MouseEvent e)
    {
    }

    public void mouseMoved(MouseEvent e)
    {
        JTable table = (JTable) e.getSource();
        Point pt = e.getPoint();
        int prev_row = row;
        int prev_col = col;
        boolean prev_ro = isRollover;
        row = table.rowAtPoint(pt);
        col = table.columnAtPoint(pt);
        isRollover = isURLColumn(table, col);
        if ((row == prev_row && col == prev_col && Boolean.valueOf(isRollover).equals(prev_ro)) || (!isRollover && !prev_ro)) {
            return;
        }
        Rectangle repaintRect;
        if (isRollover) {
            Rectangle r = table.getCellRect(row, col, false);
            repaintRect = prev_ro ? r.union(table.getCellRect(prev_row, prev_col, false)) : r;
        } else {
            repaintRect = table.getCellRect(prev_row, prev_col, false);
        }
        table.repaint(repaintRect);
    }

// --------------------- Interface TableCellRenderer ---------------------

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
    {
        super.getTableCellRendererComponent(table, value, isSelected, false, row, column);
        String stringValue = value == null ? "" : value.toString();
        if (value instanceof Link) {
            final Link link = (Link) value;
            stringValue = String.format("<html><a href=\"%s\">%s</a></html>", link.getUrl(), link.getText());
        }
        setText(stringValue);
        return this;
    }
}