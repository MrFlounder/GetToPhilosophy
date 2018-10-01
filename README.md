# GetToPhilosophy

To run this app locally:

1. Clone the project to local
2. Install/run postgres server, set your db username/password in system env, create database: bento_db, create table with: `create table path_track (id serial, path text);`, grant necessary access to db user.
3. Add jsoup jar and jdbc jar to your CLASSPATH, add project dir to your CLASSPATH too.
4. Compile server.java with `javac Server.java`.
5. Run server with `java Server`.
6. Open the html file in Frontend folder with Chrome.
7. You know what to do now.

Have fun ~
