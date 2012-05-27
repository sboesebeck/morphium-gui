/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * RecordTable.java
 *
 * Created on 18.09.2011, 10:12:14
 */
package de.caluga.morphium.gui.recordtable;

import de.caluga.morphium.Morphium;
import de.caluga.morphium.MorphiumSingleton;
import de.caluga.morphium.Query;
import de.caluga.morphium.gui.recordedit.RecordEditDialog;
import de.caluga.morphium.gui.recordtable.renderer.BooleanRenderer;
import de.caluga.morphium.secure.MongoSecurityException;
import de.caluga.morphium.secure.Permission;
import org.apache.log4j.Logger;
import org.bson.types.ObjectId;
import org.jdesktop.swingx.decorator.HighlighterFactory;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.text.LayeredHighlighter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author stephan
 */
public class RecordTable<T> extends JPanel {

    private RecordTableModel<T> model;
    private RecordTableState state;
    private JPopupMenu pop;
    private final Logger log = Logger.getLogger(RecordTable.class);
    private Class<T> type;
    private transient List<DoubleClickListener> dcl;
    private JMenuItem newMi;
    private JMenuItem editMi;
    private JMenuItem delMi;


    public RecordTable(Class<T> cls) {
        this(cls, new RecordTableState(cls));
    }

    public void setDefaultRenderer(Class cls, TableCellRenderer rd) {
        rtable.setDefaultRenderer(cls, rd);
    }

    public void setDefaultEditor(Class cls, TableCellEditor ed) {
        rtable.setDefaultEditor(cls, ed);
    }

