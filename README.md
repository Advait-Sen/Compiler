# Helium Compiler

A compiler that compiles Helium, a Java-like programming language.

Helium has the extension `.he` (after Helium's symbol, He)

Heavily inspired by this YouTube series which compiles a programming language called Hydrogen: [Creating a Compiler](https://youtube.com/playlist?list=PLUDlas_Zy_qC7c5tCgTMYq2idyyT241qs&feature=shared)

Currently, a work in progress.

The current working idea for the compiler is for it to compile to C

So a program written in Helium will be compiled by this program into a .c file
This is a totally intelligent decision, which has nothing to do with the fact that Helium's syntax is tending towards C
syntax, making the translation pretty simple (so far).

More so than this, is the the fact that I can avoid having to implement the compilation to machine code, since that part
will in theory be handled by whichever C compiler is used (provided of course, that I can compile to C properly)

## Helium Interpreter
Seen as though making a compiler is hard, that part is currently on hold, in favour of making an interpreter for the
language.

This will help flesh out what the language will look like, and allow it to develop a standard form, which should make
coding the compiler easier since the end goal will already be somewhat in sight.

