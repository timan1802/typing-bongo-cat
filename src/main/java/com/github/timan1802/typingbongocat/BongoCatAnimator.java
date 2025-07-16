package com.github.timan1802.typingbongocat;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.util.ui.ImageUtil;
import java.awt.AlphaComposite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;
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
    // --- BAM 효과 상수 ---
    private static final int BAM_THRESHOLD = 20; // 'BAM!' 효과 발동 연속 입력 횟수
    private static final int BAM_SHAKE_DURATION_MS = 300; // 'BAM!' 효과 지속 시간 (ms)
    private static final int BAM_GROW_FADE_DURATION_MS = 1200; // 'BAM!' 확대/소멸 효과 시간 (ms)
    private static final int BAM_TOTAL_DURATION_MS = BAM_SHAKE_DURATION_MS + BAM_GROW_FADE_DURATION_MS;
    private static final int BAM_SHAKE_INTENSITY = 5; // 'BAM!' 효과 흔들림 강도 (pixels)
    private static final float BAM_MAX_SCALE = 2.0f; // 'BAM!' 효과 최대 확대 배율

    // --- 이미지 리소스 로딩 ---
    private static final BufferedImage BONGO_RIGHT_IMG = loadImage("/img/cat/100/bongo_right.png");
    private static final BufferedImage BONGO_LEFT_IMG = loadImage("/img/cat/100/bongo_left.png");
    private static final BufferedImage BONGO_MIDDLE_IMG = loadImage("/img/cat/100/bongo_middle.png");
    private static final BufferedImage BONGO_BAM_IMG = loadImage("/img/cat/457/bongo_bam.png");

    private final JComponent parent;
    private final Editor editor;
     private final Random random = new Random();
    private long lastKeyPressTime = 0;
    // --- 타이머 ---
    private final Timer middleTimer;
    private boolean lastHandWasRight = false;
    private final Timer bamEffectTimer;
    // --- 상태 변수 ---
    private volatile BufferedImage currentImage;
     private int consecutiveKeyPressCount = 0;
     private volatile boolean isBamEffectActive = false;
     private long bamStartTime;
    // BAM 효과 중 입력된 키를 저장하기 위한 카운터
    private int keyPressCount = 0;
    private Point lastQueuedCaretPosition;

    private int x;
    private int y;
    // BAM 애니메이션을 위한 변수
    private float currentScale = 1.0f;
    private final Timer hideTimer;
    private float currentAlpha = 1.0f;

    public BongoCatAnimator(Editor editor) {
        this.editor = editor;
        this.parent = editor.getContentComponent();
        parent.add(this);
        updateBounds();
        setVisible(true);
        parent.addComponentListener(this);

        middleTimer = new Timer(MIDDLE_STATE_DELAY_MS, e -> {
            // 사용자가 입력을 멈췄을 때 호출됩니다. 이때 BAM! 효과를 체크합니다.
            if (consecutiveKeyPressCount >= BAM_THRESHOLD) {
                showBamEffect();
            } else {
                // 일반적인 멈춤: 중간 상태로 전환합니다.
                currentImage = BONGO_MIDDLE_IMG;
                repaint();
            }
            // 효과를 처리했으므로 연속 입력 횟수를 초기화합니다.
            consecutiveKeyPressCount = 0;
        });
        middleTimer.setRepeats(false);

        hideTimer = new Timer(HIDE_DELAY_MS, e -> {
            currentImage = null;
            repaint();
        });
        hideTimer.setRepeats(false);

        // BAM 효과 전체(흔들림 + 확대/소멸)를 제어하는 타이머
        bamEffectTimer = new Timer(25, e -> { // 25ms (약 40fps)
            long elapsed = System.currentTimeMillis() - bamStartTime;

            if (elapsed >= BAM_TOTAL_DURATION_MS) {
                // --- BAM 효과 완전 종료 ---
                isBamEffectActive = false;
                currentImage = null;
                ((Timer) e.getSource()).stop();

                // 효과가 끝난 후, 큐에 쌓인 입력이 있는지 확인
                if (keyPressCount > 0) {
                    // 쌓인 입력을 기반으로 새로운 타이핑 시퀀스를 시작
                    consecutiveKeyPressCount = keyPressCount;
                    lastKeyPressTime = System.currentTimeMillis();
                    x = lastQueuedCaretPosition.x;
                    y = lastQueuedCaretPosition.y;
                    currentImage = BONGO_RIGHT_IMG;
                    lastHandWasRight = true;

                    middleTimer.restart();
                    hideTimer.restart();

                    keyPressCount = 0;
                    lastQueuedCaretPosition = null;
                }
            } else if (elapsed >= BAM_SHAKE_DURATION_MS) {
                // --- 확대 및 소멸 단계 ---
                float progress = (float) (elapsed - BAM_SHAKE_DURATION_MS) / BAM_GROW_FADE_DURATION_MS;
                currentScale = 1.0f + (BAM_MAX_SCALE - 1.0f) * progress;
                currentAlpha = 1.0f - progress;
            } else {
                // --- 흔들림 단계 ---
                currentScale = 1.0f;
                currentAlpha = 1.0f;
            }

            repaint();
        });
        bamEffectTimer.setRepeats(true);
    }

    private static BufferedImage loadImage(String path) {
        try (InputStream stream = BongoCatAnimator.class.getResourceAsStream(path)) {
            if (stream == null) {
                LOG.warn("Resource not found: " + path);
                return null;
            }
            BufferedImage originalImage = ImageIO.read(stream);
            return resizeImage(originalImage, IMAGE_SIZE, IMAGE_SIZE);
        } catch (IOException e) {
            LOG.error("Image loading failure: " + path, e);
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

        // BAM 효과가 진행 중일 때는 키 입력만 큐에 저장
        if (isBamEffectActive) {
            keyPressCount++;
            lastQueuedCaretPosition = caretPosition;
            return;
        }

        // 새로운 키 입력이 있었으므로, 기존의 모든 타이머와 효과를 중지합니다.
        middleTimer.stop();
        hideTimer.stop();
        bamEffectTimer.stop();

        
        long now = System.currentTimeMillis();
        boolean isConsecutive = (now - lastKeyPressTime) < TYPING_INTERVAL_MS;

        if (isConsecutive) {
            consecutiveKeyPressCount++;
            // 연속 입력: 손을 교체합니다.
            if (lastHandWasRight) {
                currentImage = BONGO_LEFT_IMG;
                lastHandWasRight = false;
            } else {
                currentImage = BONGO_RIGHT_IMG;
                lastHandWasRight = true;
            }
        } else {
            // 새로운 입력 시작: 카운터를 1로 설정하고 오른손으로 시작합니다.
            consecutiveKeyPressCount = 1;
            currentImage = BONGO_RIGHT_IMG;
            lastHandWasRight = true;
        }

        // 타이핑이 멈췄을 때를 대비해 타이머를 다시 시작합니다.
        middleTimer.restart();
        hideTimer.restart();

        lastKeyPressTime = now;
        this.repaint();
     }

     private void showBamEffect() {
        // 다른 타이머 중지
        middleTimer.stop();
        hideTimer.stop();

         // BAM 효과 상태 설정
         currentImage = BONGO_BAM_IMG;
         isBamEffectActive = true;
         bamStartTime = System.currentTimeMillis();
        // 애니메이션 변수 초기화
        currentScale = 1.0f;
        currentAlpha = 1.0f;

         // 흔들림 효과 타이머 시작
        bamEffectTimer.start();
     }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (currentImage != null) {
            Graphics2D g2d = (Graphics2D) g.create();

            try {
                int drawX = this.x - (currentImage.getWidth() / 2);
                int drawY = this.y - currentImage.getHeight() - CARET_Y_OFFSET;

                if (isBamEffectActive) {
                    // 흔들림 효과 적용
                    if (System.currentTimeMillis() - bamStartTime < BAM_SHAKE_DURATION_MS) {
                        drawX += random.nextInt(BAM_SHAKE_INTENSITY * 2) - BAM_SHAKE_INTENSITY;
                        drawY += random.nextInt(BAM_SHAKE_INTENSITY * 2) - BAM_SHAKE_INTENSITY;
                    }

                    // 확대 및 투명도 효과 적용
                    int imgWidth = currentImage.getWidth();
                    int imgHeight = currentImage.getHeight();
                    int scaledWidth = (int) (imgWidth * currentScale);
                    int scaledHeight = (int) (imgHeight * currentScale);
                    int scaledDrawX = drawX - (scaledWidth - imgWidth) / 2;
                    int scaledDrawY = drawY - (scaledHeight - imgHeight) / 2;

                    g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0, Math.min(1, currentAlpha))));
                    g2d.drawImage(currentImage, scaledDrawX, scaledDrawY, scaledWidth, scaledHeight, null);
                } else {
                    g2d.drawImage(currentImage, drawX, drawY, null);
                }
            } finally {
                g2d.dispose();
            }
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
        bamEffectTimer.stop();
    }
}