    /**
     * Creates new form RecordTable
     */
    public RecordTable(Class<T> cls, RecordTableState initialState) {
        super();
        state = initialState;

        pop = new JPopupMenu();
        initComponents();
        model = new RecordTableModel(cls, initialState);
        dcl = new ArrayList<DoubleClickListener>();
        type = cls;
        pageLengthComboBx.setSelectedItem(initialState.getPageLength());
        rtable.setModel(model);
        model.setTable(rtable);
        //sorting done via model
        rtable.setSortable(false);

        model.setTableHeader(rtable.getTableHeader());

        rtable.setComponentPopupMenu(pop);
        rtable.setDefaultRenderer(Boolean.class, new BooleanRenderer(state));
        rtable.setDefaultEditor(Boolean.class,new BooleanRenderer(state));
//        rtable.setDefaultRenderer(String.class, new StringRenderer());
//        rtable.setDefaultEditor(String.class, new StringRenderer());

//        rtable.setDefaultRenderer(Long.class,new NumberRenderer(state));
//        rtable.setDefaultEditor(Long.class,new NumberRenderer(state));

//        rtable.setDefaultEditor(int.class, new DefaultCellEditor(new JTextField()));
//       rtable.setDefaultRenderer(String.class,new StringRenderer());
//       rtable.setDefaultRenderer(Number.class,new NumberRenderer());
//       rtable.setDefaultRenderer(List.class,new ListRenderer());
//       rtable.setDefaultRenderer(Map.class,new MapRenderer());

        model.setRendererMap(state.getRendererMap());

        updatePopupMenu();
        rtable.addMouseListener(new MouseListener() {

            @Override
            public void mouseClicked(MouseEvent me) {
                if (SwingUtilities.isRightMouseButton(me)) {
                    log.info("Right mousebutton pressed!");
                    if (!pop.isShowing()) {

                        pop.setLocation(me.getXOnScreen(), me.getYOnScreen());
                        pop.setVisible(true);
                    }
                    int row = rtable.rowAtPoint(me.getPoint());
                    rtable.getSelectionModel().clearSelection();
                    rtable.getSelectionModel().addSelectionInterval(row, row);
                    if (getSelectedRecord() != null) {
                        if (delMi!=null) {
                        delMi.setEnabled(true);
                        }
                        if (editMi!=null) {
                            editMi.setEnabled(true);
                        }

                    } else {
                        if (delMi!=null) {
                            delMi.setEnabled(false);
                        }
                        if (editMi!=null) {
                            editMi.setEnabled(false);
                        }
                    }
//                    updateView();
                } else {
                    log.info("Fire doublclick event");
                    final T selectedRecord = getSelectedRecord();
                    if (selectedRecord != null) {
                        for (DoubleClickListener dc : dcl) {
                            dc.onDoubleClick(me, selectedRecord);
                        }
                    }
                }
            }

            @Override
            public void mousePressed(MouseEvent me) {
                int row = rtable.rowAtPoint(me.getPoint());
                rtable.getSelectionModel().addSelectionInterval(row, row);
//                updateView();
            }

            @Override
            public void mouseReleased(MouseEvent me) {
                int row = rtable.rowAtPoint(me.getPoint());
                rtable.getSelectionModel().addSelectionInterval(row, row);
//                updateView();
            }

            @Override
            public void mouseEntered(MouseEvent me) {
            }

            @Override
            public void mouseExited(MouseEvent me) {
            }
        });

        updateView();
        delMi = new JMenuItem("löschen");
        delMi.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                MorphiumSingleton.get().deleteObject(getSelectedRecord());
                updateView();
            }
        });
    }

    public void addDoubleClickListener(DoubleClickListener dc) {
        dcl.add(dc);
    }

    public void removeDoubleClickListener(DoubleClickListener dc) {
        dcl.remove(dc);
    }

    public void updatePopupMenu() throws MongoSecurityException {
        pop.removeAll();
        if (state.isEditable()) {
            boolean update = false;
            boolean insert = false;
            boolean del = false;
            try {
                final T rec = type.newInstance();
                update = !MorphiumSingleton.get().accessDenied(rec, Permission.UPDATE);
                insert = !MorphiumSingleton.get().accessDenied(rec, Permission.INSERT);
                del = !MorphiumSingleton.get().accessDenied(rec, Permission.DELETE);
            } catch (InstantiationException ex) {
                Logger.getLogger(RecordTable.class).fatal(ex);
            } catch (IllegalAccessException ex) {
                Logger.getLogger(RecordTable.class).fatal(ex);
            }
            if (insert) {
                if (newMi == null) {
                    newMi = new JMenuItem("neu");
                    newMi.addActionListener(new ActionListener() {

                        @Override
                        public void actionPerformed(ActionEvent ae) {
                            newRecord();
                        }
                    });
                }
                pop.add(newMi);
            }
            if (update) {
                if (editMi == null) {
                    editMi = new JMenuItem("editieren");
                    if (getSelectedRecord()==null) {
                        editMi.setEnabled(false);
                    }
                    editMi.addActionListener(new ActionListener() {

                        @Override
                        public void actionPerformed(ActionEvent ae) {
                            editSelectedRecord();
                        }
                    });
                }
                pop.add(editMi);
            }
            if (del && state.isDeleteable()) {
                if (delMi == null) {
                    delMi=new JMenuItem("löschen");
                    if (getSelectedRecord()==null) {
                        delMi.setEnabled(false);
                    }
                    delMi.addActionListener(new ActionListener() {

                        @Override
                        public void actionPerformed(ActionEvent ae) {
                            deleteSelectedRecord();
                        }
                    });
                }
                pop.add(delMi);
            }
        }
        List<AbstractRecMenuItem> menuItemList = state.getMenuItemList();
        for (AbstractRecMenuItem it: menuItemList) {
            it.setEnabled(it.isEnabled(getSelectedRecord()));
            pop.add(it);
        }

    }

    public void addContextMenuItem(JMenuItem it) {
        pop.add(it);
    }

    public void newRecord() {
        try {
            T rec = type.newInstance();
            RecordEditDialog dlg = new RecordEditDialog(rec, "Neu anlegen");
            dlg.setVisible(true);
            if (dlg.isConfirmed()) {
                log.info("Object stored with id " + MorphiumSingleton.get().getId(rec));
                updateView();
            }
        } catch (InstantiationException ex) {
            Logger.getLogger(RecordTable.class).fatal(ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(RecordTable.class).fatal(ex);
        }

    }

    public void deleteSelectedRecord() {
        int ret = JOptionPane.showConfirmDialog(this, "Den Eintrag wirklich löschen?", "Frage", JOptionPane.OK_CANCEL_OPTION);
        if (ret == JOptionPane.OK_OPTION) {
            MorphiumSingleton.get().deleteObject(getSelectedRecord());
            updateView();
        }
    }

    public void editSelectedRecord() {
        final T selectedRecord = getSelectedRecord();
//        log.info("Editing: " + MorphiumSingleton.get().getJsonString(selectedRecord));

        RecordEditDialog dlg = new RecordEditDialog(selectedRecord, "Edit", false);
        dlg.setVisible(true);
        if (dlg.isConfirmed()) {
            log.info("Stored successful...");
            updateView();
        }

    }

    public RecordTableState<T> getCurrentState() {
        return state;
    }

    public void setState(RecordTableState<T> st) {
        state=st;
    }

    public T getSelectedRecord() {
        if (rtable == null) return null;
        if (rtable.getSelectionModel() == null) return null;
        int idx = rtable.getSelectionModel().getMinSelectionIndex();

        if (state == null) return null;

        if (state.isSearchable()) {
            idx--;
        }
        if (idx < 0) {
            return null;
        }
        log.info("Returning index " + idx);
        if (model == null) return null;
        if (model.getData() == null) return null;
        return (model.getData().get(idx));
    }

    public void setSelectedRecord(T rec) {
        if (rec==null) return;
        if (rtable == null) return;
        if (rtable.getSelectionModel() == null) return;
        if (state==null) return;
        int offset=0;
        if (state.isSearchable()) {
            offset=+1;
        }
        ObjectId recId=MorphiumSingleton.get().getConfig().getMapper().getId(rec);
        if (recId==null) {
            //Sorry, cannot help yet
            return;
        }
        for (int i=0;i<model.getData().size();i++) {

            ObjectId id=MorphiumSingleton.get().getConfig().getMapper().getId(model.getData().get(i));
            if (id==null) continue;
            if (id.equals(recId)) {
                rtable.getSelectionModel().setSelectionInterval(i+offset,i+offset);
            }
        }
    }

    public List<String> getColumnHeader() {
        return state.getColumnHeader();
    }

    public void setColumnHeader(List<String> columnHeader) {
        state.setColumnHeader(columnHeader);
    }

    public Map<String, RecordTableColumnTypes> getDisplayTypeForField() {
        if (state==null) return null;
        return state.getDisplayTypeForField();
    }

    public void setDisplayTypeForField(Map<String, RecordTableColumnTypes> displayTypeForField) {
        state.setDisplayTypeForField(displayTypeForField);
    }

    public List<String> getFieldsToSearchFor() {
        if (state==null) return new ArrayList<String>();
        return state.getFieldsToSearchFor();
    }

    public void setFieldsToSearchFor(List<String> fieldsToSearchFor) {
        state.setFieldsToSearchFor(fieldsToSearchFor);
    }

    public List<String> getFieldsToShow() {
        if (state==null) return new ArrayList<String>();
        return state.getFieldsToShow();
    }

    public void setFieldsToShow(List<String> fieldsToShow) {
        state.setFieldsToShow(fieldsToShow);
    }

    public Query getInitialSearch() {
        if (state==null) return null;
        return state.getInitialSearch();
    }

    public void setInitialSearch(Query initialSearch) {
        state.setInitialSearch(initialSearch);
    }

    public List<String> getSearchableFields() {
        return state.getSearchableFields();
    }

    public void setSearchableFields(List<String> searchableFields) {
        state.setSearchableFields(searchableFields);
    }

    public boolean isDeleteable() {
        return state.isDeleteable();
    }

    public void setDeleteable(boolean deleteable) {
        state.setDeleteable(deleteable);
    }

    public Class getType() {
        if (state == null) return Object.class;
        return state.getType();
    }

    public void setType(Class type) {
        state.setType(type);
    }

    public boolean isSearchable() {
        return state.isSearchable();
    }

    public void setSearchable(boolean searchable) {
        state.setSearchable(searchable);
    }

    public Map<String, Object> getSearchValues() {
        return state.getSearchValues();
    }

    public void setSearchValues(Map<String, String> searchValues) {
        state.setSearchValues(searchValues);
    }

    public void removeSearchForCol(String fld) {
        state.removeSearchForCol(fld);
    }

    public Query getSearch() {
        if (state == null) return null;
        if (!MorphiumSingleton.isConfigured()) return null;
        return state.getSearch();
    }

    public boolean isEditable() {
        return state.isEditable();
    }

    public void setEditable(boolean editable) {
        state.setEditable(editable);
    }

    public String getColumnName(int col) {
        return state.getColumnName(col);
    }

    public int getCurrentPage() {
        return state.getCurrentPage();
    }

    public void setCurrentPage(int currentPage) {
        state.setCurrentPage(currentPage);
    }

    public int getPageLength() {
        return state.getPageLength();
    }

    public void setPageLength(int pageLength) {
        state.setPageLength(pageLength);
    }

    public boolean isPaging() {
        return state.isPaging();
    }

    public void setPaging(boolean paging) {
        state.setPaging(paging);
    }

    public boolean isSearchable(int fldIdx) {
        return state.isSearchable(fldIdx);
    }

    public boolean isSearchable(String fld) {
        return state.isSearchable(fld);
    }

    public boolean isPreCacheAll() {
        return state.isPreCacheAll();
    }

    public void setPreCacheAll(boolean preCacheAll) {
        state.setPreCacheAll(preCacheAll);
    }

    /**
     * updates the RecordTable view - shows or hides searching line and paging panel
     */
    public final void updateView() {
        pagingPane.setVisible(state.isPaging());
        if (state.isPaging()) {
            final long elementCount = model.getElementCount();
            nextBtn.setEnabled((state.getCurrentPage() + 1) * state.getPageLength() < elementCount);
            prevBtn.setEnabled(state.getCurrentPage() > 0);
            int pages = (int) ((float) elementCount / (float) state.getPageLength());
            pagesLabel.setText(Integer.toString(pages));
            pageLabel.setText(Integer.toString(state.getCurrentPage() + 1));


        }
        T r=getSelectedRecord();
        updatePopupMenu();
        model.updateModel();

        setSelectedRecord(r);


    }

    /**
     * @deprecated - only for gui editor use!
     */
    public RecordTable() {
        super();
        initComponents();
        state = new RecordTableState<Object>(Object.class);
        delMi = new JMenuItem("löschen");
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        pagingPane = new JPanel();
        firstBtn = new JButton();
        prevBtn = new JButton();
        pageLabel = new JLabel();
        JLabel jLabel1 = new JLabel();
        pagesLabel = new JLabel();
        lastBtn = new JButton();
        nextBtn = new JButton();
        pageLengthComboBx = new JComboBox();
        JLabel jLabel2 = new JLabel();
        jScrollPane1 = new JScrollPane();
        rtable = new org.jdesktop.swingx.JXTable();
//        rtable.setAutoCreateRowSorter(false);
//        rtable.setAutoResizeMode(true);

        setSize(new Dimension(550, 73));
        setLayout(new BorderLayout());

        pagingPane.setBorder(BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
        pagingPane.setMinimumSize(new Dimension(550, 50));

        firstBtn.setText("|<");
        firstBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                firstBtnActionPerformed(evt);
            }
        });

        prevBtn.setText("<");
        prevBtn.setActionCommand("prev");
        prevBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                prevBtnActionPerformed(evt);
            }
        });

        pageLabel.setText("99");

        jLabel1.setText("/");

        pagesLabel.setText("102");

        lastBtn.setText(">|");
        lastBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                lastBtnActionPerformed(evt);
            }
        });

        nextBtn.setText(">");
        nextBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                nextBtnActionPerformed(evt);
            }
        });

        pageLengthComboBx.setEditable(true);
        pageLengthComboBx.setModel(new DefaultComboBoxModel(new String[]{"10", "20", "30", "40", "50", "60", "100", "200", "1000"}));
        pageLengthComboBx.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                pageLengthComboBxActionPerformed(evt);
            }
        });

        jLabel2.setText("Seitenlänge");

        org.jdesktop.layout.GroupLayout pagingPaneLayout = new org.jdesktop.layout.GroupLayout(pagingPane);
        pagingPane.setLayout(pagingPaneLayout);
        pagingPaneLayout.setHorizontalGroup(
                pagingPaneLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                        .add(pagingPaneLayout.createSequentialGroup()
                                .add(firstBtn)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                                .add(prevBtn)
                                .add(159, 159, 159)
                                .add(pageLabel)
                                .add(18, 18, 18)
                                .add(jLabel1)
                                .add(18, 18, 18)
                                .add(pagesLabel)
                                .add(32, 32, 32)
                                .add(jLabel2)
                                .add(18, 18, 18)
                                .add(pageLengthComboBx, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 153, Short.MAX_VALUE)
                                .add(nextBtn)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                                .add(lastBtn))
        );
        pagingPaneLayout.setVerticalGroup(
                pagingPaneLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                        .add(pagingPaneLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                                .add(firstBtn)
                                .add(prevBtn)
                                .add(lastBtn)
                                .add(nextBtn)
                                .add(pageLabel)
                                .add(jLabel1)
                                .add(pagesLabel)
                                .add(jLabel2)
                                .add(pageLengthComboBx, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
        );

        add(pagingPane, BorderLayout.PAGE_START);

        rtable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        rtable.setColumnControlVisible(true);
        rtable.setHighlighters(HighlighterFactory.createSimpleStriping());
        rtable.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED));
        rtable.setModel(new javax.swing.table.DefaultTableModel(
                new Object[][]{
                        {null, null, null, null},
                        {null, null, null, null},
                        {null, null, null, null},
                        {null, null, null, null}
                },
                new String[]{
                        "Title 1", "Title 2", "Title 3", "Title 4"
                }
        ));
        rtable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        jScrollPane1.setViewportView(rtable);

        add(jScrollPane1, BorderLayout.CENTER);
    }// </editor-fold>//GEN-END:initComponents

    private void firstBtnActionPerformed(ActionEvent evt) {//GEN-FIRST:event_firstBtnActionPerformed
        state.setCurrentPage(0);
        updateView();
    }//GEN-LAST:event_firstBtnActionPerformed

    private void prevBtnActionPerformed(ActionEvent evt) {//GEN-FIRST:event_prevBtnActionPerformed
        state.setCurrentPage(state.getCurrentPage() - 1);
        updateView();
    }//GEN-LAST:event_prevBtnActionPerformed

    private void lastBtnActionPerformed(ActionEvent evt) {//GEN-FIRST:event_lastBtnActionPerformed
        state.setCurrentPage((int) ((float) model.getElementCount() / (float) state.getPageLength() - 1));
        updateView();
    }//GEN-LAST:event_lastBtnActionPerformed

    private void nextBtnActionPerformed(ActionEvent evt) {//GEN-FIRST:event_nextBtnActionPerformed
        state.setCurrentPage(state.getCurrentPage() + 1);
        updateView();
    }//GEN-LAST:event_nextBtnActionPerformed

    private void pageLengthComboBxActionPerformed(ActionEvent evt) {//GEN-FIRST:event_pageLengthComboBxActionPerformed
        try {
            if (state != null) {
                int pl = Integer.valueOf(pageLengthComboBx.getSelectedItem().toString());

                state.setPageLength(pl);
                state.setCurrentPage(0);
                updateView();
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid number!");

        }
    }//GEN-LAST:event_pageLengthComboBxActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private JButton firstBtn;
    private JScrollPane jScrollPane1;
    private JButton lastBtn;
    private JButton nextBtn;
    private JLabel pageLabel;
    private JComboBox pageLengthComboBx;
    private JLabel pagesLabel;
    private JPanel pagingPane;
    private JButton prevBtn;
    private org.jdesktop.swingx.JXTable rtable;
    // End of variables declaration//GEN-END:variables
}
