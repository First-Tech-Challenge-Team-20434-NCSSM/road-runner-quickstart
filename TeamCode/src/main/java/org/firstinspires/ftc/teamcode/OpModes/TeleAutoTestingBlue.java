package org.firstinspires.ftc.teamcode.OpModes;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;

import org.firstinspires.ftc.teamcode.Utils.Team;

@Autonomous
public class TeleAutoTestingBlue extends TeleAutoTesting {
    @Override
    public Team getTeam() {
        return Team.BLUE;
    }
}
