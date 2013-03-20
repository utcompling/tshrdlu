# tshrdlu
=======

Author: **Jason Baldridge** (jasonbaldridge@gmail.com)

This is a parent repository for [project](https://github.com/utcompling/applied-nlp/wiki/Course-Project) related code for [Applied NLP course](https://github.com/utcompling/applied-nlp/wiki) being taught by [Jason Baldridge](http://www.jasonbaldridge.com) at [UT Austin](http://www.utexas.edu). This involves creating applications that use Twitter streams and can take automated actions as Twitter users, using natural language processing and machine learning.

The name "tshrdlu" comes from Twitter+[SHRDLU](http://en.wikipedia.org/wiki/SHRDLU).

For more information, updates, etc., follow [@appliednlp](https://twitter.com/appliednlp) on Twitter. The [@tshrdlu](https://twitter.com/tshrdlu) account is now doing some tweeting of its own (by which I mean automated tweeting, based on the code in this repository).

## Requirements

* Version 1.6 of the Java 2 SDK (http://java.sun.com)

## Configuring your environment variables

The easiest thing to do is to set the environment variables `JAVA_HOME`
and `TSHRDLU_DIR` to the relevant locations on your system. Set `JAVA_HOME`
to match the top level directory containing the Java installation you
want to use.

Next, add the directory `TSHRDLU_DIR/bin` to your path. For example, you
can set the path in your `.bashrc` file as follows:

	export PATH=$PATH:$TSHRDLU_DIR/bin

Once you have taken care of these three things, you should be able to
build and use tshrdlu.

If you plan to index and search objects using the provided code based on
Lucene, you can customize the directory where on-disk indexes are stored (the
default is your home directory) by setting the environment variable
`TSHRDLU_INDEX_DIR`.


## Building the system from source

tshrdlu uses SBT (Simple Build Tool) with a standard directory
structure.  To build tshrdlu, type (in the `TSHRDLU_DIR` directory):

	$ ./build update compile

This will compile the source files and put them in
`./target/classes`. If this is your first time running it, you will see
messages about Scala being downloaded -- this is fine and
expected. Once that is over, the tshrdlu code will be compiled.

To try out other build targets, do:

	$ ./build

This will drop you into the SBT interface. To see the actions that are
possible, hit the TAB key. (In general, you can do auto-completion on
any command prefix in SBT, hurrah!)

To make sure all the tests pass, do:

	$ ./build test

Documentation for SBT is at <http://www.scala-sbt.org/>

Note: if you have SBT already installed on your system, you can
also just call it directly with "sbt" in `TSHRDLU_DIR`.


# Questions or suggestions?

Email Jason Baldridge: <jasonbaldridge@gmail.com>

Or, create an issue: <https://github.com/utcompling/tshrdlu/issues>
