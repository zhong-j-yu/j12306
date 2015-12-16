package j12306;

import java.util.concurrent.ThreadLocalRandom;

import static j12306.Constants.*;
import static j12306.TrainTicketMap.*;
import static j12306.Ticket.*;


public class Test0
{
    public static void main(String[] args)
    {
        // some simple tests from original code from weiqj@github

        TrainTicketMap.init();

        // very simple tests of correctness
        test1();

        // performance test on allocate/reserve, single thread
        benchmark();
    }

    static void testReserve(int train, int start, int length)
    {
        System.out.printf("Reserving Start=%d Length=%d\n", start, length);
        /*T*/int t = allocate(trains[train], start, length);
        if (NULL != t) {
            t = reserve(trains[train], start, length, t);
            assert t!=NULL;
            System.out.printf("Succeed number=%d start=%d length=%d\n", getSeat(t), getStart(t), getLength(t));
            TicketPool.free(t);
        } else {
            System.out.printf("Failed\n");
        }
    }

    static void test1() {
        for (int i=0; i<SEATS + 2; i++) {
            testReserve(0, 3, 1);
            testReserve(0, 0, 3);
            testReserve(0, 4, 6);
        }
    }

    static void benchmark() {
        ThreadLocalRandom rand = ThreadLocalRandom.current();
        double t1 = System.currentTimeMillis();
        System.out.printf("start benchmark\n");
        for (int i=0; i<100000000; i++) {
            int train = rand.nextInt(TRAINS);
            int start = rand.nextInt(SEGMENTS);
            int length = rand.nextInt(SEGMENTS - start);
            if (length == 0) {
                length = 1;
            }
            assert((start + length) <= SEGMENTS);
            /*T*/int t = allocate(trains[train], start, length);
            if (NULL != t) {
                t = reserve(trains[train], start, length, t);
                assert t!=NULL;
                TicketPool.free(t);
            }
        }
        double t2 = System.currentTimeMillis();
        System.out.printf("Total time = %f\n", (t2 - t1) / 1000);


    }

}
