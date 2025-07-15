package com.github.timan1802.typingbongocat;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

/**
 * 에디터 위에 표시되는 단일 이미지(Bongo Cat)를 관리하는 컴포넌트입니다.
 * 키 입력 타이밍에 따라 상태를 변경하고, 자체 타이머를 통해 이미지를 숨깁니다.
 * @author Baptiste Mesta
 * @author Gemini Code Assist (Refactored for stateful image management)
 */
public class BongoCatAnimator extends JComponent implements ComponentListener, Disposable {

    private static final Logger LOG = Logger.getInstance(BongoCatAnimator.class);

    // --- 이미지 리소스 로딩 ---
    private static final BufferedImage BONGO_RIGHT_IMG = loadImage("/img/bongo_right.png");
    private static final BufferedImage BONGO_LEFT_IMG = loadImage("/img/bongo_left.png");
    private static final BufferedImage BONGO_MIDDLE_IMG = loadImage("/img/bongo_middle.png");

    private final JComponent parent;
    private final Editor editor;

    // --- 상태 관리를 위한 필드 ---
    private volatile BufferedImage currentImage;
    private long lastKeyPressTime = 0;
    private boolean lastHandWasRight = false;

    // --- 위치 저장을 위한 필드 ---
    private int x;
    private int y;

    // --- 시간 기반 로직을 위한 타이머 (Swing Timer 사용) ---
    private final Timer middleTimer;
    private final Timer hideTimer;

    public BongoCatAnimator(Editor editor) {
        this.editor = editor;
        this.parent = editor.getContentComponent();
        parent.add(this);
        updateBounds();
        setVisible(true);
        parent.addComponentListener(this);

        middleTimer = new Timer(500, e -> {
            currentImage = BONGO_MIDDLE_IMG;
            repaint();
        });
        middleTimer.setRepeats(false);

        hideTimer = new Timer(1000, e -> {
            currentImage = null;
            repaint();
        });
        hideTimer.setRepeats(false);
    }

    /**
     * 지정된 경로에서 이미지를 로드하고 100x100 크기로 조절합니다.
     * @param path 리소스 경로
     * @return 크기가 조절된 이미지
     */
    private static BufferedImage loadImage(String path) {
        try (InputStream stream = BongoCatAnimator.class.getResourceAsStream(path)) {
            if (stream == null) {
                LOG.warn("리소스를 찾을 수 없습니다: " + path);
                return null;
            }
            BufferedImage originalImage = ImageIO.read(stream);
            // 로드된 이미지를 리사이즈합니다.
            return resizeImage(originalImage, 200, 200);
        } catch (IOException e) {
            LOG.error("이미지 로딩 실패: " + path, e);
            return null;
        }
    }

    /**
     * BufferedImage의 크기를 조절하는 헬퍼 메소드입니다.
     * @param originalImage 원본 이미지
     * @param targetWidth 목표 너비
     * @param targetHeight 목표 높이
     * @return 크기가 조절된 새 이미지
     */
    private static BufferedImage resizeImage(BufferedImage originalImage, int targetWidth, int targetHeight) {
        if (originalImage == null) {
            return null;
        }
        // 목표 크기의 새 이미지를 생성합니다.
        BufferedImage resizedImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = resizedImage.createGraphics();

        // 이미지 품질을 높이기 위해 렌더링 힌트를 설정합니다.
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 원본 이미지를 새 이미지에 크기를 조절하여 그립니다.
        g2d.drawImage(originalImage, 0, 0, targetWidth, targetHeight, null);
        g2d.dispose();

        return resizedImage;
    }


    /**
     * 키 입력이 있을 때마다 호출되어 고양이의 상태와 위치를 업데이트합니다.
     * @param caretPosition 캐럿의 현재 위치
     */
    public void update(Point caretPosition) {
        // 전달받은 캐럿 위치를 저장합니다.
        this.x = caretPosition.x;
        this.y = caretPosition.y;

        long now = System.currentTimeMillis();

        if (now - lastKeyPressTime < 500) {
            // 연속 입력: 마지막 손과 반대 손을 보여줍니다.
            if (lastHandWasRight) {
                currentImage = BONGO_LEFT_IMG;
                lastHandWasRight = false;
            } else {
                currentImage = BONGO_RIGHT_IMG;
                lastHandWasRight = true;
            }
        } else {
            // 첫 입력 또는 오랜만의 입력: 항상 오른손으로 시작합니다.
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
            // 저장된 캐럿 위치(this.x, this.y)를 기준으로 이미지를 그립니다.
            // 이미지의 좌상단 모서리 좌표를 계산하여 캐럿 바로 위에 위치시킵니다.
            int drawX = this.x - (currentImage.getWidth() / 2);
            int drawY = this.y - currentImage.getHeight() - 10; // 캐럿 위치보다 10px 위
            g.drawImage(currentImage, drawX, drawY, null);
        }
    }

    @Override
    public void componentResized(ComponentEvent e) {
        updateBounds();
    }

    @Override
    public void componentMoved(ComponentEvent e) {
        updateBounds();
    }



    private void updateBounds() {
        this.setBounds(editor.getScrollingModel().getVisibleArea().getBounds());
    }

    @Override
    public void componentShown(ComponentEvent e) {}

    @Override
    public void componentHidden(ComponentEvent e) {}

    @Override
    public void dispose() {
        parent.removeComponentListener(this);
        parent.remove(this);
        middleTimer.stop();
        hideTimer.stop();
    }
}