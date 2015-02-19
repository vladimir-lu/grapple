_**grapple**_

* _(transitive) To seize something and hold it firmly._
* _(nautical) A device consisting of iron claws, attached to the end of a rope, used for grasping and holding an enemy ship prior to boarding; a grapnel or grappling iron._

# Grapple - a minimal free software application launcher for Java applications

_**WARNING: currently alpha quality; may accidentally your cat**_

## Description

Grapple is a launcher and updater for applications whose contents are served over HTTP. It is written in Java and
primarily caters for JVM applications that also need to bundle a JVM. It may optionally be combined with Java Web Start
as the initial delivery mechanism.

### Goals

Grapple aims to achieve:

 * Separation of system JVM from application target JVM without administrative intervention of any kind
 * Simplicity of application deployment by keeping all assets (optionally) immutable

For more information, see [design](doc/design.md)

## Usage

To use Grapple with a JVM target follow these steps:

 1. Generate two manifests for the JVM *and* the application using `org.halfway.grapple.ManifestTool`
 2. Place the two directories under a HTTP server
 3. Run `org.halfway.grapple.Grapple` as described below:

    ```
    java
        -Dgrapple.application.root=/opt/application
        -Dgrapple.application.name=application
        -Dgrapple.application.urls=http://localhost:8080/static/application
        -Dgrapple.application.jvm.urls=http://localhost:8080/static/jvm
        -Dgrapple.application.jvm.root=/opt/application-jvm
        -Dgrapple.application.jvm.main-class=my.application.Main
        -jar grapple.jar
    ```

## Roadmap

The software is currently *alpha* quality and will not be considered *stable* until version 1.0. The following releases
are planned leading up to `1.0`:

 * `0.8`
   * initial release on github
   * travis build
 * `0.9`
   * RFCs for manifest format and configuration
   * Add support for creating Windows start menu shortcuts
   * Add support for logging into a file
   * Add unit tests that test the update process end-to-end using an in-process HTTP server
 * `1.0`
   * Add support for signed JNLP scenarios
   * Finish documentation
   * Add support for localization of messages
   * Stabilise the manifest and mandatory configuration options
   * Publish unsigned jar in maven central

After `1.0` the goal is to follow semantic versioning with respect to the manifest format.

### Future work

There are items that will not be tackled by `1.0`:

 * UI design improvements, including testing of things like HiDPI scaling issues
 * Diffing of jar files to reduce download size

Please file an issue if the above or anything else is a problem for you.

## Other software

See [other software](doc/other_software.md) for a discussion of projects that accomplish similar goals.

## Contribution process

Grapple is the work of its contributors and is a free software project licensed under the LGPLv3 or later.
[Chapter 6](http://zguide.zeromq.org/page:chapter6#The-Importance-of-Contracts) of the ØMQ guide explains why you should
also pick the GPL for your next project.

If you would like to contribute please follow the [C4](http://rfc.zeromq.org/spec:22) process.
