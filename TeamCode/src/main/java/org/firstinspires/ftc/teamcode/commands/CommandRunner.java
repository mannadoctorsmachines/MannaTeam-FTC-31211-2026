package org.firstinspires.ftc.teamcode.commands;

import java.util.ArrayDeque;
import java.util.Queue;

/**
 * Executor mínimo: roda um Command por vez, em sequência, chamado a cada
 * ciclo do OpMode com update().
 *
 * Ciclo de vida garantido:
 *  - initialize() uma vez, execute() a cada update(), end(false) ao terminar;
 *  - timeout (Command.getTimeoutMs) termina com end(true);
 *  - cancelAll() e cancelCurrent() terminam o Command atual com end(true).
 *
 * Paralelismo e grupos de Commands ainda não existem de propósito.
 */
public class CommandRunner {

    private final Queue<Command> queue = new ArrayDeque<>();
    private Command current = null;

    /** Agenda um Command para rodar depois dos que já estão na fila. */
    public void schedule(Command command) {
        if (command != null) {
            queue.add(command);
        }
    }

    /** Deve ser chamado uma vez por ciclo do OpMode. */
    public void update() {
        if (current == null) {
            current = queue.poll();

            if (current == null) {
                return;
            }

            current.markStarted();
            current.initialize();
        }

        if (current.isFinished()) {
            finishCurrent(false);
            return;
        }

        current.execute();

        if (current.hasTimedOut()) {
            finishCurrent(true);
        } else if (current.isFinished()) {
            finishCurrent(false);
        }
    }

    /** Cancela só o Command atual; os seguintes continuam na fila. */
    public void cancelCurrent() {
        if (current != null) {
            finishCurrent(true);
        }
    }

    /** Cancela o Command atual e descarta a fila. Use no stop() do OpMode. */
    public void cancelAll() {
        queue.clear();
        cancelCurrent();
    }

    public boolean isIdle() {
        return current == null && queue.isEmpty();
    }

    private void finishCurrent(boolean interrupted) {
        Command finished = current;
        current = null;
        finished.end(interrupted);
    }
}
