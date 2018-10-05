/*
 * MegaMekLab - Copyright (C) 2018 - The MegaMek Team
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 */
package megameklab.com.ui.util;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractListModel;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import megamek.common.AmmoType;
import megamek.common.Entity;
import megamek.common.Mounted;
import megamek.common.Protomech;
import megamek.common.WeaponType;
import megameklab.com.ui.EntitySource;
import megameklab.com.util.CConfig;
import megameklab.com.util.CriticalTransferHandler;
import megameklab.com.util.RefreshListener;
import megameklab.com.util.UnitUtil;

/**
 * @author Neoancient
 *
 */
public class ProtomekMountList extends JList<Mounted> {

    /**
     * 
     */
    private static final long serialVersionUID = -2051124199120063905L;
    private final EntitySource eSource;
    private final int location;
    private RefreshListener refresh;
    
    private final MountedListModel model = new MountedListModel();

    public ProtomekMountList(EntitySource eSource, RefreshListener refresh, int location) {
        this.eSource = eSource;
        this.refresh = refresh;
        this.location = location;
        for (Mounted m : eSource.getEntity().getEquipment()) {
            if (m.getLocation() == location) {
                model.add(m);
            }
        }
        setModel(model);
        setCellRenderer(new MountCellRenderer(true));
        addMouseListener(mouseListener);
        setTransferHandler(new CriticalTransferHandler(eSource, refresh));
    }
    
    public void dragEnter(DropTargetDragEvent dtde) {
    }

    public void dragExit(DropTargetEvent dte) {
    }

    public void dragOver(DropTargetDragEvent dtde) {
    }

    public void dropActionChanged(DropTargetDragEvent dtde) {
    }

    public Protomech getProtomech() {
        return (Protomech) eSource.getEntity();
    }
    
    private void refresh() {
        refresh.refreshEquipment();
        refresh.refreshPreview();
        refresh.refreshBuild();
    }
    
    private void removeMount(Mounted mount) {
        mount.setLocation(Entity.LOC_NONE, false);
        refresh();
    }
    
    private void deleteMount(Mounted mount) {
        UnitUtil.removeMounted(getProtomech(), mount);
        refresh();
    }
    
    private void changeFacing(Mounted mount) {
        mount.setLocation(location, !mount.isRearMounted());
        refresh();
    }
    
    private MouseListener mouseListener = new MouseAdapter() {
        @Override
        public void mousePressed(MouseEvent e) {
            if (location == Protomech.LOC_BODY) {
                return;
            }
            final int index = locationToIndex(e.getPoint());
            final Mounted mount = model.getElementAt(index);
            if (null == mount) {
                return;
            }
            
            if (e.getButton() == MouseEvent.BUTTON2) {
                remove(index);
            } else if (e.getButton() == MouseEvent.BUTTON3) {
                if ((e.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) != 0) {
                    remove(index);
                    return;
                }

                JPopupMenu popup = new JPopupMenu();

                if ((location == Protomech.LOC_TORSO)
                        && (e.getModifiersEx() & InputEvent.ALT_DOWN_MASK) != 0) {
                    changeFacing(mount);
                    return;
                }

                JMenuItem info;

                info = new JMenuItem("Remove " + mount.getName());
                info.addActionListener(ev -> removeMount(mount));
                popup.add(info);
                info = new JMenuItem("Delete " + mount.getName());
                info.addActionListener(ev -> deleteMount(mount));
                popup.add(info);
                if ((location == Protomech.LOC_TORSO)
                        && (mount.getType() instanceof WeaponType)) {
                    info = new JMenuItem("Change Facing");
                    info.addActionListener(ev -> changeFacing(mount));
                    popup.add(info);
                }


                if (popup.getComponentCount() > 0) {
                    popup.show(ProtomekMountList.this, e.getX(), e.getY());
                }
            }
        }
    };
    
    private static class MountedListModel extends AbstractListModel<Mounted> {

        private static final long serialVersionUID = 2575915653617712928L;

        private List<Mounted> list = new ArrayList<>();

        @Override
        public int getSize() {
            return Math.max(1, list.size());
        }

        @Override
        public Mounted getElementAt(int index) {
            if (index >= list.size()) {
                return null;
            }
            return list.get(index);
        }
        
        public void add(Mounted mounted) {
            list.add(mounted);
            fireContentsChanged(this, list.size() - 1, list.size() - 1);
        }
        
    }
    
    private static class MountCellRenderer extends DefaultListCellRenderer {

        private static final long serialVersionUID = -1115364118975814321L;
        
        private boolean useColor = false;

        public MountCellRenderer(boolean useColor) {
            this.useColor = useColor;
        }

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean hasFocus) {
            final JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, hasFocus);
            final ProtomekMountList lstMount = (ProtomekMountList) list;
            final Entity entity = lstMount.eSource.getEntity();
            
            label.setPreferredSize(new Dimension(110,15));
            label.setMaximumSize(new Dimension(110,15));
            label.setMinimumSize(new Dimension(110,15));

            if (null == value) {
                if (useColor) {
                    label.setBackground(CConfig.getBackgroundColor(CConfig.CONFIG_EMPTY));
                    label.setForeground(CConfig.getForegroundColor(CConfig.CONFIG_EMPTY));
                }
                label.setText("-Empty-");
            } else {
                    final Mounted mount = (Mounted) value;
        
                    if (useColor) {
                        if (mount.getType() instanceof WeaponType) {
                            label.setBackground(CConfig.getBackgroundColor(CConfig.CONFIG_WEAPONS));
                            label.setForeground(CConfig.getForegroundColor(CConfig.CONFIG_WEAPONS));
                        } else if (mount.getType() instanceof AmmoType) {
                            label.setBackground(CConfig.getBackgroundColor(CConfig.CONFIG_AMMO));
                            label.setForeground(CConfig.getForegroundColor(CConfig.CONFIG_AMMO));
                        } else {
                            label.setBackground(CConfig.getBackgroundColor(CConfig.CONFIG_EQUIPMENT));
                            label.setForeground(CConfig.getForegroundColor(CConfig.CONFIG_EQUIPMENT));
                        }
                    }
                String name = UnitUtil.getCritName(entity, mount.getType());
                if (mount.isRearMounted()) {
                    name += " (R)";
                }
                if (mount.getType() instanceof AmmoType) {
                    name += " (" + mount.getBaseShotsLeft() + ")";
                }
                String toolTipText = UnitUtil.getToolTipInfo(entity, mount);
                label.setText(name);
                label.setToolTipText(toolTipText);
            }

            if ((index > 0) && (index < list.getModel().getSize())) {
                label.setBorder( BorderFactory.createMatteBorder(1, 0, 0, 0, Color.black));
            }

            return label;
        }
    }
}
