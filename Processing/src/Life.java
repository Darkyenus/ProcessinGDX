import processing.core.PApplet;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

/**
 *
 */
public class Life extends PApplet {

    private final Random RNG = new Random();

    public void settings() {
        size(1000, 800);
    }

    public void setup() {
        background(20);
        colorMode(HSB, 1f, 1f, 1f, 1f);

        final Creature first = new Creature();
        first.x = width/2f;
        first.y = height/2f;
        first.velX = 0f;
        first.velY = 0f;
        first.speed = 2.2f;
        timeToNextCreature = 25f;
        creatures.add(first);
    }

    private final ArrayList<Creature> creatures = new ArrayList<>();
    private float timeToNextCreature = -10f;

    private long lastDrawNano = System.nanoTime();

    private static int clampPix(int value) {
        if (value <= 0) {
            return 0;
        } else if (value >= 0xFF) {
            return 0xFF;
        } else {
            return value;
        }
    }

    private void dim(int rOff, int gOff, int bOff) {
        //noStroke();
        //fill(0, 0, 0.1f, 0.1f);
        //rect(0,0,width,height);

        loadPixels();
        for (int i = 0; i < pixels.length; i++) {
            final int pixel = pixels[i];
            int r = pixel & 0xFF;
            int g = (pixel >> 8) & 0xFF;
            int b = (pixel >> 16) & 0xFF;

            r = clampPix(r + rOff);
            g = clampPix(g + gOff);
            b = clampPix(b + bOff);

            pixels[i] = (pixel & 0xFF000000) | ((b & 0xFF) << 16) | ((g & 0xFF) << 8) | (r & 0xFF);
        }
        updatePixels();
    }

    public void draw() {
        dim(-4, -4, -6);

        final long nowNano = System.nanoTime();
        final float delta = (nowNano - lastDrawNano) / 1000_000_000f;
        lastDrawNano = nowNano;

        timeToNextCreature -= delta;
        while (timeToNextCreature < 0) {
            if (creatures.size() < 100) {
                creatures.add(new Creature());
            }
            timeToNextCreature += random(0.8f, 3.5f);
        }

        for (Iterator<Creature> iterator = creatures.iterator(); iterator.hasNext(); ) {
            Creature c = iterator.next();
            c.age(delta);
            c.draw();

            if (c.dead) {
                iterator.remove();
            }
        }
    }

    class Creature {
        final float sideOffset = 70;
        float x = random(sideOffset, width-sideOffset), y = random(sideOffset, height-sideOffset);
        float velX = random(-1, 1), velY = random(-1, 1);
        float hue = random(0, 1f);

        float[] points = new float[32 + RNG.nextInt(64)];

        float life = 0;
        float speed = random(0.85f, 4.15f);
        float lifeLength = random(20, 60);
        boolean dead = false;

        void age(float delta) {
            delta *= speed;

            x += velX*delta;
            y += velY*delta;

            life += delta;
            if (life < lifeLength) {
                for (int i=0; i < points.length; i++) {
                    points[i] += delta * 2.3 * (1 + noise(x*0.008f+i,y*0.008f+i,i*0.08f)*0.1-0.05);
                }
            } else {
                for (int i=0; i < points.length; i++) {
                    points[i] -= delta * noise(x, y, i) * 3;
                    if (points[i] < -5) {
                        dead = true;
                    }
                }
            }
        }

        void draw() {
            float lines;
            if (life < lifeLength) {
                stroke(hue, life/lifeLength, 1.0f);
                lines = life * 2;
            } else {
                float d = lifeLength / life;
                stroke(hue, d * d, d);
                float agingFactor = map(life, lifeLength, lifeLength + 5f/speed, 2, 1);
                if (agingFactor < 0.5f) {
                    agingFactor = 0.5f;
                }

                lines = d * lifeLength * agingFactor;
            }

            for (int i = 0; i < lines; i++) {
                final int fromI = RNG.nextInt(points.length);
                final int toI = (points.length + fromI + RNG.nextInt(30) - 15) % points.length;//RNG.nextInt(pointCount);

                float fromAngle = PI*2 * (float) fromI /points.length;
                float x1 = cos(fromAngle) * points[fromI] + x;
                float y1 = sin(fromAngle) * points[fromI] + y;

                float toAngle = PI*2 * (float) toI /points.length;
                float x2 = cos(toAngle) * points[toI] + x;
                float y2 = sin(toAngle) * points[toI] + y;

                line(x1, y1, x2, y2);
            }
        }
    }

    public static void main(String[] args){
        PApplet.main(Life.class);
    }
}
