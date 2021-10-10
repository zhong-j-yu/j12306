# A Toy Model of Train Ticket Reservation System

Original C++ version from *Qingjun Wei* - https://github.com/weiqj/pc12306

This is a Java port. I'm interested in finding out how well the architecture works on Java platforms.

"12306" refers to Chinese Railroad website - http://www.12306.cn


## Trains and Tickets

There are a lot of trains; each train has a lot of seats;
each train runs on a fixed path with multiple stops.

A customer wants to ride from station A to station B;
this may require multiple connecting trains and multiple tickets;
each ticket is for a fixed seat on a single train.

The question here is how to *atomically* reserve these tickets.
The solution we are exploring here processes all reservation requests sequentially on a single thread.
This is a more pleasant programming model when dealing with contention on multiple resources.
However, is a single computer powerful enough to handle the tremendous request load on Chinese New Year?


## Simplification

We aren't going to write a complete system; we only want to study performance matrix on the key component
of the system. For that purpose, we assume that, the system has constant number of resources --
there are `5000` trains; each train has `3000` seats; each train has `11` stops (i.e. `10` segments).


## Single Train Reservation

`TrainTicketMap` handles query and reservation on a single train.
Given a train, and departure-arrival stations,
it answers whether a seat is available; and whether it can be reserved (on a followup request).
All information are kept in in-memory data structure.


## Sequential Execution

An online ticket purchase is a complex action; fortunately many parts of the action can be executed in parallel.
The most difficult part to parallelize is seat reservation. Our solution is to extract that part (in bare minimum)
and execute it sequentially. The correctness of the solution is much simpler to prove; and the performance of
the solution is very stable and quantifiable.


## LMAX Disruptor

To handle concurrent interactions between reservation processor and other parts of the system,
we use LMAX Disruptor (https://lmax-exchange.github.io/disruptor/),
which is designed exactly for this type of application.
See my explanation on LMAX: https://stackoverflow.com/a/6715618/2158288

See class [`TestLmax`](https://github.com/zhong-j-yu/j12306/blob/master/src/j12306/TestLmax.java#L21).
There is one event publisher, which queues reservation requests.
All requests are processed sequentially by consumer `C1`, which only invokes `TrainTicketMap`.
`C1` is really fast.
We also throw in C2 and C3 which depend on C1; they are no-op now, but they can be used for
transaction logging, response writing, etc.

    P1 -->  <request queue>  -->  C1 -----> C2
                                    `-----> C3

P1, C1, C2, C3 all run on their own threads; there will be costs of data handover between them, but not much.

`TestLmax` can easily achieve `1M req/s` on a commodity PC.
This indicates that the architecture is feasible on Java platforms to handle real world workloads.



## Why Java

If we are building such a high throughput system, every CPU cycle counts;
shouldn't we use something more metal instead, like C++?

It depends on economics. Java is cheaper in a lot of senses.
Given the budget, time-to-market, and other constraints,
you may not be able to afford to find a competent C++ guy that can do the job.
But Java programmers that can write something like `TestLmax` are dime a dozen:)

If Java is adequate to meet the performance requirement of an application,
we can turn the question around and ask, Why C++?

