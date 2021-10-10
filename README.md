# FTP Media Server
A simple media server written in Java with just enough of the FTP
protocol implemented for it to work with VLC.

## Usage
Just run the `jar` file. Right now it supports no configuration
and serves  the current directory on port `2121`.

## Security Notes
This server does not use encryption or any password protection.
However, care has been taken such that files outside the current
directory cannot be downloaded (e.g. using `../`).

## Motivation
I wanted a simple way to stream media from my computer to VLC on my
Android phone or Android TV.

Normally DLNA/UPnP is the de-facto standard here, but it has two
issues:

 1) Good implementations of UPnP often comes embedded in a
    full-featured media server which is not something I want.
    I just want a simple way to serve files from the current
    directory.
 2) UPnP is great when it works, but when it doesn't, it's a
    pain to debug.

Thus, I went and look for the simplest protocol VLC supports
that also supports directory listing. VLC does not support
directory listing on HTTP so the next simplest protocol was
FTP.

First, I tried to run one of the standard FTP servers written
in C. However, they were full-featured FTP servers meant to
support read-write and multi-user usage. They were hard to
set up correctly in a self-contained way (e.g. without adding
users to the system passwd file) and wanted to be run as root.
Some of them also do not work properly with VLC.

So finally I have decided that it would be the easiest to
just implement a simple FTP server in Java.