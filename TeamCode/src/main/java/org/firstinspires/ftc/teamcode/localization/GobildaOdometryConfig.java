package org.firstinspires.ftc.teamcode.localization;

import com.qualcomm.hardware.gobilda.GoBildaPinpointDriver;

/**
 * Configuração física do goBILDA Pinpoint. É a ÚNICA fonte desses valores.
 * Fica junto da implementação porque usa tipos do driver do Pinpoint.
 */
public class GobildaOdometryConfig {

    // Nome do dispositivo no Driver Hub.
    // "odmetry" era o nome que o Drivetrain já buscava em toda execução do
    // robô; a GobildaOdometry antiga usava "odometry", mas nunca chegou a ser
    // executada. CONFIRME no Driver Hub qual nome está configurado.
    public static final String PINPOINT = "odmetry";

    // Offsets dos pods em relação ao centro de rotação, em cm
    // (valores originais da GobildaOdometry; não validados fisicamente).
    public static final double X_POD_OFFSET_CM = -18.0;
    public static final double Y_POD_OFFSET_CM = -10.0;

    public static final GoBildaPinpointDriver.GoBildaOdometryPods POD_TYPE =
            GoBildaPinpointDriver.GoBildaOdometryPods.goBILDA_4_BAR_POD;

    public static final GoBildaPinpointDriver.EncoderDirection X_ENCODER_DIRECTION =
            GoBildaPinpointDriver.EncoderDirection.FORWARD;
    public static final GoBildaPinpointDriver.EncoderDirection Y_ENCODER_DIRECTION =
            GoBildaPinpointDriver.EncoderDirection.FORWARD;
}
