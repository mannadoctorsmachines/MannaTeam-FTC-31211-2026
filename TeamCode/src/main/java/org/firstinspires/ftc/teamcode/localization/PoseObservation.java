package org.firstinspires.ftc.teamcode.localization;

/**
 * Uma pose observada por uma fonte externa (Vision: AprilTag/Limelight),
 * oferecida à Localization como possível correção.
 *
 * A pose JÁ vem nas unidades do projeto (ver {@link Pose}) e no mesmo sistema
 * de coordenadas da Localization. Quem produz a observação é responsável por
 * essa conversão; se o sistema de coordenadas da fonte ainda não foi alinhado
 * com o da Localization, a fonte deve devolver null em vez de uma observação.
 */
public final class PoseObservation {

    private final Pose pose;
    private final String source;
    private final long timestampMs;

    /**
     * @param pose        pose observada (cm, graus)
     * @param source      identificação da origem (ex.: "limelight")
     * @param timestampMs System.currentTimeMillis() do momento da observação
     */
    public PoseObservation(Pose pose, String source, long timestampMs) {
        this.pose = pose;
        this.source = source;
        this.timestampMs = timestampMs;
    }

    public Pose getPose() {
        return pose;
    }

    public String getSource() {
        return source;
    }

    public long getTimestampMs() {
        return timestampMs;
    }

    public long getAgeMs() {
        return System.currentTimeMillis() - timestampMs;
    }
}
