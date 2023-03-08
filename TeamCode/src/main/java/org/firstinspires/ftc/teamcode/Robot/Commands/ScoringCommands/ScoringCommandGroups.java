package org.firstinspires.ftc.teamcode.Robot.Commands.ScoringCommands;

import com.acmerobotics.roadrunner.geometry.Pose2d;

import org.firstinspires.ftc.teamcode.CommandFramework.Command;
import org.firstinspires.ftc.teamcode.Math.Kinematics.IntakeKinematics;
import org.firstinspires.ftc.teamcode.Robot.Commands.MiscCommands.Delay;
import org.firstinspires.ftc.teamcode.Robot.Commands.MiscCommands.MultipleCommand;
import org.firstinspires.ftc.teamcode.Robot.Commands.MiscCommands.NullCommand;
import org.firstinspires.ftc.teamcode.Robot.Commands.MiscCommands.RunCommand;
import org.firstinspires.ftc.teamcode.Robot.Commands.ScoringCommands.primitiveMovements.AsyncMoveVerticalExtension;
import org.firstinspires.ftc.teamcode.Robot.Commands.ScoringCommands.primitiveMovements.CancelableMoveArmDirect;
import org.firstinspires.ftc.teamcode.Robot.Commands.ScoringCommands.primitiveMovements.CloseLatch;
import org.firstinspires.ftc.teamcode.Robot.Commands.ScoringCommands.primitiveMovements.MoveArm;
import org.firstinspires.ftc.teamcode.Robot.Commands.ScoringCommands.primitiveMovements.MoveArmDirect;
import org.firstinspires.ftc.teamcode.Robot.Commands.ScoringCommands.primitiveMovements.MoveClaw;
import org.firstinspires.ftc.teamcode.Robot.Commands.ScoringCommands.primitiveMovements.MoveHorizontalExtension;
import org.firstinspires.ftc.teamcode.Robot.Commands.ScoringCommands.primitiveMovements.SetHorizontalExtensionInches;
import org.firstinspires.ftc.teamcode.Robot.Commands.ScoringCommands.primitiveMovements.MoveTurret;
import org.firstinspires.ftc.teamcode.Robot.Commands.ScoringCommands.primitiveMovements.MoveTurretAsync;
import org.firstinspires.ftc.teamcode.Robot.Commands.ScoringCommands.primitiveMovements.MoveTurretDirect;
import org.firstinspires.ftc.teamcode.Robot.Commands.ScoringCommands.primitiveMovements.MoveVerticalExtension;
import org.firstinspires.ftc.teamcode.Robot.Commands.ScoringCommands.primitiveMovements.OpenLatch;
import org.firstinspires.ftc.teamcode.Robot.Commands.VisionCommands.MeasureConestack;
import org.firstinspires.ftc.teamcode.Robot.Commands.VisionCommands.VisualIntake;
import org.firstinspires.ftc.teamcode.Robot.Commands.VisionCommands.VisualIntakeStage1;
import org.firstinspires.ftc.teamcode.Robot.Commands.VisionCommands.VisualIntakeStage2;
import org.firstinspires.ftc.teamcode.Robot.Subsystems.Dashboard;
import org.firstinspires.ftc.teamcode.Robot.Subsystems.Drivetrain;
import org.firstinspires.ftc.teamcode.Robot.Subsystems.ScoringMechanism.HorizontalExtension;
import org.firstinspires.ftc.teamcode.Robot.Subsystems.ScoringMechanism.MainScoringMechanism;
import org.firstinspires.ftc.teamcode.Robot.Subsystems.ScoringMechanism.Turret;
import org.firstinspires.ftc.teamcode.Robot.Subsystems.ScoringMechanism.VerticalExtension;
import org.firstinspires.ftc.teamcode.Robot.Subsystems.Vision.BackCamera;
import org.firstinspires.ftc.teamcode.Utils.Side;
import org.firstinspires.ftc.teamcode.VisionUtils.Cone;

