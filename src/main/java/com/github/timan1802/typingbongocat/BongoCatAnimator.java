package com.github.timan1802.typingbongocat;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.util.ui.ImageUtil;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.Timer;

public class BongoCatAnimator extends JComponent implements ComponentListener, Disposable {

    private static final Logger LOG = Logger.getInstance(BongoCatAnimator.class);

    // --- 추가된 부분: 상수 정의 ---
    private static final int IMAGE_SIZE = 100; // 이미지 크기
    private static final int TYPING_INTERVAL_MS = 500; // 연속 입력으로 간주할 시간 (ms)
    private static final int MIDDLE_STATE_DELAY_MS = 500; // 중간 상태로 바뀌기까지의 지연 시간 (ms)
    private static final int HIDE_DELAY_MS = 1000; // 이미지가 사라지기까지의 지연 시간 (ms)
    private static final int CARET_Y_OFFSET = 10; // 캐럿 위의 Y축 간격

    // --- 이미지 리소스 로딩 ---
    private static final BufferedImage BONGO_RIGHT_IMG = loadImage("/img/cat/100/bongo_right.png");
    private static final BufferedImage BONGO_LEFT_IMG = loadImage("/img/cat/100/bongo_left.png");
    private static final BufferedImage BONGO_MIDDLE_IMG = loadImage("/img/cat/100/bongo_middle.png");

    private final JComponent parent;
    private final Editor editor;

    private volatile BufferedImage currentImage;
    private long lastKeyPressTime = 0;
    private boolean lastHandWasRight = false;

    private int x;
    private int y;

    private final Timer middleTimer;
    private final Timer hideTimer;

    public BongoCatAnimator(Editor editor) {
        this.editor = editor;
        this.parent = editor.getContentComponent();
        parent.add(this);
        updateBounds();
        setVisible(true);
        parent.addComponentListener(this);

        middleTimer = new Timer(MIDDLE_STATE_DELAY_MS, e -> {
            currentImage = BONGO_MIDDLE_IMG;
            repaint();
        });
        middleTimer.setRepeats(false);

        hideTimer = new Timer(HIDE_DELAY_MS, e -> {
            currentImage = null;
            repaint();
        });
        hideTimer.setRepeats(false);
    }

    private static BufferedImage loadImage(String path) {
        try (InputStream stream = BongoCatAnimator.class.getResourceAsStream(path)) {
            if (stream == null) {
                LOG.warn("리소스를 찾을 수 없습니다: " + path);
                return null;
            }
            BufferedImage originalImage = ImageIO.read(stream);
//            return resizeImage(originalImage, IMAGE_SIZE, IMAGE_SIZE);
            return originalImage;
        } catch (IOException e) {
            LOG.error("이미지 로딩 실패: " + path, e);
            return null;
        }
    }

    private static BufferedImage resizeImage(BufferedImage originalImage, int targetWidth, int targetHeight) {
        if (originalImage == null) return null;
        BufferedImage resizedImage = ImageUtil.createImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = resizedImage.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.drawImage(originalImage, 0, 0, targetWidth, targetHeight, null);
        g2d.dispose();
        return resizedImage;
    }

    public void update(Point caretPosition) {
        this.x = caretPosition.x;
        this.y = caretPosition.y;

        long now = System.currentTimeMillis();

        if (now - lastKeyPressTime < TYPING_INTERVAL_MS) {
            if (lastHandWasRight) {
                currentImage = BONGO_LEFT_IMG;
                lastHandWasRight = false;
            } else {
                currentImage = BONGO_RIGHT_IMG;
                lastHandWasRight = true;
            }
        } else {
            currentImage = BONGO_RIGHT_IMG;
            lastHandWasRight = true;
        }
        lastKeyPressTime = now;

        middleTimer.restart();
        hideTimer.restart();

        this.repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (currentImage != null) {
            int drawX = this.x - (currentImage.getWidth() / 2);
            int drawY = this.y - currentImage.getHeight() - CARET_Y_OFFSET;
            g.drawImage(currentImage, drawX, drawY, null);
        }
    }

    @Override
    public void componentResized(ComponentEvent e) { updateBounds(); }

    @Override
    public void componentMoved(ComponentEvent e) { updateBounds(); }

    private void updateBounds() {
        this.setBounds(editor.getScrollingModel().getVisibleArea().getBounds());
    }

    @Override
    public void componentShown(ComponentEvent e) {}

    @Override
    public void componentHidden(ComponentEvent e) {}

    /**
     * 이 컴포넌트가 소멸될 때 호출되어, 부모로부터 자신을 제거하고 타이머를 중지하여 메모리 누수를 방지합니다.
     */
    @Override
    public void dispose() {
        parent.removeComponentListener(this);
        parent.remove(this);
        middleTimer.stop();
        hideTimer.stop();
    }
}