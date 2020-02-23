/*
 * MIT License
 *
 * Copyright (c) 2018 Bonosoft
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package photon.application.dialogs;

import photon.application.MainForm;
import photon.file.PhotonFile;
import photon.file.parts.PhotonDot;
import photon.file.parts.PhotonFileLayer;
import photon.file.parts.PhotonLayer;
import photon.file.ui.PhotonEditPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.HashSet;

public class EditDialog extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JLabel infoText;
    private JPanel editArea;
    private JButton editModeBtn;

    private EditDialog me;
    private MainForm mainForm;
    private PhotonFile photonFile;
    private PhotonFileLayer fileLayer;
    private PhotonLayer layer;
    private int layerNo;
    private int layerX;
    private int layerY;
    private HashSet<PhotonDot> dots;

    private PhotonDot pressedDot;
    private boolean editModeSwap = true;

    private boolean mirrored;
    private PhotonDot cursorDot;
    private int mouseX;
    private int mouseY;

    public EditDialog(MainForm mainForm) {
        super(mainForm.frame);
        this.me = this;
        this.mainForm = mainForm;

        $$$setupUI$$$();

        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

        buttonOK.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {onOK();}
        });

        buttonCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {onCancel();}
        });

        editModeBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                editModeSwap = !editModeSwap;
                editModeBtn.setText(editModeSwap ? "Uses Swap" : "Uses ON/OFF");
            }
        });
        
        editModeBtn.addKeyListener(new KeyListener() {
            private int prevLayerNo;
            private boolean pressed = false;
            private HashSet<PhotonDot> prevDots;

			@Override
			public void keyTyped(KeyEvent e) {}

			@Override
			public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_Z && !pressed) {
                    pressed = true;
                    
                    prevLayerNo = layerNo;
                    prevDots = dots;

                    setInformation(photonFile, prevLayerNo + (e.isShiftDown() ? -1 : 1), mouseX, mouseY);
                }
			}

			@Override
			public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_Z) {
                    pressed = false;
                    
                    setInformation(photonFile, prevLayerNo, mouseX, mouseY);
                    
                    dots = prevDots;
                    prevDots = null;

                    for (PhotonDot dot : dots) {
                        int dotX = dot.y - layerX;
                        int dotY = dot.x - layerY;

                        ((PhotonEditPanel) editArea).drawDot(dotX, dotY, layer, Color.CYAN);
                    }

                    editArea.repaint();
                }
			}
        });

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        editArea.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                pressedDot = getPosition(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                PhotonDot releasedDot = getPosition(e);
                if (pressedDot != null && releasedDot != null) {
                    boolean onOff = false;
                    if (!editModeSwap) {
                        byte original = layer.get(layerY + pressedDot.y, layerX + pressedDot.x);
                        onOff = original != PhotonLayer.OFF;
                        if (dots.contains(new PhotonDot(layerY + pressedDot.y, layerX + pressedDot.x))) {
                            onOff = !onOff;
                        }
                    }

                    int x1 = Integer.min(pressedDot.x, releasedDot.x);
                    int x2 = Integer.max(pressedDot.x, releasedDot.x);
                    int y1 = Integer.min(pressedDot.y, releasedDot.y);
                    int y2 = Integer.max(pressedDot.y, releasedDot.y);

                    for (int x = x1; x <= x2; x++) {
                        for (int y = y1; y <= y2; y++) {
                            if (editModeSwap) {
                                me.handleClick(x, y);
                            } else {
                                me.handleClick(x, y, onOff);
                            }
                        }
                    }
                }
            }
        });

        editArea.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {

                PhotonDot lastCursorDot = cursorDot;

                cursorDot = getPosition(e);
                if (lastCursorDot != null && !lastCursorDot.equals(cursorDot)) {
                    Color color = getColor(lastCursorDot);
                    ((PhotonEditPanel) editArea).drawDot(lastCursorDot.x, lastCursorDot.y, layer, color);
                    editArea.repaint();
                }
                if (cursorDot != null && !cursorDot.equals(lastCursorDot)) {
                    Color original = getColor(cursorDot);
                    Color color = original.brighter();
                    if (original.equals(Color.black)) {
                        color = Color.lightGray;
                    }
                    ((PhotonEditPanel) editArea).drawDot(cursorDot.x, cursorDot.y, layer, color);
                    editArea.repaint();
                }

            }
        });
    }

    private void handleClick(int x, int y, boolean onOff) {
        byte original = layer.get(layerY + y, layerX + x);
        boolean isOriginalOn = original != PhotonLayer.OFF;

        PhotonDot dot = new PhotonDot(layerY + y, layerX + x);

        if (dots.contains(dot)) {
            dots.remove(dot);
        }
        if ((onOff && !isOriginalOn) || (!onOff && isOriginalOn)) {
            dots.add(dot);
        }

        Color color = onOff ? Color.darkGray : Color.cyan;
        ;
        ((PhotonEditPanel) editArea).drawDot(x, y, layer, color);
        editArea.repaint();
    }

    private void handleClick(int x, int y) {
        Color color = isSet(x, y);
        ((PhotonEditPanel) editArea).drawDot(x, y, layer, color);
        editArea.repaint();
    }

    private Color isSet(int x, int y) {
        byte original = layer.get(layerY + y, layerX + x);
        boolean result = original != PhotonLayer.OFF;

        PhotonDot dot = new PhotonDot(layerY + y, layerX + x);
        if (dots.contains(dot)) {
            dots.remove(dot);

            switch (layer.get(layerY + y, layerX + x)) {
                case PhotonLayer.SUPPORTED:
                    return Color.decode("#008800");

                case PhotonLayer.CONNECTED:
                    return Color.decode("#FFFF00");

                case PhotonLayer.ISLAND:
                    return Color.decode("#FF0000");

                default:
                    return Color.black;

            }

        } else {
            dots.add(dot);
            return result ? Color.darkGray : Color.cyan;
        }
    }

    private Color getColor(PhotonDot dst) {
        byte original = layer.get(layerY + dst.y, layerX + dst.x);
        boolean result = original != PhotonLayer.OFF;

        PhotonDot dot = new PhotonDot(layerY + dst.y, layerX + dst.x);
        if (!dots.contains(dot)) {

            switch (original) {
                case PhotonLayer.SUPPORTED:
                    return Color.decode("#008800");

                case PhotonLayer.CONNECTED:
                    return Color.decode("#FFFF00");

                case PhotonLayer.ISLAND:
                    return Color.decode("#FF0000");

                default:
                    return Color.black;

            }

        } else {
            return result ? Color.black : Color.cyan;
        }
    }

    private void onOK() {
        if (dots.size() > 0) {
            for (PhotonDot dot : dots) {
                byte type = layer.get(dot.x, dot.y);
                if (type == PhotonLayer.OFF) {
                    layer.island(dot.x, dot.y);
                } else {
                    layer.remove(dot.x, dot.y, type);
                }
            }
            try {
                fileLayer.saveLayer(layer);
                photonFile.calculate(layerNo);

                // check next layer, just in case we created new islands
                if (layerNo < photonFile.getLayerCount() - 1) {
                    photonFile.calculate(layerNo + 1);
                }

            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
            mainForm.changeLayer();
            mainForm.showMarginAndIslandInformation();
        }
        dispose();
    }

    private void onCancel() {
        dispose();
    }


    public void setInformation(PhotonFile photonFile, int layerNo, int mouseX, int mouseY) {
        this.dots = new HashSet<>();
        this.layerNo = layerNo;
        this.photonFile = photonFile;
        this.fileLayer = photonFile.getLayer(layerNo);
        this.layer = fileLayer.getLayer();
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        
        mirrored = photonFile.getPhotonFileHeader().isMirrored();

        int indexX = (mouseX < 38) ? 1 : mouseX - 38;
        int indexY = (mouseY < 23) ? 1 : mouseY - 23;

        if (indexX + 74 >= photonFile.getWidth()) {
            indexX = photonFile.getWidth() - 74;
        }
        if (indexY + 44 >= photonFile.getHeight()) {
            indexY = photonFile.getHeight() - 44;
        }

        if (mirrored) {
            indexY = photonFile.getHeight() - indexY - 44;
        }

        ((PhotonEditPanel) editArea).setMirrored(mirrored);

        layerX = indexX - 1;
        layerY = indexY - 1;

        infoText.setText("Showing column " + indexX + " to " + (indexX + 74) + ", in row " + indexY + " to " + (indexY + 44) + ") | Layer " + layerNo);
        ((PhotonEditPanel) editArea).drawLayer(layerX, layerY, layer);
        editArea.repaint();
    }


    private void createUIComponents() {
        editArea = new PhotonEditPanel(780, 480);
    }

    private PhotonDot getPosition(MouseEvent e) {
        return getPosition(e.getX(), e.getY());
    }

    private PhotonDot getPosition(int mx, int my) {
        if (mirrored) {
            my = editArea.getHeight() - my - 1;
            my -= 5;
        }
        if (mx > 15 && my > 15) {
            int x = (mx - 15) / 10;
            int y = (my - 15) / 10;
            if (x < 75 && y < 45) {
                return new PhotonDot(x, y);
            }
        }
        return null;
    }


    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        createUIComponents();
        contentPane = new JPanel();
        contentPane.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(3, 1, new Insets(10, 10, 10, 10), -1, -1));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(panel1, new com.intellij.uiDesigner.core.GridConstraints(2, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0, false));
        final com.intellij.uiDesigner.core.Spacer spacer1 = new com.intellij.uiDesigner.core.Spacer();
        panel1.add(spacer1, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(panel2, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        buttonOK = new JButton();
        buttonOK.setText("OK");
        panel2.add(buttonOK, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonCancel = new JButton();
        buttonCancel.setText("Cancel");
        panel2.add(buttonCancel, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(panel3, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, new Dimension(-1, 30), new Dimension(-1, 30), new Dimension(-1, 30), 0, false));
        infoText = new JLabel();
        infoText.setText("");
        panel3.add(infoText, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        editModeBtn = new JButton();
        editModeBtn.setText("Using Swap");
        panel3.add(editModeBtn, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(panel4, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        editArea.setBackground(new Color(-1250068));
        editArea.setEnabled(true);
        panel4.add(editArea, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, new Dimension(300, 100), new Dimension(300, 100), null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() { return contentPane; }

}
