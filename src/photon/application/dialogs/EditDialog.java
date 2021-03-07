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
import photon.application.utilities.MainUtils;
import photon.file.PhotonFile;
import photon.file.parts.PhotonDot;
import photon.file.parts.PhotonFileLayer;
import photon.file.parts.PhotonLayer;
import photon.file.ui.PhotonEditPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

public class EditDialog extends JDialog {

    private static final int MAX_FILL_RADIUS_SQUARED = 15 * 15;

    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JLabel infoText;
    private JPanel editArea;
    private JComboBox modeCombo;
    private JLabel editModeHelp;

    private EditDialog me;
    private MainForm mainForm;
    private PhotonFile photonFile;
    private PhotonFileLayer fileLayer;
    private PhotonLayer layer;
    private int layerNo;
    private int layerX;
    private int layerY;
    private Set<EditDot> dots;
    private Stack<Set<EditDot>> operations;
    private Stack<Set<EditDot>> undoneOperations;

    private boolean mirrored;
    private PhotonDot cursorDot;

    private int mouseX;
    private int mouseY;
    private boolean inLayerPreview = false;

    private MouseAdapter currentEditModeHandler;

    private static class EditDot extends PhotonDot {
        private Color color;

        public EditDot(PhotonDot dot, Color color) {
            this(dot.x, dot.y, color);
        }

