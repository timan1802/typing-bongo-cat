package com.github.timan1802.catmode;
/*
 * Copyright 2015 Baptiste Mesta
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import javax.imageio.ImageIO;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Baptiste Mesta
 */
public class Particle {

    private static final List<BufferedImage> PARTICLE_IMAGES = new ArrayList<>();
    private static final int FRAME_COUNT = 9;

    static {
        // static 블록에서 발생하는 예외를 더 명확하게 처리합니다.
        for (int i = 1; i <= FRAME_COUNT; i++) {
            String path = String.format("/BongoCat_img/bongo_%d.png", i);
            // try-with-resources를 사용하여 InputStream을 안전하게 관리합니다.
            try (InputStream stream = Particle.class.getResourceAsStream(path)) {
                // getResourceAsStream이 null을 반환하는지 확인하여 ExceptionInInitializerError를 방지합니다.
                if (stream == null) {
                    System.err.println("리소스를 찾을 수 없습니다. 경로를 확인하세요: " + path);
                    continue; // 다음 이미지 로드를 시도합니다.
                }
                BufferedImage img = ImageIO.read(stream);
                if (img != null) {
                    PARTICLE_IMAGES.add(img);
                } else {
                    System.err.println("이미지 파일을 디코딩할 수 없습니다: " + path);
                }
            } catch (IOException e) {
                System.err.println("이미지 파일 로딩 중 에러가 발생했습니다: " + path);
                e.printStackTrace();
            }
        }
    }

    private int x;
    private int y;
    private final int dx;
    private final int dy;
    private final int size;
    private int life;
    private final int maxLife; // 애니메이션 진행률 계산을 위해 초기 life 값을 저장
    private final Color c;

    private BufferedImage currentImage;

    public Particle(int x, int y, int dx, int dy, int size, int life, Color c) {
        this.x = x;
        this.y = y;
        this.dx = dx;
        this.dy = dy;
        this.size = size;
        this.life = life;
        this.maxLife = life; // 생성 시의 life를 maxLife로 저장
        this.c = c;
    }

    public boolean update() {
//        x += dx;
//        y += dy;
        life--;

        // 애니메이션을 위해 현재 이미지를 업데이트합니다.
        if (!PARTICLE_IMAGES.isEmpty()) {
            // 파티클의 수명 비율을 계산합니다.
            double lifeRatio = Math.max(0, (double) life / maxLife);
            // 수명이 줄어들수록 애니메이션 프레임이 진행되도록 인덱스를 계산합니다.
            int frameIndex = (int) ((1.0 - lifeRatio) * (PARTICLE_IMAGES.size() - 1));
            // 인덱스가 배열 범위를 벗어나지 않도록 보정합니다.
            frameIndex = Math.min(PARTICLE_IMAGES.size() - 1, Math.max(0, frameIndex));
            this.currentImage = PARTICLE_IMAGES.get(frameIndex);
        }

        return life <= 0;
    }

    public void render(Graphics g) {
        if (life > 0) {
            Graphics2D g2d = (Graphics2D) g.create();

            // --- 수정된 부분 ---
            // 파티클의 남은 수명에 따라 투명도를 조절합니다.
            // lifeRatio는 1.0f (최대 수명) 에서 0.0f (소멸) 까지 변합니다.
            float lifeRatio = Math.max(0f, (float) life / maxLife);

            // AlphaComposite를 설정하여 전체 파티클에 페이드 아웃 효과를 적용합니다.
            // SRC_OVER는 표준 알파 블렌딩 규칙이며, lifeRatio 값으로 투명도를 조절합니다.
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, lifeRatio));
            // --- 수정 종료 ---

            if (currentImage != null) {
                // 사각형 대신 이미지를 그립니다.
                // 이미지는 파티클의 (x, y)를 중심으로 'size' 크기에 맞춰 그려집니다.
                int drawX = x - (size / 2);
                int drawY = y - (size / 2);
                g2d.drawImage(currentImage, drawX, drawY, 100, 100, null);
            } else {
                // 이미지 로딩에 실패한 경우, 기존처럼 사각형을 그립니다.
                // setComposite가 이미 적용되었으므로 색상만 설정하면 투명도가 반영됩니다.
                g2d.setColor(c);
                g2d.fillRect(x - (size / 2), y - (size / 2), size, size);
            }

            g2d.dispose();
        }
    }

    @Override
    public String toString() {
        return "Particle{" +
            "x=" + x +
            ", y=" + y +
            ", dx=" + dx +
            ", dy=" + dy +
            ", size=" + size +
            ", life=" + life +
            ", c=" + c +
            '}';
    }
}