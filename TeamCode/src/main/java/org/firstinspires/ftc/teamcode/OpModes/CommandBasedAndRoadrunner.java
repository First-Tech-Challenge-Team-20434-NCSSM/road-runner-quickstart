package org.firstinspires.ftc.teamcode.OpModes;

import com.acmerobotics.roadrunner.geometry.Pose2d;
import com.acmerobotics.roadrunner.geometry.Vector2d;
import com.acmerobotics.roadrunner.trajectory.Trajectory;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;

import org.firstinspires.ftc.teamcode.CommandFramework.BaseAuto;
import org.firstinspires.ftc.teamcode.CommandFramework.Command;
import org.firstinspires.ftc.teamcode.CommandFramework.CommandScheduler;

@Autonomous
public class CommandBasedAndRoadrunner extends BaseAuto {
	@Override
	public Command setupAuto(CommandScheduler scheduler) {
		Trajectory traj = robot.drivetrain.getBuilder().trajectoryBuilder(new Pose2d())
				.splineTo(new Vector2d(30,0),Math.toRadians(0))
				.splineTo(new Vector2d(30,30),Math.toRadians(90))
				.splineTo(new Vector2d(0,30),Math.toRadians(180))
				.splineTo(new Vector2d(0,0),0)
				.build();

		return followRR(traj);

	}


}