        public EditDot(int x, int y, Color color) {
            super(x, y);
            this.color = color;
        }


        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("EditDot{");
            sb.append("x=").append(x);
            sb.append(", y=").append(y);
            sb.append(", color=").append(color);
            sb.append('}');
            return sb.toString();
        }
    }

    private class ModePencilHandler extends MouseAdapter {
        PhotonDot lastCell;

        @Override
        public void mousePressed(MouseEvent e) {
            saveState();
            handleCellChange(e);
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            handleCellChange(e);
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            lastCell = null;
            if (e.getButton() == MouseEvent.BUTTON2) {
                flood(getPosition(e));
            }
        }

        private void handleCellChange(MouseEvent e) {
            if (inLayerPreview) return;

            PhotonDot currentCell = getPosition(e);
            if (currentCell != null) {
                if (!currentCell.equals(lastCell)) {
                    if (e.getButton() == MouseEvent.BUTTON1 || e.getButton() == MouseEvent.BUTTON3) {
                        boolean on = e.getButton() == MouseEvent.BUTTON1;
                        editPixel(currentCell, on);
                        editArea.repaint();

                    }
                    lastCell = currentCell;
                }
            }
        }

        private void flood(PhotonDot origin) {
            if (inLayerPreview) return;
            if (origin == null) {
                return;
            }

            saveState();

            int count = 0;

            // do we fill or erase?
            boolean on = !isDotOn(origin);

            int ox = origin.x;
            int oy = origin.y;

            Stack<PhotonDot> stack = new Stack<>();
            stack.push(origin);

            while (!stack.empty()) {
                PhotonDot dot = stack.pop();

                // inside edit view / layer?
                if (isValid(dot)) {

                    // check for walls
                    boolean dotOn = isDotOn(dot);
                    if (dotOn != on) {

                        // lets not flood the whole layer
                        int dx = dot.x - ox;
                        int dy = dot.y - oy;
                        int d = dx * dx + dy * dy;

                        if (d < MAX_FILL_RADIUS_SQUARED) {
                            editPixel(dot, on);
                            ++count;

                            stack.push(new PhotonDot(dot.x - 1, dot.y));
                            stack.push(new PhotonDot(dot.x, dot.y - 1));
                            stack.push(new PhotonDot(dot.x + 1, dot.y));
                            stack.push(new PhotonDot(dot.x, dot.y + 1));
                        }
                    }
                }
            }

            if (count > 0) {
                editArea.repaint();
            }


        }
    }

    private void editPixel(PhotonDot cell, boolean on) {
        byte original = layer.get(layerY + cell.y, layerX + cell.x);
        boolean isOriginalOn = original != PhotonLayer.OFF;
        Color originalColor = getOriginalColor(cell);

        EditDot dot = new EditDot(layerY + cell.y, layerX + cell.x, originalColor);

        if (dots.contains(dot)) {
            dots.remove(dot);
        }

        if (on) {
            if (!isOriginalOn) {
                dot.color = Color.cyan;
                dots.add(dot);
            }
        } else {
            if (isOriginalOn) {
                dot.color = Color.darkGray;
                dots.add(dot);
            }
        }

        ((PhotonEditPanel) editArea).drawDot(cell.x, cell.y, layer, dot.color);
    }

    ;

    private class ModeSwapHandler extends MouseAdapter {
        private PhotonDot pressedDot;
        private Rectangle rect;

        @Override
        public void mousePressed(MouseEvent e) {
            saveState();
            pressedDot = getPosition(e);
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            if (pressedDot != null) {

                clearRect();

                PhotonDot currentDot = getPosition(e);
                int x1 = Integer.min(pressedDot.x, currentDot.x);
                int x2 = Integer.max(pressedDot.x, currentDot.x);
                int y1 = Integer.min(pressedDot.y, currentDot.y);
                int y2 = Integer.max(pressedDot.y, currentDot.y);

                rect = new Rectangle(x1, y1, x2 - x1, y2 - y1);
                ((PhotonEditPanel) editArea).drawRect(rect, Color.cyan);
                editArea.repaint();
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            clearRect();
            PhotonDot releasedDot = getPosition(e);
            if (pressedDot != null && releasedDot != null) {

                int x1 = Integer.min(pressedDot.x, releasedDot.x);
                int x2 = Integer.max(pressedDot.x, releasedDot.x);
                int y1 = Integer.min(pressedDot.y, releasedDot.y);
                int y2 = Integer.max(pressedDot.y, releasedDot.y);

                for (int x = x1; x <= x2; x++) {
                    for (int y = y1; y <= y2; y++) {
                        Color color = isSet(x, y);
                        ((PhotonEditPanel) editArea).drawDot(x, y, layer, color);
                        editArea.repaint();
                    }
                }
            }
        }

        private void clearRect() {
            if (rect != null) {
                ((PhotonEditPanel) editArea).drawRect(rect, Color.decode("#999999"));
                rect = null;
            }
        }

    }

    ;

    private class ModeRectHandler extends MouseAdapter {
        private PhotonDot pressedDot;
        private Rectangle rect;

        @Override
        public void mousePressed(MouseEvent e) {
            saveState();
            pressedDot = getPosition(e);
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            if (pressedDot != null) {

                clearRect();

                PhotonDot currentDot = getPosition(e);
                int x1 = Integer.min(pressedDot.x, currentDot.x);
                int x2 = Integer.max(pressedDot.x, currentDot.x);
                int y1 = Integer.min(pressedDot.y, currentDot.y);
                int y2 = Integer.max(pressedDot.y, currentDot.y);

                rect = new Rectangle(x1, y1, x2 - x1, y2 - y1);
                Color color = e.getButton() == MouseEvent.BUTTON1 ? Color.cyan : Color.lightGray;
                ((PhotonEditPanel) editArea).drawRect(rect, color);
                editArea.repaint();
            }
        }

        private void clearRect() {
            if (rect != null) {
                ((PhotonEditPanel) editArea).drawRect(rect, Color.decode("#999999"));
                rect = null;
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            clearRect();
            PhotonDot releasedDot = getPosition(e);
            if (pressedDot != null && releasedDot != null) {

                boolean on = e.getButton() == MouseEvent.BUTTON1;

                int x1 = Integer.min(pressedDot.x, releasedDot.x);
                int x2 = Integer.max(pressedDot.x, releasedDot.x);
                int y1 = Integer.min(pressedDot.y, releasedDot.y);
                int y2 = Integer.max(pressedDot.y, releasedDot.y);

                for (int x = x1; x <= x2; x++) {
                    for (int y = y1; y <= y2; y++) {
                        editPixel(new PhotonDot(x, y), on);
                    }
                    editArea.repaint();
                }
            }
        }
    }

    ;

    private enum EditMode {
        pencil("Tip: Use left mouse to set and right mouse to unset pixels. Using the middle mouse button you can fill or clear areas."),
        rect("Tip: Use left mouse drag to set and right mouse drag to unset areas."),
        swap("Tip: Use left mouse to toggle pixels. Drag for toggling within rectangle area.");
        String help;

        EditMode(String help) {
            this.help = help;
        }
    }

    ;
    private EditMode editMode = EditMode.pencil;

    private Map<EditMode, MouseAdapter> editModeHandlerRegistry;

    public EditDialog(MainForm mainForm) {
        super(mainForm.frame);
        this.me = this;
        this.mainForm = mainForm;

        editModeHandlerRegistry = new HashMap<>();
        editModeHandlerRegistry.put(EditMode.pencil, new ModePencilHandler());
        editModeHandlerRegistry.put(EditMode.rect, new ModeRectHandler());
        editModeHandlerRegistry.put(EditMode.swap, new ModeSwapHandler());

        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

        buttonOK.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {onOK();}
        });

        buttonCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {onCancel();}
        });

        modeCombo.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JComboBox cb = (JComboBox) e.getSource();
                String modeName = ((String) cb.getSelectedItem()).toLowerCase();
                editMode = EditMode.valueOf(modeName);
                setEditMode(editMode);
            }
        });

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        contentPane.setFocusable(true);
        contentPane.addKeyListener(new KeyListener() {
            private int prevLayerNo;
            private boolean debounce = false;
            private Set<EditDot> prevDots;

            public boolean isLeftOrRight(KeyEvent e) {
                return e.getKeyCode() == KeyEvent.VK_LEFT || e.getKeyCode() == KeyEvent.VK_RIGHT;
            }

            @Override
            public void keyTyped(KeyEvent e) {}

            @Override
            public void keyPressed(KeyEvent e) {
                if (isLeftOrRight(e) && !debounce) {
                    debounce = true;
                    prevLayerNo = layerNo;
                    prevDots = dots;
                    inLayerPreview = true;

                    setInformation(photonFile, prevLayerNo + (e.getKeyCode() == KeyEvent.VK_LEFT ? -1 : 1), mouseX, mouseY);
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                if (isLeftOrRight(e)) {
                    debounce = false;
                    inLayerPreview = false;

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

        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        contentPane.registerKeyboardAction(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                undo();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_Z, MainUtils.getSystemDefaultModifierMask()), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        contentPane.registerKeyboardAction(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                redo();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_Z, MainUtils.getSystemDefaultModifierMask() | KeyEvent.SHIFT_DOWN_MASK), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        setEditMode(EditMode.pencil);

        editArea.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                handleCursor(e);
            }
        });
    }

    private void saveState() {
        Set<EditDot> state = new HashSet<>(dots);
        operations.push(state);
    }

    private void undo() {
        if (!operations.empty()) {
            Set<EditDot> lastState = new HashSet<>(operations.pop());
            undoneOperations.push(new HashSet<>(dots));
            dots = new HashSet<>(lastState);

            redraw();
        }
    }

    private void redo() {
        if (!undoneOperations.empty()) {
            Set<EditDot> nextState = undoneOperations.pop();
            operations.push(new HashSet<>(dots));
            dots = new HashSet<>(nextState);

            redraw();
        }
    }

    private void redraw() {
        ((PhotonEditPanel) editArea).drawLayer(layerX, layerY, layer);
        for (EditDot dot : dots) {
            ((PhotonEditPanel) editArea).drawDot(dot.y - layerX, dot.x - layerY, layer, dot.color);
        }
        editArea.repaint();
    }

    private void setEditMode(EditMode newEditMode) {
        editArea.removeMouseListener(currentEditModeHandler);
        editArea.removeMouseMotionListener(currentEditModeHandler);
        editMode = newEditMode;
        editModeHelp.setText(editMode.help);
        currentEditModeHandler = editModeHandlerRegistry.get(editMode);
        editArea.addMouseListener(currentEditModeHandler);
        editArea.addMouseMotionListener(currentEditModeHandler);
    }

    private void handleCursor(MouseEvent e) {
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
            } else if (original.equals(Color.cyan)) {
                color = Color.lightGray;
            }
            ((PhotonEditPanel) editArea).drawDot(cursorDot.x, cursorDot.y, layer, color);
            editArea.repaint();
        }
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
            Color color = result ? Color.darkGray : Color.cyan;
            dots.add(new EditDot(dot, color));
            return color;
        }
    }

    private Color getOriginalColor(PhotonDot dst) {
        byte original = layer.get(layerY + dst.y, layerX + dst.x);
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
    }

    // check if dot is currently on (edited or original)
    private boolean isDotOn(PhotonDot dot) {
        Color color = getColor(dot);
        return !(color.equals(Color.black) || color.equals(Color.darkGray));
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
            return result ? Color.darkGray : Color.cyan;
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
        setInformation(photonFile, layerNo);

        mirrored = photonFile.getPhotonFileHeader().isMirrored();

        this.mouseX = mouseX;
        this.mouseY = mouseY;

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

        layerX = Math.max(0, indexX - 1);
        layerY = Math.max(0, indexY - 1);

        infoText.setText("Showing column " + indexX + " to " + (indexX + 74) + ", in row " + indexY + " to " + (indexY + 44) + ")");
        ((PhotonEditPanel) editArea).drawLayer(layerX, layerY, layer);
        editArea.repaint();
    }

    public void setInformation(PhotonFile photonFile, int layerNo) {
        this.dots = new HashSet<>();
        operations = new Stack<>();
        undoneOperations = new Stack<>();
        this.layerNo = layerNo;
        this.photonFile = photonFile;
        this.fileLayer = photonFile.getLayer(layerNo);
        this.layer = fileLayer.getLayer();
        mirrored = photonFile.getPhotonFileHeader().isMirrored();
        editArea.repaint();
    }


    private void createUIComponents() {
        editArea = new PhotonEditPanel(780, 480);
    }

    private PhotonDot getPosition(MouseEvent e) {
        int my = e.getY();
        if (mirrored) {
            my = editArea.getHeight() - e.getY() - 1;
        }
        if (e.getX() > 15 && my > 15) {
            int x = (e.getX() - 15) / 10;
            int y = (my - 15) / 10;
            if (x < 75 && y < 45) {
                return new PhotonDot(x, y);
            }
        }
        return null;
    }

    private boolean isValid(PhotonDot dot) {
        if (dot.x < 0) return false;
        if (dot.x >= 75) return false;
        if (dot.y < 0) return false;
        if (dot.y >= 45) return false;
        return true;
    }

}
