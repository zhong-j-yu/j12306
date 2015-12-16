package j12306;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;

import java.util.concurrent.*;

import static j12306.Constants.NULL;
import static j12306.Constants.SEGMENTS;
import static j12306.Constants.TRAINS;
import static j12306.Ticket.getLength;
import static j12306.Ticket.getSeat;
import static j12306.Ticket.getStart;
import static j12306.TrainTicketMap.allocate;
import static j12306.TrainTicketMap.reserve;
import static j12306.TrainTicketMap.trains;

public class TestLmax
{
    // use LMAX disruptor to coordinate reservation requests and processors
    //     https://lmax-exchange.github.io/disruptor/
    //     disruptor-3.3.2.jar

    // the "Event" is reservation request.
    // there is  1 event publisher, e.g. the IO thread taking requests from front ends.
    // there are 3 event consumers in this demo
    //     C1
    //         process requests sequentially; if success, the result is the reserved seat.
    //     C2, C3
    //         for generating responses, writing transaction logs, etc.
    //         both depend on C1's result. no dependency between C2 and C3 in this demo.
    //         currently C2,C2 are no-op; they are included to illustrate the architecture.
    //

    static class Req
    {
        int   train;
        short start;
        short length;

        // C1's result
        int seat; // failure = -1
    }


    static void publishReq(RingBuffer<Req> ring, int train, short start, short length)
    {
        long sequence = ring.next();  // Grab the next sequence
        try
        {
            Req req = ring.get(sequence); // Get the entry in the Disruptor
            // for the sequence
            req.train = train;
            req.start = start;
            req.length = length;
        }
        finally
        {
            ring.publish(sequence);
        }
    }


    // process a reservation request
    static EventHandler<Req> C1 = (Req req, long sequence, boolean endOfBatch) ->
    {
        /*T*/int t = allocate(trains[req.train], req.start, req.length);
        if (NULL != t) {
            t = reserve(trains[req.train], req.start, req.length, t);
            assert t!=NULL;
            TicketPool.free(t);
            req.seat = getSeat(t);
        } else {
            req.seat = -1;
        }
    };

    static EventHandler<Req> C2 = (Req req, long sequence, boolean endOfBatch) ->
    {
        //System.out.printf("C2 - %d %d %d %d %n", req.train, req.start, req.length, req.seat);
    };

    static EventHandler<Req> C3 = (Req req, long sequence, boolean endOfBatch) ->
    {
        //System.out.printf("C3 - %d %d %d %d %n", req.train, req.start, req.length, req.seat);
    };


    // -Xms4G -Xmx4G
    public static void main(String[] args)  throws Exception
    {

        // Executor that will be used to construct new threads for consumers
        ExecutorService executor = Executors.newFixedThreadPool(3);

        // Specify the size of the ring buffer, must be power of 2.
        int bufferSize = 1024;

        // Construct the Disruptor with a SingleProducerSequencer
        Disruptor<Req> disruptor = new Disruptor<>(Req::new,
            bufferSize,
            executor,
            ProducerType.SINGLE, // Single producer
            new BlockingWaitStrategy()
        );

        disruptor.handleEventsWith(C1);
        disruptor.after(C1).handleEventsWith(C2);
        disruptor.after(C1).handleEventsWith(C3);

        // Start the Disruptor, starts all threads running
        disruptor.start();

        // Get the ring buffer from the Disruptor to be used for publishing.
        RingBuffer<Req> ring = disruptor.getRingBuffer();

        // main thread publishes reservation requests ===============================================

        int N = 30_000_000;

        TrainTicketMap.init();
        ThreadLocalRandom rand = ThreadLocalRandom.current();
        long t1 = System.nanoTime();
        System.out.printf("start benchmark\n");
        for (int i=0; i<N; i++) {
            int train = rand.nextInt(TRAINS);
            int start = rand.nextInt(SEGMENTS);
            int length = rand.nextInt(SEGMENTS - start);
            if (length == 0) {
                length = 1;
            }

            publishReq(ring, train, (short)start, (short)length);

        }
        long t2 = System.nanoTime();
        // at this point, not all events have been processed; but it's close.
        System.out.printf("benchmark: ns/req = %d %n", (t2 - t1) / N);

        disruptor.shutdown(1, TimeUnit.SECONDS);
        executor.shutdown();

    }




}
