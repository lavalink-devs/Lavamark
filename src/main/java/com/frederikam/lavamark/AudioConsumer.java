package com.frederikam.lavamark;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

public class AudioConsumer extends Thread {

    private static final Logger log = LoggerFactory.getLogger(AudioConsumer.class);

    private final Player player;
    private final int INTERVAL = 20; // A frame is 20ms

    private static AtomicInteger served = new AtomicInteger();
    private static AtomicInteger missed = new AtomicInteger();
    private static EndReason endReason = EndReason.NONE;
    private static boolean running = true;

    AudioConsumer(Player player) {
        this.player = player;
    }

    @Override
    public void run() {
        long start = System.currentTimeMillis();
        long i = 0;

        //noinspection InfiniteLoopStatement
        while (running) {

            if (player.canProvide()) {
                served.incrementAndGet();
            } else {
                missed.incrementAndGet();
            }

            long targetTime = ((start / INTERVAL) + i + 1) * INTERVAL;
            long diff = targetTime - System.currentTimeMillis();
            i++;

            if(diff < -5000) {
                endReason = EndReason.CANT_KEEP_UP;
                break;
            }

            synchronized (this) {
                try {
                    if(diff > 0) {
                        sleep(diff/2);
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    static Results getResults() {
        int serv = served.getAndSet(0);
        int miss = missed.getAndSet(0);

        if((serv + miss) / 100 < miss) {
            endReason = EndReason.MISSED_FRAMES;
            running = false;
        }

        return new Results(serv, miss, endReason);
    }

    public static class Results {
        int served;
        int missed;
        EndReason endReason;

        Results(int served, int missed, EndReason endReason) {
            this.served = served;
            this.missed = missed;
            this.endReason = endReason;
        }

        public int getServed() {
            return served;
        }

        public int getMissed() {
            return missed;
        }

        public EndReason getEndReason() {
            return endReason;
        }

        public String getLossPercentString() {
            //log.info("Miss " + missed);
            //log.info("Serv " + served);
            double frac = 1 - ((double) served) / ((double) (served + missed));
            frac = Math.floor(frac * 10000) / 10000;
            frac = frac * 100;
            return frac + "%";
        }
    }

    public enum EndReason {
        NONE,
        CANT_KEEP_UP,
        MISSED_FRAMES;
    }
}
