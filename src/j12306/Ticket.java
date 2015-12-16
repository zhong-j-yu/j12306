package j12306;

import static j12306.TicketPool.POOL;

// We don't use java Object to represent tickets.
// Instead, a ticket is just some bytes in TicketPool.POOL.
// A ticket is identified by an `int` pointer, which is its address in POOL.
// There's no typedef in java, so we'll write `/*T*/int` to denote the type.
//
// This class helps to manipulate ticket data in a struct-like way,
// but instead of `ticket.seat=1`, do `setSeat(ticket, 1)`


public class Ticket
{
    static final int SIZE_OF = 3;
    // a ticket requires 3 ints
    //
    //   int       seat
    //   short     start
    //   short     length
    //   /*T*/int  next

    static int getSeat(/*T*/int ticket)
    {
        return POOL[ticket+0];
    }
    static void setSeat(/*T*/int ticket, int seat)
    {
        POOL[ticket+0] = seat;
    }

    static /*T*/int getNext(/*T*/int ticket)
    {
        return POOL[ticket+2];
    }
    static void setNext(/*T*/int ticket, /*T*/int next)
    {
        POOL[ticket+2] = next;
    }

    static short getStart(/*T*/int ticket)
    {
        int x = POOL[ticket+1];
        return (short)(x>>16);
    }
    static short getLength(/*T*/int ticket)
    {
        int x = POOL[ticket+1];
        return (short)x;
    }
    static void setStartLength(/*T*/int ticket, short start, short length)
    {
        POOL[ticket+1] = (start<<16) | length;
    }

}
