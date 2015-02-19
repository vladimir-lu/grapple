package org.halfway.grapple.gui;

import com.google.common.base.Verify;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Ellipse2D;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A rudimentary circular spinner that gives the user the satisfaction that something is happening
 */
class CircularSpinner extends JComponent {
    private static final double MARGIN_MULT = 0.2;
    private static final double INNER_MULT = Math.sqrt(2) / (Math.E * Math.E);
    private static final double RAD_MULT = 2 * Math.PI * 0.01d;

    private final AtomicInteger currentPercentage;

    private final Stroke fatStroke = new BasicStroke(2);
    private final Timer timer;
    private double outerRadius;
    private double innerRadius;
    private double innerOffset;

    private Shape mainCircle = null;

    /**
     * The parameters can be used to control the rate of animation
     *
     * @param timerInterval The interval of the timer in milliseconds
     * @param progressStep  The number of percentage points to go round each time
     */
    public CircularSpinner(final int timerInterval, final int progressStep) {
        Verify.verify(timerInterval > 0, "timer interval must be positive");
        Verify.verify(progressStep > 0 && progressStep < 50, "progress step must be between zero and 50");

        final SpinAction action = new SpinAction(progressStep);
        this.timer = new Timer(timerInterval, action);
        this.currentPercentage = new AtomicInteger(0);
        this.timer.start();
    }

    public Timer getTimer() {
        return timer;
    }

    private void resizeProperties(final double newWidth) {
        final double margin = newWidth * MARGIN_MULT;
        this.outerRadius = (newWidth / 2.0d) - margin;
        this.innerRadius = outerRadius * INNER_MULT;
        this.innerOffset = outerRadius - innerRadius + margin;
        this.mainCircle = new Ellipse2D.Double(margin, margin, outerRadius * 2, outerRadius * 2);
    }

    @Override
    public void setPreferredSize(final Dimension preferredSize) {
        resizeProperties(preferredSize.getWidth());
    }

    private void drawMainCircle(final Graphics2D g) {
        g.setColor(Color.LIGHT_GRAY);
        g.setStroke(fatStroke);
        g.draw(mainCircle);
    }

    private void drawInnerCircle(final Graphics g) {
        final int innerDiameter = (int) (innerRadius * 2);
        final double rad = RAD_MULT * currentPercentage.get();
        final double x = outerRadius * Math.cos(rad);
        final double y = outerRadius * Math.sin(rad);
        g.setColor(Color.DARK_GRAY);
        g.fillOval((int) (x + innerOffset), (int) (y + innerOffset), innerDiameter, innerDiameter);
    }

    @Override
    protected void paintComponent(final Graphics g) {
        super.paintComponent(g);
        if (this.mainCircle == null) {
            resizeProperties(getWidth());
        }

        drawMainCircle((Graphics2D) g);
        drawInnerCircle(g);
    }

    private final class SpinAction implements ActionListener {
        private static final int PERCENT_MAX = 100;
        private final int progressStep;

        public SpinAction(final int progressStep) {
            this.progressStep = progressStep;
        }

        @Override
        public void actionPerformed(final ActionEvent e) {
            if (currentPercentage.addAndGet(progressStep) > PERCENT_MAX) {
                currentPercentage.set(0);
            }

            CircularSpinner.this.repaint();
        }
    }


}
