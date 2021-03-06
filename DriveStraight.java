package com.qualcomm.ftcrobotcontroller.opmodes.AtomicTheory;

import android.util.Log;

import com.qualcomm.robotcore.exception.RobotCoreException;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.util.Hardware;


/**
* c/o Atomic Theory 4174 (2015-2016)
* Nathaniel Ostrer & Davis Haupt
*/

public abstract class LinearAlpha extends BaseAuto {

  double turnPower = .65;
  double drivePower = 65;

  DcMotor left;
  DcMotor right;
  Servo aim;
  Servo dump;
  Servo rightZip;
  Servo leftZip;
  double aimCount;
  double dumpCount;
  double leftZipCount;
  double rightZipCount;
  DcMotor pull;


  void moveLeft(double power) {
    left.setPower(power);
  }

  void moveRight(double power) {
    right.setPower(power);
  }

  void stopMotors() {
    while (left.getPower() != 0.0 || right.getPower() != 0) {
      left.setPower(0);
      right.setPower(0);
    }
  }

  public void setup() {
    left = encoderMotor1 = hardwareMap.dcMotor.get("left");
    right = encoderMotor2 = hardwareMap.dcMotor.get("right");
    pull = hardwareMap.dcMotor.get("pull");
    aim = hardwareMap.servo.get("aim");
    leftZip = hardwareMap.servo.get("leftZip");
    rightZip = hardwareMap.servo.get("rightZip");
    dump = hardwareMap.servo.get("dump");

    aimCount = 0;
    dumpCount = 1;
    rightZipCount = .3;
    leftZipCount = .7;

    aim.setPosition(aimCount);
    dump.setPosition(dumpCount);
    leftZip.setPosition(leftZipCount);
    rightZip.setPosition(rightZipCount);

    left.setDirection(DcMotor.Direction.FORWARD);
    right.setDirection(DcMotor.Direction.REVERSE);



    /* gyro initialization */
    curHeading = 0; //CHANGE BASED ON PROGRAM
    hasStarted = false;
    prevHeading = curHeading;

    systemTime = System.nanoTime();
    prevTime = systemTime;
    try {
      gyro = new AdafruitIMU(hardwareMap, "bno055"
              , (byte) (AdafruitIMU.BNO055_ADDRESS_A * 2)//By convention the FTC SDK always does 8-bit I2C bus
              , (byte) AdafruitIMU.OPERATION_MODE_IMU);
    } catch (RobotCoreException e) {
      Log.i("FtcRobotController", "Exception: " + e.getMessage());
    }

    systemTime = System.nanoTime();
    gyro.startIMU();//Set up the IMU as needed for a continual stream of I2C reads.
    telemetry.addData("FtcRobotController", "IMU Start method finished in: "
            + (-(systemTime - (systemTime = System.nanoTime()))) + " ns.");
            
    Thread headingThread = new Thread() {
		    public void run() {
		        while(true){
					    updateHeadingThreaded();
				    }
		    }  
		};

		headingThread.start();
		
    //hopefully reduces lag during initial autonomous
  }

  /*gyro specific stuff*/

  static AdafruitIMU gyro;

  //The following arrays contain both the Euler angles reported by the IMU (indices = 0) AND the
  // Tait-Bryan angles calculated from the 4 components of the quaternion vector (indices = 1)
  static volatile double[] rollAngle = new double[2], pitchAngle = new double[2], yawAngle = new double[2], accs = new double[3];

  boolean hasStarted;

  static long systemTime;//Relevant values of System.nanoTime
  static long elapsedTime;
  static long prevTime;

  /* heading is from -180 to 180 degrees */

  static double curHeading;
  static double prevHeading;
  static double desiredHeading;

  public void turnToHeading(double turnPower, double desiredHeading) {
    updateHeading();
    if (curHeading+180 > desiredHeading+180) {
      /* might need a dead zone for turning... */
      //Turn left until robot reaches the desiredHeading
      while (curHeading+180 > desiredHeading+180) {
        updateHeading();
        moveLeft(turnPower);
        moveRight(-turnPower);
      }
      stopMotors();
    } else {
      //Turn right until robot reaches the desiredHeading
      while (curHeading+180 < desiredHeading+180) {
        updateHeading();
        moveLeft(turnPower);
        moveRight(-turnPower);
      }
      stopMotors();
    }
    updateHeading();
  }
  public void rotateDegs(double turnPower, double degs) {
    updateHeading();
    double h = curHeading + degs * -sign(turnPower);
    if (Math.abs(h) > 180)
      h += -360*sign(h);
    turnToHeading(Math.abs(turnPower), h);
  }

  /**
   * Calculate one step of the proportional control for driving straight
   * With the Adafruit IMU.
   *
   * driveTicksStraight allows us to drive straight using encoder ticks to determine when to stop,
   * but we might want to stop driving when we reach other thresholds, such as a distance away
   * from the wall measured by an Optical Distance Sensor.
   *
   * This function allows us to do that. Use it inside of a loop where the breakout condition is
   * when you want to stop driving. Pass in the heading you want to drive to and the power
   * you want to drive at, and right after the loop call stopMotors();
   *
   * @param power
   * @param initHeading the heading to drive to
   */
  public void driveStraight(double power, double initHeading) {
    updateHeading();

    double error = curHeading - initHeading;
    double error_const = .04; // .04 is an empirically decided value
    double pl = scale(power - error*error_const);
    double pr = scale(power + error*error_const);

    moveLeft(pl);
    moveRight(pr);
  }

  /**
   * Drive forward until encoder tick threshold is met.
   * @param power
   * @param ticks Number of encoder ticks to travel
   */
  public void driveTicksStraight(double power, int ticks) {
    int start = encoderMotor1.getCurrentPosition();
    int start2 = encoderMotor2.getCurrentPosition();

    updateHeading();
    double initHeading = curHeading;
    double error_const = .04;


    while (Math.abs(encoderMotor1.getCurrentPosition() - start) < ticks
            || Math.abs(encoderMotor2.getCurrentPosition() - start2) < ticks) {
      updateHeading();

      //gyro is too finicky to do integral stuff so just the basic derivative stuff
      double pl = power;
      double pr = power;


      double error = curHeading - initHeading;

      pl-=error * error_const;
      pr+=error * error_const;

      pl = scale(pl);
      pr = scale(pr);

      moveLeft(pl);
      moveRight(pr);
      telemetry.addData("m1:", Math.abs(encoderMotor1.getCurrentPosition() - start) - ticks);
      telemetry.addData("m2:", Math.abs(encoderMotor2.getCurrentPosition() - start2) - ticks);
    }

    stopMotors();
  }

  public void updateHeading() {

  }
  
  public static void updateHeadingThreaded() {
    elapsedTime = systemTime - prevTime;
    prevTime = systemTime;
    double elapsedSeconds = elapsedTime / 1000000000;
    systemTime = System.nanoTime();

    //Update gyro values
    gyro.getIMUGyroAngles(rollAngle, pitchAngle, yawAngle);
    prevHeading = curHeading;
    curHeading = yawAngle[0];

    //Display information on screen
    //telemetry.addData("Headings(yaw): ",
    //        String.format("Euler= %4.5f", yawAngle[0]));

  }
}