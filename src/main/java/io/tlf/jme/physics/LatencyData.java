package io.tlf.jme.physics;

import java.util.LinkedList;

public class LatencyData {

    private LinkedList<Sample> oneSecond = new LinkedList<>();
    private LinkedList<Sample> oneMinute = new LinkedList<>();
    private Object lock = new Object();

    public void add(PhysicsEchoMessage msg) {
        synchronized (lock) {
            long received = System.currentTimeMillis();
            Sample s = new Sample();
            //Round trip average
            long delta = received - msg.getServerTime();
            s.delay = delta > 0 ? delta / 2 : 0;
            s.timestamp = received;
            oneSecond.push(s);
            update();
        }
    }

    public void update() {
        synchronized (lock) {
            long current = System.currentTimeMillis();
            while (oneSecond.size() > 0) {
                if (oneSecond.peek().timestamp < current - 1000) {
                    oneSecond.pop();
                } else {
                    break;
                }
            }
            Sample minuteSample = new Sample();
            minuteSample.timestamp = current;
            minuteSample.delay = getSecondAverage();
            oneMinute.push(minuteSample);

            while (oneMinute.size() > 0) {
                if (oneMinute.peek().timestamp < current - 60000) {
                    oneMinute.pop();
                } else {
                    break;
                }
            }
        }
    }

    public long getSecondAverage() {
        synchronized (lock) {
            long total = 0;
            for (Sample s : oneSecond) {
                total += s.delay;
            }
            return total > 0 ? total / oneSecond.size() : 0;
        }
    }

    public long getMinuteAverage() {
        synchronized (lock) {
            long total = 0;
            for (Sample s : oneMinute) {
                total += s.delay;
            }
            return total > 0 ? total / oneMinute.size() : 0;
        }
    }

    private class Sample {
        long delay;
        long timestamp;
    }
}