public class ScoringCommandGroups {
	public int currentCone = 5;
	BackCamera backCamera;
	Turret turret;
	VerticalExtension verticalExtension;
	HorizontalExtension horizontalExtension;
	Drivetrain drivetrain;
	Pose2d intakePosition = new Pose2d();
	double[] armConeHeights = {0.08, 0.11, 0.1357, 0.1632, 0.1800};

	public ScoringCommandGroups(MainScoringMechanism mechanism, Drivetrain drivetrain, BackCamera backCamera) {
		this.horizontalExtension = mechanism.horizontalExtension;
		this.verticalExtension = mechanism.verticalExtension;
		this.turret = mechanism.turret;
		this.drivetrain = drivetrain;
		this.backCamera = backCamera;
	}
	public Command teleAuto() {
		Command command = new NullCommand();
		for (int i = 0; i < 5; ++i) {
			addCycle(command);
		}
		return command;
	}

	private void addCycle(Command command) {
		command.addNext(new MeasureConestack(turret, backCamera,horizontalExtension))
				.addNext(new VisualIntakeStage1(turret, backCamera,horizontalExtension))
				.addNext(cancelableSetArmHeightVision())
				.addNext(new VisualIntakeStage2(turret, backCamera,horizontalExtension))
				.addNext(new Delay(0.1))
				.addNext(grabCone())
				.addNext(new Delay(0.1))
				.addNext(moveArm(Turret.ArmStates.TRANSFER_SAFE))
				.addNext(moveTurret(Turret.TurretStates.TRANSFER))
				.addNext(moveHorizontalExtension(HorizontalExtension.IN_POSITION))
				.addNext(moveArm(Turret.ArmStates.TRANSFER))
				.addNext(new Delay(0.1))
				.addNext(releaseCone())
				.addNext(closeLatch())
				.addNext(new Delay(0.1))
				.addNext(moveArm(Turret.ArmStates.TRANSFER_SAFE))
				.addNext(moveVerticalExtension(VerticalExtension.HIGH_POSITION + .06))
				.addNext(depositConeAsync())
				.addNext(openLatch())
				.addNext(new Delay(0.2));
	}
	public Command autoGoToCone() {
		return new RunCommand(() -> {
			Cone cone = backCamera.getCone();
			if (cone != null) {

				double angle = IntakeKinematics.getTurretAngleToTarget(-1 * cone.position.dx);
				double extendDistance = IntakeKinematics.getHorizontalSlideExtensionToTarget(cone.position.dy, -1 * cone.position.dx, horizontalExtension.getSlidePositionInches());
				Dashboard.packet.put("THE dx", cone.position.dx);
				Dashboard.packet.put("THE dy", cone.position.dy);
				Dashboard.packet.put("THE angle", Math.toDegrees(angle));
				Dashboard.packet.put("THE extendDist", extendDistance);
				if (extendDistance <= 15) {
					if (angle < 0) {
						angle += Math.PI * 2;
					}
					return openClaw().addNext(new MoveTurretDirect(turret, angle).addNext(new SetHorizontalExtensionInches(horizontalExtension, extendDistance)).addNext(moveArm(Turret.ArmStates.DOWN)));
				} else return new NullCommand();

			} else return new NullCommand();
		});
	}

	public Command autoIntakeCmd() {
		return new RunCommand(() -> {
			Cone cone = backCamera.getCone();
			if (cone != null) {

				double angle = IntakeKinematics.getTurretAngleToTarget(-1 * cone.position.dx);
				double extendDistance = IntakeKinematics.getHorizontalSlideExtensionToTarget(cone.position.dy, -1 * cone.position.dx, horizontalExtension.getSlidePositionInches());
				if (extendDistance <= 15) {
					if (angle < 0) {
						angle += Math.PI * 2;
					}
					return openClaw()
							.addNext(new MoveTurretDirect(turret, angle))
							.addNext(new SetHorizontalExtensionInches(horizontalExtension, extendDistance))
							.addNext(moveArm(Turret.ArmStates.DOWN))
							.addNext(new Delay(.08))
							.addNext(grabCone())
							.addNext(openLatch())
							.addNext(collectCone())
							.addNext(closeLatch());
				} else return new NullCommand();

			} else return new NullCommand();
		});
	}

