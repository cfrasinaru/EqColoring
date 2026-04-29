/*
 * Copyright (C) 2026 Cristian Frăsinaru and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package ro.uaic.info.eqcol;

import org.graph4j.exceptions.TimeLimitExceededException;

/**
 *
 * @author Cristian Frăsinaru
 */
public class Timer {

    private final long timeLimitMs;
    private final long deadline;
    private int counter = 0;
    private static final int CHECK_INTERVAL = 1024;

    public Timer(long timeLimitMs) {
        this.timeLimitMs = timeLimitMs;
        this.deadline = timeLimitMs <= 0 ? 0 : System.nanoTime() + timeLimitMs * 1_000_000;
    }

    public void check() {
        if (timeLimitMs <= 0) {
            return;
        }
        if ((++counter & (CHECK_INTERVAL - 1)) == 0) {
            if (System.nanoTime() > deadline) {
                throw new TimeLimitExceededException(timeLimitMs);
            }
        }
    }
}
