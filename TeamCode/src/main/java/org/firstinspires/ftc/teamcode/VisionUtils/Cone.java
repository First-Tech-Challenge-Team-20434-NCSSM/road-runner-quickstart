package org.firstinspires.ftc.teamcode.VisionUtils;

import com.acmerobotics.dashboard.config.Config;

import org.firstinspires.ftc.teamcode.Math.Geometry.Pose2d;
import org.firstinspires.ftc.teamcode.visionPipelines.ConeDetectionFast;
import org.opencv.core.Point;
import org.opencv.core.Size;

public class Cone {
    public enum Classification {
        GOOD,
        FAR,
        CLOSE
    }

    public Size size;
    public CameraBasedPosition position;
    public Point top;
    public Classification classification;
    public boolean deadzoned;

    public double hd;
    public double vd;

    @Config
    public static class Ranking {
        private static double DX_WEIGHT = 2;
        private static double MAX_DX = 15;
        private static double MAX_DY = 35;
    }

    public double score = 0;

    public Cone(Size size, CameraBasedPosition position, Point top, double hd, double vd){
        this.size = size;
        this.position = position;
        this.top = top;
        this.classify();
        this.hd = hd;
        this.vd = vd;
    }
    private void classify() {
        this.deadzoned = false;//!(this.position.angle <= Turret.MAX_SERVO_RADIANS) || !(this.position.angle >= Turret.MIN_SERVO_RADIANS);
        if (this.position.distance < ConeDetectionFast.ConeConfig.perfectDistance - ConeDetectionFast.ConeConfig.perfectTolerance) {
            this.classification = Classification.CLOSE;
        }
        else if (this.position.distance > 40) {
            this.classification = Classification.FAR;
        }
        else {
            this.classification = Classification.GOOD;
        }
        double dxScore = (Ranking.MAX_DX - Math.abs(this.position.dx)) / Ranking.MAX_DX;
        double dyScore = (Ranking.MAX_DY - Math.abs(this.position.dy)) / Ranking.MAX_DY;
        this.score = (Ranking.DX_WEIGHT * dxScore + dyScore) / 2;
    }
}