	public Command autoIntake() {
		return openClaw()
				.addNext(new VisualIntake(turret, backCamera,horizontalExtension,0))
				.addNext(moveArm(Turret.ArmStates.DOWN))
				.addNext(new Delay(.15))
				.addNext(grabCone())
				.addNext(openLatch())
				.addNext(new Delay(.15))
				.addNext(collectCone())
				.addNext(closeLatch());
	}

	public Command autoGoToConeTesting() {
		Cone cone = backCamera.getCone();
		if (cone != null) {

			double angle = IntakeKinematics.getTurretAngleToTarget(-1 * cone.position.dx);
			double extendDistance = IntakeKinematics.getHorizontalSlideExtensionToTarget(cone.position.dy, -1 * cone.position.dx, horizontalExtension.getSlidePositionInches());
			Dashboard.packet.put("THE dx", cone.position.dx);
			Dashboard.packet.put("THE dy", cone.position.dy);
			Dashboard.packet.put("THE angle", Math.toDegrees(angle));
			Dashboard.packet.put("THE extendDist", extendDistance);
			if (extendDistance <= 17) {
				if (angle < 0) {
					angle += Math.PI * 2;
				}
				return openClaw().addNext(new MoveTurretDirect(turret, angle).addNext(new SetHorizontalExtensionInches(horizontalExtension, extendDistance)).addNext(moveArm(Turret.ArmStates.DOWN)));
			} else return new NullCommand();

		} else return new NullCommand();
	}



	// near straight but tilted to the left
	public Command moveToIntakingLeft() {
		return moveHorizontalExtension(HorizontalExtension.CLOSE_INTAKE)
				.addNext(moveArm(Turret.ArmStates.TRANSFER_SAFE))
				.addNext(moveTurret(Turret.TurretStates.Slight_LEFT))
				.addNext(new MultipleCommand(moveArm(Turret.ArmStates.DOWN), openClaw()));
	}

	public Command moveToIntakingLeftWithDeposit() {
		Pose2d drivetrainPos = drivetrain.getPose();
		if (Math.hypot(intakePosition.getX() - drivetrainPos.getX(), intakePosition.getY() - drivetrainPos.getY()) > 15) {
			// if this is the case, dont put out the extension, just put out the vertical extension
			return moveVerticalExtension(VerticalExtension.HIGH_POSITION);
		}
		return new MultipleCommand(moveHorizontalExtension(HorizontalExtension.CLOSE_INTAKE), moveVerticalExtension(VerticalExtension.HIGH_POSITION))
				.addNext(moveArm(Turret.ArmStates.TRANSFER_SAFE))
				.addNext(moveTurret(Turret.TurretStates.Slight_LEFT))
				.addNext(new MultipleCommand(moveArm(Turret.ArmStates.DOWN), openClaw()));
	}

	public Command moveToIntakingRight() {
		return moveHorizontalExtension(HorizontalExtension.EXTENSION1)
				.addNext(moveArm(Turret.ArmStates.TRANSFER_SAFE))
				.addNext(moveTurret(Turret.TurretStates.Slight_RIGHT_AUTO))
				.addNext(new MultipleCommand(moveArm(Turret.ArmStates.DOWN), openClaw()));
	}

	public Command setArmHeightVision() {
		currentCone--;
		return new MultipleCommand(moveArmDirect(-0.02 + armConeHeights[currentCone]), openClaw());
	}
	public Command cancelableSetArmHeightVision() {
		if (!CancelableMoveArmDirect.cancelled) {currentCone--;
			return new MultipleCommand(new CancelableMoveArmDirect(turret,-0.02 + armConeHeights[currentCone]), openClaw());
		}
		else {
			return new NullCommand();
		}

	}

