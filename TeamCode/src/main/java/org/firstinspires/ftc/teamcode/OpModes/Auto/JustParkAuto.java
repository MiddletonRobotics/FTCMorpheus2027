package org.firstinspires.ftc.teamcode.OpModes.Auto;

import com.pedropathing.api.Paths;

import com.pedropathing.api.PoseFactory;
import com.pedropathing.math.Pose;
import com.pedropathing.paths.Path;
import com.pedropathing.paths.Path;
import com.pedropathing.paths.interpolator.Interpolator;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
@Autonomous
public class JustParkAuto extends OpMode {

    private final PoseFactory poseFactory = PoseFactory.degrees();

    private final Pose start = poseFactory.of(46.9837, 9.5779, 90);
    private final Pose path1 = poseFactory.of(19.0331, 22.8136, 0);
    private final Pose point2 = poseFactory.of(11.0471, 93.5846, 0);
    private final Pose point2Segment1Start = poseFactory.of(11.0471, 93.5846, 0);
    private final Pose point2Segment1End = poseFactory.of(11.0471, 93.5846, 0);

    public Path path1() {
        return com.pedropathing.api.Paths.line(start, path1).linear(start, path1);
    }

    public Path path2() {
        return com.pedropathing.api.Paths.line(path1, point2).heading(Interpolator.piecewise().until(1, Interpolator.linear(point2Segment1Start, point2Segment1End)));
    }

    @Override
    public void init() {

    }

    @Override
    public void loop() {

    }
}
