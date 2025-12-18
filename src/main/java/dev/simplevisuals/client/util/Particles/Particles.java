package dev.simplevisuals.client.util.Particles;

import net.minecraft.util.math.Vec3d;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class Particles {

    private final List<Particle> particles = new ArrayList<>();
    private final Random random = new Random();

    public void clear() {
        particles.clear();
    }

    public boolean isEmpty() {
        return particles.isEmpty();
    }

    public void add(Particle particle) {
        particles.add(particle);
    }

    public Iterator<Particle> iterator() {
        return particles.iterator();
    }

    public void startScattering() {
        for (Particle p : particles) {
            p.startScattering();
        }
    }

    public static class Particle {
        private Vec3d pos;
        private Vec3d targetPos;
        private double angle, angleOffset, offsetY;
        private final float width, height;
        private int age = 0;
        private final int maxAge;
        private boolean scattering = false;
        private double vx, vy, vz;
        private final List<Vec3d> trailPositions = new ArrayList<>();
        private static final int MAX_TRAIL_LENGTH = 3;

        public Particle(Vec3d pos, Vec3d targetPos, float width, float height, boolean converging, int maxAge) {
            this.pos = pos;
            this.targetPos = targetPos;
            this.width = width;
            this.height = height;
            this.scattering = !converging;
            this.angle = 0;
            this.angleOffset = 0;
            this.offsetY = 0;
            this.maxAge = maxAge;
        }

        public Particle(Vec3d origin, double angle, double angleOffset, double offsetY, float width, float height, int maxAge) {
            this.pos = origin;
            this.angle = angle;
            this.angleOffset = angleOffset;
            this.offsetY = offsetY;
            this.width = width;
            this.height = height;
            this.maxAge = maxAge;
        }

        public void startScattering() {
            this.scattering = true;
            Random random = new Random();
            vx = (random.nextDouble() - 0.5) * 0.19;
            vy = (random.nextDouble() - 0.5) * 0.19;
            vz = (random.nextDouble() - 0.5) * 0.19;
        }

        public void update(Vec3d origin, double time, double omega, float animVal, boolean isSoulMode, double ghostRadius) {
            // Save current position for trail
            if (!scattering && trailPositions.size() < MAX_TRAIL_LENGTH) {
                trailPositions.add(pos);
            } else if (trailPositions.size() >= MAX_TRAIL_LENGTH) {
                trailPositions.remove(0);
                trailPositions.add(pos);
            }

            if (scattering) {
                pos = pos.add(vx, vy, vz);
                vy += -0.002;
                vx *= 0.98;
                vy *= 0.98;
                vz *= 0.98;
                age++;
            } else if (isSoulMode) {
                // For Soul: particles converge to orbit
                Vec3d direction = targetPos.subtract(pos).normalize().multiply(0.2);
                pos = pos.add(direction);
                if (pos.distanceTo(targetPos) < 0.05) {
                    age = maxAge;
                }
            } else {
                // For Ghosts: particles form two circles
                double currentRadius = ghostRadius * (2.0 - animVal);
                double offsetX = Math.cos(angle + angleOffset) * currentRadius;
                double offsetZ = Math.sin(angle + angleOffset) * currentRadius;
                double currentOffsetY = Math.sin((time * omega * 0.5) + angleOffset) * 0.5 * height + 0.5 * height;
                pos = origin.add(offsetX, -height * 0.5 + currentOffsetY, offsetZ);
                angle += omega / 60.0;
            }
        }

        public float getLifeProgress() {
            return scattering ? 1.0f - ((float) age / maxAge) : 1.0f;
        }

        public Vec3d getPos() {
            return pos;
        }

        public double getAngle() {
            return angle;
        }

        public int getAge() {
            return age;
        }

        public int getMaxAge() {
            return maxAge;
        }

        public boolean isScattering() {
            return scattering;
        }

        public List<Vec3d> getTrailPositions() {
            return trailPositions;
        }
    }
}