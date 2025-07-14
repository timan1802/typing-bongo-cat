package com.github.timan1802.catmode;/*
 * Copyright 2015 Baptiste Mesta
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.ui.JBColor;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.util.ArrayList;
import java.util.Iterator;
import javax.swing.JComponent;

/**
 * @author Baptiste Mesta
 */
public class ParticleContainer extends JComponent implements ComponentListener {

    private final JComponent parent;
    private final Editor editor;
    private boolean shakeDir;
    private ArrayList<Particle> particles = new ArrayList<>(50);
    private Point currentCursorPosition; // 현재 마우스 커서 위치를 저장할 변수

    public ParticleContainer(Editor editor) {
        this.editor = editor;
        parent = this.editor.getContentComponent();
        parent.add(this);
        updateBounds();
        setVisible(true);
        parent.addComponentListener(this);
    }

    private void shakeEditor(JComponent parent, int dx, int dy, boolean dir) {
        final Rectangle bounds = parent.getBounds();
        parent.setBounds(bounds.x + (dir ? -dx : dx), bounds.y + (dir ? -dy : dy), bounds.width, bounds.height);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        renderParticles(g);
    }

    public void updateParticles() {
        if (!particles.isEmpty()) {
            ArrayList<Particle> tempParticles = new ArrayList<>(particles);
            final Iterator<Particle> particleIterator = tempParticles.iterator();
            while (particleIterator.hasNext()) {
                if (particleIterator.next().update()) {
                    particleIterator.remove();
                }
            }
            particles = tempParticles;
            this.repaint();
        }

    }

    public void addParticle(int x, int y) {
        //TODO configurable
        int dx, dy;
        dx = (int) (Math.random() * 4) * (Math.random() > 0.5 ? -1 : 1);
        dy = (int) (Math.random() * -3 - 1);

        int size = (int) (Math.random() * 3 + 1);
//        int size = 100;
        int life = 60;
        final Particle e = new Particle(x, y, dx, dy, size, life, JBColor.darkGray);
        particles.add(e);
    }

    public void renderParticles(Graphics g) {
        for (Particle particle : particles) {
            particle.render(g);
        }
    }

    public void update(Point point) {
        //TODO configurable
        for (int i = 0; i < 7; i++) {
            addParticle(point.x, point.y);
        }
        shakeEditor(parent, 5, 5, shakeDir);
        shakeDir = !shakeDir;
        this.repaint();
    }

    @Override
    public void componentResized(ComponentEvent e) {
        updateBounds();

        Logger.getInstance(this.getClass()).info("Resized");

    }

    private void updateBounds() {
        ParticleContainer.this.setBounds(editor.getScrollingModel().getVisibleArea().getBounds());
    }

    @Override
    public void componentMoved(ComponentEvent e) {
        updateBounds();
        Logger.getInstance(this.getClass()).info("Moved");

    }

    @Override
    public void componentShown(ComponentEvent e) {

    }

    @Override
    public void componentHidden(ComponentEvent e) {

    }
}
