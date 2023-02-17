package org.firstinspires.ftc.teamcode.Robot.Commands.VisionCommands;

import org.firstinspires.ftc.teamcode.CommandFramework.Command;
import org.firstinspires.ftc.teamcode.Math.Kinematics.IntakeKinematics;
import org.firstinspires.ftc.teamcode.Robot.Subsystems.ScoringMechanism.HorizontalExtension;
import org.firstinspires.ftc.teamcode.Robot.Subsystems.ScoringMechanism.Turret;
import org.firstinspires.ftc.teamcode.Robot.Subsystems.Vision.BackCamera;
import org.firstinspires.ftc.teamcode.VisionUtils.Cone;

public class VisualIntake extends Command {
    private final Turret turret;
    private final BackCamera backCamera;
    private final HorizontalExtension horizontalExtension;
    private Cone target = null;
    private double angle;
    private double extendDistance;
    private boolean failed;
    private boolean finished = false;
    private double add;


    public VisualIntake(Turret turret, BackCamera backCamera, HorizontalExtension horizontalExtension,double add) {
        super(turret, backCamera);
        this.turret = turret;
        this.backCamera = backCamera;
        this.horizontalExtension = horizontalExtension;
        this.add = add;
    }

    @Override
    public void init() {
        this.target = this.backCamera.getCone();
        if (this.target != null) {
            this.failed = false;
            this.angle = IntakeKinematics.getTurretAngleToTarget(-1 * this.target.position.dx);
            this.extendDistance = IntakeKinematics.getHorizontalSlideExtensionToTarget(this.target.position.dy, -1 * this.target.position.dx, horizontalExtension.getSlidePositionInches());
            if (extendDistance <= 16) {
                if (this.angle < 0) {
                    this.angle += Math.PI * 2;
                }
                this.backCamera.accumulatedConestackAngle = (this.backCamera.accumulatedConestackAngle + this.angle) / 2;
                this.backCamera.accumulatedConestackDistance = (this.extendDistance+this.add + this.extendDistance) / 2;
                this.horizontalExtension.setTargetPositionInches(this.backCamera.accumulatedConestackDistance);
                this.turret.setBasedTurretPosition(this.backCamera.accumulatedConestackAngle);
            }
        } else {
            this.failed = false;
            this.finished = true;
        }
    }

    @Override
    public void periodic() {
        if (horizontalExtension.isMovementFinished()) {
            this.finished = true;
        }
    }

    @Override
    public boolean completed() {
        return this.finished;
    }

    @Override
    public void shutdown() {
        turret.shutdown();
    }


}