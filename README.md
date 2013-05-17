Locality UUID (vB) Java
=======================

This is a UUID class intended to help control data locality when inserting into a distributed data 
system, such as MongoDB or HBase. There is also [a Ruby implementation](http://github.com/groupon/locality-uuid.rb).
This version does not conform to any external standard or spec. Developed at Groupon in Palo Alto
by Peter Bakkum and Michael Craig.

Problems we encountered with other UUID solutions:

- Collisions when used with heavy concurrency.
- Difficult to retrieve useful information, such as the timetamp, from the id.
- Time-based versions (such as v1) place this component at the front, meaning all generated ids 
    start with the same bytes for some time period. This was disadvantageous for us because all
    of these writes were hashed to the same shards of our distributed databases. Thus, only one
    machine was receiving writes at a given time.

Solutions:

- We have tested thoroughly in a concurrent environment and include both the PID and MAC Address 
    in the UUID. In the first block of the UUID we have a counter which we increment with a large
    primary number, ensuring that the counter in a single process takes a long time to wrap around
    to the same value.
- The UUID layout is very simple and documented below. Retrieving the millisecond-precision timestamp
    is as simple as copying the last segment and converting from hex to decimal.
- To ensure that generated ids are evenly distributed in terms of the content of their first few bytes,
    we increment this value with a large number. This means that DB writes using these values are
    evenly distributed across the cluster. We ALSO allow toggling into sequential mode, so that the
    first few bytes of the UUID are consistent and writes done with these keys hash to tho same machine
    in the cluster, when this characteristic is desireable. Thus, this library is a tool for managing
    the locality of reads and writes of data inserted with UUID keys.

This library has both Java and Ruby implementations, and is also accessible from the command line.

Format
------

This generates UUIDs in the following format:

```
   wwwwwwww-xxxx-byyy-yyyy-zzzzzzzzzzzz

w: counter value
x: process id
b: literal hex 'b' representing the UUID version
y: fragment of machine MAC address
z: UTC timestamp (milliseconds since epoch)
```

Example:

```
   20be0ffc-314a-bd53-7a50-013a65ca76d2

counter     : 3,488,672,514
process id  : 12,618
MAC address : __:__:_d:53:7a:50
timestamp   : 1,350,327,498,450 (Mon, 15 Oct 2012 18:58:18.450 UTC)
```

Example Use
-----------

If the jar is in one of your repositories, add this to your pom.xml:

```XML
<dependency>
  <groupId>com.groupon</groupId>
  <artifactId>locality-uuid</artifactId>
  <version>1.0.0</version>
</dependency>
```

Use it in a program:

```Java
import com.groupon.uuid.UUID;
import java.util.Arrays;

public class Example {
  public static void main(String[] args) {
      UUID generated = new UUID();

      System.out.println("UUID            : " + generated.toString());
      System.out.println("raw bytes       : " + Arrays.toString(generated.getBytes()));
      System.out.println("process id      : " + generated.getProcessId());
      System.out.println("MAC fragment    : " + Arrays.toString(generated.getMacFragment()));
      System.out.println("timestamp       : " + generated.getTimestamp());
      System.out.println("UUID version    : " + generated.getVersion());

      UUID copy = new UUID(generated.toString());
      System.out.println("copied          : " + generated.equals(copy));
  }
}
```

Or get the jar and run from the command line:

```
java -cp locality-uuid-1.2.0.jar com.groupon.uuid.GenerateUUID
```

Notes
-----

This UUID version was designed to have easily readable PID, MAC address, and
timestamp values, with a regularly incremented count. The motivations for this
implementation are to reduce the chance of duplicate ids, store more useful
information in UUIDs, and ensure that the first few characters vary for successively
generated ids, which can be important for splitting ids over a cluster. The UUID
generator is also designed to be be thread-safe without locking.

Uniqueness is supported by the millisecond precision timestamp, the MAC address
of the generating machine, the 2 byte process id, and a 4 byte counter. Thus,
a UUID is guaranteed to be unique in an id space if each machine allows 65,536 processes or less,
does not share the last 28 bits of its MAC address with another machine in the id
space, and generates fewer than 4,294,967,295 ids per millisecond in a process.

___Counter___
The counter value is reversed, such that the least significant 4-bit block is the first
character of the UUID. This is useful because it makes the earlier bits of the UUID
change more often. Note that the counter is not incremented by 1 each time, but rather
by a large prime number, such that its incremental value is significantly different, but
it takes many iterations to reach the same value.

Examples of sequentially generated ids in the default counter mode:
```
c8c9cef9-7a7f-bd53-7a50-013e4e2afbde
14951cfa-7a7f-bd53-7a50-013e4e2afbde
6f5169fb-7a7f-bd53-7a50-013e4e2afbde
ba2da6fc-7a7f-bd53-7a50-013e4e2afbde
06f8f3fc-7a7f-bd53-7a50-013e4e2afbde
51c441fd-7a7f-bd53-7a50-013e4e2afbde
ac809efe-7a7f-bd53-7a50-013e4e2afbde
f75cdbff-7a7f-bd53-7a50-013e4e2afbde
```

Note the high variability of the first few characters.

The counter can also be toggled into sequential mode to effectively reverse this logic.
This is useful because it means you can control the locality of your data as you generate
ids across a cluster. Sequential mode works by creating an initial value based on a hash
of the current date and hour. This means it can be discovered independently on distributed
machines. The value is then incremented by one for each id generated. If you use key-based
sharding, data inserted with these ids should have some locality.


Examples of sequentially generated ids in sequential counter mode:
```
f5166777-7a7f-bd53-7a50-013e4e2afc26
f5166778-7a7f-bd53-7a50-013e4e2afc26
f5166779-7a7f-bd53-7a50-013e4e2afc26
f516677a-7a7f-bd53-7a50-013e4e2afc26
f516677b-7a7f-bd53-7a50-013e4e2afc26
f516677c-7a7f-bd53-7a50-013e4e2afc26
f516677d-7a7f-bd53-7a50-013e4e2afc26
f516677e-7a7f-bd53-7a50-013e4e2afc26
```

___PID___
This value is just the current process id modulo 65,536. In my experience, most linux
machines do not allow PID numbers to go this high, but OSX machines do.

___MAC Address___
The last 28 bits of the first active MAC address found on the machine. If no active
MAC address is found, this is filled in with zeroes.

___Timestamp___
This is the UTC milliseconds since Unix epoch. To convert to a time manually first
copy the last segment of the UUID, convert to decimal, then use a time library to
count up from 1970-1-1 0:00:00.000 UTC.


API
---

__UUID()__

Generate a new UUID object.

__UUID(String uuid)__

Construct a UUID with the given String, must be of the form `xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx`
where `x` matches `[0-9a-f]`.

__UUID(byte[] uuid)__

Construct a UUID given its raw byte array contents.

__static void useSequentialIds()__

Toggle into sequential mode, so ids are generated in order.

__static void useVariableIds()__

Toggle into variable mode, so the first few characters of each id vary during generation. This is the default mode.

__byte[] getBytes()__

Get raw byte content of UUID.

__String toString()__

Get UUID String in the standard format.

__char getVersion()__

Return the UUID version character, which is 'b' for ids generated by this library.

__int getProcessId()__

Return process id embedded in UUID.

__Date getTimestamp()__

Return timestamp embedded in UUID, which is set at generation.

__byte[] getMacFragment()__

Get the embedded MAC Address fragment. This will be 6 bytes long, with the first two and a half bytes set to 0.
