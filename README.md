# kineis-async

A different take on a AWS Kinesis consumer.

Rather than supplying a function when creating a worker, `start-worker` will instead return a core.async channel. The consumer will stop checkpointing in AWS as soon as the channel is closed.

## TODO

 - Figure out how to kill the worker when the channel is closed.
 - Exclude all of the Amazonica dependencies until we have only the bare minimum required
 - Remove dependency on Amazonica altogether

## License

Copyright © 2016 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
