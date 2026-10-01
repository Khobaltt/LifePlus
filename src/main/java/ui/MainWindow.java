package main.java.ui;

import main.java.core.HexGrid;

import javax.swing.*;
import java.awt.*;

public class MainWindow extends JFrame {

    private static final int WINDOW_WIDTH = 1200;
    private static final int WINDOW_HEIGHT = 800;

    private JPanel gridPanel;
    private JPanel toolPanel;

    public MainWindow() {
        super("Life Plus");

        initializeWindow();
        initializeComponents();
        initializeMenuBar();
    }

    /*
     * Configures the main JFrame.
     */
    private void initializeWindow() {
        setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        setMinimumSize(new Dimension(800, 600));

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        setLayout(new BorderLayout());
    }

    /*
     * Creates the major sections of the window
     * Currently uses placeholder panels until GridPanel
     * and ToolPalette are implemented
     */
    private void initializeComponents() {

        // main simulation area
        setGridPanel();
        gridPanel.setBackground(Color.DARK_GRAY);

        // toolbar controls
        toolPanel = new JPanel();
        toolPanel.setPreferredSize(new Dimension(250, 0));
        toolPanel.setBackground(new Color(230, 230, 230));

        add(gridPanel, BorderLayout.CENTER);
        add(toolPanel, BorderLayout.WEST);
    }

    /*
     * Creates the application menu bar
     */
    private void initializeMenuBar() {

        // create menu bar
        JMenuBar menuBar = new JMenuBar();

        // file menu
        JMenu fileMenu = new JMenu("File");

        JMenuItem newItem = new JMenuItem("New");
        JMenuItem openItem = new JMenuItem("Open");
        JMenuItem saveItem = new JMenuItem("Save");
        JMenuItem exitItem = new JMenuItem("Exit");

        exitItem.addActionListener(e -> {
            System.exit(0);
        });

        fileMenu.add(newItem);
        fileMenu.add(openItem);
        fileMenu.add(saveItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);

        // simulation menu
        JMenu simulationMenu = new JMenu("Simulation");

        JMenuItem playItem = new JMenuItem("Play");
        JMenuItem pauseItem = new JMenuItem("Pause");
        JMenuItem stepItem = new JMenuItem("Step");

        simulationMenu.add(playItem);
        simulationMenu.add(pauseItem);
        simulationMenu.addSeparator();
        simulationMenu.add(stepItem);

        // add menus to menu bar
        menuBar.add(fileMenu);
        menuBar.add(simulationMenu);

        // add the menu bar to window
        setJMenuBar(menuBar);
    }

    /*
     * Replaces the current grid panel
     */
    public void setGridPanel() {

        HexGrid simulationGrid = new HexGrid();
        gridPanel = new GridPanel(simulationGrid);

        add(gridPanel, BorderLayout.CENTER);

        revalidate();
        repaint();
    }

    /*
     * Replaces the current tool panel
     */
    public void setToolPanel(JPanel panel) {
        remove(toolPanel);

        toolPanel = panel;

        add(toolPanel, BorderLayout.EAST);

        revalidate();
        repaint();
    }

    /*
     * Starts and displays the window
     */
    public static void launch() {
        SwingUtilities.invokeLater(() -> {
            MainWindow window = new MainWindow();
            window.setVisible(true);
        });
    }
}
