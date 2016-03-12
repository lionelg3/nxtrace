# The nxtraces project

The nxtraces project is a mail store for Unix system.

## Build

    $ mvn clean package
    
## Requirements
    
* java 8

Check your java version using the command :

    $ java -version
    java version "1.8.0_74"
    Java(TM) SE Runtime Environment (build 1.8.0_74-b02)
    Java HotSpot(TM) 64-Bit Server VM (build 25.74-b02, mixed mode)
    
or if you run OpenJDK
    
    $ java -version
    openjdk version "1.8.0_72-internal"
    OpenJDK Runtime Environment (build 1.8.0_72-internal-b15)
    OpenJDK 64-Bit Server VM (build 25.72-b15, mixed mode)
    
## Install 
    
Ensure the java is in your $PATH.
    
    $ which java

Then run :
    
    $ tar xvfz nxtraces-.tar.gz
    $ cd nxtraces-*
    $ ./bin/nxstraces start

    
## Basic Unix client configuration with Postfix

File : main.cf

   inet_interfaces = loopback-only
   default_transport = postmail
   relay_transport = postmail
   
File : master.cf

Append the folling lines :
   
    postmail  unix  -       n       n       -       -       pipe user=nobody 
      argv=/usr/local/sbin/postmail

Create the file "/usr/local/sbin/postmail" :
 
    #!/bin/sh
    
    EX_TEMPFAIL=75
    CURL=/usr/bin/curl
    NXTRACES_SERVER_IP=localhost
    NXTRACES_PORT=8080
    
    TARGET="http://$NXTRACES_SERVER_IP:$NXTRACES_PORT/api/post"
    CMD="$CURL -s --form mail=@- $TARGET"
    $CMD || exit $EX_TEMPFAIL 

Reload postfix : 

    $ postfix reload
    
## Simple test

Any mail sent from the client using "mail" or "sendmail" command will be received
and indexed by the "nxtraces" server.

    $ sendmail -i -t fox.mulder@x-files.org
    Subject: I want to believe
    
    Mulder and Scully have been out of the FBI for several years — with Mulder living 
    in isolation as a fugitive from the organization and Scully having become a doctor 
    at a Catholic-run hospital, where she has formed a friendly relationship with a seriously 
    ill boy patient — but, when an FBI agent is mysteriously kidnapped and a former priest 
    who has been convicted of being a pedophile claims to be experiencing psychic visions 
    of the endangered agent, Mulder and Scully reluctantly accept the FBI's request for their 
    particular paranormal expertise on the case.
    
Now this email is store and indexed, the query file "q1.json" :

    {
      "max": 10,
      "first": 0,
      "queries": [
        [
          "containts", "subject", "believe"
        ]
      ]
    }
    
Use curl to submit the query :
    
    $ curl -H "Content-Type: application/json" -X POST -d @q1.json http://localhost:8080/api/query
    
Will give the result :
    
    {
      "count": 1,
      "results": [
        {
          "id": "HQDWEZAL7FLPFEH7TUPJFZSMPX4KDR7DVPM36LI7UFTTCZBLJ7ZA====",
          "subject": "I want to believe",
          "date": "10:53:07 11/03/2016"
        }
      ],
      "request": {
        "max": 10,
        "first": 0,
        "queries": [
          [
            "containts",
            "subject",
            "believe"
          ]
        ]
      }
    }

The full email content can be load :

    $ curl -s http://localhost:8080/api/query/load?id=HQDWEZAL7FLPFEH7TUPJFZSMPX4KDR7DVPM36LI7UFTTCZBLJ7ZA====" 

    {
      "content-type": "text/plain",
      "content": "Mulder and Scully have been out of the FBI for several years â with Mulder living in isolation as a fugitive from the organization and Scully having become a doctor at a Catholic-run hospital, where she has formed a friendly relationship with a seriously ill boy patient â but, when an FBI agent is mysteriously kidnapped and a former priest who has been convicted of being a pedophile claims to be experiencing psychic visions of the endangered agent, Mulder and Scully reluctantly accept the FBI's request for their particular paranormal expertise on the case.\n.\n",
      "from": "root <root@d8client>",
      "messageid": "<20160311095318.B57F93B30@d8client.lan>",
      "recipients": "",
      "size": 569,
      "id": "HQDWEZAL7FLPFEH7TUPJFZSMPX4KDR7DVPM36LI7UFTTCZBLJ7ZA====",
      "subject": "I want to believe",
      "date": "10:53:07 11/03/2016"
    }
