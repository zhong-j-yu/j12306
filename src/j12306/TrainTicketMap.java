package j12306;

import static j12306.Constants.*;
import static j12306.Ticket.*;

// this is the key class for ticket reservation algorithm
// each train maintains a `map`, which is a 10x10 matrix of tickets.

public class TrainTicketMap
{
    static /*T*/int[] initTickets()
    {
        /*T*/int[] map = new /*T*/int[SEGMENTS * SEGMENTS];

        /*T*/int prev = NULL;
        for (int i=0; i<SEATS; i++) {
            /*T*/int t = TicketPool.allocate();
            setStartLength(t, (short) 0, (short) SEGMENTS);
            setSeat(t, i + 1);
            setNext(t, NULL);
            if (NULL == prev) {
                map[0 + (SEGMENTS - 1) * SEGMENTS] = t;
            } else {
                setNext(prev, t);
            }
            prev = t;
        }

        return map;
    }

    static final /*T*/int[][] trains = new int[TRAINS][];
    static
    {
        System.out.println("init trains...");
        for(int i=0; i<TRAINS; i++)
            trains[i] = initTickets();
        System.out.println("init trains... done");
    }

    public static void init()
    {
        // nothing; just to trigger static initializer
    }


    // ---------------------------------------------------------------

    static int[] offsets; //  [start,length, start,length, ...]
    static // generateSearchPatterns
    {
        int nSearches = (SEGMENTS+1)*SEGMENTS/2;
        offsets = new int[nSearches*2];
        int x = 0;
        for (int i = 0; i<SEGMENTS; i++) {
            for (int j = 0; j<=i; j++) {
                offsets[x++] = -j;
                offsets[x++] = i-j;
            }
        }
    }

    static /*T*/int allocate(/*T*/int[] map, int start, int length)
    {
        /*T*/int ret = NULL;
        length--;		// normalize to [0, 9]
        for (int it = 0; it<offsets.length; )
        {
            int it_start  = offsets[it++];
            int it_length = offsets[it++];
            int curStart = start + it_start;
            int curLength = length - it_start + it_length;
            if (curStart >= 0 && curLength < SEGMENTS) {
                int index = curStart + curLength * SEGMENTS;
                if (NULL != map[index]) {
                    ret = map[index];
                    break;
                }
            }
        }
        return ret;
    }

    static /*T*/int reserve(/*T*/int[] map, int start, int length, /*T*/int t)
    {
        short t_start = getStart(t);
        short t_length = getLength(t);

        int index = (t_length - 1) * SEGMENTS + t_start;
        /*T*/int ret = map[index];
        if (NULL != ret)
        {
            map[index] = getNext(ret);
            {
                int startDelta = start - t_start;
                if (startDelta > 0) {
                    /*T*/int p = TicketPool.allocate();
                    setSeat(p, getSeat(ret));
                    setStartLength(p, t_start, (short) startDelta);
                    int index2 = getStart(p) + (startDelta - 1) * SEGMENTS;
                    setNext(p, map[index2]);
                    map[index2] = p;
                }
            }
            {
                int tailDelta = t_start + t_length - start - length;
                if (tailDelta > 0) {
                    /*T*/int p = TicketPool.allocate();
                    setSeat(p, getSeat(ret));
                    setStartLength(p, (short)(start + length), (short)tailDelta);
                    int index2 = getStart(p) + (tailDelta - 1) * SEGMENTS;
                    setNext(p, map[index2]);
                    map[index2] = p;
                }
            }
        }
        return ret;
    }


}
