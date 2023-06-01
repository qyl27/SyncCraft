package cx.rain.synccraft.sync.gui;

import cx.rain.synccraft.sync.config.SyncConfigManager;
import cx.rain.synccraft.sync.config.SyncLanguageConfig;

import javax.swing.*;
import java.awt.*;

public class SyncUI {
    private SyncConfigManager configManager;
    private SyncLanguageConfig language;
    private JFrame frame;

    private JPanel panel;
    private JProgressBar progressBarTotal;
    private JLabel labelTotal;
    private JProgressBar progressBarSingle;
    private JLabel labelSingle;
    private JButton buttonCancel;
    private JButton buttonHide;

    public SyncUI(SyncConfigManager configManager) {
        this.configManager = configManager;
        this.language = configManager.getLanguage();

        init();
    }

    private void init() {
//        JFrame.setDefaultLookAndFeelDecorated(true);

        frame = new JFrame(language.windowTitle);

        var graphicsDevice = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        var width = graphicsDevice.getDisplayMode().getWidth() >= 1920 ? (graphicsDevice.getDisplayMode().getWidth() / 7) : 384;
        var height = graphicsDevice.getDisplayMode().getHeight() / 5;
        setSize(width, height);

        panel = new JPanel();

        progressBarTotal = new JProgressBar();
        labelTotal = new JLabel(language.processGetManifest);

        progressBarSingle = new JProgressBar();
        labelSingle = new JLabel();

        buttonCancel = new JButton(language.cancelLaunch);
        buttonHide = new JButton(language.hideWindow);

        panel.add(progressBarTotal);
        panel.add(labelTotal);
        panel.add(progressBarSingle);
        panel.add(labelSingle);
        panel.add(buttonCancel);
        panel.add(buttonHide);

        frame.setContentPane(panel);

        show();
    }

    public void setSize(int width, int height) {
        frame.setSize(width, height);
    }

    public void show() {
        frame.setVisible(true);
    }

    public void hide() {
        frame.setVisible(false);
    }
}