	public Command moveToIntakingRightAuto() {
		currentCone--;

		return moveArm(Turret.ArmStates.TRANSFER_SAFE)
				.addNext(moveTurret(Turret.TurretStates.Slight_RIGHT_AUTO))
				.addNext(new MultipleCommand(moveArmDirect(-0.02 + armConeHeights[currentCone]), openClaw()));
	}

	public Command moveToIntakingLeftAuto() {
		currentCone--;

		return moveArm(Turret.ArmStates.TRANSFER_SAFE)
				.addNext(moveTurret(Turret.TurretStates.Slight_LEFT_AUTO))
				.addNext(new MultipleCommand(moveArmDirect(-0.02 + armConeHeights[currentCone]), openClaw()));
	}

	public Command moveToIntakingLeftSideMidAuto() {
		currentCone--;

		return moveArm(Turret.ArmStates.TRANSFER_SAFE)
				.addNext(moveTurret(Turret.TurretStates.Slight_RIGHT_AUTO))
				.addNext(new MultipleCommand(moveArmDirect(-0.02 + armConeHeights[currentCone]), openClaw()));
	}

	// very far to the left, in order to place near by
	public Command moveToIntakingLeftClosePole() {
		return moveArm(Turret.ArmStates.TRANSFER_SAFE)
				.addNext(moveHorizontalExtension(HorizontalExtension.PRE_EMPTIVE_EXTEND))
				.addNext(moveTurret(Turret.TurretStates.Slight_LEFT))//new MultipleCommand(moveArm(Turret.ArmStates.TRANSFER_SAFE),
				//moveHorizontalExtension(HorizontalExtension.PRE_EMPTIVE_EXTEND),
				//moveTurret(Turret.TurretStates.FAR_LEFT))
				.addNext(new MultipleCommand(moveArm(Turret.ArmStates.DOWN), openClaw()))
				.addNext(moveHorizontalExtension(HorizontalExtension.TELE_CYCLE_EXTENSION));
	}

	public Command moveToIntakingRightClosePole() {
		Command command = new MultipleCommand(moveArm(Turret.ArmStates.TRANSFER_SAFE),
				moveHorizontalExtension(HorizontalExtension.PRE_EMPTIVE_EXTEND),
				moveTurret(Turret.TurretStates.Slight_RIGHT))
				.addNext(new Delay(0.1))
				.addNext(new MultipleCommand(moveArm(Turret.ArmStates.DOWN), openClaw()))
				.addNext(moveHorizontalExtension(HorizontalExtension.TELE_CYCLE_EXTENSION));
		return new RunCommand(() -> command);
	}

	public Command syncedScoringCollecting() {
		Command depositCommand = moveVerticalExtension(VerticalExtension.HIGH_POSITION);
		Command intakeCommand = moveToIntakingLeft();
		return intakeCommand.addNext(depositCommand);
	}


	public Command collectCone() {

		intakePosition = drivetrain.getPose();

		if (verticalExtension.getSlidePosition() > 50) {
			return new NullCommand();
		}

		return grabCone() // 0.25s
				.addNext(openLatch()) // 0s
				.addNext(moveArm(Turret.ArmStates.TRANSFER_SAFE)) // 0.25s
				.addNext(moveTurret(Turret.TurretStates.TRANSFER)) // 0.2s
				.addNext(moveHorizontalExtension(HorizontalExtension.IN_POSITION)) // some amount of time, assuming about 0.2
				.addNext(moveArm(Turret.ArmStates.TRANSFER)) // 0.25s
				.addNext(new Delay(0.15)) // 0.15s
				.addNext(closeLatch()) // 0.0s
				.addNext(releaseCone())  // 0.25s
				.addNext(moveArm(Turret.ArmStates.TRANSFER_SAFE)); // 0.25
	}

