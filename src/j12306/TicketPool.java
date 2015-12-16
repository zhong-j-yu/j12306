package j12306;

import static j12306.Constants.*;

// In the original C++ solution from weiqj@github, there's an object pool of all Tickets
// It's unclear whether we should do the same thing in Java.
// For now, we aim at faithful porting, so we'll follow the original design.
//
// However, JVM seems to struggle really hard with a huge array of Ticket objects.
// Instead, we'll directly manage a huge array of integers as ticket data.

public class TicketPool
{
    static final int N = SEGMENTS * TRAINS * SEATS;  // max number of tickets

    // bytes representing ticket data. 0 means NULL, therefore POOL[0] is reserved.
    static final int OFF = 1;
    static final int[] POOL = new int[ OFF + N * Ticket.SIZE_OF];
    // this is a huge array; try jvm memory settings:  -Xms4G -Xmx4G


    static /*T*/int head;

    // pointer to i-th ticket in pool
    static /*T*/int ticket(int i)
    {
        return OFF + i * Ticket.SIZE_OF;
    }

    static // init pool
    {
        System.out.println("init ticket pool...");
        for(int i=0; i<N; i++)
        {
            /*T*/int cur = ticket(i);
            Ticket.setNext(cur, ticket(i + 1));
        }
        Ticket.setNext(ticket(N - 1), NULL);
        head = ticket(0);
        System.out.println("init ticket pool... done");
    }

    static /*T*/int allocate()
    {
        /*T*/int ret = head;
        head = Ticket.getNext(head);
        return ret;
    }

    static void free(/*T*/int t)
    {
        Ticket.setNext(t, head);
        head = t;
    }

}