	public Command collectConeAuto(double autoExtensionDistance) {

		return depositConeAsync()
				.addNext(openLatch())
				.addNext(moveHorizontalExtension(autoExtensionDistance))
				.addNext(new Delay(0.25))
				.addNext(grabCone())
				.addNext(moveArm(Turret.ArmStates.TRANSFER_SAFE))
				.addNext(moveTurret(Turret.TurretStates.TRANSFER))
				.addNext(moveHorizontalExtension(HorizontalExtension.IN_POSITION))
				.addNext(moveArm(Turret.ArmStates.TRANSFER))
				.addNext(new Delay(0.15))
				.addNext(releaseCone())
				.addNext(closeLatch())
				.addNext(new Delay(0.1))
				.addNext(moveArm(Turret.ArmStates.TRANSFER_SAFE));

	}
	public Command visuallyCollectConeAuto(Side side, boolean last) {
		double add;
		if (last) add = 1.5;
		else add = 0;
		Turret.TurretStates state;
		if (side == Side.LEFT) {
			state = Turret.TurretStates.Slight_LEFT;
		}
		else {
			state = Turret.TurretStates.Slight_RIGHT;
		}
		return depositConeAsync()
				.addNext(moveTurret(state))
				.addNext(setArmHeightVision())
				.addNext(new VisualIntake(turret,backCamera,horizontalExtension,add))
				.addNext(new Delay(0.1))
				.addNext(grabCone())
				.addNext(moveArm(Turret.ArmStates.TRANSFER_SAFE))
				.addNext(new Delay(0.1))
				.addNext(moveTurret(Turret.TurretStates.TRANSFER))
				.addNext(moveHorizontalExtension(HorizontalExtension.IN_POSITION))
				.addNext(moveArm(Turret.ArmStates.TRANSFER))
				.addNext(new Delay(0.1))
				.addNext(releaseCone())
				.addNext(closeLatch())
				.addNext(moveArm(Turret.ArmStates.TRANSFER_SAFE));
	}

	public Command grabCone() {
		return moveClaw(Turret.ClawStates.Closed);
	}

	public Command releaseCone() {
		return moveClaw(Turret.ClawStates.Transfer);
	}

	public Command openClaw() {
		return moveClaw(Turret.ClawStates.Open);
	}

	public Command moveToLowGoalScoring() {
		return moveClaw(Turret.ClawStates.Closed)
				.addNext(moveArm(Turret.ArmStates.LOW_SCORING))
				.addNext(moveHorizontalExtension(HorizontalExtension.IN_POSITION));
	}

	// Move vertical extension down. If it is already going down, open the claw
	public Command moveVerticalExtensionDownOrReleaseClaw() {
		if (verticalExtension.getSlideTargetPosition() == VerticalExtension.IN_POSITION) {
			return moveClaw(Turret.ClawStates.Open);
		} else {
			return depositCone();
		}
	}


	public Command asyncMoveVerticalExtension(double position) {
		return new AsyncMoveVerticalExtension(verticalExtension, drivetrain, position);
	}

	public MoveArm moveArm(Turret.ArmStates armStates) {
		return new MoveArm(turret, armStates);
	}

	public MoveArmDirect moveArmDirect(double position) {
		return new MoveArmDirect(turret, position);
	}

	public MoveClaw moveClaw(Turret.ClawStates clawStates) {
		return new MoveClaw(turret, clawStates);
	}

	public MoveTurret moveTurret(Turret.TurretStates turretStates) {
		return new MoveTurret(turret, turretStates);
	}

	public MoveTurret moveTurretAsync(Turret.TurretStates turretStates) {
		return new MoveTurretAsync(turret, turretStates);
	}

	public MoveHorizontalExtension moveHorizontalExtension(double position) {
		return new MoveHorizontalExtension(horizontalExtension, position);
	}

	public MoveVerticalExtension moveVerticalExtension(double position) {
		return new MoveVerticalExtension(verticalExtension, position, drivetrain);
	}

	public Command depositCone() {
		return openLatch().addNext(moveVerticalExtension(VerticalExtension.IN_POSITION));
	}

	public Command depositConeAsync() {
		return openLatch().addNext(asyncMoveVerticalExtension(VerticalExtension.IN_POSITION));
	}

	public CloseLatch closeLatch() {
		return new CloseLatch(turret);
	}

	public OpenLatch openLatch() {
		return new OpenLatch(turret);
	}

}
